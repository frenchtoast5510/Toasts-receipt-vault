package com.frenchtoast.receiptvault.data.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.frenchtoast.receiptvault.data.storage.VaultImageStorage
import com.frenchtoast.receiptvault.model.EntryType
import com.frenchtoast.receiptvault.model.ItemCategory
import com.frenchtoast.receiptvault.model.VaultDate
import com.frenchtoast.receiptvault.model.VaultItem
import java.io.ByteArrayOutputStream

class DemoDataSeeder(private val imageStorage: VaultImageStorage) {

    fun buildSampleItems(todayEpochDay: Long = VaultDate.todayEpochDay()): List<VaultItem> {
        val now = System.currentTimeMillis()
        return listOf(
            VaultItem(
                id = "demo-sony-xm5",
                entryType = EntryType.RECEIPT,
                title = "Sony WH-1000XM5 Wireless Headphones",
                storeName = "Best Buy",
                category = ItemCategory.ELECTRONICS,
                purchaseDateEpochDay = VaultDate.plusDays(todayEpochDay, -10),
                priceCents = 34999,
                serialNumber = "SN-XM5-88421094A",
                returnDeadlineEpochDay = VaultDate.plusDays(todayEpochDay, 4),
                warrantyExpirationEpochDay = VaultDate.plusDays(todayEpochDay, 355),
                notes = "Midnight Blue. Extended return window active; original box kept in closet.",
                rawOcrText = buildSampleOcr(
                    store = "BEST BUY #0412",
                    item = "SONY WH1000XM5 BLU",
                    price = "$349.99",
                    date = VaultDate.formatIso(VaultDate.plusDays(todayEpochDay, -10)),
                    serial = "SN-XM5-88421094A",
                    cardLast4 = "4821",
                ),
                receiptImagePath = "demo_receipt_xm5.png",
                serialImagePath = "demo_serial_xm5.png",
                isDemo = true,
                createdAtMillis = now - 6_000,
                updatedAtMillis = now - 6_000,
            ),
            VaultItem(
                id = "demo-breville-espresso",
                entryType = EntryType.WARRANTY,
                title = "Breville Bambino Plus Espresso Machine",
                storeName = "Breville USA",
                category = ItemCategory.APPLIANCES,
                purchaseDateEpochDay = VaultDate.plusDays(todayEpochDay, -12),
                priceCents = 49995,
                serialNumber = "BRV-2410-902184",
                returnDeadlineEpochDay = VaultDate.plusDays(todayEpochDay, 18),
                warrantyExpirationEpochDay = VaultDate.plusYears(
                    VaultDate.plusDays(todayEpochDay, -12),
                    2,
                ),
                notes = "Brushed Stainless Steel. 2-year manufacturer warranty registration recorded.",
                rawOcrText = buildSampleOcr(
                    store = "WILLIAMS SONOMA",
                    item = "BREVILLE BAMBINO PLUS",
                    price = "$499.95",
                    date = VaultDate.formatIso(VaultDate.plusDays(todayEpochDay, -12)),
                    serial = "BRV-2410-902184",
                    cardLast4 = "9014",
                ),
                receiptImagePath = "demo_receipt_breville.png",
                serialImagePath = "demo_serial_breville.png",
                isDemo = true,
                createdAtMillis = now - 5_000,
                updatedAtMillis = now - 5_000,
            ),
            VaultItem(
                id = "demo-appliance-policy",
                entryType = EntryType.DOCUMENT,
                title = "Home Appliance Protection Policy",
                storeName = "Pacific Mutual Coverage",
                category = ItemCategory.APPLIANCES,
                purchaseDateEpochDay = VaultDate.plusDays(todayEpochDay, -180),
                priceCents = 24000,
                serialNumber = "POL-PM-2026-8814",
                returnDeadlineEpochDay = null,
                warrantyExpirationEpochDay = VaultDate.plusDays(todayEpochDay, 185),
                notes = "Annual kitchen & workshop equipment coverage certificate and claim schedule.",
                rawOcrText = "",
                receiptImagePath = null,
                serialImagePath = null,
                documentPath = "demo_appliance_policy.pdf",
                documentDisplayName = "Home_Appliance_Policy_2026.pdf",
                documentMimeType = "application/pdf",
                isDemo = true,
                createdAtMillis = now - 4_000,
                updatedAtMillis = now - 4_000,
            ),
            VaultItem(
                id = "demo-dewalt-drill",
                entryType = EntryType.WARRANTY,
                title = "DeWalt 20V MAX XR Brushless Drill Kit",
                storeName = "Home Depot",
                category = ItemCategory.TOOLS,
                purchaseDateEpochDay = VaultDate.plusDays(todayEpochDay, -345),
                priceCents = 19900,
                serialNumber = "DW-DCD800-77319",
                returnDeadlineEpochDay = VaultDate.plusDays(todayEpochDay, -255),
                warrantyExpirationEpochDay = VaultDate.plusDays(todayEpochDay, 20),
                notes = "Includes 2x 2.0Ah batteries and charger. Warranty expires soon.",
                rawOcrText = buildSampleOcr(
                    store = "THE HOME DEPOT #6619",
                    item = "DEWALT 20V XR KIT",
                    price = "$199.00",
                    date = VaultDate.formatIso(VaultDate.plusDays(todayEpochDay, -345)),
                    serial = "DW-DCD800-77319",
                    cardLast4 = "3310",
                ),
                receiptImagePath = "demo_receipt_dewalt.png",
                serialImagePath = "demo_serial_dewalt.png",
                isDemo = true,
                createdAtMillis = now - 3_000,
                updatedAtMillis = now - 3_000,
            ),
            VaultItem(
                id = "demo-switch-oled",
                entryType = EntryType.RECEIPT,
                title = "Steam Deck OLED 512GB Handheld",
                storeName = "Micro Center",
                category = ItemCategory.GAMING,
                purchaseDateEpochDay = VaultDate.plusDays(todayEpochDay, -42),
                priceCents = 54900,
                serialNumber = "FVXX41200984B",
                returnDeadlineEpochDay = VaultDate.plusDays(todayEpochDay, -12),
                warrantyExpirationEpochDay = VaultDate.plusDays(todayEpochDay, 323),
                notes = "Carrying case & matte screen protector installed on day one.",
                rawOcrText = buildSampleOcr(
                    store = "MICRO CENTER",
                    item = "OLED HANDHELD 512GB",
                    price = "$549.00",
                    date = VaultDate.formatIso(VaultDate.plusDays(todayEpochDay, -42)),
                    serial = "FVXX41200984B",
                    cardLast4 = "7742",
                ),
                receiptImagePath = "demo_receipt_gaming.png",
                serialImagePath = "demo_serial_gaming.png",
                isDemo = true,
                createdAtMillis = now - 2_000,
                updatedAtMillis = now - 2_000,
            ),
            VaultItem(
                id = "demo-desk-chair",
                entryType = EntryType.DOCUMENT,
                title = "Steelcase Leap Refurbished Certificate",
                storeName = "Design Within Reach",
                category = ItemCategory.OTHER,
                purchaseDateEpochDay = VaultDate.plusDays(todayEpochDay, -420),
                priceCents = 89500,
                serialNumber = "SC-LEAP-V2-00419",
                returnDeadlineEpochDay = VaultDate.plusDays(todayEpochDay, -390),
                warrantyExpirationEpochDay = VaultDate.plusDays(todayEpochDay, -55),
                notes = "1-year certified refurbished warranty certificate; kept for insurance records.",
                rawOcrText = buildSampleOcr(
                    store = "DESIGN WITHIN REACH",
                    item = "STEELCASE LEAP V2",
                    price = "$895.00",
                    date = VaultDate.formatIso(VaultDate.plusDays(todayEpochDay, -420)),
                    serial = "SC-LEAP-V2-00419",
                    cardLast4 = "1188",
                ),
                receiptImagePath = "demo_receipt_chair.png",
                serialImagePath = null,
                isDemo = true,
                createdAtMillis = now - 1_000,
                updatedAtMillis = now - 1_000,
            ),
        )
    }

