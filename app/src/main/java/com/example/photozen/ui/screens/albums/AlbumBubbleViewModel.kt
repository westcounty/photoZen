package com.example.photozen.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.source.Album
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.AlbumStats
import com.example.photozen.ui.components.bubble.BubbleNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Data class representing an album with its stats for the bubble view.
 */
data class AlbumBubbleData(
    val bucketId: String,
    val displayName: String,
    val totalCount: Int,
    val sortedCount: Int,
    val coverUri: String? = null
) {
    val unsortedCount: Int get() = totalCount - sortedCount
    val sortedPercentage: Float get() = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
}

/**
 * UI State for the Album Bubble screen.
 */
data class AlbumBubbleUiState(
    val isLoading: Boolean = true,
    val albums: List<AlbumBubbleData> = emptyList(),
    val bubbleNodes: List<BubbleNode> = emptyList(),
    val viewMode: AlbumViewMode = AlbumViewMode.BUBBLE,
    val showAlbumPicker: Boolean = false,
    val availableAlbums: List<Album> = emptyList(),
    val isLoadingAlbums: Boolean = false,
    val selectedAlbumIds: Set<String> = emptySet(),
    val error: String? = null,
    val message: String? = null,
    // For undo remove operation
    val lastRemovedAlbum: AlbumBubbleEntity? = null,
    val showUndoSnackbar: Boolean = false
)

/**
 * View mode for album display.
 */
enum class AlbumViewMode {
    BUBBLE,  // Bubble/tag cloud view
    LIST     // List view
}

/**
 * ViewModel for the Album Bubble screen.
 */
