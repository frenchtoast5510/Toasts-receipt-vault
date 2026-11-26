package com.frenchtoast.receiptvault.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.frenchtoast.receiptvault.data.VaultRepository
import com.frenchtoast.receiptvault.data.preferences.AppLockMode
import com.frenchtoast.receiptvault.data.preferences.AutoLockTimeout
import com.frenchtoast.receiptvault.data.preferences.VaultSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemindersBackupUiState(
    val settings: VaultSettings = VaultSettings(),
    val itemCount: Int = 0,
    val storageBytes: Long = 0L,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class RemindersBackupViewModel(
    private val repository: VaultRepository,
) : ViewModel() {

    private val busyFlow = MutableStateFlow(false)
    private val messageFlow = MutableStateFlow<Pair<String?, String?>>(null to null)

    val uiState: StateFlow<RemindersBackupUiState> = combine(
        repository.settingsFlow,
        repository.itemsFlow,
        busyFlow,
        messageFlow,
    ) { settings, items, busy, (status, error) ->
        RemindersBackupUiState(
            settings = settings,
            itemCount = items.size,
            storageBytes = repository.localStorageBytes(),
            isBusy = busy,
            statusMessage = status,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemindersBackupUiState(),
    )

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRemindersEnabled(enabled)
            messageFlow.value = if (enabled) {
                "Daily local deadline checks enabled." to null
            } else {
                "Local deadline reminders paused." to null
            }
        }
    }

    fun updateLeadDays(returnDays: Int, warrantyDays: Int) {
        viewModelScope.launch {
            repository.updateLeadDays(returnDays, warrantyDays)
        }
    }

    fun runImmediateReminderCheck() {
        repository.triggerImmediateReminderCheck()
        messageFlow.value = "Queued immediate local deadline check." to null
    }

    fun setAppLockMode(mode: AppLockMode) {
        viewModelScope.launch {
            if (mode == AppLockMode.NONE) {
                repository.clearCustomPin()
            }
            repository.setAppLockMode(mode)
            messageFlow.value = "App lock updated: ${mode.label}" to null
        }
    }

    fun setCustomPin(secret: String, allowBiometrics: Boolean) {
        viewModelScope.launch {
            repository.configureCustomPin(secret)
            repository.setAllowBiometricWithCustomPin(allowBiometrics)
            messageFlow.value = "Custom app PIN / password saved locally." to null
        }
    }

    fun setAllowBiometricWithCustomPin(allowed: Boolean) {
        viewModelScope.launch {
            repository.setAllowBiometricWithCustomPin(allowed)
        }
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        viewModelScope.launch {
            repository.setAutoLockTimeout(timeout)
        }
    }

    fun setHideInRecents(hide: Boolean) {
        viewModelScope.launch {
            repository.setHideInRecents(hide)
        }
    }

    fun exportBackup(destinationUri: Uri, password: String) {
        viewModelScope.launch {
            busyFlow.value = true
            messageFlow.value = null to null
            val result = repository.exportEncryptedBackup(destinationUri, password)
            busyFlow.value = false
            result.onSuccess { count ->
                messageFlow.value = "Exported $count encrypted vault item(s)." to null
            }.onFailure { err ->
                messageFlow.value = null to (err.message ?: "Backup export failed.")
            }
        }
    }

    fun importBackup(sourceUri: Uri, password: String, replaceExisting: Boolean) {
        viewModelScope.launch {
            busyFlow.value = true
            messageFlow.value = null to null
            val result = repository.importEncryptedBackup(sourceUri, password, replaceExisting)
            busyFlow.value = false
            result.onSuccess { summary ->
                messageFlow.value =
                    "Restored ${summary.restoredItemsCount} item(s), ${summary.restoredImagesCount} image(s), ${summary.restoredDocsCount} doc(s)." to null
            }.onFailure { err ->
                messageFlow.value = null to (err.message ?: "Backup restore failed.")
            }
        }
    }

    fun clearMessages() {
        messageFlow.update { null to null }
    }

    class Factory(private val repository: VaultRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RemindersBackupViewModel(repository) as T
        }
    }
}
