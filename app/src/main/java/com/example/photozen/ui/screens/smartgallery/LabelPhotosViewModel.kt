package com.example.photozen.ui.screens.smartgallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.repository.LabelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Label Photos screen.
 */
data class LabelPhotosUiState(
    val label: String = "",
    val photos: List<PhotoEntity> = emptyList(),
    val selectedPhotoIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSelectionMode: Boolean = false,
    val error: String? = null
) {
    val photoCount: Int
        get() = photos.size
    
    val selectedCount: Int
        get() = selectedPhotoIds.size
    
    val hasPhotos: Boolean
        get() = photos.isNotEmpty()
}

/**
 * ViewModel for Label Photos screen.
 */
@HiltViewModel
class LabelPhotosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val labelRepository: LabelRepository
) : ViewModel() {
    
    private val label: String = savedStateHandle.get<String>("label") ?: ""
    
    private val _photos = MutableStateFlow<List<PhotoEntity>>(emptyList())
    private val _selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(true)
    private val _isSelectionMode = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<LabelPhotosUiState> = combine(
        _photos,
        _selectedPhotoIds,
        _isLoading,
        _isSelectionMode,
        _error
    ) { photos, selectedIds, isLoading, isSelectionMode, error ->
        LabelPhotosUiState(
            label = label,
            photos = photos,
            selectedPhotoIds = selectedIds,
            isLoading = isLoading,
            isSelectionMode = isSelectionMode,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LabelPhotosUiState(label = label)
    )
    
    init {
        loadPhotos()
    }
    
    private fun loadPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                labelRepository.getPhotosByLabel(label).collect { photos ->
                    _photos.value = photos
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "加载照片失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle selection mode.
     */
    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedPhotoIds.value = emptySet()
        }
    }
    
    /**
     * Toggle photo selection.
     */
    fun togglePhotoSelection(photoId: String) {
        val currentSelection = _selectedPhotoIds.value.toMutableSet()
        if (currentSelection.contains(photoId)) {
            currentSelection.remove(photoId)
        } else {
            currentSelection.add(photoId)
        }
        _selectedPhotoIds.value = currentSelection
        
        // Auto-enable selection mode if selecting
        if (currentSelection.isNotEmpty() && !_isSelectionMode.value) {
            _isSelectionMode.value = true
        }
        // Auto-disable selection mode if nothing selected
        if (currentSelection.isEmpty() && _isSelectionMode.value) {
            _isSelectionMode.value = false
        }
    }
    
    /**
     * Select all photos.
     */
    fun selectAll() {
        _selectedPhotoIds.value = _photos.value.map { it.id }.toSet()
        _isSelectionMode.value = true
    }
    
    /**
     * Clear selection.
     */
    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
        _isSelectionMode.value = false
    }
    
    /**
     * Clear error.
     */
    fun clearError() {
        _error.value = null
    }
}
