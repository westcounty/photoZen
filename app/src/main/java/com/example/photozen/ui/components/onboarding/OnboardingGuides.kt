package com.example.photozen.ui.components.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 滑动筛选全屏新手引导 (REQ-067)
 *
 * 精美的全屏引导界面，展示滑动手势操作：
 * - 中央模拟照片卡片，带动画演示
 * - 四个方向的浮动指示器
 * - 流畅的动画效果
 *
 * @param onDismiss 关闭引导回调
 */
@Composable
fun SwipeSortOnboarding(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 当前演示的方向 (0=右, 1=左, 2=上, 3=下)
    var currentDirection by remember { mutableIntStateOf(0) }

    // 自动切换方向
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            currentDirection = (currentDirection + 1) % 4
        }
    }

    // 卡片动画
    val infiniteTransition = rememberInfiniteTransition(label = "card")
    val cardSwipeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "cardSwipe"
    )

    // 根据当前方向计算卡片偏移
    val swipeDistance = 80f
    val cardOffsetX = when (currentDirection) {
        0 -> swipeDistance * cardSwipeProgress  // 右
        1 -> -swipeDistance * cardSwipeProgress // 左
        else -> 0f
    }
    val cardOffsetY = when (currentDirection) {
        2 -> -swipeDistance * cardSwipeProgress // 上
        3 -> swipeDistance * cardSwipeProgress  // 下
        else -> 0f
    }
    val cardRotation = when (currentDirection) {
        0 -> 8f * cardSwipeProgress
        1 -> -8f * cardSwipeProgress
        else -> 0f
    }
    val cardAlpha = 1f - (cardSwipeProgress * 0.3f)

    // 渐变背景
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f0f23)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* 防止点击穿透 */ }
            ),
        contentAlignment = Alignment.Center
    ) {
        // 背景装饰光晕
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-50).dp, y = (-100).dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KeepGreen.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = 80.dp, y = 150.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TrashRed.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 关闭按钮
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // 标题
            Text(
                text = "滑动筛选",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "快速整理你的照片",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 中央演示区域
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // 四个方向指示器
                // 左侧 - 保留
                DirectionIndicatorBubble(
                    icon = Icons.Default.Favorite,
                    label = "保留",
                    color = KeepGreen,
                    isActive = currentDirection == 1,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-20).dp)
                )

                // 右侧 - 保留
                DirectionIndicatorBubble(
                    icon = Icons.Default.Favorite,
                    label = "保留",
                    color = KeepGreen,
                    isActive = currentDirection == 0,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 20.dp)
                )

                // 上方 - 删除
                DirectionIndicatorBubble(
                    icon = Icons.Default.Delete,
                    label = "删除",
                    color = TrashRed,
                    isActive = currentDirection == 2,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-10).dp)
                )

                // 下方 - 待定
                DirectionIndicatorBubble(
                    icon = Icons.Default.QuestionMark,
                    label = "待定",
                    color = MaybeAmber,
                    isActive = currentDirection == 3,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 10.dp)
                )

                // 方向箭头动画
                SwipeArrowAnimation(
                    direction = currentDirection,
                    progress = cardSwipeProgress,
                    modifier = Modifier.fillMaxSize()
                )

                // 模拟照片卡片
                PhotoCardDemo(
                    offsetX = cardOffsetX,
                    offsetY = cardOffsetY,
                    rotation = cardRotation,
                    alpha = cardAlpha,
                    highlightColor = when (currentDirection) {
                        0, 1 -> KeepGreen
                        2 -> TrashRed
                        else -> MaybeAmber
                    },
                    modifier = Modifier.size(140.dp, 180.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 底部说明
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SwipeHintRow(
                    directions = "← →",
                    action = "保留照片",
                    color = KeepGreen
                )
                SwipeHintRow(
                    directions = "↑",
                    action = "删除照片",
                    color = TrashRed
                )
                SwipeHintRow(
                    directions = "↓",
                    action = "稍后再看",
                    color = MaybeAmber
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 开始按钮
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
            ) {
                Text(
                    text = "开始整理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1a1a2e)
                )
            }
        }
    }
}

/**
 * 方向指示气泡
 */
@Composable
private fun DirectionIndicatorBubble(
    icon: ImageVector,
    label: String,
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.4f,
        animationSpec = tween(300),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = if (isActive) 0.2f else 0.1f),
            border = if (isActive) BorderStroke(2.dp, color) else null,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) color else Color.White.copy(alpha = 0.5f)
        )
    }
}

/**
 * 滑动箭头动画
 */
