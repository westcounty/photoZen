package com.example.photozen.ui.screens.workflow

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.screens.flowsorter.FlowSorterContent
import com.example.photozen.ui.screens.lighttable.LightTableContent
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * Workflow Screen - The immersive "Flow Tunnel" experience.
 * 
 * Manages the sequential flow through stages (dynamic based on cardSortingAlbumEnabled):
 * 
 * When cardSortingAlbumEnabled = true (3 stages):
 * 1. SWIPE - Sort photos using swipe gestures
 * 2. COMPARE - Compare "Maybe" photos in Light Table
 * 3. TRASH - Clean up trash photos
 * 4. VICTORY - Show results and celebration
 * 
 * When cardSortingAlbumEnabled = false (4 stages):
 * 1. SWIPE - Sort photos using swipe gestures
 * 2. COMPARE - Compare "Maybe" photos in Light Table
 * 3. CLASSIFY - Classify "Keep" photos to albums
 * 4. TRASH - Clean up trash photos
 * 5. VICTORY - Show results and celebration
 * 
 * Features:
 * - Full-screen immersive mode (no bottom nav)
 * - Dynamic progress indicator at top
 * - "Next" button to proceed to next stage
 * - Back button confirmation to exit
 * - Automatic stage transitions when all items processed
 * - Empty stage auto-skip with friendly messages
 */
