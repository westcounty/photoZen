package com.example.photozen.ui.screens.lighttable

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.components.DragSelectPhotoGrid
import com.example.photozen.ui.components.PhotoGridMode
import com.example.photozen.ui.components.SortDropdownButton
import com.example.photozen.ui.components.SortOption
import com.example.photozen.ui.components.ViewModeDropdownButton
import com.example.photozen.ui.components.fullscreen.UnifiedFullscreenViewer
import com.example.photozen.ui.components.fullscreen.FullscreenActionType
import com.example.photozen.ui.components.shareImage
import com.example.photozen.ui.components.openImageWithChooser
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.launch

/**
 * Light Table Screen - Photo comparison with synchronized zoom.
 * 
 * Two modes:
 * 1. SELECTION: Grid of "Maybe" photos, select 2-6 for comparison
 * 2. COMPARISON: Synchronized zoom/pan view, multi-select photos to keep
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightTableScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LightTableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Shared transform state for synchronized zoom
    val transformState = rememberTransformState()

    // Individual transform states for each photo (max 6 photos in comparison)
    // These are managed here so reset button can reset all of them
    val individualTransformStates = remember {
        List(6) { TransformState() }
    }

    // Helper function to reset all zoom states
    fun resetAllZoomStates() {
        transformState.reset()
        individualTransformStates.forEach { it.reset() }
    }

    // Fullscreen preview state
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenStartIndex by remember { mutableIntStateOf(0) }

    // Sync zoom toggle state (default: enabled for synchronized zoom)
    var syncZoomEnabled by remember { mutableStateOf(true) }

    // Handle back press in comparison mode or fullscreen
    BackHandler(enabled = showFullscreen || uiState.mode == LightTableMode.COMPARISON) {
        if (showFullscreen) {
            showFullscreen = false
        } else {
            viewModel.exitComparison()
            resetAllZoomStates()
        }
    }
    
    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Show messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Different layouts for SELECTION vs COMPARISON mode
        when (uiState.mode) {
            LightTableMode.SELECTION -> {
                // Sort options for dropdown
                val sortOptions = remember {
                    LightTableSortOrder.entries.map { order ->
                        SortOption(
                            id = order.name,
                            displayName = order.displayName
                        )
                    }
                }
                val currentSortOption = remember(uiState.sortOrder) {
                    SortOption(
                        id = uiState.sortOrder.name,
                        displayName = uiState.sortOrder.displayName
                    )
                }

                // Selection mode: Use Scaffold with topBar and bottomBar
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "待定照片",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = if (uiState.selectionCount > 0) {
                                            "已选择 ${uiState.selectionCount} 张（共 ${uiState.allMaybePhotos.size} 张）"
                                        } else {
                                            "${uiState.allMaybePhotos.size}张待对比"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (uiState.selectionCount > 0) MaybeAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                // 排序按钮
                                SortDropdownButton(
                                    options = sortOptions,
                                    currentSort = currentSortOption,
                                    onSortSelected = { option ->
                                        val order = LightTableSortOrder.valueOf(option.id)
                                        viewModel.setSortOrder(order)
                                    }
                                )
                                // 视图模式按钮
                                ViewModeDropdownButton(
                                    currentMode = uiState.gridMode,
                                    currentColumns = uiState.gridColumns,
                                    onModeChanged = { mode ->
                                        viewModel.setGridMode(mode)
                                    },
                                    onColumnsChanged = { cols ->
                                        viewModel.setGridColumns(cols)
                                    }
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        AnimatedVisibility(
                            visible = uiState.selectionCount > 0,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            SelectionBottomBar(
                                selectionCount = uiState.selectionCount,
                                canCompare = uiState.canCompare,
                                onKeep = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.keepAllSelected()
                                },
                                onTrash = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.trashAllSelected()
                                },
                                onClearSelection = { viewModel.clearSelection() },
                                onStartComparison = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.startComparison()
                                },
                                onCompareDisabledClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("请选择2-6张照片进行对比")
                                    }
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when {
                            uiState.isLoading -> LoadingContent()
                            uiState.allMaybePhotos.isEmpty() -> EmptyContent(onNavigateBack = onNavigateBack)
                            else -> {
                                DragSelectPhotoGrid(
                                    photos = uiState.allMaybePhotos,
                                    selectedIds = uiState.selectedForComparison,
                                    onSelectionChanged = { newSelection ->
                                        // 限制最多选择 6 张
                                        if (newSelection.size <= LightTableUiState.MAX_COMPARISON_PHOTOS) {
                                            newSelection.forEach { photoId ->
                                                if (photoId !in uiState.selectedForComparison) {
                                                    viewModel.toggleSelection(photoId)
                                                }
                                            }
                                            uiState.selectedForComparison.forEach { photoId ->
                                                if (photoId !in newSelection) {
                                                    viewModel.toggleSelection(photoId)
                                                }
                                            }
                                        } else {
                                            // 超过限制，显示提示
                                            scope.launch {
                                                snackbarHostState.showSnackbar("最多可对比${LightTableUiState.MAX_COMPARISON_PHOTOS}张照片")
                                            }
                                        }
                                    },
                                    onPhotoClick = { _, _ ->
                                        // 对比台：点击由 onSelectionToggle 处理
                                    },
                                    clickAlwaysTogglesSelection = true,
                                    onPhotoLongPress = { _, _ ->
                                        // 长按选择已在 DragSelectPhotoGrid.onLongPress 中处理
                                        // 此回调仅用于额外操作（如显示操作菜单），目前无需额外处理
                                    },
                                    columns = uiState.gridColumns,
                                    gridMode = uiState.gridMode,
                                    selectionColor = MaybeAmber,
                                    onSelectionToggle = { photoId ->
                                        // Toggle selection using ViewModel to ensure fresh state
                                        viewModel.toggleSelection(photoId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            LightTableMode.COMPARISON -> {
                // Comparison mode: Immersive full-screen with floating buttons
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Full-screen comparison grid
                    ComparisonGrid(
                        photos = uiState.comparisonPhotos,
                        transformState = transformState,
                        syncZoomEnabled = syncZoomEnabled,
                        individualTransformStates = individualTransformStates.take(uiState.comparisonPhotos.size),
                        selectedPhotoIds = uiState.selectedInComparison,
                        onSelectPhoto = { viewModel.toggleComparisonSelection(it) },
                        onFullscreenClick = { index ->
                            fullscreenStartIndex = index
                            showFullscreen = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Top-left: Back button (floating)
                    SmallFloatingActionButton(
                        onClick = {
                            viewModel.exitComparison()
                            resetAllZoomStates()
                        },
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                    
                    // Top-right: Sync zoom toggle and Reset zoom buttons (floating)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 16.dp, top = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sync zoom toggle button
                        SmallFloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                syncZoomEnabled = !syncZoomEnabled
                            },
                            containerColor = if (syncZoomEnabled) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            contentColor = if (syncZoomEnabled)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(
                                imageVector = if (syncZoomEnabled) Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = if (syncZoomEnabled) "联动缩放已开启" else "联动缩放已关闭"
                            )
                        }
                        
                        // Reset zoom button
                        SmallFloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                resetAllZoomStates()
                            },
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "重置缩放"
                            )
                        }
                    }
                    
                    // Bottom compact action buttons (pill-shaped)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp, start = 32.dp, end = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Trash all button (compact pill)
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.trashAllSelected()
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = TrashRed.copy(alpha = 0.95f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("丢弃", fontWeight = FontWeight.Medium, color = Color.White)
                            }
                        }
                        
                        // Keep all button (compact pill)
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.keepAllSelected()
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = KeepGreen.copy(alpha = 0.95f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("保留", fontWeight = FontWeight.Medium, color = Color.White)
                            }
                        }
                    }
                    
                    // "Keep Selected" compact button (when photos are selected)
                    AnimatedVisibility(
                        visible = uiState.hasSelectedInComparison,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 76.dp)
                    ) {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.keepSelectedTrashRest()
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = KeepGreen,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, null, Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("保留选中 (${uiState.selectedInComparison.size})", fontWeight = FontWeight.Medium, color = Color.White)
                            }
                        }
                    }
                    
                    // Snackbar host
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 160.dp)
                    )
                }
            }
        }
        
        // Fullscreen preview overlay (works in both modes)
        if (showFullscreen) {
            val photosToShow = if (uiState.mode == LightTableMode.COMPARISON) {
                uiState.comparisonPhotos
            } else {
                uiState.allMaybePhotos
            }
            if (photosToShow.isNotEmpty()) {
                UnifiedFullscreenViewer(
                    photos = photosToShow,
                    initialIndex = fullscreenStartIndex.coerceIn(0, photosToShow.lastIndex),
                    onExit = { showFullscreen = false },
                    onAction = { action, photo ->
                        // 处理全屏预览中的操作
                        when (action) {
                            FullscreenActionType.COPY -> {
                                viewModel.copyPhoto(photo.id)
                            }
                            FullscreenActionType.OPEN_WITH -> {
                                openImageWithChooser(context, android.net.Uri.parse(photo.systemUri))
                            }
                            FullscreenActionType.EDIT -> { /* 编辑功能 */ }
                            FullscreenActionType.SHARE -> {
                                shareImage(context, android.net.Uri.parse(photo.systemUri))
                            }
                            FullscreenActionType.DELETE -> {
                                // 待定列表不支持直接删除
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Bottom bar for selection mode.
 */
@Composable
private fun SelectionBottomBar(
    selectionCount: Int,
    canCompare: Boolean,
    onKeep: () -> Unit,
    onTrash: () -> Unit,
    onClearSelection: () -> Unit,
    onStartComparison: () -> Unit,
    onCompareDisabledClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 保留按钮
            LightTableBottomBarItem(
                icon = Icons.Default.Favorite,
                label = "保留",
                color = KeepGreen,
                onClick = onKeep
            )

            // 回收站按钮
            LightTableBottomBarItem(
                icon = Icons.Outlined.Delete,
                label = "回收站",
                color = MaterialTheme.colorScheme.onSurface,
                onClick = onTrash
            )

            // 清除选择按钮
            LightTableBottomBarItem(
                icon = Icons.Default.Clear,
                label = "清除选择",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onClearSelection
            )

            // 对比按钮（禁用时也可点击，显示提示）
            LightTableBottomBarItem(
                icon = Icons.Default.Compare,
                label = "对比",
                color = if (canCompare) MaybeAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                onClick = if (canCompare) onStartComparison else onCompareDisabledClick
            )
        }
    }
}

