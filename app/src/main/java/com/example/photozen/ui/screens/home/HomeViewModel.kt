package com.example.photozen.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.AchievementData
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.DailyTaskStatus
import com.example.photozen.domain.usecase.GetDailyTaskStatusUseCase
import com.example.photozen.data.local.dao.FaceDao
import com.example.photozen.data.local.dao.PhotoAnalysisDao
import com.example.photozen.data.local.dao.PhotoLabelDao
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.GetUnsortedPhotosUseCase
import com.example.photozen.domain.usecase.SyncPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
    val photoFilterMode: PhotoFilterMode = PhotoFilterMode.ALL,
    val dailyTaskStatus: DailyTaskStatus? = null,
    val onestopEnabled: Boolean = false,
    val experimentalEnabled: Boolean = false,
    // Smart Gallery stats
    val smartGalleryPersonCount: Int = 0,
    val smartGalleryLabelCount: Int = 0,
    val smartGalleryGpsPhotoCount: Int = 0,
    val smartGalleryAnalysisProgress: Float = 0f,
    val smartGalleryIsAnalyzing: Boolean = false
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
    private val photoRepository: PhotoRepository,
    private val getDailyTaskStatusUseCase: GetDailyTaskStatusUseCase,
    // Smart Gallery DAOs
    private val faceDao: FaceDao,
    private val photoLabelDao: PhotoLabelDao,
    private val photoAnalysisDao: PhotoAnalysisDao,
    private val photoDao: com.example.photozen.data.local.dao.PhotoDao
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
     * Smart Gallery statistics data class.
     */
    private data class SmartGalleryStats(
        val personCount: Int = 0,
        val labelCount: Int = 0,
        val gpsPhotoCount: Int = 0,
        val analyzedCount: Int = 0,
        val totalPhotos: Int = 0
    ) {
        val analysisProgress: Float
            get() = if (totalPhotos > 0) analyzedCount.toFloat() / totalPhotos else 0f
    }
    
    /**
     * Smart Gallery statistics flow.
     * Must be defined before uiState to avoid initialization order issues.
     */
    private val smartGalleryStats: StateFlow<SmartGalleryStats> = combine(
        faceDao.getPersonCountFlow(),
        photoLabelDao.getUniqueLabelCountFlow(),
        photoDao.getPhotosWithGpsCount(),
        photoAnalysisDao.getAnalyzedCountFlow(),
        getPhotosUseCase.getTotalCount()
    ) { personCount, labelCount, gpsCount, analyzedCount, totalPhotos ->
        SmartGalleryStats(
            personCount = personCount,
            labelCount = labelCount,
            gpsPhotoCount = gpsCount,
            analyzedCount = analyzedCount,
            totalPhotos = totalPhotos
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SmartGalleryStats()
    )
    
    /**
     * Intermediate flow for filtered counts.
     */
    private val filteredCountsFlow: Flow<FilteredCounts> = combine(
        getFilteredTotalCount(),
        getFilteredSortedCount(),
        preferencesRepository.getOnestopEnabled(),
        preferencesRepository.getExperimentalEnabled()
    ) { total, sorted, onestop, experimental ->
        FilteredCounts(total, sorted, onestop, experimental)
    }
    
    /**
     * Intermediate flow for extra data (error, achievement, filter mode, daily task, filtered counts).
     */
    private val extraDataFlow: Flow<ExtraData> = combine(
        _error,
        preferencesRepository.getAllAchievementData(),
        preferencesRepository.getPhotoFilterMode(),
        getDailyTaskStatusUseCase(),
        filteredCountsFlow
    ) { error, achievementData, filterMode, dailyTaskStatus, counts ->
        ExtraData(error, achievementData, filterMode, dailyTaskStatus, counts)
    }
    
    /**
     * Intermediate flow for photo counts (total, unsorted, keep, trash, maybe).
     */
    private val photoCountsFlow: Flow<PhotoCounts> = combine(
        getPhotosUseCase.getTotalCount(),
        getFilteredUnsortedCount(),
        getPhotosUseCase.getCountByStatus(PhotoStatus.KEEP),
        getPhotosUseCase.getCountByStatus(PhotoStatus.TRASH),
        getPhotosUseCase.getCountByStatus(PhotoStatus.MAYBE)
    ) { total, unsorted, keep, trash, maybe ->
        PhotoCounts(total, unsorted, keep, trash, maybe)
    }
    
    /**
     * Intermediate flow for UI state flags.
     */
    private val uiStateFlags: Flow<UiStateFlags> = combine(
        _hasPermission,
        _isLoading,
        _isSyncing,
        _syncResult
    ) { hasPermission, isLoading, isSyncing, syncResult ->
        UiStateFlags(hasPermission, isLoading, isSyncing, syncResult)
    }
    
    /**
     * UI State exposed to the screen.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        photoCountsFlow,
        uiStateFlags,
        extraDataFlow,
        smartGalleryStats
    ) { counts, flags, extraData, sgStats ->
        HomeUiState(
            totalPhotos = counts.total,
            unsortedCount = counts.unsorted,
            keepCount = counts.keep,
            trashCount = counts.trash,
            maybeCount = counts.maybe,
            filteredTotal = extraData.counts.filteredTotal,
            filteredSorted = extraData.counts.filteredSorted,
            hasPermission = flags.hasPermission,
            isLoading = flags.isLoading,
            isSyncing = flags.isSyncing,
            syncResult = flags.syncResult,
            error = extraData.error,
            achievementData = extraData.achievementData,
            photoFilterMode = extraData.filterMode,
            dailyTaskStatus = extraData.dailyTaskStatus,
            onestopEnabled = extraData.counts.onestopEnabled,
            experimentalEnabled = extraData.counts.experimentalEnabled,
            // Smart Gallery stats
            smartGalleryPersonCount = sgStats.personCount,
            smartGalleryLabelCount = sgStats.labelCount,
            smartGalleryGpsPhotoCount = sgStats.gpsPhotoCount,
            smartGalleryAnalysisProgress = sgStats.analysisProgress,
            smartGalleryIsAnalyzing = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
    
    /**
     * Helper data class for combining filtered data.
     */
    private data class FilteredCounts(
        val filteredTotal: Int,
        val filteredSorted: Int,
        val onestopEnabled: Boolean,
        val experimentalEnabled: Boolean
    )
    
    private data class ExtraData(
        val error: String?,
        val achievementData: AchievementData,
        val filterMode: PhotoFilterMode,
        val dailyTaskStatus: DailyTaskStatus?,
        val counts: FilteredCounts
    )
    
    private data class PhotoCounts(
        val total: Int,
        val unsorted: Int,
        val keep: Int,
        val trash: Int,
        val maybe: Int
    )
    
    private data class UiStateFlags(
        val hasPermission: Boolean,
        val isLoading: Boolean,
        val isSyncing: Boolean,
        val syncResult: String?
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
