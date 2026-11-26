package com.frenchtoast.receiptvault.ocr

data class MaskResult(
    val maskedText: String,
    val maskedCount: Int,
)

object CardMasker {

    private val cardCandidateRegex = Regex("""(?<![A-Za-z0-9])(?:\d[\s-]?){13,19}(?![A-Za-z0-9])""")
    private val nonCardLabelRegex = Regex(
        """(?i)\b(s/?n|serial|imei|upc|ean|gtin|sku|model|order|inv(?:oice)?|ref|tel|phone|fax|tracking)\s*[:#-]?\s*$"""
    )
    private val cardContextRegex = Regex(
        """(?i)\b(visa|mastercard|mc|amex|american express|discover|debit|credit|card|acct|account|auth|pan|payment)\b"""
    )

    fun maskCardNumbers(input: String): MaskResult {
        if (input.isBlank()) return MaskResult(input, 0)
        var count = 0
        val lines = input.lines()
        val processedLines = lines.map { line ->
            val hasCardContext = cardContextRegex.containsMatchIn(line)
            cardCandidateRegex.replace(line) { match ->
                val raw = match.value
                val digits = raw.filter { it.isDigit() }
                if (digits.length !in 13..19) {
                    return@replace raw
                }
                val prefixBefore = line.substring(0, match.range.first)
                if (!hasCardContext && nonCardLabelRegex.containsMatchIn(prefixBefore)) {
                    return@replace raw
                }
                val hasGroupedSeparators = raw.contains(' ') || raw.contains('-')
                val shouldMask = passesLuhn(digits) || (hasCardContext && hasGroupedSeparators) || hasStandardCardGrouping(raw)
                if (!shouldMask) {
                    return@replace raw
                }
                count++
                val last4 = digits.takeLast(4)
                if (digits.length >= 16) "•••• •••• •••• $last4" else "•••• •••••• •$last4"
            }
        }
        return MaskResult(processedLines.joinToString("\n"), count)
    }

    private fun hasStandardCardGrouping(raw: String): Boolean {
        val trimmed = raw.trim()
        val parts = trimmed.split(Regex("""[\s-]+"""))
        return (parts.size == 4 && parts.all { it.length == 4 }) ||
            (parts.size == 3 && parts[0].length == 4 && parts[1].length == 6 && parts[2].length == 5)
    }

    internal fun passesLuhn(digits: String): Boolean {
        if (digits.length !in 13..19 || digits.all { it == digits[0] }) return false
        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}
