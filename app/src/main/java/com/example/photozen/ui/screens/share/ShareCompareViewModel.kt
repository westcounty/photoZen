package com.example.photozen.ui.screens.share

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * External photo data with dimensions for smart layout.
 */
data class ExternalPhoto(
    val uri: Uri,
    val width: Int,
    val height: Int
) {
    val aspectRatio: Float get() = if (height > 0) width.toFloat() / height else 1f
}

/**
 * Validation result for photo count.
 */
sealed class CompareValidation {
    data object Valid : CompareValidation()
    data class TooFew(val count: Int) : CompareValidation()
    data class TooMany(val count: Int) : CompareValidation()
}

/**
 * UI State for Share Compare screen.
 */
data class ShareCompareUiState(
    val photos: List<ExternalPhoto> = emptyList(),
    val selectedUris: Set<Uri> = emptySet(),
    val albums: List<AlbumBubbleEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isCopying: Boolean = false,
    val syncZoomEnabled: Boolean = true,
    val showAlbumPicker: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isEmpty: Boolean = false,
    val validation: CompareValidation = CompareValidation.Valid,
    val snackbarMessage: String? = null
) {
    val isValid: Boolean
        get() = validation is CompareValidation.Valid
    
    val errorMessage: String?
        get() = when (validation) {
            is CompareValidation.TooFew -> "对比至少需要 2 张照片"
            is CompareValidation.TooMany -> "对比最多支持 6 张照片"
            is CompareValidation.Valid -> null
        }
    
    val hasSelection: Boolean get() = selectedUris.isNotEmpty()
    val selectionCount: Int get() = selectedUris.size
    val isAllSelected: Boolean get() = selectedUris.size == photos.size && photos.isNotEmpty()
}

/**
 * Internal state holder for ShareCompareViewModel.
 */
private data class CompareInternalState(
    val photos: List<ExternalPhoto> = emptyList(),
    val selectedUris: Set<Uri> = emptySet(),
    val isLoading: Boolean = true,
    val isCopying: Boolean = false,
    val syncZoomEnabled: Boolean = true,
    val showAlbumPicker: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isEmpty: Boolean = false,
    val validation: CompareValidation = CompareValidation.Valid,
    val snackbarMessage: String? = null
)

/**
 * ViewModel for Share Compare screen.
 * Handles comparing shared photos from external apps with selection,
 * copy to album, and system delete capabilities.
 */
