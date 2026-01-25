package com.example.photozen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.theme.PicZenTokens

/**
 * 可选中列表项组件 (DES-030)
 *
 * 带选中状态的列表项，提供丰富的视觉反馈：
 * - 选中时高亮边框
 * - 按压时缩放效果
 * - 选中指示图标
 *
 * @param selected 是否选中
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 * @param selectedBorderColor 选中时的边框颜色
 * @param showCheckIcon 是否显示选中图标
 * @param content 内容
 */
@Composable
fun SelectableListItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedBorderColor: Color = MaterialTheme.colorScheme.primary,
    showCheckIcon: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "listItemScale"
    )

    // 边框宽度动画
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 0.dp,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "borderWidth"
    )

    // 背景色动画
    val backgroundColor by animateColorAsState(
        targetValue = when {
            selected -> selectedBorderColor.copy(alpha = PicZenTokens.Alpha.SurfaceTint)
            isPressed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = PicZenTokens.Alpha.Pressed)
            else -> Color.Transparent
        },
        label = "backgroundColor"
    )

    // 阴影动画
    val elevation by animateDpAsState(
        targetValue = if (selected) PicZenTokens.Elevation.Level2 else PicZenTokens.Elevation.Level0,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "elevation"
    )

    val shape = RoundedCornerShape(PicZenTokens.Radius.M)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, shape)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, selectedBorderColor, shape)
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(PicZenTokens.Spacing.M)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()

            // 选中图标
            if (showCheckIcon && selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = selectedBorderColor,
                    modifier = Modifier.padding(start = PicZenTokens.Spacing.S)
                )
            }
        }
    }
}

/**
 * 多选列表项
 *
 * 支持多选模式的列表项，带勾选框样式。
 */
@Composable
fun MultiSelectListItem(
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    SelectableListItem(
        selected = selected,
        onClick = onToggle,
        modifier = modifier,
        enabled = enabled,
        showCheckIcon = true,
        content = content
    )
}

/**
 * 单选列表项
 *
 * 用于单选场景，选中时有明显的高亮效果。
 */
@Composable
fun RadioListItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    SelectableListItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        showCheckIcon = false,
        content = content
    )
}

/**
 * 悬停效果修饰符
 *
 * 为列表项添加悬停时的微妙背景变化。
 */
@Composable
fun Modifier.hoverHighlight(
    enabled: Boolean = true
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = PicZenTokens.Alpha.Hover)
        } else {
            Color.Transparent
        },
        label = "hoverBackground"
    )

    return this
        .background(backgroundColor, RoundedCornerShape(PicZenTokens.Radius.S))
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {}
        )
}
