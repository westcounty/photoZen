package com.example.photozen.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.photozen.ui.theme.PicZenActionColors
import kotlin.math.abs

/**
 * 边缘发光效果组件 (Phase 3-6)
 * 
 * 根据滑动方向和进度在卡片边缘显示发光效果：
 * - 右滑（保留）→ 左边缘绿色发光
 * - 左滑（保留）→ 右边缘绿色发光
 * - 上滑（删除）→ 底边缘红色发光
 * - 下滑（待定）→ 顶边缘黄色发光
 * 
 * ## 性能优化
 * - 使用 `drawBehind` 避免重组开销
 * - 仅在滑动进度 > 0.1f 时绘制
 * - 动画使用 `animateFloatAsState` 与 `infiniteRepeatable`
 * 
 * @param swipeProgressX 水平滑动进度 (-1 到 1)
 * @param swipeProgressY 垂直滑动进度 (-1 到 1)
 * @param hasReachedThreshold 是否已达到触发阈值
 * @param modifier Modifier
 */
@Composable
fun EdgeGlowOverlay(
    swipeProgressX: Float,
    swipeProgressY: Float,
    hasReachedThreshold: Boolean,
    modifier: Modifier = Modifier
) {
    // 发光强度动画（到达阈值时增强）
    val glowIntensity by animateFloatAsState(
        targetValue = if (hasReachedThreshold) 0.8f else 0.5f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "glowIntensity"
    )
    
    // 脉冲动画（到达阈值时）
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseFactor by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseFactor"
    )
    
    val effectivePulse = if (hasReachedThreshold) pulseFactor else 1f
    
    // 判断主要滑动方向
    val isHorizontalSwipe = abs(swipeProgressX) > abs(swipeProgressY)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                when {
                    // 右滑 - 左边缘绿色发光
                    isHorizontalSwipe && swipeProgressX > 0.1f -> {
                        drawLeftEdgeGlow(
                            color = PicZenActionColors.Keep.Primary,
                            progress = swipeProgressX,
                            intensity = glowIntensity * effectivePulse
                        )
                    }
                    // 左滑 - 右边缘绿色发光
                    isHorizontalSwipe && swipeProgressX < -0.1f -> {
                        drawRightEdgeGlow(
                            color = PicZenActionColors.Keep.Primary,
                            progress = abs(swipeProgressX),
                            intensity = glowIntensity * effectivePulse
                        )
                    }
                    // 上滑 - 底边缘红色发光
                    !isHorizontalSwipe && swipeProgressY < -0.1f -> {
                        drawBottomEdgeGlow(
                            color = PicZenActionColors.Trash.Primary,
                            progress = abs(swipeProgressY),
                            intensity = glowIntensity * effectivePulse
                        )
                    }
                    // 下滑 - 顶边缘黄色发光
                    !isHorizontalSwipe && swipeProgressY > 0.1f -> {
                        drawTopEdgeGlow(
                            color = PicZenActionColors.Maybe.Primary,
                            progress = swipeProgressY,
                            intensity = glowIntensity * effectivePulse
                        )
                    }
                }
            }
    )
}

/**
 * 左边缘发光
 */
private fun DrawScope.drawLeftEdgeGlow(
    color: Color,
    progress: Float,
    intensity: Float
) {
    val glowWidth = size.width * 0.3f * progress.coerceIn(0f, 1f)
    val alpha = (progress * intensity).coerceIn(0f, 0.7f)
    
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.5f),
                Color.Transparent
            ),
            startX = 0f,
            endX = glowWidth
        ),
        topLeft = Offset.Zero,
        size = Size(glowWidth, size.height)
    )
}

/**
 * 右边缘发光
 */
private fun DrawScope.drawRightEdgeGlow(
    color: Color,
    progress: Float,
    intensity: Float
) {
    val glowWidth = size.width * 0.3f * progress.coerceIn(0f, 1f)
    val alpha = (progress * intensity).coerceIn(0f, 0.7f)
    
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = alpha * 0.5f),
                color.copy(alpha = alpha)
            ),
            startX = size.width - glowWidth,
            endX = size.width
        ),
        topLeft = Offset(size.width - glowWidth, 0f),
        size = Size(glowWidth, size.height)
    )
}

