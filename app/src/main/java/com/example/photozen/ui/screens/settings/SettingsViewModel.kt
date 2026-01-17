package com.example.photozen.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.repository.DailyTaskMode
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.Album
import com.example.photozen.data.source.MediaStoreDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.photozen.util.AlarmScheduler
import com.example.photozen.util.WidgetUpdater

import com.example.photozen.data.repository.WidgetPhotoSource
import com.example.photozen.data.repository.ThemeMode
import com.example.photozen.data.repository.PhotoClassificationMode
import com.example.photozen.data.repository.AlbumAddAction
import com.example.photozen.util.StoragePermissionHelper

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
    val widgetStartDate: Long? = null,
    val widgetEndDate: Long? = null,
    val cardZoomEnabled: Boolean = true,
    val onestopEnabled: Boolean = false,
    val experimentalEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val swipeSensitivity: Float = 1.0f,
    // Photo classification mode settings
    val photoClassificationMode: PhotoClassificationMode = PhotoClassificationMode.TAG,
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val cardSortingAlbumEnabled: Boolean = false,
    val albumTagSize: Float = 1.0f,
    val maxAlbumTagCount: Int = 0,  // 0 = unlimited
    val hasManageStoragePermission: Boolean = false,
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
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: WidgetUpdater,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val photoRepository: PhotoRepository,
    private val storagePermissionHelper: StoragePermissionHelper
) : ViewModel() {
    
    private val _internalState = MutableStateFlow(InternalState())
    
    // Albums list for widget settings
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()
    
    init {
        loadAlbums()
    }
    
    private fun loadAlbums() {
        viewModelScope.launch {
            try {
                _albums.value = mediaStoreDataSource.getAllAlbums()
            } catch (e: Exception) {
                _internalState.update { it.copy(error = "加载相册失败: ${e.message}") }
            }
        }
    }
    
    // Combine widget settings separately
    private val widgetSettingsFlow = combine(
        preferencesRepository.getWidgetPhotoSource(),
        preferencesRepository.getWidgetCustomAlbumIds(),
        preferencesRepository.getWidgetDateRange()
    ) { source, albumIds, dateRange ->
        Triple(source, albumIds, dateRange)
    }
    
    // Combine extra settings
    private val extraSettingsFlow = combine(
        preferencesRepository.getCardZoomEnabled(),
        preferencesRepository.getOnestopEnabled(),
        preferencesRepository.getExperimentalEnabled()
    ) { cardZoom, onestop, experimental -> Triple(cardZoom, onestop, experimental) }
    
    // Combine appearance and swipe settings
    private val appearanceSettingsFlow = combine(
        preferencesRepository.getThemeMode(),
        preferencesRepository.getSwipeSensitivity()
    ) { themeMode, sensitivity -> Pair(themeMode, sensitivity) }
    
    // Combine photo classification mode settings
    private val classificationSettingsFlow = combine(
        preferencesRepository.getPhotoClassificationMode(),
        preferencesRepository.getAlbumAddAction(),
        preferencesRepository.getCardSortingAlbumEnabled(),
        preferencesRepository.getAlbumTagSize(),
        preferencesRepository.getMaxAlbumTagCount()
    ) { mode, action, cardEnabled, tagSize, maxCount ->
        ClassificationSettings(mode, action, cardEnabled, tagSize, maxCount)
    }
    
    // Data class for classification settings
    private data class ClassificationSettings(
        val mode: PhotoClassificationMode,
        val action: AlbumAddAction,
        val cardSortingEnabled: Boolean,
        val tagSize: Float,
        val maxCount: Int
    )
    
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.getTotalSortedCount(),
        preferencesRepository.getPhotoFilterMode(),
        preferencesRepository.getDailyTaskEnabled(),
        preferencesRepository.getDailyTaskTarget(),
        preferencesRepository.getDailyTaskMode(),
        preferencesRepository.getDailyReminderEnabled(),
        preferencesRepository.getDailyReminderTime(),
        widgetSettingsFlow,
        extraSettingsFlow,
        combine(_internalState, appearanceSettingsFlow, classificationSettingsFlow) { internal, appearance, classification -> 
            Triple(internal, appearance, classification) 
        }
    ) { params ->
        val totalSorted = params[0] as Int
        val filterMode = params[1] as PhotoFilterMode
        val dailyEnabled = params[2] as Boolean
        val dailyTarget = params[3] as Int
        val dailyMode = params[4] as DailyTaskMode
        val reminderEnabled = params[5] as Boolean
        @Suppress("UNCHECKED_CAST")
        val reminderTime = params[6] as Pair<Int, Int>
        @Suppress("UNCHECKED_CAST")
        val widgetSettings = params[7] as Triple<WidgetPhotoSource, Set<String>, Pair<Long?, Long?>>
        @Suppress("UNCHECKED_CAST")
        val extraSettings = params[8] as Triple<Boolean, Boolean, Boolean>
        @Suppress("UNCHECKED_CAST")
        val combined = params[9] as Triple<InternalState, Pair<ThemeMode, Float>, ClassificationSettings>
        val internal = combined.first
        val appearanceSettings = combined.second
        val classificationSettings = combined.third
        
        SettingsUiState(
            totalSorted = totalSorted,
            photoFilterMode = filterMode,
            dailyTaskEnabled = dailyEnabled,
            dailyTaskTarget = dailyTarget,
            dailyTaskMode = dailyMode,
            dailyReminderEnabled = reminderEnabled,
            dailyReminderTime = reminderTime,
            widgetPhotoSource = widgetSettings.first,
            widgetCustomAlbumIds = widgetSettings.second,
            widgetStartDate = widgetSettings.third.first,
            widgetEndDate = widgetSettings.third.second,
            cardZoomEnabled = extraSettings.first,
            onestopEnabled = extraSettings.second,
            experimentalEnabled = extraSettings.third,
            themeMode = appearanceSettings.first,
            swipeSensitivity = appearanceSettings.second,
            photoClassificationMode = classificationSettings.mode,
            albumAddAction = classificationSettings.action,
            cardSortingAlbumEnabled = classificationSettings.cardSortingEnabled,
            albumTagSize = classificationSettings.tagSize,
            maxAlbumTagCount = classificationSettings.maxCount,
            hasManageStoragePermission = storagePermissionHelper.hasManageStoragePermission(),
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
            // Update widgets to reflect change
            widgetUpdater.updateDailyProgressWidgets()
        }
    }
    
    fun setDailyTaskTarget(target: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyTaskTarget(target)
            // Also update the DailyStats table for today to keep target in sync
            photoRepository.updateDailyStatsTarget(target)
            // Update widgets to reflect new target
            widgetUpdater.updateDailyProgressWidgets()
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
    
    
    fun setOnestopEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setOnestopEnabled(enabled)
        }
    }
    
    fun setExperimentalEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setExperimentalEnabled(enabled)
        }
    }
    
    /**
     * Set theme mode (DARK, LIGHT, SYSTEM).
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }
    
    /**
     * Set swipe sensitivity for card sorting.
     * @param sensitivity Value between 0.5 (very sensitive) and 1.5 (less sensitive)
     */
    fun setSwipeSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            preferencesRepository.setSwipeSensitivity(sensitivity)
        }
    }
    
    fun clearError() {
        _internalState.update { it.copy(error = null) }
    }
    
    // ==================== PHOTO CLASSIFICATION MODE SETTINGS ====================
    
    /**
     * Set photo classification mode (TAG or ALBUM).
     */
    fun setPhotoClassificationMode(mode: PhotoClassificationMode) {
        viewModelScope.launch {
            preferencesRepository.setPhotoClassificationMode(mode)
        }
    }
    
    /**
     * Set album add action (COPY or MOVE).
     */
    fun setAlbumAddAction(action: AlbumAddAction) {
        viewModelScope.launch {
            preferencesRepository.setAlbumAddAction(action)
        }
    }
    
    /**
     * Set whether card sorting album classification is enabled.
     */
    fun setCardSortingAlbumEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setCardSortingAlbumEnabled(enabled)
        }
    }
    
    /**
     * Set album tag size for card sorting.
     */
    fun setAlbumTagSize(size: Float) {
        viewModelScope.launch {
            preferencesRepository.setAlbumTagSize(size)
        }
    }
    
    /**
     * Set max album tag count for card sorting.
     */
    fun setMaxAlbumTagCount(count: Int) {
        viewModelScope.launch {
            preferencesRepository.setMaxAlbumTagCount(count)
        }
    }
    
    /**
     * Check if MANAGE_EXTERNAL_STORAGE permission is needed.
     */
    fun isManageStoragePermissionApplicable(): Boolean {
        return storagePermissionHelper.isManageStoragePermissionApplicable()
    }
    
    /**
     * Refresh permission status (call after returning from settings).
     */
    fun refreshPermissionStatus() {
        _internalState.update { it }  // Trigger recomposition
    }
}
