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
 * Daily Task Mode.
 */
enum class DailyTaskMode {
    FLOW,  // Heart Flow Mode (Stack)
    QUICK  // Quick Sorter Mode (Tinder-style)
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
        private val KEY_TOTAL_SORTED_COUNT = intPreferencesKey("total_sorted_count")
        
        // Photo filter settings
        private val KEY_PHOTO_FILTER_MODE = stringPreferencesKey("photo_filter_mode")
        
        // Daily Task Settings
        private val KEY_DAILY_TASK_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("daily_task_enabled")
        private val KEY_DAILY_TASK_TARGET = intPreferencesKey("daily_task_target")
        private val KEY_DAILY_TASK_MODE = stringPreferencesKey("daily_task_mode")
        private val KEY_DAILY_REMINDER_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("daily_reminder_enabled")
        private val KEY_DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        private val KEY_DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        
        // Default external app for opening photos
        private val KEY_DEFAULT_EXTERNAL_APP = stringPreferencesKey("default_external_app")
        
        // Bubble positions (JSON encoded map: tagId -> "x,y")
        private val KEY_BUBBLE_POSITIONS = stringPreferencesKey("bubble_positions")
        
        // Grid column preferences for different screens
        private val KEY_GRID_COLUMNS_KEEP = intPreferencesKey("grid_columns_keep")
        private val KEY_GRID_COLUMNS_MAYBE = intPreferencesKey("grid_columns_maybe")
        private val KEY_GRID_COLUMNS_TRASH = intPreferencesKey("grid_columns_trash")
        private val KEY_GRID_COLUMNS_TAGGED = intPreferencesKey("grid_columns_tagged")
        private val KEY_GRID_COLUMNS_FLOW = intPreferencesKey("grid_columns_flow")
        
        // Achievement keys
        private val KEY_TAGGED_COUNT = intPreferencesKey("total_tagged_count")
        private val KEY_MAX_COMBO = intPreferencesKey("max_combo")
        private val KEY_TRASH_EMPTIED_COUNT = intPreferencesKey("trash_emptied_count")
        private val KEY_VIRTUAL_COPIES_CREATED = intPreferencesKey("virtual_copies_created")
        private val KEY_PHOTOS_EXPORTED = intPreferencesKey("photos_exported")
        private val KEY_TAGS_CREATED = intPreferencesKey("tags_created")
        private val KEY_COMPARISON_SESSIONS = intPreferencesKey("comparison_sessions")
        private val KEY_FLOW_SESSIONS_COMPLETED = intPreferencesKey("flow_sessions_completed")
        private val KEY_PERFECT_DAYS = intPreferencesKey("perfect_days") // Days with 100+ sorted
        private val KEY_LAST_ACTIVE_DATE = longPreferencesKey("last_active_date")
        private val KEY_CONSECUTIVE_DAYS = intPreferencesKey("consecutive_days")
        private val KEY_KEEP_COUNT = intPreferencesKey("keep_count")
        private val KEY_TRASH_COUNT = intPreferencesKey("trash_count")
        private val KEY_MAYBE_COUNT = intPreferencesKey("maybe_count")
    }
    
    // ==================== PHOTO FILTER SETTINGS ====================
    
    /**
     * Get current photo filter mode.
     */
    fun getPhotoFilterMode(): Flow<PhotoFilterMode> = dataStore.data.map { preferences ->
        val modeStr = preferences[KEY_PHOTO_FILTER_MODE] ?: PhotoFilterMode.ALL.name
        try {
            PhotoFilterMode.valueOf(modeStr)
        } catch (e: IllegalArgumentException) {
            PhotoFilterMode.ALL
        }
    }
    
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
        val modeStr = preferences[KEY_DAILY_TASK_MODE] ?: DailyTaskMode.FLOW.name
        try {
            DailyTaskMode.valueOf(modeStr)
        } catch (e: IllegalArgumentException) {
            DailyTaskMode.FLOW
        }
    }
    
    suspend fun setDailyTaskMode(mode: DailyTaskMode) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_TASK_MODE] = mode.name
        }
    }
    
    fun getDailyReminderEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_REMINDER_ENABLED] ?: false
    }
    
    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_REMINDER_ENABLED] = enabled
        }
    }
    
    fun getDailyReminderTime(): Flow<Pair<Int, Int>> = dataStore.data.map { preferences ->
        val hour = preferences[KEY_DAILY_REMINDER_HOUR] ?: 20
        val minute = preferences[KEY_DAILY_REMINDER_MINUTE] ?: 0
        Pair(hour, minute)
    }
    
    suspend fun setDailyReminderTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_REMINDER_HOUR] = hour
            preferences[KEY_DAILY_REMINDER_MINUTE] = minute
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
    
    // ==================== GRID COLUMN PREFERENCES ====================
    
    /**
     * Grid column count for different screens.
     * Default is 2 columns.
     */
    enum class GridScreen {
        KEEP, MAYBE, TRASH, TAGGED, FLOW
    }
    
    /**
     * Get grid column count for a specific screen.
     */
    fun getGridColumns(screen: GridScreen): Flow<Int> = dataStore.data.map { preferences ->
        val key = when (screen) {
            GridScreen.KEEP -> KEY_GRID_COLUMNS_KEEP
            GridScreen.MAYBE -> KEY_GRID_COLUMNS_MAYBE
            GridScreen.TRASH -> KEY_GRID_COLUMNS_TRASH
            GridScreen.TAGGED -> KEY_GRID_COLUMNS_TAGGED
            GridScreen.FLOW -> KEY_GRID_COLUMNS_FLOW
        }
        preferences[key] ?: 2 // Default 2 columns
    }
    
    /**
     * Get grid column count synchronously.
     */
    suspend fun getGridColumnsSync(screen: GridScreen): Int {
        return getGridColumns(screen).first()
    }
    
    /**
     * Set grid column count for a specific screen.
     */
    suspend fun setGridColumns(screen: GridScreen, columns: Int) {
        val key = when (screen) {
            GridScreen.KEEP -> KEY_GRID_COLUMNS_KEEP
            GridScreen.MAYBE -> KEY_GRID_COLUMNS_MAYBE
            GridScreen.TRASH -> KEY_GRID_COLUMNS_TRASH
            GridScreen.TAGGED -> KEY_GRID_COLUMNS_TAGGED
            GridScreen.FLOW -> KEY_GRID_COLUMNS_FLOW
        }
        dataStore.edit { preferences ->
            preferences[key] = columns.coerceIn(1, 3) // 1-3 columns
        }
    }
    
    /**
     * Cycle grid columns: 2 -> 3 -> 1 -> 2
     */
    suspend fun cycleGridColumns(screen: GridScreen): Int {
        val current = getGridColumnsSync(screen)
        val next = when (current) {
            1 -> 2
            2 -> 3
            else -> 1
        }
        setGridColumns(screen, next)
        return next
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
            maybeCount = preferences[KEY_MAYBE_COUNT] ?: 0
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
    val maybeCount: Int = 0
)

/**
 * Custom filter session data for CUSTOM filter mode.
 * Not persisted - only valid for the current session.
 */
data class CustomFilterSession(
    val albumIds: List<String>? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)

/**
 * Bubble position data for tag bubble screen.
 */
data class BubblePosition(
    val tagId: String,
    val x: Float,
    val y: Float
)
