package com.example.photozen.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.PicZenActionColors
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.theme.PicZenTokens
import com.example.photozen.ui.theme.TrashRed

/**
 * Photo status badge component - displays a small tag/ribbon indicating photo status.
 * 
 * Position: Top-left corner of the photo
 * Style: Small rounded ribbon with color-coded status (non-interactive appearance):
 *   - KEEP → Green with heart icon (not check to avoid "selected" confusion)
 *   - MAYBE → Amber with ? icon
 *   - TRASH → Red with trash icon (not X to avoid "close button" confusion)
 *   - UNSORTED → Not displayed (returns null)
 *
 * Design principles:
 *   - No shadow (avoids button appearance)
 *   - Rounded rectangle shape (avoids circular button appearance)
 *   - Distinctive icons that don't imply interactivity
 *
 * @param status The photo status to display
 * @param size The size of the badge (default: 24.dp for grid view, use 32.dp for fullscreen)
 * @param showForUnsorted Whether to show badge for UNSORTED status (default: false)
 * @param modifier Modifier for positioning
 */
@Composable
fun PhotoStatusBadge(
    status: PhotoStatus,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    showForUnsorted: Boolean = false
) {
    // Don't display badge for UNSORTED status unless explicitly requested
    if (status == PhotoStatus.UNSORTED && !showForUnsorted) {
        return
    }
    
    val (backgroundColor, icon, iconTint) = when (status) {
        PhotoStatus.KEEP -> Triple(KeepGreen.copy(alpha = 0.9f), Icons.Default.Favorite, Color.White)
        PhotoStatus.MAYBE -> Triple(MaybeAmber.copy(alpha = 0.9f), Icons.Default.QuestionMark, Color.White)
        PhotoStatus.TRASH -> Triple(TrashRed.copy(alpha = 0.9f), Icons.Default.Delete, Color.White)
        PhotoStatus.UNSORTED -> Triple(Color.Gray.copy(alpha = 0.6f), null, Color.White)
    }
    
    // Use rounded rectangle shape (like a small ribbon/tag) instead of circle
    // to avoid button-like appearance
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = status.name,
                tint = iconTint,
                modifier = Modifier.size(size * 0.65f)
            )
        }
    }
}

/**
 * Convenience composable that wraps content with a status badge overlay.
 * 
 * @param status The photo status
 * @param badgeSize Size of the badge
 * @param showBadge Whether to show the badge at all
 * @param content The content to overlay with the badge
 */
@Composable
fun PhotoWithStatusBadge(
    status: PhotoStatus,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 24.dp,
    showBadge: Boolean = true,
    badgePadding: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        
        if (showBadge && status != PhotoStatus.UNSORTED) {
            PhotoStatusBadge(
                status = status,
                size = badgeSize,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(badgePadding)
            )
        }
    }
}

/**
 * 照片状态三角角标 (REQ-063)
 *
 * 设计: 左上角直角三角形，足够小且表意明确
 * - 保留 (KEEP): 绿色
 * - 待定 (MAYBE): 琥珀色
 * - 回收站 (TRASH): 红色
 * - 未筛选 (UNSORTED): 不显示角标
 *
 * 应用于: 我的相册照片列表、时间线照片列表
 *
 * @param status 照片状态
 * @param size 角标大小，默认16dp
 * @param modifier Modifier
 */
@Composable
fun PhotoStatusTriangleBadge(
    status: PhotoStatus,
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    // 未筛选状态不显示角标
    if (status == PhotoStatus.UNSORTED) return

    val color = when (status) {
        PhotoStatus.KEEP -> KeepGreen
        PhotoStatus.MAYBE -> MaybeAmber
        PhotoStatus.TRASH -> TrashRed
        PhotoStatus.UNSORTED -> return
    }

    Canvas(
        modifier = modifier.size(size)
    ) {
        // 绘制左上角直角三角形
        val path = Path().apply {
            moveTo(0f, 0f)                      // 左上角
            lineTo(this@Canvas.size.width, 0f)  // 右上角
            lineTo(0f, this@Canvas.size.height) // 左下角
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Fill
        )
    }
}

