package com.example.photozen.ui.components.fullscreen

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import kotlinx.coroutines.launch
import kotlin.math.abs

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
 * - REQ-020: 上下滑退出 (在1x时垂直滑动)
 * - 边缘穿透: 缩放状态下滑到边缘继续滑动切换照片
 *
 * @param photos 照片列表
 * @param currentIndex 当前索引
 * @param onIndexChange 索引变更回调
 * @param onDismiss 滑动退出回调
 * @param modifier Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableImagePager(
    photos: List<PhotoEntity>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onDismiss: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { photos.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // 只在 currentIndex 真正变化时同步（外部控制）
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(currentIndex)
        }
    }

    // 用户滑动后同步到外部
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != currentIndex) {
            onIndexChange(pagerState.settledPage)
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
            onDismiss = onDismiss,
            onTap = onTap
        )
    }
}

/**
 * 单张可缩放图片
 *
 * 手势行为:
 * - 1x缩放时：水平滑动切换照片（由Pager处理），垂直滑动退出预览
 * - 放大时：拖动平移查看，滑到边缘切换照片
 * - 双指捏合：缩放图片
 * - 双击：切换1x/2.5x缩放
 */
@Composable
private fun ZoomableImage(
    photo: PhotoEntity,
    isCurrentPage: Boolean,
    onSwipeToNext: () -> Unit,
    onSwipeToPrevious: () -> Unit,
    onDismiss: (() -> Unit)?,
    onTap: (() -> Unit)?
) {
    val context = LocalContext.current

    // 缩放和偏移状态
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // 容器尺寸
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }

    // 垂直滑动累积距离（用于退出）
    var dismissProgress by remember { mutableFloatStateOf(0f) }

    // 边缘穿透冷却
    var lastEdgeSwipeTime by remember { mutableLongStateOf(0L) }

    // 是否正在处理垂直滑动退出手势
    var isHandlingVerticalGesture by remember { mutableStateOf(false) }

    // 切换页面时重置状态
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            dismissProgress = 0f
            isHandlingVerticalGesture = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                containerWidth = size.width.toFloat()
                containerHeight = size.height.toFloat()
            }
            // 自定义手势检测：选择性消费手势
            .pointerInput(isCurrentPage, scale) {
                if (!isCurrentPage) return@pointerInput

                awaitEachGesture {
                    // 等待第一个触点
                    val firstDown = awaitFirstDown(requireUnconsumed = false)

                    var totalPanX = 0f
                    var totalPanY = 0f
                    var pointerId = firstDown.id
                    var gestureStarted = false
                    var isMultiTouch = false

                    do {
                        val event = awaitPointerEvent()

                        // 检测多点触控（捏合缩放）
                        val pointerCount = event.changes.count { it.pressed }
                        if (pointerCount >= 2) {
                            isMultiTouch = true
                            gestureStarted = true
                        }

                        if (event.type == PointerEventType.Move) {
                            if (isMultiTouch && pointerCount >= 2) {
                                // 处理捏合缩放
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()

                                val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                scale = newScale

                                if (newScale > 1f) {
                                    val maxOffsetX = (newScale - 1f) * containerWidth / 2f
                                    val maxOffsetY = (newScale - 1f) * containerHeight / 2f
                                    offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                }

                                // 消费所有变化
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (pointerCount == 1) {
                                // 单指拖动
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.firstOrNull { it.pressed }

                                if (change != null) {
                                    pointerId = change.id
                                    val panX = change.position.x - change.previousPosition.x
                                    val panY = change.position.y - change.previousPosition.y

                                    totalPanX += panX
                                    totalPanY += panY

                                    if (scale > 1.01f) {
                                        // 放大状态：处理平移
                                        gestureStarted = true

                                        val newOffsetX = offsetX + panX
                                        val newOffsetY = offsetY + panY
                                        val maxOffsetX = (scale - 1f) * containerWidth / 2f
                                        val maxOffsetY = (scale - 1f) * containerHeight / 2f

                                        // 边缘穿透检测
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

                                        change.consume()
                                    } else {
                                        // 1x状态：判断是水平还是垂直滑动
                                        val absX = abs(totalPanX)
                                        val absY = abs(totalPanY)

                                        // 需要足够的移动距离才判断方向
                                        if (absX > 20f || absY > 20f) {
                                            if (!gestureStarted) {
                                                // 首次判断方向
                                                if (absY > absX * 0.8f) {
                                                    // 垂直方向为主 -> 处理退出手势
                                                    gestureStarted = true
                                                    isHandlingVerticalGesture = true
                                                } else {
                                                    // 水平方向为主 -> 不消费，让Pager处理
                                                    isHandlingVerticalGesture = false
                                                    // 不设置gestureStarted，继续观察
                                                }
                                            }
                                        }

                                        // 处理垂直退出手势
                                        if (isHandlingVerticalGesture) {
                                            dismissProgress += panY
                                            offsetY = dismissProgress * 0.5f

                                            // 上滑或下滑超过阈值都退出
                                            if (abs(dismissProgress) > DISMISS_THRESHOLD && onDismiss != null) {
                                                onDismiss()
                                                dismissProgress = 0f
                                                offsetY = 0f
                                            }

                                            change.consume()
                                        }
                                        // 水平滑动不消费，让Pager处理
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // 手势结束时重置状态
                    if (scale <= 1.01f) {
                        dismissProgress = 0f
                        offsetY = 0f
                    }
                    isHandlingVerticalGesture = false
                }
            }
            .pointerInput(isCurrentPage, onTap) {
                detectTapGestures(
                    onTap = {
                        if (isCurrentPage) {
                            onTap?.invoke()
                        }
                    },
                    onDoubleTap = { tapOffset ->
                        if (!isCurrentPage) return@detectTapGestures

                        // 双击缩放 (REQ-017)
                        if (scale > 1.1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = DOUBLE_TAP_SCALE
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
                    // 滑动退出时的透明度变化
                    alpha = if (scale <= 1f && abs(dismissProgress) > 0) {
                        (1f - abs(dismissProgress) / DISMISS_THRESHOLD * 0.3f).coerceIn(0.7f, 1f)
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
private const val DISMISS_THRESHOLD = 450f  // 垂直滑动退出阈值
