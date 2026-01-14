package com.example.photozen.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.repository.AchievementData
import com.example.photozen.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for Achievements Screen.
 */
@HiltViewModel
class AchievementsViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    val achievementData: StateFlow<AchievementData> = preferencesRepository
        .getAllAchievementData()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AchievementData()
        )
}
