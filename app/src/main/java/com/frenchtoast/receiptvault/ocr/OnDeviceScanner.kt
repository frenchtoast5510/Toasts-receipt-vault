package com.frenchtoast.receiptvault.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class ScanExtractionOutput(
    val rawMaskedText: String,
    val barcodes: List<String>,
    val parsed: ParsedReceiptResult,
)

class OnDeviceScanner(private val context: Context) {

    suspend fun analyzeImageUri(uri: Uri): ScanExtractionOutput = withContext(Dispatchers.IO) {
        val inputImage = InputImage.fromFilePath(context, uri)
        val ocrText = runTextRecognition(inputImage)
        val barcodes = runBarcodeScanning(inputImage)
        val parsed = ReceiptParser.parse(ocrText, barcodes)
        ScanExtractionOutput(
            rawMaskedText = parsed.maskedText,
            barcodes = barcodes,
            parsed = parsed,
        )
    }

    private suspend fun runTextRecognition(image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    recognizer.close()
                    if (cont.isActive) cont.resume(result.text)
                }
                .addOnFailureListener {
                    recognizer.close()
                    if (cont.isActive) cont.resume("")
                }
        }

    private suspend fun runBarcodeScanning(image: InputImage): List<String> =
        suspendCancellableCoroutine { cont ->
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    scanner.close()
                    val values = barcodes.mapNotNull { it.rawValue?.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    if (cont.isActive) cont.resume(values)
                }
                .addOnFailureListener {
                    scanner.close()
                    if (cont.isActive) cont.resume(emptyList())
                }
        }
}
