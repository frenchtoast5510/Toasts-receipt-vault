package com.frenchtoast.receiptvault.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.frenchtoast.receiptvault.data.storage.VaultImageStorage
import com.frenchtoast.receiptvault.model.EntryType
import com.frenchtoast.receiptvault.model.ItemCategory
import com.frenchtoast.receiptvault.model.VaultDate
import com.frenchtoast.receiptvault.security.AppLockController
import com.frenchtoast.receiptvault.ui.components.categoryIcon
import com.frenchtoast.receiptvault.ui.components.entryTypeIcon
import com.frenchtoast.receiptvault.ui.theme.JetBrainsMonoFamily
import com.frenchtoast.receiptvault.ui.viewmodel.AddEditUiState
import com.frenchtoast.receiptvault.ui.viewmodel.CaptureTarget
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch

private enum class DateFieldTarget {
    PURCHASE,
    RETURN_DEADLINE,
    WARRANTY_EXPIRATION,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditItemScreen(
    uiState: AddEditUiState,
    imageStorage: VaultImageStorage,
    appLockController: AppLockController,
    onEntryTypeChange: (EntryType) -> Unit,
    onTitleChange: (String) -> Unit,
    onStoreChange: (String) -> Unit,
    onCategoryChange: (ItemCategory) -> Unit,
    onPurchaseDateChange: (Long) -> Unit,
    onPriceChange: (String) -> Unit,
    onSerialChange: (String) -> Unit,
    onReturnDeadlineChange: (Long?) -> Unit,
    onWarrantyExpirationChange: (Long?) -> Unit,
    onNotesChange: (String) -> Unit,
    onRawOcrChange: (String) -> Unit,
    onImageSelected: (Uri, CaptureTarget) -> Unit,
    onDocumentSelected: (Uri) -> Unit,
    onRemoveReceiptImage: () -> Unit,
    onRemoveSerialImage: () -> Unit,
    onRemoveDocument: () -> Unit,
    onApplyBarcodeAsSerial: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    onClearScanMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeCaptureTarget by remember { mutableStateOf(CaptureTarget.RECEIPT) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var datePickerTarget by remember { mutableStateOf<DateFieldTarget?>(null) }
    var showRawOcr by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.scanStatusMessage) {
        val msg = uiState.scanStatusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onClearScanMessage()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onImageSelected(uri, activeCaptureTarget)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            onDocumentSelected(uri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onImageSelected(uri, activeCaptureTarget)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val (_, uri) = imageStorage.createCameraCaptureUri()
            pendingCameraUri = uri
            appLockController.suppressNextBackgroundLock()
            takePictureLauncher.launch(uri)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission denied. You can still pick a photo or PDF.")
            }
        }
    }

