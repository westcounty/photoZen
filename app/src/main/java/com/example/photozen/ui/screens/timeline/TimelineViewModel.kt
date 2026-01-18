package com.example.photozen.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.repository.CustomFilterSession
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.domain.EventGrouper
import com.example.photozen.domain.EventGrouper.Companion.getEffectiveTime
import com.example.photozen.domain.GroupingMode
import com.example.photozen.domain.PhotoEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Timeline screen.
 */
data class TimelineUiState(
    val events: List<PhotoEvent> = emptyList(),
    val groupingMode: GroupingMode = GroupingMode.AUTO,
    val isLoading: Boolean = true,
    val totalPhotos: Int = 0,
    val expandedEventIds: Set<String> = emptySet(),
    val error: String? = null,
    val navigateToSorter: Boolean = false
) {
    val hasEvents: Boolean get() = events.isNotEmpty()
    val totalEvents: Int get() = events.size
}

/**
 * ViewModel for Timeline screen.
 * Handles grouping photos into events and managing timeline state.
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val photoDao: PhotoDao,
    private val eventGrouper: EventGrouper,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()
    
    init {
        loadTimeline()
    }
    
    /**
     * Load and group all photos into timeline events.
     */
    private fun loadTimeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                photoDao.getAllPhotos().collect { photos ->
                    // Filter out photos without valid effective time
                    // Uses getEffectiveTime() which prefers dateTaken, falls back to dateAdded * 1000
                    val validPhotos = photos.filter { it.getEffectiveTime() > 0 }
                    
                    // Group photos into events
                    val events = eventGrouper.groupPhotos(
                        validPhotos, 
                        _uiState.value.groupingMode
                    )
                    
                    _uiState.update { state ->
                        state.copy(
                            events = events,
                            totalPhotos = validPhotos.size,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "加载时间线失败: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Change the grouping mode and re-group photos.
     */
    fun setGroupingMode(mode: GroupingMode) {
        if (mode == _uiState.value.groupingMode) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(groupingMode = mode, isLoading = true) }
            
            try {
                photoDao.getAllPhotos().collect { photos ->
                    val validPhotos = photos.filter { it.getEffectiveTime() > 0 }
                    val events = eventGrouper.groupPhotos(validPhotos, mode)
                    
                    _uiState.update { state ->
                        state.copy(
                            events = events,
                            expandedEventIds = emptySet(), // Reset expanded state
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "切换分组模式失败: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Toggle event expanded state.
     */
    fun toggleEventExpanded(eventId: String) {
        _uiState.update { state ->
            val newExpanded = if (eventId in state.expandedEventIds) {
                state.expandedEventIds - eventId
            } else {
                state.expandedEventIds + eventId
            }
            state.copy(expandedEventIds = newExpanded)
        }
    }
    
    /**
     * Expand all events.
     */
    fun expandAll() {
        _uiState.update { state ->
            state.copy(expandedEventIds = state.events.map { it.id }.toSet())
        }
    }
    
    /**
     * Collapse all events.
     */
    fun collapseAll() {
        _uiState.update { it.copy(expandedEventIds = emptySet()) }
    }
    
    /**
     * Check if an event is expanded.
     */
    fun isEventExpanded(eventId: String): Boolean {
        return eventId in _uiState.value.expandedEventIds
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Sort a group of photos by setting up custom filter and triggering navigation.
     * Uses precise mode to ensure exact time range filtering.
     * 
     * @param startTime Start time of the event (milliseconds)
     * @param endTime End time of the event (milliseconds)
     */
    fun sortGroup(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            // Set custom filter with precise mode
            preferencesRepository.setSessionCustomFilter(
                CustomFilterSession(
                    albumIds = null,
                    startDate = startTime,
                    endDate = endTime,
                    preciseMode = true  // Use exact timestamps, no day extension
                )
            )
            
            // Set filter mode to CUSTOM
            preferencesRepository.setPhotoFilterMode(PhotoFilterMode.CUSTOM)
            
            // Trigger navigation
            _uiState.update { it.copy(navigateToSorter = true) }
        }
    }
    
    /**
     * Reset navigation state after navigation is complete.
     */
    fun onNavigationComplete() {
        _uiState.update { it.copy(navigateToSorter = false) }
    }
}
