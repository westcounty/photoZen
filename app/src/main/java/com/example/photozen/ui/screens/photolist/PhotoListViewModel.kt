package com.example.photozen.ui.screens.photolist

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.PhotoTagCrossRef
import com.example.photozen.data.local.entity.TagEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.AlbumAddAction
import com.example.photozen.data.repository.PhotoClassificationMode
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.MovePhotoResult
import com.example.photozen.domain.usecase.SortPhotoUseCase
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
    val untaggedCount: Int = 0,
    val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC,
    val isSelectionMode: Boolean = false,
    val selectedPhotoIds: Set<String> = emptySet(),
    val availableTags: List<TagInfo> = emptyList(),
    val showTagDialog: Boolean = false,
    val gridColumns: Int = 2,
    // Album mode support
    val classificationMode: PhotoClassificationMode = PhotoClassificationMode.TAG,
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val showAlbumDialog: Boolean = false
) {
    val selectedCount: Int get() = selectedPhotoIds.size
    val allSelected: Boolean get() = photos.isNotEmpty() && selectedPhotoIds.size == photos.size
    // Batch management is available for KEEP, MAYBE, and TRASH
    val canBatchManage: Boolean get() = status in listOf(PhotoStatus.KEEP, PhotoStatus.MAYBE, PhotoStatus.TRASH)
    // Tag operations only for KEEP status
    val canBatchTag: Boolean get() = status == PhotoStatus.KEEP
    // Album operations for KEEP status in album mode
    val canBatchAlbum: Boolean get() = status == PhotoStatus.KEEP && classificationMode == PhotoClassificationMode.ALBUM
}

/**
 * Simplified tag info for display.
 */
data class TagInfo(
    val id: String,
    val name: String,
    val color: Int,
    val photoCount: Int
)

private data class InternalState(
    val isLoading: Boolean = true,
    val message: String? = null,
    val defaultExternalApp: String? = null,
    val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC,
    val isSelectionMode: Boolean = false,
    val selectedPhotoIds: Set<String> = emptySet(),
    val showTagDialog: Boolean = false,
    val gridColumns: Int = 2,
    val showAlbumDialog: Boolean = false
)

private data class AlbumState(
    val classificationMode: PhotoClassificationMode = PhotoClassificationMode.TAG,
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE
)

