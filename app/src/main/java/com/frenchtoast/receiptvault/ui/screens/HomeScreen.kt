package com.frenchtoast.receiptvault.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frenchtoast.receiptvault.R
import com.frenchtoast.receiptvault.export.ClaimPacketGenerator
import com.frenchtoast.receiptvault.model.EntryType
import com.frenchtoast.receiptvault.model.HomeFilterChip
import com.frenchtoast.receiptvault.model.VaultDate
import com.frenchtoast.receiptvault.model.VaultItem
import com.frenchtoast.receiptvault.security.AppLockController
import com.frenchtoast.receiptvault.ui.components.EntryTypePill
import com.frenchtoast.receiptvault.ui.components.SerialBadgeChip
import com.frenchtoast.receiptvault.ui.components.ShareOptionsBottomSheet
import com.frenchtoast.receiptvault.ui.components.StatusBadgePill
import com.frenchtoast.receiptvault.ui.components.categoryIcon
import com.frenchtoast.receiptvault.ui.theme.JetBrainsMonoFamily
import com.frenchtoast.receiptvault.ui.viewmodel.HomeUiState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    claimPacketGenerator: ClaimPacketGenerator,
    appLockController: AppLockController,
    onSearchChange: (String) -> Unit,
    onSelectFilter: (HomeFilterChip) -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onClearDemo: () -> Unit,
    onResetDemo: () -> Unit,
    onItemClick: (String) -> Unit,
    onAddItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shareTargetItem by remember { mutableStateOf<VaultItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddItemClick,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(stringResource(R.string.action_add_entry))
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_item_fab"),
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            val columns = if (maxWidth >= 620.dp) 2 else 1

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 92.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(columns) }) {
                    DeclutteredVaultHeader(
                        demoModeEnabled = uiState.demoModeEnabled,
                        hasDemoItems = uiState.hasDemoItems,
                        onToggleDemoMode = onToggleDemoMode,
                        onClearDemo = onClearDemo,
                        onResetDemo = onResetDemo,
                    )
                }

                item(span = { GridItemSpan(columns) }) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search",
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_search_input"),
                    )
                }

                item(span = { GridItemSpan(columns) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(HomeFilterChip.entries) { filterChip ->
                            val selected = uiState.selectedFilter == filterChip
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectFilter(filterChip) },
                                label = {
                                    Text(
                                        text = filterChip.label,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                modifier = Modifier.testTag("home_filter_${filterChip.name.lowercase()}"),
                            )
                        }
                    }
                }

                if (uiState.filteredItems.isEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        EmptyVaultState(
                            hasAnyItems = uiState.allItemsCount > 0,
                            onAddItemClick = onAddItemClick,
                            onResetDemo = onResetDemo,
                            onClearFilters = {
                                onSearchChange("")
                                onSelectFilter(HomeFilterChip.ALL)
                            },
                        )
                    }
                } else {
                    items(
                        items = uiState.filteredItems,
                        key = { it.id },
                    ) { item ->
                        ExpressiveVaultCard(
                            item = item,
                            returnLeadDays = uiState.returnLeadDays,
                            warrantyLeadDays = uiState.warrantyLeadDays,
                            onClick = { onItemClick(item.id) },
                            onQuickShareClick = { shareTargetItem = item },
                        )
                    }
                }
            }
        }
    }

    shareTargetItem?.let { target ->
        ShareOptionsBottomSheet(
            item = target,
            claimPacketGenerator = claimPacketGenerator,
            appLockController = appLockController,
            onDismiss = { shareTargetItem = null },
            onFeedback = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
        )
    }
}

@Composable
private fun DeclutteredVaultHeader(
    demoModeEnabled: Boolean,
    hasDemoItems: Boolean,
    onToggleDemoMode: (Boolean) -> Unit,
    onClearDemo: () -> Unit,
    onResetDemo: () -> Unit,
) {
    var showDemoMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Toasts Receipt Vault",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Offline receipts, warranties & documents",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(8.dp))

        Box {
            Surface(
                onClick = { showDemoMenu = true },
                shape = CircleShape,
                color = if (demoModeEnabled || hasDemoItems) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.testTag("demo_mode_pill"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "Demo Mode options",
                        tint = if (demoModeEnabled || hasDemoItems) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = if (demoModeEnabled) "Demo On" else "Demo Off",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (demoModeEnabled || hasDemoItems) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            DropdownMenu(
                expanded = showDemoMenu,
                onDismissRequest = { showDemoMenu = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(if (demoModeEnabled) "Turn Demo Mode Off" else "Turn Demo Mode On")
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Shield, contentDescription = null)
                    },
                    onClick = {
                        showDemoMenu = false
                        onToggleDemoMode(!demoModeEnabled)
                    },
                    modifier = Modifier.testTag("demo_mode_switch"),
                )
                if (demoModeEnabled || hasDemoItems) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.demo_mode_clear)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                        },
                        onClick = {
                            showDemoMenu = false
                            onClearDemo()
                        },
                        modifier = Modifier.testTag("clear_demo_button"),
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.demo_mode_reset)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                    },
                    onClick = {
                        showDemoMenu = false
                        onResetDemo()
                    },
                    modifier = Modifier.testTag("reset_demo_button"),
                )
            }
        }
    }
}

@Composable
private fun ExpressiveVaultCard(
    item: VaultItem,
    returnLeadDays: Int,
    warrantyLeadDays: Int,
    onClick: () -> Unit,
    onQuickShareClick: () -> Unit,
) {
    val (countdownText, countdownStatus) = item.primaryCountdownSummary(
        returnLeadDays = returnLeadDays,
        warrantyLeadDays = warrantyLeadDays,
    )

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_item_card_${item.id}"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    EntryTypePill(entryType = item.entryType)
                    Icon(
                        imageVector = categoryIcon(item.category),
                        contentDescription = item.category.label,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = item.storeName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = onQuickShareClick,
                    modifier = Modifier.testTag("quick_share_${item.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share ${item.title}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (item.priceCents > 0L) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = item.formattedPrice(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val datePrefix = if (item.entryType == EntryType.DOCUMENT) "Issued" else "Purchased"
                Text(
                    text = "$datePrefix ${VaultDate.formatDisplay(item.purchaseDateEpochDay)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val fileCount = item.attachedFileCount()
                if (fileCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (!item.documentPath.isNullOrBlank()) {
                                Icons.Rounded.PictureAsPdf
                            } else {
                                Icons.Rounded.AttachFile
                            },
                            contentDescription = "Attached files",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (!item.documentDisplayName.isNullOrBlank() && fileCount == 1) {
                                "PDF"
                            } else {
                                "$fileCount file${if (fileCount == 1) "" else "s"}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadgePill(
                    label = countdownText,
                    status = countdownStatus,
                )
                if (item.serialNumber.isNotBlank()) {
                    SerialBadgeChip(
                        serial = item.serialNumber,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultState(
    hasAnyItems: Boolean,
    onAddItemClick: () -> Unit,
    onResetDemo: () -> Unit,
    onClearFilters: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!hasAnyItems) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_vault),
                contentDescription = "Empty receipt vault illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            )
            Text(
                text = stringResource(R.string.empty_vault_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.empty_vault_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onAddItemClick,
                    shape = CircleShape,
                    modifier = Modifier.testTag("empty_add_receipt_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Entry")
                }
                OutlinedButton(
                    onClick = onResetDemo,
                    shape = CircleShape,
                ) {
                    Text("Load Demo Data")
                }
            }
        } else {
            Text(
                text = "No matching entries",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Try clearing your search or switching filter chips.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onClearFilters) {
                Text("Reset Filters")
            }
        }
    }
}
