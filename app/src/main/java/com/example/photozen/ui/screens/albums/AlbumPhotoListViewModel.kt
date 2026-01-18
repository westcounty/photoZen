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
import com.example.photozen.data.repository.CustomFilterSession
import com.example.photozen.data.repository.PhotoFilterMode
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
    val allPhotos: List<PhotoEntity> = emptyList(),  // Unfiltered photos
    val totalCount: Int = 0,
    val sortedCount: Int = 0,
    val viewMode: AlbumPhotoListViewMode = AlbumPhotoListViewMode.GRID_2,
    val isLoading: Boolean = true,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val pendingOperation: PendingAlbumOperation? = null,
    val pendingDeleteIds: List<String> = emptyList(),  // Track IDs pending deletion for Room sync
    val error: String? = null,
    val message: String? = null,
    // Phase 7.2: Global status filter
    val statusFilter: Set<PhotoStatus> = PhotoStatus.entries.toSet()
) {
    val selectedCount: Int get() = selectedIds.size
    val unsortedCount: Int get() = totalCount - sortedCount
    val sortedPercentage: Float get() = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
    val isFilterActive: Boolean get() = statusFilter.size < PhotoStatus.entries.size
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
                photoDao.getPhotosByBucketId(bucketId).collect { allPhotos ->
                    val sortedCount = allPhotos.count { it.status != PhotoStatus.UNSORTED }
                    val statusFilter = _uiState.value.statusFilter
                    // Apply status filter
                    val filteredPhotos = if (statusFilter.size == PhotoStatus.entries.size) {
                        allPhotos
                    } else {
                        allPhotos.filter { it.status in statusFilter }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            photos = filteredPhotos,
                            allPhotos = allPhotos,
                            totalCount = allPhotos.size,
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
        // Phase 7.2: Load status filter preference
        viewModelScope.launch {
            preferencesRepository.getAlbumPhotoStatusFilter().collect { filter ->
                _uiState.update { currentState ->
                    // Re-apply filter to photos
                    val filteredPhotos = if (filter.size == PhotoStatus.entries.size) {
                        currentState.allPhotos
                    } else {
                        currentState.allPhotos.filter { it.status in filter }
                    }
                    currentState.copy(
                        statusFilter = filter,
                        photos = filteredPhotos
                    )
                }
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
            val movedPhotoIds = mutableListOf<String>()
            
            // Get target album path once before loop
            val targetAlbum = _uiState.value.albumBubbleList.find { it.bucketId == targetBucketId }
            // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
            val targetPath = mediaStoreDataSource.getAlbumPath(targetBucketId)
                ?: "Pictures/${targetAlbum?.displayName ?: "PhotoZen"}"
            
            for (photo in selectedPhotos) {
                val photoUri = Uri.parse(photo.systemUri)
                
                when (val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)) {
                    is MovePhotoResult.Success -> {
                        successCount++
                        movedPhotoIds.add(photo.id)
                    }
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
                // Sync Room database: update bucket_id for moved photos
                // This ensures the photo list updates immediately without needing app restart
                for (photoId in movedPhotoIds) {
                    photoDao.updateBucketId(photoId, targetBucketId)
                }
                
                _uiState.update { it.copy(message = "已移动 $successCount 张照片") }
                clearSelection()
                // Note: Room Flow will automatically update UI since we updated the database
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
     * Also saves the IDs for later Room database sync.
     */
    fun getSelectedPhotoUris(): List<Uri> {
        val selectedPhotos = _uiState.value.photos.filter { it.id in _uiState.value.selectedIds }
        
        // Save the IDs for deletion from Room after user confirms
        _uiState.update { it.copy(pendingDeleteIds = selectedPhotos.map { photo -> photo.id }) }
        
        return selectedPhotos.map { Uri.parse(it.systemUri) }
    }
    
    /**
     * Handle delete confirmation result.
     * Syncs Room database by removing deleted photo records.
     */
    fun onDeleteConfirmed() {
        viewModelScope.launch {
            val pendingDeleteIds = _uiState.value.pendingDeleteIds
            
            if (pendingDeleteIds.isNotEmpty()) {
                // Delete from Room database to ensure immediate UI update
                photoDao.deleteByIds(pendingDeleteIds)
            }
            
            // Clear pending state
            _uiState.update { 
                it.copy(
                    pendingDeleteIds = emptyList(),
                    message = "照片已删除"
                ) 
            }
            clearSelection()
            // Note: Room Flow will automatically update UI since we deleted from database
        }
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
    
    /**
     * Set custom filter session with specific photo IDs and prepare for navigation.
     * This allows "从此张开始筛选" feature to work by filtering only the specified photos.
     */
    fun setFilterSessionAndNavigate(photoIds: List<String>) {
        viewModelScope.launch {
            // Set custom filter session with specific photo IDs
            preferencesRepository.setSessionCustomFilter(
                CustomFilterSession(
                    photoIds = photoIds,
                    preciseMode = true
                )
            )
            
            // Set filter mode to CUSTOM
            preferencesRepository.setPhotoFilterMode(PhotoFilterMode.CUSTOM)
        }
    }
    
    // ==================== Phase 7.2: Status Filter ====================
    
    /**
     * Toggle a status in the filter.
     */
    fun toggleStatusFilter(status: PhotoStatus) {
        viewModelScope.launch {
            val currentFilter = _uiState.value.statusFilter
            val newFilter = if (status in currentFilter) {
                // Don't allow removing the last status
                if (currentFilter.size > 1) {
                    currentFilter - status
                } else {
                    currentFilter
                }
            } else {
                currentFilter + status
            }
            preferencesRepository.setAlbumPhotoStatusFilter(newFilter)
        }
    }
    
    /**
     * Select all statuses (reset filter).
     */
    fun selectAllStatuses() {
        viewModelScope.launch {
            preferencesRepository.setAlbumPhotoStatusFilter(PhotoStatus.entries.toSet())
        }
    }
}
