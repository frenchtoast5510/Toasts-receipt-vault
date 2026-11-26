package com.frenchtoast.receiptvault.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.frenchtoast.receiptvault.data.VaultRepository
import com.frenchtoast.receiptvault.data.preferences.VaultSettings
import com.frenchtoast.receiptvault.export.ClaimPacketGenerator
import com.frenchtoast.receiptvault.model.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DetailUiState(
    val item: VaultItem? = null,
    val isLoading: Boolean = true,
    val returnLeadDays: Int = 3,
    val warrantyLeadDays: Int = 14,
    val isDeleted: Boolean = false,
)

class DetailViewModel(
    private val itemId: String,
    private val repository: VaultRepository,
    private val claimPacketGenerator: ClaimPacketGenerator,
) : ViewModel() {

    private val deletedFlow = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeItem(itemId),
        repository.settingsFlow,
        deletedFlow,
    ) { item: VaultItem?, settings: VaultSettings, deleted: Boolean ->
        DetailUiState(
            item = item,
            isLoading = false,
            returnLeadDays = settings.returnLeadDays,
            warrantyLeadDays = settings.warrantyLeadDays,
            isDeleted = deleted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(),
    )

    fun deleteItem() {
        viewModelScope.launch {
            repository.deleteItem(itemId)
            deletedFlow.value = true
        }
    }

    suspend fun buildShareOriginalFilesIntent(item: VaultItem): Intent? = withContext(Dispatchers.IO) {
        claimPacketGenerator.buildShareOriginalFilesIntent(item)
    }

    fun buildShareSummaryIntent(item: VaultItem): Intent {
        return claimPacketGenerator.buildShareSummaryIntent(item)
    }

    suspend fun buildSharePacketPdfIntent(item: VaultItem): Intent = withContext(Dispatchers.IO) {
        claimPacketGenerator.buildSharePacketPdfIntent(item)
    }

    class Factory(
        private val itemId: String,
        private val repository: VaultRepository,
        private val claimPacketGenerator: ClaimPacketGenerator,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailViewModel(itemId, repository, claimPacketGenerator) as T
        }
    }
}
