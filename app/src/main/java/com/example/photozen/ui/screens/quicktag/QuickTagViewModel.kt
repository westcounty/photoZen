package com.example.photozen.ui.screens.quicktag

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.TagEntity
import com.example.photozen.data.repository.AlbumAddAction
import com.example.photozen.data.repository.PhotoClassificationMode
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.MovePhotoResult
import com.example.photozen.domain.usecase.TagAlbumSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sort order for photos in Quick Tag.
 */
enum class QuickTagSortOrder(val displayName: String) {
    DATE_DESC("时间倒序"),  // Newest first (default)
    DATE_ASC("时间正序"),   // Oldest first
    RANDOM("随机排序")      // Random shuffle
}

/**
 * UI State for Quick Tag screen.
 */
data class QuickTagUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val currentIndex: Int = 0,
    val tags: List<TagEntity> = emptyList(),
    val currentPhotoTags: List<TagEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,
    val taggedCount: Int = 0,
    val skippedCount: Int = 0,
    val error: String? = null,
    val sortOrder: QuickTagSortOrder = QuickTagSortOrder.DATE_DESC,
    // Album mode support
    val classificationMode: PhotoClassificationMode = PhotoClassificationMode.TAG,
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val message: String? = null
) {
    val currentPhoto: PhotoEntity?
        get() = photos.getOrNull(currentIndex)
    
    val nextPhoto: PhotoEntity?
        get() = photos.getOrNull(currentIndex + 1)
    
    val progress: Float
        get() = if (photos.isNotEmpty()) currentIndex.toFloat() / photos.size else 0f
    
    val remainingCount: Int
        get() = (photos.size - currentIndex).coerceAtLeast(0)
}

/**
 * ViewModel for Quick Tag screen.
 * Manages the flow-style tagging experience for kept photos.
 * Supports both tag mode and album mode based on classification settings.
 */
