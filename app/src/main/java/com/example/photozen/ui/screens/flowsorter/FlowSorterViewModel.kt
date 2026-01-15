package com.example.photozen.ui.screens.flowsorter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.GetUnsortedPhotosUseCase
import com.example.photozen.domain.usecase.SortPhotoUseCase
import com.example.photozen.domain.usecase.SyncPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Combo state for tracking rapid sorting streaks.
 */
data class ComboState(
    val count: Int = 0,
    val maxCount: Int = 0,
    val lastSwipeTime: Long = 0,
    val isActive: Boolean = false
) {
    companion object {
        /** Time threshold in ms to maintain combo */
        const val COMBO_THRESHOLD_MS = 1500L
    }
    
    /** Combo level affects visual feedback */
    val level: ComboLevel
        get() = when {
            count >= 20 -> ComboLevel.FIRE
            count >= 10 -> ComboLevel.HOT
            count >= 5 -> ComboLevel.WARM
            count >= 1 -> ComboLevel.NORMAL
            else -> ComboLevel.NONE
        }
}

/**
 * Visual levels for combo feedback.
 */
enum class ComboLevel {
    NONE,    // No combo
    NORMAL,  // x1-x4: White
    WARM,    // x5-x9: Light orange
    HOT,     // x10-x19: Orange
    FIRE     // x20+: Red/Fire
}

/**
 * UI State for Flow Sorter screen.
 */
data class FlowSorterUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val totalCount: Int = 0,
    val sortedCount: Int = 0,
    val keepCount: Int = 0,
    val trashCount: Int = 0,
    val maybeCount: Int = 0,
    val lastAction: SortAction? = null,
    val error: String? = null,
    val combo: ComboState = ComboState()
) {
    val currentPhoto: PhotoEntity?
        get() = photos.getOrNull(currentIndex)
    
    val nextPhoto: PhotoEntity?
        get() = photos.getOrNull(currentIndex + 1)
    
    val hasPhotos: Boolean
        get() = photos.isNotEmpty()
    
    val isComplete: Boolean
        get() = photos.isEmpty() || currentIndex >= photos.size
    
    val progress: Float
        get() = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
}

/**
 * Represents the last sorting action for undo functionality.
 */
