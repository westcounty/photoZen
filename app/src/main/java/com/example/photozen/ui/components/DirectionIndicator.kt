package com.example.photozen.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * 滑动方向指示器 (Phase 3-6)
 * 
 * 显示当前滑动方向和状态。
 * 
 * ## 特性
 * - 图标随方向变化
 * - 到达阈值时脉冲动画
 * - 颜色与操作语义匹配（保留=绿色，删除=红色，待定=黄色）
 * 
 * ## 性能优化
 * - 仅在 progress > 0.1f 时渲染
 * - 使用 `graphicsLayer` 实现高效缩放动画
 */
@Composable
fun DirectionIndicator(
    direction: SwipeIndicatorDirection,
    progress: Float,
    hasReachedThreshold: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    showLabel: Boolean = false
) {
    // 根据方向获取配置
    val config = direction.getConfig()
    
    // 图标选择：到达阈值时使用实心图标
    val icon = if (hasReachedThreshold) config.iconFilled else config.iconOutlined
    
    // 脉冲动画
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    // 实际缩放：到达阈值时应用脉冲
    val actualScale = if (hasReachedThreshold) pulseScale else 1f
    
    // 透明度动画
    val alpha by animateFloatAsState(
        targetValue = if (progress > 0.1f) 0.95f else 0f,
        animationSpec = tween(150),
        label = "alpha"
    )
    
    if (alpha > 0f) {
        Column(
            modifier = modifier.graphicsLayer { this.alpha = alpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 图标容器
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = actualScale
                        scaleY = actualScale
                    }
                    .clip(CircleShape)
                    .background(config.color.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = config.label,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
            
            // 可选标签
            if (showLabel) {
                Text(
                    text = config.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = config.color
                )
            }
        }
    }
}

/**
 * 带发光效果的方向指示器
 * 
 * 增强版本，包含：
 * - 外发光光晕
 * - 更强的脉冲效果
 */
@Composable
fun GlowingDirectionIndicator(
    direction: SwipeIndicatorDirection,
    progress: Float,
    hasReachedThreshold: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    val config = direction.getConfig()
    val icon = if (hasReachedThreshold) config.iconFilled else config.iconOutlined
    
    // 发光动画
    val glowAnim = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    // 脉冲缩放
    val pulseScale by glowAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val actualScale = if (hasReachedThreshold) pulseScale else 1f
    val actualGlow = if (hasReachedThreshold) glowAlpha else 0.2f
    
    // 整体透明度
    val alpha by animateFloatAsState(
        targetValue = if (progress > 0.1f) 1f else 0f,
        animationSpec = tween(150),
        label = "alpha"
    )
    
    if (alpha > 0f) {
        Box(
            modifier = modifier
                .graphicsLayer { this.alpha = alpha }
                .size(size * 1.5f),
            contentAlignment = Alignment.Center
        ) {
            // 外发光层
            Box(
                modifier = Modifier
                    .size(size * 1.4f)
                    .graphicsLayer {
                        scaleX = actualScale
                        scaleY = actualScale
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    config.color.copy(alpha = actualGlow),
                                    config.color.copy(alpha = actualGlow * 0.5f),
                                    Color.Transparent
                                ),
                                radius = this.size.minDimension / 2
                            )
                        )
                    }
            )
            
            // 主图标
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = actualScale
                        scaleY = actualScale
                    }
                    .clip(CircleShape)
                    .background(config.color.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = config.label,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}

/**
 * 滑动方向枚举
 */
enum class SwipeIndicatorDirection {
    LEFT,   // 左滑 - 保留
    RIGHT,  // 右滑 - 保留
    UP,     // 上滑 - 删除
    DOWN,   // 下滑 - 待定
    NONE    // 无方向
}

/**
 * 方向配置
 */
data class DirectionConfig(
    val color: Color,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val label: String
)

/**
 * 获取方向对应的配置
 */
fun SwipeIndicatorDirection.getConfig(): DirectionConfig = when (this) {
    SwipeIndicatorDirection.LEFT, SwipeIndicatorDirection.RIGHT -> DirectionConfig(
        color = KeepGreen,
        iconOutlined = Icons.Default.FavoriteBorder,
        iconFilled = Icons.Default.Favorite,
        label = "保留"
    )
    SwipeIndicatorDirection.UP -> DirectionConfig(
        color = TrashRed,
        iconOutlined = Icons.Default.DeleteOutline,
        iconFilled = Icons.Default.Delete,
        label = "删除"
    )
    SwipeIndicatorDirection.DOWN -> DirectionConfig(
        color = MaybeAmber,
        iconOutlined = Icons.Default.HelpOutline,
        iconFilled = Icons.Default.Help,
        label = "待定"
    )
    SwipeIndicatorDirection.NONE -> DirectionConfig(
        color = Color.Gray,
        iconOutlined = Icons.Default.Circle,
        iconFilled = Icons.Default.Circle,
        label = ""
    )
}

/**
 * 小型方向指示器 - 用于角落显示
 */
@Composable
fun CompactDirectionIndicator(
    direction: SwipeIndicatorDirection,
    hasReachedThreshold: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    if (direction == SwipeIndicatorDirection.NONE) return
    
    val config = direction.getConfig()
    val icon = if (hasReachedThreshold) config.iconFilled else config.iconOutlined
    
    // 简单脉冲
    val scale by animateFloatAsState(
        targetValue = if (hasReachedThreshold) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(config.color.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = config.label,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
