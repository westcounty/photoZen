package com.example.photozen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
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
