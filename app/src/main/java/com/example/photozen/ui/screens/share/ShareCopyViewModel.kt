package com.example.photozen.ui.screens.share

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Share Copy screen.
 */
data class ShareCopyUiState(
    val photoUris: List<Uri> = emptyList(),
    val albums: List<AlbumBubbleEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isCopying: Boolean = false,
    val showAlbumPicker: Boolean = false,
    val copySuccess: Boolean = false,
    val copiedCount: Int = 0,
    val totalCount: Int = 0,
    val targetAlbumName: String = "",
    val error: String? = null
)

/**
 * Internal state holder for ShareCopyViewModel.
 */
private data class CopyInternalState(
    val photoUris: List<Uri> = emptyList(),
    val isLoading: Boolean = true,
    val isCopying: Boolean = false,
    val showAlbumPicker: Boolean = false,
    val copySuccess: Boolean = false,
    val copiedCount: Int = 0,
    val targetAlbumName: String = "",
    val error: String? = null
)

/**
 * ViewModel for Share Copy screen.
 * Handles copying shared photos from external apps to a selected album.
 */
@HiltViewModel
class ShareCopyViewModel @Inject constructor(
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val albumBubbleDao: AlbumBubbleDao,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _internalState = MutableStateFlow(CopyInternalState())
    
    val uiState: StateFlow<ShareCopyUiState> = combine(
        _internalState,
        albumBubbleDao.getAll()
    ) { internal: CopyInternalState, albums: List<AlbumBubbleEntity> ->
        ShareCopyUiState(
            photoUris = internal.photoUris,
            albums = albums,
            isLoading = internal.isLoading,
            isCopying = internal.isCopying,
            showAlbumPicker = internal.showAlbumPicker,
            copySuccess = internal.copySuccess,
            copiedCount = internal.copiedCount,
            totalCount = internal.photoUris.size,
            targetAlbumName = internal.targetAlbumName,
            error = internal.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShareCopyUiState()
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
        
        _internalState.update { 
            it.copy(photoUris = uris, isLoading = false) 
        }
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
     * Copy all photos to the selected album.
     */
    fun copyToAlbum(album: AlbumBubbleEntity) {
        val uris = _internalState.value.photoUris
        if (uris.isEmpty()) return
        
        _internalState.update { 
            it.copy(
                showAlbumPicker = false, 
                isCopying = true,
                targetAlbumName = album.displayName
            ) 
        }
        
        viewModelScope.launch {
            var successCount = 0
            
            for (uri in uris) {
                try {
                    // Build album path from album name
                    // Most albums are under Pictures or DCIM
                    val albumPath = "Pictures/${album.displayName}"
                    
                    val result = albumOperationsUseCase.copyPhotoToAlbum(uri, albumPath)
                    if (result.isSuccess) {
                        successCount++
                    }
                } catch (e: Exception) {
                    // Continue with other photos even if one fails
                }
            }
            
            // Trigger album refresh if any photos were copied successfully
            if (successCount > 0) {
                preferencesRepository.triggerAlbumRefresh()
            }

            _internalState.update {
                it.copy(
                    isCopying = false,
                    copySuccess = successCount > 0,
                    copiedCount = successCount,
                    error = if (successCount == 0) "复制失败" else null
                )
            }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _internalState.update { it.copy(error = null) }
    }
}
