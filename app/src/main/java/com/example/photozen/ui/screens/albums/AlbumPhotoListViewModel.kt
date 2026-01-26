package com.example.photozen.ui.screens.albums

import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.AlbumAddAction
import com.example.photozen.data.repository.CustomFilterSession
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.MovePhotoResult
import com.example.photozen.ui.components.PhotoGridMode
import com.example.photozen.ui.screens.photolist.PhotoListSortOrder
import com.example.photozen.ui.state.PhotoSelectionStateHolder
import com.example.photozen.util.StoragePermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View mode for album photo list.
 */
enum class AlbumPhotoListViewMode {
    GRID_1,  // 1-column grid (single column, waterfall style)
    GRID_2,  // 2-column grid
    GRID_3,  // 3-column grid
    GRID_4   // 4-column grid
}

/**
 * Pending operation that requires user confirmation.
 */
data class PendingAlbumOperation(
    val intentSender: IntentSender,
    val photoUri: Uri,
    val targetBucketId: String,
    val operationType: String  // "move" or "delete"
)

/**
 * UI State for Album Photo List screen.
 */
data class AlbumPhotoListUiState(
    val bucketId: String = "",
    val albumName: String = "",
    val photos: List<PhotoEntity> = emptyList(),
    val allPhotos: List<PhotoEntity> = emptyList(),  // Unfiltered photos
    val totalCount: Int = 0,
    val sortedCount: Int = 0,
    val viewMode: AlbumPhotoListViewMode = AlbumPhotoListViewMode.GRID_2,
    val isLoading: Boolean = true,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val pendingOperation: PendingAlbumOperation? = null,
    val pendingDeleteIds: List<String> = emptyList(),  // Track IDs pending deletion for Room sync
    val error: String? = null,
    val message: String? = null,
    // Phase 7.2: Global status filter
    val statusFilter: Set<PhotoStatus> = PhotoStatus.entries.toSet(),
    // REQ-002, REQ-007: 网格模式和列数
    val isSquareMode: Boolean = true,  // true=网格, false=瀑布流
    val columnCount: Int = 3,  // 列数
    // REQ-038: 排序
    val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC,
    // Permission dialog for move operations
    val showPermissionDialog: Boolean = false,
    val permissionRetryError: Boolean = false
) {
    val selectedCount: Int get() = selectedIds.size
    val unsortedCount: Int get() = totalCount - sortedCount
    val sortedPercentage: Float get() = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
    val isFilterActive: Boolean get() = statusFilter.size < PhotoStatus.entries.size
    // 计算当前 gridMode
    val gridMode: PhotoGridMode get() = if (isSquareMode) PhotoGridMode.SQUARE else PhotoGridMode.WATERFALL
}

/**
 * ViewModel for Album Photo List screen.
 */
