package com.example.photozen.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.theme.PicZenTokens
import com.example.photozen.ui.theme.TrashRed
import kotlin.math.roundToInt

// ==================== 动画组件 ====================

/**
 * 动画数字显示组件
 *
 * 数字从当前值平滑滚动到目标值，支持格式化显示。
 *
 * ## 动画规格
 * - 动画时长: Duration.Normal (200ms)
 * - 缓动曲线: EmphasizedDecelerate
 *
 * @param targetValue 目标数值
 * @param modifier Modifier
 * @param style 文字样式
 * @param color 文字颜色
 * @param formatNumber 是否格式化数字 (1.2k, 1.2w)
 */
@Composable
private fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    formatNumber: Boolean = true
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(
                durationMillis = PicZenMotion.Duration.Normal,
                easing = PicZenMotion.Easing.EmphasizedDecelerate
            )
        )
    }

    val displayValue = animatedValue.value.roundToInt()
    val displayText = if (formatNumber) formatNumber(displayValue) else displayValue.toString()

    Text(
        text = displayText,
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}

/**
 * 动画火焰图标组件
 *
 * 火焰图标持续摇曳动画，增添活力感。
 *
 * ## 动画规格
 * - 缩放脉冲: 1.0 → 1.15 → 1.0 (600ms循环)
 * - 轻微旋转: -5° → +5° (400ms循环)
 *
 * @param modifier Modifier
 * @param size 图标尺寸
 * @param tint 图标颜色
 */
@Composable
private fun AnimatedFlameIcon(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = MaybeAmber
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")

    // 缩放脉冲动画
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameScale"
    )

    // 轻微旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameRotation"
    )

    Icon(
        imageVector = Icons.Default.LocalFireDepartment,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
    )
}

// ==================== 统计卡片组件 ====================

/**
 * 总览统计卡片
 * 
 * 显示历史总整理数量，作为主要展示数据。
 */
@Composable
fun TotalStatsCard(
    totalSorted: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 使用动画数字显示
            AnimatedCounter(
                targetValue = totalSorted,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "张照片已整理",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 周/月统计卡片行
 * 
 * 并排显示本周和本月的整理数量。
 */
@Composable
fun PeriodStatsRow(
    weekSorted: Int,
    monthSorted: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 本周卡片
        PeriodCard(
            count = weekSorted,
            label = "本周整理",
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        
        // 本月卡片
        PeriodCard(
            count = monthSorted,
            label = "本月整理",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 单个周期统计卡片
 */
@Composable
private fun PeriodCard(
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 使用动画数字显示
            AnimatedCounter(
                targetValue = count,
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 本周分类统计卡片
 * 
 * 显示本周的保留/删除/待定分类明细。
 */
@Composable
fun WeekBreakdownCard(
    keptCount: Int,
    trashedCount: Int,
    maybeCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "本周分类明细",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BreakdownItem(count = keptCount, label = "保留", color = KeepGreen)
                BreakdownItem(count = trashedCount, label = "删除", color = TrashRed)
                BreakdownItem(count = maybeCount, label = "待定", color = MaybeAmber)
            }
        }
    }
}

/**
 * 分类明细项
 */
@Composable
private fun BreakdownItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 使用动画数字显示
        AnimatedCounter(
            targetValue = count,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 连续整理天数卡片
 *
 * 显示连续整理的天数，激励用户保持习惯。
 *
 * ## Enhanced Features
 * - 火焰图标摇曳动画
 * - 天数数字滚动动画
 */
@Composable
fun StreakCard(
    days: Int,
    modifier: Modifier = Modifier
) {
    if (days <= 0) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaybeAmber.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 使用动画火焰图标
            AnimatedFlameIcon(
                size = 32.dp,
                tint = MaybeAmber
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "连续整理 ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // 使用动画数字显示
                    AnimatedCounter(
                        targetValue = days,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaybeAmber,
                        formatNumber = false
                    )
                    Text(
                        text = " 天",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = getStreakMessage(days),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 根据连续天数获取激励文案
 */
private fun getStreakMessage(days: Int): String {
    return when {
        days >= 30 -> "太厉害了！坚持就是胜利！"
        days >= 14 -> "两周了！你是整理达人！"
        days >= 7 -> "一周连续！继续保持！"
        days >= 3 -> "三天连续！养成好习惯！"
        else -> "保持好习惯！"
    }
}

/**
 * 首页迷你统计卡片
 *
 * 用于首页入口，展示简要统计信息。
 *
 * ## Enhanced Features
 * - 按压缩放动画 (0.98f)
 * - 阴影动态变化 (Level2 → Level1)
 * - 数字滚动动画
 * - 火焰图标摇曳动画
 */
@Composable
fun MiniStatsCard(
    totalSorted: Int,
    weekSorted: Int,
    consecutiveDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按压缩放动画 - 0.98f for cards
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "miniStatsScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 总整理数
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedCounter(
                    targetValue = totalSorted,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "总整理",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 分隔线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // 本周整理
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedCounter(
                    targetValue = weekSorted,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "本周",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 连续天数（如果有）
            if (consecutiveDays > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 使用动画火焰图标
                    AnimatedFlameIcon(
                        size = 20.dp,
                        tint = MaybeAmber
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedCounter(
                            targetValue = consecutiveDays,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaybeAmber,
                            formatNumber = false
                        )
                        Text(
                            text = "连续",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 格式化数字显示
 * 
 * 大于 1000 时显示为 1.2k 格式
 */
private fun formatNumber(num: Int): String {
    return when {
        num >= 10000 -> String.format("%.1fw", num / 10000.0)
        num >= 1000 -> String.format("%.1fk", num / 1000.0)
        else -> num.toString()
    }
}
