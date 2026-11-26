package com.frenchtoast.receiptvault.ocr

import com.frenchtoast.receiptvault.model.VaultDate
import kotlin.math.roundToLong

data class ParsedReceiptResult(
    val storeName: String? = null,
    val itemTitle: String? = null,
    val priceCents: Long? = null,
    val purchaseDateEpochDay: Long? = null,
    val serialNumber: String? = null,
    val returnDeadlineEpochDay: Long? = null,
    val warrantyExpirationEpochDay: Long? = null,
    val maskedText: String = "",
    val didMaskCard: Boolean = false,
    val detectedBarcodes: List<String> = emptyList(),
)

object ReceiptParser {

    private val totalLineRegex = Regex(
        """(?i)(grand\s*total|amount\s*due|total\s*paid|balance\s*due|\btotal\b|\bsale\b).*?(\$?\s*\d{1,3}(?:,\d{3})*\.\d{2})"""
    )
    private val currencyRegex = Regex("""\$\s*(\d{1,3}(?:,\d{3})*\.\d{2})""")
    private val decimalPriceRegex = Regex("""(?<!\d)(\d{1,4}\.\d{2})(?!\d)""")

    private val isoDateRegex = Regex("""\b(20\d{2})[-/](\d{1,2})[-/](\d{1,2})\b""")
    private val usDateRegex = Regex("""\b(\d{1,2})[-/](\d{1,2})[-/](20\d{2}|\d{2})\b""")
    private val textMonthDateRegex = Regex(
        """(?i)\b(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\.?\s+(\d{1,2}),?\s+(20\d{2})\b"""
    )

    private val serialRegex = Regex(
        """(?i)\b(?:s/?n|serial(?:\s*(?:no|num|number|#))?|imei|service\s*tag)\s*[:#-]?\s*([A-Z0-9][A-Z0-9-]{4,23})"""
    )
    private val returnDaysRegex = Regex("""(?i)\b(\d{1,3})\s*[-]?\s*day\s+return""")
    private val warrantyYearsRegex = Regex("""(?i)\b(\d{1,2})\s*[-]?\s*(?:year|yr)\s+(?:limited\s+)?warranty""")

    private val skipHeaderWords = setOf(
        "RECEIPT", "CUSTOMER COPY", "MERCHANT COPY", "WELCOME", "THANK YOU",
        "STORE", "INVOICE", "ORDER", "TAX INVOICE", "DUPLICATE",
    )

    fun parse(rawText: String, barcodes: List<String> = emptyList()): ParsedReceiptResult {
        val maskResult = CardMasker.maskCardNumbers(rawText)
        val text = maskResult.maskedText
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val storeName = extractStoreName(lines)
        val priceCents = extractPriceCents(lines, text)
        val purchaseDate = extractDate(text)
        val serial = extractSerial(text, barcodes)
        val itemTitle = extractItemTitle(lines, storeName)

        val returnDeadline = purchaseDate?.let { pDate ->
            returnDaysRegex.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { days ->
                if (days in 1..365) VaultDate.plusDays(pDate, days) else null
            }
        }
        val warrantyExpiration = purchaseDate?.let { pDate ->
            warrantyYearsRegex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { years ->
                if (years in 1..10) VaultDate.plusYears(pDate, years) else null
            }
        }

        return ParsedReceiptResult(
            storeName = storeName,
            itemTitle = itemTitle,
            priceCents = priceCents,
            purchaseDateEpochDay = purchaseDate,
            serialNumber = serial,
            returnDeadlineEpochDay = returnDeadline,
            warrantyExpirationEpochDay = warrantyExpiration,
            maskedText = text,
            didMaskCard = maskResult.maskedCount > 0,
            detectedBarcodes = barcodes.distinct(),
        )
    }

    private fun extractStoreName(lines: List<String>): String? {
        for (line in lines.take(6)) {
            val upper = line.uppercase()
            if (upper in skipHeaderWords) continue
            if (upper.startsWith("DATE") || upper.startsWith("TEL") || upper.startsWith("HTTP")) continue
            if (line.count { it.isLetter() } < 3) continue
            if (Regex("""^\d+\s+.*(?:ST|AVE|BLVD|RD|DR|LN|WAY)\b""", RegexOption.IGNORE_CASE).containsMatchIn(line)) continue
            return line.replace(Regex("""#\d+"""), "").trim().ifEmpty { line }
        }
        return null
    }

