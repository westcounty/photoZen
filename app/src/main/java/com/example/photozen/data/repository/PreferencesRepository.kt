package com.example.photozen.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Photo filter mode for sorting.
 */
enum class PhotoFilterMode {
    ALL,           // 整理全部照片
    CAMERA_ONLY,   // 仅整理相机照片
    EXCLUDE_CAMERA, // 排除相机照片
    CUSTOM         // 每次整理前选择
}

/**
 * Widget photo source.
 */
enum class WidgetPhotoSource {
    ALL,
    CAMERA,
    CUSTOM
}

/**
 * Daily Task Mode.
 */
enum class DailyTaskMode {
    FLOW,  // Heart Flow Mode (Stack)
    QUICK  // Quick Sorter Mode (Tinder-style)
}

/**
 * Theme Mode for app appearance.
 */
enum class ThemeMode {
    DARK,   // 深色模式
    LIGHT,  // 浅色模式
    SYSTEM  // 跟随系统
}

/**
 * Photo classification mode.
 * Determines whether photos are organized using tags or albums.
 */
enum class PhotoClassificationMode {
    TAG,    // 使用标签分类
    ALBUM   // 使用相册分类
}

/**
 * Default action when adding photos to an album.
 */
enum class AlbumAddAction {
    COPY,   // 复制到相册（保留原位置）
    MOVE    // 移动到相册（从原位置删除）
}

