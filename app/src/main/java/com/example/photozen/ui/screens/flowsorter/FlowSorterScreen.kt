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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.components.ComboOverlay
import com.example.photozen.ui.components.FullscreenPhotoViewer
import com.example.photozen.ui.components.SelectableStaggeredPhotoGrid
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import com.example.photozen.ui.util.rememberHapticFeedbackManager

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
    val hapticManager = rememberHapticFeedbackManager()
    
    // Fullscreen viewer state
    var fullscreenPhoto by remember { mutableStateOf<PhotoEntity?>(null) }
    
    // Handle back press - exit selection mode first, then fullscreen, then navigate back
    BackHandler(enabled = fullscreenPhoto != null || uiState.isSelectionMode) {
        when {
            fullscreenPhoto != null -> fullscreenPhoto = null
            uiState.isSelectionMode -> viewModel.clearSelection()
        }
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
                        if (uiState.isSelectionMode) {
                            Text(
                                text = "已选择 ${uiState.selectedCount} 张",
                                style = MaterialTheme.typography.titleLarge
                            )
                        } else {
                            Column {
                                Text(
                                    text = if (uiState.viewMode == FlowSorterViewMode.CARD) "Flow Sorter" else "列表整理",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                if (uiState.isDailyTask) {
                                    Text(
                                        text = "${uiState.dailyTaskCurrent} / ${uiState.dailyTaskTarget} 今日目标",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (uiState.totalCount > 0) {
                                    Text(
                                        text = "${uiState.sortedCount} / ${uiState.totalCount} 已整理",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (uiState.isSelectionMode) {
                                viewModel.clearSelection()
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = if (uiState.isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (uiState.isSelectionMode) "取消选择" else "返回"
                            )
                        }
                    },
                    actions = {
                        if (uiState.isSelectionMode) {
                            // Select all button
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "全选"
                                )
                            }
                        } else {
                            // Sort order button - distinct icons for each mode
                            IconButton(onClick = { viewModel.cycleSortOrder() }) {
                                Icon(
                                    imageVector = when (uiState.sortOrder) {
                                        PhotoSortOrder.DATE_DESC -> Icons.Default.ArrowDownward
                                        PhotoSortOrder.DATE_ASC -> Icons.Default.ArrowUpward
                                        PhotoSortOrder.RANDOM -> Icons.Default.Shuffle
                                    },
                                    contentDescription = "排序: ${uiState.sortOrder.displayName}"
                                )
                            }
                            
                            // Grid columns toggle (only in list view)
                            if (uiState.viewMode == FlowSorterViewMode.LIST) {
                                IconButton(onClick = { viewModel.cycleGridColumns() }) {
                                    Icon(
                                        imageVector = when (uiState.gridColumns) {
                                            1 -> Icons.Default.ViewColumn
                                            2 -> Icons.Default.GridView
                                            else -> Icons.Default.ViewModule
                                        },
                                        contentDescription = "${uiState.gridColumns}列视图"
                                    )
                                }
                            }
                            
                            // View mode toggle
                            IconButton(onClick = { viewModel.toggleViewMode() }) {
                                Icon(
                                    imageVector = if (uiState.viewMode == FlowSorterViewMode.CARD) 
                                        Icons.Default.GridView else Icons.Default.ViewCarousel,
                                    contentDescription = if (uiState.viewMode == FlowSorterViewMode.CARD)
                                        "列表视图" else "卡片视图"
                                )
                            }
                            
                            // Undo button
                            AnimatedVisibility(
                                visible = uiState.lastAction != null && uiState.viewMode == FlowSorterViewMode.CARD,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                IconButton(
                                    onClick = {
                                        hapticManager.performClick()
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
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                // Batch action bar when in selection mode
                AnimatedVisibility(
                    visible = uiState.isSelectionMode,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    BatchActionBar(
                        selectedCount = uiState.selectedCount,
                        onKeep = { viewModel.keepSelectedPhotos() },
                        onTrash = { viewModel.trashSelectedPhotos() },
                        onMaybe = { viewModel.maybeSelectedPhotos() }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main content
                FlowSorterContent(
                    isWorkflowMode = false,
                    onNavigateBack = onNavigateBack,
                    onNavigateToLightTable = onNavigateToLightTable,
                    viewModel = viewModel
                )
            }
        }
    }
}

/**
 * Flow Sorter Content - Reusable content for both standalone and workflow modes.
 *
 * @param isWorkflowMode When true, hides top bar and uses callback instead of navigation
 * @param onPhotoSorted Callback when a photo is sorted (with photoId, status and current combo)
 * @param onComplete Callback when all photos are sorted
 * @param onNavigateBack Callback for navigation back (standalone mode only)
 * @param onNavigateToLightTable Callback for navigation to Light Table (standalone mode only)
 */
@Composable
fun FlowSorterContent(
    isWorkflowMode: Boolean = false,
    onPhotoSorted: ((String, PhotoStatus, Int) -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    onNavigateBack: () -> Unit,
    onNavigateToLightTable: () -> Unit = {},
    viewModel: FlowSorterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticManager = rememberHapticFeedbackManager()
    
    var fullscreenPhoto by remember { mutableStateOf<PhotoEntity?>(null) }
    
    // Local view mode state for workflow mode (since we don't have TopAppBar)
    var localViewMode by remember { mutableStateOf(FlowSorterViewMode.CARD) }
    val effectiveViewMode = if (isWorkflowMode) localViewMode else uiState.viewMode
    
    // Handle back press in fullscreen or selection mode
    BackHandler(enabled = fullscreenPhoto != null || uiState.isSelectionMode) {
        when {
            fullscreenPhoto != null -> fullscreenPhoto = null
            uiState.isSelectionMode -> viewModel.clearSelection()
        }
    }
    
    // Notify workflow of completion
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete && isWorkflowMode) {
            onComplete?.invoke()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Progress bar
            if (uiState.totalCount > 0 || uiState.isDailyTask) {
                val progress = if (uiState.isDailyTask && uiState.dailyTaskTarget > 0) {
                    uiState.dailyTaskCurrent.toFloat() / uiState.dailyTaskTarget
                } else {
                    uiState.progress
                }
                
                LinearProgressIndicator(
                    progress = { progress },
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
                    uiState.isComplete || uiState.isDailyTaskComplete -> {
                        if (isWorkflowMode && !uiState.isDailyTaskComplete) {
                            // In workflow mode (normal), show minimal completion (will auto-advance)
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = KeepGreen,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "整理完成",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            CompletionContent(
                                keepCount = uiState.keepCount,
                                trashCount = uiState.trashCount,
                                maybeCount = uiState.maybeCount,
                                isDailyTask = uiState.isDailyTask,
                                dailyTarget = uiState.dailyTaskTarget,
                                onNavigateToLightTable = if (isWorkflowMode) { {} } else onNavigateToLightTable,
                                onGoBack = onNavigateBack
                            )
                        }
                    }
                    effectiveViewMode == FlowSorterViewMode.LIST -> {
                        // List view with staggered grid
                        SelectableStaggeredPhotoGrid(
                            photos = uiState.photos,
                            selectedIds = uiState.selectedPhotoIds,
                            onSelectionChanged = { viewModel.updateSelection(it) },
                            onPhotoClick = { photoId, index ->
                                val photo = uiState.photos.find { it.id == photoId }
                                if (photo != null) {
                                    fullscreenPhoto = photo
                                }
                            },
                            columns = uiState.gridColumns
                        )
                    }
                    else -> {
                        // Card stack with combo overlay
                        Box(modifier = Modifier.fillMaxSize()) {
                            CardStack(
                                uiState = uiState,
                                onSwipeLeft = {
                                    // Left swipe = Keep
                                    val photoId = uiState.currentPhoto?.id ?: ""
                                    val combo = viewModel.keepCurrentPhoto()
                                    hapticManager.performSwipeFeedback(combo, uiState.combo.level)
                                    onPhotoSorted?.invoke(photoId, PhotoStatus.KEEP, combo)
                                },
                                onSwipeRight = {
                                    // Right swipe = Keep
                                    val photoId = uiState.currentPhoto?.id ?: ""
                                    val combo = viewModel.keepCurrentPhoto()
                                    hapticManager.performSwipeFeedback(combo, uiState.combo.level)
                                    onPhotoSorted?.invoke(photoId, PhotoStatus.KEEP, combo)
                                },
                                onSwipeUp = {
                                    // Up swipe = Trash
                                    val photoId = uiState.currentPhoto?.id ?: ""
                                    val combo = viewModel.trashCurrentPhoto()
                                    hapticManager.performSwipeFeedback(combo, uiState.combo.level)
                                    onPhotoSorted?.invoke(photoId, PhotoStatus.TRASH, combo)
                                },
                                onSwipeDown = {
                                    // Down swipe = Maybe (sinking into pending pool)
                                    val photoId = uiState.currentPhoto?.id ?: ""
                                    val combo = viewModel.maybeCurrentPhoto()
                                    hapticManager.performSwipeFeedback(combo, uiState.combo.level)
                                    onPhotoSorted?.invoke(photoId, PhotoStatus.MAYBE, combo)
                                },
                                onPhotoClick = { photo ->
                                    fullscreenPhoto = photo
                                }
                            )
                            
                            // Combo overlay
                            ComboOverlay(
                                comboState = uiState.combo,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 32.dp)
                            )
                        }
                    }
                }
            }
            
            // Bottom bar for batch actions when in selection mode (list view)
            AnimatedVisibility(
                visible = uiState.isSelectionMode,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                BatchActionBar(
                    selectedCount = uiState.selectedCount,
                    onKeep = { viewModel.keepSelectedPhotos() },
                    onTrash = { viewModel.trashSelectedPhotos() },
                    onMaybe = { viewModel.maybeSelectedPhotos() }
                )
            }
        }
        
        // View mode toggle button - shown in top right corner for workflow mode
        if (isWorkflowMode && !uiState.isComplete && !uiState.isDailyTaskComplete && !uiState.isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                // View mode toggle with selection mode actions
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show selection mode controls when in selection mode
                    if (uiState.isSelectionMode) {
                        // Clear selection button
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "取消选择",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        // Selected count badge
                        Text(
                            text = "${uiState.selectedCount}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Select all button
                        IconButton(
                            onClick = { viewModel.selectAll() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "全选",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Sort order button - distinct icons for each mode
                        IconButton(
                            onClick = { viewModel.cycleSortOrder() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = when (uiState.sortOrder) {
                                    PhotoSortOrder.DATE_DESC -> Icons.Default.ArrowDownward
                                    PhotoSortOrder.DATE_ASC -> Icons.Default.ArrowUpward
                                    PhotoSortOrder.RANDOM -> Icons.Default.Shuffle
                                },
                                contentDescription = "排序: ${uiState.sortOrder.displayName}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Grid columns toggle (only in list view)
                        if (effectiveViewMode == FlowSorterViewMode.LIST) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.cycleGridColumns() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                            ) {
                                Icon(
                                    imageVector = when (uiState.gridColumns) {
                                        1 -> Icons.Default.ViewColumn
                                        2 -> Icons.Default.GridView
                                        else -> Icons.Default.ViewModule
                                    },
                                    contentDescription = "${uiState.gridColumns}列视图",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        // View mode toggle button
                        IconButton(
                            onClick = {
                                localViewMode = if (localViewMode == FlowSorterViewMode.CARD) {
                                    FlowSorterViewMode.LIST
                                } else {
                                    FlowSorterViewMode.CARD
                                }
                                // Clear selection when switching modes
                                viewModel.clearSelection()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = if (localViewMode == FlowSorterViewMode.CARD) 
                                    Icons.Default.GridView else Icons.Default.ViewCarousel,
                                contentDescription = if (localViewMode == FlowSorterViewMode.CARD)
                                    "列表视图" else "卡片视图",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Fullscreen viewer
        AnimatedContent(
            targetState = fullscreenPhoto,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.92f))
                    .togetherWith(fadeOut() + scaleOut(targetScale = 0.92f))
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
    onSwipeDown: () -> Unit,
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
                onSwipeDown = onSwipeDown,
                onPhotoClick = { onPhotoClick(currentPhoto) }
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
    isDailyTask: Boolean = false,
    dailyTarget: Int = 0,
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
            text = if (isDailyTask) "今日任务完成！" else "整理完成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isDailyTask) "已达成 ${dailyTarget} 张整理目标" else "所有照片已分类完毕",
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
        if (maybeCount > 0 && !isDailyTask) {
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

/**
 * Bottom action bar for batch operations.
 */
@Composable
private fun BatchActionBar(
    selectedCount: Int,
    onKeep: () -> Unit,
    onTrash: () -> Unit,
    onMaybe: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Keep button
        FilledTonalButton(
            onClick = onKeep,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = KeepGreen.copy(alpha = 0.15f),
                contentColor = KeepGreen
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("保留")
        }
        
        // Trash button
        FilledTonalButton(
            onClick = onTrash,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = TrashRed.copy(alpha = 0.15f),
                contentColor = TrashRed
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("删除")
        }
        
        // Maybe button
        FilledTonalButton(
            onClick = onMaybe,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaybeAmber.copy(alpha = 0.15f),
                contentColor = MaybeAmber
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QuestionMark,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("待定")
        }
    }
}
