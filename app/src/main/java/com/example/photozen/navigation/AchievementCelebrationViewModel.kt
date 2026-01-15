package com.example.photozen.navigation

import androidx.lifecycle.ViewModel
import com.example.photozen.domain.AchievementManager
import com.example.photozen.domain.AchievementUnlockEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel for managing achievement celebration UI state across the app.
 */
@HiltViewModel
class AchievementCelebrationViewModel @Inject constructor(
    private val achievementManager: AchievementManager
) : ViewModel() {
    
    val currentCelebration: StateFlow<AchievementUnlockEvent?> = achievementManager.currentCelebration
    
    fun clearCelebration() {
        achievementManager.clearCelebration()
    }
}