/**
 * Repository for managing app preferences using DataStore.
 * Handles persistent storage for settings like cumulative sort count and achievements.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        // Sort count
        val KEY_TOTAL_SORTED_COUNT = intPreferencesKey("total_sorted_count")
        
        // Photo filter settings
        val KEY_PHOTO_FILTER_MODE = stringPreferencesKey("photo_filter_mode")
        
        // Daily Task Settings
        val KEY_DAILY_TASK_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("daily_task_enabled")
        val KEY_DAILY_TASK_TARGET = intPreferencesKey("daily_task_target")
        val KEY_DAILY_TASK_MODE = stringPreferencesKey("daily_task_mode")
        val KEY_DAILY_REMINDER_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("daily_reminder_enabled")
        val KEY_DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val KEY_DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val KEY_PROGRESS_NOTIFICATION_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("progress_notification_enabled")
        
        // Widget Settings
        val KEY_WIDGET_PHOTO_SOURCE = stringPreferencesKey("widget_photo_source")
        val KEY_WIDGET_CUSTOM_ALBUM_IDS = androidx.datastore.preferences.core.stringSetPreferencesKey("widget_custom_album_ids")
        val KEY_WIDGET_START_DATE = longPreferencesKey("widget_start_date")
        val KEY_WIDGET_END_DATE = longPreferencesKey("widget_end_date")
        
        // Default external app for opening photos
        val KEY_DEFAULT_EXTERNAL_APP = stringPreferencesKey("default_external_app")
        
        // Bubble positions (JSON encoded map: tagId -> "x,y")
        val KEY_BUBBLE_POSITIONS = stringPreferencesKey("bubble_positions")
        
        // Grid column preferences for different screens
        val KEY_GRID_COLUMNS_KEEP = intPreferencesKey("grid_columns_keep")
        val KEY_GRID_COLUMNS_MAYBE = intPreferencesKey("grid_columns_maybe")
        val KEY_GRID_COLUMNS_TRASH = intPreferencesKey("grid_columns_trash")
        val KEY_GRID_COLUMNS_TAGGED = intPreferencesKey("grid_columns_tagged")
        val KEY_GRID_COLUMNS_FLOW = intPreferencesKey("grid_columns_flow")
        val KEY_GRID_COLUMNS_ALBUM = intPreferencesKey("grid_columns_album")
        val KEY_GRID_COLUMNS_TIMELINE = intPreferencesKey("grid_columns_timeline")

        // Grid mode keys (SQUARE vs WATERFALL)
        val KEY_GRID_MODE_KEEP = stringPreferencesKey("grid_mode_keep")
        val KEY_GRID_MODE_MAYBE = stringPreferencesKey("grid_mode_maybe")
        val KEY_GRID_MODE_TRASH = stringPreferencesKey("grid_mode_trash")
        val KEY_GRID_MODE_TAGGED = stringPreferencesKey("grid_mode_tagged")
        val KEY_GRID_MODE_FLOW = stringPreferencesKey("grid_mode_flow")
        val KEY_GRID_MODE_ALBUM = stringPreferencesKey("grid_mode_album")
        val KEY_GRID_MODE_TIMELINE = stringPreferencesKey("grid_mode_timeline")

        // Sort preferences for different screens (REQ-023)
        val KEY_SORT_ORDER_KEEP = stringPreferencesKey("sort_order_keep")
        val KEY_SORT_ORDER_MAYBE = stringPreferencesKey("sort_order_maybe")
        val KEY_SORT_ORDER_TRASH = stringPreferencesKey("sort_order_trash")
        val KEY_SORT_ORDER_ALBUM = stringPreferencesKey("sort_order_album")
        val KEY_SORT_ORDER_TIMELINE = stringPreferencesKey("sort_order_timeline")
        val KEY_SORT_ORDER_FILTER = stringPreferencesKey("sort_order_filter")
        
        // Feature settings
        val KEY_CARD_ZOOM_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("card_zoom_enabled")
        val KEY_ONESTOP_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("onestop_enabled")
        val KEY_EXPERIMENTAL_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("experimental_enabled")
        
        // Theme settings
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        
        // Swipe sensitivity settings (1.0 = default, 0.5 = very sensitive, 1.5 = less sensitive)
        val KEY_SWIPE_SENSITIVITY = androidx.datastore.preferences.core.floatPreferencesKey("swipe_sensitivity")
        
        // Haptic feedback settings (Phase 3-7)
        val KEY_HAPTIC_FEEDBACK_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("haptic_feedback_enabled")
        
        // Photo classification mode settings
        val KEY_PHOTO_CLASSIFICATION_MODE = stringPreferencesKey("photo_classification_mode")
        val KEY_ALBUM_ADD_ACTION = stringPreferencesKey("album_add_action")
        val KEY_CARD_SORTING_ALBUM_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("card_sorting_album_enabled")
        val KEY_ALBUM_TAG_SIZE = androidx.datastore.preferences.core.floatPreferencesKey("album_tag_size")
        val KEY_MAX_ALBUM_TAG_COUNT = intPreferencesKey("max_album_tag_count")
        val KEY_ALBUM_VIEW_MODE = stringPreferencesKey("album_view_mode")
        
        // Keep list display settings
        val KEY_SHOW_PHOTOS_IN_ALBUM_KEEP_LIST = androidx.datastore.preferences.core.booleanPreferencesKey("show_photos_in_album_keep_list")
        
        // Album photo list status filter (comma-separated status names, default all)
        val KEY_ALBUM_PHOTO_STATUS_FILTER = stringPreferencesKey("album_photo_status_filter")
        
        // Changelog and Quick Start version tracking
        val KEY_LAST_SEEN_CHANGELOG_VERSION = stringPreferencesKey("last_seen_changelog_version")
        val KEY_COMPLETED_QUICK_START_VERSION = stringPreferencesKey("completed_quick_start_version")

        // Onboarding guide flags (REQ-067)
        val KEY_PINCH_ZOOM_GUIDE_SEEN = androidx.datastore.preferences.core.booleanPreferencesKey("pinch_zoom_guide_seen")
        
        // Achievement keys
        val KEY_TAGGED_COUNT = intPreferencesKey("total_tagged_count")
        val KEY_MAX_COMBO = intPreferencesKey("max_combo")
        val KEY_TRASH_EMPTIED_COUNT = intPreferencesKey("trash_emptied_count")
        val KEY_VIRTUAL_COPIES_CREATED = intPreferencesKey("virtual_copies_created")
        val KEY_PHOTOS_EXPORTED = intPreferencesKey("photos_exported")
        val KEY_TAGS_CREATED = intPreferencesKey("tags_created")
        val KEY_COMPARISON_SESSIONS = intPreferencesKey("comparison_sessions")
        val KEY_FLOW_SESSIONS_COMPLETED = intPreferencesKey("flow_sessions_completed")
        val KEY_PERFECT_DAYS = intPreferencesKey("perfect_days") // Days with 100+ sorted
        val KEY_LAST_ACTIVE_DATE = longPreferencesKey("last_active_date")
        val KEY_CONSECUTIVE_DAYS = intPreferencesKey("consecutive_days")
        val KEY_KEEP_COUNT = intPreferencesKey("keep_count")
        val KEY_TRASH_COUNT = intPreferencesKey("trash_count")
        val KEY_MAYBE_COUNT = intPreferencesKey("maybe_count")
        val KEY_DAILY_TASKS_COMPLETED = intPreferencesKey("daily_tasks_completed")
    }
    
    // ==================== PHOTO FILTER SETTINGS ====================
    
    /**
     * Get current photo filter mode.
     * Uses distinctUntilChanged to prevent re-emissions when other preferences change.
     */
    fun getPhotoFilterMode(): Flow<PhotoFilterMode> = dataStore.data.map { preferences ->
        val modeStr = preferences[KEY_PHOTO_FILTER_MODE] ?: PhotoFilterMode.ALL.name
        try {
            PhotoFilterMode.valueOf(modeStr)
        } catch (e: IllegalArgumentException) {
            PhotoFilterMode.ALL
        }
    }.distinctUntilChanged()
    
    /**
     * Get current photo filter mode synchronously.
     */
    suspend fun getPhotoFilterModeSync(): PhotoFilterMode {
        return getPhotoFilterMode().first()
    }
    
    /**
     * Set photo filter mode.
     */
    suspend fun setPhotoFilterMode(mode: PhotoFilterMode) {
        dataStore.edit { preferences ->
            preferences[KEY_PHOTO_FILTER_MODE] = mode.name
        }
    }
    
    // Session-based custom filter (not persisted, cleared on app restart)
    private val _sessionCustomFilter = MutableStateFlow<CustomFilterSession?>(null)

    // Album refresh trigger - incremented when photos are added/moved to albums
    // AlbumBubbleScreen observes this to refresh stats when switching tabs
    private val _albumRefreshTrigger = MutableStateFlow(0L)
    val albumRefreshTrigger: StateFlow<Long> = _albumRefreshTrigger.asStateFlow()

    /**
     * Trigger album list refresh. Call this after adding/moving photos to albums.
     */
    fun triggerAlbumRefresh() {
        _albumRefreshTrigger.value = System.currentTimeMillis()
    }

    /**
     * Get current session's custom filter settings as Flow.
     */
    fun getSessionCustomFilterFlow(): StateFlow<CustomFilterSession?> = _sessionCustomFilter.asStateFlow()
    
    /**
     * Get current session's custom filter settings (synchronous).
     */
    fun getSessionCustomFilter(): CustomFilterSession? = _sessionCustomFilter.value
    
    /**
     * Set session custom filter for CUSTOM mode.
     */
    fun setSessionCustomFilter(filter: CustomFilterSession?) {
        _sessionCustomFilter.value = filter
    }
    
    /**
     * Clear session custom filter.
     */
    fun clearSessionCustomFilter() {
        _sessionCustomFilter.value = null
    }
    
    // ==================== DAILY TASK SETTINGS ====================
    
    fun getDailyTaskEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_TASK_ENABLED] ?: true
    }
    
    suspend fun setDailyTaskEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_TASK_ENABLED] = enabled
        }
    }
    
    fun getDailyTaskTarget(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_TASK_TARGET] ?: 100
    }
    
    suspend fun setDailyTaskTarget(target: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_TASK_TARGET] = target.coerceIn(1, 1000)
        }
    }
    
    fun getDailyTaskMode(): Flow<DailyTaskMode> = dataStore.data.map { preferences ->
        val modeStr = preferences[KEY_DAILY_TASK_MODE] ?: DailyTaskMode.QUICK.name
        try {
            DailyTaskMode.valueOf(modeStr)
        } catch (e: IllegalArgumentException) {
            DailyTaskMode.QUICK
        }
    }
    
    suspend fun setDailyTaskMode(mode: DailyTaskMode) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_TASK_MODE] = mode.name
        }
    }
    
    fun getDailyReminderEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_REMINDER_ENABLED] ?: true  // Default enabled
    }
    
    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_REMINDER_ENABLED] = enabled
        }
    }
    
    fun getDailyReminderTime(): Flow<Pair<Int, Int>> = dataStore.data.map { preferences ->
        val hour = preferences[KEY_DAILY_REMINDER_HOUR] ?: 22  // Default 10 PM
        val minute = preferences[KEY_DAILY_REMINDER_MINUTE] ?: 0
        Pair(hour, minute)
    }
    
    suspend fun setDailyReminderTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_REMINDER_HOUR] = hour
            preferences[KEY_DAILY_REMINDER_MINUTE] = minute
        }
    }
    
    /**
     * Get whether progress notification service is enabled.
     * Default: true (enabled)
     */
    fun getProgressNotificationEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_PROGRESS_NOTIFICATION_ENABLED] ?: true
    }
    
    /**
     * Set progress notification service enabled/disabled.
     */
    suspend fun setProgressNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_PROGRESS_NOTIFICATION_ENABLED] = enabled
        }
    }

    // ==================== WIDGET SETTINGS ====================
    
    fun getWidgetPhotoSource(): Flow<WidgetPhotoSource> = dataStore.data.map { preferences ->
        val modeStr = preferences[KEY_WIDGET_PHOTO_SOURCE] ?: WidgetPhotoSource.ALL.name
        try {
            WidgetPhotoSource.valueOf(modeStr)
        } catch (e: IllegalArgumentException) {
            WidgetPhotoSource.ALL
        }
    }
    
    suspend fun setWidgetPhotoSource(source: WidgetPhotoSource) {
        dataStore.edit { preferences ->
            preferences[KEY_WIDGET_PHOTO_SOURCE] = source.name
        }
    }
    
    fun getWidgetCustomAlbumIds(): Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[KEY_WIDGET_CUSTOM_ALBUM_IDS] ?: emptySet()
    }
    
    suspend fun setWidgetCustomAlbumIds(albumIds: Set<String>) {
        dataStore.edit { preferences ->
            preferences[KEY_WIDGET_CUSTOM_ALBUM_IDS] = albumIds
        }
    }
    
    fun getWidgetDateRange(): Flow<Pair<Long?, Long?>> = dataStore.data.map { preferences ->
        val startDate = preferences[KEY_WIDGET_START_DATE]
        val endDate = preferences[KEY_WIDGET_END_DATE]
        Pair(startDate, endDate)
    }
    
    suspend fun setWidgetDateRange(startDate: Long?, endDate: Long?) {
        dataStore.edit { preferences ->
            if (startDate != null) {
                preferences[KEY_WIDGET_START_DATE] = startDate
            } else {
                preferences.remove(KEY_WIDGET_START_DATE)
            }
            if (endDate != null) {
                preferences[KEY_WIDGET_END_DATE] = endDate
            } else {
                preferences.remove(KEY_WIDGET_END_DATE)
            }
        }
    }

    // ==================== FEATURE SETTINGS ====================
    
    /**
     * Get whether card zoom (click to enlarge) is enabled.
     * Default is true.
     */
    fun getCardZoomEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_CARD_ZOOM_ENABLED] ?: true
    }
    
    /**
     * Set card zoom enabled state.
     */
    suspend fun setCardZoomEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_CARD_ZOOM_ENABLED] = enabled
        }
    }
    
    /**
     * Get whether one-stop sorting (一站式整理) is enabled on home screen.
     * Default is false.
     */
    fun getOnestopEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONESTOP_ENABLED] ?: false
    }
    
    /**
     * Set one-stop sorting enabled state.
     */
    suspend fun setOnestopEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONESTOP_ENABLED] = enabled
        }
    }
    
    /**
     * Get whether experimental features are enabled.
     * Default is false.
     */
    fun getExperimentalEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_EXPERIMENTAL_ENABLED] ?: false
    }
    
    /**
     * Set experimental features enabled state.
     */
    suspend fun setExperimentalEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_EXPERIMENTAL_ENABLED] = enabled
        }
    }
    
    // ==================== THEME SETTINGS ====================
    
    /**
     * Get current theme mode.
     * Default is SYSTEM (follow system dark mode).
     */
    fun getThemeMode(): Flow<ThemeMode> = dataStore.data.map { preferences ->
        val modeStr = preferences[KEY_THEME_MODE] ?: ThemeMode.DARK.name
        try {
            ThemeMode.valueOf(modeStr)
        } catch (e: IllegalArgumentException) {
            ThemeMode.DARK
        }
    }
    
    /**
     * Set theme mode.
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }
    
    // ==================== SWIPE SENSITIVITY SETTINGS ====================
    
    /**
     * Get swipe sensitivity for card sorting.
     * Default is 1.0 (normal).
     * Range: 0.5 (very sensitive) to 1.5 (less sensitive)
     */
    fun getSwipeSensitivity(): Flow<Float> = dataStore.data.map { preferences ->
        preferences[KEY_SWIPE_SENSITIVITY] ?: 1.0f
    }
    
    /**
     * Set swipe sensitivity.
     * @param sensitivity Value between 0.5 and 1.5
     */
    suspend fun setSwipeSensitivity(sensitivity: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_SWIPE_SENSITIVITY] = sensitivity.coerceIn(0.5f, 1.5f)
        }
    }
    
    // ==================== HAPTIC FEEDBACK SETTINGS (Phase 3-7) ====================
    
    /**
     * Get haptic feedback enabled state.
     * Default is true.
     */
    fun getHapticFeedbackEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HAPTIC_FEEDBACK_ENABLED] ?: true
    }
    
    /**
     * Set haptic feedback enabled state.
     */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }
    
    // ==================== PHOTO CLASSIFICATION MODE SETTINGS ====================
    
    /**
     * Get photo classification mode (TAG or ALBUM).
     * Default is ALBUM.
     */
    fun getPhotoClassificationMode(): Flow<PhotoClassificationMode> = dataStore.data.map { preferences ->
        val modeStr = preferences[KEY_PHOTO_CLASSIFICATION_MODE] ?: PhotoClassificationMode.ALBUM.name
        try {
            PhotoClassificationMode.valueOf(modeStr)
        } catch (e: IllegalArgumentException) {
            PhotoClassificationMode.ALBUM
        }
    }
    
    /**
     * Set photo classification mode.
     */
    suspend fun setPhotoClassificationMode(mode: PhotoClassificationMode) {
        dataStore.edit { preferences ->
            preferences[KEY_PHOTO_CLASSIFICATION_MODE] = mode.name
        }
    }
    
    /**
     * Get album add action (COPY or MOVE).
     * Default is MOVE.
     */
    fun getAlbumAddAction(): Flow<AlbumAddAction> = dataStore.data.map { preferences ->
        val actionStr = preferences[KEY_ALBUM_ADD_ACTION] ?: AlbumAddAction.MOVE.name
        try {
            AlbumAddAction.valueOf(actionStr)
        } catch (e: IllegalArgumentException) {
            AlbumAddAction.MOVE
        }
    }
    
    /**
     * Set album add action.
     */
    suspend fun setAlbumAddAction(action: AlbumAddAction) {
        dataStore.edit { preferences ->
            preferences[KEY_ALBUM_ADD_ACTION] = action.name
        }
    }
    
    /**
     * Get album view mode (BUBBLE or LIST).
     * Default is BUBBLE.
     */
    fun getAlbumViewMode(): Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_ALBUM_VIEW_MODE] ?: "BUBBLE"
    }
    
    /**
     * Set album view mode.
     */
    suspend fun setAlbumViewMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ALBUM_VIEW_MODE] = mode
        }
    }
    
    /**
     * Get whether to show photos that are already in albums in keep list.
     * When false, only photos not in any "my albums" are shown.
     * Default is true (show all).
     */
    fun getShowPhotosInAlbumKeepList(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SHOW_PHOTOS_IN_ALBUM_KEEP_LIST] ?: true
    }
    
    /**
     * Set whether to show photos that are already in albums in keep list.
     */
    suspend fun setShowPhotosInAlbumKeepList(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_PHOTOS_IN_ALBUM_KEEP_LIST] = show
        }
    }
    
    /**
     * Get album photo list status filter.
     * Returns a set of PhotoStatus values that should be shown.
     * Default is all statuses (KEEP, MAYBE, TRASH, UNSORTED).
     */
    fun getAlbumPhotoStatusFilter(): Flow<Set<com.example.photozen.data.model.PhotoStatus>> = dataStore.data.map { preferences ->
        val filterStr = preferences[KEY_ALBUM_PHOTO_STATUS_FILTER]
        if (filterStr.isNullOrEmpty()) {
            // Default: show all statuses
            com.example.photozen.data.model.PhotoStatus.entries.toSet()
        } else {
            filterStr.split(",")
                .mapNotNull { name ->
                    try {
                        com.example.photozen.data.model.PhotoStatus.valueOf(name.trim())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
                .toSet()
                .ifEmpty { com.example.photozen.data.model.PhotoStatus.entries.toSet() }
        }
    }
    
    /**
     * Set album photo list status filter.
     */
    suspend fun setAlbumPhotoStatusFilter(statuses: Set<com.example.photozen.data.model.PhotoStatus>) {
        val filterStr = if (statuses.size == com.example.photozen.data.model.PhotoStatus.entries.size) {
            // All selected - store empty string (default)
            ""
        } else {
            statuses.joinToString(",") { it.name }
        }
        dataStore.edit { preferences ->
            preferences[KEY_ALBUM_PHOTO_STATUS_FILTER] = filterStr
        }
    }
    
    /**
     * Get whether card sorting album classification is enabled.
     * When enabled, album tags appear at the bottom of card sorting screen.
     * Default is false.
     */
    fun getCardSortingAlbumEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_CARD_SORTING_ALBUM_ENABLED] ?: false
    }
    
    /**
     * Set card sorting album classification enabled.
     */
    suspend fun setCardSortingAlbumEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_CARD_SORTING_ALBUM_ENABLED] = enabled
        }
    }
    
    /**
     * Get album tag size for card sorting screen.
     * Default is 1.0 (normal size).
     * Range: 0.8 (smaller) to 1.2 (larger)
     */
    fun getAlbumTagSize(): Flow<Float> = dataStore.data.map { preferences ->
        preferences[KEY_ALBUM_TAG_SIZE] ?: 1.0f
    }
    
    /**
     * Set album tag size.
     */
    suspend fun setAlbumTagSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_ALBUM_TAG_SIZE] = size
        }
    }
    
    /**
     * Get max album tag count for card sorting screen.
     * 0 means unlimited.
     * Default is 0 (unlimited).
     */
    fun getMaxAlbumTagCount(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_MAX_ALBUM_TAG_COUNT] ?: 0
    }
    
    /**
     * Set max album tag count.
     * @param count 0 for unlimited, or 1-20 for limited
     */
    suspend fun setMaxAlbumTagCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_MAX_ALBUM_TAG_COUNT] = count.coerceIn(0, 20)
        }
    }
    
    // ==================== EXTERNAL APP SETTINGS ====================
    
    /**
     * Get default external app package name for opening photos.
     */
    fun getDefaultExternalApp(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_EXTERNAL_APP]
    }
    
    /**
     * Get default external app synchronously.
     */
    suspend fun getDefaultExternalAppSync(): String? {
        return getDefaultExternalApp().first()
    }
    
    /**
     * Set default external app for opening photos.
     */
    suspend fun setDefaultExternalApp(packageName: String?) {
        dataStore.edit { preferences ->
            if (packageName == null) {
                preferences.remove(KEY_DEFAULT_EXTERNAL_APP)
            } else {
                preferences[KEY_DEFAULT_EXTERNAL_APP] = packageName
            }
        }
    }
    
    // ==================== SORT COUNT ====================
    
    /**
     * Get the cumulative total of sorted photos.
     */
    fun getTotalSortedCount(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_TOTAL_SORTED_COUNT] ?: 0
    }
    
    /**
     * Increment the total sorted count.
     */
    suspend fun incrementSortedCount(amount: Int = 1) {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_TOTAL_SORTED_COUNT] ?: 0
            preferences[KEY_TOTAL_SORTED_COUNT] = currentCount + amount
        }
    }
    
    /**
     * Set the total sorted count.
     */
    suspend fun setTotalSortedCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_TOTAL_SORTED_COUNT] = count.coerceAtLeast(0)
        }
    }
    
    // ==================== TAGGED COUNT ====================
    
    /**
     * Get total photos tagged.
     */
    fun getTaggedCount(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_TAGGED_COUNT] ?: 0
    }
    
    /**
     * Increment tagged count.
     */
    suspend fun incrementTaggedCount(amount: Int = 1) {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_TAGGED_COUNT] ?: 0
            preferences[KEY_TAGGED_COUNT] = currentCount + amount
        }
    }
    
    // ==================== MAX COMBO ====================
    
    /**
     * Get the highest combo achieved.
     */
    fun getMaxCombo(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_MAX_COMBO] ?: 0
    }
    
    /**
     * Update max combo if new value is higher.
     */
    suspend fun updateMaxCombo(newCombo: Int) {
        dataStore.edit { preferences ->
            val currentMax = preferences[KEY_MAX_COMBO] ?: 0
            if (newCombo > currentMax) {
                preferences[KEY_MAX_COMBO] = newCombo
            }
        }
    }
    
    // ==================== TRASH EMPTIED ====================
    
    /**
     * Get trash emptied count.
     */
    fun getTrashEmptiedCount(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_TRASH_EMPTIED_COUNT] ?: 0
    }
    
    /**
     * Increment trash emptied count.
     */
    suspend fun incrementTrashEmptied() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_TRASH_EMPTIED_COUNT] ?: 0
            preferences[KEY_TRASH_EMPTIED_COUNT] = currentCount + 1
        }
    }
    
    // ==================== VIRTUAL COPIES ====================
    
    /**
     * Get virtual copies created count.
     */
    fun getVirtualCopiesCreated(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_VIRTUAL_COPIES_CREATED] ?: 0
    }
    
    /**
     * Increment virtual copies count.
     */
    suspend fun incrementVirtualCopiesCreated() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_VIRTUAL_COPIES_CREATED] ?: 0
            preferences[KEY_VIRTUAL_COPIES_CREATED] = currentCount + 1
        }
    }
    
    // ==================== PHOTOS EXPORTED ====================
    
    /**
     * Get photos exported count.
     */
    fun getPhotosExported(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_PHOTOS_EXPORTED] ?: 0
    }
    
    /**
     * Increment photos exported count.
     */
    suspend fun incrementPhotosExported() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_PHOTOS_EXPORTED] ?: 0
            preferences[KEY_PHOTOS_EXPORTED] = currentCount + 1
        }
    }
    
    // ==================== TAGS CREATED ====================
    
    /**
     * Get tags created count.
     */
    fun getTagsCreated(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_TAGS_CREATED] ?: 0
    }
    
    /**
     * Increment tags created count.
     */
    suspend fun incrementTagsCreated() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_TAGS_CREATED] ?: 0
            preferences[KEY_TAGS_CREATED] = currentCount + 1
        }
    }
    
    // ==================== COMPARISON SESSIONS ====================
    
    /**
     * Get comparison sessions count.
     */
    fun getComparisonSessions(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_COMPARISON_SESSIONS] ?: 0
    }
    
    /**
     * Increment comparison sessions count.
     */
    suspend fun incrementComparisonSessions() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_COMPARISON_SESSIONS] ?: 0
            preferences[KEY_COMPARISON_SESSIONS] = currentCount + 1
        }
    }
    
    // ==================== FLOW SESSIONS ====================
    
    /**
     * Get flow sessions completed count.
     */
    fun getFlowSessionsCompleted(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_FLOW_SESSIONS_COMPLETED] ?: 0
    }
    
    /**
     * Increment flow sessions count.
     */
    suspend fun incrementFlowSessionsCompleted() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_FLOW_SESSIONS_COMPLETED] ?: 0
            preferences[KEY_FLOW_SESSIONS_COMPLETED] = currentCount + 1
        }
    }
    
    // ==================== CONSECUTIVE DAYS ====================
    
    /**
     * Get consecutive active days.
     */
    fun getConsecutiveDays(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_CONSECUTIVE_DAYS] ?: 0
    }
    
    /**
     * Update consecutive days tracking.
     */
    suspend fun updateConsecutiveDays() {
        dataStore.edit { preferences ->
            val lastActive = preferences[KEY_LAST_ACTIVE_DATE] ?: 0L
            val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000) // Day number
            val lastDay = lastActive / (24 * 60 * 60 * 1000)
            
            when {
                today == lastDay -> {
                    // Same day, no change
                }
                today == lastDay + 1 -> {
                    // Consecutive day
                    val consecutive = preferences[KEY_CONSECUTIVE_DAYS] ?: 0
                    preferences[KEY_CONSECUTIVE_DAYS] = consecutive + 1
                    preferences[KEY_LAST_ACTIVE_DATE] = System.currentTimeMillis()
                }
                else -> {
                    // Streak broken, reset to 1
                    preferences[KEY_CONSECUTIVE_DAYS] = 1
                    preferences[KEY_LAST_ACTIVE_DATE] = System.currentTimeMillis()
                }
            }
        }
    }
    
    // ==================== PERFECT DAYS ====================
    
    /**
     * Get perfect days count (100+ sorted in a day).
     */
    fun getPerfectDays(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_PERFECT_DAYS] ?: 0
    }
    
    /**
     * Increment perfect days count.
     */
    suspend fun incrementPerfectDays() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_PERFECT_DAYS] ?: 0
            preferences[KEY_PERFECT_DAYS] = currentCount + 1
        }
    }
    
    // ==================== KEEP/TRASH/MAYBE COUNTS ====================
    
    /**
     * Get keep action count.
     */
    fun getKeepCount(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_KEEP_COUNT] ?: 0
    }
    
    /**
     * Increment keep count.
     */
    suspend fun incrementKeepCount() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_KEEP_COUNT] ?: 0
            preferences[KEY_KEEP_COUNT] = currentCount + 1
        }
    }
    
    /**
     * Get trash action count.
     */
    fun getTrashCount(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_TRASH_COUNT] ?: 0
    }
    
    /**
     * Increment trash count.
     */
    suspend fun incrementTrashCount() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_TRASH_COUNT] ?: 0
            preferences[KEY_TRASH_COUNT] = currentCount + 1
        }
    }
    
    /**
     * Get maybe action count.
     */
    fun getMaybeCount(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_MAYBE_COUNT] ?: 0
    }
    
    /**
     * Increment maybe count.
     */
    suspend fun incrementMaybeCount() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_MAYBE_COUNT] ?: 0
            preferences[KEY_MAYBE_COUNT] = currentCount + 1
        }
    }
    
    // ==================== DAILY TASKS COMPLETED ====================
    
    /**
     * Get daily tasks completed count.
     */
    fun getDailyTasksCompleted(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_TASKS_COMPLETED] ?: 0
    }
    
    /**
     * Increment daily tasks completed count.
     */
    suspend fun incrementDailyTasksCompleted() {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_DAILY_TASKS_COMPLETED] ?: 0
            preferences[KEY_DAILY_TASKS_COMPLETED] = currentCount + 1
        }
    }
    
    // ==================== BUBBLE POSITIONS ====================
    
    /**
     * Save bubble positions for tag bubble screen.
     * @param positions Map of tagId to Pair(x, y)
     */
    suspend fun saveBubblePositions(positions: Map<String, Pair<Float, Float>>) {
        dataStore.edit { preferences ->
            // Encode as simple string: "tagId:x,y;tagId:x,y;..."
            val encoded = positions.entries.joinToString(";") { (id, pos) ->
                "$id:${pos.first},${pos.second}"
            }
            preferences[KEY_BUBBLE_POSITIONS] = encoded
        }
    }
    
    /**
     * Get saved bubble positions.
     * @return Map of tagId to Pair(x, y), or empty map if none saved
     */
    suspend fun getBubblePositions(): Map<String, Pair<Float, Float>> {
        val encoded = dataStore.data.first()[KEY_BUBBLE_POSITIONS] ?: return emptyMap()
        return try {
            encoded.split(";").filter { it.isNotBlank() }.associate { entry ->
                val parts = entry.split(":")
                val id = parts[0]
                val coords = parts[1].split(",")
                id to Pair(coords[0].toFloat(), coords[1].toFloat())
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Clear saved bubble positions (reset to default).
     */
    suspend fun clearBubblePositions() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_BUBBLE_POSITIONS)
        }
    }
    
    // ==================== GRID COLUMN PREFERENCES (REQ-002, REQ-007) ====================

    /**
     * Grid column count for different screens.
     * Default is 3 columns (REQ-002, REQ-007).
     *
     * REQ-002: 网格视图支持 2-5 列切换，默认3列
     * REQ-007: 瀑布流视图支持 1-5 列切换，默认3列
     */
    enum class GridScreen {
        KEEP, MAYBE, TRASH, TAGGED, FLOW, ALBUM, TIMELINE
    }

    /**
     * Get grid column count for a specific screen.
     * Default is 3 columns per REQ-002, REQ-007.
     */
    fun getGridColumns(screen: GridScreen): Flow<Int> = dataStore.data.map { preferences ->
        val key = when (screen) {
            GridScreen.KEEP -> KEY_GRID_COLUMNS_KEEP
            GridScreen.MAYBE -> KEY_GRID_COLUMNS_MAYBE
            GridScreen.TRASH -> KEY_GRID_COLUMNS_TRASH
            GridScreen.TAGGED -> KEY_GRID_COLUMNS_TAGGED
            GridScreen.FLOW -> KEY_GRID_COLUMNS_FLOW
            GridScreen.ALBUM -> KEY_GRID_COLUMNS_ALBUM
            GridScreen.TIMELINE -> KEY_GRID_COLUMNS_TIMELINE
        }
        preferences[key] ?: 3 // Default 3 columns (REQ-002, REQ-007)
    }

    /**
     * Get grid column count synchronously.
     */
    suspend fun getGridColumnsSync(screen: GridScreen): Int {
        return getGridColumns(screen).first()
    }

    /**
     * Set grid column count for a specific screen.
     * REQ-002: 网格视图 2-5 列
     * REQ-007: 瀑布流视图 1-5 列
     */
    suspend fun setGridColumns(screen: GridScreen, columns: Int) {
        val key = when (screen) {
            GridScreen.KEEP -> KEY_GRID_COLUMNS_KEEP
            GridScreen.MAYBE -> KEY_GRID_COLUMNS_MAYBE
            GridScreen.TRASH -> KEY_GRID_COLUMNS_TRASH
            GridScreen.TAGGED -> KEY_GRID_COLUMNS_TAGGED
            GridScreen.FLOW -> KEY_GRID_COLUMNS_FLOW
            GridScreen.ALBUM -> KEY_GRID_COLUMNS_ALBUM
            GridScreen.TIMELINE -> KEY_GRID_COLUMNS_TIMELINE
        }
        dataStore.edit { preferences ->
            preferences[key] = columns.coerceIn(1, 5) // 1-5 columns (REQ-002, REQ-007)
        }
    }

    /**
     * Cycle grid columns: 2 -> 3 -> 4 -> 5 -> 2 (for grid mode)
     * Or: 1 -> 2 -> 3 -> 4 -> 5 -> 1 (for waterfall mode)
     *
     * @param minColumns Minimum columns (2 for grid, 1 for waterfall)
     */
    suspend fun cycleGridColumns(screen: GridScreen, minColumns: Int = 2): Int {
        val current = getGridColumnsSync(screen)
        val next = if (current >= 5) minColumns else current + 1
        setGridColumns(screen, next)
        return next
    }

    /**
     * Get grid mode (SQUARE vs WATERFALL) for a specific screen.
     * Default is SQUARE (网格模式).
     */
    fun getGridMode(screen: GridScreen): Flow<com.example.photozen.ui.components.PhotoGridMode> = dataStore.data.map { preferences ->
        val key = when (screen) {
            GridScreen.KEEP -> KEY_GRID_MODE_KEEP
            GridScreen.MAYBE -> KEY_GRID_MODE_MAYBE
            GridScreen.TRASH -> KEY_GRID_MODE_TRASH
            GridScreen.TAGGED -> KEY_GRID_MODE_TAGGED
            GridScreen.FLOW -> KEY_GRID_MODE_FLOW
            GridScreen.ALBUM -> KEY_GRID_MODE_ALBUM
            GridScreen.TIMELINE -> KEY_GRID_MODE_TIMELINE
        }
        val modeName = preferences[key] ?: "SQUARE"
        try {
            com.example.photozen.ui.components.PhotoGridMode.valueOf(modeName)
        } catch (e: Exception) {
            com.example.photozen.ui.components.PhotoGridMode.SQUARE
        }
    }

    /**
     * Set grid mode for a specific screen.
     */
    suspend fun setGridMode(screen: GridScreen, mode: com.example.photozen.ui.components.PhotoGridMode) {
        val key = when (screen) {
            GridScreen.KEEP -> KEY_GRID_MODE_KEEP
            GridScreen.MAYBE -> KEY_GRID_MODE_MAYBE
            GridScreen.TRASH -> KEY_GRID_MODE_TRASH
            GridScreen.TAGGED -> KEY_GRID_MODE_TAGGED
            GridScreen.FLOW -> KEY_GRID_MODE_FLOW
            GridScreen.ALBUM -> KEY_GRID_MODE_ALBUM
            GridScreen.TIMELINE -> KEY_GRID_MODE_TIMELINE
        }
        dataStore.edit { preferences ->
            preferences[key] = mode.name
        }
    }

    // ==================== SORT ORDER PREFERENCES (REQ-023) ====================

    /**
     * Sort screen identifier for sort order preferences.
     */
    enum class SortScreen {
        KEEP, MAYBE, TRASH, ALBUM, TIMELINE, FILTER
    }

    /**
     * Get sort order for a specific screen.
     * Default is "photo_time_desc" (时间倒序).
     */
    fun getSortOrder(screen: SortScreen): Flow<String> = dataStore.data.map { preferences ->
        val key = when (screen) {
            SortScreen.KEEP -> KEY_SORT_ORDER_KEEP
            SortScreen.MAYBE -> KEY_SORT_ORDER_MAYBE
            SortScreen.TRASH -> KEY_SORT_ORDER_TRASH
            SortScreen.ALBUM -> KEY_SORT_ORDER_ALBUM
            SortScreen.TIMELINE -> KEY_SORT_ORDER_TIMELINE
            SortScreen.FILTER -> KEY_SORT_ORDER_FILTER
        }
        preferences[key] ?: "photo_time_desc" // Default: 时间倒序
    }

    /**
     * Get sort order synchronously.
     */
    suspend fun getSortOrderSync(screen: SortScreen): String {
        return getSortOrder(screen).first()
    }

    /**
     * Set sort order for a specific screen.
     */
    suspend fun setSortOrder(screen: SortScreen, sortOrderId: String) {
        val key = when (screen) {
            SortScreen.KEEP -> KEY_SORT_ORDER_KEEP
            SortScreen.MAYBE -> KEY_SORT_ORDER_MAYBE
            SortScreen.TRASH -> KEY_SORT_ORDER_TRASH
            SortScreen.ALBUM -> KEY_SORT_ORDER_ALBUM
            SortScreen.TIMELINE -> KEY_SORT_ORDER_TIMELINE
            SortScreen.FILTER -> KEY_SORT_ORDER_FILTER
        }
        dataStore.edit { preferences ->
            preferences[key] = sortOrderId
        }
    }
    
    // ==================== ONBOARDING GUIDE FLAGS (REQ-067) ====================

    /**
     * Check if user has seen the pinch zoom guide.
     * Default is false (not seen).
     */
    fun hasPinchZoomGuideSeen(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_PINCH_ZOOM_GUIDE_SEEN] ?: false
    }

    /**
     * Mark pinch zoom guide as seen.
     */
    suspend fun setPinchZoomGuideSeen(seen: Boolean = true) {
        dataStore.edit { preferences ->
            preferences[KEY_PINCH_ZOOM_GUIDE_SEEN] = seen
        }
    }

    // ==================== CHANGELOG AND QUICK START VERSION TRACKING ====================

    /**
     * Get the last seen changelog version.
     * Returns null if user has never seen any changelog.
     */
    fun getLastSeenChangelogVersion(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_SEEN_CHANGELOG_VERSION]
    }
    
    /**
     * Set the last seen changelog version.
     * Should be called after user dismisses the changelog dialog.
     */
    suspend fun setLastSeenChangelogVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_SEEN_CHANGELOG_VERSION] = version
        }
    }
    
    /**
     * Get the completed quick start guide version.
     * Returns null if user has never completed the quick start guide.
     */
    fun getCompletedQuickStartVersion(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_COMPLETED_QUICK_START_VERSION]
    }
    
    /**
     * Set the completed quick start guide version.
     * Should be called after user completes all steps of the quick start guide.
     */
    suspend fun setCompletedQuickStartVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[KEY_COMPLETED_QUICK_START_VERSION] = version
        }
    }
    
    // ==================== ALL ACHIEVEMENT DATA ====================
    
    /**
     * Get all achievement-related data as a combined flow.
     */
    fun getAllAchievementData(): Flow<AchievementData> = dataStore.data.map { preferences ->
        AchievementData(
            totalSorted = preferences[KEY_TOTAL_SORTED_COUNT] ?: 0,
            totalTagged = preferences[KEY_TAGGED_COUNT] ?: 0,
            maxCombo = preferences[KEY_MAX_COMBO] ?: 0,
            trashEmptied = preferences[KEY_TRASH_EMPTIED_COUNT] ?: 0,
            virtualCopiesCreated = preferences[KEY_VIRTUAL_COPIES_CREATED] ?: 0,
            photosExported = preferences[KEY_PHOTOS_EXPORTED] ?: 0,
            tagsCreated = preferences[KEY_TAGS_CREATED] ?: 0,
            comparisonSessions = preferences[KEY_COMPARISON_SESSIONS] ?: 0,
            flowSessionsCompleted = preferences[KEY_FLOW_SESSIONS_COMPLETED] ?: 0,
            perfectDays = preferences[KEY_PERFECT_DAYS] ?: 0,
            consecutiveDays = preferences[KEY_CONSECUTIVE_DAYS] ?: 0,
            keepCount = preferences[KEY_KEEP_COUNT] ?: 0,
            trashCount = preferences[KEY_TRASH_COUNT] ?: 0,
            maybeCount = preferences[KEY_MAYBE_COUNT] ?: 0,
            dailyTasksCompleted = preferences[KEY_DAILY_TASKS_COMPLETED] ?: 0
        )
    }
}

