package com.example.photozen.ui.screens.flowsorter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoSortOrder
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.CustomFilterSession
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.GetDailyTaskStatusUseCase
import com.example.photozen.domain.usecase.GetUnsortedPhotosUseCase
import com.example.photozen.domain.usecase.SortPhotoUseCase
import com.example.photozen.domain.usecase.SyncPhotosUseCase
import com.example.photozen.util.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
 * Combo state for tracking rapid sorting streaks.
 */
data class ComboState(
    val count: Int = 0,
    val maxCount: Int = 0,
    val lastSwipeTime: Long = 0,
    val isActive: Boolean = false
) {
    companion object {
        /** Time threshold in ms to maintain combo */
        const val COMBO_THRESHOLD_MS = 1500L
    }
    
    /** Combo level affects visual feedback */
    val level: ComboLevel
        get() = when {
            count >= 20 -> ComboLevel.FIRE
            count >= 10 -> ComboLevel.HOT
            count >= 5 -> ComboLevel.WARM
            count >= 1 -> ComboLevel.NORMAL
            else -> ComboLevel.NONE
        }
}

/**
 * Visual levels for combo feedback.
 */
enum class ComboLevel {
    NONE,    // No combo
    NORMAL,  // x1-x4: White
    WARM,    // x5-x9: Light orange
    HOT,     // x10-x19: Orange
    FIRE     // x20+: Red/Fire
}

/**
 * View mode for the flow sorter.
 */
enum class FlowSorterViewMode {
    CARD,  // Single card swipe view
    LIST   // Grid list with batch selection
}

/**
 * UI State for Flow Sorter screen.
 */
data class FlowSorterUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isReloading: Boolean = false,
    val totalCount: Int = 0,
    val sortedCount: Int = 0,
    val keepCount: Int = 0,
    val trashCount: Int = 0,
    val maybeCount: Int = 0,
    val lastAction: SortAction? = null,
    val error: String? = null,
    val combo: ComboState = ComboState(),
    val viewMode: FlowSorterViewMode = FlowSorterViewMode.CARD,
    val selectedPhotoIds: Set<String> = emptySet(),
    val sortOrder: PhotoSortOrder = PhotoSortOrder.DATE_DESC,
    val gridColumns: Int = 2,
    val filterMode: PhotoFilterMode = PhotoFilterMode.ALL,
    val cameraAlbumsLoaded: Boolean = false,
    val isDailyTask: Boolean = false,
    val dailyTaskTarget: Int = -1,
    val dailyTaskCurrent: Int = 0,
    val isDailyTaskComplete: Boolean = false,
    val cardZoomEnabled: Boolean = true,
    val swipeSensitivity: Float = 1.0f
) {
    val currentPhoto: PhotoEntity?
        get() = photos.getOrNull(currentIndex)
    
    val nextPhoto: PhotoEntity?
        get() = photos.getOrNull(currentIndex + 1)
    
    val hasPhotos: Boolean
        get() = photos.isNotEmpty()
    
    val isComplete: Boolean
        get() = photos.isEmpty() || currentIndex >= photos.size
    
    val progress: Float
        get() = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
    
    val isSelectionMode: Boolean
        get() = selectedPhotoIds.isNotEmpty()
    
    val selectedCount: Int
        get() = selectedPhotoIds.size
}

/**
 * Represents the last sorting action for undo functionality.
 */
