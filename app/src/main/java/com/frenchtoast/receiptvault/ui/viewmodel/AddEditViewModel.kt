package com.frenchtoast.receiptvault.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.frenchtoast.receiptvault.data.VaultRepository
import com.frenchtoast.receiptvault.data.storage.VaultImageStorage
import com.frenchtoast.receiptvault.model.EntryType
import com.frenchtoast.receiptvault.model.ItemCategory
import com.frenchtoast.receiptvault.model.VaultDate
import com.frenchtoast.receiptvault.model.VaultItem
import com.frenchtoast.receiptvault.ocr.CardMasker
import com.frenchtoast.receiptvault.ocr.OnDeviceScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

enum class CaptureTarget {
    RECEIPT,
    SERIAL_LABEL,
}

data class AddEditUiState(
    val editingItemId: String? = null,
    val entryType: EntryType = EntryType.RECEIPT,
    val title: String = "",
    val storeName: String = "",
    val category: ItemCategory = ItemCategory.ELECTRONICS,
    val purchaseDateEpochDay: Long = VaultDate.todayEpochDay(),
    val priceInput: String = "",
    val serialNumber: String = "",
    val returnDeadlineEpochDay: Long? = VaultDate.plusDays(VaultDate.todayEpochDay(), 30),
    val warrantyExpirationEpochDay: Long? = VaultDate.plusYears(VaultDate.todayEpochDay(), 1),
    val notes: String = "",
    val rawOcrText: String = "",
    val receiptImagePath: String? = null,
    val serialImagePath: String? = null,
    val documentPath: String? = null,
    val documentDisplayName: String? = null,
    val documentMimeType: String? = null,
    val isScanning: Boolean = false,
    val cardMaskedNotice: Boolean = false,
    val detectedBarcodes: List<String> = emptyList(),
    val scanStatusMessage: String? = null,
    val validationError: String? = null,
    val savedSuccessfully: Boolean = false,
    val isExistingDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

class AddEditViewModel(
    private val itemId: String?,
    private val repository: VaultRepository,
    private val imageStorage: VaultImageStorage,
    private val scanner: OnDeviceScanner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState(editingItemId = itemId))
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (itemId != null) {
            viewModelScope.launch {
                val existing = repository.getItem(itemId) ?: return@launch
                _uiState.update {
                    it.copy(
                        editingItemId = existing.id,
                        entryType = existing.entryType,
                        title = existing.title,
                        storeName = existing.storeName,
                        category = existing.category,
                        purchaseDateEpochDay = existing.purchaseDateEpochDay,
                        priceInput = if (existing.priceCents > 0L) {
                            String.format(Locale.US, "%.2f", existing.priceCents / 100.0)
                        } else {
                            ""
                        },
                        serialNumber = existing.serialNumber,
                        returnDeadlineEpochDay = existing.returnDeadlineEpochDay,
                        warrantyExpirationEpochDay = existing.warrantyExpirationEpochDay,
                        notes = existing.notes,
                        rawOcrText = existing.rawOcrText,
                        receiptImagePath = existing.receiptImagePath,
                        serialImagePath = existing.serialImagePath,
                        documentPath = existing.documentPath,
                        documentDisplayName = existing.documentDisplayName,
                        documentMimeType = existing.documentMimeType,
                        isExistingDemo = existing.isDemo,
                        createdAtMillis = existing.createdAtMillis,
                    )
                }
            }
        }
    }

    fun updateEntryType(type: EntryType) {
        _uiState.update { current ->
            val updatedReturn = if (current.editingItemId == null && type == EntryType.DOCUMENT) {
                null
            } else if (current.editingItemId == null && type == EntryType.RECEIPT && current.returnDeadlineEpochDay == null) {
                VaultDate.plusDays(current.purchaseDateEpochDay, 30)
            } else {
                current.returnDeadlineEpochDay
            }
            current.copy(entryType = type, returnDeadlineEpochDay = updatedReturn)
        }
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, validationError = null) }
    fun updateStoreName(value: String) = _uiState.update { it.copy(storeName = value, validationError = null) }
    fun updateCategory(value: ItemCategory) = _uiState.update { it.copy(category = value) }
    fun updatePurchaseDate(epochDay: Long) = _uiState.update { it.copy(purchaseDateEpochDay = epochDay) }
    fun updatePriceInput(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(priceInput = filtered) }
    }
    fun updateSerialNumber(value: String) = _uiState.update { it.copy(serialNumber = value) }
    fun updateReturnDeadline(epochDay: Long?) = _uiState.update { it.copy(returnDeadlineEpochDay = epochDay) }
    fun updateWarrantyExpiration(epochDay: Long?) = _uiState.update { it.copy(warrantyExpirationEpochDay = epochDay) }
    fun updateNotes(value: String) = _uiState.update { it.copy(notes = value) }
    fun updateRawOcrText(value: String) {
        val masked = CardMasker.maskCardNumbers(value)
        _uiState.update {
            it.copy(
                rawOcrText = masked.maskedText,
                cardMaskedNotice = it.cardMaskedNotice || masked.maskedCount > 0,
            )
        }
    }

    fun removeReceiptImage() = _uiState.update { it.copy(receiptImagePath = null) }
    fun removeSerialImage() = _uiState.update { it.copy(serialImagePath = null) }
    fun removeDocument() = _uiState.update {
        it.copy(documentPath = null, documentDisplayName = null, documentMimeType = null)
    }
    fun clearScanMessage() = _uiState.update { it.copy(scanStatusMessage = null) }

    fun onDocumentSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanStatusMessage = null) }
            val imported = withContext(Dispatchers.IO) {
                imageStorage.importDocumentUri(uri)
            }
            if (imported == null) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanStatusMessage = "Could not copy selected document.",
                    )
                }
                return@launch
            }
            _uiState.update { current ->
                val suggestedTitle = if (current.title.isBlank()) {
                    imported.displayName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ')
                } else {
                    current.title
                }
                current.copy(
                    title = suggestedTitle,
                    documentPath = imported.storedFileName,
                    documentDisplayName = imported.displayName,
                    documentMimeType = imported.mimeType,
                    isScanning = false,
                    scanStatusMessage = "Document copied locally: ${imported.displayName}",
                )
            }
        }
    }

    fun onImageSelected(uri: Uri, target: CaptureTarget) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanStatusMessage = null) }
            val prefix = if (target == CaptureTarget.RECEIPT) "receipt" else "serial"
            val storedFileName = withContext(Dispatchers.IO) {
                imageStorage.importUriAsJpeg(uri, prefix)
            }
            if (storedFileName == null) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanStatusMessage = "Could not import image. Try another photo.",
                    )
                }
                return@launch
            }

            _uiState.update { state ->
                when (target) {
                    CaptureTarget.RECEIPT -> state.copy(receiptImagePath = storedFileName)
                    CaptureTarget.SERIAL_LABEL -> state.copy(serialImagePath = storedFileName)
                }
            }

            val extraction = runCatching { scanner.analyzeImageUri(uri) }.getOrNull()
            if (extraction == null) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanStatusMessage = "Photo attached. Enter fields manually if needed.",
                    )
                }
                return@launch
            }

            val parsed = extraction.parsed
            _uiState.update { current ->
                val mergedOcr = when {
                    parsed.maskedText.isBlank() -> current.rawOcrText
                    current.rawOcrText.isBlank() -> parsed.maskedText
                    else -> "${current.rawOcrText}\n---\n${parsed.maskedText}"
                }

                val newSerial = if (current.serialNumber.isBlank() && !parsed.serialNumber.isNullOrBlank()) {
                    parsed.serialNumber
                } else if (target == CaptureTarget.SERIAL_LABEL && !parsed.serialNumber.isNullOrBlank()) {
                    parsed.serialNumber
                } else {
                    current.serialNumber
                }

                val newPrice = if (current.priceInput.isBlank() && parsed.priceCents != null) {
                    String.format(Locale.US, "%.2f", parsed.priceCents / 100.0)
                } else {
                    current.priceInput
                }

                val newStore = if (current.storeName.isBlank() && !parsed.storeName.isNullOrBlank()) {
                    parsed.storeName
                } else {
                    current.storeName
                }

                val newTitle = if (current.title.isBlank() && !parsed.itemTitle.isNullOrBlank()) {
                    parsed.itemTitle
                } else {
                    current.title
                }

                val newPurchaseDate = parsed.purchaseDateEpochDay ?: current.purchaseDateEpochDay
                val newReturn = parsed.returnDeadlineEpochDay ?: current.returnDeadlineEpochDay
                val newWarranty = parsed.warrantyExpirationEpochDay ?: current.warrantyExpirationEpochDay
                val allBarcodes = (current.detectedBarcodes + extraction.barcodes).distinct()

                val statusSummary = buildString {
                    append("On-device scan complete")
                    if (parsed.didMaskCard) append(" • Card masked to last 4 digits")
                    if (extraction.barcodes.isNotEmpty()) append(" • ${extraction.barcodes.size} barcode(s)")
                }

                current.copy(
                    title = newTitle,
                    storeName = newStore,
                    priceInput = newPrice,
                    purchaseDateEpochDay = newPurchaseDate,
                    serialNumber = newSerial,
                    returnDeadlineEpochDay = newReturn,
                    warrantyExpirationEpochDay = newWarranty,
                    rawOcrText = mergedOcr,
                    cardMaskedNotice = current.cardMaskedNotice || parsed.didMaskCard,
                    detectedBarcodes = allBarcodes,
                    isScanning = false,
                    scanStatusMessage = statusSummary,
                )
            }
        }
    }

    fun applyDetectedBarcodeAsSerial(barcode: String) {
        _uiState.update { it.copy(serialNumber = barcode) }
    }

    fun saveItem() {
        val state = _uiState.value
        val title = state.title.trim()
        val store = state.storeName.trim()
        if (title.isEmpty()) {
            _uiState.update { it.copy(validationError = "Please enter a title.") }
            return
        }
        if (store.isEmpty()) {
            val fieldLabel = if (state.entryType == EntryType.DOCUMENT) "issuer or organization" else "store or merchant"
            _uiState.update { it.copy(validationError = "Please enter a $fieldLabel name.") }
            return
        }

        val priceCents = state.priceInput.toDoubleOrNull()?.let { (it * 100.0).roundToLong() } ?: 0L
        val safeOcr = CardMasker.maskCardNumbers(state.rawOcrText).maskedText
        val now = System.currentTimeMillis()

        val item = VaultItem(
            id = state.editingItemId ?: UUID.randomUUID().toString(),
            entryType = state.entryType,
            title = title,
            storeName = store,
            category = state.category,
            purchaseDateEpochDay = state.purchaseDateEpochDay,
            priceCents = priceCents,
            currencyCode = "USD",
            serialNumber = state.serialNumber.trim(),
            returnDeadlineEpochDay = state.returnDeadlineEpochDay,
            warrantyExpirationEpochDay = state.warrantyExpirationEpochDay,
            notes = state.notes.trim(),
            rawOcrText = safeOcr,
            receiptImagePath = state.receiptImagePath,
            serialImagePath = state.serialImagePath,
            documentPath = state.documentPath,
            documentDisplayName = state.documentDisplayName,
            documentMimeType = state.documentMimeType,
            isDemo = if (state.editingItemId != null) state.isExistingDemo else false,
            createdAtMillis = state.createdAtMillis,
            updatedAtMillis = now,
        )

        viewModelScope.launch {
            repository.saveItem(item)
            _uiState.update { it.copy(savedSuccessfully = true) }
        }
    }

    class Factory(
        private val itemId: String?,
        private val repository: VaultRepository,
        private val imageStorage: VaultImageStorage,
        private val scanner: OnDeviceScanner,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddEditViewModel(itemId, repository, imageStorage, scanner) as T
        }
    }
}
