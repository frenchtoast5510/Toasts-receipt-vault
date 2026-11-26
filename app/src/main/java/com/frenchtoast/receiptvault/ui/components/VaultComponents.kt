package com.frenchtoast.receiptvault.ui.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.frenchtoast.receiptvault.data.storage.VaultImageStorage
import com.frenchtoast.receiptvault.export.ClaimPacketGenerator
import com.frenchtoast.receiptvault.model.EntryType
import com.frenchtoast.receiptvault.model.ItemCategory
import com.frenchtoast.receiptvault.model.VaultItem
import com.frenchtoast.receiptvault.model.WindowStatus
import com.frenchtoast.receiptvault.security.AppLockController
import com.frenchtoast.receiptvault.ui.theme.JetBrainsMonoFamily
import com.frenchtoast.receiptvault.ui.theme.LocalVaultStatusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun categoryIcon(category: ItemCategory): ImageVector = when (category) {
    ItemCategory.ELECTRONICS -> Icons.Rounded.Devices
    ItemCategory.APPLIANCES -> Icons.Rounded.Kitchen
    ItemCategory.TOOLS -> Icons.Rounded.Build
    ItemCategory.GAMING -> Icons.Rounded.SportsEsports
    ItemCategory.OTHER -> Icons.Rounded.Inventory2
}

fun entryTypeIcon(entryType: EntryType): ImageVector = when (entryType) {
    EntryType.RECEIPT -> Icons.Rounded.ReceiptLong
    EntryType.WARRANTY -> Icons.Rounded.VerifiedUser
    EntryType.DOCUMENT -> Icons.Rounded.Description
}

@Composable
fun EntryTypePill(
    entryType: EntryType,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = when (entryType) {
        EntryType.RECEIPT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        EntryType.WARRANTY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        EntryType.DOCUMENT -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = entryTypeIcon(entryType),
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = entryType.label,
                color = fg,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
fun StatusBadgePill(
    label: String,
    status: WindowStatus,
    modifier: Modifier = Modifier,
) {
    val statusColors = LocalVaultStatusColors.current
    val (bg, fg) = when (status) {
        WindowStatus.ACTIVE -> statusColors.activeContainer to statusColors.onActiveContainer
        WindowStatus.EXPIRING_SOON -> statusColors.expiringContainer to statusColors.onExpiringContainer
        WindowStatus.EXPIRED -> statusColors.expiredContainer to statusColors.onExpiredContainer
        WindowStatus.NONE -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bg,
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun SerialBadgeChip(
    serial: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ConfirmationNumber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = serial,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ZoomableImageBox(
    file: File,
    contentDescription: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onResetVisible: Boolean = true,
) {
    var scale by remember(file.absolutePath) { mutableFloatStateOf(1f) }
    var offset by remember(file.absolutePath) { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .onSizeChanged { boxSize = it }
            .pointerInput(file.absolutePath) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .pointerInput(file.absolutePath) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale <= 1.01f) {
                        offset = Offset.Zero
                    } else {
                        val maxX = (boxSize.width * (newScale - 1f)) / 2f
                        val maxY = (boxSize.height * (newScale - 1f)) / 2f
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                        )
                    }
                }
            },
    ) {
        AsyncImage(
            model = file,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )

        if (onResetVisible && scale > 1.05f) {
            FilledTonalIconButton(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ZoomOutMap,
                    contentDescription = "Reset zoom",
                )
            }
        }
    }
}

@Composable
fun ZoomableBitmapBox(
    bitmap: Bitmap,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .background(Color.Black)
            .onSizeChanged { boxSize = it }
            .pointerInput(bitmap) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .pointerInput(bitmap) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale <= 1.01f) {
                        offset = Offset.Zero
                    } else {
                        val maxX = (boxSize.width * (newScale - 1f)) / 2f
                        val maxY = (boxSize.height * (newScale - 1f)) / 2f
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}

