package com.frenchtoast.receiptvault.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.frenchtoast.receiptvault.AppContainer
import com.frenchtoast.receiptvault.R
import com.frenchtoast.receiptvault.ui.screens.AddEditItemScreen
import com.frenchtoast.receiptvault.ui.screens.HomeScreen
import com.frenchtoast.receiptvault.ui.screens.ItemDetailScreen
import com.frenchtoast.receiptvault.ui.screens.RemindersBackupScreen
import com.frenchtoast.receiptvault.ui.viewmodel.AddEditViewModel
import com.frenchtoast.receiptvault.ui.viewmodel.DetailViewModel
import com.frenchtoast.receiptvault.ui.viewmodel.HomeViewModel
import com.frenchtoast.receiptvault.ui.viewmodel.RemindersBackupViewModel
import kotlinx.serialization.Serializable

@Serializable
object VaultHomeRoute

@Serializable
object RemindersBackupRoute

@Serializable
data class AddEditItemRoute(val itemId: String? = null)

@Serializable
data class ItemDetailRoute(val itemId: String)

@Composable
fun VaultAppRoot(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val isTopLevel = currentDestination?.hasRoute<VaultHomeRoute>() == true ||
        currentDestination?.hasRoute<RemindersBackupRoute>() == true

    val isHomeSelected = currentDestination?.hasRoute<VaultHomeRoute>() == true
    val isSettingsSelected = currentDestination?.hasRoute<RemindersBackupRoute>() == true

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = isHomeSelected,
                            onClick = { navigateTopLevel(VaultHomeRoute) },
                            icon = {
                                Icon(
                                    imageVector = if (isHomeSelected) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong,
                                    contentDescription = stringResource(R.string.nav_vault),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_vault)) },
                            modifier = Modifier.testTag("nav_vault_tab"),
                        )
                        NavigationBarItem(
                            selected = isSettingsSelected,
                            onClick = { navigateTopLevel(RemindersBackupRoute) },
                            icon = {
                                Icon(
                                    imageVector = if (isSettingsSelected) Icons.Rounded.Settings else Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.nav_settings),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_settings)) },
                            modifier = Modifier.testTag("nav_reminders_tab"),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isTopLevel && useRail) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = isHomeSelected,
                            onClick = { navigateTopLevel(VaultHomeRoute) },
                            icon = {
                                Icon(
                                    imageVector = if (isHomeSelected) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong,
                                    contentDescription = stringResource(R.string.nav_vault),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_vault)) },
                        )
                        NavigationRailItem(
                            selected = isSettingsSelected,
                            onClick = { navigateTopLevel(RemindersBackupRoute) },
                            icon = {
                                Icon(
                                    imageVector = if (isSettingsSelected) Icons.Rounded.Settings else Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.nav_settings),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_settings)) },
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = VaultHomeRoute,
                    modifier = Modifier.weight(1f),
                ) {
                    composable<VaultHomeRoute> {
                        val vm: HomeViewModel = viewModel(
                            factory = HomeViewModel.Factory(container.repository),
                        )
                        val uiState by vm.uiState.collectAsStateWithLifecycle()
                        HomeScreen(
                            uiState = uiState,
                            claimPacketGenerator = container.claimPacketGenerator,
                            appLockController = container.appLockController,
                            onSearchChange = vm::updateSearchQuery,
                            onSelectFilter = vm::selectFilter,
                            onToggleDemoMode = vm::toggleDemoMode,
                            onClearDemo = vm::clearDemoToEmptyVault,
                            onResetDemo = vm::resetSampleData,
                            onItemClick = { itemId ->
                                navController.navigate(ItemDetailRoute(itemId))
                            },
                            onAddItemClick = {
                                navController.navigate(AddEditItemRoute(itemId = null))
                            },
                        )
                    }

                    composable<AddEditItemRoute> { entry ->
                        val route = entry.toRoute<AddEditItemRoute>()
                        val vm: AddEditViewModel = viewModel(
                            factory = AddEditViewModel.Factory(
                                itemId = route.itemId,
                                repository = container.repository,
                                imageStorage = container.imageStorage,
                                scanner = container.scanner,
                            ),
                        )
                        val uiState by vm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(uiState.savedSuccessfully) {
                            if (uiState.savedSuccessfully) {
                                navController.popBackStack()
                            }
                        }
                        AddEditItemScreen(
                            uiState = uiState,
                            imageStorage = container.imageStorage,
                            appLockController = container.appLockController,
                            onEntryTypeChange = vm::updateEntryType,
                            onTitleChange = vm::updateTitle,
                            onStoreChange = vm::updateStoreName,
                            onCategoryChange = vm::updateCategory,
                            onPurchaseDateChange = vm::updatePurchaseDate,
                            onPriceChange = vm::updatePriceInput,
                            onSerialChange = vm::updateSerialNumber,
                            onReturnDeadlineChange = vm::updateReturnDeadline,
                            onWarrantyExpirationChange = vm::updateWarrantyExpiration,
                            onNotesChange = vm::updateNotes,
                            onRawOcrChange = vm::updateRawOcrText,
                            onImageSelected = vm::onImageSelected,
                            onDocumentSelected = vm::onDocumentSelected,
                            onRemoveReceiptImage = vm::removeReceiptImage,
                            onRemoveSerialImage = vm::removeSerialImage,
                            onRemoveDocument = vm::removeDocument,
                            onApplyBarcodeAsSerial = vm::applyDetectedBarcodeAsSerial,
                            onSaveClick = vm::saveItem,
                            onBackClick = { navController.popBackStack() },
                            onClearScanMessage = vm::clearScanMessage,
                        )
                    }

                    composable<ItemDetailRoute> { entry ->
                        val route = entry.toRoute<ItemDetailRoute>()
                        val vm: DetailViewModel = viewModel(
                            factory = DetailViewModel.Factory(
                                itemId = route.itemId,
                                repository = container.repository,
                                claimPacketGenerator = container.claimPacketGenerator,
                            ),
                        )
                        val uiState by vm.uiState.collectAsStateWithLifecycle()
                        ItemDetailScreen(
                            uiState = uiState,
                            imageStorage = container.imageStorage,
                            claimPacketGenerator = container.claimPacketGenerator,
                            appLockController = container.appLockController,
                            onBackClick = { navController.popBackStack() },
                            onEditClick = { itemId ->
                                navController.navigate(AddEditItemRoute(itemId))
                            },
                            onDeleteConfirm = vm::deleteItem,
                        )
                    }

                    composable<RemindersBackupRoute> {
                        val vm: RemindersBackupViewModel = viewModel(
                            factory = RemindersBackupViewModel.Factory(container.repository),
                        )
                        val uiState by vm.uiState.collectAsStateWithLifecycle()
                        RemindersBackupScreen(
                            uiState = uiState,
                            appLockController = container.appLockController,
                            onToggleReminders = vm::setRemindersEnabled,
                            onUpdateLeadDays = vm::updateLeadDays,
                            onRunImmediateCheck = vm::runImmediateReminderCheck,
                            onSelectAppLockMode = vm::setAppLockMode,
                            onSaveCustomPin = vm::setCustomPin,
                            onToggleBiometricWithPin = vm::setAllowBiometricWithCustomPin,
                            onSelectAutoLockTimeout = vm::setAutoLockTimeout,
                            onToggleHideInRecents = vm::setHideInRecents,
                            onLockNow = { container.appLockController.lockNow() },
                            onExportBackup = vm::exportBackup,
                            onImportBackup = vm::importBackup,
                            onClearMessages = vm::clearMessages,
                        )
                    }
                }
            }
        }
    }
}
