package com.frenchtoast.receiptvault.ui.components

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.frenchtoast.receiptvault.data.preferences.AppLockMode
import com.frenchtoast.receiptvault.data.preferences.VaultSettings
import com.frenchtoast.receiptvault.security.AppLockController
import com.frenchtoast.receiptvault.security.AppSecurityManager

@Composable
fun LockScreenOverlay(
    settings: VaultSettings,
    appLockController: AppLockController,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    var pinInput by remember { mutableStateOf("") }
    var showPinText by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val legacyKeyguardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onUnlocked()
        } else {
            errorMessage = "Authentication canceled."
        }
    }

    fun launchBiometricOrDeviceAuth() {
        val host = activity ?: return
        errorMessage = null
        appLockController.suppressNextBackgroundLock()
        AppSecurityManager.authenticateWithDevicePrompt(
            activity = host,
            title = "Unlock Toasts Receipt Vault",
            subtitle = "Verify your identity to access your offline vault",
            biometricsOnlyWithNegativeText = null,
            onSuccess = onUnlocked,
            onError = { msg -> errorMessage = msg },
            onFallbackKeyguardNeeded = {
                @Suppress("DEPRECATION")
                val km = host.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                @Suppress("DEPRECATION")
                val intent = km?.createConfirmDeviceCredentialIntent(
                    "Unlock Toasts Receipt Vault",
                    "Confirm your device screen lock",
                )
                if (intent != null) {
                    appLockController.suppressNextBackgroundLock()
                    legacyKeyguardLauncher.launch(intent)
                } else {
                    errorMessage = "Device screen lock unavailable."
                }
            },
        )
    }

    fun launchBiometricsForPinScreen() {
        val host = activity ?: return
        errorMessage = null
        appLockController.suppressNextBackgroundLock()
        AppSecurityManager.authenticateWithDevicePrompt(
            activity = host,
            title = "Unlock with Biometrics",
            subtitle = "Use fingerprint or face unlock",
            biometricsOnlyWithNegativeText = "Use App PIN",
            onSuccess = onUnlocked,
            onError = { msg -> errorMessage = msg },
        )
    }

    fun attemptCustomPinUnlock() {
        if (pinInput.isEmpty()) {
            errorMessage = "Enter your PIN or password."
            return
        }
        val ok = AppSecurityManager.verifySecret(
            secret = pinInput,
            saltHex = settings.customPinSalt,
            expectedHashHex = settings.customPinHash,
        )
        if (ok) {
            pinInput = ""
            errorMessage = null
            onUnlocked()
        } else {
            errorMessage = "Incorrect PIN or password."
        }
    }

    LaunchedEffect(settings.appLockMode) {
        if (settings.appLockMode == AppLockMode.BIOMETRIC_OR_DEVICE) {
            launchBiometricOrDeviceAuth()
        } else if (settings.appLockMode == AppLockMode.CUSTOM_PIN &&
            settings.allowBiometricWithCustomPin &&
            AppSecurityManager.canUseBiometricsOnly(context)
        ) {
            launchBiometricsForPinScreen()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Vault Locked",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                Text(
                    text = "Toasts Receipt Vault Locked",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (settings.appLockMode == AppLockMode.BIOMETRIC_OR_DEVICE) {
                        "Authenticate with fingerprint, face unlock, or your device screen lock."
                    } else {
                        "Enter your custom vault PIN or password to continue."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (settings.appLockMode == AppLockMode.CUSTOM_PIN) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            errorMessage = null
                        },
                        label = { Text("Vault PIN / Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        visualTransformation = if (showPinText) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPinText = !showPinText }) {
                                Icon(
                                    imageVector = if (showPinText) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = if (showPinText) "Hide PIN" else "Show PIN",
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { attemptCustomPinUnlock() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lock_screen_pin_input"),
                    )

                    Button(
                        onClick = { attemptCustomPinUnlock() },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("lock_screen_unlock_button"),
                    ) {
                        Text("Unlock Vault")
                    }

                    if (settings.allowBiometricWithCustomPin && AppSecurityManager.canUseBiometricsOnly(context)) {
                        FilledTonalButton(
                            onClick = { launchBiometricsForPinScreen() },
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Use Fingerprint / Face")
                        }
                    }
                } else {
                    Button(
                        onClick = { launchBiometricOrDeviceAuth() },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("lock_screen_biometric_button"),
                    ) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock with Device Lock")
                    }
                }

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
