package com.example.photozen.ui.screens.photolist

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.TagDao
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sort order for photos in list.
 */
enum class PhotoListSortOrder(val displayName: String) {
    DATE_DESC("时间倒序"),  // Newest first (default)
    DATE_ASC("时间正序"),   // Oldest first
    RANDOM("随机排序")      // Random shuffle
}

data class PhotoListUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val status: PhotoStatus = PhotoStatus.UNSORTED,
    val isLoading: Boolean = true,
    val message: String? = null,
    val defaultExternalApp: String? = null,
    val untaggedCount: Int = 0,
    val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC
)

private data class InternalState(
    val isLoading: Boolean = true,
    val message: String? = null,
    val defaultExternalApp: String? = null,
    val untaggedCount: Int = 0,
    val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC
)

@HiltViewModel
class PhotoListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val photoDao: PhotoDao,
    private val tagDao: TagDao
) : ViewModel() {
    
    private val statusName: String = savedStateHandle.get<String>("statusName") ?: "UNSORTED"
    private val status: PhotoStatus = try {
        PhotoStatus.valueOf(statusName)
    } catch (e: Exception) {
        PhotoStatus.UNSORTED
    }
    
    private val _internalState = MutableStateFlow(InternalState())
    
    // Random seed for consistent random sorting
    private var randomSeed = System.currentTimeMillis()
    
    val uiState: StateFlow<PhotoListUiState> = combine(
        getPhotosUseCase.getPhotosByStatus(status),
        _internalState
    ) { photos, internal ->
        // Apply sorting based on dateAdded (creation time)
        val sortedPhotos = applySortOrder(photos, internal.sortOrder)
        PhotoListUiState(
            photos = sortedPhotos,
            status = status,
            isLoading = internal.isLoading && photos.isEmpty(),
            message = internal.message,
            defaultExternalApp = internal.defaultExternalApp,
            untaggedCount = internal.untaggedCount,
            sortOrder = internal.sortOrder
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
        // Count untagged photos for KEEP status
        if (status == PhotoStatus.KEEP) {
            viewModelScope.launch {
                getPhotosUseCase.getPhotosByStatus(PhotoStatus.KEEP).collect { photos ->
                    var untaggedCount = 0
                    for (photo in photos) {
                        if (!tagDao.photoHasAnyTag(photo.id)) {
                            untaggedCount++
                        }
                    }
                    _internalState.update { it.copy(untaggedCount = untaggedCount) }
                }
            }
        }
    }
    
    /**
     * Apply sort order to photos list.
     * Uses dateAdded (creation time) for sorting, NOT dateModified.
     */
    private fun applySortOrder(photos: List<PhotoEntity>, sortOrder: PhotoListSortOrder): List<PhotoEntity> {
        return when (sortOrder) {
            // Sort by dateAdded (creation time in seconds), convert to comparable value
            PhotoListSortOrder.DATE_DESC -> photos.sortedByDescending { it.dateAdded }
            PhotoListSortOrder.DATE_ASC -> photos.sortedBy { it.dateAdded }
            PhotoListSortOrder.RANDOM -> photos.shuffled(kotlin.random.Random(randomSeed))
        }
    }
    
    /**
     * Set sort order.
     */
    fun setSortOrder(order: PhotoListSortOrder) {
        if (order == PhotoListSortOrder.RANDOM) {
            randomSeed = System.currentTimeMillis()
        }
        _internalState.update { it.copy(sortOrder = order) }
    }
    
    /**
     * Cycle through sort orders.
     */
    fun cycleSortOrder() {
        val nextOrder = when (_internalState.value.sortOrder) {
            PhotoListSortOrder.DATE_DESC -> PhotoListSortOrder.DATE_ASC
            PhotoListSortOrder.DATE_ASC -> PhotoListSortOrder.RANDOM
            PhotoListSortOrder.RANDOM -> PhotoListSortOrder.DATE_DESC
        }
        setSortOrder(nextOrder)
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
