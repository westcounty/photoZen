package com.example.photozen.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * UI State for Settings screen.
 */
data class SettingsUiState(
    val totalSorted: Int = 0,
    val error: String? = null
)

/**
 * Internal state for non-flow data.
 */
private data class InternalState(
    val error: String? = null
)

/**
 * ViewModel for Settings screen.
 * Uses cumulative sort count from DataStore (persists even when photos are deleted).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _internalState = MutableStateFlow(InternalState())
    
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.getTotalSortedCount(),
        _internalState
    ) { totalSorted, internal ->
        SettingsUiState(
            totalSorted = totalSorted,
            error = internal.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )
    
    fun clearError() {
        _internalState.update { it.copy(error = null) }
    }
}
