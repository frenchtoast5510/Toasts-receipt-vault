package com.frenchtoast.receiptvault.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "toasts_vault_prefs",
)

enum class AppLockMode(val label: String) {
    NONE("None"),
    BIOMETRIC_OR_DEVICE("Biometrics & Screen Lock"),
    CUSTOM_PIN("Custom PIN / Password"),
}

enum class AutoLockTimeout(val label: String, val durationMillis: Long) {
    IMMEDIATE("Immediately when leaving app", 0L),
    ONE_MINUTE("After 1 minute", 60_000L),
    FIVE_MINUTES("After 5 minutes", 300_000L),
}

data class VaultSettings(
    val demoModeEnabled: Boolean = true,
    val demoSeededOnce: Boolean = false,
    val remindersEnabled: Boolean = false,
    val returnLeadDays: Int = 3,
    val warrantyLeadDays: Int = 14,
    val lastReminderEpochDay: Long = -1L,
    val appLockMode: AppLockMode = AppLockMode.NONE,
    val customPinSalt: String = "",
    val customPinHash: String = "",
    val allowBiometricWithCustomPin: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.IMMEDIATE,
    val hideInRecents: Boolean = false,
) {
    val hasCustomPinConfigured: Boolean
        get() = customPinSalt.isNotEmpty() && customPinHash.isNotEmpty()
}

class VaultPreferences(private val context: Context) {

    private object Keys {
        val DEMO_MODE_ENABLED = booleanPreferencesKey("demo_mode_enabled")
        val DEMO_SEEDED_ONCE = booleanPreferencesKey("demo_seeded_once")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val RETURN_LEAD_DAYS = intPreferencesKey("return_lead_days")
        val WARRANTY_LEAD_DAYS = intPreferencesKey("warranty_lead_days")
        val LAST_REMINDER_EPOCH_DAY = longPreferencesKey("last_reminder_epoch_day")
        val APP_LOCK_MODE = stringPreferencesKey("app_lock_mode")
        val CUSTOM_PIN_SALT = stringPreferencesKey("custom_pin_salt")
        val CUSTOM_PIN_HASH = stringPreferencesKey("custom_pin_hash")
        val ALLOW_BIOMETRIC_WITH_PIN = booleanPreferencesKey("allow_biometric_with_pin")
        val AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout")
        val HIDE_IN_RECENTS = booleanPreferencesKey("hide_in_recents")
    }

    val settingsFlow: Flow<VaultSettings> = context.vaultDataStore.data.map { prefs ->
        VaultSettings(
            demoModeEnabled = prefs[Keys.DEMO_MODE_ENABLED] ?: true,
            demoSeededOnce = prefs[Keys.DEMO_SEEDED_ONCE] ?: false,
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: false,
            returnLeadDays = prefs[Keys.RETURN_LEAD_DAYS] ?: 3,
            warrantyLeadDays = prefs[Keys.WARRANTY_LEAD_DAYS] ?: 14,
            lastReminderEpochDay = prefs[Keys.LAST_REMINDER_EPOCH_DAY] ?: -1L,
            appLockMode = runCatching {
                AppLockMode.valueOf(prefs[Keys.APP_LOCK_MODE] ?: AppLockMode.NONE.name)
            }.getOrDefault(AppLockMode.NONE),
            customPinSalt = prefs[Keys.CUSTOM_PIN_SALT] ?: "",
            customPinHash = prefs[Keys.CUSTOM_PIN_HASH] ?: "",
            allowBiometricWithCustomPin = prefs[Keys.ALLOW_BIOMETRIC_WITH_PIN] ?: false,
            autoLockTimeout = runCatching {
                AutoLockTimeout.valueOf(prefs[Keys.AUTO_LOCK_TIMEOUT] ?: AutoLockTimeout.IMMEDIATE.name)
            }.getOrDefault(AutoLockTimeout.IMMEDIATE),
            hideInRecents = prefs[Keys.HIDE_IN_RECENTS] ?: false,
        )
    }

    suspend fun getSnapshot(): VaultSettings = settingsFlow.first()

    suspend fun setDemoModeEnabled(enabled: Boolean) {
        context.vaultDataStore.edit { it[Keys.DEMO_MODE_ENABLED] = enabled }
    }

    suspend fun markDemoSeededOnce() {
        context.vaultDataStore.edit {
            it[Keys.DEMO_SEEDED_ONCE] = true
            it[Keys.DEMO_MODE_ENABLED] = true
        }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.vaultDataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    suspend fun setLeadDays(returnDays: Int, warrantyDays: Int) {
        context.vaultDataStore.edit {
            it[Keys.RETURN_LEAD_DAYS] = returnDays.coerceIn(1, 30)
            it[Keys.WARRANTY_LEAD_DAYS] = warrantyDays.coerceIn(1, 90)
        }
    }

    suspend fun setLastReminderEpochDay(epochDay: Long) {
        context.vaultDataStore.edit { it[Keys.LAST_REMINDER_EPOCH_DAY] = epochDay }
    }

    suspend fun setAppLockMode(mode: AppLockMode) {
        context.vaultDataStore.edit { it[Keys.APP_LOCK_MODE] = mode.name }
    }

    suspend fun saveCustomPin(saltBase64: String, hashBase64: String) {
        context.vaultDataStore.edit {
            it[Keys.CUSTOM_PIN_SALT] = saltBase64
            it[Keys.CUSTOM_PIN_HASH] = hashBase64
            it[Keys.APP_LOCK_MODE] = AppLockMode.CUSTOM_PIN.name
        }
    }

    suspend fun clearCustomPin() {
        context.vaultDataStore.edit {
            it[Keys.CUSTOM_PIN_SALT] = ""
            it[Keys.CUSTOM_PIN_HASH] = ""
            it[Keys.ALLOW_BIOMETRIC_WITH_PIN] = false
        }
    }

    suspend fun setAllowBiometricWithCustomPin(allowed: Boolean) {
        context.vaultDataStore.edit { it[Keys.ALLOW_BIOMETRIC_WITH_PIN] = allowed }
    }

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        context.vaultDataStore.edit { it[Keys.AUTO_LOCK_TIMEOUT] = timeout.name }
    }

    suspend fun setHideInRecents(hide: Boolean) {
        context.vaultDataStore.edit { it[Keys.HIDE_IN_RECENTS] = hide }
    }
}
