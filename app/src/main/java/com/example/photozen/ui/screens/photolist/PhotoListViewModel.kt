package com.example.photozen.ui.screens.photolist

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
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.MovePhotoResult
import com.example.photozen.domain.usecase.SortPhotoUseCase
import com.example.photozen.data.repository.GuideRepository
import com.example.photozen.ui.components.PhotoGridMode
import com.example.photozen.ui.state.UiEvent
import com.example.photozen.ui.state.PhotoSelectionStateHolder
import com.example.photozen.domain.usecase.PhotoBatchOperationUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 * Sort order for photos in list.
 */
enum class PhotoListSortOrder(val displayName: String) {
    DATE_DESC("时间倒序"),  // Newest first (default)
    DATE_ASC("时间正序"),   // Oldest first
    RANDOM("随机排序")      // Random shuffle
}

data class PhotoListUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val status: PhotoStatus = PhotoStatus.UNSORTED,
    val isLoading: Boolean = true,
    val message: String? = null,
    val defaultExternalApp: String? = null,
    val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC,
    val isSelectionMode: Boolean = false,
    val selectedPhotoIds: Set<String> = emptySet(),
    val gridColumns: Int = 2,
    val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,
    // Album mode support
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val showAlbumDialog: Boolean = false,
    // Phase 6: Keep list album filter
    val showPhotosInAlbum: Boolean = true,  // When false, only show photos not in "my albums"
    val notInAlbumCount: Int = 0,           // Count of photos not in any "my album"
    val myAlbumBucketIds: Set<String> = emptySet(),  // Bucket IDs of "my albums"
    // Album classify mode
    val isClassifyMode: Boolean = false,
    val classifyModePhotos: List<PhotoEntity> = emptyList(),
    val classifyModeIndex: Int = 0
) {
    val selectedCount: Int get() = selectedPhotoIds.size
    val allSelected: Boolean get() = photos.isNotEmpty() && selectedPhotoIds.size == photos.size
    // Batch management is available for KEEP, MAYBE, and TRASH
    val canBatchManage: Boolean get() = status in listOf(PhotoStatus.KEEP, PhotoStatus.MAYBE, PhotoStatus.TRASH)
    // Album operations for KEEP status
    val canBatchAlbum: Boolean get() = status == PhotoStatus.KEEP
    // Current photo in classify mode
    val currentClassifyPhoto: PhotoEntity? get() = classifyModePhotos.getOrNull(classifyModeIndex)
}

// Phase 4 清理：isSelectionMode 和 selectedPhotoIds 已迁移到 PhotoSelectionStateHolder
private data class InternalState(
    val isLoading: Boolean = true,
    val message: String? = null,
    val defaultExternalApp: String? = null,
    val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC,
    val gridColumns: Int = 2,
    val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,
    val showAlbumDialog: Boolean = false,
    // Phase 6: Keep list album filter
    val showPhotosInAlbum: Boolean = true,
    // Album classify mode
    val isClassifyMode: Boolean = false,
    val classifyModeIndex: Int = 0
)

private data class AlbumState(
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val myAlbumBucketIds: Set<String> = emptySet()
)

