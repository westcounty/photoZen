package com.example.photozen.ui.screens.workflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.domain.usecase.GetPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Workflow stages in the Flow Tunnel.
 * 
 * Flow: SWIPE -> COMPARE -> TAGGING -> VICTORY
 */
enum class WorkflowStage {
    /** Stage 1: Swipe to sort photos (Keep/Trash/Maybe) */
    SWIPE,
    /** Stage 2: Compare "Maybe" photos in Light Table */
    COMPARE,
    /** Stage 3: Tag the "Keep" photos with bubble tags */
    TAGGING,
    /** Stage 4: Show victory/results screen */
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
    val endTime: Long? = null
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
    val showNextStageConfirmation: Boolean = false
) {
    /** Whether there are more photos to sort */
    val hasUnsortedPhotos: Boolean get() = unsortedCount > 0
    
    /** Whether there are maybe photos to compare */
    val hasMaybePhotos: Boolean get() = maybeCount > 0
    
    /** Whether there are keep photos to tag */
    val hasKeepPhotos: Boolean get() = keepCount > 0
    
    /** Get stage display name */
    val stageName: String get() = when (currentStage) {
        WorkflowStage.SWIPE -> "整理照片"
        WorkflowStage.COMPARE -> "对比待定"
        WorkflowStage.TAGGING -> "快速分类"
        WorkflowStage.VICTORY -> "完成"
    }
    
    /** Get stage subtitle with counts */
    val stageSubtitle: String get() = when (currentStage) {
        WorkflowStage.SWIPE -> if (unsortedCount > 0) "剩余 $unsortedCount 张" else "全部整理完成"
        WorkflowStage.COMPARE -> if (maybeCount > 0) "待定 $maybeCount 张" else "无待定照片"
        WorkflowStage.TAGGING -> if (keepCount > 0) "保留 $keepCount 张" else "无保留照片"
        WorkflowStage.VICTORY -> "整理完成"
    }
    
    /** Whether "Next" button should be enabled */
    val canProceedToNext: Boolean get() = when (currentStage) {
        WorkflowStage.SWIPE -> true // Always can proceed (skip remaining unsorted)
        WorkflowStage.COMPARE -> true // Always can proceed (skip remaining maybe)
        WorkflowStage.TAGGING -> true // Always can proceed (skip remaining untagged)
        WorkflowStage.VICTORY -> false
    }
    
    /** Text for "Next" button */
    val nextButtonText: String get() = when (currentStage) {
        WorkflowStage.SWIPE -> if (unsortedCount > 0) "下一步" else "进入对比"
        WorkflowStage.COMPARE -> if (maybeCount > 0) "下一步" else "进入分类"
        WorkflowStage.TAGGING -> if (keepCount > 0) "完成" else "查看结果"
        WorkflowStage.VICTORY -> ""
    }
    
    /** Progress through the workflow (0-1) */
    val stageProgress: Float
        get() = when (currentStage) {
            WorkflowStage.SWIPE -> 0.25f
            WorkflowStage.COMPARE -> 0.5f
            WorkflowStage.TAGGING -> 0.75f
            WorkflowStage.VICTORY -> 1f
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
    private val getPhotosUseCase: GetPhotosUseCase
) : ViewModel() {
    
    private val _internalState = MutableStateFlow(InternalState())
    
    val uiState: StateFlow<WorkflowUiState> = combine(
        _internalState,
        getPhotosUseCase.getCountByStatus(PhotoStatus.UNSORTED),
        getPhotosUseCase.getCountByStatus(PhotoStatus.MAYBE),
        getPhotosUseCase.getCountByStatus(PhotoStatus.KEEP),
        getPhotosUseCase.getPhotosByStatus(PhotoStatus.KEEP)
    ) { internal, unsortedCount, maybeCount, keepCount, keepPhotos ->
        WorkflowUiState(
            currentStage = internal.currentStage,
            stats = internal.stats,
            unsortedCount = unsortedCount,
            maybeCount = maybeCount,
            keepCount = keepCount,
            keepPhotos = keepPhotos,
            isWorkflowActive = internal.isWorkflowActive,
            showExitConfirmation = internal.showExitConfirmation,
            showNextStageConfirmation = internal.showNextStageConfirmation
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
     */
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
            WorkflowStage.COMPARE -> state.maybeCount > 0
            WorkflowStage.TAGGING -> false // No confirmation needed for tagging
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
     */
    private fun proceedToNextStage() {
        _internalState.update { state ->
            val nextStage = when (state.currentStage) {
                WorkflowStage.SWIPE -> WorkflowStage.COMPARE
                WorkflowStage.COMPARE -> WorkflowStage.TAGGING
                WorkflowStage.TAGGING -> WorkflowStage.VICTORY
                WorkflowStage.VICTORY -> WorkflowStage.VICTORY
            }
            
            val newStats = if (nextStage == WorkflowStage.VICTORY) {
                state.stats.copy(endTime = System.currentTimeMillis())
            } else {
                state.stats
            }
            
            state.copy(currentStage = nextStage, stats = newStats)
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
                stats = WorkflowStats()
            )
        }
    }
    
    private data class InternalState(
        val currentStage: WorkflowStage = WorkflowStage.SWIPE,
        val stats: WorkflowStats = WorkflowStats(),
        val isWorkflowActive: Boolean = false,
        val showExitConfirmation: Boolean = false,
        val showNextStageConfirmation: Boolean = false
    )
}
