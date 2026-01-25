package com.example.photozen.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.theme.PicZenTokens

/**
 * 增强型卡片组件
 *
 * 设计特性:
 * - 动态缩放: 按压时微缩，增加深度感
 * - 动态高度: 按压时阴影收缩
 * - 微渐变背景: 顶部微光增加高级感
 * - 精细边框: 半透明边框增加层次
 *
 * @param onClick 点击事件
 * @param modifier Modifier
 * @param elevation 默认高度
 * @param enableGloss 是否启用顶部光泽
 * @param content 卡片内容
 */
@Composable
fun EnhancedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: Dp = PicZenTokens.Elevation.Level1,
    enableGloss: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 动态缩放
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "cardScale"
    )

    // 动态高度
    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) (elevation - 2.dp).coerceAtLeast(0.dp) else elevation,
        animationSpec = tween(PicZenMotion.Duration.Quick),
        label = "cardElevation"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(PicZenTokens.Radius.L),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = animatedElevation,
        shadowElevation = animatedElevation,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = PicZenTokens.Alpha.Medium
            )
        ),
        interactionSource = interactionSource
    ) {
        Box {
            Column(content = content)

            // 微妙的顶部光泽
            if (enableGloss) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = PicZenTokens.Alpha.Gloss),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * 增强型可点击卡片 - 无onClick
 * 用于纯展示场景，但保留视觉效果
 */
@Composable
fun EnhancedCardContainer(
    modifier: Modifier = Modifier,
    elevation: Dp = PicZenTokens.Elevation.Level1,
    enableGloss: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(PicZenTokens.Radius.L),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = PicZenTokens.Alpha.Medium
            )
        )
    ) {
        Box {
            Column(content = content)

            if (enableGloss) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = PicZenTokens.Alpha.Gloss),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * 主操作卡片 - 用于首页主入口
 * 更大的圆角和阴影
 */
@Composable
fun PrimaryActionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "primaryCardScale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) PicZenTokens.Elevation.Level3 else PicZenTokens.Elevation.Level4,
        animationSpec = tween(PicZenMotion.Duration.Quick),
        label = "primaryCardElevation"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(PicZenTokens.Radius.XL),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ),
        interactionSource = interactionSource
    ) {
        Box {
            Column(content = content)

            // 顶部光泽
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