@HiltViewModel
class PhotoListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val photoDao: PhotoDao,
    private val albumBubbleDao: AlbumBubbleDao,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    val guideRepository: GuideRepository,
    // Phase 4: 共享状态和批量操作
    private val selectionStateHolder: PhotoSelectionStateHolder,
    private val batchOperationUseCase: PhotoBatchOperationUseCase
) : ViewModel() {
    
    // UI 事件流
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()
    
    private val statusName: String = savedStateHandle.get<String>("statusName") ?: "UNSORTED"
    private val status: PhotoStatus = try {
        PhotoStatus.valueOf(statusName)
    } catch (e: Exception) {
        PhotoStatus.UNSORTED
    }
    
    private val _internalState = MutableStateFlow(InternalState())
    
    // Random seed for consistent random sorting
    private var randomSeed = System.currentTimeMillis()
    
    // Album mode state
    private val _albumState = MutableStateFlow(AlbumState())
    
    // Phase 4: 使用 selectionStateHolder 的状态
    val uiState: StateFlow<PhotoListUiState> = combine(
        getPhotosUseCase.getPhotosByStatus(status),
        _internalState,
        _albumState,
        selectionStateHolder.selectedIds,
        selectionStateHolder.isSelectionMode
    ) { photos, internal, albumState, selectedIds, isSelectionMode ->
        // Apply sorting based on dateAdded (creation time)
        val sortedPhotos = applySortOrder(photos, internal.sortOrder)
        
        // Phase 6: Calculate photos not in "my albums" (only for KEEP status)
        val myAlbumBucketIds = albumState.myAlbumBucketIds
        val photosNotInAlbum = if (status == PhotoStatus.KEEP && myAlbumBucketIds.isNotEmpty()) {
            sortedPhotos.filter { photo -> photo.bucketId == null || photo.bucketId !in myAlbumBucketIds }
        } else {
            emptyList()
        }
        val notInAlbumCount = photosNotInAlbum.size
        
        // Apply album filter for KEEP status (Phase 6.2)
        val filteredPhotos = if (status == PhotoStatus.KEEP && !internal.showPhotosInAlbum && myAlbumBucketIds.isNotEmpty()) {
            photosNotInAlbum
        } else {
            sortedPhotos
        }
        
        // Filter out selected photos that no longer exist (Phase 4: 使用 StateHolder 的 selectedIds)
        val validSelectedIds = selectedIds.filter { id ->
            filteredPhotos.any { it.id == id }
        }.toSet()
        
        PhotoListUiState(
            photos = filteredPhotos,
            status = status,
            isLoading = internal.isLoading && photos.isEmpty(),
            message = internal.message,
            defaultExternalApp = internal.defaultExternalApp,
            sortOrder = internal.sortOrder,
            isSelectionMode = isSelectionMode || validSelectedIds.isNotEmpty(), // Phase 4: 使用 StateHolder
            selectedPhotoIds = validSelectedIds,
            gridColumns = internal.gridColumns,
            gridMode = internal.gridMode,
            albumBubbleList = albumState.albumBubbleList,
            albumAddAction = albumState.albumAddAction,
            showAlbumDialog = internal.showAlbumDialog,
            // Phase 6
            showPhotosInAlbum = internal.showPhotosInAlbum,
            notInAlbumCount = notInAlbumCount,
            myAlbumBucketIds = myAlbumBucketIds,
            isClassifyMode = internal.isClassifyMode,
            classifyModePhotos = photosNotInAlbum,
            classifyModeIndex = internal.classifyModeIndex
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PhotoListUiState(status = status)
    )
    
    init {
        // Phase 4: 进入页面时清空选择状态，避免与其他页面冲突
        selectionStateHolder.clear()
        
        viewModelScope.launch {
            _internalState.update { it.copy(isLoading = false) }
        }
        viewModelScope.launch {
            preferencesRepository.getDefaultExternalApp().collect { app ->
                _internalState.update { it.copy(defaultExternalApp = app) }
            }
        }
        // Load grid columns preference
        viewModelScope.launch {
            val gridScreen = when (status) {
                PhotoStatus.KEEP -> PreferencesRepository.GridScreen.KEEP
                PhotoStatus.MAYBE -> PreferencesRepository.GridScreen.MAYBE
                PhotoStatus.TRASH -> PreferencesRepository.GridScreen.TRASH
                PhotoStatus.UNSORTED -> PreferencesRepository.GridScreen.KEEP // Default
            }
            preferencesRepository.getGridColumns(gridScreen).collect { columns ->
                _internalState.update { it.copy(gridColumns = columns) }
            }
        }
        // Load album settings for KEEP status
        if (status == PhotoStatus.KEEP) {
            viewModelScope.launch {
                preferencesRepository.getAlbumAddAction().collect { action ->
                    _albumState.update { it.copy(albumAddAction = action) }
                }
            }
            viewModelScope.launch {
                albumBubbleDao.getAll().collect { albums ->
                    _albumState.update { 
                        it.copy(
                            albumBubbleList = albums,
                            myAlbumBucketIds = albums.map { album -> album.bucketId }.toSet()
                        )
                    }
                }
            }
            // Phase 6.2: Load show photos in album preference
            viewModelScope.launch {
                preferencesRepository.getShowPhotosInAlbumKeepList().collect { show ->
                    _internalState.update { it.copy(showPhotosInAlbum = show) }
                }
            }
        }
    }
    
    /**
     * Apply sort order to photos list.
     * Uses dateAdded (creation time) for sorting, NOT dateModified.
     */
    private fun applySortOrder(photos: List<PhotoEntity>, sortOrder: PhotoListSortOrder): List<PhotoEntity> {
        return when (sortOrder) {
            // Sort by dateAdded (creation time in seconds), convert to comparable value
            PhotoListSortOrder.DATE_DESC -> photos.sortedByDescending { it.dateAdded }
            PhotoListSortOrder.DATE_ASC -> photos.sortedBy { it.dateAdded }
            PhotoListSortOrder.RANDOM -> photos.shuffled(kotlin.random.Random(randomSeed))
        }
    }
    
    /**
     * Set sort order.
     */
    fun setSortOrder(order: PhotoListSortOrder) {
        if (order == PhotoListSortOrder.RANDOM) {
            randomSeed = System.currentTimeMillis()
        }
        _internalState.update { it.copy(sortOrder = order) }
    }
    
    /**
     * Cycle through sort orders.
     */
    fun cycleSortOrder() {
        val nextOrder = when (_internalState.value.sortOrder) {
            PhotoListSortOrder.DATE_DESC -> PhotoListSortOrder.DATE_ASC
            PhotoListSortOrder.DATE_ASC -> PhotoListSortOrder.RANDOM
            PhotoListSortOrder.RANDOM -> PhotoListSortOrder.DATE_DESC
        }
        setSortOrder(nextOrder)
    }
    
    /**
     * Set default external app for opening photos.
     */
    suspend fun setDefaultExternalApp(packageName: String?) {
        preferencesRepository.setDefaultExternalApp(packageName)
    }
    
    fun moveToKeep(photoId: String) {
        viewModelScope.launch {
            try {
                sortPhotoUseCase.keepPhoto(photoId)
                _internalState.update { it.copy(message = "已移至保留") }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
    fun moveToTrash(photoId: String) {
        viewModelScope.launch {
            try {
                sortPhotoUseCase.trashPhoto(photoId)
                _internalState.update { it.copy(message = "已移至回收站") }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
    fun moveToMaybe(photoId: String) {
        viewModelScope.launch {
            try {
                sortPhotoUseCase.maybePhoto(photoId)
                _internalState.update { it.copy(message = "已标记为待定") }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
    fun resetToUnsorted(photoId: String) {
        viewModelScope.launch {
            try {
                sortPhotoUseCase.resetPhoto(photoId)
                _internalState.update { it.copy(message = "已恢复未整理") }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
    fun clearMessage() {
        _internalState.update { it.copy(message = null) }
    }
    
    // ==================== Selection Mode (Phase 4: 使用 StateHolder) ====================
    
    /**
     * Toggle selection mode on/off.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun toggleSelectionMode() {
        if (selectionStateHolder.hasSelection()) {
            selectionStateHolder.clear()
        }
    }
    
    /**
     * Exit selection mode and clear selection.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun exitSelectionMode() {
        selectionStateHolder.clear()
    }
    
    /**
     * Toggle selection of a single photo.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun togglePhotoSelection(photoId: String) {
        selectionStateHolder.toggle(photoId)
    }
    
    /**
     * Update selection with a new set of IDs (for drag-select).
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun updateSelection(newSelection: Set<String>) {
        selectionStateHolder.setSelection(newSelection)
    }
    
    /**
     * Select all photos.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun selectAll() {
        val allIds = uiState.value.photos.map { it.id }
        selectionStateHolder.selectAll(allIds)
    }
    
    /**
     * Deselect all photos.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun deselectAll() {
        selectionStateHolder.clear()
    }
    
    /**
     * 批量更新选中照片状态（Phase 4: 使用 BatchUseCase）
     * 
     * 新版本使用 PhotoBatchOperationUseCase 统一处理：
     * - 状态批量更新
     * - 撤销支持（通过 UndoManager）
     * - Snackbar 反馈（通过 SnackbarManager）
     * - 统计记录
     */
    private fun batchUpdateStatus(newStatus: PhotoStatus) {
        val selectedIds = selectionStateHolder.getSelectedList()
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            // Phase 4: 使用 BatchUseCase 执行批量操作
            // UseCase 内部已处理撤销、Snackbar、统计
            batchOperationUseCase.batchUpdateStatus(selectedIds, newStatus)
            
            // 清空选择
            selectionStateHolder.clear()
        }
    }
    
    /**
     * Move selected photos to Keep.
     * Phase 4: 使用 BatchUseCase
     */
    fun moveSelectedToKeep() = batchUpdateStatus(PhotoStatus.KEEP)
    
    /**
     * Move selected photos to Trash.
     * Phase 4: 使用 BatchUseCase
     */
    fun moveSelectedToTrash() = batchUpdateStatus(PhotoStatus.TRASH)
    
    /**
     * Move selected photos to Maybe.
     * Phase 4: 使用 BatchUseCase
     */
    fun moveSelectedToMaybe() = batchUpdateStatus(PhotoStatus.MAYBE)
    
    /**
     * Reset selected photos to Unsorted.
     * Phase 4: 使用 BatchUseCase
     */
    fun resetSelectedToUnsorted() = batchUpdateStatus(PhotoStatus.UNSORTED)
    
    /**
     * Duplicate a photo, preserving all EXIF metadata and timestamps.
     * The copy will have the same status as the original photo.
     */
    fun duplicatePhoto(photoId: String) {
        viewModelScope.launch {
            try {
                // Get the original photo
                val originalPhoto = photoDao.getById(photoId)
                if (originalPhoto == null) {
                    _internalState.update { it.copy(message = "找不到照片") }
                    return@launch
                }
                
                // Duplicate the photo in MediaStore
                val sourceUri = Uri.parse(originalPhoto.systemUri)
                val newPhoto = mediaStoreDataSource.duplicatePhoto(sourceUri)
                
                if (newPhoto != null) {
                    // Save the new photo to our database with the same status
                    val photoWithStatus = newPhoto.copy(status = originalPhoto.status)
                    photoDao.insert(photoWithStatus)
                    _internalState.update { it.copy(message = "照片已复制") }
                } else {
                    _internalState.update { it.copy(message = "复制照片失败") }
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "复制照片失败: ${e.message}") }
            }
        }
    }
    
    // ==================== Album Operations ====================
    
    /**
     * Show album selection dialog.
     */
    fun showAlbumDialog() {
        _internalState.update { it.copy(showAlbumDialog = true) }
    }
    
    /**
     * Hide album selection dialog.
     */
    fun hideAlbumDialog() {
        _internalState.update { it.copy(showAlbumDialog = false) }
    }
    
    /**
     * Refresh albums list. Since albums are loaded via Flow from DAO,
     * this is mostly a no-op as the Flow will automatically provide updates.
     * Kept for explicit refresh triggers from UI.
     */
    fun refreshAlbums() {
        // Albums are automatically refreshed via Flow from albumBubbleDao.getAll()
        // This method exists for explicit triggers but the data should already be current
    }
    
    /**
     * Add selected photos to album using default action (copy or move).
     * Phase 4: 使用 selectionStateHolder
     */
    fun addSelectedToAlbum(bucketId: String) {
        val selectedIds = selectionStateHolder.getSelectedList()
        if (selectedIds.isEmpty()) return
        
        val action = _albumState.value.albumAddAction
        if (action == AlbumAddAction.MOVE) {
            moveSelectedToAlbum(bucketId)
        } else {
            copySelectedToAlbum(bucketId)
        }
    }
    
    /**
     * Move selected photos to an album.
     * Phase 4: 使用 selectionStateHolder
     */
    fun moveSelectedToAlbum(bucketId: String) {
        val selectedIds = selectionStateHolder.getSelectedList()
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                val album = _albumState.value.albumBubbleList.find { it.bucketId == bucketId }
                // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
                var successCount = 0
                
                for (photoId in selectedIds) {
                    val photo = uiState.value.photos.find { it.id == photoId } ?: continue
                    val photoUri = Uri.parse(photo.systemUri)
                    
                    when (val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)) {
                        is MovePhotoResult.Success -> successCount++
                        is MovePhotoResult.NeedsConfirmation -> {
                            // Handle one at a time for confirmation
                        }
                        is MovePhotoResult.Error -> {
                            // Continue with other photos
                        }
                    }
                }
                
                _internalState.update { 
                    it.copy(
                        showAlbumDialog = false,
                        message = "已移动 $successCount 张照片到「${album?.displayName}」"
                    )
                }
                selectionStateHolder.clear()
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "移动失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Copy selected photos to an album.
     * Phase 4: 使用 selectionStateHolder
     */
    fun copySelectedToAlbum(bucketId: String) {
        val selectedIds = selectionStateHolder.getSelectedList()
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                val album = _albumState.value.albumBubbleList.find { it.bucketId == bucketId }
                // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
                var successCount = 0
                
                for (photoId in selectedIds) {
                    val photo = uiState.value.photos.find { it.id == photoId } ?: continue
                    val photoUri = Uri.parse(photo.systemUri)
                    
                    val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                    if (result.isSuccess) {
                        successCount++
                    }
                }
                
                _internalState.update { 
                    it.copy(
                        showAlbumDialog = false,
                        message = "已复制 $successCount 张照片到「${album?.displayName}」"
                    )
                }
                selectionStateHolder.clear()
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "复制失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Add a single photo to album (for long-press menu).
     */
    fun addPhotoToAlbum(photoId: String, bucketId: String, copy: Boolean = false) {
        viewModelScope.launch {
            try {
                val photo = uiState.value.photos.find { it.id == photoId } ?: return@launch
                val album = _albumState.value.albumBubbleList.find { it.bucketId == bucketId }
                // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
                val photoUri = Uri.parse(photo.systemUri)
                
                if (copy) {
                    val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                    if (result.isSuccess) {
                        _internalState.update { it.copy(message = "已复制到「${album?.displayName}」") }
                    } else {
                        _internalState.update { it.copy(message = "复制失败") }
                    }
                } else {
                    when (val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)) {
                        is MovePhotoResult.Success -> {
                            _internalState.update { it.copy(message = "已移动到「${album?.displayName}」") }
                        }
                        is MovePhotoResult.NeedsConfirmation -> {
                            _internalState.update { it.copy(message = "需要权限确认才能移动") }
                        }
                        is MovePhotoResult.Error -> {
                            _internalState.update { it.copy(message = "移动失败: ${result.message}") }
                        }
                    }
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败: ${e.message}") }
            }
        }
    }
    
    // ==================== Grid Columns ====================
    
    /**
     * Cycle grid columns: 2 -> 3 -> 1 -> 2
     */
    fun cycleGridColumns() {
        viewModelScope.launch {
            val gridScreen = when (status) {
                PhotoStatus.KEEP -> PreferencesRepository.GridScreen.KEEP
                PhotoStatus.MAYBE -> PreferencesRepository.GridScreen.MAYBE
                PhotoStatus.TRASH -> PreferencesRepository.GridScreen.TRASH
                PhotoStatus.UNSORTED -> PreferencesRepository.GridScreen.KEEP
            }
            val newColumns = preferencesRepository.cycleGridColumns(gridScreen)
            _internalState.update { it.copy(gridColumns = newColumns) }
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

    // ==================== Phase 6: Keep List Album Features ====================
    
    /**
     * Toggle whether to show photos that are already in albums.
     */
    fun toggleShowPhotosInAlbum() {
        viewModelScope.launch {
            val newValue = !_internalState.value.showPhotosInAlbum
            preferencesRepository.setShowPhotosInAlbumKeepList(newValue)
            _internalState.update { it.copy(showPhotosInAlbum = newValue) }
        }
    }
    
    /**
     * Enter album classify mode - for quickly adding photos to albums.
     * Only available for KEEP status photos not in any "my album".
     */
    fun enterClassifyMode() {
        if (status != PhotoStatus.KEEP) return
        _internalState.update { 
            it.copy(
                isClassifyMode = true,
                classifyModeIndex = 0
            )
        }
    }
    
    /**
     * Exit album classify mode.
     */
    fun exitClassifyMode() {
        _internalState.update { 
            it.copy(
                isClassifyMode = false,
                classifyModeIndex = 0
            )
        }
    }
    
    /**
     * Add current photo in classify mode to an album, then advance to next.
     */
    fun classifyPhotoToAlbum(bucketId: String) {
        val currentPhoto = uiState.value.currentClassifyPhoto ?: return
        
        viewModelScope.launch {
            try {
                val album = _albumState.value.albumBubbleList.find { it.bucketId == bucketId }
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
                val photoUri = Uri.parse(currentPhoto.systemUri)
                
                // Copy photo to album (don't move, keep original location)
                val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                if (result.isSuccess) {
                    // Update photo's bucketId in database
                    photoDao.updateBucketId(currentPhoto.id, bucketId)
                    _internalState.update { it.copy(message = "已添加到「${album?.displayName}」") }
                }
                
                // Advance to next photo
                advanceClassifyMode()
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Skip current photo in classify mode (don't add to any album).
     */
    fun skipClassifyPhoto() {
        advanceClassifyMode()
    }
    
    /**
     * Advance to next photo in classify mode.
     */
    private fun advanceClassifyMode() {
        val currentIndex = _internalState.value.classifyModeIndex
        val totalCount = uiState.value.classifyModePhotos.size
        
        if (currentIndex + 1 >= totalCount) {
            // All photos processed, exit classify mode
            _internalState.update { 
                it.copy(
                    isClassifyMode = false,
                    classifyModeIndex = 0,
                    message = "全部照片已分类完成"
                )
            }
        } else {
            // Move to next photo
            _internalState.update { it.copy(classifyModeIndex = currentIndex + 1) }
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
