package com.example.photozen.ui.screens.tags

import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.dao.TagWithCount
import com.example.photozen.data.local.entity.AlbumCopyMode
import com.example.photozen.data.local.entity.TagEntity
import com.example.photozen.data.source.Album
import com.example.photozen.domain.usecase.CreateLinkAlbumResult
import com.example.photozen.domain.usecase.DeleteTagResult
import com.example.photozen.domain.usecase.TagAlbumSyncUseCase
import com.example.photozen.ui.components.bubble.BubbleNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Pending delete confirmation request.
 */
data class PendingDeleteRequest(
    val intentSender: IntentSender,
    val tagIdToDeleteAfter: String? = null,  // For delete tag flow
    val message: String? = null               // Message to show after confirmation
)

/**
 * UI State for the Tag Bubble screen.
 * Simplified: Only one level of tags (no hierarchy for now).
 */
data class TagBubbleUiState(
    val isLoading: Boolean = true,
    val bubbleNodes: List<BubbleNode> = emptyList(),
    val error: String? = null,
    val availableAlbums: List<Album> = emptyList(),
    val isLoadingAlbums: Boolean = false,
    val message: String? = null,
    val pendingDeleteRequest: PendingDeleteRequest? = null
) {
    val currentTitle: String = "标签"
}

/**
 * ViewModel for the Tag Bubble screen.
 * 
 * Manages the bubble graph visualization of tags with physics simulation.
 * Simplified: Only one level of tags for now.
 */
