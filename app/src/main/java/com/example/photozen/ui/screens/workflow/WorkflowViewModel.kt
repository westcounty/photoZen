package com.example.photozen.ui.screens.workflow

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.GetDailyTaskStatusUseCase
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.GetUnsortedPhotosUseCase
import com.example.photozen.domain.usecase.SortPhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Workflow stages in the Flow Tunnel.
 * 
 * Dynamic flow based on cardSortingAlbumEnabled:
 * - Enabled: SWIPE -> COMPARE -> TRASH -> VICTORY (3 stages)
 * - Disabled: SWIPE -> COMPARE -> CLASSIFY -> TRASH -> VICTORY (4 stages)
 */
enum class WorkflowStage {
    /** Stage 1: Swipe to sort photos (Keep/Trash/Maybe) */
    SWIPE,
    /** Stage 2: Compare "Maybe" photos in Light Table */
    COMPARE,
    /** Stage 3: Classify "Keep" photos to albums (only if cardSortingAlbumEnabled is false) */
    CLASSIFY,
    /** Stage 4: Clean up "Trash" photos */
    TRASH,
    /** Final: Show victory/results screen */
    VICTORY
}

/**
 * Statistics collected during a workflow session.
 */
data class WorkflowStats(
    val totalSorted: Int = 0,
    val keptCount: Int = 0,
    val trashedCount: Int = 0,
    val maybeCount: Int = 0,
    val taggedCount: Int = 0,
    val maxCombo: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    /** Photo IDs marked as MAYBE during this session */
    val sessionMaybePhotoIds: Set<String> = emptySet(),
    /** Photo IDs marked as KEEP during this session */
    val sessionKeepPhotoIds: Set<String> = emptySet(),
    /** Photo IDs marked as TRASH during this session */
    val sessionTrashPhotoIds: Set<String> = emptySet(),
    /** Number of photos classified to albums during CLASSIFY stage */
    val classifiedToAlbumCount: Int = 0,
    /** Number of photos skipped during CLASSIFY stage */
    val skippedClassifyCount: Int = 0,
    /** Number of photos permanently deleted during TRASH stage */
    val permanentlyDeletedCount: Int = 0,
    /** Number of photos restored during TRASH stage */
    val restoredFromTrashCount: Int = 0
) {
    val durationSeconds: Long
        get() = ((endTime ?: System.currentTimeMillis()) - startTime) / 1000
    
    val durationFormatted: String
        get() {
            val seconds = durationSeconds
            return when {
                seconds < 60 -> "${seconds}秒"
                seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
                else -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
            }
        }
}

/**
 * UI State for the Workflow screen.
 */
