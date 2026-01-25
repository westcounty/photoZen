package com.example.photozen.ui.screens.timeline

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.CustomFilterSession
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.ui.components.PhotoGridMode
import com.example.photozen.ui.screens.photolist.PhotoListSortOrder
import com.example.photozen.ui.state.PhotoSelectionStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Timeline Detail screen.
 */
data class TimelineDetailUiState(
    val title: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val photos: List<PhotoEntity> = emptyList(),
    val allPhotos: List<PhotoEntity> = emptyList(),
    val totalCount: Int = 0,
    val sortedCount: Int = 0,
    val isLoading: Boolean = true,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val pendingDeleteIds: List<String> = emptyList(),
    val error: String? = null,
    val message: String? = null,
    // Status filter
    val statusFilter: Set<PhotoStatus> = PhotoStatus.entries.toSet(),
    // Grid mode and columns
    val isSquareMode: Boolean = true,
    val columnCount: Int = 3,
    // Sort order
    val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC
) {
    val selectedCount: Int get() = selectedIds.size
    val unsortedCount: Int get() = totalCount - sortedCount
    val sortedPercentage: Float get() = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
    val isFilterActive: Boolean get() = statusFilter.size < PhotoStatus.entries.size
    val gridMode: PhotoGridMode get() = if (isSquareMode) PhotoGridMode.SQUARE else PhotoGridMode.WATERFALL
}

/**
 * ViewModel for Timeline Detail screen.
 *
 * Displays all photos within a specific time range (event group).
 */
