package com.example.photozen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size

/**
 * 引导气泡箭头方向
 */
enum class ArrowDirection {
    UP,    // 箭头向上，气泡在下方
    DOWN,  // 箭头向下，气泡在上方
    LEFT,  // 箭头向左，气泡在右方
    RIGHT  // 箭头向右，气泡在左方
}

/**
 * 引导步骤信息
 * 
 * @property current 当前步骤 (从 1 开始)
 * @property total 总步骤数
 */
data class GuideStepInfo(
    val current: Int,
    val total: Int
)

/**
 * 引导气泡组件
 * 
 * 显示带遮罩的引导气泡，突出目标元素。
 * 
 * ## 设计规范
 * - 遮罩层: 60% 透明度黑色
 * - 气泡: primaryContainer 背景，12dp 圆角
 * - 最大宽度: 280dp
 * - 箭头: 指向目标元素
 * 
 * ## 使用方式
 * ```kotlin
 * GuideTooltip(
 *     visible = guideState.shouldShow,
 *     message = "👉 右滑保留\n喜欢的照片向右滑动",
 *     targetBounds = cardBounds,
 *     arrowDirection = ArrowDirection.UP,
 *     stepInfo = GuideStepInfo(1, 3),
 *     onDismiss = { guideState.dismiss() }
 * )
 * ```
 * 
 * @param visible 是否显示
 * @param message 引导文字
 * @param targetBounds 目标元素边界（用于定位气泡）
 * @param arrowDirection 箭头方向
 * @param stepInfo 步骤信息（多步引导时显示进度）
 * @param onDismiss 关闭回调
 * @param dismissText 关闭按钮文字
 * @param highlightPadding 高亮区域额外内边距
 */
@Composable
fun GuideTooltip(
    visible: Boolean,
    message: String,
    targetBounds: Rect?,
    arrowDirection: ArrowDirection = ArrowDirection.DOWN,
    stepInfo: GuideStepInfo? = null,
    onDismiss: () -> Unit,
    dismissText: String = "知道了",
    highlightPadding: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    // 获取屏幕尺寸（需要在 Popup 外部获取）
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    // 使用 Popup 确保遮罩覆盖整个屏幕
    if (visible && targetBounds != null) {
        Popup(
            onDismissRequest = onDismiss,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            // 使用明确的屏幕尺寸，而不是 fillMaxSize()
            // 因为 Popup 默认是 wrap content，fillMaxSize() 无法正常工作
            Box(
                modifier = Modifier
                    .size(screenWidthDp, screenHeightDp)
                    .pointerInput(Unit) {
                        detectTapGestures { onDismiss() }
                    }
            ) {
                // 遮罩层（带挖空效果）
                GuideOverlay(
                    targetBounds = targetBounds,
                    padding = highlightPadding
                )

                // 气泡
                GuideTooltipBubble(
                    message = message,
                    targetBounds = targetBounds,
                    arrowDirection = arrowDirection,
                    stepInfo = stepInfo,
                    onDismiss = onDismiss,
                    dismissText = dismissText
                )
            }
        }
    }
}

/**
 * 遮罩层组件
 * 
 * 使用半透明遮罩覆盖全屏，目标区域保持透明（挖空效果）
 */
@Composable
private fun GuideOverlay(
    targetBounds: Rect,
    padding: Dp = 8.dp
) {
    val density = LocalDensity.current
    val paddingPx = with(density) { padding.toPx() }
    
    // 扩展高亮区域
    val highlightBounds = Rect(
        left = (targetBounds.left - paddingPx).coerceAtLeast(0f),
        top = (targetBounds.top - paddingPx).coerceAtLeast(0f),
        right = targetBounds.right + paddingPx,
        bottom = targetBounds.bottom + paddingPx
    )
    
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        // 使用图层实现挖空效果
        drawRect(
            color = Color.Black.copy(alpha = 0.6f)
        )
        
        // 绘制挖空区域（圆角矩形）
        val cornerRadius = 12.dp.toPx()
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(highlightBounds.left, highlightBounds.top),
            size = Size(
                highlightBounds.width,
                highlightBounds.height
            ),
            cornerRadius = CornerRadius(cornerRadius),
            blendMode = BlendMode.Clear
        )
    }
}

/**
 * 气泡内容组件
 */
@Composable
private fun GuideTooltipBubble(
    message: String,
    targetBounds: Rect,
    arrowDirection: ArrowDirection,
    stepInfo: GuideStepInfo?,
    onDismiss: () -> Unit,
    dismissText: String
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // 计算气泡位置
    val tooltipOffset = remember(targetBounds, arrowDirection, screenWidth, screenHeight) {
        calculateTooltipOffset(targetBounds, arrowDirection, density, screenWidth, screenHeight)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = with(density) { tooltipOffset.x.toDp() }.coerceAtLeast(8.dp),
                    top = with(density) { tooltipOffset.y.toDp() }.coerceAtLeast(8.dp)
                )
                .widthIn(max = 280.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* 阻止点击穿透 */ },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 引导文字
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 底部：步骤指示 + 关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 步骤指示
                    if (stepInfo != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(stepInfo.total) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (index < stepInfo.current) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            }
                                        )
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    // 关闭按钮
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = dismissText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 计算气泡偏移量
 */
private fun calculateTooltipOffset(
    targetBounds: Rect,
    arrowDirection: ArrowDirection,
    density: androidx.compose.ui.unit.Density,
    screenWidth: Float,
    screenHeight: Float
): Offset {
    val spacing = with(density) { 16.dp.toPx() }
    val tooltipWidth = with(density) { 280.dp.toPx() }
    val tooltipHeight = with(density) { 120.dp.toPx() }
    val padding = with(density) { 16.dp.toPx() }
    
    return when (arrowDirection) {
        ArrowDirection.UP -> {
            // 气泡在目标下方
            val x = (targetBounds.center.x - tooltipWidth / 2)
                .coerceIn(padding, screenWidth - tooltipWidth - padding)
            val y = targetBounds.bottom + spacing
            Offset(x - padding, y - padding)
        }
        ArrowDirection.DOWN -> {
            // 气泡在目标上方
            val x = (targetBounds.center.x - tooltipWidth / 2)
                .coerceIn(padding, screenWidth - tooltipWidth - padding)
            val y = (targetBounds.top - spacing - tooltipHeight).coerceAtLeast(padding)
            Offset(x - padding, y - padding)
        }
        ArrowDirection.LEFT -> {
            // 气泡在目标右侧
            val x = targetBounds.right + spacing
            val y = (targetBounds.center.y - tooltipHeight / 2)
                .coerceIn(padding, screenHeight - tooltipHeight - padding)
            Offset(x - padding, y - padding)
        }
        ArrowDirection.RIGHT -> {
            // 气泡在目标左侧
            val x = (targetBounds.left - spacing - tooltipWidth).coerceAtLeast(padding)
            val y = (targetBounds.center.y - tooltipHeight / 2)
                .coerceIn(padding, screenHeight - tooltipHeight - padding)
            Offset(x - padding, y - padding)
        }
    }
}
