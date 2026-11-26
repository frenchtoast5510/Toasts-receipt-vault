package com.frenchtoast.receiptvault.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.frenchtoast.receiptvault.data.preferences.AppLockMode
import com.frenchtoast.receiptvault.data.preferences.AutoLockTimeout
import com.frenchtoast.receiptvault.model.VaultDate
import com.frenchtoast.receiptvault.security.AppLockController
import com.frenchtoast.receiptvault.security.AppSecurityManager
import com.frenchtoast.receiptvault.ui.viewmodel.RemindersBackupUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersBackupScreen(
    uiState: RemindersBackupUiState,
    appLockController: AppLockController,
    onToggleReminders: (Boolean) -> Unit,
    onUpdateLeadDays: (Int, Int) -> Unit,
    onRunImmediateCheck: () -> Unit,
    onSelectAppLockMode: (AppLockMode) -> Unit,
    onSaveCustomPin: (String, Boolean) -> Unit,
    onToggleBiometricWithPin: (Boolean) -> Unit,
    onSelectAutoLockTimeout: (AutoLockTimeout) -> Unit,
    onToggleHideInRecents: (Boolean) -> Unit,
    onLockNow: () -> Unit,
    onExportBackup: (Uri, String) -> Unit,
    onImportBackup: (Uri, String, Boolean) -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingExportPassword by remember { mutableStateOf<String?>(null) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }

    var showCustomPinSetupDialog by remember { mutableStateOf(false) }

    val deviceSupportsBioOrScreenLock = remember {
        AppSecurityManager.canUseBiometricOrDeviceLock(context)
    }
    val deviceSupportsBiometricsOnly = remember {
        AppSecurityManager.canUseBiometricsOnly(context)
    }

    LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
        val msg = uiState.errorMessage ?: uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onClearMessages()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onToggleReminders(true)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Notification permission denied. Enable it in system settings for alerts.")
            }
        }
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val password = pendingExportPassword
        pendingExportPassword = null
        if (uri != null && !password.isNullOrEmpty()) {
            onExportBackup(uri, password)
        }
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportPasswordDialog = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                )

                if (uiState.isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // App Security Section
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column {
                                Text("App Security", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Optional local lock using biometrics, device lock, or a custom vault PIN.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        LockModeOptionRow(
                            selected = uiState.settings.appLockMode == AppLockMode.NONE,
                            title = "None (Default)",
                            subtitle = "Opens straight away using only your normal phone lock.",
                            testTag = "lock_mode_none",
                            onClick = { onSelectAppLockMode(AppLockMode.NONE) },
                        )

                        LockModeOptionRow(
                            selected = uiState.settings.appLockMode == AppLockMode.BIOMETRIC_OR_DEVICE,
                            title = "Phone Biometrics & Screen Lock",
                            subtitle = if (deviceSupportsBioOrScreenLock) {
                                "Unlock with fingerprint, face unlock, PIN, pattern, or device password."
                            } else {
                                "Requires a screen lock or biometric enrolled in Android system settings."
                            },
                            testTag = "lock_mode_biometric",
                            onClick = {
                                if (deviceSupportsBioOrScreenLock) {
                                    onSelectAppLockMode(AppLockMode.BIOMETRIC_OR_DEVICE)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "No device screen lock or biometric is set up on this phone.",
                                        )
                                    }
                                }
                            },
                        )

                        LockModeOptionRow(
                            selected = uiState.settings.appLockMode == AppLockMode.CUSTOM_PIN,
                            title = "Custom App PIN / Password",
                            subtitle = if (uiState.settings.hasCustomPinConfigured) {
                                "Custom PIN/password is active (stored locally as salted SHA-256)."
                            } else {
                                "Set a dedicated PIN or password just for Toasts Receipt Vault."
                            },
                            testTag = "lock_mode_custom_pin",
                            onClick = {
                                if (uiState.settings.hasCustomPinConfigured) {
                                    onSelectAppLockMode(AppLockMode.CUSTOM_PIN)
                                } else {
                                    showCustomPinSetupDialog = true
                                }
                            },
                        )

                        if (uiState.settings.appLockMode == AppLockMode.CUSTOM_PIN) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                FilledTonalButton(
                                    onClick = { showCustomPinSetupDialog = true },
                                    shape = CircleShape,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Change PIN / Password")
                                }
                            }

                            if (deviceSupportsBiometricsOnly) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Fingerprint,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            text = "Also allow fingerprint / face unlock",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Switch(
                                        checked = uiState.settings.allowBiometricWithCustomPin,
                                        onCheckedChange = onToggleBiometricWithPin,
                                    )
                                }
                            }
                        }

                        if (uiState.settings.appLockMode != AppLockMode.NONE) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Auto-Lock Timeout",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    AutoLockTimeout.entries.forEach { timeout ->
                                        FilterChip(
                                            selected = uiState.settings.autoLockTimeout == timeout,
                                            onClick = { onSelectAutoLockTimeout(timeout) },
                                            shape = CircleShape,
                                            label = { Text(timeout.label) },
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = onLockNow,
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lock_vault_now_button"),
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Lock Vault Now")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hide preview in recent apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "Blocks screenshots and blurs/hides app thumbnail in Recents (FLAG_SECURE).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = uiState.settings.hideInRecents,
                                onCheckedChange = onToggleHideInRecents,
                                modifier = Modifier.testTag("hide_recents_switch"),
                            )
                        }
                    }
                }

                // Local Reminders Section
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Column {
                                    Text("Local Deadline Alerts", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Daily offline WorkManager check for returns & expirations",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = uiState.settings.remindersEnabled,
                                onCheckedChange = { enable ->
                                    if (!enable) {
                                        onToggleReminders(false)
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            onToggleReminders(true)
                                        } else {
                                            appLockController.suppressNextBackgroundLock()
                                            notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS,
                                            )
                                        }
                                    } else {
                                        onToggleReminders(true)
                                    }
                                },
                                modifier = Modifier.testTag("reminders_toggle_switch"),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Return Window Lead Days",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                listOf(1, 3, 5, 7, 14).forEach { days ->
                                    FilterChip(
                                        selected = uiState.settings.returnLeadDays == days,
                                        onClick = {
                                            onUpdateLeadDays(days, uiState.settings.warrantyLeadDays)
                                        },
                                        shape = CircleShape,
                                        label = { Text("${days}d") },
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Warranty / Document Expiration Lead Days",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                listOf(7, 14, 30, 60).forEach { days ->
                                    FilterChip(
                                        selected = uiState.settings.warrantyLeadDays == days,
                                        onClick = {
                                            onUpdateLeadDays(uiState.settings.returnLeadDays, days)
                                        },
                                        shape = CircleShape,
                                        label = { Text("${days}d") },
                                    )
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    appLockController.suppressNextBackgroundLock()
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onRunImmediateCheck()
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("check_deadlines_button"),
                        ) {
                            Text("Check Deadlines Now")
                        }
                    }
                }

                // Encrypted Backup & Restore Section
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column {
                                Text("Encrypted Vault Backup", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Password-protected AES-256-GCM archive (PBKDF2-HMAC-SHA256) with Room records, photos, and PDFs via SAF.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { showExportPasswordDialog = true },
                                enabled = !uiState.isBusy,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export")
                            }

                            OutlinedButton(
                                onClick = {
                                    appLockController.suppressNextBackgroundLock()
                                    importDocumentLauncher.launch(arrayOf("*/*"))
                                },
                                enabled = !uiState.isBusy,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Import")
                            }
                        }
                    }
                }

                // App Info & Privacy Section
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text("Local-Only Storage & Permissions", style = MaterialTheme.typography.titleMedium)
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "100% Offline • Zero Cloud Accounts • Zero Telemetry • Zero Ads • No Internet Permission",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(12.dp),
                            )
                        }

                        val formattedSize = Formatter.formatShortFileSize(context, uiState.storageBytes)
                        Text(
                            text = "Stored locally: ${uiState.itemCount} item(s) • $formattedSize of photos & documents in app-private storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        PermissionExplanationRow(
                            title = "Camera (Optional)",
                            detail = "Requested only when you tap Camera to capture a receipt or serial label.",
                        )
                        PermissionExplanationRow(
                            title = "Notifications (Optional, Android 13+)",
                            detail = "Requested only if you enable local WorkManager deadline reminders.",
                        )
                        PermissionExplanationRow(
                            title = "Photo Picker & Storage Access Framework",
                            detail = "Zero storage permissions required. You explicitly pick photos, PDFs, or backup files.",
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showCustomPinSetupDialog) {
        CustomPinSetupDialog(
            canUseBiometrics = deviceSupportsBiometricsOnly,
            initialAllowBiometric = uiState.settings.allowBiometricWithCustomPin,
            onDismiss = { showCustomPinSetupDialog = false },
            onSave = { pin, allowBio ->
                showCustomPinSetupDialog = false
                onSaveCustomPin(pin, allowBio)
            },
        )
    }

    if (showExportPasswordDialog) {
        ExportPasswordDialog(
            onDismiss = { showExportPasswordDialog = false },
            onConfirm = { password ->
                showExportPasswordDialog = false
                pendingExportPassword = password
                val dateLabel = VaultDate.formatIso(VaultDate.todayEpochDay())
                appLockController.suppressNextBackgroundLock()
                exportDocumentLauncher.launch("toasts-receipt-vault-$dateLabel.trv")
            },
        )
    }

    if (showImportPasswordDialog && pendingImportUri != null) {
        ImportPasswordDialog(
            onDismiss = {
                showImportPasswordDialog = false
                pendingImportUri = null
            },
            onConfirm = { password, replaceExisting ->
                val uri = pendingImportUri
                showImportPasswordDialog = false
                pendingImportUri = null
                if (uri != null) {
                    onImportBackup(uri, password, replaceExisting)
                }
            },
        )
    }
}

