package com.example.photozen.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing app preferences using DataStore.
 * Handles persistent storage for settings like cumulative sort count.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_TOTAL_SORTED_COUNT = intPreferencesKey("total_sorted_count")
    }
    
    /**
     * Get the cumulative total of sorted photos.
     * This value persists across app sessions and does not decrease when photos are deleted.
     */
    fun getTotalSortedCount(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_TOTAL_SORTED_COUNT] ?: 0
    }
    
    /**
     * Increment the total sorted count by the given amount.
     * Called when a photo is sorted (Keep, Trash, or Maybe).
     */
    suspend fun incrementSortedCount(amount: Int = 1) {
        dataStore.edit { preferences ->
            val currentCount = preferences[KEY_TOTAL_SORTED_COUNT] ?: 0
            preferences[KEY_TOTAL_SORTED_COUNT] = currentCount + amount
        }
    }
    
    /**
     * Set the total sorted count to a specific value.
     * Useful for initial sync or correction.
     */
    suspend fun setTotalSortedCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_TOTAL_SORTED_COUNT] = count.coerceAtLeast(0)
        }
    }
}
