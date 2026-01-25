package com.example.photozen.ui.screens.trash

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.ManageTrashUseCase
import com.example.photozen.domain.usecase.PhotoBatchOperationUseCase
import com.example.photozen.ui.components.PhotoGridMode
import com.example.photozen.ui.state.PhotoSelectionStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val deleteIntentSender: IntentSender? = null,
    val message: String? = null,
    val gridColumns: Int = 2,
    val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,
    val inSelectionMode: Boolean = false,
    // REQ-033: 排序
    val sortOrder: com.example.photozen.ui.screens.photolist.PhotoListSortOrder =
        com.example.photozen.ui.screens.photolist.PhotoListSortOrder.ADDED_DESC // 默认按添加时间倒序
) {
    val isSelectionMode: Boolean get() = inSelectionMode || selectedIds.isNotEmpty()
    val selectedCount: Int get() = selectedIds.size
    val allSelected: Boolean get() = photos.isNotEmpty() && selectedIds.size == photos.size
}

// Phase 4 清理：selectedIds 和 inSelectionMode 已迁移到 PhotoSelectionStateHolder
private data class InternalState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val deleteIntentSender: IntentSender? = null,
    val message: String? = null,
    val gridColumns: Int = 2,
    val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,
    // REQ-033: 排序
    val sortOrder: com.example.photozen.ui.screens.photolist.PhotoListSortOrder =
        com.example.photozen.ui.screens.photolist.PhotoListSortOrder.ADDED_DESC
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val manageTrashUseCase: ManageTrashUseCase,
    private val preferencesRepository: com.example.photozen.data.repository.PreferencesRepository,
    // Phase 4: 共享状态和批量操作
    private val selectionStateHolder: PhotoSelectionStateHolder,
    private val batchOperationUseCase: PhotoBatchOperationUseCase
) : ViewModel() {
    
    private val _internalState = MutableStateFlow(InternalState())
    
    // Random seed for consistent random sorting
    private var randomSeed = System.currentTimeMillis()

    // Phase 4: 使用 selectionStateHolder 的状态
    val uiState: StateFlow<TrashUiState> = combine(
        getPhotosUseCase.getPhotosByStatus(PhotoStatus.TRASH),
        _internalState,
        selectionStateHolder.selectedIds,
        selectionStateHolder.isSelectionMode
    ) { photos, internal, selectedIds, isSelectionMode ->
        // REQ-033: Apply sorting
        val sortedPhotos = applySortOrder(photos, internal.sortOrder)

        // Filter out selected photos that no longer exist
        val validSelectedIds = selectedIds.filter { id ->
            sortedPhotos.any { it.id == id }
        }.toSet()

        TrashUiState(
            photos = sortedPhotos,
            selectedIds = validSelectedIds,
            isLoading = internal.isLoading && photos.isEmpty(),
            isDeleting = internal.isDeleting,
            deleteIntentSender = internal.deleteIntentSender,
            message = internal.message,
            gridColumns = internal.gridColumns,
            gridMode = internal.gridMode,
            inSelectionMode = isSelectionMode || validSelectedIds.isNotEmpty(),
            sortOrder = internal.sortOrder
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrashUiState()
    )

    /**
     * Apply sort order to photos list (REQ-033)
     */
    private fun applySortOrder(
        photos: List<PhotoEntity>,
        sortOrder: com.example.photozen.ui.screens.photolist.PhotoListSortOrder
    ): List<PhotoEntity> {
        return when (sortOrder) {
            com.example.photozen.ui.screens.photolist.PhotoListSortOrder.DATE_DESC ->
                photos.sortedByDescending { it.dateTaken }
            com.example.photozen.ui.screens.photolist.PhotoListSortOrder.DATE_ASC ->
                photos.sortedBy { it.dateTaken }
            com.example.photozen.ui.screens.photolist.PhotoListSortOrder.ADDED_DESC ->
                photos.sortedByDescending { it.updatedAt }
            com.example.photozen.ui.screens.photolist.PhotoListSortOrder.ADDED_ASC ->
                photos.sortedBy { it.updatedAt }
            com.example.photozen.ui.screens.photolist.PhotoListSortOrder.RANDOM ->
                photos.shuffled(kotlin.random.Random(randomSeed))
        }
    }

    /**
     * Set sort order (REQ-033)
     */
    fun setSortOrder(order: com.example.photozen.ui.screens.photolist.PhotoListSortOrder) {
        if (order == com.example.photozen.ui.screens.photolist.PhotoListSortOrder.RANDOM) {
            randomSeed = System.currentTimeMillis()
        }
        _internalState.update { it.copy(sortOrder = order) }
    }
    
    init {
        // Phase 4: 进入页面时清空选择状态
        selectionStateHolder.clear()
        
        viewModelScope.launch {
            _internalState.update { it.copy(isLoading = false) }
        }
        // Load grid columns preference
        viewModelScope.launch {
            preferencesRepository.getGridColumns(
                com.example.photozen.data.repository.PreferencesRepository.GridScreen.TRASH
            ).collect { columns ->
                _internalState.update { it.copy(gridColumns = columns) }
            }
        }
    }
    
    // Phase 4: 选择操作委托给 StateHolder
    fun toggleSelection(photoId: String) {
        selectionStateHolder.toggle(photoId)
    }
    
    /**
     * Update selection with a new set of IDs (for drag-select).
     * Phase 4: 委托给 StateHolder
     */
    fun updateSelection(newSelection: Set<String>) {
        selectionStateHolder.setSelection(newSelection)
    }
    
    /**
     * Phase 4: 委托给 StateHolder
     */
    fun selectAll() {
        val allIds = uiState.value.photos.map { it.id }
        selectionStateHolder.selectAll(allIds)
    }
    
    /**
     * Phase 4: 委托给 StateHolder
     */
    fun clearSelection() {
        selectionStateHolder.clear()
    }
    
    /**
     * 进入选择模式
     * Phase 4: 委托给 StateHolder
     */
    fun enterSelectionMode() {
        selectionStateHolder.enterSelectionMode()
    }
    
    /**
     * Move selected photos to Keep.
     * Phase 4: 使用 BatchUseCase
     */
    fun keepSelected() {
        val selected = selectionStateHolder.getSelectedList()
        if (selected.isEmpty()) return
        
        viewModelScope.launch {
            batchOperationUseCase.batchRestoreFromTrash(selected, PhotoStatus.KEEP)
            selectionStateHolder.clear()
        }
    }
    
    /**
     * Move selected photos to Maybe.
     * Phase 4: 使用 BatchUseCase
     */
    fun maybeSelected() {
        val selected = selectionStateHolder.getSelectedList()
        if (selected.isEmpty()) return
        
        viewModelScope.launch {
            batchOperationUseCase.batchRestoreFromTrash(selected, PhotoStatus.MAYBE)
            selectionStateHolder.clear()
        }
    }
    
    fun cycleGridColumns() {
        viewModelScope.launch {
            val newColumns = preferencesRepository.cycleGridColumns(
                com.example.photozen.data.repository.PreferencesRepository.GridScreen.TRASH
            )
            _internalState.update { it.copy(gridColumns = newColumns) }
        }
    }

    /**
     * Set grid columns directly (for pinch-zoom gesture).
     * REQ-003, REQ-008: 双指缩放切换列数
     */
    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            // Apply limits based on current grid mode
            val minColumns = if (_internalState.value.gridMode == PhotoGridMode.SQUARE) 2 else 1
            val validColumns = columns.coerceIn(minColumns, 5)
            preferencesRepository.setGridColumns(
                com.example.photozen.data.repository.PreferencesRepository.GridScreen.TRASH,
                validColumns
            )
            _internalState.update { it.copy(gridColumns = validColumns) }
        }
    }

    /**
     * Toggle between SQUARE (grid) and WATERFALL (staggered) modes.
     * SQUARE mode supports drag-to-select, WATERFALL mode does not.
     */
    fun toggleGridMode() {
        _internalState.update { state ->
            val newMode = when (state.gridMode) {
                PhotoGridMode.SQUARE -> PhotoGridMode.WATERFALL
                PhotoGridMode.WATERFALL -> PhotoGridMode.SQUARE
            }
            state.copy(gridMode = newMode)
        }
    }

    /**
     * Set grid mode directly.
     * @param mode The new grid mode
     */
    fun setGridMode(mode: PhotoGridMode) {
        _internalState.update { state ->
            // Adjust columns if switching to SQUARE mode and current columns < 2
            val newColumns = if (mode == PhotoGridMode.SQUARE && state.gridColumns < 2) {
                2
            } else {
                state.gridColumns
            }
            state.copy(gridMode = mode, gridColumns = newColumns)
        }
    }

    /**
     * Restore selected photos to Unsorted.
     * Phase 4: 使用 BatchUseCase
     */
    fun restoreSelected() {
        val selected = selectionStateHolder.getSelectedList()
        if (selected.isEmpty()) return
        
        viewModelScope.launch {
            batchOperationUseCase.batchRestoreFromTrash(selected, PhotoStatus.UNSORTED)
            selectionStateHolder.clear()
        }
    }
    
    /**
     * Request permanent deletion via system API.
     * For Android 11+, this creates an IntentSender for user confirmation.
     * For older versions, we just remove from our database (actual file deletion 
     * would require WRITE_EXTERNAL_STORAGE which is restricted).
     * 
     * Note: 永久删除保持原逻辑，不使用 BatchUseCase，因为涉及系统权限请求。
     */
    fun requestPermanentDelete() {
        // Phase 4: 使用 selectionStateHolder 获取选中的照片
        val selectedIds = selectionStateHolder.getSelectedList()
        val selectedPhotos = uiState.value.photos.filter { it.id in selectedIds }
        if (selectedPhotos.isEmpty()) return
        
        viewModelScope.launch {
            _internalState.update { it.copy(isDeleting = true) }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+: Use MediaStore.createDeleteRequest
                    val uris = selectedPhotos.mapNotNull { photo ->
                        try {
                            Uri.parse(photo.systemUri)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (uris.isNotEmpty()) {
                        val intentSender = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            uris
                        ).intentSender
                        
                        _internalState.update { 
                            it.copy(deleteIntentSender = intentSender, isDeleting = false)
                        }
                    }
                } else {
                    // Older versions: Just remove selected photos from our database
                    // (actual file deletion not supported without special permissions)
                    val count = selectedIds.size
                    manageTrashUseCase.deletePhotos(selectedIds)
                    selectionStateHolder.clear()
                    _internalState.update { 
                        it.copy(
                            isDeleting = false,
                            message = "已从整理记录中移除 $count 张照片"
                        )
                    }
                }
            } catch (e: Exception) {
                _internalState.update { 
                    it.copy(isDeleting = false, message = "删除失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Called after system delete dialog completes.
     * Phase 4: 使用 selectionStateHolder
     */
    fun onDeleteComplete(success: Boolean) {
        viewModelScope.launch {
            if (success) {
                // Remove only the selected photos from our database
                val selectedIds = selectionStateHolder.getSelectedList()
                val count = selectedIds.size
                manageTrashUseCase.deletePhotos(selectedIds)
                
                // Update achievement progress - "清洁工" and "清理大师" achievements
                // Only count if all selected photos were deleted (full trash clear or selection clear)
                val allSelected = uiState.value.allSelected
                if (allSelected && count > 0) {
                    preferencesRepository.incrementTrashEmptied()
                }
                
                selectionStateHolder.clear()
                _internalState.update { 
                    it.copy(
                        deleteIntentSender = null,
                        message = "已彻底删除 $count 张照片"
                    )
                }
            } else {
                _internalState.update { 
                    it.copy(deleteIntentSender = null, message = "删除已取消")
                }
            }
        }
    }
    
    fun clearIntentSender() {
        _internalState.update { it.copy(deleteIntentSender = null) }
    }
    
    fun clearMessage() {
        _internalState.update { it.copy(message = null) }
    }
    
    /**
     * Phase 4: 页面销毁时清理选择状态
     */
    override fun onCleared() {
        super.onCleared()
        selectionStateHolder.clear()
    }
}