/**
 * 带小图标的照片状态三角角标（增强版）
 *
 * 在三角形内绘制小图标增强表意:
 * - 保留: 勾号
 * - 待定: 圆点
 * - 回收站: X号
 *
 * @param status 照片状态
 * @param size 角标大小，默认20dp（比基础版稍大以容纳图标）
 * @param modifier Modifier
 */
@Composable
fun PhotoStatusTriangleBadgeWithIcon(
    status: PhotoStatus,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    // 未筛选状态不显示角标
    if (status == PhotoStatus.UNSORTED) return

    val color = when (status) {
        PhotoStatus.KEEP -> KeepGreen
        PhotoStatus.MAYBE -> MaybeAmber
        PhotoStatus.TRASH -> TrashRed
        PhotoStatus.UNSORTED -> return
    }

    Canvas(
        modifier = modifier.size(size)
    ) {
        val triangleSize = this.size.width

        // 绘制左上角直角三角形
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(triangleSize, 0f)
            lineTo(0f, triangleSize)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Fill
        )

        // 绘制小图标（白色，位于三角形左上区域）
        val iconColor = Color.White
        val iconSize = triangleSize * 0.35f
        val iconCenter = Offset(triangleSize * 0.28f, triangleSize * 0.28f)
        val strokeWidth = triangleSize * 0.08f

        when (status) {
            PhotoStatus.KEEP -> {
                // 绘制勾号 ✓
                val checkPath = Path().apply {
                    moveTo(iconCenter.x - iconSize * 0.4f, iconCenter.y)
                    lineTo(iconCenter.x - iconSize * 0.1f, iconCenter.y + iconSize * 0.3f)
                    lineTo(iconCenter.x + iconSize * 0.4f, iconCenter.y - iconSize * 0.3f)
                }
                drawPath(
                    path = checkPath,
                    color = iconColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }
            PhotoStatus.MAYBE -> {
                // 绘制圆点
                drawCircle(
                    color = iconColor,
                    radius = iconSize * 0.25f,
                    center = iconCenter
                )
            }
            PhotoStatus.TRASH -> {
                // 绘制X号
                val xSize = iconSize * 0.3f
                drawLine(
                    color = iconColor,
                    start = Offset(iconCenter.x - xSize, iconCenter.y - xSize),
                    end = Offset(iconCenter.x + xSize, iconCenter.y + xSize),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = iconColor,
                    start = Offset(iconCenter.x + xSize, iconCenter.y - xSize),
                    end = Offset(iconCenter.x - xSize, iconCenter.y + xSize),
                    strokeWidth = strokeWidth
                )
            }
            PhotoStatus.UNSORTED -> { /* 不会执行 */ }
        }
    }
}

/**
 * 带状态三角角标的照片容器
 *
 * @param status 照片状态
 * @param showBadge 是否显示角标
 * @param useTriangleBadge 是否使用三角角标（true）或方形角标（false）
 * @param badgeSize 角标大小
 * @param content 内容
 */