data class WorkflowUiState(
    val currentStage: WorkflowStage = WorkflowStage.SWIPE,
    val stats: WorkflowStats = WorkflowStats(),
    val unsortedCount: Int = 0,
    val maybeCount: Int = 0,
    val keepCount: Int = 0,
    val keepPhotos: List<PhotoEntity> = emptyList(),
    val isWorkflowActive: Boolean = false,
    val showExitConfirmation: Boolean = false,
    val showNextStageConfirmation: Boolean = false,
    /** Maybe photos from this session only */
    val sessionMaybePhotos: List<PhotoEntity> = emptyList(),
    /** Keep photos from this session only */
    val sessionKeepPhotos: List<PhotoEntity> = emptyList(),
    /** Trash photos from this session only */
    val sessionTrashPhotos: List<PhotoEntity> = emptyList(),
    // Daily Task Info
    val isDailyTask: Boolean = false,
    val dailyTaskTarget: Int = -1,
    val dailyTaskCurrent: Int = 0,
    // New stage support
    /** Whether card sorting album classification is enabled (affects workflow stages) */
    val cardSortingAlbumEnabled: Boolean = false,
    /** Albums available for classification */
    val albumBubbleList: List<com.example.photozen.data.local.entity.AlbumBubbleEntity> = emptyList(),
    /** Current index in CLASSIFY stage */
    val classifyModeIndex: Int = 0,
    /** Selected photo IDs in TRASH stage */
    val trashSelectedIds: Set<String> = emptySet(),
    /** Delete intent sender for system delete dialog */
    val deleteIntentSender: android.content.IntentSender? = null
) {
    /** Whether there are more photos to sort */
    val hasUnsortedPhotos: Boolean get() = unsortedCount > 0
    
    /** Whether there are maybe photos to compare (session-based) */
    val hasMaybePhotos: Boolean get() = sessionMaybePhotos.isNotEmpty()
    
    /** Whether there are keep photos to tag (session-based) */
    val hasKeepPhotos: Boolean get() = sessionKeepPhotos.isNotEmpty()
    
    /** Whether there are trash photos to clean (session-based) */
    val hasTrashPhotos: Boolean get() = sessionTrashPhotos.isNotEmpty()
    
    /** Session-based maybe count for display */
    val sessionMaybeCount: Int get() = sessionMaybePhotos.size
    
    /** Session-based keep count for display */
    val sessionKeepCount: Int get() = sessionKeepPhotos.size
    
    /** Session-based trash count for display */
    val sessionTrashCount: Int get() = sessionTrashPhotos.size
    
    /** Current photo to classify (if in CLASSIFY stage) */
    val currentClassifyPhoto: PhotoEntity? 
        get() = sessionKeepPhotos.getOrNull(classifyModeIndex)
    
    /** Dynamic stage list based on cardSortingAlbumEnabled */
    val stageList: List<WorkflowStage>
        get() = if (cardSortingAlbumEnabled) {
            listOf(WorkflowStage.SWIPE, WorkflowStage.COMPARE, WorkflowStage.TRASH, WorkflowStage.VICTORY)
        } else {
            listOf(WorkflowStage.SWIPE, WorkflowStage.COMPARE, WorkflowStage.CLASSIFY, WorkflowStage.TRASH, WorkflowStage.VICTORY)
        }
    
    /** Functional stages (excluding VICTORY) */
    val functionalStages: List<WorkflowStage>
        get() = stageList.filter { it != WorkflowStage.VICTORY }
    
    /** Get stage display name */
    val stageName: String get() = when (currentStage) {
        WorkflowStage.SWIPE -> "整理照片"
        WorkflowStage.COMPARE -> "对比待定"
        WorkflowStage.CLASSIFY -> "分类到相册"
        WorkflowStage.TRASH -> "清理回收站"
        WorkflowStage.VICTORY -> "完成"
    }
    
    /** Get stage subtitle with counts (uses session counts for COMPARE) */
    val stageSubtitle: String get() = when (currentStage) {
        WorkflowStage.SWIPE -> {
            if (isDailyTask) {
                "今日进度: $dailyTaskCurrent / $dailyTaskTarget"
            } else {
                if (unsortedCount > 0) "剩余 $unsortedCount 张" else "全部整理完成"
            }
        }
        WorkflowStage.COMPARE -> if (sessionMaybeCount > 0) "本次待定 $sessionMaybeCount 张" else "无待定照片"
        WorkflowStage.CLASSIFY -> "${classifyModeIndex + 1} / $sessionKeepCount"
        WorkflowStage.TRASH -> if (sessionTrashCount > 0) "本次回收站 $sessionTrashCount 张" else "无需清理"
        WorkflowStage.VICTORY -> "整理完成"
    }
    
    /** Whether "Next" button should be enabled */
    val canProceedToNext: Boolean get() = when (currentStage) {
        WorkflowStage.SWIPE -> true // Always can proceed (skip remaining unsorted)
        WorkflowStage.COMPARE -> true // Always can proceed (skip remaining maybe)
        WorkflowStage.CLASSIFY -> true // Always can proceed (skip remaining classify)
        WorkflowStage.TRASH -> true // Always can proceed (skip remaining trash cleanup)
        WorkflowStage.VICTORY -> false
    }
    
    /** Text for "Next" button */
    val nextButtonText: String get() {
        val isLastFunctionalStage = currentStage == functionalStages.lastOrNull()
        return when (currentStage) {
            WorkflowStage.SWIPE -> if (unsortedCount > 0) "下一步" else "进入对比"
            WorkflowStage.COMPARE -> if (sessionMaybeCount > 0) "下一步" else {
                if (cardSortingAlbumEnabled) "清理回收站" else "分类到相册"
            }
            WorkflowStage.CLASSIFY -> if (classifyModeIndex < sessionKeepCount) "下一步" else "清理回收站"
            WorkflowStage.TRASH -> if (isLastFunctionalStage) "完成整理" else "下一步"
            WorkflowStage.VICTORY -> ""
        }
    }
    
    /** Progress through the workflow (0-1) */
    val stageProgress: Float
        get() {
            val stages = functionalStages
            val currentIndex = stages.indexOf(currentStage)
            return if (currentIndex >= 0) {
                (currentIndex + 1).toFloat() / stages.size
            } else {
                1f // VICTORY
            }
        }
}