@Composable
private fun LightTableBottomBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val actualColor = if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (enabled) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = actualColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = actualColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Fullscreen comparison viewer with circular paging.
 * 
 * 交互设计参考微信/Google Photos：
 * - 缩放=1时：左右滑动切换图片
 * - 缩放>1时：拖动平移图片，到达边界后可继续滑动切换
 * - 双击：缩放/还原
 * - 双指捏合：缩放
 */
@Composable
private fun FullscreenComparisonViewer(
    photos: List<PhotoEntity>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    // Use large page count for "infinite" circular scrolling
    val virtualPageCount = photos.size * 1000
    val initialPage = (virtualPageCount / 2) - ((virtualPageCount / 2) % photos.size) + initialIndex
    
    val pagerState = rememberPagerState(initialPage = initialPage) { virtualPageCount }
    val scope = rememberCoroutineScope()
    
    // Current real index
    val currentRealIndex = pagerState.currentPage % photos.size
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Pager - 始终启用滑动，通过子组件控制手势消费
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val realIndex = page % photos.size
            val photo = photos[realIndex]
            val isCurrentPage = page == pagerState.currentPage
            
            ZoomableImage(
                photo = photo,
                isCurrentPage = isCurrentPage,
                onRequestNextPage = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                onRequestPrevPage = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            )
        }
        
        // Close button (always on top)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }
        
        // Page indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${currentRealIndex + 1} / ${photos.size}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 可缩放图片组件 - 简化版
 * 
 * 核心逻辑：
 * 1. scale = 1: 让 Pager 处理滑动
 * 2. scale > 1: 处理平移，边界时触发页面切换
 * 3. 双击：在 1x 和 2.5x 之间切换
 * 4. 双指：实时缩放（无动画延迟）
 */
