package com.example.photozen.ui.components.fullscreen

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import kotlinx.coroutines.launch

// 边缘穿透冷却时间（毫秒）
private const val EDGE_SWIPE_COOLDOWN_MS = 500L

/**
 * PhotoZen 可缩放图片翻页器 (REQ-017, REQ-018, REQ-019, REQ-020)
 * ============================================================
 *
 * 功能特性:
 * - REQ-017: 双击缩放 (1x↔2.5x，以点击位置为中心)
 * - REQ-018: 双指缩放 (1-10倍)，缩放后可平移查看
 * - REQ-019: 1x时左右滑动切换前后张
 * - REQ-020: 下滑退出 (在1x时向下滑动)
 * - 边缘穿透: 缩放状态下滑到边缘继续滑动切换照片
 *
 * @param photos 照片列表
 * @param currentIndex 当前索引
 * @param onIndexChange 索引变更回调
 * @param onDismiss 下滑退出回调
 * @param modifier Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableImagePager(
    photos: List<PhotoEntity>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { photos.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // 同步外部索引变化到Pager
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    // 同步Pager页面变化到外部
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentIndex) {
            onIndexChange(pagerState.currentPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondViewportPageCount = 1,
        key = { photos.getOrNull(it)?.id ?: it.toString() }
    ) { page ->
        val photo = photos.getOrNull(page) ?: return@HorizontalPager

        ZoomableImage(
            photo = photo,
            isCurrentPage = page == pagerState.currentPage,
            onSwipeToNext = {
                if (page < photos.lastIndex) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page + 1)
                    }
                }
            },
            onSwipeToPrevious = {
                if (page > 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page - 1)
                    }
                }
            },
            onDismiss = onDismiss
        )
    }
}

/**
 * 单张可缩放图片
 *
 * @param photo 照片实体
 * @param isCurrentPage 是否为当前页
 * @param onSwipeToNext 滑到右边缘时触发下一张
 * @param onSwipeToPrevious 滑到左边缘时触发上一张
 * @param onDismiss 下滑退出回调
 */
@Composable
private fun ZoomableImage(
    photo: PhotoEntity,
    isCurrentPage: Boolean,
    onSwipeToNext: () -> Unit,
    onSwipeToPrevious: () -> Unit,
    onDismiss: (() -> Unit)?
) {
    val context = LocalContext.current

    // 缩放和偏移状态
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // 容器尺寸
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }

    // 下滑累积距离
    var dismissProgress by remember { mutableFloatStateOf(0f) }

    // 边缘穿透冷却：防止快速重复触发切换
    var lastEdgeSwipeTime by remember { mutableLongStateOf(0L) }

    // 切换页面时重置状态
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            dismissProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                containerWidth = size.width.toFloat()
                containerHeight = size.height.toFloat()
            }
            .pointerInput(isCurrentPage) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // 只在当前页处理手势
                    if (!isCurrentPage) return@detectTransformGestures

                    // 缩放: 限制在 1-10 倍 (REQ-018)
                    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    scale = newScale

                    if (newScale > 1f) {
                        // 缩放状态下的平移
                        val newOffsetX = offsetX + pan.x
                        val newOffsetY = offsetY + pan.y

                        // 计算最大偏移量 (基于缩放比例)
                        val maxOffsetX = (newScale - 1f) * containerWidth / 2f
                        val maxOffsetY = (newScale - 1f) * containerHeight / 2f

                        // 边界检测 - 滑到边缘后触发切换 (REQ-018)
                        // 添加冷却机制防止快速重复触发
                        val currentTime = System.currentTimeMillis()
                        val canSwipe = currentTime - lastEdgeSwipeTime > EDGE_SWIPE_COOLDOWN_MS

                        if (newOffsetX > maxOffsetX + EDGE_THRESHOLD && canSwipe) {
                            lastEdgeSwipeTime = currentTime
                            onSwipeToPrevious()
                            offsetX = maxOffsetX
                        } else if (newOffsetX < -maxOffsetX - EDGE_THRESHOLD && canSwipe) {
                            lastEdgeSwipeTime = currentTime
                            onSwipeToNext()
                            offsetX = -maxOffsetX
                        } else {
                            offsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                        }

                        offsetY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                        dismissProgress = 0f
                    } else {
                        // 1x时检测下滑退出 (REQ-020)
                        offsetX = 0f

                        if (pan.y > 0) {
                            dismissProgress += pan.y
                            offsetY = dismissProgress * 0.5f // 阻尼效果

                            if (dismissProgress > DISMISS_THRESHOLD && onDismiss != null) {
                                onDismiss()
                                dismissProgress = 0f
                                offsetY = 0f
                            }
                        } else {
                            dismissProgress = 0f
                            offsetY = 0f
                        }
                    }
                }
            }
            .pointerInput(isCurrentPage) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (!isCurrentPage) return@detectTapGestures

                        // 双击缩放 (REQ-017)
                        if (scale > 1.1f) {
                            // 缩放状态下恢复1x
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            // 1x时放大到2.5x，以点击位置为中心
                            scale = DOUBLE_TAP_SCALE
                            // 计算偏移使点击位置成为中心
                            val centerX = containerWidth / 2f
                            val centerY = containerHeight / 2f
                            offsetX = (centerX - tapOffset.x) * (DOUBLE_TAP_SCALE - 1f)
                            offsetY = (centerY - tapOffset.y) * (DOUBLE_TAP_SCALE - 1f)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(photo.systemUri))
                .crossfade(200)
                .build(),
            contentDescription = photo.displayName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    // 下滑时的透明度变化
                    alpha = if (scale <= 1f && dismissProgress > 0) {
                        (1f - dismissProgress / DISMISS_THRESHOLD * 0.3f).coerceIn(0.7f, 1f)
                    } else 1f
                },
            contentScale = ContentScale.Fit
        )
    }
}

// 常量
private const val MIN_SCALE = 1f
private const val MAX_SCALE = 10f
private const val DOUBLE_TAP_SCALE = 2.5f
private const val EDGE_THRESHOLD = 100f  // 边缘穿透阈值
private const val DISMISS_THRESHOLD = 300f  // 下滑退出阈值