data class SortAction(
    val photoId: String,
    val status: PhotoStatus,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ViewModel for Flow Sorter screen.
 * Manages photo list, sorting actions, and undo functionality.
 */
@HiltViewModel
class FlowSorterViewModel @Inject constructor(
    private val getUnsortedPhotosUseCase: GetUnsortedPhotosUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val syncPhotosUseCase: SyncPhotosUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val savedStateHandle: SavedStateHandle,
    private val getDailyTaskStatusUseCase: GetDailyTaskStatusUseCase,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {
    
    private val isDailyTask: Boolean = savedStateHandle["isDailyTask"] ?: false
    private val targetCount: Int = savedStateHandle["targetCount"] ?: -1
    
    private val _isLoading = MutableStateFlow(true)
    private val _isSyncing = MutableStateFlow(false)
    private val _currentIndex = MutableStateFlow(0)
    private val _lastAction = MutableStateFlow<SortAction?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _counters = MutableStateFlow(SortCounters())
    private val _combo = MutableStateFlow(ComboState())
    private val _viewMode = MutableStateFlow(FlowSorterViewMode.CARD)
    private val _selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    private val _sortOrder = MutableStateFlow(PhotoSortOrder.DATE_DESC)
    private val _gridColumns = MutableStateFlow(2)
    
    // CRITICAL FIX: Store initial total count to prevent flickering during rapid swipes
    // This ensures totalCount remains stable even when counters and photos list update at different times
    private val _initialTotalCount = MutableStateFlow(-1) // -1 means not yet initialized
    
    // Random seed for consistent random sorting until changed
    private var randomSeed = System.currentTimeMillis()
    
    // Camera album IDs as StateFlow for reactive updates
    private val _cameraAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _cameraAlbumsLoaded = MutableStateFlow(false)
    
    // Job for auto-hiding combo after timeout
    private var comboTimeoutJob: Job? = null
    
    // ==================== PAGINATION STATE ====================
    // Pagination ensures correct sorting across ALL photos, not just the first 500.
    // The database applies ORDER BY to all matching photos, then LIMIT/OFFSET for pagination.
    
    /** Current page of photos loaded (0-indexed) */
    private var currentPage = 0
    
    /** Whether we're currently loading more photos */
    private val _isLoadingMore = MutableStateFlow(false)
    
    /** Whether we're reloading photos (e.g., after sort order change) - prevents "complete" flash */
    private val _isReloading = MutableStateFlow(false)
    
    /** All photos loaded so far (accumulated across pages) */
    private val _pagedPhotos = MutableStateFlow<List<PhotoEntity>>(emptyList())
    
    /** Set of photo IDs that have been sorted (removed from display) */
    private val _sortedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    
    /** Whether there are more pages to load */
    private var hasMorePages = true
    
    /** Job for loading photos */
    private var loadPhotosJob: Job? = null
    
    /**
     * Counters for sorted photos by status.
     */
    private data class SortCounters(
        val keep: Int = 0,
        val trash: Int = 0,
        val maybe: Int = 0
    ) {
        val total: Int get() = keep + trash + maybe
    }
    
    /**
     * Get filtered photos flow based on current filter mode.
     * Now uses pagination: photos are loaded in batches but sorted correctly at database level.
     * 
     * The flow returns photos that have been loaded so far, minus any that have been sorted.
     */
    private fun getFilteredPhotosFlow(): Flow<List<PhotoEntity>> {
        return combine(
            _pagedPhotos,
            _sortedPhotoIds
        ) { photos, sortedIds ->
            // Filter out photos that have been sorted
            photos.filter { it.id !in sortedIds }
        }
    }
    
    /**
     * Load the initial batch of photos based on current filter mode and sort order.
     * Called when filter mode or sort order changes.
     * 
     * Uses _isReloading flag to prevent "complete" screen flash during reload.
     */
    private fun loadInitialPhotos() {
        loadPhotosJob?.cancel()
        loadPhotosJob = viewModelScope.launch {
            // Set reloading flag BEFORE clearing data to prevent "complete" flash
            _isReloading.value = true
            
            // Reset pagination state
            currentPage = 0
            hasMorePages = true
            _pagedPhotos.value = emptyList()
            _sortedPhotoIds.value = emptySet()
            _isLoadingMore.value = true
            
            // Reset initial total count to recalculate with new filter/sort
            // This ensures accurate total count after filter changes
            _initialTotalCount.value = -1
            
            // Reset session counters (keep/trash/maybe) for the new sort session
            _counters.value = SortCounters()
            
            try {
                val photos = loadPhotosPage(0)
                _pagedPhotos.value = photos
                hasMorePages = photos.size >= GetUnsortedPhotosUseCase.PAGE_SIZE
            } catch (e: Exception) {
                _error.value = "加载照片失败: ${e.message}"
            } finally {
                _isLoadingMore.value = false
                _isLoading.value = false
                _isReloading.value = false
            }
        }
    }
    
    /**
     * Load a specific page of photos based on current filter mode and sort order.
     * 
     * @param page Page number (0-indexed)
     * @param offsetAdjustment Adjustment to offset for pagination accuracy.
     *                         Pass negative value equal to number of photos sorted since last load.
     *                         This compensates for photos that are no longer UNSORTED in DB.
     */
    private suspend fun loadPhotosPage(page: Int, offsetAdjustment: Int = 0): List<PhotoEntity> {
        val filterMode = preferencesRepository.getPhotoFilterMode().stateIn(viewModelScope).value
        val sortOrder = _sortOrder.value
        val cameraIds = _cameraAlbumIds.value
        val sessionFilter = preferencesRepository.getSessionCustomFilterFlow().stateIn(viewModelScope).value
        
        return when (filterMode) {
            PhotoFilterMode.ALL -> {
                getUnsortedPhotosUseCase.getPage(page, sortOrder, randomSeed, offsetAdjustment)
            }
            PhotoFilterMode.CAMERA_ONLY -> {
                if (cameraIds.isNotEmpty()) {
                    getUnsortedPhotosUseCase.getPageByBuckets(cameraIds, page, sortOrder, randomSeed, offsetAdjustment)
                } else {
                    emptyList()
                }
            }
            PhotoFilterMode.EXCLUDE_CAMERA -> {
                if (cameraIds.isNotEmpty()) {
                    getUnsortedPhotosUseCase.getPageExcludingBuckets(cameraIds, page, sortOrder, randomSeed, offsetAdjustment)
                } else {
                    getUnsortedPhotosUseCase.getPage(page, sortOrder, randomSeed, offsetAdjustment)
                }
            }
            PhotoFilterMode.CUSTOM -> {
                if (sessionFilter != null) {
                    getUnsortedPhotosUseCase.getPageFiltered(
                        bucketIds = sessionFilter.albumIds,
                        startDate = sessionFilter.startDate,
                        endDate = sessionFilter.endDate,
                        page = page,
                        sortOrder = sortOrder,
                        randomSeed = randomSeed,
                        offsetAdjustment = offsetAdjustment
                    )
                } else {
                    getUnsortedPhotosUseCase.getPage(page, sortOrder, randomSeed, offsetAdjustment)
                }
            }
        }
    }
    
    /** Debounce job for loadMorePhotosIfNeeded */
    private var loadMoreDebounceJob: Job? = null
    
    /**
     * Load more photos when approaching the end of the current batch.
     * Called automatically when remaining unsorted photos drop below threshold.
     * 
     * IMPORTANT: Uses adjusted offset to account for photos sorted since last load.
     * Without this adjustment, some photos would be skipped when loading next page
     * because the database query only includes UNSORTED photos (sorted ones are excluded).
     * 
     * NOTE: This method uses debouncing to avoid rapid consecutive calls during fast swiping.
     * Errors are silently handled to prevent "加载照片失败" messages during normal operation.
     */
    private fun loadMorePhotosIfNeeded() {
        if (_isLoadingMore.value || !hasMorePages || _isReloading.value) return
        
        val unsortedRemaining = _pagedPhotos.value.count { it.id !in _sortedPhotoIds.value }
        if (unsortedRemaining < GetUnsortedPhotosUseCase.PRELOAD_THRESHOLD) {
            // Cancel any pending load to avoid duplicate requests
            loadMoreDebounceJob?.cancel()
            loadMoreDebounceJob = viewModelScope.launch {
                // Small delay to debounce rapid calls
                delay(50)
                
                if (_isLoadingMore.value || !hasMorePages) return@launch
                
                _isLoadingMore.value = true
                try {
                    // Calculate offset adjustment to account for photos sorted since initial load.
                    // When photos are sorted, they become non-UNSORTED in DB, effectively shifting
                    // the result set. We compensate by reducing offset by the number of sorted photos.
                    val sortedCount = _sortedPhotoIds.value.size
                    val offsetAdjustment = -sortedCount  // Negative to shift offset back
                    
                    currentPage++
                    val morePhotos = loadPhotosPage(currentPage, offsetAdjustment)
                    
                    if (morePhotos.isNotEmpty()) {
                        // Filter out any photos we already have (duplicates due to offset shift)
                        val existingIds = _pagedPhotos.value.map { it.id }.toSet()
                        val newPhotos = morePhotos.filter { it.id !in existingIds }
                        if (newPhotos.isNotEmpty()) {
                            _pagedPhotos.value = _pagedPhotos.value + newPhotos
                        }
                    }
                    hasMorePages = morePhotos.size >= GetUnsortedPhotosUseCase.PAGE_SIZE
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Coroutine was cancelled, don't treat as error
                    currentPage-- // Revert page increment
                    throw e
                } catch (e: Exception) {
                    // Silent error handling for background loading
                    // Don't show error to user for pagination failures
                    currentPage-- // Revert page increment on error
                } finally {
                    _isLoadingMore.value = false
                }
            }
        }
    }
    
    /**
     * Mark a photo as sorted (it will be filtered out from the display list).
     */
    private fun markPhotoAsSorted(photoId: String) {
        _sortedPhotoIds.value = _sortedPhotoIds.value + photoId
        // Check if we need to load more photos
        loadMorePhotosIfNeeded()
    }
    
    /**
     * Helper data class for filter parameters.
     */
    private data class FilterParams(
        val filterMode: PhotoFilterMode,
        val cameraIds: List<String>,
        val cameraLoaded: Boolean,
        val sessionFilter: CustomFilterSession?,
        val sortOrder: PhotoSortOrder
    )
    
    /**
     * Get the REAL count of unsorted photos (not limited by LIMIT 500).
     * Used to display accurate total count in the UI.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getFilteredPhotosCountFlow(): Flow<Int> {
        return combine(
            preferencesRepository.getPhotoFilterMode(),
            _cameraAlbumIds,
            _cameraAlbumsLoaded,
            preferencesRepository.getSessionCustomFilterFlow(),
            _sortOrder
        ) { filterMode, cameraIds, loaded, sessionFilter, sortOrder ->
            FilterParams(filterMode, cameraIds, loaded, sessionFilter, sortOrder)
        }.flatMapLatest { params ->
            when (params.filterMode) {
                PhotoFilterMode.ALL -> getUnsortedPhotosUseCase.getCount()
                PhotoFilterMode.CAMERA_ONLY -> {
                    if (!params.cameraLoaded) {
                        flowOf(0)
                    } else if (params.cameraIds.isNotEmpty()) {
                        getUnsortedPhotosUseCase.getCountByBuckets(params.cameraIds)
                    } else {
                        flowOf(0)
                    }
                }
                PhotoFilterMode.EXCLUDE_CAMERA -> {
                    if (!params.cameraLoaded) {
                        flowOf(0)
                    } else if (params.cameraIds.isNotEmpty()) {
                        getUnsortedPhotosUseCase.getCountExcludingBuckets(params.cameraIds)
                    } else {
                        getUnsortedPhotosUseCase.getCount()
                    }
                }
            PhotoFilterMode.CUSTOM -> {
                val sessionFilter = params.sessionFilter
                if (sessionFilter != null) {
                    getUnsortedPhotosUseCase.getCountFiltered(
                        sessionFilter.albumIds,
                        sessionFilter.startDate,
                        sessionFilter.endDate
                    )
                } else {
                    getUnsortedPhotosUseCase.getCount()
                }
            }
        }
    }
    }
    
    /**
     * UI State exposed to the screen.
     */
    val uiState: StateFlow<FlowSorterUiState> = combine(
        getFilteredPhotosFlow(),
        getFilteredPhotosCountFlow(), // Add real count flow
        combine(_isLoading, _isSyncing, _isReloading) { loading, syncing, reloading -> Triple(loading, syncing, reloading) },
        _currentIndex,
        _lastAction,
        combine(_error, _counters, _combo) { e, c, co -> Triple(e, c, co) },
        _viewMode,
        combine(_selectedPhotoIds, _sortOrder, _gridColumns) { ids, order, cols -> Triple(ids, order, cols) },
        combine(
            preferencesRepository.getPhotoFilterMode(), 
            _cameraAlbumsLoaded, 
            preferencesRepository.getCardZoomEnabled(),
            preferencesRepository.getSwipeSensitivity()
        ) { mode, loaded, zoom, sensitivity -> 
            // Use Pair of Pairs for 4 values: ((mode, loaded), (zoom, sensitivity))
            Pair(Pair(mode, loaded), Pair(zoom, sensitivity))
        },
        if (isDailyTask) getDailyTaskStatusUseCase() else flowOf(null)
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        var photos = values[0] as List<PhotoEntity>
        val realUnsortedCount = values[1] as Int  // Real count from COUNT query (not limited)
        val loadingStates = values[2] as Triple<Boolean, Boolean, Boolean>
        val isLoading = loadingStates.first
        val isSyncing = loadingStates.second
        val isReloading = loadingStates.third
        val currentIndex = values[3] as Int
        val lastAction = values[4] as SortAction?
        val combined = values[5] as Triple<String?, SortCounters, ComboState>
        val viewMode = values[6] as FlowSorterViewMode
        val selectionAndSortAndCols = values[7] as Triple<Set<String>, PhotoSortOrder, Int>
        @Suppress("UNCHECKED_CAST")
        val prefsState = values[8] as Pair<Pair<PhotoFilterMode, Boolean>, Pair<Boolean, Float>>
        val dailyStatus = values[9] as com.example.photozen.domain.usecase.DailyTaskStatus?
        
        val error = combined.first
        val counters = combined.second
        val combo = combined.third
        val selectedIds = selectionAndSortAndCols.first
        val sortOrder = selectionAndSortAndCols.second
        val gridColumns = selectionAndSortAndCols.third
        val filterMode = prefsState.first.first
        val cameraLoaded = prefsState.first.second
        val cardZoomEnabled = prefsState.second.first
        val swipeSensitivity = prefsState.second.second
        
        var isDailyComplete = false
        var dailyCurrent = 0
        
        // Handle Daily Task Logic
        if (isDailyTask && dailyStatus != null) {
            dailyCurrent = dailyStatus.current
            val needed = (targetCount - dailyCurrent).coerceAtLeast(0)
            
            if (needed == 0) {
                isDailyComplete = true
            }
            
            // Limit photos to needed count + buffer (to avoid empty list flickering if close)
            // But requirement says "list only loads corresponding number of photos".
            // If needed is 5, we show 5.
            if (photos.size > needed) {
                photos = photos.take(needed)
            }
        }
        
        // Apply sorting to photos
        val sortedPhotos = applySortOrder(photos, sortOrder)
        
        // Show loading if camera filter is active but not loaded yet
        val shouldShowLoading = isLoading || 
            ((filterMode == PhotoFilterMode.CAMERA_ONLY || filterMode == PhotoFilterMode.EXCLUDE_CAMERA) && !cameraLoaded)
        
        // Use REAL unsorted count + sorted count for total
        // This ensures we show the actual total, not limited by LIMIT 500
        val realTotalCount = realUnsortedCount + counters.total
        
        // Use stable total count to prevent flickering during rapid swipes
        if (_initialTotalCount.value < 0 && realTotalCount > 0) {
            _initialTotalCount.value = realTotalCount
        }
        val stableTotalCount = if (_initialTotalCount.value >= 0) {
            _initialTotalCount.value
        } else {
            realTotalCount
        }
        
        FlowSorterUiState(
            photos = sortedPhotos,
            currentIndex = 0, // Always 0 since we remove sorted photos from list
            isLoading = shouldShowLoading && sortedPhotos.isEmpty(),
            isSyncing = isSyncing,
            isReloading = isReloading,
            totalCount = stableTotalCount,
            sortedCount = counters.total,
            keepCount = counters.keep,
            trashCount = counters.trash,
            maybeCount = counters.maybe,
            lastAction = lastAction,
            error = error,
            combo = combo,
            viewMode = viewMode,
            selectedPhotoIds = selectedIds,
            sortOrder = sortOrder,
            gridColumns = gridColumns,
            filterMode = filterMode,
            cameraAlbumsLoaded = cameraLoaded,
            isDailyTask = isDailyTask,
            dailyTaskTarget = targetCount,
            dailyTaskCurrent = dailyCurrent,
            isDailyTaskComplete = isDailyComplete,
            cardZoomEnabled = cardZoomEnabled,
            swipeSensitivity = swipeSensitivity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FlowSorterUiState()
    )
    
    /**
     * Apply sort order to photos list.
     * NOTE: All sorting (DATE_ASC, DATE_DESC, RANDOM) is now handled at database level
     * for correct pagination across the entire dataset.
     * This method now just returns the photos as-is since they're already sorted.
     */
    private fun applySortOrder(photos: List<PhotoEntity>, sortOrder: PhotoSortOrder): List<PhotoEntity> {
        // All sorting is done at database level using LIMIT/OFFSET pagination
        // This ensures correct sorting across ALL photos, not just the current batch
        return photos
    }
    
    init {
        // Load camera album IDs first, then sync, then load photos
        viewModelScope.launch {
            loadCameraAlbumIds()
            syncPhotos()
            // Load initial batch of photos after sync
            loadInitialPhotos()
        }
        // Load grid columns preference
        viewModelScope.launch {
            preferencesRepository.getGridColumns(PreferencesRepository.GridScreen.FLOW).collect { columns ->
                _gridColumns.value = columns
            }
        }
        // Monitor filter mode changes and reload photos
        viewModelScope.launch {
            preferencesRepository.getPhotoFilterMode().collect { _ ->
                // Reload photos when filter mode changes
                if (_cameraAlbumsLoaded.value) {
                    loadInitialPhotos()
                }
            }
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
     * Sync photos from MediaStore.
     */
    fun syncPhotos() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = syncPhotosUseCase()
                if (result.isInitialSync) {
                    // Reset counters on initial sync
                    _counters.value = SortCounters()
                    // Reset initial total count to recalculate on next photos load
                    _initialTotalCount.value = -1
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
     * Keep a photo by ID.
     * CRITICAL: This method uses the provided photoId instead of currentPhoto,
     * ensuring correct photo is processed during rapid swiping.
     * @param photoId The ID of the photo to keep
     * @return The current combo count after sorting
     */
    fun keepPhoto(photoId: String): Int {
        if (photoId.isEmpty()) return 0
        val comboCount = updateCombo()
        // Mark photo as sorted immediately (removes from display list)
        markPhotoAsSorted(photoId)
        viewModelScope.launch {
            try {
                sortPhotoUseCase.keepPhoto(photoId)
                _lastAction.value = SortAction(photoId, PhotoStatus.KEEP)
                _counters.value = _counters.value.copy(keep = _counters.value.keep + 1)
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementKeepCount()
                // Update widget immediately after sorting
                widgetUpdater.updateDailyProgressWidgets()
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
        return comboCount
    }
    
    /**
     * Trash a photo by ID.
     * @param photoId The ID of the photo to trash
     * @return The current combo count after sorting
     */
    fun trashPhoto(photoId: String): Int {
        if (photoId.isEmpty()) return 0
        val comboCount = updateCombo()
        // Mark photo as sorted immediately (removes from display list)
        markPhotoAsSorted(photoId)
        viewModelScope.launch {
            try {
                sortPhotoUseCase.trashPhoto(photoId)
                _lastAction.value = SortAction(photoId, PhotoStatus.TRASH)
                _counters.value = _counters.value.copy(trash = _counters.value.trash + 1)
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementTrashCount()
                // Update widget immediately after sorting
                widgetUpdater.updateDailyProgressWidgets()
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
        return comboCount
    }
    
    /**
     * Mark a photo as Maybe by ID.
     * @param photoId The ID of the photo to mark as maybe
     * @return The current combo count after sorting
     */
    fun maybePhoto(photoId: String): Int {
        if (photoId.isEmpty()) return 0
        val comboCount = updateCombo()
        // Mark photo as sorted immediately (removes from display list)
        markPhotoAsSorted(photoId)
        viewModelScope.launch {
            try {
                sortPhotoUseCase.maybePhoto(photoId)
                _lastAction.value = SortAction(photoId, PhotoStatus.MAYBE)
                _counters.value = _counters.value.copy(maybe = _counters.value.maybe + 1)
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementMaybeCount()
                // Update widget immediately after sorting
                widgetUpdater.updateDailyProgressWidgets()
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
        return comboCount
    }
    
    /**
     * Keep the current photo (swipe right).
     * @deprecated Use keepPhoto(photoId) instead for reliable operation during rapid swiping.
     * @return The current combo count after sorting
     */
    fun keepCurrentPhoto(): Int {
        val photo = uiState.value.currentPhoto ?: return 0
        return keepPhoto(photo.id)
    }
    
    /**
     * Trash the current photo (swipe up).
     * @deprecated Use trashPhoto(photoId) instead for reliable operation during rapid swiping.
     * @return The current combo count after sorting
     */
    fun trashCurrentPhoto(): Int {
        val photo = uiState.value.currentPhoto ?: return 0
        return trashPhoto(photo.id)
    }
    
    /**
     * Mark current photo as Maybe (swipe down).
     * @deprecated Use maybePhoto(photoId) instead for reliable operation during rapid swiping.
     * @return The current combo count after sorting
     */
    fun maybeCurrentPhoto(): Int {
        val photo = uiState.value.currentPhoto ?: return 0
        return maybePhoto(photo.id)
    }
    
    /**
     * Undo the last sorting action.
     * Note: Undo does NOT decrement the cumulative count - it's a permanent achievement.
     */
    fun undoLastAction() {
        val lastAction = _lastAction.value ?: return
        viewModelScope.launch {
            try {
                sortPhotoUseCase.resetPhoto(lastAction.photoId)
                // Remove from sorted list so it reappears in the display
                _sortedPhotoIds.value = _sortedPhotoIds.value - lastAction.photoId
                // Update session counters only (cumulative count stays)
                _counters.value = when (lastAction.status) {
                    PhotoStatus.KEEP -> _counters.value.copy(keep = (_counters.value.keep - 1).coerceAtLeast(0))
                    PhotoStatus.TRASH -> _counters.value.copy(trash = (_counters.value.trash - 1).coerceAtLeast(0))
                    PhotoStatus.MAYBE -> _counters.value.copy(maybe = (_counters.value.maybe - 1).coerceAtLeast(0))
                    else -> _counters.value
                }
                _lastAction.value = null
            } catch (e: Exception) {
                _error.value = "撤销失败: ${e.message}"
            }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Switch between card and list view mode.
     */
    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == FlowSorterViewMode.CARD) {
            FlowSorterViewMode.LIST
        } else {
            FlowSorterViewMode.CARD
        }
        // Clear selection when switching modes
        _selectedPhotoIds.value = emptySet()
    }
    
    /**
     * Set sort order for photos.
     * Reloads photos with new sort order applied at database level.
     */
    fun setSortOrder(order: PhotoSortOrder) {
        // If switching to random, generate new seed
        if (order == PhotoSortOrder.RANDOM) {
            randomSeed = System.currentTimeMillis()
        }
        _sortOrder.value = order
        // Reload photos with new sort order
        loadInitialPhotos()
    }
    
    /**
     * Cycle through sort orders: DATE_DESC -> DATE_ASC -> RANDOM -> DATE_DESC
     */
    fun cycleSortOrder() {
        val nextOrder = when (_sortOrder.value) {
            PhotoSortOrder.DATE_DESC -> PhotoSortOrder.DATE_ASC
            PhotoSortOrder.DATE_ASC -> PhotoSortOrder.RANDOM
            PhotoSortOrder.RANDOM -> PhotoSortOrder.DATE_DESC
        }
        setSortOrder(nextOrder)
    }
    
    /**
     * Cycle grid columns: 2 -> 3 -> 1 -> 2
     */
    fun cycleGridColumns() {
        viewModelScope.launch {
            val newColumns = preferencesRepository.cycleGridColumns(PreferencesRepository.GridScreen.FLOW)
            _gridColumns.value = newColumns
        }
    }
    
    /**
     * Update selected photo IDs.
     */
    fun updateSelection(selectedIds: Set<String>) {
        _selectedPhotoIds.value = selectedIds
    }
    
    /**
     * Clear all selections.
     */
    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }
    
    /**
     * Select all photos.
     */
    fun selectAll() {
        _selectedPhotoIds.value = uiState.value.photos.map { it.id }.toSet()
    }
    
    /**
     * Batch keep selected photos.
     * Clears selection first to prevent UI crash during batch operation.
     */
    fun keepSelectedPhotos() {
        val selectedIds = _selectedPhotoIds.value.toList()
        if (selectedIds.isEmpty()) return
        
        // Clear selection immediately to prevent UI accessing removed items
        _selectedPhotoIds.value = emptySet()
        
        viewModelScope.launch {
            try {
                // Use batch operation for better performance and atomicity
                sortPhotoUseCase.batchUpdateStatus(selectedIds, PhotoStatus.KEEP)
                _counters.value = _counters.value.copy(keep = _counters.value.keep + selectedIds.size)
                preferencesRepository.incrementSortedCount(selectedIds.size)
            } catch (e: Exception) {
                _error.value = "批量操作失败: ${e.message}"
            }
        }
    }
    
    /**
     * Batch trash selected photos.
     * Clears selection first to prevent UI crash during batch operation.
     */
    fun trashSelectedPhotos() {
        val selectedIds = _selectedPhotoIds.value.toList()
        if (selectedIds.isEmpty()) return
        
        // Clear selection immediately to prevent UI accessing removed items
        _selectedPhotoIds.value = emptySet()
        
        viewModelScope.launch {
            try {
                // Use batch operation for better performance and atomicity
                sortPhotoUseCase.batchUpdateStatus(selectedIds, PhotoStatus.TRASH)
                _counters.value = _counters.value.copy(trash = _counters.value.trash + selectedIds.size)
                preferencesRepository.incrementSortedCount(selectedIds.size)
            } catch (e: Exception) {
                _error.value = "批量操作失败: ${e.message}"
            }
        }
    }
    
    /**
     * Batch maybe selected photos.
     * Clears selection first to prevent UI crash during batch operation.
     */
    fun maybeSelectedPhotos() {
        val selectedIds = _selectedPhotoIds.value.toList()
        if (selectedIds.isEmpty()) return
        
        // Clear selection immediately to prevent UI accessing removed items
        _selectedPhotoIds.value = emptySet()
        
        viewModelScope.launch {
            try {
                // Use batch operation for better performance and atomicity
                sortPhotoUseCase.batchUpdateStatus(selectedIds, PhotoStatus.MAYBE)
                _counters.value = _counters.value.copy(maybe = _counters.value.maybe + selectedIds.size)
                preferencesRepository.incrementSortedCount(selectedIds.size)
            } catch (e: Exception) {
                _error.value = "批量操作失败: ${e.message}"
            }
        }
    }
    
    /**
     * Update combo state based on time since last swipe.
     * Called after each successful sort action.
     * Starts auto-hide timer for combo display.
     * 
     * @return The new combo count (for haptic feedback intensity)
     */
    private fun updateCombo(): Int {
        val currentTime = System.currentTimeMillis()
        val lastTime = _combo.value.lastSwipeTime
        val timeSinceLastSwipe = currentTime - lastTime
        
        val newCombo = if (lastTime == 0L || timeSinceLastSwipe <= ComboState.COMBO_THRESHOLD_MS) {
            // Within threshold - increment combo
            _combo.value.count + 1
        } else {
            // Too slow - reset to 1
            1
        }
        
        val newMaxCombo = maxOf(_combo.value.maxCount, newCombo)
        
        _combo.value = _combo.value.copy(
            count = newCombo,
            maxCount = newMaxCombo,
            lastSwipeTime = currentTime,
            isActive = true
        )
        
        // Update global max combo achievement
        viewModelScope.launch {
            preferencesRepository.updateMaxCombo(newMaxCombo)
        }
        
        // Cancel previous timeout and start new one
        startComboTimeout()
        
        return newCombo
    }
    
    /**
     * Start a timeout to auto-hide combo after COMBO_THRESHOLD_MS.
     * If user doesn't continue swiping, combo will reset and hide.
     */
    private fun startComboTimeout() {
        comboTimeoutJob?.cancel()
        comboTimeoutJob = viewModelScope.launch {
            delay(ComboState.COMBO_THRESHOLD_MS)
            // Time expired without new swipe - hide combo
            _combo.value = _combo.value.copy(isActive = false)
            // After a brief moment, reset the count for next session
            delay(300)
            _combo.value = ComboState(maxCount = _combo.value.maxCount)
        }
    }
    
    /**
     * Reset combo (e.g., when leaving the screen or after timeout).
     */
    fun resetCombo() {
        comboTimeoutJob?.cancel()
        _combo.value = ComboState()
    }
    
    /**
     * Get the current combo count.
     */
    fun getCurrentCombo(): Int = _combo.value.count
    
    /**
     * Get the max combo achieved in this session.
     */
    fun getMaxCombo(): Int = _combo.value.maxCount
    
    override fun onCleared() {
        super.onCleared()
        comboTimeoutJob?.cancel()
    }
}
