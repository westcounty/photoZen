package com.example.photozen.ui.screens.flowsorter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.components.FullscreenPhotoViewer
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * Flow Sorter Screen - Tinder-style swipe interface for sorting photos.
 * 
 * Gestures:
 * - Swipe Left → Trash (delete)
 * - Swipe Right → Keep (preserve)
 * - Swipe Up → Maybe (review later in Light Table)
 * - Tap Photo → Fullscreen view with pinch-to-zoom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowSorterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLightTable: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlowSorterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    
    // Fullscreen viewer state
    var fullscreenPhoto by remember { mutableStateOf<PhotoEntity?>(null) }
    
    // Handle back press when in fullscreen mode
    BackHandler(enabled = fullscreenPhoto != null) {
        fullscreenPhoto = null
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Flow Sorter",
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (uiState.totalCount > 0) {
                                Text(
                                    text = "${uiState.sortedCount} / ${uiState.totalCount} 已整理",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        // Undo button
                        AnimatedVisibility(
                            visible = uiState.lastAction != null,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.undoLastAction()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "撤销"
                                )
                            }
                        }
                        
                        // Refresh button
                        IconButton(
                            onClick = { viewModel.syncPhotos() },
                            enabled = !uiState.isSyncing
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Progress bar
                if (uiState.totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = KeepGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                
                // Main content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        uiState.isLoading -> {
                            LoadingContent()
                        }
                        uiState.isComplete -> {
                            CompletionContent(
                                keepCount = uiState.keepCount,
                                trashCount = uiState.trashCount,
                                maybeCount = uiState.maybeCount,
                                onNavigateToLightTable = onNavigateToLightTable,
                                onGoBack = onNavigateBack
                            )
                        }
                        else -> {
                            // Card stack
                            CardStack(
                                uiState = uiState,
                                onSwipeLeft = { viewModel.trashCurrentPhoto() },
                                onSwipeRight = { viewModel.keepCurrentPhoto() },
                                onSwipeUp = { viewModel.maybeCurrentPhoto() },
                                onPhotoClick = { photo ->
                                    fullscreenPhoto = photo
                                }
                            )
                        }
                    }
                }
                
                // Action buttons (only show when there are photos)
                if (!uiState.isLoading && !uiState.isComplete) {
                    ActionButtons(
                        onTrash = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.trashCurrentPhoto()
                        },
                        onKeep = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.keepCurrentPhoto()
                        },
                        onMaybe = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.maybeCurrentPhoto()
                        },
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
        }
        
        // Fullscreen photo viewer overlay
        AnimatedContent(
            targetState = fullscreenPhoto,
            transitionSpec = {
                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                        scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                    )
            },
            label = "fullscreen"
        ) { photo ->
            if (photo != null) {
                FullscreenPhotoViewer(
                    photo = photo,
                    onDismiss = { fullscreenPhoto = null }
                )
            }
        }
    }
}

/**
 * Card stack showing current and next photos.
 */
@Composable
private fun CardStack(
    uiState: FlowSorterUiState,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onPhotoClick: (PhotoEntity) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Preview card (behind)
        uiState.nextPhoto?.let { nextPhoto ->
            PreviewPhotoCard(
                photo = nextPhoto,
                stackIndex = 1
            )
        }
        
        // Current card (front, swipeable)
        uiState.currentPhoto?.let { currentPhoto ->
            SwipeablePhotoCard(
                photo = currentPhoto,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
                onSwipeUp = onSwipeUp,
                onPhotoClick = { onPhotoClick(currentPhoto) }
            )
        }
    }
}

/**
 * Bottom action buttons for sorting.
 */
@Composable
private fun ActionButtons(
    onTrash: () -> Unit,
    onKeep: () -> Unit,
    onMaybe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Trash button
        FilledIconButton(
            onClick = onTrash,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = TrashRed.copy(alpha = 0.15f),
                contentColor = TrashRed
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Maybe button
        FilledIconButton(
            onClick = onMaybe,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaybeAmber.copy(alpha = 0.15f),
                contentColor = MaybeAmber
            )
        ) {
            Icon(
                imageVector = Icons.Default.QuestionMark,
                contentDescription = "待定",
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Keep button
        FilledIconButton(
            onClick = onKeep,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = KeepGreen.copy(alpha = 0.15f),
                contentColor = KeepGreen
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "保留",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Loading state content.
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在加载照片...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Completion state content.
 */
@Composable
private fun CompletionContent(
    keepCount: Int,
    trashCount: Int,
    maybeCount: Int,
    onNavigateToLightTable: () -> Unit,
    onGoBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(KeepGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = KeepGreen,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "整理完成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "所有照片已分类完毕",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Statistics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(count = keepCount, label = "保留", color = KeepGreen)
            StatItem(count = trashCount, label = "删除", color = TrashRed)
            StatItem(count = maybeCount, label = "待定", color = MaybeAmber)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Actions
        if (maybeCount > 0) {
            Button(
                onClick = onNavigateToLightTable,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaybeAmber
                )
            ) {
                Text(
                    text = "查看待定照片 ($maybeCount)",
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Button(
            onClick = onGoBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("返回首页")
        }
    }
}

/**
 * Statistics item.
 */
@Composable
private fun StatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