@HiltViewModel
class AlbumPhotoListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoDao: PhotoDao,
    private val albumBubbleDao: AlbumBubbleDao,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val preferencesRepository: PreferencesRepository,
    // Phase 4: 共享选择状态
    private val selectionStateHolder: PhotoSelectionStateHolder,
    private val storagePermissionHelper: StoragePermissionHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AlbumPhotoListUiState())
    
    // Phase 4: 组合 uiState 以包含共享选择状态
    val uiState: StateFlow<AlbumPhotoListUiState> = kotlinx.coroutines.flow.combine(
        _uiState,
        selectionStateHolder.selectedIds,
        selectionStateHolder.isSelectionMode
    ) { state, selectedIds, isSelectionMode ->
        // Filter out selected photos that no longer exist
        val validSelectedIds = selectedIds.filter { id ->
            state.photos.any { it.id == id }
        }.toSet()
        
        state.copy(
            selectedIds = validSelectedIds,
            isSelectionMode = isSelectionMode || validSelectedIds.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumPhotoListUiState()
    )
    
    private var loadJob: Job? = null
    
    /**
     * Initialize with album info.
     * Phase 4: 进入页面时清空选择状态
     */
    fun initialize(bucketId: String, albumName: String) {
        selectionStateHolder.clear()
        _uiState.update { it.copy(bucketId = bucketId, albumName = albumName) }
        loadPhotos()
        loadAlbumBubbleList()
        loadSettings()
    }
    
    /**
     * Load photos from the album.
     */
    private fun loadPhotos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val bucketId = _uiState.value.bucketId
            
            // Guard: Don't load if bucketId is empty (not initialized yet)
            if (bucketId.isEmpty()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                photoDao.getPhotosByBucketId(bucketId).collect { allPhotos ->
                    val sortedCount = allPhotos.count { it.status != PhotoStatus.UNSORTED }
                    val statusFilter = _uiState.value.statusFilter
                    val sortOrder = _uiState.value.sortOrder
                    // Apply status filter
                    val filteredPhotos = if (statusFilter.size == PhotoStatus.entries.size) {
                        allPhotos
                    } else {
                        allPhotos.filter { it.status in statusFilter }
                    }
                    // REQ-038: Apply sorting
                    val sortedPhotos = applySortOrder(filteredPhotos, sortOrder)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            photos = sortedPhotos,
                            allPhotos = allPhotos,
                            totalCount = allPhotos.size,
                            sortedCount = sortedCount,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载照片失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Load album bubble list for target selection.
     */
    private fun loadAlbumBubbleList() {
        viewModelScope.launch {
            try {
                albumBubbleDao.getAll().collect { albums ->
                    _uiState.update { it.copy(albumBubbleList = albums) }
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }
    
    /**
     * Load settings.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            preferencesRepository.getAlbumAddAction().collect { action ->
                _uiState.update { it.copy(albumAddAction = action) }
            }
        }
        // Phase 7.2: Load status filter preference
        viewModelScope.launch {
            preferencesRepository.getAlbumPhotoStatusFilter().collect { filter ->
                _uiState.update { currentState ->
                    // Re-apply filter to photos
                    val filteredPhotos = if (filter.size == PhotoStatus.entries.size) {
                        currentState.allPhotos
                    } else {
                        currentState.allPhotos.filter { it.status in filter }
                    }
                    currentState.copy(
                        statusFilter = filter,
                        photos = filteredPhotos
                    )
                }
            }
        }
    }
    
    /**
     * Refresh photos.
     */
    fun refresh() {
        loadPhotos()
    }
    
    /**
     * Toggle view mode (1 -> 2 -> 3 -> 4 -> 1).
     * @deprecated Use toggleGridMode() and setGridColumns() instead
     */
    fun cycleViewMode() {
        _uiState.update {
            it.copy(
                viewMode = when (it.viewMode) {
                    AlbumPhotoListViewMode.GRID_1 -> AlbumPhotoListViewMode.GRID_2
                    AlbumPhotoListViewMode.GRID_2 -> AlbumPhotoListViewMode.GRID_3
                    AlbumPhotoListViewMode.GRID_3 -> AlbumPhotoListViewMode.GRID_4
                    AlbumPhotoListViewMode.GRID_4 -> AlbumPhotoListViewMode.GRID_1
                }
            )
        }
    }

    // ==================== REQ-002, REQ-003, REQ-007, REQ-008: 网格模式和双指缩放 ====================

    /**
     * Toggle between SQUARE (grid) and WATERFALL (staggered) modes.
     * REQ-027: 视图模式切换按钮
     */
    fun toggleGridMode() {
        _uiState.update { state ->
            val newIsSquare = !state.isSquareMode
            // 切换到网格模式时，列数最少为2
            val newColumns = if (newIsSquare && state.columnCount < 2) 2 else state.columnCount
            state.copy(isSquareMode = newIsSquare, columnCount = newColumns)
        }
    }

    /**
     * Set grid columns directly (for pinch-zoom gesture).
     * REQ-003, REQ-008: 双指缩放切换列数
     */
    fun setGridColumns(columns: Int) {
        _uiState.update { state ->
            val minColumns = if (state.isSquareMode) 2 else 1
            val validColumns = columns.coerceIn(minColumns, 5)
            state.copy(columnCount = validColumns)
        }
    }

    /**
     * Set grid mode directly.
     * @param mode The new grid mode
     */
    fun setGridMode(mode: PhotoGridMode) {
        _uiState.update { state ->
            val newIsSquare = mode == PhotoGridMode.SQUARE
            // 切换到网格模式时，列数最少为2
            val newColumns = if (newIsSquare && state.columnCount < 2) 2 else state.columnCount
            state.copy(isSquareMode = newIsSquare, columnCount = newColumns)
        }
    }

    /**
     * Set columns directly (alias for setGridColumns for consistency).
     */
    fun setColumns(columns: Int) = setGridColumns(columns)

    // ==================== REQ-038: 相册列表排序 ====================

    // Random seed for consistent random sorting
    private var randomSeed = System.currentTimeMillis()

    /**
     * Set sort order (REQ-038)
     */
    fun setSortOrder(order: PhotoListSortOrder) {
        if (order == PhotoListSortOrder.RANDOM) {
            randomSeed = System.currentTimeMillis()
        }
        _uiState.update { state ->
            // Re-apply sorting to current photos
            val sortedPhotos = applySortOrder(state.photos, order)
            state.copy(sortOrder = order, photos = sortedPhotos)
        }
    }

    /**
     * Apply sort order to photos list (REQ-038)
     * Uses effective time: prefers dateTaken (EXIF), falls back to dateAdded * 1000
     */
    private fun applySortOrder(
        photos: List<PhotoEntity>,
        sortOrder: PhotoListSortOrder
    ): List<PhotoEntity> {
        return when (sortOrder) {
            PhotoListSortOrder.DATE_DESC -> photos.sortedByDescending { it.getEffectiveTime() }
            PhotoListSortOrder.DATE_ASC -> photos.sortedBy { it.getEffectiveTime() }
            PhotoListSortOrder.ADDED_DESC -> photos.sortedByDescending { it.updatedAt }
            PhotoListSortOrder.ADDED_ASC -> photos.sortedBy { it.updatedAt }
            PhotoListSortOrder.RANDOM -> photos.shuffled(kotlin.random.Random(randomSeed))
        }
    }

    /**
     * Get effective time for sorting: prefers dateTaken, falls back to dateAdded * 1000
     * Matches the database COALESCE logic in PhotoDao
     */
    private fun PhotoEntity.getEffectiveTime(): Long {
        return if (dateTaken > 0) dateTaken else dateAdded * 1000
    }

    /**
     * Enter selection mode.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun enterSelectionMode(photoId: String) {
        selectionStateHolder.clear()
        selectionStateHolder.select(photoId)
    }
    
    /**
     * Toggle photo selection.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun togglePhotoSelection(photoId: String) {
        selectionStateHolder.toggle(photoId)
    }
    
    /**
     * Select all photos.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun selectAll() {
        val allIds = _uiState.value.photos.map { it.id }
        selectionStateHolder.selectAll(allIds)
    }
    
    /**
     * Clear selection.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun clearSelection() {
        selectionStateHolder.clear()
    }
    
    /**
     * Update selection state (用于 DragSelectPhotoGrid 回调).
     * Phase 4: 委托给 PhotoSelectionStateHolder
     * 
     * 当用户通过拖动选择多张照片时，DragSelectPhotoGrid 会调用此方法
     * 更新整个选中集合。
     * 
     * @param newSelection 新的选中照片 ID 集合
     */
    fun updateSelection(newSelection: Set<String>) {
        selectionStateHolder.setSelection(newSelection)
    }
    
    // Pending move operation for permission flow
    private var pendingMoveTargetBucketId: String? = null

    /**
     * Move selected photos to another album.
     * Phase 4: 使用 selectionStateHolder
     */
    fun moveSelectedToAlbum(targetBucketId: String) {
        // Check permission for move operation
        if (storagePermissionHelper.isManageStoragePermissionApplicable() &&
            !storagePermissionHelper.hasManageStoragePermission()) {
            // Save pending operation and show permission dialog
            pendingMoveTargetBucketId = targetBucketId
            _uiState.update {
                it.copy(
                    showPermissionDialog = true,
                    permissionRetryError = false
                )
            }
            return
        }

        executeMoveSelectedToAlbum(targetBucketId)
    }

    /**
     * Execute the move operation after permission check.
     */
    private fun executeMoveSelectedToAlbum(targetBucketId: String) {
        viewModelScope.launch {
            val selectedIds = selectionStateHolder.getSelectedList().toSet()
            val selectedPhotos = _uiState.value.photos.filter { it.id in selectedIds }
            var successCount = 0
            var needsConfirmation = false
            val movedPhotoIds = mutableListOf<String>()

            // Get target album path once before loop
            val targetAlbum = _uiState.value.albumBubbleList.find { it.bucketId == targetBucketId }
            // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
            val targetPath = mediaStoreDataSource.getAlbumPath(targetBucketId)
                ?: "Pictures/${targetAlbum?.displayName ?: "PhotoZen"}"

            for (photo in selectedPhotos) {
                val photoUri = Uri.parse(photo.systemUri)

                when (val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)) {
                    is MovePhotoResult.Success -> {
                        successCount++
                        movedPhotoIds.add(photo.id)
                    }
                    is MovePhotoResult.NeedsConfirmation -> {
                        needsConfirmation = true
                        _uiState.update {
                            it.copy(
                                pendingOperation = PendingAlbumOperation(
                                    intentSender = result.intentSender,
                                    photoUri = photoUri,
                                    targetBucketId = targetBucketId,
                                    operationType = "move"
                                )
                            )
                        }
                        break // Handle one at a time
                    }
                    is MovePhotoResult.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }
                }
            }

            if (successCount > 0) {
                // Sync Room database: update bucket_id for moved photos
                // This ensures the photo list updates immediately without needing app restart
                for (photoId in movedPhotoIds) {
                    photoDao.updateBucketId(photoId, targetBucketId)
                }

                _uiState.update { it.copy(message = "已移动 $successCount 张照片") }
                clearSelection()
                // Note: Room Flow will automatically update UI since we updated the database
            }
        }
    }

    // ==================== Permission Dialog ====================

    /**
     * Called when user returns from settings after granting permission.
     */
    fun onPermissionGranted() {
        if (storagePermissionHelper.hasManageStoragePermission()) {
            // Permission granted, execute pending operation
            val targetBucketId = pendingMoveTargetBucketId

            // Close dialog and clear pending state
            _uiState.update {
                it.copy(
                    showPermissionDialog = false,
                    permissionRetryError = false
                )
            }
            pendingMoveTargetBucketId = null

            // Execute the pending operation
            if (targetBucketId != null) {
                executeMoveSelectedToAlbum(targetBucketId)
            }
        } else {
            // Permission still not granted, show error
            _uiState.update { it.copy(permissionRetryError = true) }
        }
    }

    /**
     * Dismiss permission dialog without granting permission.
     */
    fun dismissPermissionDialog() {
        _uiState.update {
            it.copy(
                showPermissionDialog = false,
                permissionRetryError = false
            )
        }
        pendingMoveTargetBucketId = null
    }
    
    /**
     * Copy selected photos to another album.
     * Phase 4: 使用 selectionStateHolder
     */
    fun copySelectedToAlbum(targetBucketId: String) {
        viewModelScope.launch {
            val selectedIds = selectionStateHolder.getSelectedList().toSet()
            val selectedPhotos = _uiState.value.photos.filter { it.id in selectedIds }
            var successCount = 0
            
            // Get target album path once before loop
            val targetAlbum = _uiState.value.albumBubbleList.find { it.bucketId == targetBucketId }
            // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
            val targetPath = mediaStoreDataSource.getAlbumPath(targetBucketId)
                ?: "Pictures/${targetAlbum?.displayName ?: "PhotoZen"}"
            
            for (photo in selectedPhotos) {
                val photoUri = Uri.parse(photo.systemUri)
                
                val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                if (result.isSuccess) {
                    successCount++
                }
            }
            
            if (successCount > 0) {
                _uiState.update { it.copy(message = "已复制 $successCount 张照片") }
                clearSelection()
            }
        }
    }
    
    /**
     * Check if there are selected photos to delete.
     * Phase 4: 使用 selectionStateHolder
     */
    fun hasSelectedPhotos(): Boolean {
        return selectionStateHolder.hasSelection()
    }
    
    /**
     * Get URIs of selected photos for delete request.
     * Also saves the IDs for later Room database sync.
     * Phase 4: 使用 selectionStateHolder
     */
    fun getSelectedPhotoUris(): List<Uri> {
        val selectedIds = selectionStateHolder.getSelectedList().toSet()
        val selectedPhotos = _uiState.value.photos.filter { it.id in selectedIds }
        
        // Save the IDs for deletion from Room after user confirms
        _uiState.update { it.copy(pendingDeleteIds = selectedPhotos.map { photo -> photo.id }) }
        
        return selectedPhotos.map { Uri.parse(it.systemUri) }
    }
    
    /**
     * Handle delete confirmation result.
     * Syncs Room database by removing deleted photo records.
     */
    fun onDeleteConfirmed() {
        viewModelScope.launch {
            val pendingDeleteIds = _uiState.value.pendingDeleteIds
            
            if (pendingDeleteIds.isNotEmpty()) {
                // Delete from Room database to ensure immediate UI update
                photoDao.deleteByIds(pendingDeleteIds)
            }
            
            // Clear pending state
            _uiState.update { 
                it.copy(
                    pendingDeleteIds = emptyList(),
                    message = "照片已删除"
                ) 
            }
            clearSelection()
            // Note: Room Flow will automatically update UI since we deleted from database
        }
    }
    
    /**
     * Clear pending operation.
     */
    fun clearPendingOperation() {
        _uiState.update { it.copy(pendingOperation = null) }
    }
    
    /**
     * Clear error.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Clear message.
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    /**
     * Set custom filter session with specific photo IDs and prepare for navigation.
     * This allows "从此张开始筛选" feature to work by filtering only the specified photos.
     */
    fun setFilterSessionAndNavigate(photoIds: List<String>) {
        viewModelScope.launch {
            // Set custom filter session with specific photo IDs
            preferencesRepository.setSessionCustomFilter(
                CustomFilterSession(
                    photoIds = photoIds,
                    preciseMode = true
                )
            )
            
            // Set filter mode to CUSTOM
            preferencesRepository.setPhotoFilterMode(PhotoFilterMode.CUSTOM)
        }
    }
    
    // ==================== Phase 7.2: Status Filter ====================
    
    /**
     * Toggle a status in the filter.
     */
    fun toggleStatusFilter(status: PhotoStatus) {
        viewModelScope.launch {
            val currentFilter = _uiState.value.statusFilter
            val newFilter = if (status in currentFilter) {
                // Don't allow removing the last status
                if (currentFilter.size > 1) {
                    currentFilter - status
                } else {
                    currentFilter
                }
            } else {
                currentFilter + status
            }
            preferencesRepository.setAlbumPhotoStatusFilter(newFilter)
        }
    }
    
    /**
     * Select all statuses (reset filter).
     */
    fun selectAllStatuses() {
        viewModelScope.launch {
            preferencesRepository.setAlbumPhotoStatusFilter(PhotoStatus.entries.toSet())
        }
    }
    
    // ==================== REQ-047, REQ-048: 相册列表批量操作 ====================

    /**
     * 复制选中的照片 (REQ-047)
     */
    fun copySelectedPhotos() {
        viewModelScope.launch {
            val selectedIds = selectionStateHolder.getSelectedList().toSet()
            val selectedPhotos = _uiState.value.photos.filter { it.id in selectedIds }
            var successCount = 0

            for (photo in selectedPhotos) {
                val photoUri = Uri.parse(photo.systemUri)
                // 复制到同一相册
                val bucketId = _uiState.value.bucketId
                val targetAlbum = _uiState.value.albumBubbleList.find { it.bucketId == bucketId }
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${targetAlbum?.displayName ?: "PhotoZen"}"

                val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                if (result.isSuccess) {
                    // 将新照片插入 Room 数据库，保留原照片的筛选状态
                    result.getOrNull()?.let { newPhoto ->
                        val photoWithStatus = newPhoto.copy(status = photo.status)
                        photoDao.insert(photoWithStatus)
                    }
                    successCount++
                }
            }

            if (successCount > 0) {
                _uiState.update { it.copy(message = "已复制 $successCount 张照片") }
                clearSelection()
            }
        }
    }

    /**
     * 复制指定照片 (全屏预览中使用)
     * 复制后将新照片插入 Room 数据库，保留原照片的筛选状态
     */
    fun copyPhotos(photoIds: List<String>) {
        viewModelScope.launch {
            val photos = _uiState.value.photos.filter { it.id in photoIds }
            var successCount = 0

            for (photo in photos) {
                val photoUri = Uri.parse(photo.systemUri)
                val bucketId = _uiState.value.bucketId
                val targetAlbum = _uiState.value.albumBubbleList.find { it.bucketId == bucketId }
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${targetAlbum?.displayName ?: "PhotoZen"}"

                val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                if (result.isSuccess) {
                    // 将新照片插入 Room 数据库，保留原照片的筛选状态
                    result.getOrNull()?.let { newPhoto ->
                        val photoWithStatus = newPhoto.copy(status = photo.status)
                        photoDao.insert(photoWithStatus)
                    }
                    successCount++
                }
            }

            if (successCount > 0) {
                _uiState.update { it.copy(message = "已复制照片") }
            }
        }
    }

    /**
     * 批量修改选中照片的筛选状态 (REQ-048)
     */
    fun changeSelectedPhotosStatus(newStatus: PhotoStatus) {
        viewModelScope.launch {
            val selectedIds = selectionStateHolder.getSelectedList().toSet()

            // Update status in Room database
            for (photoId in selectedIds) {
                photoDao.updateStatus(photoId, newStatus)
            }

            val statusName = when (newStatus) {
                PhotoStatus.KEEP -> "保留"
                PhotoStatus.MAYBE -> "待定"
                PhotoStatus.TRASH -> "回收站"
                PhotoStatus.UNSORTED -> "未筛选"
            }
            _uiState.update { it.copy(message = "已将 ${selectedIds.size} 张照片标记为$statusName") }
            clearSelection()
        }
    }

    /**
     * Phase 4: 页面销毁时清理选择状态
     */
    override fun onCleared() {
        super.onCleared()
        selectionStateHolder.clear()
    }
}
