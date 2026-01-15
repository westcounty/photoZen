package com.example.photozen.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
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
    private var _sessionCustomFilter: CustomFilterSession? = null
    
    /**
     * Get current session's custom filter settings.
     */
    fun getSessionCustomFilter(): CustomFilterSession? = _sessionCustomFilter
    
    /**
     * Set session custom filter for CUSTOM mode.
     */
    fun setSessionCustomFilter(filter: CustomFilterSession?) {
        _sessionCustomFilter = filter
    }
    
    /**
     * Clear session custom filter.
     */
    fun clearSessionCustomFilter() {
        _sessionCustomFilter = null
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