    val documentScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pageUri = scanResult?.pages?.firstOrNull()?.imageUri
            if (pageUri != null) {
                onImageSelected(pageUri, activeCaptureTarget)
            }
        }
    }

    fun launchDocumentScanner(target: CaptureTarget) {
        activeCaptureTarget = target
        val activity = context.findActivity()
        if (activity == null) {
            appLockController.suppressNextBackgroundLock()
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            return
        }
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        val client = GmsDocumentScanning.getClient(options)
        client.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                appLockController.suppressNextBackgroundLock()
                documentScannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Document scanner unavailable on this device. Opening Photo Picker instead.",
                    )
                }
                appLockController.suppressNextBackgroundLock()
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }
    }

    fun launchCamera(target: CaptureTarget) {
        activeCaptureTarget = target
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val (_, uri) = imageStorage.createCameraCaptureUri()
            pendingCameraUri = uri
            appLockController.suppressNextBackgroundLock()
            takePictureLauncher.launch(uri)
        } else {
            appLockController.suppressNextBackgroundLock()
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery(target: CaptureTarget) {
        activeCaptureTarget = target
        appLockController.suppressNextBackgroundLock()
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    fun launchDocumentPicker() {
        appLockController.suppressNextBackgroundLock()
        documentPickerLauncher.launch(
            arrayOf(
                "application/pdf",
                "text/plain",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "*/*",
            ),
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.editingItemId == null) "Add Vault Entry" else "Edit Vault Entry")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onSaveClick,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("save_item_button"),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Entry Type Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Entry Type",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        EntryType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = EntryType.entries.size,
                                ),
                                onClick = { onEntryTypeChange(type) },
                                selected = uiState.entryType == type,
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = uiState.entryType == type) {
                                        Icon(
                                            imageVector = entryTypeIcon(type),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("entry_type_${type.name.lowercase()}"),
                            ) {
                                Text(type.label, maxLines = 1)
                            }
                        }
                    }
                }

                if (uiState.isScanning) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Processing attachment locally…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (uiState.cardMaskedNotice) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = "Payment card digits detected in scanned text were automatically masked to the last 4 digits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                // PDF / Document SAF Attachment Card
                DocumentAttachmentCard(
                    documentFile = imageStorage.resolveDocumentFile(uiState.documentPath),
                    displayName = uiState.documentDisplayName,
                    onAttachDocument = ::launchDocumentPicker,
                    onRemoveDocument = onRemoveDocument,
                )

                // Receipt / Primary Photo Card
                AttachmentCaptureCard(
                    title = if (uiState.entryType == EntryType.DOCUMENT) {
                        "Document / Certificate Photo (Optional)"
                    } else {
                        "Receipt Photo"
                    },
                    subtitle = "Scan or pick an image to extract merchant, total, date & mask card digits.",
                    icon = Icons.Rounded.ReceiptLong,
                    imageFile = imageStorage.resolveImageFile(uiState.receiptImagePath),
                    onScan = { launchDocumentScanner(CaptureTarget.RECEIPT) },
                    onCamera = { launchCamera(CaptureTarget.RECEIPT) },
                    onPick = { launchGallery(CaptureTarget.RECEIPT) },
                    onRemove = onRemoveReceiptImage,
                )

                // Serial / Box Photo Card
                AttachmentCaptureCard(
                    title = "Product Box / Serial Label",
                    subtitle = "Scan or pick a serial sticker to extract S/N text & barcodes offline.",
                    icon = Icons.Rounded.QrCodeScanner,
                    imageFile = imageStorage.resolveImageFile(uiState.serialImagePath),
                    onScan = { launchDocumentScanner(CaptureTarget.SERIAL_LABEL) },
                    onCamera = { launchCamera(CaptureTarget.SERIAL_LABEL) },
                    onPick = { launchGallery(CaptureTarget.SERIAL_LABEL) },
                    onRemove = onRemoveSerialImage,
                )

                AnimatedVisibility(visible = uiState.detectedBarcodes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Detected Barcodes / Serials (tap to use)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            uiState.detectedBarcodes.forEach { code ->
                                AssistChip(
                                    onClick = { onApplyBarcodeAsSerial(code) },
                                    label = {
                                        Text(
                                            text = code,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                uiState.validationError?.let { errorText ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Text(
                    text = if (uiState.entryType == EntryType.DOCUMENT) {
                        "Document Details"
                    } else {
                        "Item & Purchase Details"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    label = {
                        Text(if (uiState.entryType == EntryType.DOCUMENT) "Document Title *" else "Item Title *")
                    },
                    placeholder = {
                        Text(
                            if (uiState.entryType == EntryType.DOCUMENT) {
                                "e.g., Home Appliance Protection Policy"
                            } else {
                                "e.g., Sony WH-1000XM5 Headphones"
                            },
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_title"),
                )

                OutlinedTextField(
                    value = uiState.storeName,
                    onValueChange = onStoreChange,
                    label = {
                        Text(
                            when (uiState.entryType) {
                                EntryType.DOCUMENT -> "Issuer / Organization *"
                                EntryType.WARRANTY -> "Store / Manufacturer *"
                                EntryType.RECEIPT -> "Store / Merchant *"
                            },
                        )
                    },
                    placeholder = {
                        Text(if (uiState.entryType == EntryType.DOCUMENT) "e.g., Pacific Mutual" else "e.g., Best Buy")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_store"),
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ItemCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = uiState.category == cat,
                                onClick = { onCategoryChange(cat) },
                                label = { Text(cat.label) },
                                shape = CircleShape,
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryIcon(cat),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.priceInput,
                        onValueChange = onPriceChange,
                        label = { Text(if (uiState.entryType == EntryType.DOCUMENT) "Value / Fee ($)" else "Total Paid ($)") },
                        placeholder = { Text("0.00") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_price"),
                    )

                    DateSelectorButton(
                        label = if (uiState.entryType == EntryType.DOCUMENT) "Issue Date" else "Purchase Date",
                        epochDay = uiState.purchaseDateEpochDay,
                        onClick = { datePickerTarget = DateFieldTarget.PURCHASE },
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = uiState.serialNumber,
                    onValueChange = onSerialChange,
                    label = {
                        Text(
                            if (uiState.entryType == EntryType.DOCUMENT) {
                                "Policy / Reference Number"
                            } else {
                                "Serial Number / Service Tag"
                            },
                        )
                    },
                    placeholder = { Text("Optional serial, IMEI, or policy ID") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_serial"),
                )

                if (uiState.entryType != EntryType.DOCUMENT) {
                    DeadlineFieldCard(
                        title = "Return Window Deadline",
                        epochDay = uiState.returnDeadlineEpochDay,
                        onPickDate = { datePickerTarget = DateFieldTarget.RETURN_DEADLINE },
                        onClear = { onReturnDeadlineChange(null) },
                        quickPresets = listOf(
                            "+14d" to { VaultDate.plusDays(uiState.purchaseDateEpochDay, 14) },
                            "+30d" to { VaultDate.plusDays(uiState.purchaseDateEpochDay, 30) },
                            "+90d" to { VaultDate.plusDays(uiState.purchaseDateEpochDay, 90) },
                        ),
                        onSelectPreset = onReturnDeadlineChange,
                    )
                }

                DeadlineFieldCard(
                    title = if (uiState.entryType == EntryType.DOCUMENT) {
                        "Expiration / Renewal Date"
                    } else {
                        "Warranty Expiration"
                    },
                    epochDay = uiState.warrantyExpirationEpochDay,
                    onPickDate = { datePickerTarget = DateFieldTarget.WARRANTY_EXPIRATION },
                    onClear = { onWarrantyExpirationChange(null) },
                    quickPresets = listOf(
                        "+1 Year" to { VaultDate.plusYears(uiState.purchaseDateEpochDay, 1) },
                        "+2 Years" to { VaultDate.plusYears(uiState.purchaseDateEpochDay, 2) },
                        "+3 Years" to { VaultDate.plusYears(uiState.purchaseDateEpochDay, 3) },
                    ),
                    onSelectPreset = onWarrantyExpirationChange,
                )

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes") },
                    placeholder = { Text("Condition, coverage details, claim contact…") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_notes"),
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        TextButton(
                            onClick = { showRawOcr = !showRawOcr },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = if (showRawOcr) "Hide Masked OCR Text" else "View / Edit Masked OCR Text",
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = if (showRawOcr) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                            )
                        }
                        AnimatedVisibility(visible = showRawOcr) {
                            OutlinedTextField(
                                value = uiState.rawOcrText,
                                onValueChange = onRawOcrChange,
                                label = { Text("Scanned Receipt Text (Card Numbers Masked)") },
                                minLines = 4,
                                maxLines = 10,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .testTag("input_ocr_text"),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    datePickerTarget?.let { target ->
        val initialEpochDay = when (target) {
            DateFieldTarget.PURCHASE -> uiState.purchaseDateEpochDay
            DateFieldTarget.RETURN_DEADLINE -> uiState.returnDeadlineEpochDay ?: VaultDate.plusDays(uiState.purchaseDateEpochDay, 30)
            DateFieldTarget.WARRANTY_EXPIRATION -> uiState.warrantyExpirationEpochDay ?: VaultDate.plusYears(uiState.purchaseDateEpochDay, 1)
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = VaultDate.toUtcMidnightMillis(initialEpochDay),
        )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = pickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val epochDay = VaultDate.fromUtcMillis(selectedMillis)
                            when (target) {
                                DateFieldTarget.PURCHASE -> onPurchaseDateChange(epochDay)
                                DateFieldTarget.RETURN_DEADLINE -> onReturnDeadlineChange(epochDay)
                                DateFieldTarget.WARRANTY_EXPIRATION -> onWarrantyExpirationChange(epochDay)
                            }
                        }
                        datePickerTarget = null
                    },
                ) {
                    Text("Set Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTarget = null }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DocumentAttachmentCard(
    documentFile: java.io.File?,
    displayName: String?,
    onAttachDocument: () -> Unit,
    onRemoveDocument: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("PDF / Document File", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Attach manuals, insurance policies, invoices, or certificates. Saved offline inside app storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (documentFile != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName ?: documentFile.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = Formatter.formatShortFileSize(context, documentFile.length()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onRemoveDocument) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove document")
                        }
                    }
                }
            }

            FilledTonalButton(
                onClick = onAttachDocument,
                shape = CircleShape,
                modifier = Modifier.testTag("attach_document_button"),
            ) {
                Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (documentFile != null) "Replace PDF / Document" else "Attach PDF or Document")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttachmentCaptureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    imageFile: java.io.File?,
    onScan: () -> Unit,
    onCamera: () -> Unit,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (imageFile != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    ) {
                        IconButton(onClick = onRemove) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove $title",
                            )
                        }
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onScan,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan")
                }
                OutlinedButton(
                    onClick = onCamera,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Camera")
                }
                OutlinedButton(
                    onClick = onPick,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery")
                }
            }
        }
    }
}

@Composable
private fun DateSelectorButton(
    label: String,
    epochDay: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(56.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = VaultDate.formatDisplay(epochDay),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeadlineFieldCard(
    title: String,
    epochDay: Long?,
    onPickDate: () -> Unit,
    onClear: () -> Unit,
    quickPresets: List<Pair<String, () -> Long>>,
    onSelectPreset: (Long) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(title, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = epochDay?.let { VaultDate.formatDisplay(it) } ?: "Not set",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (epochDay != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (epochDay != null) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear $title")
                        }
                    }
                    FilledTonalButton(onClick = onPickDate, shape = CircleShape) {
                        Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick")
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                quickPresets.forEach { (label, compute) ->
                    AssistChip(
                        onClick = { onSelectPreset(compute()) },
                        shape = CircleShape,
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
