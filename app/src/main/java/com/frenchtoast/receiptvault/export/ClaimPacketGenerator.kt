package com.frenchtoast.receiptvault.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.frenchtoast.receiptvault.data.storage.VaultImageStorage
import com.frenchtoast.receiptvault.model.EntryType
import com.frenchtoast.receiptvault.model.VaultDate
import com.frenchtoast.receiptvault.model.VaultItem
import java.io.File
import java.io.FileOutputStream

data class OriginalShareFile(
    val uri: Uri,
    val mimeType: String,
    val label: String,
)

class ClaimPacketGenerator(
    private val context: Context,
    private val imageStorage: VaultImageStorage,
) {

    fun buildTextSummary(item: VaultItem): String = buildString {
        appendLine("TOASTS RECEIPT VAULT — ${item.entryType.label.uppercase()} SUMMARY")
        appendLine("Title: ${item.title}")
        appendLine("${if (item.entryType == EntryType.DOCUMENT) "Issuer" else "Store / Issuer"}: ${item.storeName}")
        appendLine("Category: ${item.category.label}")
        appendLine("Date: ${VaultDate.formatDisplay(item.purchaseDateEpochDay)}")
        if (item.priceCents > 0L) {
            appendLine("Total Paid: ${item.formattedPrice()}")
        }
        if (item.serialNumber.isNotBlank()) {
            appendLine("Serial / Reference: ${item.serialNumber}")
        }
        item.returnDeadlineEpochDay?.let {
            appendLine("Return Deadline: ${VaultDate.formatDisplay(it)}")
        }
        item.warrantyExpirationEpochDay?.let {
            val label = if (item.entryType == EntryType.DOCUMENT) "Expiration Date" else "Warranty Expires"
            appendLine("$label: ${VaultDate.formatDisplay(it)}")
        }
        if (!item.documentDisplayName.isNullOrBlank()) {
            appendLine("Attached Document: ${item.documentDisplayName}")
        }
        if (item.notes.isNotBlank()) {
            appendLine()
            appendLine("Notes:")
            appendLine(item.notes)
        }
    }.trim()

    fun getOriginalShareFiles(item: VaultItem): List<OriginalShareFile> {
        val out = mutableListOf<OriginalShareFile>()
        imageStorage.resolveImageFile(item.receiptImagePath)?.let { file ->
            out.add(
                OriginalShareFile(
                    uri = imageStorage.getFileProviderUri(file),
                    mimeType = mimeTypeForFile(file),
                    label = file.name,
                ),
            )
        }
        imageStorage.resolveImageFile(item.serialImagePath)?.let { file ->
            out.add(
                OriginalShareFile(
                    uri = imageStorage.getFileProviderUri(file),
                    mimeType = mimeTypeForFile(file),
                    label = file.name,
                ),
            )
        }
        imageStorage.resolveDocumentFile(item.documentPath)?.let { file ->
            out.add(
                OriginalShareFile(
                    uri = imageStorage.getFileProviderUri(file),
                    mimeType = item.documentMimeType ?: mimeTypeForFile(file),
                    label = item.documentDisplayName ?: file.name,
                ),
            )
        }
        return out
    }

    fun buildShareSummaryIntent(item: VaultItem): Intent {
        val summary = buildTextSummary(item)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${item.entryType.label}: ${item.title}")
            putExtra(Intent.EXTRA_TEXT, summary)
        }
        return Intent.createChooser(sendIntent, "Share Item Summary")
    }

    fun buildShareOriginalFilesIntent(item: VaultItem): Intent? {
        val files = getOriginalShareFiles(item)
        if (files.isEmpty()) return null

        val summary = buildTextSummary(item)
        return if (files.size == 1) {
            val single = files.first()
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = single.mimeType
                putExtra(Intent.EXTRA_STREAM, single.uri)
                putExtra(Intent.EXTRA_SUBJECT, "${item.title} (${single.label})")
                putExtra(Intent.EXTRA_TEXT, summary)
                clipData = ClipData.newUri(context.contentResolver, single.label, single.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(sendIntent, "Share Original File")
        } else {
            val uris = ArrayList(files.map { it.uri })
            val distinctTypes = files.map { it.mimeType }.distinct()
            val commonMime = if (distinctTypes.size == 1) {
                distinctTypes.first()
            } else if (distinctTypes.all { it.startsWith("image/") }) {
                "image/*"
            } else {
                "*/*"
            }
            val clip = ClipData.newUri(context.contentResolver, files.first().label, files.first().uri)
            files.drop(1).forEach { clip.addItem(ClipData.Item(it.uri)) }

            val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = commonMime
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "${item.title} — Vault Files")
                putExtra(Intent.EXTRA_TEXT, summary)
                clipData = clip
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(sendIntent, "Share Original Files")
        }
    }

    fun generateClaimPdfUri(item: VaultItem): Uri {
        val outDir = File(context.cacheDir, "claim_packets").apply { mkdirs() }
        val safeName = item.title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(32)
            .ifEmpty { "item" }
        val pdfFile = File(outDir, "${safeName}_claim_packet.pdf")

        val document = PdfDocument()
        val pageWidth = 612
        val pageHeight = 792

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 26, 22)
            textSize = 19f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 62, 26)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 100, 92)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 26, 22)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 26, 22)
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(220, 210, 198)
            strokeWidth = 1.2f
        }

        val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        var y = 48f
        canvas.drawText("TOASTS RECEIPT VAULT • ${item.entryType.label.uppercase()} PACKET", 44f, y, subtitlePaint)
        y += 24f
        canvas.drawText(item.title.take(56), 44f, y, titlePaint)
        y += 14f
        canvas.drawLine(44f, y, pageWidth - 44f, y, dividerPaint)
        y += 22f

        val rows = buildList {
            add((if (item.entryType == EntryType.DOCUMENT) "ISSUER" else "STORE / ISSUER") to item.storeName)
            add("ENTRY TYPE" to "${item.entryType.label} (${item.category.label})")
            add("DATE" to VaultDate.formatDisplay(item.purchaseDateEpochDay))
            if (item.priceCents > 0L) add("TOTAL PAID" to item.formattedPrice())
            if (item.serialNumber.isNotBlank()) add("SERIAL / REF NO." to item.serialNumber)
            item.returnDeadlineEpochDay?.let { add("RETURN DEADLINE" to VaultDate.formatDisplay(it)) }
            item.warrantyExpirationEpochDay?.let {
                val lbl = if (item.entryType == EntryType.DOCUMENT) "EXPIRATION" else "WARRANTY EXPIRES"
                add(lbl to VaultDate.formatDisplay(it))
            }
            if (!item.documentDisplayName.isNullOrBlank()) {
                add("DOCUMENT FILE" to item.documentDisplayName)
            }
        }

        rows.forEach { (label, value) ->
            canvas.drawText(label, 44f, y, labelPaint)
            val paintToUse = if (label == "SERIAL / REF NO." || label == "TOTAL PAID") monoPaint else valuePaint
            canvas.drawText(value.take(54), 184f, y, paintToUse)
            y += 19f
        }

        if (item.notes.isNotBlank()) {
            y += 6f
            canvas.drawLine(44f, y, pageWidth - 44f, y, dividerPaint)
            y += 16f
            canvas.drawText("NOTES", 44f, y, labelPaint)
            y += 14f
            wrapLine(item.notes, 78).take(3).forEach { line ->
                canvas.drawText(line, 44f, y, valuePaint)
                y += 15f
            }
        }

        val receiptFile = imageStorage.resolveImageFile(item.receiptImagePath)
        val serialFile = imageStorage.resolveImageFile(item.serialImagePath)
        val docFile = imageStorage.resolveDocumentFile(item.documentPath)

        val primaryBitmap: Bitmap? = when {
            receiptFile != null -> decodeSampledBitmap(receiptFile)
            docFile != null && (item.documentMimeType?.contains("pdf") == true || docFile.extension.equals("pdf", true)) ->
                imageStorage.renderPdfPageBitmap(docFile, pageIndex = 0, targetWidth = 900)
            serialFile != null -> decodeSampledBitmap(serialFile)
            else -> null
        }

        if (primaryBitmap != null) {
            y += 10f
            canvas.drawLine(44f, y, pageWidth - 44f, y, dividerPaint)
            y += 16f
            val imageLabel = when {
                receiptFile != null -> "ATTACHED RECEIPT IMAGE"
                docFile != null -> "ATTACHED DOCUMENT PREVIEW (PAGE 1)"
                else -> "ATTACHED SERIAL LABEL"
            }
            canvas.drawText(imageLabel, 44f, y, labelPaint)
            y += 8f
            drawBitmapFit(canvas, primaryBitmap, RectF(44f, y, pageWidth - 44f, pageHeight - 36f))
            primaryBitmap.recycle()
        }

        document.finishPage(page)

        FileOutputStream(pdfFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return imageStorage.getFileProviderUri(pdfFile)
    }

    fun buildSharePacketPdfIntent(item: VaultItem): Intent {
        val pdfUri = generateClaimPdfUri(item)
        val summary = buildTextSummary(item)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "Claim / Summary Packet: ${item.title}")
            putExtra(Intent.EXTRA_TEXT, summary)
            clipData = ClipData.newUri(context.contentResolver, "Claim Packet PDF", pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share PDF Packet")
    }

    private fun decodeSampledBitmap(file: File): Bitmap? {
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun drawBitmapFit(canvas: Canvas, bmp: Bitmap, bounds: RectF) {
        val availW = bounds.width()
        val availH = bounds.height()
        if (availW <= 0f || availH <= 0f) return
        val scale = minOf(availW / bmp.width.toFloat(), availH / bmp.height.toFloat(), 1f)
        val drawW = bmp.width * scale
        val drawH = bmp.height * scale
        val left = bounds.left + (availW - drawW) / 2f
        val top = bounds.top
        canvas.drawBitmap(bmp, null, RectF(left, top, left + drawW, top + drawH), null)
    }

    private fun mimeTypeForFile(file: File): String = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        else -> "application/octet-stream"
    }

    private fun wrapLine(text: String, maxChars: Int): List<String> {
        val words = text.split(Regex("\\s+"))
        val out = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            if (current.isEmpty()) {
                current.append(w)
            } else if (current.length + 1 + w.length <= maxChars) {
                current.append(' ').append(w)
            } else {
                out.add(current.toString())
                current = StringBuilder(w)
            }
        }
        if (current.isNotEmpty()) out.add(current.toString())
        return out
    }
}