data class SortAction(
    val photoId: String,
    val status: PhotoStatus,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ViewModel for Flow Sorter screen.
 * Manages photo list, sorting actions, and undo functionality.
 */
@HiltViewModel
class FlowSorterViewModel @Inject constructor(
    private val getUnsortedPhotosUseCase: GetUnsortedPhotosUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val syncPhotosUseCase: SyncPhotosUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val mediaStoreDataSource: MediaStoreDataSource
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(true)
    private val _isSyncing = MutableStateFlow(false)
    private val _currentIndex = MutableStateFlow(0)
    private val _lastAction = MutableStateFlow<SortAction?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _counters = MutableStateFlow(SortCounters())
    private val _combo = MutableStateFlow(ComboState())
    
    // Camera album IDs as StateFlow for reactive updates
    private val _cameraAlbumIds = MutableStateFlow<List<String>>(emptyList())
    
    // Job for auto-hiding combo after timeout
    private var comboTimeoutJob: Job? = null
    
    /**
     * Counters for sorted photos by status.
     */
    private data class SortCounters(
        val keep: Int = 0,
        val trash: Int = 0,
        val maybe: Int = 0
    ) {
        val total: Int get() = keep + trash + maybe
    }
    
    /**
     * Get filtered photos flow based on current filter mode.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getFilteredPhotosFlow(): Flow<List<PhotoEntity>> {
        return combine(
            preferencesRepository.getPhotoFilterMode(),
            _cameraAlbumIds
        ) { filterMode, cameraIds ->
            Pair(filterMode, cameraIds)
        }.flatMapLatest { (filterMode, cameraIds) ->
            when (filterMode) {
                PhotoFilterMode.ALL -> getUnsortedPhotosUseCase()
                PhotoFilterMode.CAMERA_ONLY -> {
                    if (cameraIds.isNotEmpty()) {
                        getUnsortedPhotosUseCase.byBuckets(cameraIds)
                    } else {
                        // Return empty flow while loading camera IDs
                        getUnsortedPhotosUseCase()
                    }
                }
                PhotoFilterMode.EXCLUDE_CAMERA -> {
                    if (cameraIds.isNotEmpty()) {
                        getUnsortedPhotosUseCase.excludingBuckets(cameraIds)
                    } else {
                        getUnsortedPhotosUseCase()
                    }
                }
                PhotoFilterMode.CUSTOM -> {
                    val sessionFilter = preferencesRepository.getSessionCustomFilter()
                    if (sessionFilter != null && !sessionFilter.albumIds.isNullOrEmpty()) {
                        getUnsortedPhotosUseCase.byBuckets(sessionFilter.albumIds)
                    } else {
                        getUnsortedPhotosUseCase()
                    }
                }
            }
        }
    }
    
    /**
     * UI State exposed to the screen.
     */
    val uiState: StateFlow<FlowSorterUiState> = combine(
        getFilteredPhotosFlow(),
        _isLoading,
        _isSyncing,
        _currentIndex,
        _lastAction,
        _error,
        _counters,
        _combo
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val photos = values[0] as List<PhotoEntity>
        val isLoading = values[1] as Boolean
        val isSyncing = values[2] as Boolean
        val currentIndex = values[3] as Int
        val lastAction = values[4] as SortAction?
        val error = values[5] as String?
        val counters = values[6] as SortCounters
        val combo = values[7] as ComboState
        
        FlowSorterUiState(
            photos = photos,
            currentIndex = 0, // Always 0 since we remove sorted photos from list
            isLoading = isLoading && photos.isEmpty(),
            isSyncing = isSyncing,
            totalCount = photos.size + counters.total,
            sortedCount = counters.total,
            keepCount = counters.keep,
            trashCount = counters.trash,
            maybeCount = counters.maybe,
            lastAction = lastAction,
            error = error,
            combo = combo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FlowSorterUiState()
    )
    
    init {
        // Load camera album IDs first, then sync
        viewModelScope.launch {
            loadCameraAlbumIds()
            syncPhotos()
        }
    }
    
    /**
     * Load camera album IDs from MediaStore.
     */
    private suspend fun loadCameraAlbumIds() {
        try {
            val albums = mediaStoreDataSource.getAllAlbums()
            _cameraAlbumIds.value = albums.filter { it.isCamera }.map { it.id }
        } catch (e: Exception) {
            // Ignore errors, just use empty list
        }
    }
    
    /**
     * Sync photos from MediaStore.
     */
    fun syncPhotos() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = syncPhotosUseCase()
                if (result.isInitialSync) {
                    // Reset counters on initial sync
                    _counters.value = SortCounters()
                }
            } catch (e: Exception) {
                _error.value = "同步失败: ${e.message}"
            } finally {
                _isSyncing.value = false
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Keep the current photo (swipe right).
     * @return The current combo count after sorting
     */
    fun keepCurrentPhoto(): Int {
        val photo = uiState.value.currentPhoto ?: return 0
        // Update combo first for immediate feedback
        val comboCount = updateCombo()
        viewModelScope.launch {
            try {
                sortPhotoUseCase.keepPhoto(photo.id)
                _lastAction.value = SortAction(photo.id, PhotoStatus.KEEP)
                _counters.value = _counters.value.copy(keep = _counters.value.keep + 1)
                // Increment cumulative sort count and keep count
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementKeepCount()
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
        return comboCount
    }
    
    /**
     * Trash the current photo (swipe up).
     * @return The current combo count after sorting
     */
    fun trashCurrentPhoto(): Int {
        val photo = uiState.value.currentPhoto ?: return 0
        // Update combo first for immediate feedback
        val comboCount = updateCombo()
        viewModelScope.launch {
            try {
                sortPhotoUseCase.trashPhoto(photo.id)
                _lastAction.value = SortAction(photo.id, PhotoStatus.TRASH)
                _counters.value = _counters.value.copy(trash = _counters.value.trash + 1)
                // Increment cumulative sort count and trash count
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementTrashCount()
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
        return comboCount
    }
    
    /**
     * Mark current photo as Maybe (swipe down).
     * @return The current combo count after sorting
     */
    fun maybeCurrentPhoto(): Int {
        val photo = uiState.value.currentPhoto ?: return 0
        // Update combo first for immediate feedback
        val comboCount = updateCombo()
        viewModelScope.launch {
            try {
                sortPhotoUseCase.maybePhoto(photo.id)
                _lastAction.value = SortAction(photo.id, PhotoStatus.MAYBE)
                _counters.value = _counters.value.copy(maybe = _counters.value.maybe + 1)
                // Increment cumulative sort count and maybe count
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementMaybeCount()
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
        return comboCount
    }
    
    /**
     * Undo the last sorting action.
     * Note: Undo does NOT decrement the cumulative count - it's a permanent achievement.
     */
    fun undoLastAction() {
        val lastAction = _lastAction.value ?: return
        viewModelScope.launch {
            try {
                sortPhotoUseCase.resetPhoto(lastAction.photoId)
                // Update session counters only (cumulative count stays)
                _counters.value = when (lastAction.status) {
                    PhotoStatus.KEEP -> _counters.value.copy(keep = (_counters.value.keep - 1).coerceAtLeast(0))
                    PhotoStatus.TRASH -> _counters.value.copy(trash = (_counters.value.trash - 1).coerceAtLeast(0))
                    PhotoStatus.MAYBE -> _counters.value.copy(maybe = (_counters.value.maybe - 1).coerceAtLeast(0))
                    else -> _counters.value
                }
                _lastAction.value = null
            } catch (e: Exception) {
                _error.value = "撤销失败: ${e.message}"
            }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Update combo state based on time since last swipe.
     * Called after each successful sort action.
     * Starts auto-hide timer for combo display.
     * 
     * @return The new combo count (for haptic feedback intensity)
     */
    private fun updateCombo(): Int {
        val currentTime = System.currentTimeMillis()
        val lastTime = _combo.value.lastSwipeTime
        val timeSinceLastSwipe = currentTime - lastTime
        
        val newCombo = if (lastTime == 0L || timeSinceLastSwipe <= ComboState.COMBO_THRESHOLD_MS) {
            // Within threshold - increment combo
            _combo.value.count + 1
        } else {
            // Too slow - reset to 1
            1
        }
        
        val newMaxCombo = maxOf(_combo.value.maxCount, newCombo)
        
        _combo.value = _combo.value.copy(
            count = newCombo,
            maxCount = newMaxCombo,
            lastSwipeTime = currentTime,
            isActive = true
        )
        
        // Update global max combo achievement
        viewModelScope.launch {
            preferencesRepository.updateMaxCombo(newMaxCombo)
        }
        
        // Cancel previous timeout and start new one
        startComboTimeout()
        
        return newCombo
    }
    
    /**
     * Start a timeout to auto-hide combo after COMBO_THRESHOLD_MS.
     * If user doesn't continue swiping, combo will reset and hide.
     */
    private fun startComboTimeout() {
        comboTimeoutJob?.cancel()
        comboTimeoutJob = viewModelScope.launch {
            delay(ComboState.COMBO_THRESHOLD_MS)
            // Time expired without new swipe - hide combo
            _combo.value = _combo.value.copy(isActive = false)
            // After a brief moment, reset the count for next session
            delay(300)
            _combo.value = ComboState(maxCount = _combo.value.maxCount)
        }
    }
    
    /**
     * Reset combo (e.g., when leaving the screen or after timeout).
     */
    fun resetCombo() {
        comboTimeoutJob?.cancel()
        _combo.value = ComboState()
    }
    
    /**
     * Get the current combo count.
     */
    fun getCurrentCombo(): Int = _combo.value.count
    
    /**
     * Get the max combo achieved in this session.
     */
    fun getMaxCombo(): Int = _combo.value.maxCount
    
    override fun onCleared() {
        super.onCleared()
        comboTimeoutJob?.cancel()
    }
}
