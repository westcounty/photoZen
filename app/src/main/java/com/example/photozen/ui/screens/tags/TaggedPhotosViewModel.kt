package com.example.photozen.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.PhotoTagCrossRef
import com.example.photozen.data.local.entity.TagEntity
import com.example.photozen.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for TaggedPhotosScreen.
 */
data class TaggedPhotosUiState(
    val isLoading: Boolean = true,
    val tagId: String? = null,
    val tagName: String? = null,
    val photos: List<PhotoEntity> = emptyList(),
    val allTags: List<TagEntity> = emptyList(),
    val defaultExternalApp: String? = null,
    val error: String? = null
)

/**
 * ViewModel for displaying photos with a specific tag.
 */
@HiltViewModel
class TaggedPhotosViewModel @Inject constructor(
    private val photoDao: PhotoDao,
    private val tagDao: TagDao,
    private val preferencesRepository: PreferencesRepository
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
    
    /**
     * Load photos for the given tag.
     */
    fun loadPhotosForTag(tagId: String) {
        if (_uiState.value.tagId == tagId) return
        
        _uiState.update { it.copy(isLoading = true, tagId = tagId) }
        
        viewModelScope.launch {
            try {
                // Get tag info
                val tag = tagDao.getById(tagId)
                _uiState.update { it.copy(tagName = tag?.name) }
                
                // Get photo IDs with this tag
                tagDao.getPhotoIdsWithTag(tagId).collect { photoIds ->
                    if (photoIds.isEmpty()) {
                        _uiState.update { 
                            it.copy(isLoading = false, photos = emptyList()) 
                        }
                    } else {
                        // Load photo entities
                        val photos = photoIds.mapNotNull { photoId ->
                            photoDao.getById(photoId)
                        }
                        _uiState.update { 
                            it.copy(isLoading = false, photos = photos) 
                        }
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

    fun removeTagFromPhoto(photoId: String) {
        val currentTagId = _uiState.value.tagId ?: return
        viewModelScope.launch {
            tagDao.removeTagFromPhoto(photoId, currentTagId)
        }
    }

    fun changeTagForPhoto(photoId: String, newTagId: String) {
        val currentTagId = _uiState.value.tagId ?: return
        if (newTagId == currentTagId) return
        viewModelScope.launch {
            tagDao.removeTagFromPhoto(photoId, currentTagId)
            tagDao.addTagToPhoto(PhotoTagCrossRef(photoId, newTagId))
        }
    }
}
