package com.example.photozen.ui.screens.filterselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.repository.CustomFilterSession
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.Album
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.data.source.PhotoFilter
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
    private val preferencesRepository: PreferencesRepository
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
                val totalCount = mediaStoreDataSource.getPhotoCount()
                _uiState.update { state ->
                    state.copy(
                        albums = albums,
                        isLoading = false,
                        filteredPhotoCount = totalCount
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
     */
    private fun updateFilteredCount() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val filter = PhotoFilter(
                    albumIds = if (state.selectedAlbumIds.isEmpty()) null else state.selectedAlbumIds.toList(),
                    startDate = state.startDate,
                    endDate = state.endDate
                )
                val count = mediaStoreDataSource.getFilteredPhotoCount(filter)
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
     */
    fun saveSessionFilter() {
        val state = _uiState.value
        val filter = CustomFilterSession(
            albumIds = if (state.selectedAlbumIds.isEmpty()) null else state.selectedAlbumIds.toList(),
            startDate = state.startDate,
            endDate = state.endDate
        )
        preferencesRepository.setSessionCustomFilter(filter)
    }
}