@Composable
fun InAppPdfViewerDialog(
    file: File,
    title: String,
    imageStorage: VaultImageStorage,
    onDismiss: () -> Unit,
) {
    var pageCount by remember(file.absolutePath) { mutableIntStateOf(0) }
    var pageIndex by remember(file.absolutePath) { mutableIntStateOf(0) }
    var renderedBitmap by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }

    LaunchedEffect(file.absolutePath) {
        pageCount = withContext(Dispatchers.IO) { imageStorage.getPdfPageCount(file) }
    }

    LaunchedEffect(file.absolutePath, pageIndex) {
        isLoading = true
        val bmp = withContext(Dispatchers.IO) {
            imageStorage.renderPdfPageBitmap(file, pageIndex = pageIndex, targetWidth = 1400)
        }
        renderedBitmap = bmp
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pageCount > 0) {
                        Text(
                            text = "Page ${pageIndex + 1} of $pageCount",
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close PDF viewer",
                        tint = Color.White,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else if (renderedBitmap != null) {
                    ZoomableBitmapBox(
                        bitmap = renderedBitmap!!,
                        contentDescription = "$title page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = "Unable to render PDF preview.",
                        color = Color.White,
                    )
                }
            }

            if (pageCount > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(
                        onClick = { if (pageIndex > 0) pageIndex-- },
                        enabled = pageIndex > 0,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous page",
                        )
                    }
                    Text(
                        text = "Pinch or double-tap to zoom",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    FilledTonalIconButton(
                        onClick = { if (pageIndex < pageCount - 1) pageIndex++ },
                        enabled = pageIndex < pageCount - 1,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next page",
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareOptionsBottomSheet(
    item: VaultItem,
    claimPacketGenerator: ClaimPacketGenerator,
    appLockController: AppLockController,
    onDismiss: () -> Unit,
    onFeedback: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasOriginalFiles = remember(item) { item.hasOriginalFiles() }
    var isPreparing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Share Vault Item",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            ShareOptionCard(
                title = "Share Original Files",
                subtitle = if (hasOriginalFiles) {
                    "Attach saved receipt photos and/or PDF documents (${item.attachedFileCount()} file${if (item.attachedFileCount() == 1) "" else "s"})"
                } else {
                    "No photos or PDF documents attached to this item"
                },
                icon = Icons.Rounded.AttachFile,
                enabled = hasOriginalFiles && !isPreparing,
                testTag = "share_option_original",
                onClick = {
                    scope.launch {
                        isPreparing = true
                        val chooser = withContext(Dispatchers.IO) {
                            claimPacketGenerator.buildShareOriginalFilesIntent(item)
                        }
                        isPreparing = false
                        if (chooser != null) {
                            appLockController.suppressNextBackgroundLock()
                            context.startActivity(chooser)
                            onDismiss()
                        } else {
                            onFeedback("No attached files found to share.")
                        }
                    }
                },
            )

            ShareOptionCard(
                title = "Share Item Summary",
                subtitle = "Clean formatted text with store/issuer, date, total, serial, and expiration dates",
                icon = Icons.Rounded.TextSnippet,
                enabled = !isPreparing,
                testTag = "share_option_summary",
                onClick = {
                    val chooser = claimPacketGenerator.buildShareSummaryIntent(item)
                    appLockController.suppressNextBackgroundLock()
                    context.startActivity(chooser)
                    onDismiss()
                },
            )

            ShareOptionCard(
                title = "Share PDF Claim / Summary Packet",
                subtitle = "Single-page PDF combining item details, serial number, and receipt or document image",
                icon = Icons.Rounded.PictureAsPdf,
                enabled = !isPreparing,
                testTag = "share_option_pdf_packet",
                onClick = {
                    scope.launch {
                        isPreparing = true
                        val chooserResult = withContext(Dispatchers.IO) {
                            runCatching { claimPacketGenerator.buildSharePacketPdfIntent(item) }
                        }
                        isPreparing = false
                        chooserResult.onSuccess { chooser ->
                            appLockController.suppressNextBackgroundLock()
                            context.startActivity(chooser)
                            onDismiss()
                        }.onFailure {
                            onFeedback("Failed to generate PDF packet.")
                        }
                    }
                },
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ShareOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
