package com.example.photozen.ui.screens.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.AspectRatio
import com.example.photozen.data.model.CropState
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.util.ImageSaver
import com.example.photozen.domain.usecase.CreateVirtualCopyUseCase
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.UpdateCropStateUseCase
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

data class PhotoEditorUiState(
    val photo: PhotoEntity? = null,
    val virtualCopies: List<PhotoEntity> = emptyList(),
    val currentCropState: CropState = CropState(),
    val selectedAspectRatio: AspectRatio = AspectRatio.ORIGINAL,
    val isVirtualCopy: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSavingToFile: Boolean = false,
    val hasChanges: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    // For fullscreen virtual copy preview
    val previewingVirtualCopy: PhotoEntity? = null,
    val savedImageUri: Uri? = null
)

private data class InternalState(
    val currentCropState: CropState = CropState(),
    val originalCropState: CropState = CropState(),
    val selectedAspectRatio: AspectRatio = AspectRatio.ORIGINAL,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSavingToFile: Boolean = false,
    val previewingVirtualCopy: PhotoEntity? = null,
    val message: String? = null,
    val error: String? = null,
    val savedImageUri: Uri? = null
) {
    val hasChanges: Boolean get() = currentCropState != originalCropState
}

@HiltViewModel
class PhotoEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val updateCropStateUseCase: UpdateCropStateUseCase,
    private val createVirtualCopyUseCase: CreateVirtualCopyUseCase,
    private val photoRepository: PhotoRepository,
    private val imageSaver: ImageSaver
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
            selectedAspectRatio = internal.selectedAspectRatio,
            isVirtualCopy = photo?.isVirtualCopy == true,
            isLoading = internal.isLoading,
            isSaving = internal.isSaving,
            isSavingToFile = internal.isSavingToFile,
            hasChanges = internal.hasChanges,
            message = internal.message,
            error = internal.error,
            previewingVirtualCopy = internal.previewingVirtualCopy,
            savedImageUri = internal.savedImageUri
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
                val photo = getPhotosUseCase.getPhotoById(photoId).first()
                photo?.let {
                    val aspectRatio = AspectRatio.fromId(photo.cropState.aspectRatioId)
                    _internalState.update { state ->
                        state.copy(
                            currentCropState = photo.cropState,
                            originalCropState = photo.cropState,
                            selectedAspectRatio = aspectRatio,
                            isLoading = false
                        )
                    }
                } ?: run {
                    _internalState.update { it.copy(isLoading = false, error = "照片不存在") }
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
                currentCropState = state.currentCropState.copy(
                    scale = scale.coerceIn(0.5f, 10f)
                )
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
    
    fun updateRotation(rotation: Float) {
        _internalState.update { state ->
            state.copy(
                currentCropState = state.currentCropState.copy(
                    rotation = rotation.coerceIn(-45f, 45f)
                )
            )
        }
    }
    
    fun selectAspectRatio(aspectRatio: AspectRatio) {
        _internalState.update { state ->
            state.copy(
                selectedAspectRatio = aspectRatio,
                currentCropState = state.currentCropState.copy(aspectRatioId = aspectRatio.id)
            )
        }
    }
    
    fun resetCrop() {
        _internalState.update { state ->
            state.copy(
                currentCropState = CropState(),
                selectedAspectRatio = AspectRatio.ORIGINAL
            )
        }
    }
    
    fun saveCropState() {
        viewModelScope.launch {
            _internalState.update { it.copy(isSaving = true) }
            try {
                val cropState = _internalState.value.currentCropState
                updateCropStateUseCase.update(photoId, cropState)
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
                // Pass current crop state to the virtual copy
                val currentCropState = _internalState.value.currentCropState
                createVirtualCopyUseCase(photoId, currentCropState)
                _internalState.update { 
                    it.copy(isSaving = false, message = "虚拟副本已创建（包含当前裁切）")
                }
            } catch (e: Exception) {
                _internalState.update { 
                    it.copy(isSaving = false, error = "创建失败: ${e.message}")
                }
            }
        }
    }
    
    fun deleteVirtualCopy(copyId: String) {
        viewModelScope.launch {
            try {
                photoRepository.deleteVirtualCopy(copyId)
                _internalState.update { it.copy(message = "虚拟副本已删除") }
            } catch (e: Exception) {
                _internalState.update { it.copy(error = "删除失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Load crop state from a virtual copy into the editor
     */
    fun loadCropStateFromCopy(copy: PhotoEntity) {
        val aspectRatio = AspectRatio.fromId(copy.cropState.aspectRatioId)
        _internalState.update { state ->
            state.copy(
                currentCropState = copy.cropState,
                originalCropState = copy.cropState,
                selectedAspectRatio = aspectRatio
            )
        }
    }
    
    /**
     * Save current crop state to a specific virtual copy
     */
    fun saveVirtualCopyCropState(copyId: String) {
        viewModelScope.launch {
            _internalState.update { it.copy(isSaving = true) }
            try {
                val cropState = _internalState.value.currentCropState
                updateCropStateUseCase.update(copyId, cropState)
                _internalState.update { state ->
                    state.copy(
                        isSaving = false,
                        originalCropState = state.currentCropState,
                        message = "副本已更新"
                    )
                }
            } catch (e: Exception) {
                _internalState.update { 
                    it.copy(isSaving = false, error = "保存失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Show fullscreen preview for a virtual copy
     */
    fun previewVirtualCopy(copy: PhotoEntity) {
        _internalState.update { it.copy(previewingVirtualCopy = copy) }
    }
    
    /**
     * Dismiss fullscreen preview
     */
    fun dismissPreview() {
        _internalState.update { it.copy(previewingVirtualCopy = null) }
    }
    
    /**
     * Save virtual copy (or current photo with crop) to a new image file
     */
    fun saveToNewImage(photo: PhotoEntity? = null) {
        val photoToSave = photo ?: _internalState.value.previewingVirtualCopy ?: return
        
        viewModelScope.launch {
            _internalState.update { it.copy(isSavingToFile = true) }
            try {
                val savedUri = imageSaver.saveWithCrop(photoToSave)
                if (savedUri != null) {
                    _internalState.update { 
                        it.copy(
                            isSavingToFile = false,
                            savedImageUri = savedUri,
                            message = "图片已保存到相册 PicZen 文件夹"
                        )
                    }
                } else {
                    _internalState.update { 
                        it.copy(isSavingToFile = false, error = "保存失败，请检查存储权限")
                    }
                }
            } catch (e: Exception) {
                _internalState.update { 
                    it.copy(isSavingToFile = false, error = "保存失败: ${e.message}")
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
    
    fun clearSavedImageUri() {
        _internalState.update { it.copy(savedImageUri = null) }
    }
}