    private fun extractPriceCents(lines: List<String>, fullText: String): Long? {
        for (line in lines.reversed()) {
            val match = totalLineRegex.find(line)
            if (match != null) {
                parseDollarToCents(match.groupValues[2])?.let { return it }
            }
        }
        val dollarMatches = currencyRegex.findAll(fullText)
            .mapNotNull { parseDollarToCents(it.groupValues[1]) }
            .toList()
        if (dollarMatches.isNotEmpty()) return dollarMatches.maxOrNull()

        val decimalMatches = decimalPriceRegex.findAll(fullText)
            .mapNotNull { parseDollarToCents(it.groupValues[1]) }
            .filter { it in 100L..5_000_000L }
            .toList()
        return decimalMatches.maxOrNull()
    }

    private fun parseDollarToCents(raw: String): Long? {
        val cleaned = raw.replace("$", "").replace(",", "").trim()
        val value = cleaned.toDoubleOrNull() ?: return null
        return (value * 100.0).roundToLong()
    }

    private fun extractDate(text: String): Long? {
        isoDateRegex.find(text)?.let { m ->
            val y = m.groupValues[1].toIntOrNull()
            val mo = m.groupValues[2].toIntOrNull()
            val d = m.groupValues[3].toIntOrNull()
            if (y != null && mo in 1..12 && d in 1..31) {
                return VaultDate.fromYmd(y, mo!!, d!!)
            }
        }
        usDateRegex.find(text)?.let { m ->
            val mo = m.groupValues[1].toIntOrNull()
            val d = m.groupValues[2].toIntOrNull()
            val rawY = m.groupValues[3].toIntOrNull()
            val y = if (rawY != null && rawY < 100) 2000 + rawY else rawY
            if (y != null && mo in 1..12 && d in 1..31) {
                return VaultDate.fromYmd(y, mo!!, d!!)
            }
        }
        textMonthDateRegex.find(text)?.let { m ->
            val mon = when (m.groupValues[1].lowercase().take(3)) {
                "jan" -> 1
                "feb" -> 2
                "mar" -> 3
                "apr" -> 4
                "may" -> 5
                "jun" -> 6
                "jul" -> 7
                "aug" -> 8
                "sep" -> 9
                "oct" -> 10
                "nov" -> 11
                "dec" -> 12
                else -> null
            }
            val d = m.groupValues[2].toIntOrNull()
            val y = m.groupValues[3].toIntOrNull()
            if (mon != null && d in 1..31 && y != null) {
                return VaultDate.fromYmd(y, mon, d!!)
            }
        }
        return null
    }

    private fun extractSerial(text: String, barcodes: List<String>): String? {
        serialRegex.find(text)?.let { m ->
            val candidate = m.groupValues[1].trim()
            if (candidate.length >= 6) return candidate
        }
        return barcodes.firstOrNull { it.length in 6..32 }
    }

    private fun extractItemTitle(lines: List<String>, storeName: String?): String? {
        val ignoredPrefixes = listOf(
            "DATE", "TIME", "SUBTOTAL", "TAX", "TOTAL", "VISA", "MASTERCARD", "AMEX",
            "DISCOVER", "DEBIT", "CREDIT", "CARD", "PAYMENT", "CASH", "CHANGE", "THANK",
            "RETURN", "WARRANTY", "S/N", "SN:", "SERIAL", "CATEGORY", "ITEM:",
        )
        for (line in lines) {
            if (storeName != null && line.contains(storeName, ignoreCase = true)) continue
            val upper = line.uppercase()
            if (upper.startsWith("ITEM:")) {
                val stripped = line.substringAfter(":").trim()
                if (stripped.length >= 3) return stripped
            }
            if (ignoredPrefixes.any { upper.startsWith(it) }) continue
            if (line.count { it.isLetter() } < 4) continue
            if (line.length in 4..60) {
                return line.replace(currencyRegex, "").trim().ifEmpty { line }
            }
        }
        return null
    }
}
