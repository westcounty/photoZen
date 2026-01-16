package com.example.photozen.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.repository.DailyTaskMode
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.photozen.util.AlarmScheduler

import com.example.photozen.data.repository.WidgetPhotoSource

/**
 * UI State for Settings screen.
 */
data class SettingsUiState(
    val totalSorted: Int = 0,
    val photoFilterMode: PhotoFilterMode = PhotoFilterMode.ALL,
    val dailyTaskEnabled: Boolean = true,
    val dailyTaskTarget: Int = 100,
    val dailyTaskMode: DailyTaskMode = DailyTaskMode.FLOW,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: Pair<Int, Int> = Pair(20, 0),
    val widgetPhotoSource: WidgetPhotoSource = WidgetPhotoSource.ALL,
    val widgetCustomAlbumIds: Set<String> = emptySet(),
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
    private val preferencesRepository: PreferencesRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    
    private val _internalState = MutableStateFlow(InternalState())
    
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.getTotalSortedCount(),
        preferencesRepository.getPhotoFilterMode(),
        preferencesRepository.getDailyTaskEnabled(),
        preferencesRepository.getDailyTaskTarget(),
        preferencesRepository.getDailyTaskMode(),
        preferencesRepository.getDailyReminderEnabled(),
        preferencesRepository.getDailyReminderTime(),
        preferencesRepository.getWidgetPhotoSource(),
        preferencesRepository.getWidgetCustomAlbumIds(),
        _internalState
    ) { params ->
        val totalSorted = params[0] as Int
        val filterMode = params[1] as PhotoFilterMode
        val dailyEnabled = params[2] as Boolean
        val dailyTarget = params[3] as Int
        val dailyMode = params[4] as DailyTaskMode
        val reminderEnabled = params[5] as Boolean
        val reminderTime = params[6] as Pair<Int, Int>
        val widgetSource = params[7] as WidgetPhotoSource
        val widgetAlbumIds = params[8] as Set<String>
        val internal = params[9] as InternalState
        
        SettingsUiState(
            totalSorted = totalSorted,
            photoFilterMode = filterMode,
            dailyTaskEnabled = dailyEnabled,
            dailyTaskTarget = dailyTarget,
            dailyTaskMode = dailyMode,
            dailyReminderEnabled = reminderEnabled,
            dailyReminderTime = reminderTime,
            widgetPhotoSource = widgetSource,
            widgetCustomAlbumIds = widgetAlbumIds,
            error = internal.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )
    
    /**
     * Set photo filter mode.
     */
    fun setPhotoFilterMode(mode: PhotoFilterMode) {
        viewModelScope.launch {
            preferencesRepository.setPhotoFilterMode(mode)
        }
    }
    
    fun setDailyTaskEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDailyTaskEnabled(enabled)
        }
    }
    
    fun setDailyTaskTarget(target: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyTaskTarget(target)
        }
    }
    
    fun setDailyTaskMode(mode: DailyTaskMode) {
        viewModelScope.launch {
            preferencesRepository.setDailyTaskMode(mode)
        }
    }
    
    fun setDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDailyReminderEnabled(enabled)
            if (enabled) {
                val time = preferencesRepository.getDailyReminderTime().first()
                alarmScheduler.scheduleDailyReminder(time.first, time.second)
            } else {
                alarmScheduler.cancelDailyReminder()
            }
        }
    }
    
    fun setDailyReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyReminderTime(hour, minute)
            val enabled = preferencesRepository.getDailyReminderEnabled().first()
            if (enabled) {
                alarmScheduler.scheduleDailyReminder(hour, minute)
            }
        }
    }
    
    fun setWidgetPhotoSource(source: WidgetPhotoSource) {
        viewModelScope.launch {
            preferencesRepository.setWidgetPhotoSource(source)
        }
    }
    
    fun setWidgetCustomAlbumIds(albumIds: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.setWidgetCustomAlbumIds(albumIds)
        }
    }
    
    fun clearError() {
        _internalState.update { it.copy(error = null) }
    }
}
