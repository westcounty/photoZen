package com.example.photozen.ui.screens.filterselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.repository.CustomFilterSession
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
    private val photoDao: PhotoDao
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
     */
    private fun updateFilteredCount() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                
                // Convert bucket IDs: empty set means no filter (null)
                val bucketIds = if (state.selectedAlbumIds.isEmpty()) null 
                                else state.selectedAlbumIds.toList()
                
                // Dates are already in milliseconds
                // For end date, extend to end of day (23:59:59.999) by adding 86400000 - 1
                val startDateMs = state.startDate
                val endDateMs = state.endDate?.let { it + 86400L * 1000 - 1 }
                
                val count = photoDao.getUnsortedCountFilteredSync(
                    bucketIds = bucketIds,
                    startDateMs = startDateMs,
                    endDateMs = endDateMs
                )
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
     * IMPORTANT: When ALL albums are selected, we use null to indicate "no album restriction".
     * This prevents SQL parameter overflow when there are many albums (SQLite has ~999 param limit),
     * and also improves query performance.
     */
    fun saveSessionFilter() {
        val state = _uiState.value
        
        // If all albums are selected, treat it as "no album filter" (null)
        // This prevents SQL issues with large IN clauses and improves performance
        val albumIds = when {
            state.selectedAlbumIds.isEmpty() -> null  // No selection = no filter
            state.selectedAlbumIds.size == state.albums.size -> null  // All selected = no filter
            else -> state.selectedAlbumIds.toList()  // Partial selection = specific filter
        }
        
        val filter = CustomFilterSession(
            albumIds = albumIds,
            startDate = state.startDate,
            endDate = state.endDate
        )
        preferencesRepository.setSessionCustomFilter(filter)
    }
}
