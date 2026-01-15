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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.BottomAppBar
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
import com.example.photozen.data.local.entity.PhotoEntity
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
    
    // Shared transform state for synchronized zoom
    val transformState = rememberTransformState()
    
    // Fullscreen preview state
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenStartIndex by remember { mutableIntStateOf(0) }
    
    // Handle back press in comparison mode or fullscreen
    BackHandler(enabled = showFullscreen || uiState.mode == LightTableMode.COMPARISON) {
        if (showFullscreen) {
            showFullscreen = false
        } else {
            viewModel.exitComparison()
            transformState.reset()
        }
    }
    
    // Show errors
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
                                text = when (uiState.mode) {
                                    LightTableMode.SELECTION -> "Light Table"
                                    LightTableMode.COMPARISON -> "照片对比"
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = when (uiState.mode) {
                                    LightTableMode.SELECTION -> "${uiState.allMaybePhotos.size} 张待定照片"
                                    LightTableMode.COMPARISON -> {
                                        val selectedCount = uiState.selectedInComparison.size
                                        if (selectedCount > 0) "已选中 $selectedCount 张" else "点击照片选中"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (uiState.mode == LightTableMode.COMPARISON) {
                                    viewModel.exitComparison()
                                    transformState.reset()
                                } else {
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        if (uiState.mode == LightTableMode.SELECTION) {
                            // Select all button
                            IconButton(
                                onClick = { viewModel.selectAll() },
                                enabled = uiState.allMaybePhotos.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "全选"
                                )
                            }
                        } else {
                            // Reset zoom button
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    transformState.reset()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "重置缩放"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                when (uiState.mode) {
                    LightTableMode.SELECTION -> SelectionBottomBar(
                        selectionCount = uiState.selectionCount,
                        maxSelection = LightTableUiState.MAX_COMPARISON_PHOTOS,
                        canCompare = uiState.canCompare,
                        onClearSelection = { viewModel.clearSelection() },
                        onStartComparison = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startComparison()
                        }
                    )
                    LightTableMode.COMPARISON -> ComparisonBottomBar(
                        onKeepAll = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.keepAllSelected()
                        },
                        onTrashAll = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.trashAllSelected()
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
                    uiState.isLoading -> {
                        LoadingContent()
                    }
                    uiState.allMaybePhotos.isEmpty() -> {
                        EmptyContent(onNavigateBack = onNavigateBack)
                    }
                    else -> {
                        AnimatedContent(
                            targetState = uiState.mode,
                            transitionSpec = {
                                fadeIn() + scaleIn(initialScale = 0.95f) togetherWith
                                    fadeOut() + scaleOut(targetScale = 0.95f)
                            },
                            label = "mode_transition"
                        ) { mode ->
                            when (mode) {
                                LightTableMode.SELECTION -> {
                                    PhotoThumbnailGrid(
                                        photos = uiState.allMaybePhotos,
                                        selectedIds = uiState.selectedForComparison,
                                        onToggleSelection = { viewModel.toggleSelection(it) }
                                    )
                                }
                                LightTableMode.COMPARISON -> {
                                    ComparisonGrid(
                                        photos = uiState.comparisonPhotos,
                                        transformState = transformState,
                                        selectedPhotoIds = uiState.selectedInComparison,
                                        onSelectPhoto = { viewModel.toggleComparisonSelection(it) },
                                        onFullscreenClick = { index ->
                                            fullscreenStartIndex = index
                                            showFullscreen = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Floating action button for "Keep Selected" (only in comparison mode with selections)
        AnimatedVisibility(
            visible = uiState.mode == LightTableMode.COMPARISON && uiState.hasSelectedInComparison,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.keepSelectedTrashRest()
                },
                containerColor = KeepGreen,
                contentColor = Color.White,
                icon = {
                    Icon(Icons.Default.Star, contentDescription = null)
                },
                text = {
                    Text("保留选中 (${uiState.selectedInComparison.size})")
                }
            )
        }
        
        // Fullscreen preview overlay
        if (showFullscreen && uiState.comparisonPhotos.isNotEmpty()) {
            FullscreenComparisonViewer(
                photos = uiState.comparisonPhotos,
                initialIndex = fullscreenStartIndex,
                onDismiss = { showFullscreen = false }
            )
        }
    }
}

/**
 * Bottom bar for selection mode.
 */
@Composable
private fun SelectionBottomBar(
    selectionCount: Int,
    maxSelection: Int,
    canCompare: Boolean,
    onClearSelection: () -> Unit,
    onStartComparison: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaybeAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectionCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaybeAmber
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "已选择 (2-${maxSelection}张)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedVisibility(
                    visible = selectionCount > 0,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    OutlinedButton(onClick = onClearSelection) {
                        Text("清除")
                    }
                }
                
                Button(
                    onClick = onStartComparison,
                    enabled = canCompare,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaybeAmber
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("对比", color = Color.Black)
                }
            }
        }
    }
}

/**
 * Bottom bar for comparison mode - only "Trash All" and "Keep All"
 */
@Composable
private fun ComparisonBottomBar(
    onKeepAll: () -> Unit,
    onTrashAll: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trash all
            FilledTonalButton(
                onClick = onTrashAll,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = TrashRed.copy(alpha = 0.15f),
                    contentColor = TrashRed
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, null, Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("丢弃全部")
            }
            
            // Keep all
            FilledTonalButton(
                onClick = onKeepAll,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = KeepGreen.copy(alpha = 0.15f),
                    contentColor = KeepGreen
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("保留全部")
            }
        }
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(photo.systemUri))
                .crossfade(true)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaybeAmber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = MaybeAmber,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "没有待定照片",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "在 Flow Sorter 中向上滑动可将照片\n标记为「待定」以便稍后比较",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onNavigateBack) {
            Text("返回")
        }
    }
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
    
    // Set session filter when provided
    LaunchedEffect(sessionPhotoIds) {
        viewModel.setSessionPhotoIds(sessionPhotoIds)
    }
    
    // Fullscreen state
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenStartIndex by remember { mutableIntStateOf(0) }
    
    // Handle back press in comparison mode
    BackHandler(enabled = showFullscreen || uiState.mode == LightTableMode.COMPARISON) {
        if (showFullscreen) {
            showFullscreen = false
        } else {
            viewModel.exitComparison()
            transformState.reset()
        }
    }
    
    // Auto-complete when no more "Maybe" photos in workflow mode
    LaunchedEffect(uiState.allMaybePhotos.isEmpty(), uiState.isLoading) {
        if (isWorkflowMode && !uiState.isLoading && uiState.allMaybePhotos.isEmpty()) {
            onComplete?.invoke()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        LoadingContent()
                    }
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
                        AnimatedContent(
                            targetState = uiState.mode,
                            transitionSpec = {
                                fadeIn() + scaleIn(initialScale = 0.95f) togetherWith
                                    fadeOut() + scaleOut(targetScale = 0.95f)
                            },
                            label = "mode_transition"
                        ) { mode ->
                            when (mode) {
                                LightTableMode.SELECTION -> {
                                    PhotoThumbnailGrid(
                                        photos = uiState.allMaybePhotos,
                                        selectedIds = uiState.selectedForComparison,
                                        onToggleSelection = { viewModel.toggleSelection(it) }
                                    )
                                }
                                LightTableMode.COMPARISON -> {
                                    ComparisonGrid(
                                        photos = uiState.comparisonPhotos,
                                        transformState = transformState,
                                        selectedPhotoIds = uiState.selectedInComparison,
                                        onSelectPhoto = { viewModel.toggleComparisonSelection(it) },
                                        onFullscreenClick = { index ->
                                            fullscreenStartIndex = index
                                            showFullscreen = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom bar (only when there are photos)
            if (!uiState.isLoading && uiState.allMaybePhotos.isNotEmpty()) {
                when (uiState.mode) {
                    LightTableMode.SELECTION -> SelectionBottomBar(
                        selectionCount = uiState.selectionCount,
                        maxSelection = LightTableUiState.MAX_COMPARISON_PHOTOS,
                        canCompare = uiState.canCompare,
                        onClearSelection = { viewModel.clearSelection() },
                        onStartComparison = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startComparison()
                        }
                    )
                    LightTableMode.COMPARISON -> ComparisonBottomBar(
                        onKeepAll = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.keepAllSelected()
                        },
                        onTrashAll = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.trashAllSelected()
                        }
                    )
                }
            }
        }
        
        // Floating action button for "Keep Selected"
        AnimatedVisibility(
            visible = uiState.mode == LightTableMode.COMPARISON && uiState.hasSelectedInComparison,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.keepSelectedTrashRest()
                },
                containerColor = KeepGreen,
                contentColor = Color.White,
                icon = {
                    Icon(Icons.Default.Star, contentDescription = null)
                },
                text = {
                    Text("保留选中 (${uiState.selectedInComparison.size})")
                }
            )
        }
        
        // Fullscreen preview
        if (showFullscreen && uiState.comparisonPhotos.isNotEmpty()) {
            FullscreenComparisonViewer(
                photos = uiState.comparisonPhotos,
                initialIndex = fullscreenStartIndex,
                onDismiss = { showFullscreen = false }
            )
        }
    }
}
