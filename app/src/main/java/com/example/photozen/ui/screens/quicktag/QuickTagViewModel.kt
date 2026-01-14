package com.example.photozen.ui.screens.quicktag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.PhotoTagCrossRef
import com.example.photozen.data.local.entity.TagEntity
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val error: String? = null
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
 */
@HiltViewModel
class QuickTagViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val tagDao: TagDao,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _currentIndex = MutableStateFlow(0)
    private val _taggedCount = MutableStateFlow(0)
    private val _skippedCount = MutableStateFlow(0)
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _currentPhotoTags = MutableStateFlow<List<TagEntity>>(emptyList())
    
    // Cache photos list to avoid reloading
    private var cachedPhotos: List<PhotoEntity> = emptyList()
    
    val uiState: StateFlow<QuickTagUiState> = combine(
        photoRepository.getKeepPhotos(),
        tagDao.getAllTags(),
        _currentIndex,
        _taggedCount,
        _skippedCount,
        _isLoading,
        _error,
        _currentPhotoTags
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val photos = values[0] as List<PhotoEntity>
        val tags = values[1] as List<TagEntity>
        val currentIndex = values[2] as Int
        val taggedCount = values[3] as Int
        val skippedCount = values[4] as Int
        val isLoading = values[5] as Boolean
        val error = values[6] as String?
        val currentPhotoTags = values[7] as List<TagEntity>
        
        // Update cached photos
        if (photos.isNotEmpty() && cachedPhotos.isEmpty()) {
            cachedPhotos = photos
        }
        
        val displayPhotos = if (cachedPhotos.isNotEmpty()) cachedPhotos else photos
        
        QuickTagUiState(
            photos = displayPhotos,
            currentIndex = currentIndex,
            tags = tags,
            currentPhotoTags = currentPhotoTags,
            isLoading = isLoading && displayPhotos.isEmpty(),
            isComplete = currentIndex >= displayPhotos.size && displayPhotos.isNotEmpty(),
            taggedCount = taggedCount,
            skippedCount = skippedCount,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuickTagUiState()
    )
    
    init {
        viewModelScope.launch {
            _isLoading.value = false
            // Load tags for current photo
            loadCurrentPhotoTags()
        }
    }
    
    /**
     * Load tags for the current photo.
     */
    private fun loadCurrentPhotoTags() {
        viewModelScope.launch {
            val currentPhoto = uiState.value.currentPhoto
            if (currentPhoto != null) {
                val tags = tagDao.getTagsForPhoto(currentPhoto.id).first()
                _currentPhotoTags.value = tags
            } else {
                _currentPhotoTags.value = emptyList()
            }
        }
    }
    
    /**
     * Add tag to current photo and advance to next.
     * Single-tap = assign tag + next photo.
     */
    fun tagCurrentPhotoAndNext(tag: TagEntity) {
        val photo = uiState.value.currentPhoto ?: return
        
        viewModelScope.launch {
            try {
                // Add tag to photo
                tagDao.addTagToPhoto(PhotoTagCrossRef(photo.id, tag.id))
                _taggedCount.value++
                
                // Increment tag achievement count
                preferencesRepository.incrementTaggedCount()
                
                // Move to next photo
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
        _currentIndex.value++
        loadCurrentPhotoTags()
    }
    
    /**
     * Go back to previous photo.
     */
    fun goToPrevious() {
        if (_currentIndex.value > 0) {
            _currentIndex.value--
            loadCurrentPhotoTags()
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
}
