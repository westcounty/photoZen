package com.example.photozen.ui.screens.albums

import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.AlbumAddAction
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.MovePhotoResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View mode for album photo list.
 */
enum class AlbumPhotoListViewMode {
    GRID_1,  // 1-column grid (single column, waterfall style)
    GRID_2,  // 2-column grid
    GRID_3,  // 3-column grid
    GRID_4   // 4-column grid
}

/**
 * Pending operation that requires user confirmation.
 */
data class PendingAlbumOperation(
    val intentSender: IntentSender,
    val photoUri: Uri,
    val targetBucketId: String,
    val operationType: String  // "move" or "delete"
)

/**
 * UI State for Album Photo List screen.
 */
data class AlbumPhotoListUiState(
    val bucketId: String = "",
    val albumName: String = "",
    val photos: List<PhotoEntity> = emptyList(),
    val totalCount: Int = 0,
    val sortedCount: Int = 0,
    val viewMode: AlbumPhotoListViewMode = AlbumPhotoListViewMode.GRID_2,
    val isLoading: Boolean = true,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val pendingOperation: PendingAlbumOperation? = null,
    val error: String? = null,
    val message: String? = null
) {
    val selectedCount: Int get() = selectedIds.size
    val unsortedCount: Int get() = totalCount - sortedCount
    val sortedPercentage: Float get() = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
}

/**
 * ViewModel for Album Photo List screen.
 */