@HiltViewModel
class QuickTagViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val tagDao: TagDao,
    private val albumBubbleDao: AlbumBubbleDao,
    private val preferencesRepository: PreferencesRepository,
    private val tagAlbumSyncUseCase: TagAlbumSyncUseCase,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val mediaStoreDataSource: MediaStoreDataSource
) : ViewModel() {
    
    private val _currentIndex = MutableStateFlow(0)
    private val _taggedCount = MutableStateFlow(0)
    private val _skippedCount = MutableStateFlow(0)
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)
    private val _currentPhotoTags = MutableStateFlow<List<TagEntity>>(emptyList())
    private val _sortOrder = MutableStateFlow(QuickTagSortOrder.DATE_DESC)
    private val _classificationMode = MutableStateFlow(PhotoClassificationMode.TAG)
    private val _albumBubbleList = MutableStateFlow<List<AlbumBubbleEntity>>(emptyList())
    private val _albumAddAction = MutableStateFlow(AlbumAddAction.MOVE)
    
    // Random seed for consistent random sorting until changed
    private var randomSeed = System.currentTimeMillis()
    
    // Cache photos list to avoid reloading
    private var cachedPhotos: List<PhotoEntity> = emptyList()
    
    // Combined flow for misc state to reduce combine parameters
    private val miscStateFlow = combine(
        _skippedCount, _isLoading, _error, _currentPhotoTags, _sortOrder
    ) { s, l, e, p, o ->
        MiscState(s, l, e, p, o)
    }
    
    // Combined flow for album state
    private val albumStateFlow = combine(
        _classificationMode, _albumBubbleList, _albumAddAction, _message
    ) { mode, albums, action, msg ->
        AlbumState(mode, albums, action, msg)
    }
    
    val uiState: StateFlow<QuickTagUiState> = combine(
        photoRepository.getKeepPhotos(),
        tagDao.getAllTags(),
        _currentIndex,
        _taggedCount,
        combine(miscStateFlow, albumStateFlow) { misc, album -> Pair(misc, album) }
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val photos = values[0] as List<PhotoEntity>
        val tags = values[1] as List<TagEntity>
        val currentIndex = values[2] as Int
        val taggedCount = values[3] as Int
        val (misc, album) = values[4] as Pair<MiscState, AlbumState>
        
        // Update cached photos with sorting applied
        if (photos.isNotEmpty()) {
            cachedPhotos = applySortOrder(photos, misc.sortOrder)
        }
        
        val displayPhotos = if (cachedPhotos.isNotEmpty()) cachedPhotos else applySortOrder(photos, misc.sortOrder)
        
        QuickTagUiState(
            photos = displayPhotos,
            currentIndex = currentIndex,
            tags = tags,
            currentPhotoTags = misc.currentPhotoTags,
            isLoading = misc.isLoading && displayPhotos.isEmpty(),
            isComplete = currentIndex >= displayPhotos.size && displayPhotos.isNotEmpty(),
            taggedCount = taggedCount,
            skippedCount = misc.skippedCount,
            error = misc.error,
            sortOrder = misc.sortOrder,
            classificationMode = album.classificationMode,
            albumBubbleList = album.albumBubbleList,
            albumAddAction = album.albumAddAction,
            message = album.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuickTagUiState()
    )
    
    private data class MiscState(
        val skippedCount: Int,
        val isLoading: Boolean,
        val error: String?,
        val currentPhotoTags: List<TagEntity>,
        val sortOrder: QuickTagSortOrder
    )
    
    private data class AlbumState(
        val classificationMode: PhotoClassificationMode,
        val albumBubbleList: List<AlbumBubbleEntity>,
        val albumAddAction: AlbumAddAction,
        val message: String?
    )
    
    /**
     * Apply sort order to photos list.
     * Uses dateAdded (creation time) for sorting, NOT dateModified or dateTaken.
     */
    private fun applySortOrder(photos: List<PhotoEntity>, sortOrder: QuickTagSortOrder): List<PhotoEntity> {
        return when (sortOrder) {
            // Sort by dateAdded (creation time in seconds)
            QuickTagSortOrder.DATE_DESC -> photos.sortedByDescending { it.dateAdded }
            QuickTagSortOrder.DATE_ASC -> photos.sortedBy { it.dateAdded }
            QuickTagSortOrder.RANDOM -> photos.shuffled(kotlin.random.Random(randomSeed))
        }
    }
    
    /**
     * Set sort order for photos.
     * Takes effect immediately. Resets the current index and re-sorts cached photos.
     */
    fun setSortOrder(order: QuickTagSortOrder) {
        // If switching to random, generate new seed
        if (order == QuickTagSortOrder.RANDOM) {
            randomSeed = System.currentTimeMillis()
        }
        _sortOrder.value = order
        // Reset position when sort order changes
        _currentIndex.value = 0
        cachedPhotos = emptyList() // Clear cache to trigger re-sort
    }
    
    /**
     * Cycle through sort orders: DATE_DESC -> DATE_ASC -> RANDOM -> DATE_DESC
     */
    fun cycleSortOrder() {
        val nextOrder = when (_sortOrder.value) {
            QuickTagSortOrder.DATE_DESC -> QuickTagSortOrder.DATE_ASC
            QuickTagSortOrder.DATE_ASC -> QuickTagSortOrder.RANDOM
            QuickTagSortOrder.RANDOM -> QuickTagSortOrder.DATE_DESC
        }
        setSortOrder(nextOrder)
    }
    
    init {
        viewModelScope.launch {
            _isLoading.value = false
            // Skip photos that already have tags on entry (and when list loads)
            photoRepository.getKeepPhotos().collect { photos ->
                if (photos.isNotEmpty() && cachedPhotos.isEmpty()) {
                    cachedPhotos = photos
                }
                if (photos.isNotEmpty()) {
                    moveToNextUntagged(_currentIndex.value)
                } else {
                    _currentIndex.value = 0
                    loadTagsForPhoto(null)
                }
            }
        }
        
        // Load classification settings
        viewModelScope.launch {
            preferencesRepository.getPhotoClassificationMode().collect { mode ->
                _classificationMode.value = mode
            }
        }
        viewModelScope.launch {
            preferencesRepository.getAlbumAddAction().collect { action ->
                _albumAddAction.value = action
            }
        }
        viewModelScope.launch {
            albumBubbleDao.getAll().collect { albums ->
                _albumBubbleList.value = albums
            }
        }
    }
    
    /**
     * Load tags for the current photo.
     */
    private suspend fun loadTagsForPhoto(photoId: String?) {
        if (photoId != null) {
            val tags = tagDao.getTagsForPhoto(photoId).first()
            _currentPhotoTags.value = tags
        } else {
            _currentPhotoTags.value = emptyList()
        }
    }
    
    /**
     * Add tag to current photo and advance to next.
     * Single-tap = assign tag + next photo.
     * Uses TagAlbumSyncUseCase to sync with linked albums.
     */
    fun tagCurrentPhotoAndNext(tag: TagEntity) {
        val photo = uiState.value.currentPhoto ?: return
        
        viewModelScope.launch {
            try {
                // Add tag to photo with album sync (copies photo to linked album if applicable)
                tagAlbumSyncUseCase.addPhotoToTagWithSync(photo.id, tag.id)
                _taggedCount.value++
                
                // Increment tag achievement count
                preferencesRepository.incrementTaggedCount()
                
                // Move to next untagged photo
                advanceToNext()
            } catch (e: Exception) {
                _error.value = "标签添加失败: ${e.message}"
            }
        }
    }
    
    /**
     * Skip current photo without tagging.
     */
    fun skipCurrentPhoto() {
        _skippedCount.value++
        advanceToNext()
    }
    
    /**
     * Advance to the next photo.
     */
    private fun advanceToNext() {
        val nextIndex = _currentIndex.value + 1
        viewModelScope.launch {
            moveToNextUntagged(nextIndex)
        }
    }
    
    /**
     * Go back to previous photo.
     */
    fun goToPrevious() {
        val previousIndex = _currentIndex.value - 1
        if (previousIndex >= 0) {
            viewModelScope.launch {
                moveToPreviousUntagged(previousIndex)
            }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }

    fun createTag(name: String, color: Int) {
        viewModelScope.launch {
            try {
                val tag = TagEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    parentId = null,
                    color = color
                )
                tagDao.insert(tag)
            } catch (e: Exception) {
                _error.value = "创建标签失败: ${e.message}"
            }
        }
    }
    
    /**
     * Assign current photo to an album and advance to next.
     * The action (copy or move) is determined by albumAddAction setting.
     */
    fun assignCurrentPhotoToAlbum(album: AlbumBubbleEntity) {
        val photo = uiState.value.currentPhoto ?: return
        
        viewModelScope.launch {
            try {
                val photoUri = Uri.parse(photo.systemUri)
                // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
                // Fall back to Pictures/ only for newly created albums
                val targetPath = mediaStoreDataSource.getAlbumPath(album.bucketId)
                    ?: "Pictures/${album.displayName}"
                
                when (_albumAddAction.value) {
                    AlbumAddAction.COPY -> {
                        val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                        if (result.isSuccess) {
                            _taggedCount.value++
                            _message.value = "已复制到 ${album.displayName}"
                            advanceToNext()
                        } else {
                            _error.value = "复制失败: ${result.exceptionOrNull()?.message}"
                        }
                    }
                    AlbumAddAction.MOVE -> {
                        when (val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)) {
                            is MovePhotoResult.Success -> {
                                _taggedCount.value++
                                _message.value = "已移动到 ${album.displayName}"
                                advanceToNext()
                            }
                            is MovePhotoResult.NeedsConfirmation -> {
                                // Store pending operation for UI to handle
                                _error.value = "需要权限确认才能移动照片"
                            }
                            is MovePhotoResult.Error -> {
                                _error.value = "移动失败: ${result.message}"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
    }
    
    /**
     * Clear message.
     */
    fun clearMessage() {
        _message.value = null
    }

    private suspend fun moveToNextUntagged(startIndex: Int) {
        val photos = if (cachedPhotos.isNotEmpty()) cachedPhotos else uiState.value.photos
        var index = startIndex
        var skipped = 0
        while (index < photos.size && tagDao.photoHasAnyTag(photos[index].id)) {
            skipped++
            index++
        }
        if (skipped > 0) {
            _skippedCount.value += skipped
        }
        _currentIndex.value = index
        loadTagsForPhoto(photos.getOrNull(index)?.id)
    }

    private suspend fun moveToPreviousUntagged(startIndex: Int) {
        val photos = if (cachedPhotos.isNotEmpty()) cachedPhotos else uiState.value.photos
        val fallbackIndex = _currentIndex.value
        var index = startIndex
        var skipped = 0
        while (index >= 0 && tagDao.photoHasAnyTag(photos[index].id)) {
            skipped++
            index--
        }
        if (skipped > 0) {
            _skippedCount.value += skipped
        }
        if (index < 0) {
            _currentIndex.value = fallbackIndex
            loadTagsForPhoto(photos.getOrNull(fallbackIndex)?.id)
            return
        }
        _currentIndex.value = index
        loadTagsForPhoto(photos.getOrNull(index)?.id)
    }
}
