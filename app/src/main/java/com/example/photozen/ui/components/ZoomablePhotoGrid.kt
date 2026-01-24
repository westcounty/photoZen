package com.example.photozen.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs

/**
 * PhotoZen 双指缩放网格容器组件 (REQ-003, REQ-008)
 * =====================================================
 *
 * 最佳实践参考: Google Photos, iOS Photos
 *
 * 功能特性:
 * - 双指张开: 减少列数 (放大照片)
 * - 双指收缩: 增加列数 (缩小照片)
 * - 边界回弹: 到达最大/最小列数时有回弹效果和震动反馈
 * - 手势与滚动协调: 缩放手势不干扰列表滚动
 *
 * 实现要点:
 * 1. 使用累积缩放因子 + 阈值触发机制
 * 2. 阈值设计: 张开1.3x触发减列，收缩0.7x触发增列
 * 3. 边界处理: 到达边界时重置累积值并提供震动反馈
 * 4. 回弹动画: 使用spring动画提供视觉反馈
 *
 * @param columns 当前列数
 * @param onColumnsChange 列数变更回调
 * @param minColumns 最小列数 (默认2，网格模式)
 * @param maxColumns 最大列数 (默认5)
 * @param hapticEnabled 是否启用震动反馈
 * @param modifier Modifier
 * @param content 子内容
 */
@Composable
fun ZoomablePhotoGrid(
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    minColumns: Int = 2,
    maxColumns: Int = 5,
    hapticEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // 累积缩放因子
    var cumulativeScale by remember { mutableFloatStateOf(1f) }

    // 边界回弹状态
    var isBouncing by remember { mutableStateOf(false) }
    var bounceDirection by remember { mutableStateOf(0) } // -1: 向左回弹(已最小), 1: 向右回弹(已最大)

    // 回弹动画
    val bounceScale by animateFloatAsState(
        targetValue = if (isBouncing) {
            if (bounceDirection > 0) 0.95f else 1.05f
        } else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = {
            isBouncing = false
            bounceDirection = 0
        },
        label = "bounceScale"
    )

    Box(
        modifier = modifier
            .pointerInput(columns, minColumns, maxColumns, hapticEnabled) {
                awaitEachGesture {
                    // 等待第一个手指按下
                    awaitFirstDown(requireUnconsumed = false)

                    // 重置累积缩放值
                    cumulativeScale = 1f

                    do {
                        val event = awaitPointerEvent()

                        // 只在多指触控时处理缩放
                        if (event.changes.size >= 2) {
                            val zoom = event.calculateZoom()

                            if (zoom != 1f) {
                                cumulativeScale *= zoom

                                // 检查是否达到阈值
                                when {
                                    // 双指张开 -> 减少列数 (放大)
                                    cumulativeScale > ZOOM_OUT_THRESHOLD && columns > minColumns -> {
                                        onColumnsChange(columns - 1)
                                        cumulativeScale = 1f
                                        if (hapticEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                    // 双指收缩 -> 增加列数 (缩小)
                                    cumulativeScale < ZOOM_IN_THRESHOLD && columns < maxColumns -> {
                                        onColumnsChange(columns + 1)
                                        cumulativeScale = 1f
                                        if (hapticEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                    // 边界回弹 - 已达最小列数但继续张开
                                    columns == minColumns && cumulativeScale > ZOOM_OUT_THRESHOLD -> {
                                        cumulativeScale = 1f
                                        isBouncing = true
                                        bounceDirection = -1
                                        if (hapticEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    // 边界回弹 - 已达最大列数但继续收缩
                                    columns == maxColumns && cumulativeScale < ZOOM_IN_THRESHOLD -> {
                                        cumulativeScale = 1f
                                        isBouncing = true
                                        bounceDirection = 1
                                        if (hapticEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }

                                // 消费缩放手势，防止与其他手势冲突
                                event.changes.forEach {
                                    if (it.positionChanged()) {
                                        it.consume()
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        content()
    }
}

/**
 * 缩放手势的阈值常量
 */
private const val ZOOM_OUT_THRESHOLD = 1.35f  // 张开阈值 (减少列数)
private const val ZOOM_IN_THRESHOLD = 0.7f    // 收缩阈值 (增加列数)

/**
 * 扩展 Modifier: 为任意 Composable 添加双指缩放切换列数功能
 *
 * 使用示例:
 * ```kotlin
 * LazyVerticalGrid(
 *     columns = GridCells.Fixed(columnCount),
 *     modifier = Modifier.zoomableColumns(
 *         columns = columnCount,
 *         onColumnsChange = { columnCount = it }
 *     )
 * )
 * ```
 */
@Composable
fun Modifier.zoomableColumns(
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    minColumns: Int = 2,
    maxColumns: Int = 5,
    hapticEnabled: Boolean = true
): Modifier {
    val haptic = LocalHapticFeedback.current
    var cumulativeScale by remember { mutableFloatStateOf(1f) }

    return this.pointerInput(columns, minColumns, maxColumns, hapticEnabled) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            cumulativeScale = 1f

            do {
                val event = awaitPointerEvent()

                if (event.changes.size >= 2) {
                    val zoom = event.calculateZoom()

                    if (zoom != 1f) {
                        cumulativeScale *= zoom

                        when {
                            cumulativeScale > ZOOM_OUT_THRESHOLD && columns > minColumns -> {
                                onColumnsChange(columns - 1)
                                cumulativeScale = 1f
                                if (hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                            cumulativeScale < ZOOM_IN_THRESHOLD && columns < maxColumns -> {
                                onColumnsChange(columns + 1)
                                cumulativeScale = 1f
                                if (hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                            columns == minColumns && cumulativeScale > ZOOM_OUT_THRESHOLD -> {
                                cumulativeScale = 1f
                                if (hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            columns == maxColumns && cumulativeScale < ZOOM_IN_THRESHOLD -> {
                                cumulativeScale = 1f
                                if (hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }

                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