@Composable
fun PhotoWithTriangleBadge(
    status: PhotoStatus,
    modifier: Modifier = Modifier,
    showBadge: Boolean = true,
    useEnhancedBadge: Boolean = false,
    badgeSize: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        if (showBadge && status != PhotoStatus.UNSORTED) {
            if (useEnhancedBadge) {
                PhotoStatusTriangleBadgeWithIcon(
                    status = status,
                    size = badgeSize,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            } else {
                PhotoStatusTriangleBadge(
                    status = status,
                    size = badgeSize,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

/**
 * 获取状态对应的颜色
 */
fun PhotoStatus.toColor(): Color = when (this) {
    PhotoStatus.KEEP -> KeepGreen
    PhotoStatus.MAYBE -> MaybeAmber
    PhotoStatus.TRASH -> TrashRed
    PhotoStatus.UNSORTED -> Color.Gray
}

/**
 * 状态徽章尺寸枚举
 */
enum class PillSize {
    Small,   // 18dp x 10dp icon
    Medium,  // 24dp x 14dp icon
    Large    // 32dp x 18dp icon
}

/**
 * 精致圆角状态徽章 (DES-017, DES-018)
 *
 * 设计特性:
 * - 带光泽效果的渐变背景
 * - 微阴影层次增加深度感
 * - 平滑圆角设计
 * - 语义化图标 (勾号/时钟/关闭)
 *
 * 增强动画:
 * - 状态切换时缩放脉冲 (1.2f)
 * - 图标切换动画 (scaleIn/scaleOut)
 * - 颜色过渡动画
 *
 * @param status 照片状态
 * @param modifier Modifier
 * @param size 徽章尺寸
 */
@Composable
fun PhotoStatusPill(
    status: PhotoStatus,
    modifier: Modifier = Modifier,
    size: PillSize = PillSize.Medium
) {
    // UNSORTED 状态不显示
    if (status == PhotoStatus.UNSORTED) return

    val (icon, color) = when (status) {
        PhotoStatus.KEEP -> Icons.Rounded.Check to PicZenActionColors.Keep.Primary
        PhotoStatus.MAYBE -> Icons.Rounded.Schedule to PicZenActionColors.Maybe.Primary
        PhotoStatus.TRASH -> Icons.Rounded.Close to PicZenActionColors.Trash.Primary
        PhotoStatus.UNSORTED -> return
    }

    // 状态变化脉冲动画
    var previousStatus by remember { mutableStateOf(status) }
    var triggerBounce by remember { mutableIntStateOf(0) }

    LaunchedEffect(status) {
        if (status != previousStatus) {
            triggerBounce++
            previousStatus = status
        }
    }

    val bounceScale by animateFloatAsState(
        targetValue = if (triggerBounce % 2 == 1) 1.2f else 1f,
        animationSpec = PicZenMotion.Springs.playful(),
        label = "bounceScale",
        finishedListener = {
            if (triggerBounce % 2 == 1) {
                triggerBounce++
            }
        }
    )

    // 颜色过渡动画
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(PicZenMotion.Duration.Normal),
        label = "pillColor"
    )

    // 根据尺寸计算容器和图标大小
    val (containerSize, iconSize) = when (size) {
        PillSize.Small -> 18.dp to 10.dp
        PillSize.Medium -> 24.dp to 14.dp
        PillSize.Large -> 32.dp to 18.dp
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            }
            .size(containerSize)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(PicZenTokens.Radius.S),
                ambientColor = animatedColor.copy(alpha = 0.3f),
                spotColor = animatedColor.copy(alpha = 0.2f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        animatedColor,
                        animatedColor.copy(alpha = 0.85f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(containerSize.value, containerSize.value)
                ),
                shape = RoundedCornerShape(PicZenTokens.Radius.S)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 顶部光泽层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = containerSize.value * 0.5f
                    ),
                    shape = RoundedCornerShape(PicZenTokens.Radius.S)
                )
        )

        // 图标 - 带切换动画
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (scaleIn(animationSpec = PicZenMotion.Springs.playful()) + fadeIn()) togetherWith
                        (scaleOut() + fadeOut())
            },
            label = "iconTransition"
        ) { targetIcon ->
            Icon(
                imageVector = targetIcon,
                contentDescription = status.name,
                modifier = Modifier.size(iconSize),
                tint = Color.White
            )
        }
    }
}

/**
 * 带精致状态徽章的照片容器
 *
 * @param status 照片状态
 * @param showBadge 是否显示徽章
 * @param badgeSize 徽章尺寸
 * @param badgePadding 徽章边距
 * @param content 内容
 */
@Composable
fun PhotoWithStatusPill(
    status: PhotoStatus,
    modifier: Modifier = Modifier,
    showBadge: Boolean = true,
    badgeSize: PillSize = PillSize.Medium,
    badgePadding: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        if (showBadge && status != PhotoStatus.UNSORTED) {
            PhotoStatusPill(
                status = status,
                size = badgeSize,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(badgePadding)
            )
        }
    }
}