@HiltViewModel
class PhotoListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val photoDao: PhotoDao,
    private val tagDao: TagDao,
    private val albumBubbleDao: AlbumBubbleDao,
    private val albumOperationsUseCase: AlbumOperationsUseCase
) : ViewModel() {
    
    private val statusName: String = savedStateHandle.get<String>("statusName") ?: "UNSORTED"
    private val status: PhotoStatus = try {
        PhotoStatus.valueOf(statusName)
    } catch (e: Exception) {
        PhotoStatus.UNSORTED
    }
    
    private val _internalState = MutableStateFlow(InternalState())
    
    // Random seed for consistent random sorting
    private var randomSeed = System.currentTimeMillis()
    
    // Track untagged count separately to avoid blocking UI
    private val _untaggedCount = MutableStateFlow(0)
    
    // Available tags for batch tagging
    private val _availableTags = MutableStateFlow<List<TagInfo>>(emptyList())
    
    // Album mode state
    private val _albumState = MutableStateFlow(AlbumState())
    
    val uiState: StateFlow<PhotoListUiState> = combine(
        getPhotosUseCase.getPhotosByStatus(status),
        _internalState,
        _untaggedCount,
        combine(_availableTags, _albumState) { tags, album -> Pair(tags, album) }
    ) { photos, internal, untaggedCount, (tags, albumState) ->
        // Apply sorting based on dateAdded (creation time)
        val sortedPhotos = applySortOrder(photos, internal.sortOrder)
        // Filter out selected photos that no longer exist
        val validSelectedIds = internal.selectedPhotoIds.filter { id ->
            sortedPhotos.any { it.id == id }
        }.toSet()
        PhotoListUiState(
            photos = sortedPhotos,
            status = status,
            isLoading = internal.isLoading && photos.isEmpty(),
            message = internal.message,
            defaultExternalApp = internal.defaultExternalApp,
            untaggedCount = untaggedCount,
            sortOrder = internal.sortOrder,
            isSelectionMode = internal.isSelectionMode,
            selectedPhotoIds = validSelectedIds,
            availableTags = tags,
            showTagDialog = internal.showTagDialog,
            gridColumns = internal.gridColumns,
            classificationMode = albumState.classificationMode,
            albumBubbleList = albumState.albumBubbleList,
            albumAddAction = albumState.albumAddAction,
            showAlbumDialog = internal.showAlbumDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PhotoListUiState(status = status)
    )
    
    init {
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
        // Count untagged photos for KEEP status - reactive to photo list changes
        if (status == PhotoStatus.KEEP) {
            viewModelScope.launch {
                getPhotosUseCase.getPhotosByStatus(PhotoStatus.KEEP).collect { photos ->
                    // Calculate untagged count in background
                    var count = 0
                    for (photo in photos) {
                        if (!tagDao.photoHasAnyTag(photo.id)) {
                            count++
                        }
                    }
                    _untaggedCount.value = count
                }
            }
            // Load available tags for KEEP status (batch tagging)
            viewModelScope.launch {
                tagDao.getTagsWithPhotoCount().collect { tagsWithCount ->
                    _availableTags.value = tagsWithCount.map { tagWithCount ->
                        TagInfo(
                            id = tagWithCount.id,
                            name = tagWithCount.name,
                            color = tagWithCount.color,
                            photoCount = tagWithCount.photoCount
                        )
                    }
                }
            }
            // Load album settings for KEEP status
            viewModelScope.launch {
                preferencesRepository.getPhotoClassificationMode().collect { mode ->
                    _albumState.update { it.copy(classificationMode = mode) }
                }
            }
            viewModelScope.launch {
                preferencesRepository.getAlbumAddAction().collect { action ->
                    _albumState.update { it.copy(albumAddAction = action) }
                }
            }
            viewModelScope.launch {
                albumBubbleDao.getAll().collect { albums ->
                    _albumState.update { it.copy(albumBubbleList = albums) }
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
    
    // ==================== Selection Mode ====================
    
    /**
     * Toggle selection mode on/off.
     */
    fun toggleSelectionMode() {
        _internalState.update { 
            it.copy(
                isSelectionMode = !it.isSelectionMode,
                selectedPhotoIds = if (it.isSelectionMode) emptySet() else it.selectedPhotoIds
            )
        }
    }
    
    /**
     * Exit selection mode and clear selection.
     */
    fun exitSelectionMode() {
        _internalState.update { 
            it.copy(isSelectionMode = false, selectedPhotoIds = emptySet())
        }
    }
    
    /**
     * Toggle selection of a single photo.
     */
    fun togglePhotoSelection(photoId: String) {
        _internalState.update { state ->
            val newSelection = if (photoId in state.selectedPhotoIds) {
                state.selectedPhotoIds - photoId
            } else {
                state.selectedPhotoIds + photoId
            }
            state.copy(selectedPhotoIds = newSelection)
        }
    }
    
    /**
     * Select all photos.
     */
    fun selectAll() {
        val allIds = uiState.value.photos.map { it.id }.toSet()
        _internalState.update { it.copy(selectedPhotoIds = allIds) }
    }
    
    /**
     * Deselect all photos.
     */
    fun deselectAll() {
        _internalState.update { it.copy(selectedPhotoIds = emptySet()) }
    }
    
    /**
     * Move selected photos to Keep.
     */
    fun moveSelectedToKeep() {
        val selectedIds = _internalState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                selectedIds.forEach { photoId ->
                    sortPhotoUseCase.keepPhoto(photoId)
                }
                _internalState.update { 
                    it.copy(
                        message = "已将 ${selectedIds.size} 张照片移至保留",
                        selectedPhotoIds = emptySet(),
                        isSelectionMode = false
                    )
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
    /**
     * Move selected photos to Trash.
     */
    fun moveSelectedToTrash() {
        val selectedIds = _internalState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                selectedIds.forEach { photoId ->
                    sortPhotoUseCase.trashPhoto(photoId)
                }
                _internalState.update { 
                    it.copy(
                        message = "已将 ${selectedIds.size} 张照片移至回收站",
                        selectedPhotoIds = emptySet(),
                        isSelectionMode = false
                    )
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
    /**
     * Move selected photos to Maybe.
     */
    fun moveSelectedToMaybe() {
        val selectedIds = _internalState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                selectedIds.forEach { photoId ->
                    sortPhotoUseCase.maybePhoto(photoId)
                }
                _internalState.update { 
                    it.copy(
                        message = "已将 ${selectedIds.size} 张照片标记为待定",
                        selectedPhotoIds = emptySet(),
                        isSelectionMode = false
                    )
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
    /**
     * Reset selected photos to Unsorted.
     */
    fun resetSelectedToUnsorted() {
        val selectedIds = _internalState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                selectedIds.forEach { photoId ->
                    sortPhotoUseCase.resetPhoto(photoId)
                }
                _internalState.update { 
                    it.copy(
                        message = "已将 ${selectedIds.size} 张照片恢复为未整理",
                        selectedPhotoIds = emptySet(),
                        isSelectionMode = false
                    )
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
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
    
    // ==================== Batch Tagging ====================
    
    /**
     * Show tag selection dialog.
     */
    fun showTagDialog() {
        _internalState.update { it.copy(showTagDialog = true) }
    }
    
    /**
     * Hide tag selection dialog.
     */
    fun hideTagDialog() {
        _internalState.update { it.copy(showTagDialog = false) }
    }
    
    /**
     * Apply a tag to all selected photos.
     */
    fun applyTagToSelected(tagId: String) {
        val selectedIds = _internalState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                var addedCount = 0
                selectedIds.forEach { photoId ->
                    // Check if already tagged
                    if (!tagDao.photoHasTag(photoId, tagId)) {
                        tagDao.addTagToPhoto(PhotoTagCrossRef(photoId, tagId))
                        addedCount++
                    }
                }
                val tagName = _availableTags.value.find { it.id == tagId }?.name ?: "标签"
                _internalState.update { 
                    it.copy(
                        showTagDialog = false,
                        message = "已为 $addedCount 张照片添加标签「$tagName」",
                        selectedPhotoIds = emptySet(),
                        isSelectionMode = false
                    )
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "设置标签失败") }
            }
        }
    }
    
    /**
     * Create a new tag and apply to selected photos.
     */
    fun createTagAndApplyToSelected(tagName: String, tagColor: Int) {
        val selectedIds = _internalState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                // Check if tag name already exists
                if (tagDao.tagNameExists(tagName)) {
                    _internalState.update { it.copy(message = "标签「$tagName」已存在") }
                    return@launch
                }
                
                // Create the new tag
                val newTag = TagEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = tagName,
                    color = tagColor
                )
                tagDao.insert(newTag)
                
                // Apply to selected photos
                selectedIds.forEach { photoId ->
                    tagDao.addTagToPhoto(PhotoTagCrossRef(photoId, newTag.id))
                }
                
                _internalState.update { 
                    it.copy(
                        showTagDialog = false,
                        message = "已创建标签「$tagName」并应用到 ${selectedIds.size} 张照片",
                        selectedPhotoIds = emptySet(),
                        isSelectionMode = false
                    )
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "创建标签失败: ${e.message}") }
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
     * Add selected photos to album using default action (copy or move).
     */
    fun addSelectedToAlbum(bucketId: String) {
        val selectedIds = _internalState.value.selectedPhotoIds
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
     */
    fun moveSelectedToAlbum(bucketId: String) {
        val selectedIds = _internalState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                val album = _albumState.value.albumBubbleList.find { it.bucketId == bucketId }
                val targetPath = "Pictures/${album?.displayName ?: "PhotoZen"}"
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
                        message = "已移动 $successCount 张照片到「${album?.displayName}」",
                        selectedPhotoIds = emptySet(),
                        isSelectionMode = false
                    )
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "移动失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Copy selected photos to an album.
     */
    fun copySelectedToAlbum(bucketId: String) {
        val selectedIds = _internalState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                val album = _albumState.value.albumBubbleList.find { it.bucketId == bucketId }
                val targetPath = "Pictures/${album?.displayName ?: "PhotoZen"}"
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
                        message = "已复制 $successCount 张照片到「${album?.displayName}」",
                        selectedPhotoIds = emptySet(),
                        isSelectionMode = false
                    )
                }
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
                val targetPath = "Pictures/${album?.displayName ?: "PhotoZen"}"
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
}
