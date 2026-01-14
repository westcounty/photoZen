package com.example.photozen.ui.screens.workflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.entity.PhotoTagCrossRef
import com.example.photozen.data.local.entity.TagEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the tagging stage in the workflow.
 * Handles tag selection and assignment to photos.
 */
@HiltViewModel
class TaggingViewModel @Inject constructor(
    private val tagDao: TagDao,
    private val photoDao: PhotoDao
) : ViewModel() {
    
    /**
     * All available tags (root level only for simplicity in workflow).
     */
    val tags: StateFlow<List<TagEntity>> = tagDao.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _currentPhotoId = MutableStateFlow<String?>(null)
    
    private val _currentPhotoTags = MutableStateFlow<List<TagEntity>>(emptyList())
    val currentPhotoTags: StateFlow<List<TagEntity>> = _currentPhotoTags.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Set the current photo being tagged.
     */
    fun setCurrentPhoto(photoId: String) {
        if (_currentPhotoId.value == photoId) return
        
        _currentPhotoId.value = photoId
        loadPhotoTags(photoId)
    }
    
    /**
     * Load tags for the current photo.
     */
    private fun loadPhotoTags(photoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val photoWithTags = photoDao.getWithTagsById(photoId)
                _currentPhotoTags.value = photoWithTags?.tags ?: emptyList()
            } catch (e: Exception) {
                _currentPhotoTags.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle tag assignment for a photo.
     * If already assigned, remove it. Otherwise, add it.
     */
    fun togglePhotoTag(photoId: String, tag: TagEntity) {
        viewModelScope.launch {
            val currentTags = _currentPhotoTags.value
            val isAssigned = currentTags.any { it.id == tag.id }
            
            if (isAssigned) {
                // Remove tag
                tagDao.removeTagFromPhoto(photoId, tag.id)
                _currentPhotoTags.value = currentTags.filter { it.id != tag.id }
            } else {
                // Add tag
                tagDao.addTagToPhoto(PhotoTagCrossRef(photoId, tag.id))
                _currentPhotoTags.value = currentTags + tag
            }
        }
    }
    
    /**
     * Create a new tag.
     */
    fun createTag(name: String, color: Int) {
        viewModelScope.launch {
            val tag = TagEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                parentId = null,
                color = color
            )
            tagDao.insert(tag)
        }
    }
}