@HiltViewModel
class AlbumPhotoListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoDao: PhotoDao,
    private val albumBubbleDao: AlbumBubbleDao,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AlbumPhotoListUiState())
    val uiState: StateFlow<AlbumPhotoListUiState> = _uiState.asStateFlow()
    
    private var loadJob: Job? = null
    
    /**
     * Initialize with album info.
     */
    fun initialize(bucketId: String, albumName: String) {
        _uiState.update { it.copy(bucketId = bucketId, albumName = albumName) }
        loadPhotos()
        loadAlbumBubbleList()
        loadSettings()
    }
    
    /**
     * Load photos from the album.
     */
    private fun loadPhotos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val bucketId = _uiState.value.bucketId
            
            // Guard: Don't load if bucketId is empty (not initialized yet)
            if (bucketId.isEmpty()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                photoDao.getPhotosByBucketId(bucketId).collect { photos ->
                    val sortedCount = photos.count { it.status != PhotoStatus.UNSORTED }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            photos = photos,
                            totalCount = photos.size,
                            sortedCount = sortedCount,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载照片失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Load album bubble list for target selection.
     */
    private fun loadAlbumBubbleList() {
        viewModelScope.launch {
            try {
                albumBubbleDao.getAll().collect { albums ->
                    _uiState.update { it.copy(albumBubbleList = albums) }
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }
    
    /**
     * Load settings.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            preferencesRepository.getAlbumAddAction().collect { action ->
                _uiState.update { it.copy(albumAddAction = action) }
            }
        }
    }
    
    /**
     * Refresh photos.
     */
    fun refresh() {
        loadPhotos()
    }
    
    /**
     * Toggle view mode (1 -> 2 -> 3 -> 4 -> 1).
     */
    fun cycleViewMode() {
        _uiState.update {
            it.copy(
                viewMode = when (it.viewMode) {
                    AlbumPhotoListViewMode.GRID_1 -> AlbumPhotoListViewMode.GRID_2
                    AlbumPhotoListViewMode.GRID_2 -> AlbumPhotoListViewMode.GRID_3
                    AlbumPhotoListViewMode.GRID_3 -> AlbumPhotoListViewMode.GRID_4
                    AlbumPhotoListViewMode.GRID_4 -> AlbumPhotoListViewMode.GRID_1
                }
            )
        }
    }
    
    /**
     * Enter selection mode.
     */
    fun enterSelectionMode(photoId: String) {
        _uiState.update {
            it.copy(
                isSelectionMode = true,
                selectedIds = setOf(photoId)
            )
        }
    }
    
    /**
     * Toggle photo selection.
     */
    fun togglePhotoSelection(photoId: String) {
        _uiState.update { state ->
            val newSelection = if (photoId in state.selectedIds) {
                state.selectedIds - photoId
            } else {
                state.selectedIds + photoId
            }
            
            state.copy(
                selectedIds = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }
    
    /**
     * Select all photos.
     */
    fun selectAll() {
        _uiState.update { state ->
            state.copy(
                isSelectionMode = true,
                selectedIds = state.photos.map { it.id }.toSet()
            )
        }
    }
    
    /**
     * Clear selection.
     */
    fun clearSelection() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedIds = emptySet()
            )
        }
    }
    
    /**
     * Move selected photos to another album.
     */
    fun moveSelectedToAlbum(targetBucketId: String) {
        viewModelScope.launch {
            val selectedPhotos = _uiState.value.photos.filter { it.id in _uiState.value.selectedIds }
            var successCount = 0
            var needsConfirmation = false
            
            // Get target album path once before loop
            val targetAlbum = _uiState.value.albumBubbleList.find { it.bucketId == targetBucketId }
            // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
            val targetPath = mediaStoreDataSource.getAlbumPath(targetBucketId)
                ?: "Pictures/${targetAlbum?.displayName ?: "PhotoZen"}"
            
            for (photo in selectedPhotos) {
                val photoUri = Uri.parse(photo.systemUri)
                
                when (val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)) {
                    is MovePhotoResult.Success -> successCount++
                    is MovePhotoResult.NeedsConfirmation -> {
                        needsConfirmation = true
                        _uiState.update {
                            it.copy(
                                pendingOperation = PendingAlbumOperation(
                                    intentSender = result.intentSender,
                                    photoUri = photoUri,
                                    targetBucketId = targetBucketId,
                                    operationType = "move"
                                )
                            )
                        }
                        break // Handle one at a time
                    }
                    is MovePhotoResult.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }
                }
            }
            
            if (successCount > 0) {
                _uiState.update { it.copy(message = "已移动 $successCount 张照片") }
                clearSelection()
                refresh()
            }
        }
    }
    
    /**
     * Copy selected photos to another album.
     */
    fun copySelectedToAlbum(targetBucketId: String) {
        viewModelScope.launch {
            val selectedPhotos = _uiState.value.photos.filter { it.id in _uiState.value.selectedIds }
            var successCount = 0
            
            // Get target album path once before loop
            val targetAlbum = _uiState.value.albumBubbleList.find { it.bucketId == targetBucketId }
            // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
            val targetPath = mediaStoreDataSource.getAlbumPath(targetBucketId)
                ?: "Pictures/${targetAlbum?.displayName ?: "PhotoZen"}"
            
            for (photo in selectedPhotos) {
                val photoUri = Uri.parse(photo.systemUri)
                
                val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                if (result.isSuccess) {
                    successCount++
                }
            }
            
            if (successCount > 0) {
                _uiState.update { it.copy(message = "已复制 $successCount 张照片") }
                clearSelection()
            }
        }
    }
    
    /**
     * Check if there are selected photos to delete.
     */
    fun hasSelectedPhotos(): Boolean {
        return _uiState.value.selectedIds.isNotEmpty()
    }
    
    /**
     * Get URIs of selected photos for delete request.
     */
    fun getSelectedPhotoUris(): List<Uri> {
        return _uiState.value.photos
            .filter { it.id in _uiState.value.selectedIds }
            .map { Uri.parse(it.systemUri) }
    }
    
    /**
     * Handle delete confirmation result.
     */
    fun onDeleteConfirmed() {
        clearSelection()
        refresh()
        _uiState.update { it.copy(message = "照片已删除") }
    }
    
    /**
     * Clear pending operation.
     */
    fun clearPendingOperation() {
        _uiState.update { it.copy(pendingOperation = null) }
    }
    
    /**
     * Clear error.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Clear message.
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
