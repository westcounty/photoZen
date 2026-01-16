package com.fuyun.photozen.ui.screens.workflow

import androidx.activity.compose.BackHandler
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
import com.fuyun.photozen.data.model.PhotoStatus
import com.fuyun.photozen.ui.screens.flowsorter.FlowSorterContent
import com.fuyun.photozen.ui.screens.lighttable.LightTableContent
import com.fuyun.photozen.ui.theme.KeepGreen
import com.fuyun.photozen.ui.theme.MaybeAmber
import com.fuyun.photozen.ui.theme.TrashRed

/**
 * Workflow Screen - The immersive "Flow Tunnel" experience.
 * 
 * Manages the sequential flow through stages:
 * 1. SWIPE - Sort photos using swipe gestures (until done or user clicks "Next")
 * 2. COMPARE - Compare "Maybe" photos in Light Table (until done or user clicks "Next")
 * 3. TAGGING - Tag "Keep" photos with bubble tags (until done or user clicks "Finish")
 * 4. VICTORY - Show results and celebration
 * 
 * Features:
 * - Full-screen immersive mode (no bottom nav)
 * - Progress indicator at top
 * - "Next" button to proceed to next stage
 * - Back button confirmation to exit
 * - Automatic stage transitions when all items processed
 */
@Composable
fun WorkflowScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkflowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
    
    // Auto-advance when compare stage completes
    LaunchedEffect(uiState.maybeCount, uiState.currentStage) {
        if (uiState.currentStage == WorkflowStage.COMPARE && !uiState.hasMaybePhotos) {
            viewModel.onCompareAutoComplete()
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
                else -> 0
            },
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
                    stageSubtitle = uiState.stageSubtitle,
                    currentStage = uiState.currentStage,
                    progress = uiState.stageProgress,
                    canProceed = uiState.canProceedToNext,
                    nextButtonText = uiState.nextButtonText,
                    onClose = { viewModel.requestExit() },
                    onNext = { viewModel.requestNextStage() }
                )
            }
            
            // Stage content with animated transitions
            AnimatedContent(
                targetState = uiState.currentStage,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
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
                            onComplete = { viewModel.onCompareAutoComplete() },
                            onRequestNext = { viewModel.requestNextStage() }
                        )
                    }
                    WorkflowStage.TAGGING -> {
                        TaggingStageContent(
                            keepPhotos = uiState.sessionKeepPhotos,
                            onPhotoTagged = { viewModel.recordTagged() },
                            onComplete = { viewModel.requestNextStage() }
                        )
                    }
                    WorkflowStage.VICTORY -> {
                        VictoryScreen(
                            stats = uiState.stats,
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
 */
@Composable
private fun WorkflowTopBar(
    stageName: String,
    stageSubtitle: String,
    currentStage: WorkflowStage,
    progress: Float,
    canProceed: Boolean,
    nextButtonText: String,
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
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Stage indicator
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stageSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Next button
            if (canProceed && nextButtonText.isNotEmpty()) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (currentStage) {
                            WorkflowStage.SWIPE -> MaybeAmber
                            WorkflowStage.COMPARE -> KeepGreen
                            WorkflowStage.TAGGING -> KeepGreen
                            WorkflowStage.VICTORY -> KeepGreen
                        }
                    )
                ) {
                    Text(nextButtonText)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
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
                WorkflowStage.TAGGING -> KeepGreen
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message) = when (currentStage) {
        WorkflowStage.SWIPE -> "跳过剩余照片？" to "还有 $remainingCount 张照片未整理，确定要进入对比阶段吗？"
        WorkflowStage.COMPARE -> "跳过剩余对比？" to "还有 $remainingCount 张待定照片未处理，确定要进入分类阶段吗？"
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
                Text("确定跳过")
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
    onComplete: () -> Unit,
    onRequestNext: () -> Unit
) {
    if (!hasPhotos) {
        EmptyStageContent(
            emoji = "👍",
            title = "没有待定的照片",
            subtitle = "可以直接进入分类阶段",
            buttonText = "进入分类阶段",
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