@HiltViewModel
class TagBubbleViewModel @Inject constructor(
    private val tagDao: TagDao,
    private val tagAlbumSyncUseCase: TagAlbumSyncUseCase
) : ViewModel() {
    
    companion object {
        // Bubble size constants - increased for better touch targets
        private const val MIN_BUBBLE_RADIUS = 100f
        private const val MAX_BUBBLE_RADIUS = 170f
    }
    
    private val _uiState = MutableStateFlow(TagBubbleUiState())
    val uiState: StateFlow<TagBubbleUiState> = _uiState.asStateFlow()
    
    private var collectionJob: Job? = null
    
    init {
        loadTags()
        // Sync all linked tags on startup
        viewModelScope.launch {
            try {
                tagAlbumSyncUseCase.syncAllLinkedTags()
            } catch (e: Exception) {
                // Ignore sync errors on startup
            }
        }
    }
    
    /**
     * Load all root tags.
     */
    private fun loadTags() {
        collectionJob?.cancel()
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        collectionJob = viewModelScope.launch {
            try {
                tagDao.getRootTagsWithPhotoCount().collect { tags ->
                    val nodes = createBubbleNodes(tags)
                    _uiState.update { 
                        it.copy(isLoading = false, bubbleNodes = nodes, error = null) 
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "加载标签失败: ${e.message}") 
                }
            }
        }
    }
    
    /**
     * Create a new tag.
     */
    fun createTag(name: String, color: Int) {
        viewModelScope.launch {
            try {
                val tag = TagEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    parentId = null, // Root level only
                    color = color
                )
                tagDao.insert(tag)
                // Flow collection will auto-update the UI
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "创建标签失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Delete a tag.
     * 
     * @param tagId Tag ID to delete
     * @param deleteLinkedAlbum Whether to also delete the linked album
     */
    fun deleteTag(tagId: String, deleteLinkedAlbum: Boolean = false) {
        viewModelScope.launch {
            try {
                when (val result = tagAlbumSyncUseCase.deleteTagWithAlbum(tagId, deleteLinkedAlbum)) {
                    is DeleteTagResult.Success -> {
                        _uiState.update { it.copy(message = "标签已删除") }
                    }
                    is DeleteTagResult.RequiresConfirmation -> {
                        // Need user confirmation to delete album photos
                        _uiState.update { 
                            it.copy(
                                pendingDeleteRequest = PendingDeleteRequest(
                                    intentSender = result.intentSender,
                                    tagIdToDeleteAfter = result.tagId,
                                    message = "标签和相册已删除"
                                )
                            )
                        }
                    }
                    is DeleteTagResult.Failed -> {
                        _uiState.update { it.copy(error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除标签失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Called after user confirms the delete request.
     * Completes the pending deletion.
     */
    fun onDeleteConfirmed() {
        val pending = _uiState.value.pendingDeleteRequest
        viewModelScope.launch {
            if (pending?.tagIdToDeleteAfter != null) {
                tagAlbumSyncUseCase.completeTagDeletion(pending.tagIdToDeleteAfter)
            }
            _uiState.update { 
                it.copy(
                    pendingDeleteRequest = null,
                    message = pending?.message
                )
            }
        }
    }
    
    /**
     * Called when user cancels the delete request.
     */
    fun onDeleteCancelled() {
        _uiState.update { it.copy(pendingDeleteRequest = null) }
    }
    
    /**
     * Load available albums for linking.
     */
    fun loadAvailableAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAlbums = true) }
            try {
                val albums = tagAlbumSyncUseCase.getAvailableAlbums()
                _uiState.update { it.copy(availableAlbums = albums, isLoadingAlbums = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingAlbums = false, 
                        error = "加载相册失败: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Create a new album and link it to a tag.
     */
    fun createAndLinkAlbum(tagId: String, albumName: String, copyMode: AlbumCopyMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                when (val result = tagAlbumSyncUseCase.createAndLinkAlbum(tagId, albumName, copyMode)) {
                    is CreateLinkAlbumResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                message = "已创建相册「${result.albumName}」并关联，已添加 ${result.photosAdded} 张照片"
                            ) 
                        }
                    }
                    is CreateLinkAlbumResult.RequiresDeleteConfirmation -> {
                        // Album created and photos copied, but need user confirmation to delete originals
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                pendingDeleteRequest = PendingDeleteRequest(
                                    intentSender = result.intentSender,
                                    tagIdToDeleteAfter = null,
                                    message = "已创建相册「${result.albumName}」并移动 ${result.photosAdded} 张照片"
                                )
                            )
                        }
                    }
                    is CreateLinkAlbumResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "关联相册失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Link an existing album to a tag.
     * Tagged photos not in the album will be copied/moved based on copyMode.
     */
    fun linkExistingAlbum(tagId: String, album: Album, copyMode: AlbumCopyMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                when (val result = tagAlbumSyncUseCase.linkExistingAlbum(tagId, album, copyMode)) {
                    is CreateLinkAlbumResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = "已关联相册「${result.albumName}」，已同步 ${result.photosAdded} 张照片"
                            )
                        }
                    }
                    is CreateLinkAlbumResult.RequiresDeleteConfirmation -> {
                        // Album linked and photos copied, but need user confirmation to delete originals
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingDeleteRequest = PendingDeleteRequest(
                                    intentSender = result.intentSender,
                                    tagIdToDeleteAfter = null,
                                    message = "已关联相册「${result.albumName}」并移动 ${result.photosAdded} 张照片"
                                )
                            )
                        }
                    }
                    is CreateLinkAlbumResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "关联相册失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Unlink album from a tag.
     */
    fun unlinkAlbum(tagId: String) {
        viewModelScope.launch {
            try {
                tagAlbumSyncUseCase.unlinkAlbum(tagId)
                _uiState.update { it.copy(message = "已解除相册关联") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "解除关联失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Get tag info by ID.
     */
    suspend fun getTagInfo(tagId: String): TagEntity? {
        return tagDao.getById(tagId)
    }
    
    /**
     * Clear error message.
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
     * Convert TagWithCount list to BubbleNode list.
     */
    private fun createBubbleNodes(tags: List<TagWithCount>): List<BubbleNode> {
        if (tags.isEmpty()) return emptyList()
        
        val maxCount = tags.maxOfOrNull { it.photoCount }?.coerceAtLeast(1) ?: 1
        
        return tags.map { tag ->
            val sizeRatio = if (maxCount > 0) {
                (tag.photoCount.toFloat() / maxCount).coerceIn(0.3f, 1f)
            } else {
                0.5f
            }
            
            val radius = MIN_BUBBLE_RADIUS + 
                (MAX_BUBBLE_RADIUS - MIN_BUBBLE_RADIUS) * sizeRatio
            
            BubbleNode(
                id = tag.id,
                label = tag.name,
                radius = radius,
                color = tag.color,
                photoCount = tag.photoCount,
                isCenter = false,
                hasChildren = false,
                linkedAlbumId = tag.linkedAlbumId,
                linkedAlbumName = tag.linkedAlbumName
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        collectionJob?.cancel()
    }
}
