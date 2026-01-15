package com.example.photozen.domain

import com.example.photozen.data.repository.AchievementData
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.ui.components.Achievement
import com.example.photozen.ui.components.generateAchievements
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class for achievement unlock event.
 */
data class AchievementUnlockEvent(
    val achievement: Achievement
)

/**
 * Manager for tracking achievement unlocks and emitting celebration events.
 * 
 * This singleton monitors achievement data and emits events when new achievements
 * are unlocked, which can be used to trigger celebration UI effects.
 */
@Singleton
class AchievementManager @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Set of already unlocked achievement IDs (to avoid duplicate notifications)
    private var unlockedIds = mutableSetOf<String>()
    private var isInitialized = false
    
    // Events for achievement unlocks
    private val _achievementUnlockEvents = MutableSharedFlow<AchievementUnlockEvent>()
    val achievementUnlockEvents: SharedFlow<AchievementUnlockEvent> = _achievementUnlockEvents.asSharedFlow()
    
    // Current celebration state
    private val _currentCelebration = MutableStateFlow<AchievementUnlockEvent?>(null)
    val currentCelebration: StateFlow<AchievementUnlockEvent?> = _currentCelebration.asStateFlow()
    
    init {
        // Start monitoring achievements
        scope.launch {
            preferencesRepository.getAllAchievementData().collect { data ->
                checkForNewUnlocks(data)
            }
        }
    }
    
    /**
     * Check for newly unlocked achievements.
     */
    private suspend fun checkForNewUnlocks(data: AchievementData) {
        val achievements = generateAchievements(data)
        val currentlyUnlocked = achievements.filter { it.isUnlocked }.map { it.id }.toSet()
        
        if (!isInitialized) {
            // First run - just record current state without triggering celebrations
            unlockedIds = currentlyUnlocked.toMutableSet()
            isInitialized = true
            return
        }
        
        // Find newly unlocked achievements
        val newlyUnlocked = currentlyUnlocked - unlockedIds
        
        // Emit events for each newly unlocked achievement
        newlyUnlocked.forEach { id ->
            val achievement = achievements.find { it.id == id }
            if (achievement != null) {
                val event = AchievementUnlockEvent(achievement)
                _achievementUnlockEvents.emit(event)
                _currentCelebration.value = event
            }
        }
        
        // Update tracked unlocks
        unlockedIds = currentlyUnlocked.toMutableSet()
    }
    
    /**
     * Clear current celebration (called after animation completes).
     */
    fun clearCelebration() {
        _currentCelebration.value = null
    }
    
    /**
     * Force check for achievements (useful after batch operations).
     */
    suspend fun forceCheck() {
        preferencesRepository.getAllAchievementData().collect { data ->
            checkForNewUnlocks(data)
            return@collect
        }
    }
}
