package com.example.photozen.ui.screens.lighttable

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
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
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
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
    
    // Current real index
    val currentRealIndex = pagerState.currentPage % photos.size
    
    // Track if any image is zoomed (to control pager scrolling)
    var isZoomed by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Pager - disable user scroll when zoomed
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed, // 关键：缩放时禁用 Pager 滑动
            beyondViewportPageCount = 1
        ) { page ->
            val realIndex = page % photos.size
            val photo = photos[realIndex]
            val isCurrentPage = page == pagerState.currentPage
            
            ZoomableImage(
                photo = photo,
                isCurrentPage = isCurrentPage,
                onZoomChanged = { zoomed -> 
                    if (isCurrentPage) {
                        isZoomed = zoomed 
                    }
                },
                pagerState = pagerState
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
 * 可缩放图片组件
 * 
 * 核心逻辑：
 * 1. scale = 1: 不拦截任何手势，让 Pager 处理滑动
 * 2. scale > 1: 处理平移，并在边界时允许切换页面
 * 3. 双击：在 1x 和 2.5x 之间切换
 * 4. 双指：缩放
 */
@Composable
private fun ZoomableImage(
    photo: PhotoEntity,
    isCurrentPage: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    pagerState: PagerState
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    val scope = rememberCoroutineScope()
    
    // 动画缩放值
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    // 当页面切换时重置缩放状态
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }
    
    // 通知父组件缩放状态变化
    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.01f)
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
                // 双击检测
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.5f) {
                            // 还原
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            // 放大到点击位置
                            scale = 2.5f
                            // 以点击位置为中心放大
                            val centerX = containerSize.width / 2f
                            val centerY = containerSize.height / 2f
                            offsetX = (centerX - tapOffset.x) * 1.5f
                            offsetY = (centerY - tapOffset.y) * 1.5f
                            
                            // 限制在边界内
                            val (maxX, maxY) = calculateBounds()
                            offsetX = offsetX.coerceIn(-maxX, maxX)
                            offsetY = offsetY.coerceIn(-maxY, maxY)
                        }
                    }
                )
            }
            .pointerInput(scale) {
                // 自定义手势处理：只在缩放时才处理平移
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    var lockedToPanZoom = false
                    
                    awaitFirstDown(requireUnconsumed = false)
                    
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val canceled = event.changes.any { it.isConsumed }
                        
                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            
                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange
                                
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = abs(1 - zoom) * centroidSize
                                val panMotion = pan.getDistance()
                                
                                if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                    pastTouchSlop = true
                                    // 双指缩放优先
                                    lockedToPanZoom = event.changes.size >= 2
                                }
                            }
                            
                            if (pastTouchSlop) {
                                val centroid = event.calculateCentroid(useCurrent = false)
                                
                                // 处理缩放
                                if (zoomChange != 1f) {
                                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                    scale = newScale
                                    lockedToPanZoom = true
                                }
                                
                                // 处理平移（只在缩放状态或双指操作时）
                                if (scale > 1.01f || lockedToPanZoom) {
                                    val (maxX, maxY) = calculateBounds()
                                    
                                    // 检查是否到达边界
                                    val atLeftEdge = offsetX >= maxX - 1f
                                    val atRightEdge = offsetX <= -maxX + 1f
                                    
                                    // 如果到达边界且继续同方向滑动，不消费事件让 Pager 处理
                                    val shouldConsume = when {
                                        scale <= 1.01f -> false // 未缩放时不消费
                                        event.changes.size >= 2 -> true // 双指始终消费
                                        atLeftEdge && panChange.x > 0 -> false // 到达左边界向右滑
                                        atRightEdge && panChange.x < 0 -> false // 到达右边界向左滑
                                        else -> true
                                    }
                                    
                                    if (shouldConsume) {
                                        offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
                                        offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
                                        
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    
                    // 释放时如果缩放太小则还原
                    if (scale < 1.1f) {
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
                .onSizeChanged { imageSize = it }
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
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
 */
@Composable
fun LightTableContent(
    isWorkflowMode: Boolean = false,
    onComplete: (() -> Unit)? = null,
    onNavigateBack: () -> Unit,
    viewModel: LightTableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val transformState = rememberTransformState()
    
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