/**
 * ViewModel for managing the Flow Tunnel workflow.
 * 
 * Manages state transitions: SWIPE -> COMPARE -> TAGGING -> VICTORY
 * 
 * Each stage can end either:
 * - Automatically: when all items are processed
 * - Manually: when user clicks "Next" button
 */
@HiltViewModel
class WorkflowViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val getUnsortedPhotosUseCase: GetUnsortedPhotosUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val savedStateHandle: SavedStateHandle,
    private val getDailyTaskStatusUseCase: GetDailyTaskStatusUseCase,
    private val albumBubbleDao: AlbumBubbleDao,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val photoDao: PhotoDao
) : ViewModel() {
    
    private val isDailyTask: Boolean = savedStateHandle["isDailyTask"] ?: false
    private val targetCount: Int = savedStateHandle["targetCount"] ?: -1
    
    private val _internalState = MutableStateFlow(InternalState())
    
    // Camera album IDs for filtering
    private val _cameraAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _cameraAlbumsLoaded = MutableStateFlow(false)
    
    init {
        // Load camera album IDs for filtering
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
     * Get filtered unsorted count based on current filter mode.
     * This matches the same filtering logic used in FlowSorterViewModel.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getFilteredUnsortedCountFlow() = combine(
        preferencesRepository.getPhotoFilterMode(),
        _cameraAlbumIds,
        _cameraAlbumsLoaded,
        preferencesRepository.getSessionCustomFilterFlow()
    ) { filterMode, cameraIds, loaded, sessionFilter ->
        FilterParams(filterMode, cameraIds, loaded, sessionFilter)
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
                val filter = params.sessionFilter
                if (filter != null && !filter.albumIds.isNullOrEmpty()) {
                    getUnsortedPhotosUseCase.getCountByBuckets(filter.albumIds)
                } else {
                    getUnsortedPhotosUseCase.getCount()
                }
            }
        }
    }
    
    /**
     * Helper data class for filter parameters.
     */
    private data class FilterParams(
        val filterMode: PhotoFilterMode,
        val cameraIds: List<String>,
        val cameraLoaded: Boolean,
        val sessionFilter: com.example.photozen.data.repository.CustomFilterSession?
    )
    
    // Combined state flows for new stages
    private data class CombinedState(
        val internal: InternalState,
        val unsortedCount: Int,
        val maybePhotos: List<PhotoEntity>,
        val keepPhotos: List<PhotoEntity>,
        val trashPhotos: List<PhotoEntity>,
        val dailyStatus: com.example.photozen.domain.usecase.DailyTaskStatus?,
        val cardSortingAlbumEnabled: Boolean,
        val albumBubbleList: List<AlbumBubbleEntity>
    )
    
    val uiState: StateFlow<WorkflowUiState> = combine(
        _internalState,
        getFilteredUnsortedCountFlow(),
        getPhotosUseCase.getPhotosByStatus(PhotoStatus.MAYBE),
        getPhotosUseCase.getPhotosByStatus(PhotoStatus.KEEP),
        getPhotosUseCase.getPhotosByStatus(PhotoStatus.TRASH)
    ) { internal, unsortedCount, maybePhotos, keepPhotos, trashPhotos ->
        CombinedState(internal, unsortedCount, maybePhotos, keepPhotos, trashPhotos, null, false, emptyList())
    }.combine(
        if (isDailyTask) getDailyTaskStatusUseCase() else flowOf(null)
    ) { combined, dailyStatus ->
        combined.copy(dailyStatus = dailyStatus)
    }.combine(
        preferencesRepository.getCardSortingAlbumEnabled()
    ) { combined, cardSortingEnabled ->
        combined.copy(cardSortingAlbumEnabled = cardSortingEnabled)
    }.combine(
        albumBubbleDao.getAll()
    ) { combined, albums ->
        combined.copy(albumBubbleList = albums)
    }.combine(
        _internalState // Re-combine to get latest internal state
    ) { combined, internal ->
        // Filter to only session photos for COMPARE, CLASSIFY, and TRASH stages
        val sessionMaybePhotos = combined.maybePhotos.filter { it.id in internal.stats.sessionMaybePhotoIds }
        val sessionKeepPhotos = combined.keepPhotos.filter { it.id in internal.stats.sessionKeepPhotoIds }
        val sessionTrashPhotos = combined.trashPhotos.filter { it.id in internal.stats.sessionTrashPhotoIds }
        
        var displayedUnsortedCount = combined.unsortedCount
        
        // Handle Daily Task Logic for counts
        if (isDailyTask && combined.dailyStatus != null) {
            val dailyCurrent = combined.dailyStatus.current
            val needed = (targetCount - dailyCurrent).coerceAtLeast(0)
            
            // Limit displayed unsorted count to what's needed
            if (displayedUnsortedCount > needed) {
                displayedUnsortedCount = needed
            }
        }
        
        WorkflowUiState(
            currentStage = internal.currentStage,
            stats = internal.stats,
            unsortedCount = displayedUnsortedCount,
            maybeCount = combined.maybePhotos.size,
            keepCount = combined.keepPhotos.size,
            keepPhotos = combined.keepPhotos,
            isWorkflowActive = internal.isWorkflowActive,
            showExitConfirmation = internal.showExitConfirmation,
            showNextStageConfirmation = internal.showNextStageConfirmation,
            sessionMaybePhotos = sessionMaybePhotos,
            sessionKeepPhotos = sessionKeepPhotos,
            sessionTrashPhotos = sessionTrashPhotos,
            isDailyTask = isDailyTask,
            dailyTaskTarget = targetCount,
            dailyTaskCurrent = combined.dailyStatus?.current ?: 0,
            cardSortingAlbumEnabled = combined.cardSortingAlbumEnabled,
            albumBubbleList = combined.albumBubbleList,
            classifyModeIndex = internal.classifyModeIndex,
            trashSelectedIds = internal.trashSelectedIds,
            deleteIntentSender = internal.deleteIntentSender
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkflowUiState()
    )
    
    /**
     * Start a new workflow session.
     */
    fun startWorkflow() {
        _internalState.update { state ->
            state.copy(
                isWorkflowActive = true,
                currentStage = WorkflowStage.SWIPE,
                stats = WorkflowStats(startTime = System.currentTimeMillis())
            )
        }
    }
    
    /**
     * Record a photo sort action.
     * @param photoId The ID of the photo that was sorted
     * @param status The status the photo was sorted to
     * @param currentCombo The current combo count
     */
    fun recordSort(photoId: String, status: PhotoStatus, currentCombo: Int) {
        _internalState.update { state ->
            val newStats = state.stats.copy(
                totalSorted = state.stats.totalSorted + 1,
                keptCount = state.stats.keptCount + if (status == PhotoStatus.KEEP) 1 else 0,
                trashedCount = state.stats.trashedCount + if (status == PhotoStatus.TRASH) 1 else 0,
                maybeCount = state.stats.maybeCount + if (status == PhotoStatus.MAYBE) 1 else 0,
                maxCombo = maxOf(state.stats.maxCombo, currentCombo),
                // Track session photo IDs
                sessionMaybePhotoIds = if (status == PhotoStatus.MAYBE) {
                    state.stats.sessionMaybePhotoIds + photoId
                } else {
                    state.stats.sessionMaybePhotoIds
                },
                sessionKeepPhotoIds = if (status == PhotoStatus.KEEP) {
                    state.stats.sessionKeepPhotoIds + photoId
                } else {
                    state.stats.sessionKeepPhotoIds
                },
                sessionTrashPhotoIds = if (status == PhotoStatus.TRASH) {
                    state.stats.sessionTrashPhotoIds + photoId
                } else {
                    state.stats.sessionTrashPhotoIds
                }
            )
            state.copy(stats = newStats)
        }
    }
    
    /**
     * Record a photo sort action (legacy, without photo ID).
     * @deprecated Use recordSort(photoId, status, currentCombo) instead
     */
    @Deprecated("Use recordSort with photoId", ReplaceWith("recordSort(photoId, status, currentCombo)"))
    fun recordSort(status: PhotoStatus, currentCombo: Int) {
        _internalState.update { state ->
            val newStats = state.stats.copy(
                totalSorted = state.stats.totalSorted + 1,
                keptCount = state.stats.keptCount + if (status == PhotoStatus.KEEP) 1 else 0,
                trashedCount = state.stats.trashedCount + if (status == PhotoStatus.TRASH) 1 else 0,
                maybeCount = state.stats.maybeCount + if (status == PhotoStatus.MAYBE) 1 else 0,
                maxCombo = maxOf(state.stats.maxCombo, currentCombo)
            )
            state.copy(stats = newStats)
        }
    }
    
    /**
     * Record a photo tagged.
     */
    fun recordTagged() {
        _internalState.update { state ->
            val newStats = state.stats.copy(
                taggedCount = state.stats.taggedCount + 1
            )
            state.copy(stats = newStats)
        }
    }
    
    /**
     * Request to proceed to next stage.
     * Shows confirmation if there are remaining items.
     */
    fun requestNextStage() {
        val state = uiState.value
        val hasRemaining = when (state.currentStage) {
            WorkflowStage.SWIPE -> state.unsortedCount > 0
            WorkflowStage.COMPARE -> state.sessionMaybeCount > 0
            WorkflowStage.CLASSIFY -> state.classifyModeIndex < state.sessionKeepCount
            WorkflowStage.TRASH -> state.sessionTrashCount > 0
            WorkflowStage.VICTORY -> false
        }
        
        if (hasRemaining) {
            _internalState.update { it.copy(showNextStageConfirmation = true) }
        } else {
            proceedToNextStage()
        }
    }
    
    /**
     * Confirm proceeding to next stage.
     */
    fun confirmNextStage() {
        _internalState.update { it.copy(showNextStageConfirmation = false) }
        proceedToNextStage()
    }
    
    /**
     * Cancel next stage request.
     */
    fun cancelNextStage() {
        _internalState.update { it.copy(showNextStageConfirmation = false) }
    }
    
    /**
     * Proceed to the next stage.
     * Uses dynamic stage list based on cardSortingAlbumEnabled.
     */
    private fun proceedToNextStage() {
        val currentUiState = uiState.value
        val stageList = currentUiState.stageList
        val currentIndex = stageList.indexOf(currentUiState.currentStage)
        
        _internalState.update { state ->
            val nextStage = if (currentIndex >= 0 && currentIndex < stageList.size - 1) {
                stageList[currentIndex + 1]
            } else {
                WorkflowStage.VICTORY
            }
            
            val newStats = if (nextStage == WorkflowStage.VICTORY) {
                state.stats.copy(endTime = System.currentTimeMillis())
            } else {
                state.stats
            }
            
            // Reset classify index when entering CLASSIFY stage
            val newClassifyIndex = if (nextStage == WorkflowStage.CLASSIFY) 0 else state.classifyModeIndex
            // Clear trash selection when entering TRASH stage
            val newTrashSelection = if (nextStage == WorkflowStage.TRASH) emptySet() else state.trashSelectedIds
            
            state.copy(
                currentStage = nextStage, 
                stats = newStats,
                classifyModeIndex = newClassifyIndex,
                trashSelectedIds = newTrashSelection
            )
        }
    }
    
    /**
     * Called when swipe stage auto-completes (all photos sorted).
     */
    fun onSwipeAutoComplete() {
        // Auto advance only if we've sorted at least one photo
        if (uiState.value.stats.totalSorted > 0) {
            proceedToNextStage()
        }
    }
    
    /**
     * Called when compare stage auto-completes (all maybe photos decided).
     */
    fun onCompareAutoComplete() {
        proceedToNextStage()
    }
    
    /**
     * Request to exit the workflow.
     */
    fun requestExit() {
        _internalState.update { it.copy(showExitConfirmation = true) }
    }
    
    /**
     * Confirm exit from workflow.
     */
    fun confirmExit() {
        _internalState.update { state ->
            state.copy(
                isWorkflowActive = false,
                showExitConfirmation = false,
                stats = state.stats.copy(endTime = System.currentTimeMillis())
            )
        }
    }
    
    /**
     * Cancel exit request.
     */
    fun cancelExit() {
        _internalState.update { it.copy(showExitConfirmation = false) }
    }
    
    /**
     * End the workflow and return to home.
     */
    fun finishWorkflow() {
        _internalState.update { state ->
            state.copy(
                isWorkflowActive = false,
                currentStage = WorkflowStage.SWIPE,
                stats = WorkflowStats(),
                classifyModeIndex = 0,
                trashSelectedIds = emptySet(),
                deleteIntentSender = null
            )
        }
    }
    
    private data class InternalState(
        val currentStage: WorkflowStage = WorkflowStage.SWIPE,
        val stats: WorkflowStats = WorkflowStats(),
        val isWorkflowActive: Boolean = false,
        val showExitConfirmation: Boolean = false,
        val showNextStageConfirmation: Boolean = false,
        // New stage support
        val classifyModeIndex: Int = 0,
        val trashSelectedIds: Set<String> = emptySet(),
        val deleteIntentSender: IntentSender? = null
    )
    
    // ==================== CLASSIFY Stage Methods ====================
    
    /**
     * Classify current photo to an album.
     */
    fun classifyPhotoToAlbum(bucketId: String) {
        viewModelScope.launch {
            val currentPhoto = uiState.value.currentClassifyPhoto ?: return@launch
            val album = uiState.value.albumBubbleList.find { it.bucketId == bucketId } ?: return@launch
            
            // Get target album path
            val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                ?: "Pictures/${album.displayName}"
            
            // Copy photo to album
            val photoUri = Uri.parse(currentPhoto.systemUri)
            val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
            
            if (result.isSuccess) {
                // Update bucket_id in database
                photoDao.updateBucketId(currentPhoto.id, bucketId)
                
                // Update stats and advance
                _internalState.update { state ->
                    state.copy(
                        stats = state.stats.copy(
                            classifiedToAlbumCount = state.stats.classifiedToAlbumCount + 1
                        ),
                        classifyModeIndex = state.classifyModeIndex + 1
                    )
                }
            }
        }
    }
    
    /**
     * Skip current photo classification.
     */
    fun skipClassifyPhoto() {
        _internalState.update { state ->
            state.copy(
                stats = state.stats.copy(
                    skippedClassifyCount = state.stats.skippedClassifyCount + 1
                ),
                classifyModeIndex = state.classifyModeIndex + 1
            )
        }
    }
    
    /**
     * Called when classify stage auto-completes (all keep photos processed).
     */
    fun onClassifyAutoComplete() {
        proceedToNextStage()
    }
    
    // ==================== TRASH Stage Methods ====================
    
    /**
     * Toggle photo selection in TRASH stage.
     */
    fun toggleTrashSelection(photoId: String) {
        _internalState.update { state ->
            val newSelection = if (photoId in state.trashSelectedIds) {
                state.trashSelectedIds - photoId
            } else {
                state.trashSelectedIds + photoId
            }
            state.copy(trashSelectedIds = newSelection)
        }
    }
    
    /**
     * Update trash selection (for drag select).
     */
    fun updateTrashSelection(selectedIds: Set<String>) {
        _internalState.update { state ->
            state.copy(trashSelectedIds = selectedIds)
        }
    }
    
    /**
     * Select all trash photos.
     */
    fun selectAllTrash() {
        val allIds = uiState.value.sessionTrashPhotos.map { it.id }.toSet()
        _internalState.update { it.copy(trashSelectedIds = allIds) }
    }
    
    /**
     * Clear trash selection.
     */
    fun clearTrashSelection() {
        _internalState.update { it.copy(trashSelectedIds = emptySet()) }
    }
    
    /**
     * Restore selected trash photos to a status.
     */
    fun restoreTrashPhotos(targetStatus: PhotoStatus) {
        viewModelScope.launch {
            val selectedIds = _internalState.value.trashSelectedIds.toList()
            if (selectedIds.isEmpty()) return@launch
            
            for (photoId in selectedIds) {
                sortPhotoUseCase.updateStatus(photoId, targetStatus)
            }
            
            // Update stats - restored photos
            _internalState.update { state ->
                state.copy(
                    stats = state.stats.copy(
                        restoredFromTrashCount = state.stats.restoredFromTrashCount + selectedIds.size,
                        // Remove restored photos from session trash
                        sessionTrashPhotoIds = state.stats.sessionTrashPhotoIds - selectedIds.toSet()
                    ),
                    trashSelectedIds = emptySet()
                )
            }
        }
    }
    
    /**
     * Request permanent delete of selected trash photos.
     */
    fun requestPermanentDelete() {
        viewModelScope.launch {
            val selectedIds = _internalState.value.trashSelectedIds.toList()
            if (selectedIds.isEmpty()) return@launch
            
            val photos = uiState.value.sessionTrashPhotos.filter { it.id in selectedIds }
            val uris = photos.map { Uri.parse(it.systemUri) }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val deleteRequest = MediaStore.createDeleteRequest(
                        context.contentResolver,
                        uris
                    )
                    _internalState.update { it.copy(deleteIntentSender = deleteRequest.intentSender) }
                } catch (e: Exception) {
                    // Handle error
                }
            } else {
                // For older versions, delete directly
                for (photo in photos) {
                    try {
                        context.contentResolver.delete(Uri.parse(photo.systemUri), null, null)
                        photoDao.deleteById(photo.id)
                    } catch (e: Exception) {
                        // Ignore individual failures
                    }
                }
                onDeleteComplete(true)
            }
        }
    }
    
    /**
     * Clear delete intent sender.
     */
    fun clearDeleteIntentSender() {
        _internalState.update { it.copy(deleteIntentSender = null) }
    }
    
    /**
     * Handle delete completion result.
     */
    fun onDeleteComplete(success: Boolean) {
        if (success) {
            viewModelScope.launch {
                val selectedIds = _internalState.value.trashSelectedIds.toList()
                
                // Delete from Room database
                for (photoId in selectedIds) {
                    photoDao.deleteById(photoId)
                }
                
                // Update stats
                _internalState.update { state ->
                    state.copy(
                        stats = state.stats.copy(
                            permanentlyDeletedCount = state.stats.permanentlyDeletedCount + selectedIds.size,
                            // Remove deleted photos from session trash
                            sessionTrashPhotoIds = state.stats.sessionTrashPhotoIds - selectedIds.toSet()
                        ),
                        trashSelectedIds = emptySet()
                    )
                }
            }
        }
    }
    
    /**
     * Called when trash stage auto-completes (all trash photos cleaned).
     */
    fun onTrashAutoComplete() {
        proceedToNextStage()
    }
}