@Composable
fun WorkflowScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkflowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Launcher for system delete request
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeleteComplete(result.resultCode == Activity.RESULT_OK)
    }
    
    // Start workflow on screen launch
    LaunchedEffect(Unit) {
        viewModel.startWorkflow()
    }
    
    // Handle back button press
    BackHandler {
        if (uiState.currentStage == WorkflowStage.VICTORY) {
            viewModel.finishWorkflow()
            onExit()
        } else {
            viewModel.requestExit()
        }
    }
    
    // Auto-advance when swipe stage completes (only if sorted at least 1 photo)
    LaunchedEffect(uiState.unsortedCount, uiState.currentStage, uiState.stats.totalSorted) {
        if (uiState.currentStage == WorkflowStage.SWIPE && 
            !uiState.hasUnsortedPhotos && 
            uiState.stats.totalSorted > 0) {
            viewModel.onSwipeAutoComplete()
        }
    }
    
    // Auto-advance when compare stage completes (no session maybe photos)
    LaunchedEffect(uiState.sessionMaybeCount, uiState.currentStage) {
        if (uiState.currentStage == WorkflowStage.COMPARE && !uiState.hasMaybePhotos) {
            viewModel.onCompareAutoComplete()
        }
    }
    
    // Auto-advance when classify stage completes
    LaunchedEffect(uiState.classifyModeIndex, uiState.sessionKeepCount, uiState.currentStage) {
        if (uiState.currentStage == WorkflowStage.CLASSIFY && 
            uiState.classifyModeIndex >= uiState.sessionKeepCount) {
            viewModel.onClassifyAutoComplete()
        }
    }
    
    // Auto-advance when trash stage completes (no session trash photos)
    LaunchedEffect(uiState.sessionTrashCount, uiState.currentStage) {
        if (uiState.currentStage == WorkflowStage.TRASH && !uiState.hasTrashPhotos) {
            viewModel.onTrashAutoComplete()
        }
    }
    
    // Launch system delete dialog when intent sender is available
    LaunchedEffect(uiState.deleteIntentSender) {
        uiState.deleteIntentSender?.let { intentSender ->
            deleteLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
            viewModel.clearDeleteIntentSender()
        }
    }
    
    // Exit confirmation dialog
    if (uiState.showExitConfirmation) {
        ExitConfirmationDialog(
            onConfirm = {
                viewModel.confirmExit()
                onExit()
            },
            onDismiss = { viewModel.cancelExit() }
        )
    }
    
    // Next stage confirmation dialog
    if (uiState.showNextStageConfirmation) {
        NextStageConfirmationDialog(
            currentStage = uiState.currentStage,
            remainingCount = when (uiState.currentStage) {
                WorkflowStage.SWIPE -> uiState.unsortedCount
                WorkflowStage.COMPARE -> uiState.sessionMaybeCount
                WorkflowStage.CLASSIFY -> uiState.sessionKeepCount - uiState.classifyModeIndex
                WorkflowStage.TRASH -> uiState.sessionTrashCount
                else -> 0
            },
            isLastFunctionalStage = uiState.currentStage == uiState.functionalStages.lastOrNull(),
            onConfirm = { viewModel.confirmNextStage() },
            onDismiss = { viewModel.cancelNextStage() }
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with progress (only in non-victory stages)
            if (uiState.currentStage != WorkflowStage.VICTORY) {
                WorkflowTopBar(
                    stageName = uiState.stageName,
                    currentStage = uiState.currentStage,
                    sortedCount = uiState.stats.totalSorted,
                    unsortedCount = uiState.unsortedCount,
                    sessionMaybeCount = uiState.sessionMaybeCount,
                    sessionKeepCount = uiState.sessionKeepCount,
                    sessionTrashCount = uiState.sessionTrashCount,
                    classifyModeIndex = uiState.classifyModeIndex,
                    isDailyTask = uiState.isDailyTask,
                    dailyTaskCurrent = uiState.dailyTaskCurrent,
                    dailyTaskTarget = uiState.dailyTaskTarget,
                    progress = uiState.stageProgress,
                    canProceed = uiState.canProceedToNext,
                    nextButtonText = uiState.nextButtonText,
                    isLastFunctionalStage = uiState.currentStage == uiState.functionalStages.lastOrNull(),
                    onClose = { viewModel.requestExit() },
                    onNext = { viewModel.requestNextStage() }
                )
            }
            
            // Stage content with animated transitions
            AnimatedContent(
                targetState = uiState.currentStage,
                transitionSpec = {
                    val stageList = uiState.stageList
                    val targetIndex = stageList.indexOf(targetState)
                    val initialIndex = stageList.indexOf(initialState)
                    val direction = if (targetIndex > initialIndex) 1 else -1
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { it * direction }
                    ) + fadeIn() togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { -it * direction }
                    ) + fadeOut()
                },
                modifier = Modifier.weight(1f),
                label = "stage_transition"
            ) { stage ->
                when (stage) {
                    WorkflowStage.SWIPE -> {
                        SwipeStageContent(
                            onPhotoSorted = { photoId, status, combo -> 
                                viewModel.recordSort(photoId, status, combo) 
                            },
                            onComplete = { viewModel.onSwipeAutoComplete() }
                        )
                    }
                    WorkflowStage.COMPARE -> {
                        CompareStageContent(
                            hasPhotos = uiState.hasMaybePhotos,
                            sessionPhotoIds = uiState.stats.sessionMaybePhotoIds,
                            cardSortingAlbumEnabled = uiState.cardSortingAlbumEnabled,
                            onComplete = { viewModel.onCompareAutoComplete() },
                            onRequestNext = { viewModel.requestNextStage() }
                        )
                    }
                    WorkflowStage.CLASSIFY -> {
                        ClassifyStageContent(
                            currentPhoto = uiState.currentClassifyPhoto,
                            currentIndex = uiState.classifyModeIndex,
                            totalCount = uiState.sessionKeepCount,
                            albums = uiState.albumBubbleList,
                            onAddToAlbum = { bucketId -> viewModel.classifyPhotoToAlbum(bucketId) },
                            onSkip = { viewModel.skipClassifyPhoto() },
                            onComplete = { viewModel.onClassifyAutoComplete() }
                        )
                    }
                    WorkflowStage.TRASH -> {
                        TrashStageContent(
                            photos = uiState.sessionTrashPhotos,
                            selectedIds = uiState.trashSelectedIds,
                            onSelectionChanged = { viewModel.updateTrashSelection(it) },
                            onSelectAll = { viewModel.selectAllTrash() },
                            onClearSelection = { viewModel.clearTrashSelection() },
                            onRestoreToKeep = { viewModel.restoreTrashPhotos(PhotoStatus.KEEP) },
                            onRestoreToMaybe = { viewModel.restoreTrashPhotos(PhotoStatus.MAYBE) },
                            onPermanentDelete = { viewModel.requestPermanentDelete() },
                            onComplete = { viewModel.onTrashAutoComplete() }
                        )
                    }
                    WorkflowStage.VICTORY -> {
                        VictoryScreen(
                            stats = uiState.stats,
                            cardSortingAlbumEnabled = uiState.cardSortingAlbumEnabled,
                            onReturnHome = {
                                viewModel.finishWorkflow()
                                onExit()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top bar showing workflow progress and controls.
 * Properly handles status bar insets to avoid overlap.
 * Displays sorted/unsorted counts in two lines to avoid conflict with buttons.
 */
@Composable
private fun WorkflowTopBar(
    stageName: String,
    currentStage: WorkflowStage,
    sortedCount: Int,
    unsortedCount: Int,
    sessionMaybeCount: Int,
    sessionKeepCount: Int,
    sessionTrashCount: Int,
    classifyModeIndex: Int,
    isDailyTask: Boolean,
    dailyTaskCurrent: Int,
    dailyTaskTarget: Int,
    progress: Float,
    canProceed: Boolean,
    nextButtonText: String,
    isLastFunctionalStage: Boolean,
    onClose: () -> Unit,
    onNext: () -> Unit
) {
    // Get status bar padding
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = statusBarPadding.calculateTopPadding())
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "退出",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Stage indicator with two-line counts
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stageName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Display counts based on current stage
                when (currentStage) {
                    WorkflowStage.SWIPE -> {
                        if (isDailyTask) {
                            Text(
                                text = "今日 $dailyTaskCurrent/$dailyTaskTarget",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "已整理 $sortedCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = KeepGreen
                            )
                            Text(
                                text = "待整理 $unsortedCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    WorkflowStage.COMPARE -> {
                        Text(
                            text = "待定 $sessionMaybeCount 张",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaybeAmber
                        )
                    }
                    WorkflowStage.CLASSIFY -> {
                        Text(
                            text = "${classifyModeIndex + 1}/$sessionKeepCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeepGreen
                        )
                    }
                    WorkflowStage.TRASH -> {
                        Text(
                            text = "回收站 $sessionTrashCount 张",
                            style = MaterialTheme.typography.labelSmall,
                            color = TrashRed
                        )
                    }
                    WorkflowStage.VICTORY -> {
                        Text(
                            text = "完成",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeepGreen
                        )
                    }
                }
            }
            
            // Next button
            if (canProceed && nextButtonText.isNotEmpty()) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isLastFunctionalStage -> KeepGreen
                            currentStage == WorkflowStage.SWIPE -> MaybeAmber
                            currentStage == WorkflowStage.COMPARE -> MaybeAmber
                            currentStage == WorkflowStage.CLASSIFY -> KeepGreen
                            currentStage == WorkflowStage.TRASH -> KeepGreen
                            else -> KeepGreen
                        }
                    )
                ) {
                    Text(nextButtonText)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (isLastFunctionalStage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = when (currentStage) {
                WorkflowStage.SWIPE -> MaterialTheme.colorScheme.primary
                WorkflowStage.COMPARE -> MaybeAmber
                WorkflowStage.CLASSIFY -> KeepGreen
                WorkflowStage.TRASH -> TrashRed
                WorkflowStage.VICTORY -> KeepGreen
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Exit confirmation dialog.
 */
@Composable
private fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确定要退出吗？") },
        text = { Text("当前进度将被保存，下次可以继续整理。") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = TrashRed)
            ) {
                Text("退出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("继续整理")
            }
        }
    )
}

/**
 * Next stage confirmation dialog.
 */
@Composable
private fun NextStageConfirmationDialog(
    currentStage: WorkflowStage,
    remainingCount: Int,
    isLastFunctionalStage: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message) = when (currentStage) {
        WorkflowStage.SWIPE -> "跳过剩余照片？" to "还有 $remainingCount 张照片未整理，确定要进入对比阶段吗？"
        WorkflowStage.COMPARE -> "跳过剩余对比？" to "还有 $remainingCount 张待定照片未处理，确定要继续吗？"
        WorkflowStage.CLASSIFY -> "跳过剩余分类？" to "还有 $remainingCount 张保留照片未分类到相册，确定要继续吗？"
        WorkflowStage.TRASH -> {
            if (isLastFunctionalStage) {
                "跳过清理回收站？" to "还有 $remainingCount 张照片在回收站未处理，确定要完成整理吗？"
            } else {
                "跳过清理回收站？" to "还有 $remainingCount 张照片在回收站未处理，确定要继续吗？"
            }
        }
        else -> "确定要继续？" to "确定要进入下一阶段吗？"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaybeAmber)
            ) {
                Text(if (isLastFunctionalStage) "确定完成" else "确定跳过")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("继续处理")
            }
        }
    )
}

