package com.example.photozen.ui.screens.lighttable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.domain.usecase.GetMaybePhotosUseCase
import com.example.photozen.domain.usecase.SortPhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View mode for Light Table screen.
 */
enum class LightTableMode {
    SELECTION,
    COMPARISON
}

/**
 * UI State for Light Table screen.
 */
data class LightTableUiState(
    val allMaybePhotos: List<PhotoEntity> = emptyList(),
    val selectedForComparison: Set<String> = emptySet(),
    val bestPhotoId: String? = null,
    val mode: LightTableMode = LightTableMode.SELECTION,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val comparisonPhotos: List<PhotoEntity>
        get() = allMaybePhotos.filter { it.id in selectedForComparison }
            .sortedBy { selectedForComparison.toList().indexOf(it.id) }
    
    val selectionCount: Int
        get() = selectedForComparison.size
    
    val canCompare: Boolean
        get() = selectedForComparison.size >= 2
    
    val maxSelectionReached: Boolean
        get() = selectedForComparison.size >= 4
}

/**
 * Internal state holder for non-photo data.
 */
private data class InternalState(
    val selectedForComparison: Set<String> = emptySet(),
    val bestPhotoId: String? = null,
    val mode: LightTableMode = LightTableMode.SELECTION,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for Light Table screen.
 */
@HiltViewModel
class LightTableViewModel @Inject constructor(
    private val getMaybePhotosUseCase: GetMaybePhotosUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase
) : ViewModel() {
    
    private val _internalState = MutableStateFlow(InternalState())
    
    val uiState: StateFlow<LightTableUiState> = combine(
        getMaybePhotosUseCase(),
        _internalState
    ) { photos, internal ->
        LightTableUiState(
            allMaybePhotos = photos,
            selectedForComparison = internal.selectedForComparison,
            bestPhotoId = internal.bestPhotoId,
            mode = internal.mode,
            isLoading = internal.isLoading && photos.isEmpty(),
            error = internal.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LightTableUiState()
    )
    
    init {
        viewModelScope.launch {
            _internalState.update { it.copy(isLoading = false) }
        }
    }
    
    fun toggleSelection(photoId: String) {
        _internalState.update { state ->
            val current = state.selectedForComparison
            val newSelection = if (photoId in current) {
                current - photoId
            } else {
                if (current.size < 4) current + photoId else current
            }
            state.copy(selectedForComparison = newSelection)
        }
    }
    
    fun clearSelection() {
        _internalState.update { 
            it.copy(selectedForComparison = emptySet(), bestPhotoId = null) 
        }
    }
    
    fun selectAll() {
        val allIds = uiState.value.allMaybePhotos.take(4).map { it.id }.toSet()
        _internalState.update { it.copy(selectedForComparison = allIds) }
    }
    
    fun startComparison() {
        if (_internalState.value.selectedForComparison.size >= 2) {
            _internalState.update { it.copy(mode = LightTableMode.COMPARISON) }
        }
    }
    
    fun exitComparison() {
        _internalState.update { 
            it.copy(mode = LightTableMode.SELECTION, bestPhotoId = null) 
        }
    }
    
    fun selectBestPhoto(photoId: String) {
        _internalState.update { state ->
            state.copy(bestPhotoId = if (state.bestPhotoId == photoId) null else photoId)
        }
    }
    
    fun keepBestTrashRest() {
        val best = _internalState.value.bestPhotoId ?: return
        val others = _internalState.value.selectedForComparison - best
        
        viewModelScope.launch {
            try {
                sortPhotoUseCase.keepPhoto(best)
                sortPhotoUseCase.batchUpdateStatus(others.toList(), PhotoStatus.TRASH)
                clearSelection()
                _internalState.update { it.copy(mode = LightTableMode.SELECTION) }
            } catch (e: Exception) {
                _internalState.update { it.copy(error = "操作失败: ${e.message}") }
            }
        }
    }
    
    fun keepAllSelected() {
        val selected = _internalState.value.selectedForComparison.toList()
        
        viewModelScope.launch {
            try {
                sortPhotoUseCase.batchUpdateStatus(selected, PhotoStatus.KEEP)
                clearSelection()
                _internalState.update { it.copy(mode = LightTableMode.SELECTION) }
            } catch (e: Exception) {
                _internalState.update { it.copy(error = "操作失败: ${e.message}") }
            }
        }
    }
    
    fun trashAllSelected() {
        val selected = _internalState.value.selectedForComparison.toList()
        
        viewModelScope.launch {
            try {
                sortPhotoUseCase.batchUpdateStatus(selected, PhotoStatus.TRASH)
                clearSelection()
                _internalState.update { it.copy(mode = LightTableMode.SELECTION) }
            } catch (e: Exception) {
                _internalState.update { it.copy(error = "操作失败: ${e.message}") }
            }
        }
    }
    
    fun clearError() {
        _internalState.update { it.copy(error = null) }
    }
}
