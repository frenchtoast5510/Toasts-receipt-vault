package com.frenchtoast.receiptvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.frenchtoast.receiptvault.data.VaultRepository
import com.frenchtoast.receiptvault.data.preferences.VaultSettings
import com.frenchtoast.receiptvault.model.HomeFilterChip
import com.frenchtoast.receiptvault.model.VaultDate
import com.frenchtoast.receiptvault.model.VaultItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val allItemsCount: Int = 0,
    val filteredItems: List<VaultItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: HomeFilterChip = HomeFilterChip.ALL,
    val demoModeEnabled: Boolean = true,
    val hasDemoItems: Boolean = false,
    val returnLeadDays: Int = 3,
    val warrantyLeadDays: Int = 14,
)

class HomeViewModel(
    private val repository: VaultRepository,
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedFilterFlow = MutableStateFlow(HomeFilterChip.ALL)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.itemsFlow,
        repository.settingsFlow,
        searchQueryFlow,
        selectedFilterFlow,
    ) { items, settings, query, filter ->
        buildState(items, settings, query, filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    private fun buildState(
        items: List<VaultItem>,
        settings: VaultSettings,
        query: String,
        filter: HomeFilterChip,
    ): HomeUiState {
        val today = VaultDate.todayEpochDay()
        val trimmedQuery = query.trim()
        val filtered = items.filter { item ->
            val matchesFilter = item.matchesHomeFilter(
                filter = filter,
                todayEpochDay = today,
                returnLeadDays = settings.returnLeadDays,
                warrantyLeadDays = settings.warrantyLeadDays,
            )
            val matchesQuery = trimmedQuery.isEmpty() ||
                item.title.contains(trimmedQuery, ignoreCase = true) ||
                item.storeName.contains(trimmedQuery, ignoreCase = true) ||
                item.serialNumber.contains(trimmedQuery, ignoreCase = true) ||
                item.notes.contains(trimmedQuery, ignoreCase = true) ||
                (item.documentDisplayName?.contains(trimmedQuery, ignoreCase = true) == true)
            matchesFilter && matchesQuery
        }

        return HomeUiState(
            allItemsCount = items.size,
            filteredItems = filtered,
            searchQuery = query,
            selectedFilter = filter,
            demoModeEnabled = settings.demoModeEnabled,
            hasDemoItems = items.any { it.isDemo },
            returnLeadDays = settings.returnLeadDays,
            warrantyLeadDays = settings.warrantyLeadDays,
        )
    }

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun selectFilter(filter: HomeFilterChip) {
        selectedFilterFlow.value = filter
    }

    fun toggleDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDemoModeEnabled(enabled)
        }
    }

    fun clearDemoToEmptyVault() {
        viewModelScope.launch {
            repository.clearDemoItemsToEmptyVault()
        }
    }

    fun resetSampleData() {
        viewModelScope.launch {
            repository.resetSampleData()
        }
    }

    class Factory(private val repository: VaultRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