@Composable
private fun ZoomableImage(
    photo: PhotoEntity,
    isCurrentPage: Boolean,
    onRequestNextPage: () -> Unit,
    onRequestPrevPage: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // 边界滑动累计值（用于触发页面切换）
    var boundarySwipeDistance by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 100f // 边界滑动触发阈值
    
    // 当页面切换时重置缩放状态
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            boundarySwipeDistance = 0f
        }
    }
    
    // 计算平移边界
    fun calculateBounds(): Pair<Float, Float> {
        if (containerSize.width == 0 || containerSize.height == 0) return 0f to 0f
        
        val scaledWidth = containerSize.width * scale
        val scaledHeight = containerSize.height * scale
        
        val maxOffsetX = ((scaledWidth - containerSize.width) / 2f).coerceAtLeast(0f)
        val maxOffsetY = ((scaledHeight - containerSize.height) / 2f).coerceAtLeast(0f)
        
        return maxOffsetX to maxOffsetY
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.5f) {
                            // 还原
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            // 放大到点击位置
                            val newScale = 2.5f
                            val centerX = containerSize.width / 2f
                            val centerY = containerSize.height / 2f
                            
                            // 计算新的边界
                            val scaledWidth = containerSize.width * newScale
                            val scaledHeight = containerSize.height * newScale
                            val maxX = ((scaledWidth - containerSize.width) / 2f).coerceAtLeast(0f)
                            val maxY = ((scaledHeight - containerSize.height) / 2f).coerceAtLeast(0f)
                            
                            // 以点击位置为中心放大
                            val newOffsetX = ((centerX - tapOffset.x) * (newScale - 1)).coerceIn(-maxX, maxX)
                            val newOffsetY = ((centerY - tapOffset.y) * (newScale - 1)).coerceIn(-maxY, maxY)
                            
                            scale = newScale
                            offsetX = newOffsetX
                            offsetY = newOffsetY
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    boundarySwipeDistance = 0f
                    
                    do {
                        val event = awaitPointerEvent()
                        
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val pointerCount = event.changes.size
                        
                        // 双指缩放 - 直接响应，无动画延迟
                        if (pointerCount >= 2 && zoomChange != 1f) {
                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                            
                            // 缩放时调整偏移以保持中心点
                            if (scale != newScale) {
                                val scaleRatio = newScale / scale
                                offsetX *= scaleRatio
                                offsetY *= scaleRatio
                                scale = newScale
                                
                                // 限制偏移在边界内
                                val (maxX, maxY) = calculateBounds()
                                offsetX = offsetX.coerceIn(-maxX, maxX)
                                offsetY = offsetY.coerceIn(-maxY, maxY)
                            }
                            
                            event.changes.forEach { it.consume() }
                        }
                        // 单指平移（仅在缩放状态）
                        else if (pointerCount == 1 && scale > 1.05f) {
                            val (maxX, maxY) = calculateBounds()
                            
                            val newOffsetX = offsetX + panChange.x
                            val newOffsetY = offsetY + panChange.y
                            
                            // 检查是否到达边界
                            val atLeftEdge = offsetX >= maxX - 1f
                            val atRightEdge = offsetX <= -maxX + 1f
                            
                            // 边界穿透逻辑
                            when {
                                atRightEdge && panChange.x < -5f -> {
                                    // 到达右边界继续向左滑 -> 切换到下一张
                                    boundarySwipeDistance += abs(panChange.x)
                                    if (boundarySwipeDistance > swipeThreshold) {
                                        onRequestNextPage()
                                        boundarySwipeDistance = 0f
                                    }
                                }
                                atLeftEdge && panChange.x > 5f -> {
                                    // 到达左边界继续向右滑 -> 切换到上一张
                                    boundarySwipeDistance += abs(panChange.x)
                                    if (boundarySwipeDistance > swipeThreshold) {
                                        onRequestPrevPage()
                                        boundarySwipeDistance = 0f
                                    }
                                }
                                else -> {
                                    // 正常平移
                                    boundarySwipeDistance = 0f
                                    offsetX = newOffsetX.coerceIn(-maxX, maxX)
                                    offsetY = newOffsetY.coerceIn(-maxY, maxY)
                                }
                            }
                            
                            event.changes.forEach { it.consume() }
                        }
                        // 未缩放时不消费事件，让 Pager 处理
                        
                    } while (event.changes.any { it.pressed })
                    
                    // 释放时如果缩放太小则还原
                    if (scale < 1.1f && scale != 1f) {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Use ORIGINAL size for full resolution when zooming
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(photo.systemUri))
                .crossfade(true)
                .size(Size.ORIGINAL)  // Load full resolution for zoom clarity
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
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
                text = "正在加载...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Empty state when no "Maybe" photos.
 */
@Composable
private fun EmptyContent(
    onNavigateBack: () -> Unit
) {
    com.example.photozen.ui.components.EmptyStates.EmptyCompare(
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Light Table Content - Reusable content for both standalone and workflow modes.
 * 
 * @param sessionPhotoIds When set, only photos with these IDs will be shown (for workflow mode)
 */
@Composable
fun LightTableContent(
    isWorkflowMode: Boolean = false,
    sessionPhotoIds: Set<String>? = null,
    onComplete: (() -> Unit)? = null,
    onNavigateBack: () -> Unit,
    viewModel: LightTableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val transformState = rememberTransformState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Individual transform states for each photo (max 6 photos in comparison)
    // These are managed here so reset button can reset all of them
    val individualTransformStates = remember {
        List(6) { TransformState() }
    }

    // Helper function to reset all zoom states
    fun resetAllZoomStates() {
        transformState.reset()
        individualTransformStates.forEach { it.reset() }
    }

    // Set session filter when provided
    LaunchedEffect(sessionPhotoIds) {
        viewModel.setSessionPhotoIds(sessionPhotoIds)
    }

    // Fullscreen state
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenStartIndex by remember { mutableIntStateOf(0) }

    // Sync zoom toggle state (default: enabled for synchronized zoom)
    var syncZoomEnabled by remember { mutableStateOf(true) }

    // Handle back press in comparison mode
    BackHandler(enabled = showFullscreen || uiState.mode == LightTableMode.COMPARISON) {
        if (showFullscreen) {
            showFullscreen = false
        } else {
            viewModel.exitComparison()
            resetAllZoomStates()
        }
    }

    // Auto-complete when no more "Maybe" photos in workflow mode
    LaunchedEffect(uiState.allMaybePhotos.isEmpty(), uiState.isLoading) {
        if (isWorkflowMode && !uiState.isLoading && uiState.allMaybePhotos.isEmpty()) {
            onComplete?.invoke()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Different layouts for SELECTION vs COMPARISON mode
        when (uiState.mode) {
            LightTableMode.SELECTION -> {
                // Selection mode: Use Column with bottom bar
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            uiState.isLoading -> LoadingContent()
                            uiState.allMaybePhotos.isEmpty() -> {
                                if (isWorkflowMode) {
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
                                                text = "对比完成",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    EmptyContent(onNavigateBack = onNavigateBack)
                                }
                            }
                            else -> {
                                PhotoThumbnailGrid(
                                    photos = uiState.allMaybePhotos,
                                    selectedIds = uiState.selectedForComparison,
                                    onToggleSelection = { viewModel.toggleSelection(it) }
                                )
                            }
                        }
                    }
                    
                    // Bottom bar for selection mode
                    AnimatedVisibility(
                        visible = !uiState.isLoading && uiState.allMaybePhotos.isNotEmpty() && uiState.selectionCount > 0,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        SelectionBottomBar(
                            selectionCount = uiState.selectionCount,
                            canCompare = uiState.canCompare,
                            onKeep = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.keepAllSelected()
                            },
                            onTrash = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.trashAllSelected()
                            },
                            onClearSelection = { viewModel.clearSelection() },
                            onStartComparison = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startComparison()
                            },
                            onCompareDisabledClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("请选择2-6张照片进行对比")
                                }
                            }
                        )
                    }
                }
            }
            
            LightTableMode.COMPARISON -> {
                // Comparison mode: Immersive full-screen with floating buttons
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Full-screen comparison grid
                    ComparisonGrid(
                        photos = uiState.comparisonPhotos,
                        transformState = transformState,
                        syncZoomEnabled = syncZoomEnabled,
                        individualTransformStates = individualTransformStates.take(uiState.comparisonPhotos.size),
                        selectedPhotoIds = uiState.selectedInComparison,
                        onSelectPhoto = { viewModel.toggleComparisonSelection(it) },
                        onFullscreenClick = { index ->
                            fullscreenStartIndex = index
                            showFullscreen = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Top-left: Back button (floating)
                    SmallFloatingActionButton(
                        onClick = {
                            viewModel.exitComparison()
                            resetAllZoomStates()
                        },
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                    
                    // Top-right: Sync zoom toggle and Reset zoom buttons (floating)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 16.dp, top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sync zoom toggle button
                        SmallFloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                syncZoomEnabled = !syncZoomEnabled
                            },
                            containerColor = if (syncZoomEnabled) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            contentColor = if (syncZoomEnabled)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(
                                imageVector = if (syncZoomEnabled) Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = if (syncZoomEnabled) "联动缩放已开启" else "联动缩放已关闭"
                            )
                        }
                        
                        // Reset zoom button
                        SmallFloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                resetAllZoomStates()
                            },
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "重置缩放"
                            )
                        }
                    }
                    
                    // Bottom compact action buttons (pill-shaped)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp, start = 32.dp, end = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Trash all button (compact pill)
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.trashAllSelected()
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = TrashRed.copy(alpha = 0.95f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("丢弃", fontWeight = FontWeight.Medium, color = Color.White)
                            }
                        }
                        
                        // Keep all button (compact pill)
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.keepAllSelected()
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = KeepGreen.copy(alpha = 0.95f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("保留", fontWeight = FontWeight.Medium, color = Color.White)
                            }
                        }
                    }
                    
                    // "Keep Selected" compact button (when photos are selected)
                    AnimatedVisibility(
                        visible = uiState.hasSelectedInComparison,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 76.dp)
                    ) {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.keepSelectedTrashRest()
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = KeepGreen,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, null, Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("保留选中 (${uiState.selectedInComparison.size})", fontWeight = FontWeight.Medium, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        
        // Fullscreen preview (works in both modes)
        if (showFullscreen && uiState.comparisonPhotos.isNotEmpty()) {
            FullscreenComparisonViewer(
                photos = uiState.comparisonPhotos,
                initialIndex = fullscreenStartIndex,
                onDismiss = { showFullscreen = false }
            )
        }
    }
}