@HiltViewModel
class AlbumBubbleViewModel @Inject constructor(
    private val albumBubbleDao: AlbumBubbleDao,
    private val photoDao: PhotoDao,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val preferencesRepository: com.example.photozen.data.repository.PreferencesRepository
) : ViewModel() {
    
    companion object {
        private const val MIN_BUBBLE_RADIUS = 80f
        private const val MAX_BUBBLE_RADIUS = 150f
    }
    
    private val _uiState = MutableStateFlow(AlbumBubbleUiState())
    val uiState: StateFlow<AlbumBubbleUiState> = _uiState.asStateFlow()
    
    private var loadJob: Job? = null
    
    init {
        // Load saved view mode
        viewModelScope.launch {
            preferencesRepository.getAlbumViewMode().collect { modeStr ->
                val mode = try {
                    AlbumViewMode.valueOf(modeStr)
                } catch (e: IllegalArgumentException) {
                    AlbumViewMode.BUBBLE
                }
                _uiState.update { it.copy(viewMode = mode) }
            }
        }
        loadAlbumBubbles()
    }
    
    /**
     * Load albums in the bubble list with their stats.
     * Uses MediaStore directly for photo counts to ensure fresh data after moves/copies.
     * 
     * IMPORTANT: Newly created albums have placeholder bucketIds (based on name.hashCode()).
     * We need to find the real bucketId from MediaStore by matching displayName.
     */
    fun loadAlbumBubbles() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Get albums from bubble list
                val bubbleAlbums = albumBubbleDao.getAllSync()
                
                // Get all system albums and create a lookup by bucketId
                // IMPORTANT: Use bucketId as key, NOT name, because multiple albums 
                // can have the same name (e.g., DCIM/Camera and Pictures/Camera)
                val allSystemAlbums = mediaStoreDataSource.getAllAlbums()
                val systemAlbumsById = allSystemAlbums.associateBy { it.id }
                
                // Get stats and cover for each album
                // Use MediaStore directly to get fresh photo counts (important after photo moves)
                val albumsWithStats = bubbleAlbums.map { album ->
                    try {
                        // The bucketId stored in database should already be correct
                        // (it comes from Album.id when added via addAlbumsToBubbleList)
                        val bucketId = album.bucketId
                        
                        // Verify the album still exists in system
                        val systemAlbum = systemAlbumsById[bucketId]
                        
                        // Get photos directly from MediaStore for accurate count
                        val photos = mediaStoreDataSource.getPhotosFromAlbum(bucketId)
                        // Get sorted count from database using the photo IDs
                        val photoIds = photos.map { it.id }
                        val sortedCount = if (photoIds.isNotEmpty()) {
                            val dbPhotos = photoDao.getPhotosByIds(photoIds)
                            dbPhotos.count { it.status != PhotoStatus.UNSORTED }
                        } else {
                            0
                        }
                        val coverUri = photos.maxByOrNull { it.dateAdded }?.systemUri
                        
                        AlbumBubbleData(
                            bucketId = bucketId,
                            displayName = systemAlbum?.name ?: album.displayName,  // Use system name if available
                            totalCount = photos.size,
                            sortedCount = sortedCount,
                            coverUri = coverUri
                        )
                    } catch (e: Exception) {
                        // Fallback to empty data if album can't be loaded
                        AlbumBubbleData(
                            bucketId = album.bucketId,
                            displayName = album.displayName,
                            totalCount = 0,
                            sortedCount = 0,
                            coverUri = null
                        )
                    }
                }
                
                // Create bubble nodes
                val bubbleNodes = createBubbleNodes(albumsWithStats)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        albums = albumsWithStats,
                        bubbleNodes = bubbleNodes,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "加载相册失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Create bubble nodes from album data.
     */
    private fun createBubbleNodes(albums: List<AlbumBubbleData>): List<BubbleNode> {
        if (albums.isEmpty()) return emptyList()
        
        val maxCount = albums.maxOfOrNull { it.totalCount } ?: 1
        
        return albums.map { album ->
            // Size based on total photo count
            val sizeRatio = if (maxCount > 0) {
                sqrt(album.totalCount.toFloat() / maxCount)
            } else {
                0.5f
            }
            val radius = MIN_BUBBLE_RADIUS + (MAX_BUBBLE_RADIUS - MIN_BUBBLE_RADIUS) * sizeRatio
            
            BubbleNode(
                id = album.bucketId,
                label = album.displayName,
                photoCount = album.totalCount,
                radius = radius,
                // Color based on sorted percentage (red = low, blue = high)
                // Convert hue to ARGB color
                color = android.graphics.Color.HSVToColor(
                    floatArrayOf(album.sortedPercentage * 240f, 0.7f, 0.8f)
                )
            )
        }
    }
    
    /**
     * Toggle view mode between bubble and list.
     * Saves the new mode to preferences.
     */
    fun toggleViewMode() {
        val newMode = if (_uiState.value.viewMode == AlbumViewMode.BUBBLE) AlbumViewMode.LIST else AlbumViewMode.BUBBLE
        _uiState.update { it.copy(viewMode = newMode) }
        
        // Save to preferences
        viewModelScope.launch {
            preferencesRepository.setAlbumViewMode(newMode.name)
        }
    }
    
    /**
     * Show the album picker to add albums to bubble list.
     */
    fun showAlbumPicker() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAlbums = true, showAlbumPicker = true) }
            
            try {
                val allAlbums = mediaStoreDataSource.getAllAlbums()
                val existingIds = albumBubbleDao.getAllBucketIds().toSet()
                
                _uiState.update { 
                    it.copy(
                        isLoadingAlbums = false,
                        availableAlbums = allAlbums,
                        selectedAlbumIds = existingIds
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingAlbums = false,
                        error = "加载系统相册失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Hide the album picker.
     */
    fun hideAlbumPicker() {
        _uiState.update { it.copy(showAlbumPicker = false) }
    }
    
    /**
     * Toggle album selection in the picker.
     */
    fun toggleAlbumSelection(albumId: String) {
        _uiState.update { state ->
            val newSelection = if (albumId in state.selectedAlbumIds) {
                state.selectedAlbumIds - albumId
            } else {
                state.selectedAlbumIds + albumId
            }
            state.copy(selectedAlbumIds = newSelection)
        }
    }
    
    /**
     * Confirm album selection and update the bubble list.
     */
    fun confirmAlbumSelection() {
        viewModelScope.launch {
            try {
                val selectedIds = _uiState.value.selectedAlbumIds
                val availableAlbums = _uiState.value.availableAlbums
                val existingIds = albumBubbleDao.getAllBucketIds().toSet()
                
                // Add new albums
                val toAdd = selectedIds - existingIds
                val albumsToAdd = availableAlbums.filter { it.id in toAdd }
                albumOperationsUseCase.addAlbumsToBubbleList(albumsToAdd)
                
                // Remove deselected albums
                val toRemove = existingIds - selectedIds
                toRemove.forEach { albumBubbleDao.deleteByBucketId(it) }
                
                hideAlbumPicker()
                loadAlbumBubbles()
                
                _uiState.update { it.copy(message = "相册列表已更新") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "更新相册列表失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Remove an album from the bubble list with undo support.
     */
    fun removeAlbumFromList(bucketId: String) {
        viewModelScope.launch {
            try {
                // Save the album for undo before deleting
                val albumEntity = albumBubbleDao.getByBucketId(bucketId)
                albumBubbleDao.deleteByBucketId(bucketId)
                loadAlbumBubbles()
                _uiState.update { 
                    it.copy(
                        lastRemovedAlbum = albumEntity,
                        showUndoSnackbar = true,
                        message = "已从列表移除"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "移除失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Undo the last album removal.
     */
    fun undoRemoveAlbum() {
        viewModelScope.launch {
            val album = _uiState.value.lastRemovedAlbum ?: return@launch
            try {
                albumBubbleDao.insert(album)
                loadAlbumBubbles()
                _uiState.update { 
                    it.copy(
                        lastRemovedAlbum = null,
                        showUndoSnackbar = false,
                        message = "已恢复「${album.displayName}」"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "恢复失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Clear the undo snackbar state.
     */
    fun clearUndoState() {
        _uiState.update { it.copy(lastRemovedAlbum = null, showUndoSnackbar = false) }
    }
    
    /**
     * Reorder albums by moving an item from one position to another.
     */
    fun reorderAlbums(fromIndex: Int, toIndex: Int) {
        val currentAlbums = _uiState.value.albums.toMutableList()
        if (fromIndex !in currentAlbums.indices || toIndex !in currentAlbums.indices) return
        
        // Swap in the local list
        val item = currentAlbums.removeAt(fromIndex)
        currentAlbums.add(toIndex, item)
        
        // Update UI state immediately for responsiveness
        _uiState.update { it.copy(albums = currentAlbums) }
        
        // Persist the new order to database
        viewModelScope.launch {
            try {
                currentAlbums.forEachIndexed { index, album ->
                    albumBubbleDao.updateSortOrder(album.bucketId, index)
                }
            } catch (e: Exception) {
                // Reload if persisting fails
                loadAlbumBubbles()
            }
        }
    }
    
    /**
     * Apply a new order to albums (used after drag-and-drop).
     * @param newOrder list of bucketIds in the new order
     */
    fun applyNewAlbumOrder(newOrder: List<String>) {
        val currentAlbums = _uiState.value.albums
        val albumMap = currentAlbums.associateBy { it.bucketId }
        
        // Reorder albums based on newOrder
        val reorderedAlbums = newOrder.mapNotNull { bucketId -> albumMap[bucketId] }
        
        // Only update if order actually changed
        if (reorderedAlbums.map { it.bucketId } == currentAlbums.map { it.bucketId }) return
        
        // Update UI state immediately
        _uiState.update { it.copy(albums = reorderedAlbums) }
        
        // Persist the new order to database
        viewModelScope.launch {
            try {
                reorderedAlbums.forEachIndexed { index, album ->
                    albumBubbleDao.updateSortOrder(album.bucketId, index)
                }
            } catch (e: Exception) {
                // Reload if persisting fails
                loadAlbumBubbles()
            }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Clear info message.
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    /**
     * Show a message.
     */
    fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
    
    /**
     * Delete an album (physically delete from system).
     * Note: This initiates the delete process. The UI should handle the IntentSender.
     */
    fun deleteAlbum(bucketId: String) {
        viewModelScope.launch {
            try {
                // The deleteAlbum returns an IntentSender for system confirmation
                // For now, we just remove from the bubble list
                // Full deletion requires UI to handle the IntentSender
                albumBubbleDao.deleteByBucketId(bucketId)
                loadAlbumBubbles()
                _uiState.update { it.copy(message = "相册已从列表移除（完全删除需要系统权限确认）") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "操作失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Create a new album in the system and add it to the bubble list.
     * The newly created album will be selected and placed at the top of the list.
     */
    fun createAlbum(albumName: String) {
        viewModelScope.launch {
            try {
                // Create album in system storage
                val result = mediaStoreDataSource.createAlbum(albumName)
                
                if (result != null) {
                    val (bucketId, displayName) = result
                    
                    // Add to bubble list with sort order 0 (top of list)
                    val newAlbum = AlbumBubbleEntity(
                        bucketId = bucketId,
                        displayName = displayName,
                        sortOrder = 0
                    )
                    
                    // Shift existing albums down
                    val existingAlbums = albumBubbleDao.getAllSync()
                    existingAlbums.forEachIndexed { index, album ->
                        albumBubbleDao.updateSortOrder(album.bucketId, index + 1)
                    }
                    
                    // Insert the new album
                    albumBubbleDao.insert(newAlbum)
                    
                    // Add to selected IDs so it appears selected in the picker
                    _uiState.update { state ->
                        state.copy(
                            selectedAlbumIds = state.selectedAlbumIds + bucketId,
                            message = "已创建相册「$displayName」"
                        )
                    }
                    
                    // Refresh the available albums list
                    var allAlbums = mediaStoreDataSource.getAllAlbums()
                    
                    // IMPORTANT: New empty albums may not appear in MediaStore query
                    // Manually add the new album to the front if not present
                    if (allAlbums.none { it.id == bucketId }) {
                        val newAlbumEntry = Album(
                            id = bucketId,
                            name = displayName,
                            photoCount = 0,
                            coverUri = null
                        )
                        allAlbums = listOf(newAlbumEntry) + allAlbums
                    } else {
                        // Move the new album to the front of the list
                        val albumToMove = allAlbums.find { it.id == bucketId }
                        if (albumToMove != null) {
                            allAlbums = listOf(albumToMove) + allAlbums.filter { it.id != bucketId }
                        }
                    }
                    
                    _uiState.update { it.copy(availableAlbums = allAlbums) }
                    
                    // Reload bubble list
                    loadAlbumBubbles()
                } else {
                    _uiState.update { it.copy(error = "创建相册失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "创建相册失败: ${e.message}") }
            }
        }
    }
}