@HiltViewModel
class TimelineDetailViewModel @Inject constructor(
    private val photoDao: PhotoDao,
    private val albumBubbleDao: AlbumBubbleDao,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val selectionStateHolder: PhotoSelectionStateHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineDetailUiState())

    val uiState: StateFlow<TimelineDetailUiState> = combine(
        _uiState,
        selectionStateHolder.selectedIds,
        selectionStateHolder.isSelectionMode
    ) { state, selectedIds, isSelectionMode ->
        val validSelectedIds = selectedIds.filter { id ->
            state.photos.any { it.id == id }
        }.toSet()

        state.copy(
            selectedIds = validSelectedIds,
            isSelectionMode = isSelectionMode || validSelectedIds.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimelineDetailUiState()
    )

    private var loadJob: Job? = null
    private var randomSeed = System.currentTimeMillis()

    /**
     * Initialize with time range.
     */
    fun initialize(title: String, startTime: Long, endTime: Long) {
        selectionStateHolder.clear()
        _uiState.update {
            it.copy(
                title = title,
                startTime = startTime,
                endTime = endTime
            )
        }
        loadPhotos()
        loadAlbumBubbleList()
    }

    /**
     * Load photos in the time range.
     */
    private fun loadPhotos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val startTime = _uiState.value.startTime
            val endTime = _uiState.value.endTime

            if (startTime == 0L || endTime == 0L) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                photoDao.getPhotosByTimeRange(startTime, endTime).collect { allPhotos ->
                    val sortedCount = allPhotos.count { it.status != PhotoStatus.UNSORTED }
                    val statusFilter = _uiState.value.statusFilter
                    val sortOrder = _uiState.value.sortOrder

                    // Apply status filter
                    val filteredPhotos = if (statusFilter.size == PhotoStatus.entries.size) {
                        allPhotos
                    } else {
                        allPhotos.filter { it.status in statusFilter }
                    }

                    // Apply sorting
                    val sortedPhotos = applySortOrder(filteredPhotos, sortOrder)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            photos = sortedPhotos,
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
     * Apply sort order to photos list.
     */
    private fun applySortOrder(
        photos: List<PhotoEntity>,
        sortOrder: PhotoListSortOrder
    ): List<PhotoEntity> {
        return when (sortOrder) {
            PhotoListSortOrder.DATE_DESC -> photos.sortedByDescending { it.dateTaken }
            PhotoListSortOrder.DATE_ASC -> photos.sortedBy { it.dateTaken }
            PhotoListSortOrder.ADDED_DESC -> photos.sortedByDescending { it.updatedAt }
            PhotoListSortOrder.ADDED_ASC -> photos.sortedBy { it.updatedAt }
            PhotoListSortOrder.RANDOM -> photos.shuffled(kotlin.random.Random(randomSeed))
        }
    }

    // ==================== Sort & View Mode ====================

    /**
     * Set sort order.
     */
    fun setSortOrder(order: PhotoListSortOrder) {
        if (order == PhotoListSortOrder.RANDOM) {
            randomSeed = System.currentTimeMillis()
        }
        _uiState.update { state ->
            val sortedPhotos = applySortOrder(state.photos, order)
            state.copy(sortOrder = order, photos = sortedPhotos)
        }
    }

    /**
     * Toggle between SQUARE (grid) and WATERFALL (staggered) modes.
     */
    fun toggleGridMode() {
        _uiState.update { state ->
            val newIsSquare = !state.isSquareMode
            val newColumns = if (newIsSquare && state.columnCount < 2) 2 else state.columnCount
            state.copy(isSquareMode = newIsSquare, columnCount = newColumns)
        }
    }

    /**
     * Set grid columns.
     */
    fun setGridColumns(columns: Int) {
        _uiState.update { state ->
            val minColumns = if (state.isSquareMode) 2 else 1
            val validColumns = columns.coerceIn(minColumns, 5)
            state.copy(columnCount = validColumns)
        }
    }

    /**
     * Set grid mode directly.
     */
    fun setGridMode(mode: PhotoGridMode) {
        _uiState.update { state ->
            val newIsSquare = mode == PhotoGridMode.SQUARE
            val newColumns = if (newIsSquare && state.columnCount < 2) 2 else state.columnCount
            state.copy(isSquareMode = newIsSquare, columnCount = newColumns)
        }
    }

    // ==================== Selection ====================

    /**
     * Enter selection mode.
     */
    fun enterSelectionMode(photoId: String) {
        selectionStateHolder.clear()
        selectionStateHolder.select(photoId)
    }

    /**
     * Toggle photo selection.
     */
    fun togglePhotoSelection(photoId: String) {
        selectionStateHolder.toggle(photoId)
    }

    /**
     * Select all photos.
     */
    fun selectAll() {
        val allIds = _uiState.value.photos.map { it.id }
        selectionStateHolder.selectAll(allIds)
    }

    /**
     * Clear selection.
     */
    fun clearSelection() {
        selectionStateHolder.clear()
    }

    /**
     * Update selection state.
     */
    fun updateSelection(newSelection: Set<String>) {
        selectionStateHolder.setSelection(newSelection)
    }

    // ==================== Status Filter ====================

    /**
     * Toggle a status in the filter.
     */
    fun toggleStatusFilter(status: PhotoStatus) {
        val currentFilter = _uiState.value.statusFilter
        val newFilter = if (status in currentFilter) {
            if (currentFilter.size > 1) {
                currentFilter - status
            } else {
                currentFilter
            }
        } else {
            currentFilter + status
        }

        _uiState.update { state ->
            val filteredPhotos = if (newFilter.size == PhotoStatus.entries.size) {
                state.allPhotos
            } else {
                state.allPhotos.filter { it.status in newFilter }
            }
            val sortedPhotos = applySortOrder(filteredPhotos, state.sortOrder)
            state.copy(statusFilter = newFilter, photos = sortedPhotos)
        }
    }

    /**
     * Select all statuses (reset filter).
     */
    fun selectAllStatuses() {
        _uiState.update { state ->
            val sortedPhotos = applySortOrder(state.allPhotos, state.sortOrder)
            state.copy(statusFilter = PhotoStatus.entries.toSet(), photos = sortedPhotos)
        }
    }

    // ==================== Album Operations ====================

    /**
     * Copy selected photos to another album.
     */
    fun copySelectedToAlbum(targetBucketId: String) {
        viewModelScope.launch {
            val selectedIds = selectionStateHolder.getSelectedList().toSet()
            val selectedPhotos = _uiState.value.photos.filter { it.id in selectedIds }
            var successCount = 0

            val targetAlbum = _uiState.value.albumBubbleList.find { it.bucketId == targetBucketId }
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
     * Copy selected photos to the same location.
     */
    fun copySelectedPhotos() {
        viewModelScope.launch {
            val selectedIds = selectionStateHolder.getSelectedList().toSet()
            val selectedPhotos = _uiState.value.photos.filter { it.id in selectedIds }
            var successCount = 0

            for (photo in selectedPhotos) {
                val photoUri = Uri.parse(photo.systemUri)
                val bucketId = photo.bucketId ?: continue
                val targetAlbum = _uiState.value.albumBubbleList.find { it.bucketId == bucketId }
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${targetAlbum?.displayName ?: "PhotoZen"}"

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

    // ==================== Status Operations ====================

    /**
     * Change selected photos status.
     */
    fun changeSelectedPhotosStatus(newStatus: PhotoStatus) {
        viewModelScope.launch {
            val selectedIds = selectionStateHolder.getSelectedList().toSet()

            for (photoId in selectedIds) {
                photoDao.updateStatus(photoId, newStatus)
            }

            val statusName = when (newStatus) {
                PhotoStatus.KEEP -> "保留"
                PhotoStatus.MAYBE -> "待定"
                PhotoStatus.TRASH -> "回收站"
                PhotoStatus.UNSORTED -> "未筛选"
            }
            _uiState.update { it.copy(message = "已将 ${selectedIds.size} 张照片标记为$statusName") }
            clearSelection()
        }
    }

    // ==================== Delete Operations ====================

    /**
     * Check if there are selected photos to delete.
     */
    fun hasSelectedPhotos(): Boolean {
        return selectionStateHolder.hasSelection()
    }

    /**
     * Get URIs of selected photos for delete request.
     */
    fun getSelectedPhotoUris(): List<Uri> {
        val selectedIds = selectionStateHolder.getSelectedList().toSet()
        val selectedPhotos = _uiState.value.photos.filter { it.id in selectedIds }

        _uiState.update { it.copy(pendingDeleteIds = selectedPhotos.map { photo -> photo.id }) }

        return selectedPhotos.map { Uri.parse(it.systemUri) }
    }

    /**
     * Handle delete confirmation result.
     */
    fun onDeleteConfirmed() {
        viewModelScope.launch {
            val pendingDeleteIds = _uiState.value.pendingDeleteIds

            if (pendingDeleteIds.isNotEmpty()) {
                photoDao.deleteByIds(pendingDeleteIds)
            }

            _uiState.update {
                it.copy(
                    pendingDeleteIds = emptyList(),
                    message = "照片已删除"
                )
            }
            clearSelection()
        }
    }

    // ==================== Navigation ====================

    /**
     * Set filter session for "从此开始筛选" feature.
     */
    fun setFilterSessionAndNavigate(photoIds: List<String>) {
        viewModelScope.launch {
            preferencesRepository.setSessionCustomFilter(
                CustomFilterSession(
                    photoIds = photoIds,
                    preciseMode = true
                )
            )
            preferencesRepository.setPhotoFilterMode(PhotoFilterMode.CUSTOM)
        }
    }

    // ==================== Utility ====================

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

    override fun onCleared() {
        super.onCleared()
        selectionStateHolder.clear()
    }
}