/**
 * Data class holding all achievement-related metrics.
 */
data class AchievementData(
    val totalSorted: Int = 0,
    val totalTagged: Int = 0,
    val maxCombo: Int = 0,
    val trashEmptied: Int = 0,
    val virtualCopiesCreated: Int = 0,
    val photosExported: Int = 0,
    val tagsCreated: Int = 0,
    val comparisonSessions: Int = 0,
    val flowSessionsCompleted: Int = 0,
    val perfectDays: Int = 0,
    val consecutiveDays: Int = 0,
    val keepCount: Int = 0,
    val trashCount: Int = 0,
    val maybeCount: Int = 0,
    val dailyTasksCompleted: Int = 0
)

/**
 * Custom filter session data for CUSTOM filter mode.
 * Not persisted - only valid for the current session.
 *
 * @param albumIds Optional list of album bucket IDs to include (include mode).
 *                 Mutually exclusive with excludeAlbumIds.
 * @param excludeAlbumIds Optional list of album bucket IDs to exclude (exclude mode).
 *                        Used when most albums are selected - more efficient than large IN clause.
 *                        Mutually exclusive with albumIds.
 * @param startDate Start time in milliseconds (inclusive)
 * @param endDate End time in milliseconds
 * @param preciseMode When false (default), endDate is treated as the start of a day and
 *                    automatically extended to include the whole day (23:59:59.999).
 *                    When true, endDate is used exactly as provided (for precise time range filtering).
 * @param photoIds Optional list of specific photo IDs to filter. When set, only these photos
 *                 will be shown, ignoring albumIds and date range filters.
 */
data class CustomFilterSession(
    val albumIds: List<String>? = null,
    val excludeAlbumIds: List<String>? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val preciseMode: Boolean = false,
    val photoIds: List<String>? = null,
    /** Optional default sort order for this session. If set, FlowSorter will use this on initial load. */
    val defaultSortOrder: com.example.photozen.data.model.PhotoSortOrder? = null
) {
    /**
     * Check if this filter uses exclude mode (NOT IN instead of IN).
     */
    val isExcludeMode: Boolean get() = !excludeAlbumIds.isNullOrEmpty()
}

/**
 * Bubble position data for tag bubble screen.
 */
data class BubblePosition(
    val tagId: String,
    val x: Float,
    val y: Float
)