    fun generateAndStoreDemoImages(items: List<VaultItem>) {
        items.forEach { item ->
            item.receiptImagePath?.let { path ->
                val bmp = renderReceiptBitmap(item)
                imageStorage.saveBitmapAsPng(bmp, path)
                bmp.recycle()
            }
            item.serialImagePath?.let { path ->
                val bmp = renderSerialLabelBitmap(item)
                imageStorage.saveBitmapAsPng(bmp, path)
                bmp.recycle()
            }
            item.documentPath?.let { docPath ->
                val pdfBytes = renderSamplePolicyPdf(item)
                imageStorage.saveDocBytes(docPath, pdfBytes)
            }
        }
    }

    private fun renderSamplePolicyPdf(item: VaultItem): ByteArray {
        val doc = PdfDocument()
        val pageInfo1 = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val p1 = doc.startPage(pageInfo1)
        val c1 = p1.canvas
        c1.drawColor(Color.WHITE)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(122, 62, 24)
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 25, 22)
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 50, 46)
            textSize = 12.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 25, 22)
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(215, 206, 196)
            strokeWidth = 1.5f
        }

        c1.drawText("PACIFIC MUTUAL COVERAGE  •  DEMO POLICY CERTIFICATE", 48f, 64f, headerPaint)
        c1.drawText(item.title, 48f, 98f, titlePaint)
        c1.drawLine(48f, 118f, 564f, 118f, linePaint)

        var y = 152f
        val details = listOf(
            "Policy / Reference ID: ${item.serialNumber}",
            "Issuer: ${item.storeName}",
            "Effective Date: ${VaultDate.formatDisplay(item.purchaseDateEpochDay)}",
            "Expiration Date: ${item.warrantyExpirationEpochDay?.let { VaultDate.formatDisplay(it) } ?: "N/A"}",
            "Annual Premium: ${item.formattedPrice()}",
        )
        details.forEach { line ->
            c1.drawText(line, 48f, y, monoPaint)
            y += 24f
        }

        y += 18f
        c1.drawLine(48f, y, 564f, y, linePaint)
        y += 30f
        c1.drawText("COVERAGE SUMMARY", 48f, y, headerPaint)
        y += 24f
        val paragraphs = listOf(
            "1. Covered Equipment: Countertop espresso systems, kitchen appliances, and corded workshop tools.",
            "2. Mechanical & Electrical Breakdown: Covers sudden failure due to parts or workmanship defects.",
            "3. Claims Documentation: Retain original receipt or serialized proof of ownership for inspection.",
            "4. Local Vault Copy: This sample PDF is bundled for testing Toasts Receipt Vault offline document previews.",
        )
        paragraphs.forEach { line ->
            wrapText(line, 74).forEach { wrapped ->
                c1.drawText(wrapped, 48f, y, bodyPaint)
                y += 18f
            }
            y += 10f
        }
        doc.finishPage(p1)

        val pageInfo2 = PdfDocument.PageInfo.Builder(612, 792, 2).create()
        val p2 = doc.startPage(pageInfo2)
        val c2 = p2.canvas
        c2.drawColor(Color.WHITE)
        c2.drawText("PAGE 2  •  CLAIM CHECKLIST & TERMS", 48f, 64f, headerPaint)
        c2.drawLine(48f, 82f, 564f, 82f, linePaint)
        c2.drawText("Keep serial number (${item.serialNumber}) accessible when filing a service request.", 48f, 120f, bodyPaint)
        c2.drawText("Deductible: $0.00 for manufacturer-certified repair centers.", 48f, 146f, bodyPaint)
        doc.finishPage(p2)

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun buildSampleOcr(
        store: String,
        item: String,
        price: String,
        date: String,
        serial: String,
        cardLast4: String,
    ): String = """
        $store
        DATE: $date  14:22
        ITEM: $item
        SUBTOTAL: $price
        TOTAL: $price
        VISA CREDIT •••• •••• •••• $cardLast4
        S/N: $serial
    """.trimIndent()

    private fun renderReceiptBitmap(item: VaultItem): Bitmap {
        val width = 760
        val height = 1040
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(245, 240, 230))

        val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 253, 248)
        }
        canvas.drawRoundRect(48f, 40f, width - 48f, height - 40f, 16f, 16f, paperPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(215, 206, 192)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(48f, 40f, width - 48f, height - 40f, 16f, 16f, borderPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(34, 30, 27)
            textSize = 36f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(48, 43, 39)
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val boldMono = Paint(monoPaint).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 28f
        }
        val mutedPaint = Paint(monoPaint).apply {
            color = Color.rgb(115, 106, 98)
            textSize = 20f
        }

        val cx = width / 2f
        canvas.drawText(item.storeName.uppercase(), cx, 120f, headerPaint)
        canvas.drawText("CUSTOMER COPY  •  DEMO ${item.entryType.label.uppercase()}", cx, 160f, mutedPaint.apply {
            textAlign = Paint.Align.CENTER
        })
        mutedPaint.textAlign = Paint.Align.LEFT

        val dashPaint = Paint().apply {
            color = Color.rgb(185, 176, 165)
            strokeWidth = 2f
        }
        canvas.drawLine(84f, 195f, width - 84f, 195f, dashPaint)

        var y = 250f
        canvas.drawText("DATE: ${VaultDate.formatDisplay(item.purchaseDateEpochDay)}", 84f, y, monoPaint)
        y += 42f
        canvas.drawText("CATEGORY: ${item.category.label.uppercase()}", 84f, y, monoPaint)
        y += 58f
        canvas.drawLine(84f, y, width - 84f, y, dashPaint)
        y += 56f

        val titleLines = wrapText(item.title.uppercase(), 28)
        titleLines.forEachIndexed { idx, line ->
            canvas.drawText(line, 84f, y, boldMono)
            if (idx == 0) {
                val priceStr = item.formattedPrice()
                val pw = boldMono.measureText(priceStr)
                canvas.drawText(priceStr, width - 84f - pw, y, boldMono)
            }
            y += 38f
        }

        y += 28f
        canvas.drawLine(84f, y, width - 84f, y, dashPaint)
        y += 54f
        canvas.drawText("TOTAL PAID", 84f, y, boldMono)
        val totalW = boldMono.measureText(item.formattedPrice())
        canvas.drawText(item.formattedPrice(), width - 84f - totalW, y, boldMono)

        y += 56f
        canvas.drawText("PAYMENT: CARD •••• •••• •••• 4821", 84f, y, monoPaint)
        y += 42f
        if (item.serialNumber.isNotBlank()) {
            canvas.drawText("SERIAL NO: ${item.serialNumber}", 84f, y, monoPaint)
            y += 42f
        }
        item.returnDeadlineEpochDay?.let {
            canvas.drawText("RETURN BY: ${VaultDate.formatDisplay(it)}", 84f, y, monoPaint)
            y += 40f
        }
        item.warrantyExpirationEpochDay?.let {
            canvas.drawText("WARRANTY:  ${VaultDate.formatDisplay(it)}", 84f, y, monoPaint)
            y += 40f
        }

        val barTop = height - 210f
        val barPaint = Paint().apply { color = Color.rgb(34, 30, 27) }
        var bx = 130f
        val seed = item.id.hashCode()
        var i = 0
        while (bx < width - 130f) {
            val w = if (((seed shr (i % 16)) + i) % 3 == 0) 8f else 3f
            canvas.drawRect(bx, barTop, bx + w, barTop + 95f, barPaint)
            bx += w + if (i % 2 == 0) 5f else 9f
            i++
        }
        canvas.drawText("THANK YOU FOR YOUR PURCHASE", cx, height - 76f, mutedPaint.apply {
            textAlign = Paint.Align.CENTER
        })
        return bmp
    }

    private fun renderSerialLabelBitmap(item: VaultItem): Bitmap {
        val width = 800
        val height = 460
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(232, 228, 220))

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(48f, 48f, width - 48f, height - 48f, 20f, 20f, labelPaint)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(60, 56, 52)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(48f, 48f, width - 48f, height - 48f, 20f, 20f, border)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 26, 24)
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 28, 26)
            textSize = 30f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 96, 92)
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        canvas.drawText("PRODUCT IDENTIFICATION LABEL", 86f, 108f, smallPaint)
        canvas.drawText(item.title.take(36), 86f, 152f, titlePaint)
        canvas.drawText("S/N: ${item.serialNumber}", 86f, 212f, monoPaint)

        val barPaint = Paint().apply { color = Color.BLACK }
        var bx = 86f
        val seed = item.serialNumber.hashCode()
        var idx = 0
        while (bx < width - 86f) {
            val w = if (((seed + idx * 7) and 3) == 0) 9f else 4f
            canvas.drawRect(bx, 248f, bx + w, 355f, barPaint)
            bx += w + if (idx % 3 == 0) 8f else 4f
            idx++
        }
        canvas.drawText("STORE: ${item.storeName.uppercase()}   CE  FC  RoHS", 86f, 392f, smallPaint)
        return bmp
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            if (current.isEmpty()) {
                current.append(w)
            } else if (current.length + 1 + w.length <= maxChars) {
                current.append(" ").append(w)
            } else {
                lines.add(current.toString())
                current = StringBuilder(w)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
