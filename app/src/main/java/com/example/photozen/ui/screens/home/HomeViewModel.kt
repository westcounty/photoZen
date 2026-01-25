package com.example.photozen.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.BuildConfig
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.AchievementData
import com.example.photozen.data.repository.PhotoClassificationMode
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.DailyTaskStatus
import com.example.photozen.domain.usecase.GetDailyTaskStatusUseCase
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.GetUnsortedPhotosUseCase
import com.example.photozen.domain.usecase.SyncPhotosUseCase
import com.example.photozen.data.repository.GuideRepository
import com.example.photozen.data.repository.StatsRepository
import com.example.photozen.data.repository.StatsSummary
import com.example.photozen.ui.state.AsyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    val photoClassificationMode: PhotoClassificationMode = PhotoClassificationMode.TAG,
    val dailyTaskStatus: DailyTaskStatus? = null,
    // Changelog and Quick Start
    val shouldShowChangelog: Boolean = false,
    val shouldShowQuickStart: Boolean = false,
    // Phase 1-D: 整理模式选择弹窗状态
    val showSortModeSheet: Boolean = false,
    // Phase 3: 整理统计摘要
    val statsSummary: StatsSummary = StatsSummary.EMPTY
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
    val guideRepository: GuideRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {
    
    private val _hasPermission = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)
    // Phase 4: 使用 AsyncState 替换 _isSyncing + _syncResult + _error
    private val _syncState = MutableStateFlow<AsyncState<String?>>(AsyncState.Idle)
    
    // Camera album IDs - loaded once and cached
    private val _cameraAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _cameraAlbumsLoaded = MutableStateFlow(false)
    
    // Changelog and Quick Start dialog states
    private val _shouldShowChangelog = MutableStateFlow(false)
    private val _shouldShowQuickStart = MutableStateFlow(false)
    
    // Phase 1-D: 整理模式选择弹窗状态
    private val _showSortModeSheet = MutableStateFlow(false)
    
    // Phase 3: 整理统计摘要（已改为使用响应式 observeStatsSummary）
    
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

    // ==================== Phase 4: 子状态 Flow ====================
    
    /**
     * Phase 4: 照片统计子状态 Flow
     * 合并了旧的 photoCountsFlow 和 filteredCountsFlow
     */
    private val photoStatsFlow: Flow<PhotoStatsState> = combine(
        getPhotosUseCase.getTotalCount(),
        getFilteredUnsortedCount(),
        getPhotosUseCase.getCountByStatus(PhotoStatus.KEEP),
        getPhotosUseCase.getCountByStatus(PhotoStatus.TRASH),
        getPhotosUseCase.getCountByStatus(PhotoStatus.MAYBE)
    ) { total, unsorted, keep, trash, maybe ->
        // 第一阶段：基础统计
        PhotoStatsState(
            totalPhotos = total,
            unsortedCount = unsorted,
            keepCount = keep,
            trashCount = trash,
            maybeCount = maybe
        )
    }.let { baseStatsFlow ->
        // 第二阶段：添加筛选统计
        combine(
            baseStatsFlow,
            getFilteredTotalCount(),
            getFilteredSortedCount()
        ) { baseStats, filteredTotal, filteredSorted ->
            baseStats.copy(
                filteredTotal = filteredTotal,
                filteredSorted = filteredSorted
            )
        }
    }
    
    /**
     * Phase 4: UI 控制子状态 Flow
     * 使用 AsyncState 替换 isSyncing + syncResult + error
     * 
     * NOTE: isLoading 逻辑保持与旧代码一致：
     * - 只有在有权限且正在同步时才显示 loading
     */
    private val uiControlFlow: Flow<UiControlState> = combine(
        _hasPermission,
        _syncState,
        _shouldShowChangelog,
        _shouldShowQuickStart,
        _showSortModeSheet
    ) { hasPermission, syncState, showChangelog, showQuickStart, showSortModeSheet ->
        UiControlState(
            hasPermission = hasPermission,
            syncState = syncState,
            shouldShowChangelog = showChangelog,
            shouldShowQuickStart = showQuickStart,
            showSortModeSheet = showSortModeSheet
        )
    }
    
    /**
     * Phase 4: 功能配置子状态 Flow
     * 合并了旧的 extraDataFlow 和 dialogStatesFlow 中的配置部分
     * 使用响应式 observeStatsSummary 替代一次性加载，确保数据一致性
     */
    private val featureConfigFlow: Flow<FeatureConfigState> = combine(
        preferencesRepository.getPhotoFilterMode(),
        preferencesRepository.getPhotoClassificationMode(),
        getDailyTaskStatusUseCase(),
        preferencesRepository.getAllAchievementData(),
        statsRepository.observeStatsSummary()
    ) { filterMode, classificationMode, dailyTask, achievement, statsSummary ->
        FeatureConfigState(
            photoFilterMode = filterMode,
            photoClassificationMode = classificationMode,
            dailyTaskStatus = dailyTask,
            achievementData = achievement,
            statsSummary = statsSummary
        )
    }
    
    /**
     * Phase 4: UI State exposed to the screen.
     * 使用 3 个子状态 Flow 组合
     */
    val uiState: StateFlow<HomeUiState> = combine(
        photoStatsFlow,
        uiControlFlow,
        featureConfigFlow
    ) { stats, uiControl, featureConfig ->
        // 计算 isLoading：只有在有权限且正在同步时才显示 loading
        // 保持与旧逻辑一致
        val effectiveLoading = uiControl.hasPermission && uiControl.syncState.isLoading

        HomeUiState(
            // 照片统计
            totalPhotos = stats.totalPhotos,
            unsortedCount = stats.unsortedCount,
            keepCount = stats.keepCount,
            trashCount = stats.trashCount,
            maybeCount = stats.maybeCount,
            filteredTotal = stats.filteredTotal,
            filteredSorted = stats.filteredSorted,
            // UI 控制
            hasPermission = uiControl.hasPermission,
            isLoading = effectiveLoading,
            isSyncing = uiControl.syncState.isLoading,
            syncResult = uiControl.syncState.getOrNull(),
            error = uiControl.syncState.errorOrNull(),
            shouldShowChangelog = uiControl.shouldShowChangelog,
            shouldShowQuickStart = uiControl.shouldShowQuickStart,
            showSortModeSheet = uiControl.showSortModeSheet,
            // 功能配置
            photoFilterMode = featureConfig.photoFilterMode,
            photoClassificationMode = featureConfig.photoClassificationMode,
            dailyTaskStatus = featureConfig.dailyTaskStatus,
            achievementData = featureConfig.achievementData,
            statsSummary = featureConfig.statsSummary
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
    
    // Phase 4: 旧的中间数据类已删除
    // - FilteredCounts -> 合并到 PhotoStatsState
    // - ExtraData -> 拆分到 FeatureConfigState
    // - PhotoCounts -> 合并到 PhotoStatsState
    // - UiStateFlags -> 合并到 UiControlState
    // - DialogStates -> 拆分到 UiControlState 和 FeatureConfigState
    
    init {
        // Update consecutive days tracking on app launch
        viewModelScope.launch {
            preferencesRepository.updateConsecutiveDays()
        }
        // Load camera album IDs
        viewModelScope.launch {
            loadCameraAlbumIds()
        }
        // Check if we should show changelog or quick start
        viewModelScope.launch {
            checkVersionDialogs()
        }
        // Stats summary is now observed via reactive flow (observeStatsSummary)
    }
    
    /**
     * Check if we should show changelog or quick start dialogs.
     * Priority: Quick Start > Changelog
     */
    private suspend fun checkVersionDialogs() {
        val currentAppVersion = BuildConfig.VERSION_NAME
        val completedQuickStartVersion = preferencesRepository.getCompletedQuickStartVersion().first()
        val lastSeenChangelogVersion = preferencesRepository.getLastSeenChangelogVersion().first()
        
        // Import the quick start guide version constant
        val quickStartGuideVersion = com.example.photozen.ui.components.QUICK_START_GUIDE_VERSION
        
        // Check quick start first (higher priority)
        if (completedQuickStartVersion != quickStartGuideVersion) {
            _shouldShowQuickStart.value = true
        } else if (lastSeenChangelogVersion != currentAppVersion) {
            // Only show changelog if quick start is completed
            _shouldShowChangelog.value = true
        }
    }
    
    /**
     * Mark changelog as seen for current version.
     */
    fun markChangelogSeen() {
        viewModelScope.launch {
            preferencesRepository.setLastSeenChangelogVersion(BuildConfig.VERSION_NAME)
            _shouldShowChangelog.value = false
        }
    }
    
    /**
     * Mark quick start as completed for current guide version.
     */
    fun markQuickStartCompleted() {
        viewModelScope.launch {
            preferencesRepository.setCompletedQuickStartVersion(
                com.example.photozen.ui.components.QUICK_START_GUIDE_VERSION
            )
            _shouldShowQuickStart.value = false
            // After completing quick start, check if we should show changelog
            val currentAppVersion = BuildConfig.VERSION_NAME
            val lastSeenChangelogVersion = preferencesRepository.getLastSeenChangelogVersion().first()
            if (lastSeenChangelogVersion != currentAppVersion) {
                _shouldShowChangelog.value = true
            }
        }
    }
    
    /**
     * Save settings from quick start and mark as completed.
     */
    fun completeQuickStartWithSettings(
        dailyTaskEnabled: Boolean,
        dailyTaskTarget: Int,
        swipeSensitivity: Float,
        cardSortingAlbumEnabled: Boolean
    ) {
        viewModelScope.launch {
            // Save all the settings
            preferencesRepository.setDailyTaskEnabled(dailyTaskEnabled)
            preferencesRepository.setDailyTaskTarget(dailyTaskTarget)
            preferencesRepository.setSwipeSensitivity(swipeSensitivity)
            preferencesRepository.setCardSortingAlbumEnabled(cardSortingAlbumEnabled)
            
            // Mark quick start as completed
            preferencesRepository.setCompletedQuickStartVersion(
                com.example.photozen.ui.components.QUICK_START_GUIDE_VERSION
            )
            _shouldShowQuickStart.value = false
            
            // After completing quick start, check if we should show changelog
            val currentAppVersion = BuildConfig.VERSION_NAME
            val lastSeenChangelogVersion = preferencesRepository.getLastSeenChangelogVersion().first()
            if (lastSeenChangelogVersion != currentAppVersion) {
                _shouldShowChangelog.value = true
            }
        }
    }
    
    /**
     * Dismiss quick start without marking as completed.
     */
    fun dismissQuickStart() {
        _shouldShowQuickStart.value = false
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
    
    // Track if initial sync has been done to prevent re-syncing on screen return
    private var hasCompletedInitialSync = false
    
    /**
     * Called when permission is granted.
     * Only syncs on first grant, not on subsequent returns to home screen.
     */
    fun onPermissionGranted() {
        val wasAlreadyGranted = _hasPermission.value
        _hasPermission.value = true
        
        // Only sync if this is the first time permission is granted in this session
        // This prevents re-syncing every time user returns to home screen
        if (!wasAlreadyGranted || !hasCompletedInitialSync) {
            syncPhotos()
        }
    }
    
    /**
     * Called when permission is denied.
     * Phase 4: 使用 AsyncState.Error 表示错误
     */
    fun onPermissionDenied() {
        _hasPermission.value = false
        _isLoading.value = false
        _syncState.value = AsyncState.Error("需要存储权限才能访问照片")
    }
    
    /**
     * Sync photos from MediaStore.
     * Phase 4: 使用 AsyncState 管理同步状态
     */
    fun syncPhotos() {
        if (!_hasPermission.value) return
        // Prevent multiple simultaneous syncs
        if (_syncState.value.isLoading) return
        
        viewModelScope.launch {
            _syncState.value = AsyncState.Loading
            try {
                val result = syncPhotosUseCase()
                hasCompletedInitialSync = true  // Mark sync as completed
                val message = if (result.isInitialSync) {
                    "已导入 ${result.newPhotosCount} 张照片"
                } else if (result.newPhotosCount > 0) {
                    "发现 ${result.newPhotosCount} 张新照片"
                } else {
                    null
                }
                _syncState.value = AsyncState.Success(message)
            } catch (e: Exception) {
                _syncState.value = AsyncState.Error("同步失败: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear sync result message.
     * Phase 4: 重置为 Idle 状态
     */
    fun clearSyncResult() {
        // 只有在成功状态时才清除
        if (_syncState.value is AsyncState.Success) {
            _syncState.value = AsyncState.Idle
        }
    }
    
    /**
     * Clear error message.
     * Phase 4: 重置为 Idle 状态
     */
    fun clearError() {
        // 只有在错误状态时才清除
        if (_syncState.value is AsyncState.Error) {
            _syncState.value = AsyncState.Idle
        }
    }
    
    // ==================== Phase 1-D: 整理模式选择 ====================
    
    /**
     * 显示整理模式选择弹窗
     */
    fun showSortModeSheet() {
        _showSortModeSheet.value = true
    }
    
    /**
     * 隐藏整理模式选择弹窗
     */
    fun hideSortModeSheet() {
        _showSortModeSheet.value = false
    }
}
