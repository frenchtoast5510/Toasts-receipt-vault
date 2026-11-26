package com.frenchtoast.receiptvault.data.storage

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ImportedDocument(
    val storedFileName: String,
    val displayName: String,
    val mimeType: String,
)

class VaultImageStorage(private val context: Context) {

    val imagesDir: File
        get() = File(context.filesDir, "vault_images").apply { mkdirs() }

    val docsDir: File
        get() = File(context.filesDir, "vault_docs").apply { mkdirs() }

    private val cameraTempDir: File
        get() = File(context.cacheDir, "camera_captures").apply { mkdirs() }

    fun resolveImageFile(fileName: String?): File? {
        if (fileName.isNullOrBlank()) return null
        val sanitized = File(fileName).name
        val target = File(imagesDir, sanitized)
        return target.takeIf { it.exists() }
    }

    fun resolveDocumentFile(fileName: String?): File? {
        if (fileName.isNullOrBlank()) return null
        val sanitized = File(fileName).name
        val target = File(docsDir, sanitized)
        return target.takeIf { it.exists() }
    }

    fun getFileProviderUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun createCameraCaptureUri(): Pair<File, Uri> {
        val file = File(cameraTempDir, "capture_${UUID.randomUUID()}.jpg")
        val uri = getFileProviderUri(file)
        return file to uri
    }

    fun importUriAsJpeg(uri: Uri, prefix: String): String? {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            val sampleSize = if (maxDim > 2048) {
                var s = 1
                while (maxDim / s > 2048) s *= 2
                s
            } else {
                1
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val scaled = downscaleIfNeeded(bitmap, 2048)
            val fileName = "${prefix}_${UUID.randomUUID()}.jpg"
            val outFile = File(imagesDir, fileName)
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            fileName
        }.onFailure {
            Log.e("VaultImageStorage", "Failed to import image URI", it)
        }.getOrNull()
    }

    fun importDocumentUri(uri: Uri): ImportedDocument? {
        return runCatching {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }

            val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"
            val displayName = queryDisplayName(uri) ?: "document.pdf"
            val ext = displayName.substringAfterLast('.', "").lowercase()
                .takeIf { it.isNotBlank() && it.length <= 6 }
                ?: if (mimeType.contains("pdf")) "pdf" else "bin"

            val storedName = "doc_${UUID.randomUUID()}.$ext"
            val outFile = File(docsDir, storedName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            ImportedDocument(
                storedFileName = storedName,
                displayName = displayName,
                mimeType = mimeType,
            )
        }.onFailure {
            Log.e("VaultImageStorage", "Failed to import document URI", it)
        }.getOrNull()
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else {
                        null
                    }
                }
        }.getOrNull()
    }

    fun getPdfPageCount(file: File): Int {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer -> renderer.pageCount }
            }
        }.getOrDefault(0)
    }

    fun renderPdfPageBitmap(file: File, pageIndex: Int = 0, targetWidth: Int = 1200): Bitmap? {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0 || pageIndex !in 0 until renderer.pageCount) {
                        return null
                    }
                    renderer.openPage(pageIndex).use { page ->
                        val scale = targetWidth.toFloat() / page.width.coerceAtLeast(1).toFloat()
                        val w = targetWidth.coerceAtLeast(1)
                        val h = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }.getOrNull()
    }

    fun saveBitmapAsPng(bitmap: Bitmap, fileName: String): String {
        val sanitized = File(fileName).name
        val outFile = File(imagesDir, sanitized)
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return sanitized
    }

    fun saveBytes(fileName: String, bytes: ByteArray): String {
        val sanitized = File(fileName).name
        val outFile = File(imagesDir, sanitized)
        outFile.writeBytes(bytes)
        return sanitized
    }

    fun saveDocBytes(fileName: String, bytes: ByteArray): String {
        val sanitized = File(fileName).name
        val outFile = File(docsDir, sanitized)
        outFile.writeBytes(bytes)
        return sanitized
    }

    fun deleteImage(fileName: String?) {
        if (fileName.isNullOrBlank()) return
        File(imagesDir, File(fileName).name).delete()
    }

    fun deleteDocument(fileName: String?) {
        if (fileName.isNullOrBlank()) return
        File(docsDir, File(fileName).name).delete()
    }

    fun pruneUnreferenced(referencedImages: Set<String>, referencedDocs: Set<String>) {
        val keepImages = referencedImages.map { File(it).name }.toSet()
        imagesDir.listFiles()?.forEach { file ->
            if (file.name !in keepImages) file.delete()
        }
        val keepDocs = referencedDocs.map { File(it).name }.toSet()
        docsDir.listFiles()?.forEach { file ->
            if (file.name !in keepDocs) file.delete()
        }
    }

    fun totalStorageBytes(): Long {
        val imgBytes = imagesDir.listFiles()?.sumOf { it.length() } ?: 0L
        val docBytes = docsDir.listFiles()?.sumOf { it.length() } ?: 0L
        return imgBytes + docBytes
    }

    private fun downscaleIfNeeded(source: Bitmap, maxEdge: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxEdge && h <= maxEdge) return source
        val scale = maxEdge.toFloat() / maxOf(w, h).toFloat()
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(source, 0, 0, w, h, matrix, true)
    }
}
