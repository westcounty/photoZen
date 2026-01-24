package com.example.photozen.ui.screens.filterselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.repository.CustomFilterSession
import com.example.photozen.data.repository.GuideRepository
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.Album
import com.example.photozen.data.source.MediaStoreDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Photo Filter Selection screen.
 */
data class PhotoFilterSelectionUiState(
    val albums: List<Album> = emptyList(),
    val selectedAlbumIds: Set<String> = emptySet(),
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isLoading: Boolean = true,
    val filteredPhotoCount: Int = 0,
    val error: String? = null
)

/**
 * ViewModel for Photo Filter Selection screen.
 */
@HiltViewModel
class PhotoFilterSelectionViewModel @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val preferencesRepository: PreferencesRepository,
    private val photoDao: PhotoDao,
    val guideRepository: GuideRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PhotoFilterSelectionUiState())
    val uiState: StateFlow<PhotoFilterSelectionUiState> = _uiState
    
    init {
        loadAlbums()
    }
    
    /**
     * Load all albums from MediaStore.
     */
    private fun loadAlbums() {
        viewModelScope.launch {
            try {
                val albums = mediaStoreDataSource.getAllAlbums()
                // Get count of UNSORTED photos from database (not total from MediaStore)
                // This ensures the displayed count matches what user will see in the sorter
                val unsortedCount = photoDao.getUnsortedCountFilteredSync(
                    bucketIds = null,
                    startDateMs = null,
                    endDateMs = null
                )
                _uiState.update { state ->
                    state.copy(
                        albums = albums,
                        isLoading = false,
                        filteredPhotoCount = unsortedCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "加载相册失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Toggle album selection.
     */
    fun toggleAlbum(albumId: String) {
        _uiState.update { state ->
            val newSelected = if (albumId in state.selectedAlbumIds) {
                state.selectedAlbumIds - albumId
            } else {
                state.selectedAlbumIds + albumId
            }
            state.copy(selectedAlbumIds = newSelected)
        }
        updateFilteredCount()
    }
    
    /**
     * Select all albums.
     */
    fun selectAllAlbums() {
        _uiState.update { state ->
            state.copy(selectedAlbumIds = state.albums.map { it.id }.toSet())
        }
        updateFilteredCount()
    }
    
    /**
     * Clear album selection.
     */
    fun clearSelection() {
        _uiState.update { state ->
            state.copy(selectedAlbumIds = emptySet())
        }
        updateFilteredCount()
    }
    
    /**
     * Set date range filter.
     */
    fun setDateRange(startDate: Long?, endDate: Long?) {
        _uiState.update { state ->
            state.copy(
                startDate = startDate,
                endDate = endDate
            )
        }
        updateFilteredCount()
    }
    
    /**
     * Clear date range filter.
     */
    fun clearDateRange() {
        _uiState.update { state ->
            state.copy(startDate = null, endDate = null)
        }
        updateFilteredCount()
    }
    
    /**
     * Update filtered photo count based on current selection.
     * IMPORTANT: This now queries UNSORTED photos from the database,
     * not all photos from MediaStore. This ensures the count matches
     * what the user will actually see in the sorting screen.
     *
     * Uses smart include/exclude mode to avoid SQL parameter overflow:
     * - If selected count < excluded count → use include mode (IN)
     * - If selected count >= excluded count → use exclude mode (NOT IN)
     */
    private fun updateFilteredCount() {
        viewModelScope.launch {
            try {
                val state = _uiState.value

                // Calculate which mode is more efficient
                val selectedCount = state.selectedAlbumIds.size
                val totalCount = state.albums.size
                val excludedCount = totalCount - selectedCount

                // Dates are already in milliseconds
                // For end date, extend to end of day (23:59:59.999) by adding 86400000 - 1
                val startDateMs = state.startDate
                val endDateMs = state.endDate?.let { it + 86400L * 1000 - 1 }

                val count = when {
                    // No selection = no album filter (all albums)
                    selectedCount == 0 -> {
                        photoDao.getUnsortedCountFilteredSync(
                            bucketIds = null,
                            startDateMs = startDateMs,
                            endDateMs = endDateMs
                        )
                    }
                    // All selected = no album filter
                    selectedCount == totalCount -> {
                        photoDao.getUnsortedCountFilteredSync(
                            bucketIds = null,
                            startDateMs = startDateMs,
                            endDateMs = endDateMs
                        )
                    }
                    // Use include mode when fewer albums are selected
                    selectedCount <= excludedCount -> {
                        photoDao.getUnsortedCountFilteredSync(
                            bucketIds = state.selectedAlbumIds.toList(),
                            startDateMs = startDateMs,
                            endDateMs = endDateMs
                        )
                    }
                    // Use exclude mode when more albums are selected
                    else -> {
                        val excludeIds = state.albums.map { it.id }.toSet() - state.selectedAlbumIds
                        photoDao.getUnsortedCountExcludingBucketsSync(
                            excludeBucketIds = excludeIds.toList(),
                            startDateMs = startDateMs,
                            endDateMs = endDateMs
                        )
                    }
                }
                _uiState.update { it.copy(filteredPhotoCount = count) }
            } catch (e: Exception) {
                // Ignore count update errors
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
     * Save the current selection as the session custom filter.
     * Called when user confirms the selection.
     *
     * Uses smart include/exclude mode to prevent SQL parameter overflow:
     * - If selected count < excluded count → use include mode (albumIds)
     * - If selected count >= excluded count → use exclude mode (excludeAlbumIds)
     * - If all or none selected → use null (no album filter)
     */
    fun saveSessionFilter() {
        val state = _uiState.value

        val selectedCount = state.selectedAlbumIds.size
        val totalCount = state.albums.size
        val excludedCount = totalCount - selectedCount

        // Determine the most efficient mode
        val (albumIds, excludeAlbumIds) = when {
            // No selection = no filter
            selectedCount == 0 -> null to null
            // All selected = no filter
            selectedCount == totalCount -> null to null
            // Use include mode when fewer albums are selected
            selectedCount <= excludedCount -> state.selectedAlbumIds.toList() to null
            // Use exclude mode when more albums are selected (fewer to exclude)
            else -> {
                val excludeIds = state.albums.map { it.id }.toSet() - state.selectedAlbumIds
                null to excludeIds.toList()
            }
        }

        val filter = CustomFilterSession(
            albumIds = albumIds,
            excludeAlbumIds = excludeAlbumIds,
            startDate = state.startDate,
            endDate = state.endDate
        )
        preferencesRepository.setSessionCustomFilter(filter)
    }
}