@HiltViewModel
class ShareCompareViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val albumBubbleDao: AlbumBubbleDao
) : ViewModel() {
    
    companion object {
        const val MIN_PHOTOS = 2
        const val MAX_PHOTOS = 6
    }
    
    private val _internalState = MutableStateFlow(CompareInternalState())
    
    val uiState: StateFlow<ShareCompareUiState> = combine(
        _internalState,
        albumBubbleDao.getAll()
    ) { internal: CompareInternalState, albums: List<AlbumBubbleEntity> ->
        ShareCompareUiState(
            photos = internal.photos,
            selectedUris = internal.selectedUris,
            albums = albums,
            isLoading = internal.isLoading,
            isCopying = internal.isCopying,
            syncZoomEnabled = internal.syncZoomEnabled,
            showAlbumPicker = internal.showAlbumPicker,
            showDeleteConfirm = internal.showDeleteConfirm,
            isEmpty = internal.isEmpty,
            validation = internal.validation,
            snackbarMessage = internal.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShareCompareUiState()
    )
    
    /**
     * Initialize with URIs from share intent.
     */
    fun setPhotoUris(urisJson: String) {
        val uris = urisJson.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { 
                try {
                    Uri.parse(it)
                } catch (e: Exception) {
                    null
                }
            }
        
        val validation = when {
            uris.size < MIN_PHOTOS -> CompareValidation.TooFew(uris.size)
            uris.size > MAX_PHOTOS -> CompareValidation.TooMany(uris.size)
            else -> CompareValidation.Valid
        }
        
        // Load photo dimensions in background
        viewModelScope.launch {
            val photos = loadPhotoSizes(uris)
            _internalState.update { 
                it.copy(
                    photos = photos,
                    isLoading = false,
                    validation = validation
                ) 
            }
        }
    }
    
    /**
     * Load photo dimensions from URIs.
     */
    private suspend fun loadPhotoSizes(uris: List<Uri>): List<ExternalPhoto> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { 
                        inJustDecodeBounds = true 
                    }
                    BitmapFactory.decodeStream(stream, null, options)
                    ExternalPhoto(uri, options.outWidth, options.outHeight)
                }
            } catch (e: Exception) {
                // If we can't get dimensions, use default aspect ratio
                ExternalPhoto(uri, 1, 1)
            }
        }
    }
    
    /**
     * Toggle sync zoom mode.
     */
    fun toggleSyncZoom() {
        _internalState.update { it.copy(syncZoomEnabled = !it.syncZoomEnabled) }
    }
    
    /**
     * Toggle selection for a photo.
     */
    fun toggleSelection(uri: Uri) {
        _internalState.update { state ->
            val newSelection = if (uri in state.selectedUris) {
                state.selectedUris - uri
            } else {
                state.selectedUris + uri
            }
            state.copy(selectedUris = newSelection)
        }
    }
    
    /**
     * Select or deselect all photos.
     */
    fun toggleSelectAll() {
        _internalState.update { state ->
            val newSelection = if (state.selectedUris.size == state.photos.size) {
                emptySet()
            } else {
                state.photos.map { it.uri }.toSet()
            }
            state.copy(selectedUris = newSelection)
        }
    }
    
    /**
     * Clear all selections.
     */
    fun clearSelection() {
        _internalState.update { it.copy(selectedUris = emptySet()) }
    }
    
    /**
     * Show album picker dialog.
     */
    fun showAlbumPicker() {
        _internalState.update { it.copy(showAlbumPicker = true) }
    }
    
    /**
     * Hide album picker dialog.
     */
    fun hideAlbumPicker() {
        _internalState.update { it.copy(showAlbumPicker = false) }
    }
    
    /**
     * Show delete confirmation dialog.
     */
    fun showDeleteConfirm() {
        _internalState.update { it.copy(showDeleteConfirm = true) }
    }
    
    /**
     * Hide delete confirmation dialog.
     */
    fun hideDeleteConfirm() {
        _internalState.update { it.copy(showDeleteConfirm = false) }
    }
    
    /**
     * Copy selected photos to the specified album.
     */
    fun copyToAlbum(album: AlbumBubbleEntity) {
        val selectedUris = _internalState.value.selectedUris
        if (selectedUris.isEmpty()) return
        
        _internalState.update { 
            it.copy(showAlbumPicker = false, isCopying = true) 
        }
        
        viewModelScope.launch {
            var successCount = 0
            val albumPath = "Pictures/${album.displayName}"
            
            for (uri in selectedUris) {
                try {
                    val result = albumOperationsUseCase.copyPhotoToAlbum(uri, albumPath)
                    if (result.isSuccess) {
                        successCount++
                    }
                } catch (e: Exception) {
                    // Continue with other photos
                }
            }
            
            _internalState.update { 
                it.copy(
                    isCopying = false,
                    selectedUris = emptySet(),
                    snackbarMessage = if (successCount > 0) {
                        "已复制 $successCount 张照片到「${album.displayName}」"
                    } else {
                        "复制失败"
                    }
                ) 
            }
        }
    }
    
    /**
     * Remove photos from the list after successful deletion.
     * Called after system delete confirmation.
     */
    fun onPhotosDeleted(deletedUris: Set<Uri>) {
        _internalState.update { state ->
            val remainingPhotos = state.photos.filter { it.uri !in deletedUris }
            val remainingSelection = state.selectedUris - deletedUris
            
            state.copy(
                photos = remainingPhotos,
                selectedUris = remainingSelection,
                showDeleteConfirm = false,
                isEmpty = remainingPhotos.isEmpty(),
                snackbarMessage = "已删除 ${deletedUris.size} 张照片"
            )
        }
    }
    
    /**
     * Clear snackbar message.
     */
    fun clearSnackbar() {
        _internalState.update { it.copy(snackbarMessage = null) }
    }
}
