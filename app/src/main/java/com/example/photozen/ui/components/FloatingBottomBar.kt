package com.example.photozen.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.theme.PicZenTokens

/**
 * 浮动操作项数据
 */
data class FloatingAction(
    val icon: ImageVector,
    val label: String,
    val tintColor: Color,
    val onClick: () -> Unit
)

/**
 * 毛玻璃效果底部操作栏
 *
 * 设计特性:
 * - 浮动圆角设计: 脱离底部边缘
 * - 半透明背景: 85%透明度
 * - 微动效反馈: 按压时图标下沉和缩放
 * - 精细边框: 增加层次感
 *
 * @param actions 操作项列表
 * @param modifier Modifier
 */
@Composable
fun FloatingBottomBar(
    actions: List<FloatingAction>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PicZenTokens.Spacing.L)
            .padding(bottom = PicZenTokens.Spacing.M)
            .navigationBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(PicZenTokens.ComponentSize.BottomBarHeight),
            shape = RoundedCornerShape(PicZenTokens.Radius.XL),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = PicZenTokens.Elevation.Level4,
            shadowElevation = PicZenTokens.Elevation.Level3,
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PicZenTokens.Spacing.M),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.forEach { action ->
                    FloatingActionItem(action = action)
                }
            }
        }
    }
}

/**
 * 浮动操作项
 */
@Composable
private fun FloatingActionItem(
    action: FloatingAction
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "actionScale"
    )

    // 图标下沉动画
    val iconOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "iconOffset"
    )

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = action.onClick
            )
            .padding(horizontal = PicZenTokens.Spacing.S),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 图标容器 - 带动态背景
        Box(
            modifier = Modifier
                .size(PicZenTokens.ComponentSize.BottomBarIconContainer)
                .offset(y = iconOffset)
                .background(
                    color = action.tintColor.copy(
                        alpha = if (isPressed) PicZenTokens.Alpha.Pressed
                        else PicZenTokens.Alpha.Hover
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                modifier = Modifier.size(PicZenTokens.IconSize.M),
                tint = action.tintColor
            )
        }

        Spacer(modifier = Modifier.height(PicZenTokens.Spacing.XS))

        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 紧凑型浮动底部栏
 * 用于空间有限的场景
 */
@Composable
fun CompactFloatingBottomBar(
    actions: List<FloatingAction>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PicZenTokens.Spacing.XL)
            .padding(bottom = PicZenTokens.Spacing.S)
            .navigationBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(PicZenTokens.Radius.L),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = PicZenTokens.Elevation.Level3,
            shadowElevation = PicZenTokens.Elevation.Level2,
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PicZenTokens.Spacing.S),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.forEach { action ->
                    CompactFloatingActionItem(action = action)
                }
            }
        }
    }
}

@Composable
private fun CompactFloatingActionItem(
    action: FloatingAction
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "compactActionScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(40.dp)
            .background(
                color = action.tintColor.copy(
                    alpha = if (isPressed) PicZenTokens.Alpha.Pressed
                    else PicZenTokens.Alpha.Hover
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = action.onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            modifier = Modifier.size(PicZenTokens.IconSize.M),
            tint = action.tintColor
        )
    }
}
