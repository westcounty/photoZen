package com.example.photozen.ui.screens.photolist

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.SortPhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoListUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val status: PhotoStatus = PhotoStatus.UNSORTED,
    val isLoading: Boolean = true,
    val message: String? = null,
    val defaultExternalApp: String? = null
)

private data class InternalState(
    val isLoading: Boolean = true,
    val message: String? = null,
    val defaultExternalApp: String? = null
)

@HiltViewModel
class PhotoListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val photoDao: PhotoDao
) : ViewModel() {
    
    private val statusName: String = savedStateHandle.get<String>("statusName") ?: "UNSORTED"
    private val status: PhotoStatus = try {
        PhotoStatus.valueOf(statusName)
    } catch (e: Exception) {
        PhotoStatus.UNSORTED
    }
    
    private val _internalState = MutableStateFlow(InternalState())
    
    val uiState: StateFlow<PhotoListUiState> = combine(
        getPhotosUseCase.getPhotosByStatus(status),
        _internalState
    ) { photos, internal ->
        PhotoListUiState(
            photos = photos,
            status = status,
            isLoading = internal.isLoading && photos.isEmpty(),
            message = internal.message,
            defaultExternalApp = internal.defaultExternalApp
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
}