/**
 * Content wrapper for the Swipe stage.
 * Uses the existing FlowSorter logic.
 * 
 * Note: Don't conditionally render based on external state - let FlowSorterContent
 * manage its own state to avoid crashes during animations.
 */
@Composable
private fun SwipeStageContent(
    onPhotoSorted: (String, PhotoStatus, Int) -> Unit,
    onComplete: () -> Unit
) {
    // Let FlowSorterContent manage everything internally
    // This prevents crashes when the photo list updates during swipe animation
    FlowSorterContent(
        isWorkflowMode = true,
        onPhotoSorted = onPhotoSorted,
        onComplete = onComplete,
        onNavigateBack = { /* Handled by parent */ }
    )
}

/**
 * Content wrapper for the Compare stage.
 * Uses the existing LightTable logic.
 */
@Composable
private fun CompareStageContent(
    hasPhotos: Boolean,
    sessionPhotoIds: Set<String>,
    cardSortingAlbumEnabled: Boolean,
    onComplete: () -> Unit,
    onRequestNext: () -> Unit
) {
    if (!hasPhotos) {
        val nextStageText = if (cardSortingAlbumEnabled) "清理回收站" else "分类到相册"
        EmptyStageContent(
            emoji = "✨",
            title = "没有待定的照片",
            subtitle = "可以直接进入$nextStageText",
            buttonText = nextStageText,
            onButtonClick = onRequestNext
        )
    } else {
        // Reuse LightTable content with session filter
        LightTableContent(
            isWorkflowMode = true,
            sessionPhotoIds = sessionPhotoIds,
            onComplete = onComplete,
            onNavigateBack = { /* Handled by parent */ }
        )
    }
}

/**
 * Empty stage placeholder.
 */
@Composable
private fun EmptyStageContent(
    emoji: String,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onButtonClick) {
                Text(buttonText)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
