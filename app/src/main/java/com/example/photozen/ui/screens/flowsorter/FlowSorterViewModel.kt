package com.example.photozen.ui.screens.flowsorter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.domain.usecase.GetUnsortedPhotosUseCase
import com.example.photozen.domain.usecase.SortPhotoUseCase
import com.example.photozen.domain.usecase.SyncPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val error: String? = null
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
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(true)
    private val _isSyncing = MutableStateFlow(false)
    private val _currentIndex = MutableStateFlow(0)
    private val _lastAction = MutableStateFlow<SortAction?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _counters = MutableStateFlow(SortCounters())
    
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
     * UI State exposed to the screen.
     */
    val uiState: StateFlow<FlowSorterUiState> = combine(
        getUnsortedPhotosUseCase(),
        _isLoading,
        _isSyncing,
        _currentIndex,
        _lastAction,
        _error,
        _counters
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val photos = values[0] as List<PhotoEntity>
        val isLoading = values[1] as Boolean
        val isSyncing = values[2] as Boolean
        val currentIndex = values[3] as Int
        val lastAction = values[4] as SortAction?
        val error = values[5] as String?
        val counters = values[6] as SortCounters
        
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
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FlowSorterUiState()
    )
    
    init {
        // Perform initial sync if needed
        viewModelScope.launch {
            syncPhotos()
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
     */
    fun keepCurrentPhoto() {
        val photo = uiState.value.currentPhoto ?: return
        viewModelScope.launch {
            try {
                sortPhotoUseCase.keepPhoto(photo.id)
                _lastAction.value = SortAction(photo.id, PhotoStatus.KEEP)
                _counters.value = _counters.value.copy(keep = _counters.value.keep + 1)
                // Increment cumulative sort count
                preferencesRepository.incrementSortedCount()
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
    }
    
    /**
     * Trash the current photo (swipe left).
     */
    fun trashCurrentPhoto() {
        val photo = uiState.value.currentPhoto ?: return
        viewModelScope.launch {
            try {
                sortPhotoUseCase.trashPhoto(photo.id)
                _lastAction.value = SortAction(photo.id, PhotoStatus.TRASH)
                _counters.value = _counters.value.copy(trash = _counters.value.trash + 1)
                // Increment cumulative sort count
                preferencesRepository.incrementSortedCount()
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
    }
    
    /**
     * Mark current photo as Maybe (swipe up).
     */
    fun maybeCurrentPhoto() {
        val photo = uiState.value.currentPhoto ?: return
        viewModelScope.launch {
            try {
                sortPhotoUseCase.maybePhoto(photo.id)
                _lastAction.value = SortAction(photo.id, PhotoStatus.MAYBE)
                _counters.value = _counters.value.copy(maybe = _counters.value.maybe + 1)
                // Increment cumulative sort count
                preferencesRepository.incrementSortedCount()
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
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
}
