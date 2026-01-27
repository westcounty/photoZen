package com.example.photozen.ui.screens.flowsorter

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoSortOrder
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.AlbumAddAction
import com.example.photozen.data.repository.CustomFilterSession
import com.example.photozen.data.repository.PhotoClassificationMode
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.repository.StatsRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.GetDailyTaskStatusUseCase
import com.example.photozen.domain.usecase.GetUnsortedPhotosUseCase
import com.example.photozen.domain.usecase.ManageTrashUseCase
import com.example.photozen.domain.usecase.MovePhotoResult
import com.example.photozen.domain.usecase.SortPhotoUseCase
import com.example.photozen.domain.usecase.SyncPhotosUseCase
import com.example.photozen.util.StoragePermissionHelper
import com.example.photozen.util.WidgetUpdater
import com.example.photozen.util.PhotoPreloader
import com.example.photozen.data.repository.GuideRepository
import com.example.photozen.data.repository.FilterPresetRepository
import com.example.photozen.domain.model.FilterConfig
import com.example.photozen.domain.model.FilterPreset
import com.example.photozen.domain.model.FilterType
import com.example.photozen.domain.model.UndoAction
import com.example.photozen.ui.components.PhotoGridMode
import com.example.photozen.ui.state.UndoManager
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
import kotlinx.coroutines.flow.map
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
    val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,  // 网格/瀑布流视图模式
    val filterMode: PhotoFilterMode = PhotoFilterMode.ALL,
    val cameraAlbumsLoaded: Boolean = false,
    val isDailyTask: Boolean = false,
    val dailyTaskTarget: Int = -1,
    val dailyTaskCurrent: Int = 0,
    val isDailyTaskComplete: Boolean = false,
    val cardZoomEnabled: Boolean = true,
    val swipeSensitivity: Float = 1.0f,
    val hapticFeedbackEnabled: Boolean = true,  // Phase 3-7
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
    val pendingAlbumBucketId: String? = null,
    // REQ-064: 来源名称（相册名/时间线分组名）
    val sourceName: String? = null
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
    private val photoDao: PhotoDao,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val storagePermissionHelper: StoragePermissionHelper,
    val guideRepository: GuideRepository,
    private val filterPresetRepository: FilterPresetRepository,
    private val statsRepository: StatsRepository,
    // Phase 4: 预加载器
    private val photoPreloader: PhotoPreloader,
    // REQ-060: 全局撤销管理器
    private val undoManager: UndoManager,
    // 永久删除支持
    private val manageTrashUseCase: ManageTrashUseCase
) : ViewModel() {
    
    private val isDailyTask: Boolean = savedStateHandle["isDailyTask"] ?: false
    private val targetCount: Int = savedStateHandle["targetCount"] ?: -1
    private val albumBucketId: String? = savedStateHandle["albumBucketId"]
    private val initialListMode: Boolean = savedStateHandle["initialListMode"] ?: false
    // REQ-064: 来源名称（从导航参数获取，如相册名或时间线分组名）
    private val sourceNameArg: String? = savedStateHandle["sourceName"]
    
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
    
    // ==================== 筛选状态 ====================
    
    private val _filterConfig = MutableStateFlow(FilterConfig.EMPTY)
    val filterConfig: StateFlow<FilterConfig> = _filterConfig.asStateFlow()
    
    val filterPresets: StateFlow<List<FilterPreset>> = 
        filterPresetRepository.getPresets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /** 相册名称映射（用于 FilterChipRow） */
    val albumNames: StateFlow<Map<String, String>> = albumBubbleDao.getAll()
        .map { list -> list.associate { it.bucketId to it.displayName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    /** 系统相册列表缓存（用于获取相册名称） */
    private val _systemAlbumsCache = MutableStateFlow<Map<String, String>>(emptyMap())

    // Camera album IDs - 需要在 albumBubblesForFilter 之前定义
    private val _cameraAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _cameraAlbumsLoaded = MutableStateFlow(false)

    /** 可选相册列表（用于 FilterBottomSheet）- 基于当前实际待筛选照片所属的相册 */
    private val _albumBubblesForFilter = MutableStateFlow<List<AlbumBubbleEntity>>(emptyList())
    val albumBubblesForFilter: StateFlow<List<AlbumBubbleEntity>> = _albumBubblesForFilter.asStateFlow()

    // 监听相关状态变化，更新筛选面板的相册列表
    init {
        viewModelScope.launch {
            combine(
                photoDao.getBucketIdsWithUnsortedPhotos(),
                _systemAlbumsCache,
                preferencesRepository.getPhotoFilterMode(),
                _cameraAlbumIds,
                preferencesRepository.getSessionCustomFilterFlow()
            ) { allBucketIds, albumNamesMap, filterMode, cameraIds, sessionFilter ->
                AlbumFilterParams(allBucketIds, albumNamesMap, filterMode, cameraIds, sessionFilter)
            }.collect { params ->
                updateAlbumBubblesForFilter(params)
            }
        }
    }

    private data class AlbumFilterParams(
        val allBucketIds: List<String>,
        val albumNamesMap: Map<String, String>,
        val filterMode: PhotoFilterMode,
        val cameraIds: List<String>,
        val sessionFilter: CustomFilterSession?
    )

    private suspend fun updateAlbumBubblesForFilter(params: AlbumFilterParams) {
        val bucketIds = try {
            // 优先级 1：如果 sessionFilter 有 photoIds，从这些照片获取相册
            if (!params.sessionFilter?.photoIds.isNullOrEmpty()) {
                photoDao.getBucketIdsByPhotoIds(params.sessionFilter!!.photoIds!!)
            }
            // 优先级 2：如果 sessionFilter 有 preciseMode 或 albumIds/日期范围
            else if (params.sessionFilter?.preciseMode == true ||
                     params.filterMode == PhotoFilterMode.CUSTOM && params.sessionFilter != null) {
                val session = params.sessionFilter!!
                when {
                    !session.albumIds.isNullOrEmpty() -> {
                        val endDateMs = if (session.preciseMode) session.endDate
                                         else session.endDate?.let { it + 86400L * 1000 - 1 }
                        photoDao.getBucketIdsWithUnsortedPhotosInAlbumsAndDateRange(
                            session.albumIds!!,
                            session.startDate,
                            endDateMs
                        )
                    }
                    session.startDate != null || session.endDate != null -> {
                        val endDateMs = if (session.preciseMode) session.endDate
                                         else session.endDate?.let { it + 86400L * 1000 - 1 }
                        photoDao.getBucketIdsWithUnsortedPhotosInDateRange(session.startDate, endDateMs)
                    }
                    else -> params.allBucketIds
                }
            }
            // 优先级 3：根据全局筛选模式过滤
            else {
                when (params.filterMode) {
                    PhotoFilterMode.ALL -> params.allBucketIds
                    PhotoFilterMode.CAMERA_ONLY -> params.allBucketIds.filter { it in params.cameraIds }
                    PhotoFilterMode.EXCLUDE_CAMERA -> params.allBucketIds.filter { it !in params.cameraIds }
                    PhotoFilterMode.CUSTOM -> params.allBucketIds
                }
            }
        } catch (e: Exception) {
            params.allBucketIds // 出错时返回所有相册
        }

        // 将 bucket IDs 转换为 AlbumBubbleEntity
        _albumBubblesForFilter.value = bucketIds.mapNotNull { bucketId ->
            val displayName = params.albumNamesMap[bucketId] ?: return@mapNotNull null
            AlbumBubbleEntity(bucketId, displayName, 0)
        }.sortedBy { it.displayName }
    }
    
    // Initialize view mode based on navigation parameter
    private val _viewMode = MutableStateFlow(
        if (initialListMode) FlowSorterViewMode.LIST else FlowSorterViewMode.CARD
    )
    private val _selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    private val _sortOrder = MutableStateFlow(PhotoSortOrder.DATE_DESC)
    private val _gridColumns = MutableStateFlow(2)
    private val _gridMode = MutableStateFlow(PhotoGridMode.WATERFALL)
    val gridMode: StateFlow<PhotoGridMode> = _gridMode.asStateFlow()
    
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
    private var _pendingBatchPhotoIds: List<String> = emptyList()  // Batch photos waiting for permission

    // System albums state (for album picker)
    private val _availableAlbums = MutableStateFlow<List<com.example.photozen.data.source.Album>>(emptyList())
    val availableAlbums: StateFlow<List<com.example.photozen.data.source.Album>> = _availableAlbums.asStateFlow()
    
    // CRITICAL FIX: Store initial total count to prevent flickering during rapid swipes
    // This ensures totalCount remains stable even when counters and photos list update at different times
    private val _initialTotalCount = MutableStateFlow(-1) // -1 means not yet initialized
    
    // Random seed for consistent random sorting until changed
    private var randomSeed = System.currentTimeMillis()

    // Job for auto-hiding combo after timeout
    private var comboTimeoutJob: Job? = null
    
    // ==================== PAGINATION STATE ====================
    // Pagination ensures correct sorting across ALL photos, not just the first 500.
    // The database applies ORDER BY to all matching photos, then LIMIT/OFFSET for pagination.

    // Two pagination strategies:
    // 1. Snapshot Mode (small datasets): Load all IDs into memory for consistent random shuffle
    // 2. Database Mode (large datasets): Use LIMIT/OFFSET directly to avoid OOM

    companion object {
        /**
         * Maximum number of photos to use snapshot pagination.
         * For larger datasets, use database-level pagination to avoid OOM.
         */
        private const val MAX_SNAPSHOT_SIZE = 5000
    }

    /** Whether to use database-level pagination instead of ID snapshot */
    private var useDatabasePagination = false

    /** Cached filter parameters for database pagination mode */
    private var dbPaginationParams: DatabasePaginationParams? = null

    /** Parameters needed for database-level pagination */
    private data class DatabasePaginationParams(
        val filterMode: PhotoFilterMode,
        val bucketIds: List<String>?,
        val excludeBucketIds: List<String>?,
        val startDateMs: Long?,
        val endDateMs: Long?,
        val sortOrder: PhotoSortOrder,
        val randomSeed: Long
    )

    // Unified Pagination Strategy (for small datasets):
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
            photos.filter { it.id !in sortedIds }
        }
    }
    
    /**
     * Load the initial batch of photos based on current filter mode and sort order.
     * Called when filter mode or sort order changes.
     *
     * Uses _isReloading flag to prevent "complete" screen flash during reload.
     * IMPORTANT: Uses flow.first() to get the CURRENT values at call time,
     * ensuring correct filter/sort parameters are used (especially for CUSTOM mode).
     *
     * CRITICAL FIX: Now also uses _filterConfig for in-page filtering (筛选按钮).
     * If _filterConfig has values, they override the global preferences.
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

                // CRITICAL FIX: Check if in-page filter (_filterConfig) is active
                // If so, use it instead of global preferences
                val currentFilterConfig = _filterConfig.value
                val effectiveSessionFilter = if (!currentFilterConfig.isEmpty) {
                    // Use _filterConfig values - create a CustomFilterSession with preciseMode=true
                    // This ensures the filter is applied correctly at database level
                    CustomFilterSession(
                        albumIds = currentFilterConfig.albumIds,
                        startDate = currentFilterConfig.startDate,
                        endDate = currentFilterConfig.endDate?.let { it + 86400L * 1000 - 1 }, // Extend to end of day
                        preciseMode = true
                    )
                } else {
                    sessionFilter
                }

                // Calculate effective filter parameters for pagination
                // Support both include mode (bucketIds) and exclude mode (excludeBucketIds)
                // IMPORTANT: sessionFilter with preciseMode=true takes priority (used by timeline group sorting)
                val useSessionFilter = sessionFilter?.preciseMode == true || filterMode == PhotoFilterMode.CUSTOM
                val effectiveBucketIds: List<String>? = when {
                    albumBucketId != null -> listOf(albumBucketId)
                    !currentFilterConfig.isEmpty -> currentFilterConfig.albumIds
                    useSessionFilter -> sessionFilter?.albumIds
                    filterMode == PhotoFilterMode.CAMERA_ONLY -> cameraIds.takeIf { it.isNotEmpty() }
                    else -> null
                }
                val effectiveExcludeBucketIds: List<String>? = when {
                    useSessionFilter -> sessionFilter?.excludeAlbumIds
                    else -> null
                }
                val effectiveStartDateMs: Long? = when {
                    !currentFilterConfig.isEmpty -> currentFilterConfig.startDate
                    useSessionFilter -> sessionFilter?.startDate
                    else -> null
                }
                val effectiveEndDateMs: Long? = when {
                    !currentFilterConfig.isEmpty -> currentFilterConfig.endDate?.let { it + 86400L * 1000 - 1 }
                    useSessionFilter -> sessionFilter?.endDate?.let {
                        if (sessionFilter?.preciseMode == true) it else it + 86400L * 1000 - 1
                    }
                    else -> null
                }

                // First, check the count to decide pagination strategy
                // This avoids loading all IDs into memory for large datasets
                // Use appropriate count method based on include/exclude mode
                val photoCount = if (!effectiveExcludeBucketIds.isNullOrEmpty()) {
                    getUnsortedPhotosUseCase.getCountExcludingBucketsFilteredSync(
                        effectiveExcludeBucketIds, effectiveStartDateMs, effectiveEndDateMs
                    )
                } else {
                    getUnsortedPhotosUseCase.getCountFilteredSync(
                        effectiveBucketIds, effectiveStartDateMs, effectiveEndDateMs
                    )
                }

                if (photoCount > MAX_SNAPSHOT_SIZE) {
                    // Large dataset: Use database-level pagination to avoid OOM
                    useDatabasePagination = true
                    currentSessionIds = null
                    dbPaginationParams = DatabasePaginationParams(
                        filterMode = if (albumBucketId != null) PhotoFilterMode.CAMERA_ONLY
                                    else if (!currentFilterConfig.isEmpty) PhotoFilterMode.CUSTOM
                                    else filterMode,
                        bucketIds = effectiveBucketIds,
                        excludeBucketIds = effectiveExcludeBucketIds,
                        startDateMs = effectiveStartDateMs,
                        endDateMs = effectiveEndDateMs,
                        sortOrder = sortOrder,
                        randomSeed = randomSeed
                    )

                    // Load first page using database pagination
                    // Use appropriate method based on include/exclude mode
                    val photos = if (!effectiveExcludeBucketIds.isNullOrEmpty()) {
                        getUnsortedPhotosUseCase.getPageExcludingBucketsFiltered(
                            excludeBucketIds = effectiveExcludeBucketIds,
                            startDateMs = effectiveStartDateMs,
                            endDateMs = effectiveEndDateMs,
                            page = 0,
                            sortOrder = sortOrder,
                            randomSeed = randomSeed
                        )
                    } else {
                        getUnsortedPhotosUseCase.getPageFiltered(
                            bucketIds = effectiveBucketIds,
                            startDateMs = effectiveStartDateMs,
                            endDateMs = effectiveEndDateMs,
                            page = 0,
                            sortOrder = sortOrder,
                            randomSeed = randomSeed
                        )
                    }
                    _pagedPhotos.value = photos
                    hasMorePages = photos.size >= GetUnsortedPhotosUseCase.PAGE_SIZE
                } else {
                    // Small dataset: Use snapshot pagination for better UX (stable random order)
                    useDatabasePagination = false
                    dbPaginationParams = null

                    // Initialize the ID snapshot for ALL sort modes
                    // This guarantees global sorting and stability
                    val allIds = if (albumBucketId != null) {
                        getUnsortedPhotosUseCase.getAllIds(
                            com.example.photozen.data.repository.PhotoFilterMode.CAMERA_ONLY,
                            listOf(albumBucketId),
                            null,
                            sortOrder
                        )
                    } else if (!currentFilterConfig.isEmpty) {
                        // Use the effective filter when _filterConfig is active
                        getUnsortedPhotosUseCase.getAllIds(
                            PhotoFilterMode.CUSTOM,
                            cameraIds,
                            effectiveSessionFilter,
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
                }
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
     * Supports two pagination modes:
     * 1. Snapshot Mode: Uses pre-loaded ID list (for small datasets)
     * 2. Database Mode: Uses LIMIT/OFFSET directly (for large datasets)
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
                    currentPage++

                    val morePhotos = if (useDatabasePagination) {
                        // Database pagination mode: Use LIMIT/OFFSET with offset adjustment
                        val params = dbPaginationParams ?: return@launch
                        // Adjust offset for photos that have been sorted (they're no longer in UNSORTED status)
                        val offsetAdjustment = -_sortedPhotoIds.value.size

                        // Use appropriate method based on include/exclude mode
                        if (!params.excludeBucketIds.isNullOrEmpty()) {
                            getUnsortedPhotosUseCase.getPageExcludingBucketsFiltered(
                                excludeBucketIds = params.excludeBucketIds,
                                startDateMs = params.startDateMs,
                                endDateMs = params.endDateMs,
                                page = currentPage,
                                sortOrder = params.sortOrder,
                                randomSeed = params.randomSeed,
                                offsetAdjustment = offsetAdjustment
                            )
                        } else {
                            getUnsortedPhotosUseCase.getPageFiltered(
                                bucketIds = params.bucketIds,
                                startDateMs = params.startDateMs,
                                endDateMs = params.endDateMs,
                                page = currentPage,
                                sortOrder = params.sortOrder,
                                randomSeed = params.randomSeed,
                                offsetAdjustment = offsetAdjustment
                            )
                        }
                    } else {
                        // Snapshot pagination mode: Use pre-loaded ID list
                        val filterMode = preferencesRepository.getPhotoFilterMode().first()
                        val sessionFilter = preferencesRepository.getSessionCustomFilterFlow().first()
                        val sortOrder = _sortOrder.value

                        loadPhotosPage(
                            page = currentPage,
                            filterMode = filterMode,
                            sessionFilter = sessionFilter,
                            sortOrder = sortOrder
                        )
                    }

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
     *
     * CRITICAL FIX: Now also uses _filterConfig for in-page filtering.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getFilteredPhotosCountFlow(): Flow<Int> {
        // If filtering by specific album, only count photos from that album
        if (albumBucketId != null) {
            return getUnsortedPhotosUseCase.getCountByBuckets(listOf(albumBucketId))
        }

        // Combine first 4 flows into FilterParams
        val paramsFlow = combine(
            preferencesRepository.getPhotoFilterMode(),
            _cameraAlbumIds,
            _cameraAlbumsLoaded,
            preferencesRepository.getSessionCustomFilterFlow()
        ) { filterMode, cameraIds, loaded, sessionFilter ->
            FilterParams(filterMode, cameraIds, loaded, sessionFilter, PhotoSortOrder.DATE_DESC)
        }

        // Then combine with _filterConfig
        return combine(paramsFlow, _filterConfig) { params, inPageFilter ->
            params to inPageFilter
        }.flatMapLatest { (params, inPageFilter) ->
            // CRITICAL FIX: Priority 0 - Check if in-page filter is active
            if (!inPageFilter.isEmpty) {
                val effectiveEndDateMs = inPageFilter.endDate?.let { it + 86400L * 1000 - 1 }
                return@flatMapLatest getUnsortedPhotosUseCase.getCountFiltered(
                    inPageFilter.albumIds,
                    inPageFilter.startDate,
                    effectiveEndDateMs
                )
            }

            // Priority 1: If sessionFilter has preciseMode=true, use it directly
            // This allows timeline sorting to work without changing the global filter mode
            val sessionFilter = params.sessionFilter
            if (sessionFilter?.preciseMode == true) {
                // Priority 1a: If photoIds is provided, use it (highest priority)
                // 这是时间线分组整理的核心逻辑
                val photoIds = sessionFilter.photoIds
                if (!photoIds.isNullOrEmpty()) {
                    return@flatMapLatest getUnsortedPhotosUseCase.getCountByPhotoIds(photoIds)
                }

                // Priority 1b: Use date range and album filters
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
        val gridMode: PhotoGridMode,
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
        combine(_selectedPhotoIds, _sortOrder, _gridColumns, _gridMode, albumStateFlow) { ids, order, cols, mode, album ->
            SelectionAndAlbumState(ids, order, cols, mode, album)
        },
        combine(
            preferencesRepository.getPhotoFilterMode(), 
            _cameraAlbumsLoaded, 
            preferencesRepository.getCardZoomEnabled(),
            preferencesRepository.getSwipeSensitivity(),
            preferencesRepository.getHapticFeedbackEnabled()  // Phase 3-7
        ) { mode, loaded, zoom, sensitivity, haptic -> 
            // Use Pair of Pairs for 5 values: ((mode, loaded), (zoom, sensitivity, haptic))
            Pair(Pair(mode, loaded), Triple(zoom, sensitivity, haptic))
        },
        if (isDailyTask) getDailyTaskStatusUseCase() else flowOf(null)
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        var photos = values[0] as List<PhotoEntity>
        val realUnsortedCount = values[1] as Int  // Real count from COUNT query (not limited)
        @Suppress("UNCHECKED_CAST")
        val loadingStates = values[2] as Triple<Boolean, Boolean, Boolean>
        val isLoading = loadingStates.first
        val isSyncing = loadingStates.second
        val isReloading = loadingStates.third
        val currentIndex = values[3] as Int
        val lastAction = values[4] as SortAction?
        @Suppress("UNCHECKED_CAST")
        val combined = values[5] as Triple<String?, SortCounters, ComboState>
        val viewMode = values[6] as FlowSorterViewMode
        val selectionAndAlbum = values[7] as SelectionAndAlbumState
        @Suppress("UNCHECKED_CAST")
        val prefsState = values[8] as Pair<Pair<PhotoFilterMode, Boolean>, Triple<Boolean, Float, Boolean>>
        val dailyStatus = values[9] as com.example.photozen.domain.usecase.DailyTaskStatus?
        
        val error = combined.first
        val counters = combined.second
        val combo = combined.third
        val selectedIds = selectionAndAlbum.selectedPhotoIds
        val sortOrder = selectionAndAlbum.sortOrder
        val gridColumns = selectionAndAlbum.gridColumns
        val gridMode = selectionAndAlbum.gridMode
        val albumState = selectionAndAlbum.albumState
        val filterMode = prefsState.first.first
        val cameraLoaded = prefsState.first.second
        val cardZoomEnabled = prefsState.second.first
        val swipeSensitivity = prefsState.second.second
        val hapticFeedbackEnabled = prefsState.second.third  // Phase 3-7
        
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
            gridMode = gridMode,
            filterMode = filterMode,
            cameraAlbumsLoaded = cameraLoaded,
            isDailyTask = isDailyTask,
            dailyTaskTarget = targetCount,
            dailyTaskCurrent = dailyCurrent,
            isDailyTaskComplete = isDailyComplete,
            cardZoomEnabled = cardZoomEnabled,
            swipeSensitivity = swipeSensitivity,
            hapticFeedbackEnabled = hapticFeedbackEnabled,  // Phase 3-7
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
            pendingAlbumBucketId = albumState.pendingAlbumBucketId,
            // REQ-064: 来源名称
            sourceName = sourceNameArg
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
            // Check if sessionFilter has a default sort order (e.g., from timeline group)
            val sessionFilter = preferencesRepository.getSessionCustomFilter()
            sessionFilter?.defaultSortOrder?.let { defaultOrder ->
                _sortOrder.value = defaultOrder
            }
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
            // Cache all album names for filter panel
            _systemAlbumsCache.value = albums.associate { it.id to it.name }
        } catch (e: Exception) {
            // Ignore errors, just use empty list
        } finally {
            _cameraAlbumsLoaded.value = true
        }
    }
    
    // ==================== 筛选操作 ====================
    
    /**
     * 应用筛选配置
     */
    fun applyFilter(config: FilterConfig) {
        // 安全处理：确保空列表转为 null，避免数据库查询问题
        val safeConfig = if (config.albumIds?.isEmpty() == true) {
            config.copy(albumIds = null)
        } else {
            config
        }

        // CRITICAL: Reset initial total count BEFORE setting filter config
        // This ensures the combine flow uses the new count instead of cached old value
        // Without this, the first filter apply shows stale count (requires two applies to update)
        _initialTotalCount.value = -1

        _filterConfig.value = safeConfig

        // 保存为最近使用
        viewModelScope.launch {
            filterPresetRepository.saveLastFilterConfig(safeConfig)
        }
        // 重新加载照片
        loadInitialPhotos()
    }
    
    /**
     * 清除指定类型的筛选条件
     */
    fun clearFilter(type: FilterType) {
        val newConfig = when (type) {
            FilterType.ALBUM -> _filterConfig.value.clearAlbumFilter()
            FilterType.DATE -> _filterConfig.value.clearDateFilter()
        }
        applyFilter(newConfig)
    }
    
    /**
     * 清除所有筛选条件
     */
    fun clearAllFilters() {
        applyFilter(FilterConfig.EMPTY)
    }
    
    /**
     * 保存筛选预设
     */
    fun saveFilterPreset(name: String) {
        viewModelScope.launch {
            val preset = FilterPreset.create(name, _filterConfig.value)
            filterPresetRepository.savePreset(preset)
        }
    }
    
    /**
     * 删除筛选预设
     */
    fun deleteFilterPreset(presetId: String) {
        viewModelScope.launch {
            filterPresetRepository.deletePreset(presetId)
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
        val comboCount = updateCombo()
        // Mark photo as sorted immediately (removes from display list)
        markPhotoAsSorted(photoId)
        // Update counters SYNCHRONOUSLY to ensure UI reflects change immediately
        _counters.value = _counters.value.copy(keep = _counters.value.keep + 1)
        _sortedCountImmediate.value++  // Immediate counter for UI
        
        // Phase 4: 预加载下一批照片
        preloadNextPhotos()
        
        // REQ-060: 获取照片当前状态用于撤销
        val photo = uiState.value.photos.find { it.id == photoId }
        val previousStatus = photo?.status ?: PhotoStatus.UNSORTED

        viewModelScope.launch {
            try {
                sortPhotoUseCase.keepPhoto(photoId)
                _lastAction.value = SortAction(photoId, PhotoStatus.KEEP)
                // REQ-060: 记录到全局撤销管理器（支持恢复到原状态）
                undoManager.recordAction(UndoAction.SortPhoto(
                    photoId = photoId,
                    previousStatus = previousStatus,
                    newStatus = PhotoStatus.KEEP
                ))
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementKeepCount()
                // Record to stats repository for detailed statistics
                statsRepository.recordKeep()
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
        // REQ-060: 获取照片当前状态用于撤销
        val photo = uiState.value.photos.find { it.id == photoId }
        val previousStatus = photo?.status ?: PhotoStatus.UNSORTED

        val comboCount = updateCombo()
        // Mark photo as sorted immediately (removes from display list)
        markPhotoAsSorted(photoId)
        // Update counters SYNCHRONOUSLY to ensure UI reflects change immediately
        _counters.value = _counters.value.copy(trash = _counters.value.trash + 1)
        _sortedCountImmediate.value++  // Immediate counter for UI

        // Phase 4: 预加载下一批照片
        preloadNextPhotos()

        viewModelScope.launch {
            try {
                sortPhotoUseCase.trashPhoto(photoId)
                _lastAction.value = SortAction(photoId, PhotoStatus.TRASH)
                // REQ-060: 记录到全局撤销管理器
                undoManager.recordAction(UndoAction.SortPhoto(
                    photoId = photoId,
                    previousStatus = previousStatus,
                    newStatus = PhotoStatus.TRASH
                ))
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementTrashCount()
                // Record to stats repository for detailed statistics
                statsRepository.recordTrash()
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
        // REQ-060: 获取照片当前状态用于撤销
        val photo = uiState.value.photos.find { it.id == photoId }
        val previousStatus = photo?.status ?: PhotoStatus.UNSORTED

        val comboCount = updateCombo()
        // Mark photo as sorted immediately (removes from display list)
        markPhotoAsSorted(photoId)
        // Update counters SYNCHRONOUSLY to ensure UI reflects change immediately
        _counters.value = _counters.value.copy(maybe = _counters.value.maybe + 1)
        _sortedCountImmediate.value++  // Immediate counter for UI

        // Phase 4: 预加载下一批照片
        preloadNextPhotos()

        viewModelScope.launch {
            try {
                sortPhotoUseCase.maybePhoto(photoId)
                _lastAction.value = SortAction(photoId, PhotoStatus.MAYBE)
                // REQ-060: 记录到全局撤销管理器
                undoManager.recordAction(UndoAction.SortPhoto(
                    photoId = photoId,
                    previousStatus = previousStatus,
                    newStatus = PhotoStatus.MAYBE
                ))
                preferencesRepository.incrementSortedCount()
                preferencesRepository.incrementMaybeCount()
                // Record to stats repository for detailed statistics
                statsRepository.recordMaybe()
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
     * Phase 4: 预加载下一批照片
     * 
     * 在滑动操作后调用，预加载接下来的3张照片以提升用户体验。
     */
    private fun preloadNextPhotos() {
        val currentState = uiState.value
        val photos = currentState.photos
        val currentIndex = currentState.currentIndex
        
        // 使用 PhotoPreloader 预加载
        photoPreloader.preloadForSwipe(
            photos = photos,
            currentIndex = currentIndex,
            preloadCount = 3
        )
    }
    
    /**
     * Undo the last sorting action.
     * REQ-060: 使用全局 UndoManager，支持恢复到原状态（而非固定为 UNSORTED）
     * Note: Undo does NOT decrement the cumulative count - it's a permanent achievement.
     */
    fun undoLastAction() {
        // 同时检查内部状态和全局 UndoManager
        val lastAction = _lastAction.value
        val undoAction = undoManager.lastAction.value

        // 优先使用全局 UndoManager（如果有 SortPhoto 类型的操作）
        if (undoAction is UndoAction.SortPhoto) {
            viewModelScope.launch {
                try {
                    // 使用全局 UndoManager 执行撤销（会恢复到原状态）
                    undoManager.undo()
                    // Remove from sorted list so it reappears in the display
                    _sortedPhotoIds.value = _sortedPhotoIds.value - undoAction.photoId
                    // Update session counters only (cumulative count stays)
                    _counters.value = when (undoAction.newStatus) {
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
        } else if (lastAction != null) {
            // 回退到旧逻辑（兼容性）
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
                // Record to stats repository for detailed statistics
                statsRepository.recordKeep()
                
                // Get album info and actual path
                val album = _albumBubbleList.value.find { it.bucketId == bucketId }
                // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
                // Fall back to Pictures/ only for newly created albums
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
                val photoUri = Uri.parse(photo.systemUri)
                
                // Perform album operation based on setting
                var operationSuccess = false
                when (_albumAddAction.value) {
                    AlbumAddAction.COPY -> {
                        val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                        if (result.isSuccess) {
                            _albumMessage.value = "已保留并复制到 ${album?.displayName}"
                            operationSuccess = true
                        } else {
                            _error.value = "复制失败: ${result.exceptionOrNull()?.message}"
                        }
                    }
                    AlbumAddAction.MOVE -> {
                        when (val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)) {
                            is MovePhotoResult.Success -> {
                                _albumMessage.value = "已保留并移动到 ${album?.displayName}"
                                operationSuccess = true
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

                // Trigger album list refresh so AlbumBubbleScreen updates when user switches tabs
                if (operationSuccess) {
                    preferencesRepository.triggerAlbumRefresh()
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
            // Close dialog first
            _showPermissionDialog.value = false
            _permissionRetryError.value = false

            // Handle batch operation if pending
            if (_pendingBatchPhotoIds.isNotEmpty()) {
                val batchIds = _pendingBatchPhotoIds
                val bucketId = _pendingAlbumBucketId.value
                _pendingBatchPhotoIds = emptyList()
                _pendingAlbumBucketId.value = null
                if (bucketId != null) {
                    executeBatchAddToAlbum(batchIds, bucketId)
                }
                return
            }

            // Handle single photo operation
            val photoId = _pendingPhotoId
            val bucketId = _pendingAlbumBucketId.value
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
        _pendingBatchPhotoIds = emptyList()
        _pendingAlbumBucketId.value = null
    }

    /**
     * Called when a photo has been permanently deleted from the device.
     * Removes the photo from our local database.
     */
    fun onPhotoDeletedFromDevice(photoId: String) {
        viewModelScope.launch {
            try {
                manageTrashUseCase.deletePhotos(listOf(photoId))
            } catch (e: Exception) {
                _error.value = "更新数据库失败: ${e.message}"
            }
        }
    }

    /**
     * 复制照片到同一相册
     * 复制后将新照片插入 Room 数据库，保留原照片的筛选状态
     */
    fun copyPhoto(photoId: String) {
        val photo = uiState.value.photos.find { it.id == photoId } ?: return

        viewModelScope.launch {
            try {
                val photoUri = Uri.parse(photo.systemUri)
                val bucketId = photo.bucketId ?: return@launch
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/PhotoZen"

                val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                if (result.isSuccess) {
                    // 将新照片插入 Room 数据库，保留原照片的筛选状态
                    result.getOrNull()?.let { newPhoto ->
                        val photoWithStatus = newPhoto.copy(status = photo.status)
                        photoDao.insert(photoWithStatus)
                    }
                    _albumMessage.value = "已复制照片"
                } else {
                    _error.value = "复制失败: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "复制失败: ${e.message}"
            }
        }
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
     * If an album with the same name already exists, shows a message instead.
     */
    fun createAlbumAndAdd(albumName: String) {
        viewModelScope.launch {
            try {
                // First check if an album with this name already exists
                val allAlbums = mediaStoreDataSource.getAllAlbums()
                val existingAlbum = allAlbums.find { it.name == albumName }

                if (existingAlbum != null) {
                    // Album already exists - show message
                    _albumMessage.value = "相册「$albumName」已存在"
                    _availableAlbums.value = allAlbums
                    return@launch
                }

                // Create album in system storage
                val result = mediaStoreDataSource.createAlbum(albumName)

                if (result != null) {
                    val displayName = result.displayName
                    val relativePath = result.relativePath

                    // Add to bubble list using relativePath as temporary bucketId
                    // It will be updated to real bucketId when photos are added
                    val newAlbum = AlbumBubbleEntity(
                        bucketId = relativePath,
                        displayName = displayName,
                        sortOrder = 0
                    )

                    // Shift existing albums down
                    val existingBubbleAlbums = albumBubbleDao.getAllSync()
                    existingBubbleAlbums.forEachIndexed { index, album ->
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
     * Cycle grid columns: 1 -> 2 -> 3 -> 4 -> 5 -> 1
     * REQ-007: 瀑布流视图支持 1-5 列切换
     */
    fun cycleGridColumns() {
        viewModelScope.launch {
            val newColumns = preferencesRepository.cycleGridColumns(PreferencesRepository.GridScreen.FLOW, minColumns = 1)
            _gridColumns.value = newColumns
        }
    }

    /**
     * Set grid columns directly.
     * REQ-007: 支持直接设置列数（用于视图模式下拉菜单）
     */
    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            val validColumns = columns.coerceIn(1, 4)
            preferencesRepository.setGridColumns(PreferencesRepository.GridScreen.FLOW, validColumns)
            _gridColumns.value = validColumns
        }
    }

    /**
     * Set grid mode (SQUARE or WATERFALL).
     * Used by ViewModeDropdownButton to switch between grid and waterfall layouts.
     */
    fun setGridMode(mode: PhotoGridMode) {
        _gridMode.value = mode
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
     * Toggle selection for a specific photo.
     * Uses current state to ensure fresh data.
     */
    fun toggleSelection(photoId: String) {
        val currentSelection = _selectedPhotoIds.value
        _selectedPhotoIds.value = if (currentSelection.contains(photoId)) {
            currentSelection - photoId
        } else {
            currentSelection + photoId
        }
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
                // Record to stats repository for detailed statistics
                statsRepository.recordKeep(selectedIds.size)
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
                // Record to stats repository for detailed statistics
                statsRepository.recordTrash(selectedIds.size)
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
                // Record to stats repository for detailed statistics
                statsRepository.recordMaybe(selectedIds.size)
            } catch (e: Exception) {
                _error.value = "批量操作失败: ${e.message}"
            }
        }
    }

    /**
     * Batch add selected photos to an album.
     * The action (copy or move) is determined by albumAddAction setting.
     *
     * For MOVE action on Android 11+, checks if MANAGE_EXTERNAL_STORAGE permission is granted.
     */
    fun batchAddToAlbum(photoIds: List<String>, bucketId: String) {
        if (photoIds.isEmpty()) return

        // For MOVE action, check permission first
        if (_albumAddAction.value == AlbumAddAction.MOVE &&
            storagePermissionHelper.isManageStoragePermissionApplicable() &&
            !storagePermissionHelper.hasManageStoragePermission()) {
            // Save pending operation and show permission dialog
            _pendingBatchPhotoIds = photoIds
            _pendingAlbumBucketId.value = bucketId
            _permissionRetryError.value = false
            _showPermissionDialog.value = true
            return
        }

        // Execute the operation
        executeBatchAddToAlbum(photoIds, bucketId)
    }

    /**
     * Execute batch add to album operation.
     * Called directly or after permission is granted.
     */
    private fun executeBatchAddToAlbum(photoIds: List<String>, bucketId: String) {
        if (photoIds.isEmpty()) return

        // Capture photos BEFORE clearing selection (important for state consistency)
        val photos = uiState.value.photos.filter { it.id in photoIds }
        if (photos.isEmpty()) return

        // Clear selection immediately
        _selectedPhotoIds.value = emptySet()

        viewModelScope.launch {
            val album = _albumBubbleList.value.find { it.bucketId == bucketId }
            val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                ?: "Pictures/${album?.displayName ?: "PhotoZen"}"

            // Step 1: Batch mark all photos as KEEP first
            try {
                sortPhotoUseCase.batchUpdateStatus(photoIds, PhotoStatus.KEEP)
                _counters.value = _counters.value.copy(keep = _counters.value.keep + photoIds.size)
                preferencesRepository.incrementSortedCount(photoIds.size)
                // incrementKeepCount doesn't support batch, call in loop
                repeat(photoIds.size) {
                    preferencesRepository.incrementKeepCount()
                }
                statsRepository.recordKeep(photoIds.size)
            } catch (e: Exception) {
                _error.value = "标记保留失败: ${e.message}"
                return@launch
            }

            // Step 2: Perform album operations
            var albumSuccessCount = 0
            var albumFailCount = 0

            for (photo in photos) {
                try {
                    val photoUri = Uri.parse(photo.systemUri)
                    val success = when (_albumAddAction.value) {
                        AlbumAddAction.COPY -> {
                            albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath).isSuccess
                        }
                        AlbumAddAction.MOVE -> {
                            val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)
                            result is MovePhotoResult.Success
                        }
                    }

                    if (success) {
                        albumSuccessCount++
                    } else {
                        albumFailCount++
                    }
                } catch (e: Exception) {
                    albumFailCount++
                }
            }

            // Show result message
            val actionName = if (_albumAddAction.value == AlbumAddAction.COPY) "复制" else "移动"
            _albumMessage.value = when {
                albumFailCount == 0 -> "已保留并${actionName} $albumSuccessCount 张照片到 ${album?.displayName}"
                albumSuccessCount == 0 -> "已保留 ${photoIds.size} 张照片，但${actionName}到相册失败"
                else -> "已保留 ${photoIds.size} 张，${actionName}成功 $albumSuccessCount 张，失败 $albumFailCount 张"
            }

            // Trigger album list refresh
            if (albumSuccessCount > 0) {
                preferencesRepository.triggerAlbumRefresh()
            }

            // Update widget
            widgetUpdater.updateDailyProgressWidgets()
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