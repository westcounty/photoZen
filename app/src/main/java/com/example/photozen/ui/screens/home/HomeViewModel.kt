package com.example.photozen.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.AchievementData
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.GetUnsortedPhotosUseCase
import com.example.photozen.domain.usecase.SyncPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Home screen.
 */
data class HomeUiState(
    val totalPhotos: Int = 0,
    val unsortedCount: Int = 0,
    val keepCount: Int = 0,
    val trashCount: Int = 0,
    val maybeCount: Int = 0,
    // Filtered counts based on current filter mode
    val filteredTotal: Int = 0,
    val filteredSorted: Int = 0,
    val hasPermission: Boolean = false,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val syncResult: String? = null,
    val error: String? = null,
    val achievementData: AchievementData = AchievementData(),
    val photoFilterMode: PhotoFilterMode = PhotoFilterMode.ALL
) {
    val sortedCount: Int
        get() = keepCount + trashCount + maybeCount
    
    val progress: Float
        get() = if (totalPhotos > 0) sortedCount.toFloat() / totalPhotos else 0f
    
    val hasPhotos: Boolean
        get() = totalPhotos > 0
    
    /**
     * Whether the user needs to select a custom filter before starting to sort.
     */
    val needsFilterSelection: Boolean
        get() = photoFilterMode == PhotoFilterMode.CUSTOM
    
    /**
     * Progress within the filtered range.
     */
    val filteredProgress: Float
        get() = if (filteredTotal > 0) filteredSorted.toFloat() / filteredTotal else 0f
    
    /**
     * Unsorted count within the filtered range.
     */
    val filteredUnsorted: Int
        get() = filteredTotal - filteredSorted
}

