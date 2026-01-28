package com.example.photozen.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.source.Album
import com.example.photozen.data.source.DeleteResult
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.AlbumStats
import com.example.photozen.ui.components.bubble.BubbleNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
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
    val isRefreshing: Boolean = false,  // For refresh button animation
    val albums: List<AlbumBubbleData> = emptyList(),
    val bubbleNodes: List<BubbleNode> = emptyList(),
    val viewMode: AlbumViewMode = AlbumViewMode.GRID,
    val showAlbumPicker: Boolean = false,
    val availableAlbums: List<Album> = emptyList(),
    val isLoadingAlbums: Boolean = false,
    val selectedAlbumIds: Set<String> = emptySet(),
    val error: String? = null,
    val message: String? = null,
    // For undo remove operation
    val lastRemovedAlbum: AlbumBubbleEntity? = null,
    val showUndoSnackbar: Boolean = false,
    // For album deletion via system API
    val pendingDeleteIntentSender: android.content.IntentSender? = null,
    val pendingDeleteBucketId: String? = null
)

/**
 * View mode for album display.
 */
enum class AlbumViewMode {
    GRID,    // Modern card grid view (replaces BUBBLE)
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
                    // Handle migration from old BUBBLE mode
                    if (modeStr == "BUBBLE") AlbumViewMode.GRID
                    else AlbumViewMode.valueOf(modeStr)
                } catch (e: IllegalArgumentException) {
                    AlbumViewMode.GRID
                }
                _uiState.update { it.copy(viewMode = mode) }
            }
        }
        // Listen for album refresh trigger (from FlowSorter when photos are added/moved)
        viewModelScope.launch {
            preferencesRepository.albumRefreshTrigger
                .drop(1)  // Skip initial value
                .collect {
                    refreshAlbumStats()
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

                // Get all system albums and create lookups
                val allSystemAlbums = mediaStoreDataSource.getAllAlbums()
                val systemAlbumsById = allSystemAlbums.associateBy { it.id }
                // Also create lookup by name for matching newly created albums
                val systemAlbumsByName = allSystemAlbums.groupBy { it.name }

                // Get stats and cover for each album
                // Use MediaStore directly to get fresh photo counts (important after photo moves)
                val albumsWithStats = bubbleAlbums.map { album ->
                    try {
                        var bucketId = album.bucketId
                        var systemAlbum = systemAlbumsById[bucketId]

                        // Check if this is a newly created album with relativePath as bucketId
                        // Format: "Pictures/AlbumName" or similar
                        if (systemAlbum == null && bucketId.contains("/")) {
                            // Extract the album name from relativePath
                            val albumName = bucketId.substringAfterLast("/")
                            // Try to find matching system album by name
                            val matchingAlbums = systemAlbumsByName[albumName]
                            if (!matchingAlbums.isNullOrEmpty()) {
                                // Found a match - use the real bucketId
                                systemAlbum = matchingAlbums.first()
                                val realBucketId = systemAlbum.id

                                // Update the database with the real bucketId
                                albumBubbleDao.deleteByBucketId(album.bucketId)
                                albumBubbleDao.insert(
                                    AlbumBubbleEntity(
                                        bucketId = realBucketId,
                                        displayName = album.displayName,
                                        sortOrder = album.sortOrder
                                    )
                                )
                                bucketId = realBucketId
                            }
                        }

                        // Get photos from MediaStore (真实的系统照片数量)
                        val mediaStorePhotos = mediaStoreDataSource.getPhotosFromAlbum(bucketId)
                        val mediaStoreIds = mediaStorePhotos.map { it.id }.toSet()

                        // Get existing photos in Room for this album
                        val roomPhotos = photoDao.getPhotosByBucketIdSync(bucketId)
                        val roomIds = roomPhotos.map { it.id }.toSet()

                        // Add missing photos to Room (MediaStore has, Room doesn't)
                        val missingPhotos = mediaStorePhotos.filter { it.id !in roomIds }
                        if (missingPhotos.isNotEmpty()) {
                            photoDao.insertAll(missingPhotos)
                        }

                        // Remove orphaned photos from Room (Room has, MediaStore doesn't)
                        // These are photos that were deleted or moved to other albums
                        val orphanedIds = roomIds - mediaStoreIds
                        if (orphanedIds.isNotEmpty()) {
                            photoDao.deleteByIds(orphanedIds.toList())
                        }

                        // Calculate sorted count from synced data
                        val syncedPhotos = if (missingPhotos.isNotEmpty() || orphanedIds.isNotEmpty()) {
                            photoDao.getPhotosByBucketIdSync(bucketId)
                        } else {
                            roomPhotos.filter { it.id in mediaStoreIds }
                        }
                        val sortedCount = syncedPhotos.count { it.status != PhotoStatus.UNSORTED }
                        val coverUri = mediaStorePhotos.maxByOrNull { it.dateAdded }?.systemUri

                        AlbumBubbleData(
                            bucketId = bucketId,
                            displayName = systemAlbum?.name ?: album.displayName,
                            totalCount = mediaStorePhotos.size,  // 使用 MediaStore 的真实数量
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
     * Refresh only the stats (photo count, sorted count, cover) for each album.
     * This is more efficient than loadAlbumBubbles() as it doesn't reload the list structure.
     * Use this when returning from other screens where photos may have been added/moved.
     */
    fun refreshAlbumStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val currentAlbums = _uiState.value.albums
                if (currentAlbums.isEmpty()) {
                    // No albums loaded yet, do a full load instead
                    loadAlbumBubbles()
                    _uiState.update { it.copy(isRefreshing = false) }
                    return@launch
                }

                // Get all system albums for name lookup
                val allSystemAlbums = mediaStoreDataSource.getAllAlbums()
                val systemAlbumsById = allSystemAlbums.associateBy { it.id }
                val systemAlbumsByName = allSystemAlbums.groupBy { it.name }

                // Filter to only albums still in the bubble list (may have been deleted)
                val validBucketIds = albumBubbleDao.getAllSync().map { it.bucketId }.toSet()
                val validAlbums = currentAlbums.filter { it.bucketId in validBucketIds }

                // Update stats for each album without changing order
                val updatedAlbums = validAlbums.map { album ->
                    try {
                        var bucketId = album.bucketId
                        var systemAlbum = systemAlbumsById[bucketId]

                        // Handle newly created albums with relativePath as bucketId
                        if (systemAlbum == null && bucketId.contains("/")) {
                            val albumName = bucketId.substringAfterLast("/")
                            val matchingAlbums = systemAlbumsByName[albumName]
                            if (!matchingAlbums.isNullOrEmpty()) {
                                systemAlbum = matchingAlbums.first()
                                val realBucketId = systemAlbum.id

                                // Update the database with the real bucketId
                                albumBubbleDao.deleteByBucketId(bucketId)
                                albumBubbleDao.insert(
                                    AlbumBubbleEntity(
                                        bucketId = realBucketId,
                                        displayName = album.displayName,
                                        sortOrder = currentAlbums.indexOf(album)
                                    )
                                )
                                bucketId = realBucketId
                            }
                        }

                        // Get photos from MediaStore (真实的系统照片数量)
                        val mediaStorePhotos = mediaStoreDataSource.getPhotosFromAlbum(bucketId)
                        val mediaStoreIds = mediaStorePhotos.map { it.id }.toSet()

                        // Get existing photos in Room for this album
                        val roomPhotos = photoDao.getPhotosByBucketIdSync(bucketId)
                        val roomIds = roomPhotos.map { it.id }.toSet()

                        // Add missing photos to Room (MediaStore has, Room doesn't)
                        val missingPhotos = mediaStorePhotos.filter { it.id !in roomIds }
                        if (missingPhotos.isNotEmpty()) {
                            photoDao.insertAll(missingPhotos)
                        }

                        // Remove orphaned photos from Room (Room has, MediaStore doesn't)
                        val orphanedIds = roomIds - mediaStoreIds
                        if (orphanedIds.isNotEmpty()) {
                            photoDao.deleteByIds(orphanedIds.toList())
                        }

                        // Calculate sorted count from synced data
                        val syncedPhotos = if (missingPhotos.isNotEmpty() || orphanedIds.isNotEmpty()) {
                            photoDao.getPhotosByBucketIdSync(bucketId)
                        } else {
                            roomPhotos.filter { it.id in mediaStoreIds }
                        }
                        val sortedCount = syncedPhotos.count { it.status != PhotoStatus.UNSORTED }
                        val coverUri = mediaStorePhotos.maxByOrNull { it.dateAdded }?.systemUri

                        // Return updated album data
                        album.copy(
                            bucketId = bucketId,
                            displayName = systemAlbum?.name ?: album.displayName,
                            totalCount = mediaStorePhotos.size,  // 使用 MediaStore 的真实数量
                            sortedCount = sortedCount,
                            coverUri = coverUri
                        )
                    } catch (e: Exception) {
                        // Keep existing data on error
                        album
                    }
                }

                // Update bubble nodes with new stats
                val bubbleNodes = createBubbleNodes(updatedAlbums)

                _uiState.update {
                    it.copy(
                        albums = updatedAlbums,
                        bubbleNodes = bubbleNodes,
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                // Clear refreshing state on error
                _uiState.update { it.copy(isRefreshing = false) }
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
     * Toggle view mode between grid and list.
     * Saves the new mode to preferences.
     */
    fun toggleViewMode() {
        val newMode = if (_uiState.value.viewMode == AlbumViewMode.GRID) AlbumViewMode.LIST else AlbumViewMode.GRID
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
                val result = mediaStoreDataSource.deleteAlbum(bucketId)
                when (result) {
                    is DeleteResult.Success -> {
                        // Album was empty or deleted on older Android
                        albumBubbleDao.deleteByBucketId(bucketId)
                        loadAlbumBubbles()
                        _uiState.update { it.copy(message = "相册已删除") }
                    }
                    is DeleteResult.RequiresConfirmation -> {
                        // Need system confirmation dialog
                        _uiState.update { it.copy(
                            pendingDeleteIntentSender = result.intentSender,
                            pendingDeleteBucketId = bucketId
                        ) }
                    }
                    is DeleteResult.Failed -> {
                        _uiState.update { it.copy(error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "操作失败: ${e.message}") }
            }
        }
    }

    fun onAlbumDeleteConfirmed() {
        viewModelScope.launch {
            val bucketId = _uiState.value.pendingDeleteBucketId
            if (bucketId != null) {
                albumBubbleDao.deleteByBucketId(bucketId)
                // Immediately remove from UI list without waiting for full reload
                _uiState.update { state ->
                    state.copy(
                        albums = state.albums.filter { it.bucketId != bucketId },
                        message = "相册已删除",
                        pendingDeleteIntentSender = null,
                        pendingDeleteBucketId = null
                    )
                }
            }
        }
    }

    fun clearPendingDelete() {
        _uiState.update { it.copy(
            pendingDeleteIntentSender = null,
            pendingDeleteBucketId = null
        ) }
    }
    
    /**
     * Create a new album in the system and add it to the bubble list.
     * The newly created album will be selected and placed at the top of the list.
     * If an album with the same name already exists, it will be selected instead.
     */
    fun createAlbum(albumName: String) {
        viewModelScope.launch {
            try {
                // First check if an album with this name already exists
                val allAlbums = mediaStoreDataSource.getAllAlbums()
                val existingAlbum = allAlbums.find { it.name == albumName }

                if (existingAlbum != null) {
                    // Album already exists - select it instead of creating
                    _uiState.update { state ->
                        state.copy(
                            selectedAlbumIds = state.selectedAlbumIds + existingAlbum.id,
                            availableAlbums = allAlbums,
                            message = "相册「$albumName」已存在，已为您选中"
                        )
                    }
                    return@launch
                }

                // Create album in system storage
                val result = mediaStoreDataSource.createAlbum(albumName)

                if (result != null) {
                    val displayName = result.displayName  // Clean name without "Pictures/" prefix
                    val relativePath = result.relativePath  // For MediaStore operations

                    // Add to bubble list with sort order 0 (top of list)
                    // Store relativePath in bucketId field temporarily - will be updated when photos are added
                    val newAlbum = AlbumBubbleEntity(
                        bucketId = relativePath,  // Use relativePath as identifier for new empty albums
                        displayName = displayName,
                        sortOrder = 0
                    )

                    // Shift existing albums down
                    val existingBubbleAlbums = albumBubbleDao.getAllSync()
                    existingBubbleAlbums.forEachIndexed { index, album ->
                        albumBubbleDao.updateSortOrder(album.bucketId, index + 1)
                    }

                    // Insert the new album
                    albumBubbleDao.insert(newAlbum)

                    // Add to selected IDs so it appears selected in the picker
                    _uiState.update { state ->
                        state.copy(
                            selectedAlbumIds = state.selectedAlbumIds + relativePath,
                            message = "已创建相册「$displayName」"
                        )
                    }

                    // Refresh the available albums list
                    var updatedAlbums = mediaStoreDataSource.getAllAlbums()

                    // IMPORTANT: New empty albums may not appear in MediaStore query
                    // Manually add the new album to the front if not present
                    val foundAlbum = updatedAlbums.find { it.name == displayName || it.id == relativePath }
                    if (foundAlbum == null) {
                        val newAlbumEntry = Album(
                            id = relativePath,
                            name = displayName,
                            photoCount = 0,
                            coverUri = null
                        )
                        updatedAlbums = listOf(newAlbumEntry) + updatedAlbums
                    } else {
                        // Move the existing album to the front of the list
                        updatedAlbums = listOf(foundAlbum) + updatedAlbums.filter { it.id != foundAlbum.id }
                    }

                    _uiState.update { it.copy(availableAlbums = updatedAlbums) }

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