@Composable
private fun LockModeOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CustomPinSetupDialog(
    canUseBiometrics: Boolean,
    initialAllowBiometric: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var allowBio by remember { mutableStateOf(initialAllowBiometric) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Vault PIN / Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Stored locally as a salted SHA-256 hash. Minimum 4 characters.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; error = null },
                    label = { Text("New PIN / Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it; error = null },
                    label = { Text("Confirm PIN / Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (canUseBiometrics) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Also allow fingerprint / face",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = allowBio, onCheckedChange = { allowBio = it })
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        pin.length < 4 -> error = "Enter at least 4 characters."
                        pin != confirm -> error = "Entries do not match."
                        else -> onSave(pin, allowBio)
                    }
                },
            ) {
                Text("Save Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun PermissionExplanationRow(
    title: String,
    detail: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelMedium)
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExportPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encrypt Backup Archive") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose a password (minimum 6 characters). Keep it safe — backups cannot be decrypted without it.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it; error = null },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        password.length < 6 -> error = "Password must be at least 6 characters."
                        password != confirm -> error = "Passwords do not match."
                        else -> onConfirm(password)
                    }
                },
            ) {
                Text("Choose File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ImportPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String, replaceExisting: Boolean) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var replaceExisting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Decrypt & Restore Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Backup Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Restore Mode", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !replaceExisting, onClick = { replaceExisting = false })
                    Text("Merge with current vault", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = replaceExisting, onClick = { replaceExisting = true })
                    Text("Replace entire vault", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (password.isNotEmpty()) onConfirm(password, replaceExisting) },
            ) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