/**
 * 顶边缘发光
 */
private fun DrawScope.drawTopEdgeGlow(
    color: Color,
    progress: Float,
    intensity: Float
) {
    val glowHeight = size.height * 0.25f * progress.coerceIn(0f, 1f)
    val alpha = (progress * intensity).coerceIn(0f, 0.7f)
    
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.5f),
                Color.Transparent
            ),
            startY = 0f,
            endY = glowHeight
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, glowHeight)
    )
}

/**
 * 底边缘发光
 */
private fun DrawScope.drawBottomEdgeGlow(
    color: Color,
    progress: Float,
    intensity: Float
) {
    val glowHeight = size.height * 0.25f * progress.coerceIn(0f, 1f)
    val alpha = (progress * intensity).coerceIn(0f, 0.7f)
    
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = alpha * 0.5f),
                color.copy(alpha = alpha)
            ),
            startY = size.height - glowHeight,
            endY = size.height
        ),
        topLeft = Offset(0f, size.height - glowHeight),
        size = Size(size.width, glowHeight)
    )
}

/**
 * 角落发光效果 - 用于更柔和的视觉效果
 */
@Composable
fun CornerGlowOverlay(
    swipeProgressX: Float,
    swipeProgressY: Float,
    hasReachedThreshold: Boolean,
    modifier: Modifier = Modifier
) {
    val glowIntensity by animateFloatAsState(
        targetValue = if (hasReachedThreshold) 0.9f else 0.6f,
        animationSpec = tween(200),
        label = "glowIntensity"
    )
    
    val isHorizontalSwipe = abs(swipeProgressX) > abs(swipeProgressY)
    
    // 确定发光颜色和位置
    val (glowColor, glowPosition) = when {
        isHorizontalSwipe && swipeProgressX > 0.1f -> PicZenActionColors.Keep.Primary to GlowPosition.LEFT
        isHorizontalSwipe && swipeProgressX < -0.1f -> PicZenActionColors.Keep.Primary to GlowPosition.RIGHT
        !isHorizontalSwipe && swipeProgressY < -0.1f -> PicZenActionColors.Trash.Primary to GlowPosition.BOTTOM
        !isHorizontalSwipe && swipeProgressY > 0.1f -> PicZenActionColors.Maybe.Primary to GlowPosition.TOP
        else -> Color.Transparent to GlowPosition.NONE
    }
    
    val progress = when (glowPosition) {
        GlowPosition.LEFT, GlowPosition.RIGHT -> abs(swipeProgressX)
        GlowPosition.TOP, GlowPosition.BOTTOM -> abs(swipeProgressY)
        GlowPosition.NONE -> 0f
    }
    
    if (glowPosition != GlowPosition.NONE && progress > 0.1f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBehind {
                    drawRadialGlow(
                        color = glowColor,
                        position = glowPosition,
                        progress = progress,
                        intensity = glowIntensity
                    )
                }
        )
    }
}

private enum class GlowPosition {
    LEFT, RIGHT, TOP, BOTTOM, NONE
}

/**
 * 径向发光绘制
 */
private fun DrawScope.drawRadialGlow(
    color: Color,
    position: GlowPosition,
    progress: Float,
    intensity: Float
) {
    val alpha = (progress * intensity * 0.8f).coerceIn(0f, 0.6f)
    val radius = size.minDimension * 0.5f * progress.coerceIn(0f, 1f)
    
    val center = when (position) {
        GlowPosition.LEFT -> Offset(0f, size.height / 2)
        GlowPosition.RIGHT -> Offset(size.width, size.height / 2)
        GlowPosition.TOP -> Offset(size.width / 2, 0f)
        GlowPosition.BOTTOM -> Offset(size.width / 2, size.height)
        GlowPosition.NONE -> return
    }
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.4f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius
    )
}
