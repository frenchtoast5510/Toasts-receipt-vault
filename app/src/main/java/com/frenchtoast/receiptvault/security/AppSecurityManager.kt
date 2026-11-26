package com.frenchtoast.receiptvault.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.frenchtoast.receiptvault.data.preferences.AppLockMode
import com.frenchtoast.receiptvault.data.preferences.AutoLockTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

object AppSecurityManager {

    private const val SALT_BYTES = 16

    fun hashSecret(secret: String): Pair<String, String> {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val digest = computeSaltedSha256(salt, secret.toByteArray(Charsets.UTF_8))
        return bytesToHex(salt) to bytesToHex(digest)
    }

    fun verifySecret(secret: String, saltHex: String, expectedHashHex: String): Boolean {
        if (saltHex.isBlank() || expectedHashHex.isBlank()) return false
        return runCatching {
            val salt = hexToBytes(saltHex)
            val expected = hexToBytes(expectedHashHex)
            val actual = computeSaltedSha256(salt, secret.toByteArray(Charsets.UTF_8))
            MessageDigest.isEqual(actual, expected)
        }.getOrDefault(false)
    }

    private fun computeSaltedSha256(salt: ByteArray, secretBytes: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(secretBytes)
        return md.digest()
    }

    private fun bytesToHex(bytes: ByteArray): String = buildString(bytes.size * 2) {
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            append(HEX_DIGITS[v ushr 4])
            append(HEX_DIGITS[v and 0x0F])
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Invalid hex length" }
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(hex[i * 2], 16)
            val lo = Character.digit(hex[i * 2 + 1], 16)
            require(hi >= 0 && lo >= 0) { "Invalid hex character" }
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    private const val HEX_DIGITS = "0123456789abcdef"

    fun canUseBiometricOrDeviceLock(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val status = bm.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            if (status == BiometricManager.BIOMETRIC_SUCCESS) return true
        } else {
            val bioStatus = bm.canAuthenticate(BIOMETRIC_WEAK)
            if (bioStatus == BiometricManager.BIOMETRIC_SUCCESS) return true
        }
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isDeviceSecure == true
    }

    fun canUseBiometricsOnly(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticateWithDevicePrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        biometricsOnlyWithNegativeText: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFallbackKeyguardNeeded: () -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        onError(errString.toString())
                    }
                }
            },
        )

        if (biometricsOnlyWithNegativeText != null) {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                .setNegativeButtonText(biometricsOnlyWithNegativeText)
                .build()
            prompt.authenticate(info)
            return
        }

        val bm = BiometricManager.from(activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            if (bm.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(authenticators)
                    .build()
                prompt.authenticate(info)
            } else {
                onError("No biometric or screen lock credential is enrolled on this device.")
            }
        } else {
            // API 24-29: BiometricPrompt does not support BIOMETRIC_STRONG | DEVICE_CREDENTIAL directly
            @Suppress("DEPRECATION")
            val canBioOrCred = bm.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
            val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (canBioOrCred) {
                @Suppress("DEPRECATION")
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDeviceCredentialAllowed(true)
                    .build()
                prompt.authenticate(info)
            } else if (km?.isDeviceSecure == true) {
                onFallbackKeyguardNeeded()
            } else {
                onError("Set up a screen lock or biometric in system settings first.")
            }
        }
    }
}

class AppLockController {
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var initialized = false
    private var backgroundedAtElapsed: Long? = null
    private var suppressRequestedAtElapsed: Long = 0L
    private var suppressedForCurrentBackground: Boolean = false

    fun initializeOnLaunch(lockMode: AppLockMode) {
        if (!initialized) {
            initialized = true
            _isLocked.value = lockMode != AppLockMode.NONE
        }
    }

    fun suppressNextBackgroundLock() {
        suppressRequestedAtElapsed = SystemClock.elapsedRealtime()
    }

    fun onAppStopped(lockMode: AppLockMode) {
        if (lockMode == AppLockMode.NONE) return
        val sinceRequest = SystemClock.elapsedRealtime() - suppressRequestedAtElapsed
        if (suppressRequestedAtElapsed > 0L && sinceRequest in 0L..5_000L) {
            suppressedForCurrentBackground = true
            suppressRequestedAtElapsed = 0L
            return
        }
        suppressRequestedAtElapsed = 0L
        suppressedForCurrentBackground = false
        backgroundedAtElapsed = SystemClock.elapsedRealtime()
    }

    fun onAppStarted(lockMode: AppLockMode, timeout: AutoLockTimeout) {
        if (lockMode == AppLockMode.NONE) {
            _isLocked.value = false
            suppressRequestedAtElapsed = 0L
            suppressedForCurrentBackground = false
            backgroundedAtElapsed = null
            return
        }
        if (suppressedForCurrentBackground) {
            suppressedForCurrentBackground = false
            suppressRequestedAtElapsed = 0L
            backgroundedAtElapsed = null
            return
        }
        suppressRequestedAtElapsed = 0L
        val stoppedAt = backgroundedAtElapsed ?: return
        backgroundedAtElapsed = null
        val elapsed = SystemClock.elapsedRealtime() - stoppedAt
        if (elapsed >= timeout.durationMillis) {
            _isLocked.value = true
        }
    }

    fun lockNow() {
        _isLocked.value = true
    }

    fun unlock() {
        _isLocked.value = false
        backgroundedAtElapsed = null
        suppressRequestedAtElapsed = 0L
        suppressedForCurrentBackground = false
    }
}
