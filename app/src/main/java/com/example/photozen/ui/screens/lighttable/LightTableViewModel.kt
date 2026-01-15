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
    val selectedInComparison: Set<String> = emptySet(), // 对比模式中选中的照片（用于保留）
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
        get() = selectedForComparison.size >= MAX_COMPARISON_PHOTOS
    
    val hasSelectedInComparison: Boolean
        get() = selectedInComparison.isNotEmpty()
    
    companion object {
        const val MAX_COMPARISON_PHOTOS = 6
    }
}

/**
 * Internal state holder for non-photo data.
 */
private data class InternalState(
    val selectedForComparison: Set<String> = emptySet(),
    val selectedInComparison: Set<String> = emptySet(),
    val mode: LightTableMode = LightTableMode.SELECTION,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** When set, only show photos with these IDs (for workflow session mode) */
    val sessionPhotoIds: Set<String>? = null
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
        // Filter photos by session IDs if in workflow mode
        val filteredPhotos = if (internal.sessionPhotoIds != null) {
            photos.filter { it.id in internal.sessionPhotoIds }
        } else {
            photos
        }
        
        LightTableUiState(
            allMaybePhotos = filteredPhotos,
            selectedForComparison = internal.selectedForComparison,
            selectedInComparison = internal.selectedInComparison,
            mode = internal.mode,
            isLoading = internal.isLoading && filteredPhotos.isEmpty(),
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
    
    /**
     * Set session photo IDs for workflow mode filtering.
     * When set, only photos with these IDs will be shown.
     */
    fun setSessionPhotoIds(photoIds: Set<String>?) {
        _internalState.update { it.copy(sessionPhotoIds = photoIds) }
    }
    
    fun toggleSelection(photoId: String) {
        _internalState.update { state ->
            val current = state.selectedForComparison
            val newSelection = if (photoId in current) {
                current - photoId
            } else {
                if (current.size < LightTableUiState.MAX_COMPARISON_PHOTOS) current + photoId else current
            }
            state.copy(selectedForComparison = newSelection)
        }
    }
    
    fun clearSelection() {
        _internalState.update { 
            it.copy(selectedForComparison = emptySet(), selectedInComparison = emptySet()) 
        }
    }
    
    fun selectAll() {
        val allIds = uiState.value.allMaybePhotos.take(LightTableUiState.MAX_COMPARISON_PHOTOS).map { it.id }.toSet()
        _internalState.update { it.copy(selectedForComparison = allIds) }
    }
    
    fun startComparison() {
        if (_internalState.value.selectedForComparison.size >= 2) {
            _internalState.update { it.copy(mode = LightTableMode.COMPARISON, selectedInComparison = emptySet()) }
        }
    }
    
    fun exitComparison() {
        _internalState.update { 
            it.copy(mode = LightTableMode.SELECTION, selectedInComparison = emptySet()) 
        }
    }
    
    /**
     * 在对比模式中切换照片选中状态（多选）
     */
    fun toggleComparisonSelection(photoId: String) {
        _internalState.update { state ->
            val current = state.selectedInComparison
            val newSelection = if (photoId in current) {
                current - photoId
            } else {
                current + photoId
            }
            state.copy(selectedInComparison = newSelection)
        }
    }
    
    /**
     * 保留选中的照片，丢弃未选中的
     */
    fun keepSelectedTrashRest() {
        val selected = _internalState.value.selectedInComparison
        if (selected.isEmpty()) return
        
        val others = _internalState.value.selectedForComparison - selected
        
        viewModelScope.launch {
            try {
                sortPhotoUseCase.batchUpdateStatus(selected.toList(), PhotoStatus.KEEP)
                if (others.isNotEmpty()) {
                    sortPhotoUseCase.batchUpdateStatus(others.toList(), PhotoStatus.TRASH)
                }
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