/**
 * ViewModel for Home screen.
 * Manages photo statistics and sync status.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPhotosUseCase: GetPhotosUseCase,
    private val getUnsortedPhotosUseCase: GetUnsortedPhotosUseCase,
    private val syncPhotosUseCase: SyncPhotosUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val photoRepository: PhotoRepository
) : ViewModel() {
    
    private val _hasPermission = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)
    private val _isSyncing = MutableStateFlow(false)
    private val _syncResult = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)
    
    // Camera album IDs - loaded once and cached
    private val _cameraAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _cameraAlbumsLoaded = MutableStateFlow(false)
    
    /**
     * Get filtered unsorted count based on filter mode.
     * IMPORTANT: Wait for camera albums to load before returning filtered counts.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getFilteredUnsortedCount() = combine(
        preferencesRepository.getPhotoFilterMode(),
        _cameraAlbumIds,
        _cameraAlbumsLoaded
    ) { filterMode, cameraIds, loaded ->
        Triple(filterMode, cameraIds, loaded)
    }.flatMapLatest { (filterMode, cameraIds, loaded) ->
        when (filterMode) {
            PhotoFilterMode.ALL -> getUnsortedPhotosUseCase.getCount()
            PhotoFilterMode.CAMERA_ONLY -> {
                // Must wait for camera albums to load
                if (!loaded) {
                    kotlinx.coroutines.flow.flowOf(0) // Show 0 while loading
                } else if (cameraIds.isNotEmpty()) {
                    getUnsortedPhotosUseCase.getCountByBuckets(cameraIds)
                } else {
                    // No camera albums found
                    kotlinx.coroutines.flow.flowOf(0)
                }
            }
            PhotoFilterMode.EXCLUDE_CAMERA -> {
                // Must wait for camera albums to load
                if (!loaded) {
                    kotlinx.coroutines.flow.flowOf(0) // Show 0 while loading
                } else if (cameraIds.isNotEmpty()) {
                    getUnsortedPhotosUseCase.getCountExcludingBuckets(cameraIds)
                } else {
                    // No camera albums found, show all photos
                    getUnsortedPhotosUseCase.getCount()
                }
            }
            PhotoFilterMode.CUSTOM -> {
                // For custom mode, show total unsorted until user selects filter
                getUnsortedPhotosUseCase.getCount()
            }
        }
    }
    
    /**
     * Get filtered total count based on filter mode.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getFilteredTotalCount() = combine(
        preferencesRepository.getPhotoFilterMode(),
        _cameraAlbumIds,
        _cameraAlbumsLoaded
    ) { filterMode, cameraIds, loaded ->
        Triple(filterMode, cameraIds, loaded)
    }.flatMapLatest { (filterMode, cameraIds, loaded) ->
        when (filterMode) {
            PhotoFilterMode.ALL -> photoRepository.getTotalCount()
            PhotoFilterMode.CAMERA_ONLY -> {
                if (!loaded) {
                    kotlinx.coroutines.flow.flowOf(0)
                } else if (cameraIds.isNotEmpty()) {
                    photoRepository.getTotalCountByBuckets(cameraIds)
                } else {
                    kotlinx.coroutines.flow.flowOf(0)
                }
            }
            PhotoFilterMode.EXCLUDE_CAMERA -> {
                if (!loaded) {
                    kotlinx.coroutines.flow.flowOf(0)
                } else if (cameraIds.isNotEmpty()) {
                    photoRepository.getTotalCountExcludingBuckets(cameraIds)
                } else {
                    photoRepository.getTotalCount()
                }
            }
            PhotoFilterMode.CUSTOM -> {
                photoRepository.getTotalCount()
            }
        }
    }
    
    /**
     * Get filtered sorted count based on filter mode.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getFilteredSortedCount() = combine(
        preferencesRepository.getPhotoFilterMode(),
        _cameraAlbumIds,
        _cameraAlbumsLoaded
    ) { filterMode, cameraIds, loaded ->
        Triple(filterMode, cameraIds, loaded)
    }.flatMapLatest { (filterMode, cameraIds, loaded) ->
        when (filterMode) {
            PhotoFilterMode.ALL -> {
                combine(
                    getPhotosUseCase.getCountByStatus(PhotoStatus.KEEP),
                    getPhotosUseCase.getCountByStatus(PhotoStatus.TRASH),
                    getPhotosUseCase.getCountByStatus(PhotoStatus.MAYBE)
                ) { keep, trash, maybe -> keep + trash + maybe }
            }
            PhotoFilterMode.CAMERA_ONLY -> {
                if (!loaded) {
                    kotlinx.coroutines.flow.flowOf(0)
                } else if (cameraIds.isNotEmpty()) {
                    photoRepository.getSortedCountByBuckets(cameraIds)
                } else {
                    kotlinx.coroutines.flow.flowOf(0)
                }
            }
            PhotoFilterMode.EXCLUDE_CAMERA -> {
                if (!loaded) {
                    kotlinx.coroutines.flow.flowOf(0)
                } else if (cameraIds.isNotEmpty()) {
                    photoRepository.getSortedCountExcludingBuckets(cameraIds)
                } else {
                    combine(
                        getPhotosUseCase.getCountByStatus(PhotoStatus.KEEP),
                        getPhotosUseCase.getCountByStatus(PhotoStatus.TRASH),
                        getPhotosUseCase.getCountByStatus(PhotoStatus.MAYBE)
                    ) { keep, trash, maybe -> keep + trash + maybe }
                }
            }
            PhotoFilterMode.CUSTOM -> {
                combine(
                    getPhotosUseCase.getCountByStatus(PhotoStatus.KEEP),
                    getPhotosUseCase.getCountByStatus(PhotoStatus.TRASH),
                    getPhotosUseCase.getCountByStatus(PhotoStatus.MAYBE)
                ) { keep, trash, maybe -> keep + trash + maybe }
            }
        }
    }
    
    /**
     * UI State exposed to the screen.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        getPhotosUseCase.getTotalCount(),
        getFilteredUnsortedCount(),
        getPhotosUseCase.getCountByStatus(PhotoStatus.KEEP),
        getPhotosUseCase.getCountByStatus(PhotoStatus.TRASH),
        getPhotosUseCase.getCountByStatus(PhotoStatus.MAYBE),
        _hasPermission,
        _isLoading,
        _isSyncing,
        _syncResult,
        combine(
            _error, 
            preferencesRepository.getAllAchievementData(),
            preferencesRepository.getPhotoFilterMode(),
            getFilteredTotalCount(),
            getFilteredSortedCount()
        ) { error, achievementData, filterMode, filteredTotal, filteredSorted ->
            FilteredData(error, achievementData, filterMode, filteredTotal, filteredSorted)
        }
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val combined = values[9] as FilteredData
        HomeUiState(
            totalPhotos = values[0] as Int,
            unsortedCount = values[1] as Int,
            keepCount = values[2] as Int,
            trashCount = values[3] as Int,
            maybeCount = values[4] as Int,
            filteredTotal = combined.filteredTotal,
            filteredSorted = combined.filteredSorted,
            hasPermission = values[5] as Boolean,
            isLoading = values[6] as Boolean,
            isSyncing = values[7] as Boolean,
            syncResult = values[8] as String?,
            error = combined.error,
            achievementData = combined.achievementData,
            photoFilterMode = combined.filterMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
    
    /**
     * Helper data class for combining filtered data.
     */
    private data class FilteredData(
        val error: String?,
        val achievementData: AchievementData,
        val filterMode: PhotoFilterMode,
        val filteredTotal: Int,
        val filteredSorted: Int
    )
    
    init {
        // Update consecutive days tracking on app launch
        viewModelScope.launch {
            preferencesRepository.updateConsecutiveDays()
        }
        // Load camera album IDs
        viewModelScope.launch {
            loadCameraAlbumIds()
        }
    }
    
    /**
     * Load camera album IDs from MediaStore.
     */
    private suspend fun loadCameraAlbumIds() {
        try {
            val albums = mediaStoreDataSource.getAllAlbums()
            _cameraAlbumIds.value = albums.filter { it.isCamera }.map { it.id }
        } catch (e: Exception) {
            // Ignore errors, just use empty list
        } finally {
            _cameraAlbumsLoaded.value = true
        }
    }
    
    /**
     * Called when permission is granted.
     */
    fun onPermissionGranted() {
        _hasPermission.value = true
        syncPhotos()
    }
    
    /**
     * Called when permission is denied.
     */
    fun onPermissionDenied() {
        _hasPermission.value = false
        _isLoading.value = false
        _error.value = "需要存储权限才能访问照片"
    }
    
    /**
     * Sync photos from MediaStore.
     */
    fun syncPhotos() {
        if (!_hasPermission.value) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = syncPhotosUseCase()
                _syncResult.value = if (result.isInitialSync) {
                    "已导入 ${result.newPhotosCount} 张照片"
                } else if (result.newPhotosCount > 0) {
                    "发现 ${result.newPhotosCount} 张新照片"
                } else {
                    null
                }
            } catch (e: Exception) {
                _error.value = "同步失败: ${e.message}"
            } finally {
                _isSyncing.value = false
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear sync result message.
     */
    fun clearSyncResult() {
        _syncResult.value = null
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
}
