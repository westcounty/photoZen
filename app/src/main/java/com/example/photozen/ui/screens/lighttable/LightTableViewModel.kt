package com.example.photozen.ui.screens.lighttable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.domain.usecase.GetMaybePhotosUseCase
import com.example.photozen.domain.usecase.PhotoBatchOperationUseCase
import com.example.photozen.domain.usecase.SortPhotoUseCase
import com.example.photozen.ui.components.PhotoGridMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
 * Sort order for Light Table photos.
 */
enum class LightTableSortOrder(val displayName: String) {
    PHOTO_DATE_DESC("照片时间倒序"),
    PHOTO_DATE_ASC("照片时间正序"),
    ADDED_DATE_DESC("添加时间倒序"),
    ADDED_DATE_ASC("添加时间正序")
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
    val error: String? = null,
    // 新增：排序和视图模式
    val sortOrder: LightTableSortOrder = LightTableSortOrder.PHOTO_DATE_DESC,
    val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,
    val gridColumns: Int = 3
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
    val sessionPhotoIds: Set<String>? = null,
    // 新增：排序和视图模式
    val sortOrder: LightTableSortOrder = LightTableSortOrder.PHOTO_DATE_DESC,
    val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,
    val gridColumns: Int = 3
)

/**
 * ViewModel for Light Table screen.
 */
@HiltViewModel
class LightTableViewModel @Inject constructor(
    private val getMaybePhotosUseCase: GetMaybePhotosUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val preferencesRepository: PreferencesRepository,
    // Phase 4: 批量操作 UseCase
    private val batchOperationUseCase: PhotoBatchOperationUseCase
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

        // Sort photos based on current sort order
        val sortedPhotos = when (internal.sortOrder) {
            LightTableSortOrder.PHOTO_DATE_DESC -> filteredPhotos.sortedByDescending { it.dateTaken }
            LightTableSortOrder.PHOTO_DATE_ASC -> filteredPhotos.sortedBy { it.dateTaken }
            LightTableSortOrder.ADDED_DATE_DESC -> filteredPhotos.sortedByDescending { it.dateAdded }
            LightTableSortOrder.ADDED_DATE_ASC -> filteredPhotos.sortedBy { it.dateAdded }
        }

        LightTableUiState(
            allMaybePhotos = sortedPhotos,
            selectedForComparison = internal.selectedForComparison,
            selectedInComparison = internal.selectedInComparison,
            mode = internal.mode,
            isLoading = internal.isLoading && sortedPhotos.isEmpty(),
            error = internal.error,
            sortOrder = internal.sortOrder,
            gridMode = internal.gridMode,
            gridColumns = internal.gridColumns
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LightTableUiState()
    )

    init {
        viewModelScope.launch {
            // Load saved grid settings
            val savedColumns = preferencesRepository.getGridColumns(PreferencesRepository.GridScreen.MAYBE).first()
            val savedMode = preferencesRepository.getGridMode(PreferencesRepository.GridScreen.MAYBE).first()
            _internalState.update {
                it.copy(
                    isLoading = false,
                    gridColumns = savedColumns,
                    gridMode = savedMode
                )
            }
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
     * 
     * Phase 4: 使用 PhotoBatchOperationUseCase 统一处理批量操作
     */
    fun keepSelectedTrashRest() {
        val selected = _internalState.value.selectedInComparison
        if (selected.isEmpty()) return
        
        val others = _internalState.value.selectedForComparison - selected
        
        viewModelScope.launch {
            try {
                // Phase 4: 使用 BatchUseCase 执行批量操作
                batchOperationUseCase.batchKeep(selected.toList(), showUndo = true)
                if (others.isNotEmpty()) {
                    batchOperationUseCase.batchTrash(others.toList(), showUndo = true)
                }
                clearSelection()
                _internalState.update { it.copy(mode = LightTableMode.SELECTION) }
            } catch (e: Exception) {
                _internalState.update { it.copy(error = "操作失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Phase 4: 使用 PhotoBatchOperationUseCase 统一处理批量操作
     */
    fun keepAllSelected() {
        val selected = _internalState.value.selectedForComparison.toList()
        
        viewModelScope.launch {
            try {
                // Phase 4: 使用 BatchUseCase 执行批量操作
                batchOperationUseCase.batchKeep(selected, showUndo = true)
                clearSelection()
                _internalState.update { it.copy(mode = LightTableMode.SELECTION) }
            } catch (e: Exception) {
                _internalState.update { it.copy(error = "操作失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Phase 4: 使用 PhotoBatchOperationUseCase 统一处理批量操作
     */
    fun trashAllSelected() {
        val selected = _internalState.value.selectedForComparison.toList()
        
        viewModelScope.launch {
            try {
                // Phase 4: 使用 BatchUseCase 执行批量操作
                batchOperationUseCase.batchTrash(selected, showUndo = true)
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

    /**
     * Set sort order for photos.
     */
    fun setSortOrder(order: LightTableSortOrder) {
        _internalState.update { it.copy(sortOrder = order) }
    }

    /**
     * Set grid display mode.
     */
    fun setGridMode(mode: PhotoGridMode) {
        _internalState.update { it.copy(gridMode = mode) }
        viewModelScope.launch {
            preferencesRepository.setGridMode(PreferencesRepository.GridScreen.MAYBE, mode)
        }
    }

    /**
     * Set grid columns.
     */
    fun setGridColumns(columns: Int) {
        val minCols = if (_internalState.value.gridMode == PhotoGridMode.WATERFALL) 1 else 2
        val newColumns = columns.coerceIn(minCols, 5)
        _internalState.update { it.copy(gridColumns = newColumns) }
        viewModelScope.launch {
            preferencesRepository.setGridColumns(PreferencesRepository.GridScreen.MAYBE, newColumns)
        }
    }
}