@Composable
private fun SwipeArrowAnimation(
    direction: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val arrowAlpha = if (progress < 0.7f) progress / 0.7f else (1f - progress) / 0.3f

    Box(modifier = modifier) {
        when (direction) {
            0 -> { // 右
                repeat(3) { index ->
                    val delay = index * 0.15f
                    val adjustedProgress = ((progress - delay).coerceIn(0f, 1f) * 1.5f).coerceAtMost(1f)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = KeepGreen.copy(alpha = arrowAlpha * (1f - index * 0.2f)),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = (40 * adjustedProgress - 60 + index * 15).dp)
                            .size(32.dp)
                    )
                }
            }
            1 -> { // 左
                repeat(3) { index ->
                    val delay = index * 0.15f
                    val adjustedProgress = ((progress - delay).coerceIn(0f, 1f) * 1.5f).coerceAtMost(1f)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = KeepGreen.copy(alpha = arrowAlpha * (1f - index * 0.2f)),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (-40 * adjustedProgress + 60 - index * 15).dp)
                            .size(32.dp)
                    )
                }
            }
            2 -> { // 上
                repeat(3) { index ->
                    val delay = index * 0.15f
                    val adjustedProgress = ((progress - delay).coerceIn(0f, 1f) * 1.5f).coerceAtMost(1f)
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = TrashRed.copy(alpha = arrowAlpha * (1f - index * 0.2f)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-40 * adjustedProgress + 50 - index * 12).dp)
                            .size(32.dp)
                    )
                }
            }
            3 -> { // 下
                repeat(3) { index ->
                    val delay = index * 0.15f
                    val adjustedProgress = ((progress - delay).coerceIn(0f, 1f) * 1.5f).coerceAtMost(1f)
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaybeAmber.copy(alpha = arrowAlpha * (1f - index * 0.2f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (40 * adjustedProgress - 50 + index * 12).dp)
                            .size(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * 模拟照片卡片
 */
@Composable
private fun PhotoCardDemo(
    offsetX: Float,
    offsetY: Float,
    rotation: Float,
    alpha: Float,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2a2a3e),
        border = BorderStroke(
            width = 2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    highlightColor.copy(alpha = alpha * 0.8f),
                    highlightColor.copy(alpha = alpha * 0.3f)
                )
            )
        ),
        shadowElevation = 8.dp,
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .graphicsLayer {
                rotationZ = rotation
                this.alpha = alpha
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // 模拟照片图标
            Icon(
                imageVector = Icons.Default.Photo,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/**
 * 滑动提示行
 */
@Composable
private fun SwipeHintRow(
    directions: String,
    action: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = directions,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = action,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

// ==================== 双指缩放引导 ====================

/**
 * 双指缩放新手引导 (REQ-067)
 *
 * 首次使用照片网格时显示，教用户双指缩放切换列数的操作。
 *
 * @param onDismiss 关闭引导回调
 */
@Composable
fun PinchZoomOnboarding(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pinch")
    val pinchProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pinchProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f0f23)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* 防止点击穿透 */ }
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            PinchGestureAnimation(
                progress = pinchProgress,
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "双指缩放",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "双指张开放大，收拢缩小",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "可在 2-5 列之间切换",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(52.dp)
            ) {
                Text(
                    text = "知道了",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1a1a2e)
                )
            }
        }
    }
}

/**
 * 双指缩放手势动画
 */
@Composable
private fun PinchGestureAnimation(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val baseDistance = 35f
    val maxDistance = 70f
    val currentDistance = baseDistance + (maxDistance - baseDistance) * progress
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 背景圆环
        Surface(
            modifier = Modifier.size(160.dp),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.1f))
        ) {}

        // 内圈指示
        Surface(
            modifier = Modifier.size(80.dp + (40.dp * progress)),
            shape = CircleShape,
            color = primaryColor.copy(alpha = 0.1f)
        ) {}

        Canvas(modifier = Modifier.size(160.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val finger1 = Offset(center.x - currentDistance, center.y - currentDistance * 0.4f)
            val finger2 = Offset(center.x + currentDistance, center.y + currentDistance * 0.4f)

            // 连接线
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = finger1,
                end = finger2,
                strokeWidth = 2.dp.toPx()
            )

            // 手指外圈
            drawCircle(color = Color.White, radius = 18.dp.toPx(), center = finger1)
            drawCircle(color = Color.White, radius = 18.dp.toPx(), center = finger2)

            // 手指内圈
            drawCircle(color = primaryColor, radius = 14.dp.toPx(), center = finger1)
            drawCircle(color = primaryColor, radius = 14.dp.toPx(), center = finger2)
        }

        // 列数指示
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${(5 - progress * 3).toInt()}列",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// ==================== 引导覆盖层容器 ====================

/**
 * 新手引导覆盖层容器
 */
@Composable
fun OnboardingOverlay(
    showPinchZoomGuide: Boolean = false,
    showSwipeSortGuide: Boolean = false,
    onPinchZoomGuideDismiss: () -> Unit = {},
    onSwipeSortGuideDismiss: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = showPinchZoomGuide,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            PinchZoomOnboarding(onDismiss = onPinchZoomGuideDismiss)
        }

        AnimatedVisibility(
            visible = showSwipeSortGuide,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            SwipeSortOnboarding(onDismiss = onSwipeSortGuideDismiss)
        }
    }
}
