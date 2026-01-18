package com.example.photozen.ui.screens.flowsorter

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoSortOrder
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.AlbumAddAction
import com.example.photozen.data.repository.CustomFilterSession
import com.example.photozen.data.repository.PhotoClassificationMode
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.GetDailyTaskStatusUseCase
import com.example.photozen.domain.usecase.GetUnsortedPhotosUseCase
import com.example.photozen.domain.usecase.MovePhotoResult
import com.example.photozen.domain.usecase.SortPhotoUseCase
import com.example.photozen.domain.usecase.SyncPhotosUseCase
import com.example.photozen.util.StoragePermissionHelper
import com.example.photozen.util.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FlowSorterVM"

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
    val swipeSensitivity: Float = 1.0f,
    // Album mode support
    val cardSortingAlbumEnabled: Boolean = false,
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumTagSize: Float = 1.0f,
    val maxAlbumTagCount: Int = 0,
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val albumMessage: String? = null,
    // Permission dialog state
    val showPermissionDialog: Boolean = false,
    val permissionRetryError: Boolean = false,
    val pendingAlbumBucketId: String? = null
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
    private val widgetUpdater: WidgetUpdater,
    private val albumBubbleDao: AlbumBubbleDao,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val storagePermissionHelper: StoragePermissionHelper
) : ViewModel() {
    
    private val isDailyTask: Boolean = savedStateHandle["isDailyTask"] ?: false
    private val targetCount: Int = savedStateHandle["targetCount"] ?: -1
    private val albumBucketId: String? = savedStateHandle["albumBucketId"]
    private val initialListMode: Boolean = savedStateHandle["initialListMode"] ?: false
    
    private val _isLoading = MutableStateFlow(true)
    private val _isSyncing = MutableStateFlow(false)
    private val _currentIndex = MutableStateFlow(0)
    private val _lastAction = MutableStateFlow<SortAction?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _counters = MutableStateFlow(SortCounters())
    private val _combo = MutableStateFlow(ComboState())
    
    // Immediate sorted count - updated SYNCHRONOUSLY on swipe for instant UI feedback
    // This bypasses the combine flow delay that can cause first-swipe counter issues
    private val _sortedCountImmediate = MutableStateFlow(0)
    val sortedCountImmediate: StateFlow<Int> = _sortedCountImmediate.asStateFlow()
    // Initialize view mode based on navigation parameter
    private val _viewMode = MutableStateFlow(
        if (initialListMode) FlowSorterViewMode.LIST else FlowSorterViewMode.CARD
    )
    private val _selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    private val _sortOrder = MutableStateFlow(PhotoSortOrder.DATE_DESC)
    private val _gridColumns = MutableStateFlow(2)
    
    // Album mode state
    private val _cardSortingAlbumEnabled = MutableStateFlow(false)
    private val _albumBubbleList = MutableStateFlow<List<AlbumBubbleEntity>>(emptyList())
    private val _albumTagSize = MutableStateFlow(1.0f)
    private val _maxAlbumTagCount = MutableStateFlow(0)
    private val _albumAddAction = MutableStateFlow(AlbumAddAction.MOVE)
    private val _albumMessage = MutableStateFlow<String?>(null)
    
    // Permission dialog state
    private val _showPermissionDialog = MutableStateFlow(false)
    private val _permissionRetryError = MutableStateFlow(false)
    private val _pendingAlbumBucketId = MutableStateFlow<String?>(null)
    private var _pendingPhotoId: String? = null  // Photo waiting for permission
    
    // System albums state (for album picker)
    private val _availableAlbums = MutableStateFlow<List<com.example.photozen.data.source.Album>>(emptyList())
    val availableAlbums: StateFlow<List<com.example.photozen.data.source.Album>> = _availableAlbums.asStateFlow()
    
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
    
    // Unified Pagination Strategy:
    // We fetch ALL matching photo IDs (sorted globally by DB) into memory (Snapshot).
    // This ensures:
    // 1. Consistent sorting order (Global Sort).
    // 2. Stability during pagination (Snapshot).
    // 3. Random sort works by shuffling this list.
    // 4. Date sort works by keeping the DB order (or reversing for ASC).
    private var currentSessionIds: List<String>? = null
    
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
            val filtered = photos.filter { it.id !in sortedIds }
            Log.d(TAG, "getFilteredPhotosFlow: pagedPhotos=${photos.size}, sortedIds=${sortedIds.size}, filtered=${filtered.size}")
            filtered
        }
    }
    
    /**
     * Load the initial batch of photos based on current filter mode and sort order.
     * Called when filter mode or sort order changes.
     * 
     * Uses _isReloading flag to prevent "complete" screen flash during reload.
     * IMPORTANT: Uses flow.first() to get the CURRENT values at call time,
     * ensuring correct filter/sort parameters are used (especially for CUSTOM mode).
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
            currentSessionIds = null // Reset snapshot
            
            // Reset initial total count to recalculate with new filter/sort
            // This ensures accurate total count after filter changes
            _initialTotalCount.value = -1
            
            // Reset session counters (keep/trash/maybe) for the new sort session
            _counters.value = SortCounters()
            _sortedCountImmediate.value = 0  // Reset immediate counter too
            
            try {
                // Get CURRENT values using first() to ensure correct parameters
                // This is critical for CUSTOM mode where sessionFilter must be correct
                val filterMode = preferencesRepository.getPhotoFilterMode().first()
                val sessionFilter = preferencesRepository.getSessionCustomFilterFlow().first()
                val sortOrder = _sortOrder.value
                val cameraIds = _cameraAlbumIds.value
                
                // Initialize the ID snapshot for ALL sort modes
                // This guarantees global sorting and stability
                // If albumBucketId is set, override filter to only show photos from that album
                val allIds = if (albumBucketId != null) {
                    getUnsortedPhotosUseCase.getAllIds(
                        com.example.photozen.data.repository.PhotoFilterMode.CAMERA_ONLY,
                        listOf(albumBucketId),
                        null,
                        sortOrder
                    )
                } else {
                    getUnsortedPhotosUseCase.getAllIds(
                        filterMode, 
                        cameraIds, 
                        sessionFilter,
                        sortOrder
                    )
                }
                
                // For RANDOM, shuffle the list
                currentSessionIds = if (sortOrder == PhotoSortOrder.RANDOM) {
                    allIds.shuffled(java.util.Random(randomSeed))
                } else {
                    allIds
                }
                
                val photos = loadPhotosPage(
                    page = 0,
                    filterMode = filterMode,
                    sessionFilter = sessionFilter,
                    sortOrder = sortOrder
                )
                _pagedPhotos.value = photos
                hasMorePages = photos.size >= GetUnsortedPhotosUseCase.PAGE_SIZE
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Coroutine was cancelled (e.g., by a new loadInitialPhotos call)
                // This is expected behavior, don't show error to user
                throw e
            } catch (e: Exception) {
                // Only show error if we're not being cancelled
                if (loadPhotosJob?.isActive == true) {
                    _error.value = "加载照片失败: ${e.message}"
                }
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
     * Uses explicit parameters to ensure correct values are used (especially after sort/filter changes).
     * This is critical for CUSTOM filter mode where sessionFilter must be read correctly.
     * 
     * @param page Page number (0-indexed)
     * @param filterMode Current filter mode
     * @param sessionFilter Current custom filter session (for CUSTOM mode)
     * @param sortOrder Current sort order
     * @param offsetAdjustment IGNORED. We use snapshot pagination now.
     */
    private suspend fun loadPhotosPage(
        page: Int,
        filterMode: PhotoFilterMode,
        sessionFilter: CustomFilterSession?,
        sortOrder: PhotoSortOrder,
        offsetAdjustment: Int = 0
    ): List<PhotoEntity> {
        // Unified Snapshot Pagination for ALL modes (Random, Date, etc.)
        // This ensures consistent global sorting and stability.
        val allIds = currentSessionIds ?: return emptyList()
        
        // Calculate slice range based on page
        val fromIndex = page * GetUnsortedPhotosUseCase.PAGE_SIZE
        if (fromIndex >= allIds.size) return emptyList()
        
        val toIndex = minOf(fromIndex + GetUnsortedPhotosUseCase.PAGE_SIZE, allIds.size)
        // Ensure valid sublist range
        if (fromIndex >= toIndex) return emptyList()
        
        val pageIds = allIds.subList(fromIndex, toIndex)
        return getUnsortedPhotosUseCase.getPhotosByIds(pageIds)
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
     * Uses flow.first() to get current filter/sort values for correct pagination.
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
                    // Get CURRENT filter/sort values using first()
                    val filterMode = preferencesRepository.getPhotoFilterMode().first()
                    val sessionFilter = preferencesRepository.getSessionCustomFilterFlow().first()
                    val sortOrder = _sortOrder.value
                    
                    // Unified Snapshot Pagination: No offset adjustment needed
                    // Just load the next page of IDs from the snapshot
                    val offsetAdjustment = 0
                    
                    currentPage++
                    val morePhotos = loadPhotosPage(
                        page = currentPage,
                        filterMode = filterMode,
                        sessionFilter = sessionFilter,
                        sortOrder = sortOrder,
                        offsetAdjustment = offsetAdjustment
                    )
                    
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
     * If albumBucketId is set, only count photos from that album.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getFilteredPhotosCountFlow(): Flow<Int> {
        // If filtering by specific album, only count photos from that album
        if (albumBucketId != null) {
            return getUnsortedPhotosUseCase.getCountByBuckets(listOf(albumBucketId))
        }
        
        return combine(
            preferencesRepository.getPhotoFilterMode(),
            _cameraAlbumIds,
            _cameraAlbumsLoaded,
            preferencesRepository.getSessionCustomFilterFlow(),
            _sortOrder
        ) { filterMode, cameraIds, loaded, sessionFilter, sortOrder ->
            FilterParams(filterMode, cameraIds, loaded, sessionFilter, sortOrder)
        }.flatMapLatest { params ->
            // Priority 1: If sessionFilter has preciseMode=true, use it directly
            // This allows timeline sorting to work without changing the global filter mode
            val sessionFilter = params.sessionFilter
            if (sessionFilter?.preciseMode == true) {
                return@flatMapLatest getUnsortedPhotosUseCase.getCountFiltered(
                    sessionFilter.albumIds,
                    sessionFilter.startDate,
                    sessionFilter.endDate
                )
            }
            
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
                if (sessionFilter != null) {
                    // Calculate effective end date based on preciseMode (non-precise uses day extension)
                    val effectiveEndDateMs = sessionFilter.endDate?.let { it + 86400L * 1000 - 1 }
                    getUnsortedPhotosUseCase.getCountFiltered(
                        sessionFilter.albumIds,
                        sessionFilter.startDate,
                        effectiveEndDateMs
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
    // Combined album mode state flow
    private val albumStateFlow: Flow<AlbumModeState> = combine(
        _cardSortingAlbumEnabled,
        _albumBubbleList,
        _albumTagSize,
        _maxAlbumTagCount,
        combine(_albumAddAction, _albumMessage, _showPermissionDialog, _permissionRetryError, _pendingAlbumBucketId) { action, msg, showDialog, retryError, pendingBucket -> 
            AlbumExtendedState(action, msg, showDialog, retryError, pendingBucket) 
        }
    ) { enabled, albums, size, count, extState ->
        AlbumModeState(enabled, albums, size, count, extState.albumAddAction, extState.albumMessage, 
            extState.showPermissionDialog, extState.permissionRetryError, extState.pendingAlbumBucketId)
    }
    
    private data class AlbumExtendedState(
        val albumAddAction: AlbumAddAction,
        val albumMessage: String?,
        val showPermissionDialog: Boolean,
        val permissionRetryError: Boolean,
        val pendingAlbumBucketId: String?
    )
    
    private data class AlbumModeState(
        val cardSortingAlbumEnabled: Boolean,
        val albumBubbleList: List<AlbumBubbleEntity>,
        val albumTagSize: Float,
        val maxAlbumTagCount: Int,
        val albumAddAction: AlbumAddAction,
        val albumMessage: String?,
        val showPermissionDialog: Boolean,
        val permissionRetryError: Boolean,
        val pendingAlbumBucketId: String?
    )
    
    private data class SelectionAndAlbumState(
        val selectedPhotoIds: Set<String>,
        val sortOrder: PhotoSortOrder,
        val gridColumns: Int,
        val albumState: AlbumModeState
    )
    
    val uiState: StateFlow<FlowSorterUiState> = combine(
        getFilteredPhotosFlow(),
        getFilteredPhotosCountFlow(), // Add real count flow
        combine(_isLoading, _isSyncing, _isReloading) { loading, syncing, reloading -> Triple(loading, syncing, reloading) },
        _currentIndex,
        _lastAction,
        combine(_error, _counters, _combo) { e, c, co -> Triple(e, c, co) },
        _viewMode,
        combine(_selectedPhotoIds, _sortOrder, _gridColumns, albumStateFlow) { ids, order, cols, album -> 
            SelectionAndAlbumState(ids, order, cols, album) 
        },
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
        val selectionAndAlbum = values[7] as SelectionAndAlbumState
        @Suppress("UNCHECKED_CAST")
        val prefsState = values[8] as Pair<Pair<PhotoFilterMode, Boolean>, Pair<Boolean, Float>>
        val dailyStatus = values[9] as com.example.photozen.domain.usecase.DailyTaskStatus?
        
        val error = combined.first
        val counters = combined.second
        val combo = combined.third
        val selectedIds = selectionAndAlbum.selectedPhotoIds
        val sortOrder = selectionAndAlbum.sortOrder
        val gridColumns = selectionAndAlbum.gridColumns
        val albumState = selectionAndAlbum.albumState
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
            swipeSensitivity = swipeSensitivity,
            // Album mode
            cardSortingAlbumEnabled = albumState.cardSortingAlbumEnabled,
            albumBubbleList = albumState.albumBubbleList,
            albumTagSize = albumState.albumTagSize,
            maxAlbumTagCount = albumState.maxAlbumTagCount,
            albumAddAction = albumState.albumAddAction,
            albumMessage = albumState.albumMessage,
            // Permission dialog
            showPermissionDialog = albumState.showPermissionDialog,
            permissionRetryError = albumState.permissionRetryError,
            pendingAlbumBucketId = albumState.pendingAlbumBucketId
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
        // Load album mode settings
        viewModelScope.launch {
            preferencesRepository.getCardSortingAlbumEnabled().collect { enabled ->
                _cardSortingAlbumEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            preferencesRepository.getAlbumTagSize().collect { size ->
                _albumTagSize.value = size
            }
        }
        viewModelScope.launch {
            preferencesRepository.getMaxAlbumTagCount().collect { count ->
                _maxAlbumTagCount.value = count
            }
        }
        viewModelScope.launch {
            preferencesRepository.getAlbumAddAction().collect { action ->
                _albumAddAction.value = action
            }
        }
        viewModelScope.launch {
            albumBubbleDao.getAll().collect { albums ->
                _albumBubbleList.value = albums
            }
        }
        // Monitor filter mode changes and reload photos
        // CRITICAL: 
        // 1. Use drop(1) to skip initial emission (we already call loadInitialPhotos above)
        // 2. Use distinctUntilChanged() to prevent reloads when DataStore emits 
        //    due to unrelated preference changes (e.g., incrementSortedCount)
        viewModelScope.launch {
            preferencesRepository.getPhotoFilterMode()
                .drop(1) // Skip initial emission - we handle initial load explicitly
                .distinctUntilChanged()
                .collect { _ ->
                    if (_cameraAlbumsLoaded.value) {
                        loadInitialPhotos()
                    }
                }
        }
        // Monitor sessionFilter changes and reload photos when in CUSTOM mode
        // This ensures sorting is applied to ALL photos when filter criteria change
        viewModelScope.launch {
            preferencesRepository.getSessionCustomFilterFlow()
                .drop(1) // Skip initial emission
                .distinctUntilChanged()
                .collect { _ ->
                    val mode = preferencesRepository.getPhotoFilterMode().first()
                    if (mode == PhotoFilterMode.CUSTOM && _cameraAlbumsLoaded.value) {
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
                    _sortedCountImmediate.value = 0  // Reset immediate counter too
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
        Log.d(TAG, "keepPhoto: photoId=$photoId, sortedCountImmediate before=${_sortedCountImmediate.value}")
        val comboCount = updateCombo()
        // Mark photo as sorted immediately (removes from display list)
        markPhotoAsSorted(photoId)
        // Update counters SYNCHRONOUSLY to ensure UI reflects change immediately
        _counters.value = _counters.value.copy(keep = _counters.value.keep + 1)
        _sortedCountImmediate.value++  // Immediate counter for UI
        Log.d(TAG, "keepPhoto: sortedCountImmediate after=${_sortedCountImmediate.value}")
        
        viewModelScope.launch {
            try {
                sortPhotoUseCase.keepPhoto(photoId)
                _lastAction.value = SortAction(photoId, PhotoStatus.KEEP)
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementKeepCount()
                // Update widget immediately after sorting
                widgetUpdater.updateDailyProgressWidgets()
            } catch (e: Exception) {
                // Rollback counters on error
                _counters.value = _counters.value.copy(keep = (_counters.value.keep - 1).coerceAtLeast(0))
                _sortedCountImmediate.value = (_sortedCountImmediate.value - 1).coerceAtLeast(0)
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
        // Update counters SYNCHRONOUSLY to ensure UI reflects change immediately
        _counters.value = _counters.value.copy(trash = _counters.value.trash + 1)
        _sortedCountImmediate.value++  // Immediate counter for UI
        
        viewModelScope.launch {
            try {
                sortPhotoUseCase.trashPhoto(photoId)
                _lastAction.value = SortAction(photoId, PhotoStatus.TRASH)
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementTrashCount()
                // Update widget immediately after sorting
                widgetUpdater.updateDailyProgressWidgets()
            } catch (e: Exception) {
                // Rollback counters on error
                _counters.value = _counters.value.copy(trash = (_counters.value.trash - 1).coerceAtLeast(0))
                _sortedCountImmediate.value = (_sortedCountImmediate.value - 1).coerceAtLeast(0)
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
        // Update counters SYNCHRONOUSLY to ensure UI reflects change immediately
        _counters.value = _counters.value.copy(maybe = _counters.value.maybe + 1)
        _sortedCountImmediate.value++  // Immediate counter for UI
        
        viewModelScope.launch {
            try {
                sortPhotoUseCase.maybePhoto(photoId)
                _lastAction.value = SortAction(photoId, PhotoStatus.MAYBE)
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementMaybeCount()
                // Update widget immediately after sorting
                widgetUpdater.updateDailyProgressWidgets()
            } catch (e: Exception) {
                // Rollback counters on error
                _counters.value = _counters.value.copy(maybe = (_counters.value.maybe - 1).coerceAtLeast(0))
                _sortedCountImmediate.value = (_sortedCountImmediate.value - 1).coerceAtLeast(0)
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
     * Clear album message.
     */
    fun clearAlbumMessage() {
        _albumMessage.value = null
    }
    
    /**
     * Keep current photo and add it to an album.
     * The action (copy or move) is determined by albumAddAction setting.
     * 
     * For MOVE action on Android 11+, checks if MANAGE_EXTERNAL_STORAGE permission is granted.
     * If not, shows a dialog prompting the user to grant permission.
     */
    fun keepAndAddToAlbum(bucketId: String) {
        val photo = uiState.value.currentPhoto ?: return
        
        // For MOVE action, check permission first
        if (_albumAddAction.value == AlbumAddAction.MOVE && 
            storagePermissionHelper.isManageStoragePermissionApplicable() &&
            !storagePermissionHelper.hasManageStoragePermission()) {
            // Save pending operation and show permission dialog
            _pendingPhotoId = photo.id
            _pendingAlbumBucketId.value = bucketId
            _permissionRetryError.value = false
            _showPermissionDialog.value = true
            return
        }
        
        // Execute the operation
        executeKeepAndAddToAlbum(photo.id, bucketId)
    }
    
    /**
     * Execute the keep and add to album operation.
     * Called directly or after permission is granted.
     */
    private fun executeKeepAndAddToAlbum(photoId: String, bucketId: String) {
        val photo = uiState.value.photos.find { it.id == photoId } ?: return
        
        // Mark as sorted to prevent re-display
        markPhotoAsSorted(photo.id)
        
        viewModelScope.launch {
            try {
                // First, mark as KEEP
                sortPhotoUseCase.keepPhoto(photo.id)
                _lastAction.value = SortAction(photo.id, PhotoStatus.KEEP)
                updateCombo()
                
                // Increment sorted count
                _counters.value = _counters.value.copy(keep = _counters.value.keep + 1)
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementKeepCount()
                
                // Get album info and actual path
                val album = _albumBubbleList.value.find { it.bucketId == bucketId }
                // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
                // Fall back to Pictures/ only for newly created albums
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
                val photoUri = Uri.parse(photo.systemUri)
                
                // Perform album operation based on setting
                when (_albumAddAction.value) {
                    AlbumAddAction.COPY -> {
                        val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                        if (result.isSuccess) {
                            _albumMessage.value = "已保留并复制到 ${album?.displayName}"
                        } else {
                            _error.value = "复制失败: ${result.exceptionOrNull()?.message}"
                        }
                    }
                    AlbumAddAction.MOVE -> {
                        when (val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)) {
                            is MovePhotoResult.Success -> {
                                _albumMessage.value = "已保留并移动到 ${album?.displayName}"
                            }
                            is MovePhotoResult.NeedsConfirmation -> {
                                // This shouldn't happen after permission check, but handle it
                                _error.value = "需要权限确认才能移动照片"
                            }
                            is MovePhotoResult.Error -> {
                                _error.value = "移动失败: ${result.message}"
                            }
                        }
                    }
                }
                
                // Update widget
                widgetUpdater.updateDailyProgressWidgets()
                
            } catch (e: Exception) {
                _error.value = "操作失败: ${e.message}"
            }
        }
    }
    
    /**
     * Called when user clicks "前往设置授权" in the permission dialog.
     * The dialog will navigate to settings; this is just for any state updates needed.
     */
    fun onOpenPermissionSettings() {
        // No state changes needed - the dialog handles navigation
    }
    
    /**
     * Called when user clicks "已授予权限" in the permission dialog.
     * Checks if permission is actually granted and either retries the operation or shows error.
     */
    fun onPermissionGranted() {
        if (storagePermissionHelper.hasManageStoragePermission()) {
            // Permission granted, execute pending operation
            val photoId = _pendingPhotoId
            val bucketId = _pendingAlbumBucketId.value
            
            // Close dialog and clear pending state
            _showPermissionDialog.value = false
            _permissionRetryError.value = false
            _pendingPhotoId = null
            _pendingAlbumBucketId.value = null
            
            // Execute the pending operation
            if (photoId != null && bucketId != null) {
                executeKeepAndAddToAlbum(photoId, bucketId)
            }
        } else {
            // Permission still not granted, show error
            _permissionRetryError.value = true
        }
    }
    
    /**
     * Called when user dismisses the permission dialog without granting permission.
     */
    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
        _permissionRetryError.value = false
        _pendingPhotoId = null
        _pendingAlbumBucketId.value = null
    }
    
    /**
     * Load all system albums for the album picker.
     */
    fun loadSystemAlbums() {
        viewModelScope.launch {
            try {
                val albums = mediaStoreDataSource.getAllAlbums()
                _availableAlbums.value = albums
            } catch (e: Exception) {
                _error.value = "加载相册列表失败: ${e.message}"
            }
        }
    }
    
    /**
     * Add a system album to the quick album list (bubble list).
     * This allows the user to quickly access this album during card sorting.
     */
    fun addAlbumToQuickList(bucketId: String) {
        viewModelScope.launch {
            try {
                // Check if album is already in the list
                val existingIds = albumBubbleDao.getAllBucketIds().toSet()
                if (bucketId in existingIds) {
                    _albumMessage.value = "相册已在快捷列表中"
                    return@launch
                }
                
                // Get album info from loaded albums or fetch from MediaStore
                val albums = _availableAlbums.value.ifEmpty { mediaStoreDataSource.getAllAlbums() }
                val album = albums.find { it.id == bucketId }
                
                if (album != null) {
                    albumOperationsUseCase.addAlbumsToBubbleList(listOf(album))
                    _albumMessage.value = "已添加「${album.name}」到快捷列表"
                } else {
                    _error.value = "找不到该相册"
                }
            } catch (e: Exception) {
                _error.value = "添加相册失败: ${e.message}"
            }
        }
    }
    
    /**
     * Remove an album from the quick album list (bubble list).
     */
    fun removeAlbumFromQuickList(bucketId: String) {
        viewModelScope.launch {
            try {
                albumBubbleDao.deleteByBucketId(bucketId)
            } catch (e: Exception) {
                _error.value = "移除相册失败: ${e.message}"
            }
        }
    }
    
    /**
     * Create a new album in the system and add it to the quick album list.
     * The newly created album will be selected.
     */
    fun createAlbumAndAdd(albumName: String) {
        viewModelScope.launch {
            try {
                // Create album in system storage
                val result = mediaStoreDataSource.createAlbum(albumName)
                
                if (result != null) {
                    val (bucketId, displayName) = result
                    
                    // Add to bubble list
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
                    
                    albumBubbleDao.insert(newAlbum)
                    _albumMessage.value = "已创建相册「$displayName」"
                    
                    // Refresh the available albums list
                    _availableAlbums.value = mediaStoreDataSource.getAllAlbums()
                } else {
                    _error.value = "创建相册失败"
                }
            } catch (e: Exception) {
                _error.value = "创建相册失败: ${e.message}"
            }
        }
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
    
    /**
     * Set view mode directly (for switching to card mode from context menu).
     */
    fun setViewMode(mode: FlowSorterViewMode) {
        _viewMode.value = mode
        // Clear selection when switching modes
        _selectedPhotoIds.value = emptySet()
    }
    
    /**
     * Start filtering from a specific index.
     * Removes all photos before the specified index from the current photo list.
     * Used for "从此张开始筛选" feature.
     */
    fun startFromIndex(index: Int) {
        val currentPhotos = _pagedPhotos.value
        if (index < 0 || index >= currentPhotos.size) return
        
        // Get photos from the specified index to the end
        val photosFromIndex = currentPhotos.drop(index)
        
        // Update the photos list
        _pagedPhotos.value = photosFromIndex
        
        // Reset the current index to 0 (start of the new list)
        _currentIndex.value = 0
        
        // Clear any selection
        _selectedPhotoIds.value = emptySet()
        
        Log.d(TAG, "startFromIndex: index=$index, remaining photos=${photosFromIndex.size}")
    }
    
    override fun onCleared() {
        super.onCleared()
        comboTimeoutJob?.cancel()
        
        // Clear session filter if it was a precise-mode filter (from timeline sorting)
        // This ensures the temporary timeline filter doesn't persist after exiting
        val sessionFilter = preferencesRepository.getSessionCustomFilter()
        if (sessionFilter?.preciseMode == true) {
            preferencesRepository.clearSessionCustomFilter()
        }
    }
}