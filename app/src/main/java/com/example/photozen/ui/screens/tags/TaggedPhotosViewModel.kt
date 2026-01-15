package com.example.photozen.ui.screens.tags

import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.TagEntity
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.DeleteResult
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.TagAlbumSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pending photo delete request requiring user confirmation.
 */
data class PendingPhotoDeleteRequest(
    val intentSender: IntentSender,
    val photoId: String,
    val message: String
)

/**
 * UI State for TaggedPhotosScreen.
 */
data class TaggedPhotosUiState(
    val isLoading: Boolean = true,
    val tagId: String? = null,
    val tagName: String? = null,
    val tagLinkedAlbumId: String? = null,  // Whether current tag is linked to an album
    val photos: List<PhotoEntity> = emptyList(),
    val allTags: List<TagEntity> = emptyList(),
    val defaultExternalApp: String? = null,
    val error: String? = null,
    val message: String? = null,
    val pendingDeleteRequest: PendingPhotoDeleteRequest? = null
)

/**
 * ViewModel for displaying photos with a specific tag.
 */
@HiltViewModel
class TaggedPhotosViewModel @Inject constructor(
    private val photoDao: PhotoDao,
    private val tagDao: TagDao,
    private val preferencesRepository: PreferencesRepository,
    private val tagAlbumSyncUseCase: TagAlbumSyncUseCase,
    private val mediaStoreDataSource: MediaStoreDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TaggedPhotosUiState())
    val uiState: StateFlow<TaggedPhotosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tagDao.getAllTags().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.getDefaultExternalApp().collect { app ->
                _uiState.update { it.copy(defaultExternalApp = app) }
            }
        }
    }
    
    /**
     * Set default external app for opening photos.
     */
    suspend fun setDefaultExternalApp(packageName: String?) {
        preferencesRepository.setDefaultExternalApp(packageName)
    }
    
    // Job for collecting photos, allows cancellation when tag changes
    private var photoCollectionJob: kotlinx.coroutines.Job? = null
    
    /**
     * Load photos for the given tag.
     * Uses a reactive Flow query to automatically update when photos change.
     */
    fun loadPhotosForTag(tagId: String) {
        if (_uiState.value.tagId == tagId) return
        
        // Cancel any existing collection job
        photoCollectionJob?.cancel()
        
        _uiState.update { it.copy(isLoading = true, tagId = tagId) }
        
        viewModelScope.launch {
            try {
                // Get tag info including linked album info
                val tag = tagDao.getById(tagId)
                _uiState.update { 
                    it.copy(
                        tagName = tag?.name,
                        tagLinkedAlbumId = tag?.linkedAlbumId
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "加载标签信息失败: ${e.message}"
                    ) 
                }
                return@launch
            }
        }
        
        // Start collecting photos with the reactive query
        photoCollectionJob = viewModelScope.launch {
            try {
                // Use the new reactive query that automatically updates when:
                // - Photos are added/removed from the tag
                // - Photo entities are updated/deleted
                photoDao.getPhotosByTagId(tagId).collect { photos ->
                    _uiState.update { 
                        it.copy(isLoading = false, photos = photos) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "加载失败: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Check if the current tag is linked to an album.
     */
    fun isTagLinkedToAlbum(): Boolean {
        return _uiState.value.tagLinkedAlbumId != null
    }

    /**
     * Remove tag from photo without deleting the photo.
     */
    fun removeTagFromPhoto(photoId: String) {
        val currentTagId = _uiState.value.tagId ?: return
        viewModelScope.launch {
            tagDao.removeTagFromPhoto(photoId, currentTagId)
            _uiState.update { it.copy(message = "已移除标签") }
        }
    }
    
    /**
     * Remove tag from photo and also delete the photo from the system.
     * On Android 11+, this may require user confirmation.
     */
    fun removeTagAndDeletePhoto(photoId: String) {
        val currentTagId = _uiState.value.tagId ?: return
        viewModelScope.launch {
            try {
                // First get the photo to get its URI
                val photo = photoDao.getById(photoId)
                if (photo == null) {
                    _uiState.update { it.copy(error = "找不到照片") }
                    return@launch
                }
                
                val photoUri = Uri.parse(photo.systemUri)
                
                // Try to delete the photo
                when (val result = mediaStoreDataSource.deletePhotos(listOf(photoUri))) {
                    is DeleteResult.Success -> {
                        // Photo deleted successfully, now remove from database
                        tagDao.removeTagFromPhoto(photoId, currentTagId)
                        photoDao.deleteById(photoId)
                        _uiState.update { it.copy(message = "已删除照片") }
                    }
                    is DeleteResult.RequiresConfirmation -> {
                        // Need user confirmation
                        _uiState.update { 
                            it.copy(
                                pendingDeleteRequest = PendingPhotoDeleteRequest(
                                    intentSender = result.intentSender,
                                    photoId = photoId,
                                    message = "已删除照片"
                                )
                            )
                        }
                    }
                    is DeleteResult.Failed -> {
                        _uiState.update { it.copy(error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除照片失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Called after user confirms photo deletion.
     */
    fun onPhotoDeleteConfirmed() {
        val pending = _uiState.value.pendingDeleteRequest
        val currentTagId = _uiState.value.tagId
        if (pending != null && currentTagId != null) {
            viewModelScope.launch {
                // Remove from database after user confirmed deletion
                tagDao.removeTagFromPhoto(pending.photoId, currentTagId)
                photoDao.deleteById(pending.photoId)
                _uiState.update { 
                    it.copy(
                        pendingDeleteRequest = null,
                        message = pending.message
                    ) 
                }
            }
        }
    }
    
    /**
     * Called when user cancels photo deletion.
     */
    fun onPhotoDeleteCancelled() {
        _uiState.update { it.copy(pendingDeleteRequest = null) }
    }
    
    /**
     * Clear message.
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    /**
     * Clear error.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Change tag for a photo - removes from current tag and adds to new tag.
     * Uses TagAlbumSyncUseCase to sync with linked albums.
     */
    fun changeTagForPhoto(photoId: String, newTagId: String) {
        val currentTagId = _uiState.value.tagId ?: return
        if (newTagId == currentTagId) return
        viewModelScope.launch {
            tagDao.removeTagFromPhoto(photoId, currentTagId)
            // Add to new tag with album sync (copies photo to linked album if applicable)
            tagAlbumSyncUseCase.addPhotoToTagWithSync(photoId, newTagId)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        photoCollectionJob?.cancel()
    }
}
