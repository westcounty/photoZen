package com.example.photozen.ui.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.CropState
import com.example.photozen.domain.usecase.CreateVirtualCopyUseCase
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.UpdateCropStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoEditorUiState(
    val photo: PhotoEntity? = null,
    val virtualCopies: List<PhotoEntity> = emptyList(),
    val currentCropState: CropState = CropState(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasChanges: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

private data class InternalState(
    val currentCropState: CropState = CropState(),
    val originalCropState: CropState = CropState(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
) {
    val hasChanges: Boolean get() = currentCropState != originalCropState
}

@HiltViewModel
class PhotoEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val updateCropStateUseCase: UpdateCropStateUseCase,
    private val createVirtualCopyUseCase: CreateVirtualCopyUseCase
) : ViewModel() {
    
    private val photoId: String = savedStateHandle.get<String>("photoId") ?: ""
    
    private val _internalState = MutableStateFlow(InternalState())
    
    val uiState: StateFlow<PhotoEditorUiState> = combine(
        getPhotosUseCase.getPhotoById(photoId),
        getPhotosUseCase.getVirtualCopies(photoId),
        _internalState
    ) { photo, virtualCopies, internal ->
        PhotoEditorUiState(
            photo = photo,
            virtualCopies = virtualCopies,
            currentCropState = internal.currentCropState,
            isLoading = internal.isLoading,
            isSaving = internal.isSaving,
            hasChanges = internal.hasChanges,
            message = internal.message,
            error = internal.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PhotoEditorUiState()
    )
    
    init {
        loadPhoto()
    }
    
    private fun loadPhoto() {
        viewModelScope.launch {
            _internalState.update { it.copy(isLoading = true) }
            try {
                // Get initial crop state from the photo
                getPhotosUseCase.getPhotoById(photoId).collect { photo ->
                    photo?.let {
                        _internalState.update { state ->
                            state.copy(
                                currentCropState = photo.cropState,
                                originalCropState = photo.cropState,
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _internalState.update { 
                    it.copy(isLoading = false, error = "加载失败: ${e.message}")
                }
            }
        }
    }
    
    fun updateCropScale(scale: Float) {
        _internalState.update { state ->
            state.copy(
                currentCropState = state.currentCropState.copy(scale = scale.coerceIn(0.1f, 3f))
            )
        }
    }
    
    fun updateCropOffset(offsetX: Float, offsetY: Float) {
        _internalState.update { state ->
            state.copy(
                currentCropState = state.currentCropState.copy(offsetX = offsetX, offsetY = offsetY)
            )
        }
    }
    
    fun resetCrop() {
        _internalState.update { state ->
            state.copy(currentCropState = CropState())
        }
    }
    
    fun saveCropState() {
        viewModelScope.launch {
            _internalState.update { it.copy(isSaving = true) }
            try {
                updateCropStateUseCase.update(photoId, _internalState.value.currentCropState)
                _internalState.update { state ->
                    state.copy(
                        isSaving = false,
                        originalCropState = state.currentCropState,
                        message = "裁切已保存"
                    )
                }
            } catch (e: Exception) {
                _internalState.update { 
                    it.copy(isSaving = false, error = "保存失败: ${e.message}")
                }
            }
        }
    }
    
    fun createVirtualCopy() {
        viewModelScope.launch {
            _internalState.update { it.copy(isSaving = true) }
            try {
                createVirtualCopyUseCase(photoId)
                _internalState.update { 
                    it.copy(isSaving = false, message = "虚拟副本已创建")
                }
            } catch (e: Exception) {
                _internalState.update { 
                    it.copy(isSaving = false, error = "创建失败: ${e.message}")
                }
            }
        }
    }
    
    fun clearMessage() {
        _internalState.update { it.copy(message = null) }
    }
    
    fun clearError() {
        _internalState.update { it.copy(error = null) }
    }
}
