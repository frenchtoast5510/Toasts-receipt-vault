package com.frenchtoast.receiptvault.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frenchtoast.receiptvault.R
import com.frenchtoast.receiptvault.data.storage.VaultImageStorage
import com.frenchtoast.receiptvault.export.ClaimPacketGenerator
import com.frenchtoast.receiptvault.model.EntryType
import com.frenchtoast.receiptvault.model.VaultDate
import com.frenchtoast.receiptvault.model.WindowStatus
import com.frenchtoast.receiptvault.security.AppLockController
import com.frenchtoast.receiptvault.ui.components.EntryTypePill
import com.frenchtoast.receiptvault.ui.components.InAppPdfViewerDialog
import com.frenchtoast.receiptvault.ui.components.ShareOptionsBottomSheet
import com.frenchtoast.receiptvault.ui.components.StatusBadgePill
import com.frenchtoast.receiptvault.ui.components.ZoomableImageBox
import com.frenchtoast.receiptvault.ui.components.categoryIcon
import com.frenchtoast.receiptvault.ui.theme.JetBrainsMonoFamily
import com.frenchtoast.receiptvault.ui.viewmodel.DetailUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    uiState: DetailUiState,
    imageStorage: VaultImageStorage,
    claimPacketGenerator: ClaimPacketGenerator,
    appLockController: AppLockController,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var fullscreenImageFile by remember { mutableStateOf<File?>(null) }
    var inAppPdfFile by remember { mutableStateOf<File?>(null) }
    var showRawOcr by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBackClick()
    }

    val item = uiState.item

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Entry Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(
                            onClick = { showShareSheet = true },
                            modifier = Modifier.testTag("top_share_button"),
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share item")
                        }
                        IconButton(
                            onClick = { onEditClick(item.id) },
                            modifier = Modifier.testTag("edit_item_button"),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit item")
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("delete_item_button"),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete item")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Item not found in vault.")
            }
            return@Scaffold
        }

        val receiptFile = imageStorage.resolveImageFile(item.receiptImagePath)
        val serialFile = imageStorage.resolveImageFile(item.serialImagePath)
        val docFile = imageStorage.resolveDocumentFile(item.documentPath)

        val availablePhotos = buildList {
            if (receiptFile != null) add("Receipt" to receiptFile)
            if (serialFile != null) add("Serial Label" to serialFile)
        }
        var selectedPhotoTab by remember(item.id) { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EntryTypePill(entryType = item.entryType)
                        Icon(
                            imageVector = categoryIcon(item.category),
                            contentDescription = item.category.label,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "${item.storeName} • ${item.category.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val dateLabel = if (item.entryType == EntryType.DOCUMENT) "Issued" else "Purchased"
                    Text(
                        text = "$dateLabel ${VaultDate.formatDisplay(item.purchaseDateEpochDay)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Document / PDF Preview Card
                if (docFile != null) {
                    DocumentPreviewCard(
                        file = docFile,
                        displayName = item.documentDisplayName ?: docFile.name,
                        mimeType = item.documentMimeType ?: "application/pdf",
                        imageStorage = imageStorage,
                        onOpenInAppPdf = { inAppPdfFile = docFile },
                        onOpenExternal = {
                            val uri = imageStorage.getFileProviderUri(docFile)
                            val mime = item.documentMimeType ?: "application/pdf"
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mime)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching {
                                appLockController.suppressNextBackgroundLock()
                                context.startActivity(Intent.createChooser(viewIntent, "Open Document"))
                            }.onFailure {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No external app available to open this file.")
                                }
                            }
                        },
                    )
                }

                // Zoomable Photo Viewer
                if (availablePhotos.isNotEmpty()) {
                    val safeIndex = selectedPhotoTab.coerceIn(0, availablePhotos.lastIndex)
                    val (photoLabel, photoFile) = availablePhotos[safeIndex]

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            if (availablePhotos.size > 1) {
                                SecondaryTabRow(selectedTabIndex = safeIndex) {
                                    availablePhotos.forEachIndexed { index, (label, _) ->
                                        Tab(
                                            selected = safeIndex == index,
                                            onClick = { selectedPhotoTab = index },
                                            text = { Text(label) },
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .padding(10.dp),
                            ) {
                                ZoomableImageBox(
                                    file = photoFile,
                                    contentDescription = "$photoLabel image for ${item.title}",
                                    modifier = Modifier.fillMaxSize(),
                                )
                                FilledTonalIconButton(
                                    onClick = { fullscreenImageFile = photoFile },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescription = "Open fullscreen viewer",
                                    )
                                }
                            }
                            Text(
                                text = "Pinch or double-tap image to zoom",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            )
                        }
                    }
                }

                // Countdown Cards
                if (item.returnDeadlineEpochDay != null || item.warrantyExpirationEpochDay != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (item.returnDeadlineEpochDay != null) {
                            DeadlineCountdownCard(
                                title = "Return Window",
                                epochDay = item.returnDeadlineEpochDay,
                                daysRemaining = item.returnDaysRemaining(),
                                status = item.returnStatus(returnLeadDays = uiState.returnLeadDays),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (item.warrantyExpirationEpochDay != null) {
                            DeadlineCountdownCard(
                                title = if (item.entryType == EntryType.DOCUMENT) "Expiration" else "Warranty",
                                epochDay = item.warrantyExpirationEpochDay,
                                daysRemaining = item.warrantyDaysRemaining(),
                                status = item.warrantyStatus(warrantyLeadDays = uiState.warrantyLeadDays),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Copyable Total & Serial / Reference Card
                if (item.priceCents > 0L || item.serialNumber.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (item.priceCents > 0L) {
                                CopyableMetricRow(
                                    label = if (item.entryType == EntryType.DOCUMENT) "Value / Fee" else "Total Paid",
                                    value = item.formattedPrice(),
                                    testTag = "copy_total_button",
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(item.formattedPrice()))
                                        scope.launch { snackbarHostState.showSnackbar("Amount copied to clipboard") }
                                    },
                                )
                            }
                            if (item.serialNumber.isNotBlank()) {
                                CopyableMetricRow(
                                    label = if (item.entryType == EntryType.DOCUMENT) "Reference / Policy ID" else "Serial Number",
                                    value = item.serialNumber,
                                    testTag = "copy_serial_button",
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(item.serialNumber))
                                        scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                                    },
                                )
                            }
                        }
                    }
                }

                // Expressive Primary Share Button
                Button(
                    onClick = { showShareSheet = true },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("share_claim_packet_button"),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Files, Summary, or PDF Packet")
                }

                if (item.notes.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Notes", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (item.rawOcrText.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
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
                                    text = if (showRawOcr) "Hide Masked Receipt Text" else "View Masked Receipt OCR Text",
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = if (showRawOcr) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                )
                            }
                            AnimatedVisibility(visible = showRawOcr) {
                                Text(
                                    text = item.rawOcrText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showShareSheet && item != null) {
        ShareOptionsBottomSheet(
            item = item,
            claimPacketGenerator = claimPacketGenerator,
            appLockController = appLockController,
            onDismiss = { showShareSheet = false },
            onFeedback = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Vault Entry?") },
            text = { Text("This permanently deletes this entry and its locally stored files from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirm()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    fullscreenImageFile?.let { file ->
        Dialog(
            onDismissRequest = { fullscreenImageFile = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                ZoomableImageBox(
                    file = file,
                    contentDescription = "Fullscreen receipt viewer",
                    backgroundColor = Color.Black,
                    modifier = Modifier.fillMaxSize(),
                )
                FilledTonalIconButton(
                    onClick = { fullscreenImageFile = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(18.dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close fullscreen viewer")
                }
            }
        }
    }

    inAppPdfFile?.let { pdfFile ->
        InAppPdfViewerDialog(
            file = pdfFile,
            title = item?.documentDisplayName ?: pdfFile.name,
            imageStorage = imageStorage,
            onDismiss = { inAppPdfFile = null },
        )
    }
}

@Composable
private fun DocumentPreviewCard(
    file: File,
    displayName: String,
    mimeType: String,
    imageStorage: VaultImageStorage,
    onOpenInAppPdf: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    val context = LocalContext.current
    val isPdf = mimeType.contains("pdf", ignoreCase = true) || file.extension.equals("pdf", true)
    var pageCount by remember(file.absolutePath) { mutableIntStateOf(0) }
    var previewBitmap by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.absolutePath, isPdf) {
        if (isPdf) {
            withContext(Dispatchers.IO) {
                pageCount = imageStorage.getPdfPageCount(file)
                previewBitmap = imageStorage.renderPdfPageBitmap(file, pageIndex = 0, targetWidth = 700)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("document_preview_card"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sizeText = Formatter.formatShortFileSize(context, file.length())
                    val pagesText = if (pageCount > 0) " • $pageCount page${if (pageCount == 1) "" else "s"}" else ""
                    Text(
                        text = "$sizeText$pagesText • Stored locally",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            previewBitmap?.let { bmp ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF preview page 1",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isPdf) {
                    FilledTonalButton(
                        onClick = onOpenInAppPdf,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_pdf_in_app_button"),
                    ) {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("View PDF")
                    }
                }
                OutlinedButton(
                    onClick = onOpenExternal,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_pdf_external_button"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open External")
                }
            }
        }
    }
}

@Composable
private fun DeadlineCountdownCard(
    title: String,
    epochDay: Long?,
    daysRemaining: Long?,
    status: WindowStatus,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (epochDay == null || daysRemaining == null) {
                Text(
                    text = "Not set",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val badgeText = when {
                    daysRemaining < 0 -> "Expired ${-daysRemaining}d ago"
                    daysRemaining == 0L -> "Due today"
                    daysRemaining == 1L -> "1 day left"
                    else -> "$daysRemaining days left"
                }
                StatusBadgePill(label = badgeText, status = status)
                Text(
                    text = VaultDate.formatDisplay(epochDay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CopyableMetricRow(
    label: String,
    value: String,
    testTag: String,
    onCopy: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(
            onClick = onCopy,
            modifier = Modifier.testTag(testTag),
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy $label",
            )
        }
    }
}
