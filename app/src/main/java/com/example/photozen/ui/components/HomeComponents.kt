package com.example.photozen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import com.example.photozen.ui.theme.PicZenTokens

/**
 * 首页新组件集合
 * 
 * Phase 1-D 首页重构所需的 UI 组件，包括：
 * - [HomeDesignTokens] - 设计规范参数
 * - [HomeMainAction] - 主操作区
 * - [HomeQuickActions] - 快捷入口区
 * - [HomeDailyTask] - 每日任务卡片
 * 
 * ## 设计原则
 * 
 * 1. **分层卡片**: 主操作区 → 快捷入口 → 每日任务
 * 2. **颜色语义**: 绿色=保留, 红色=删除, 黄色=待定
 * 3. **可折叠**: 每日任务默认展开（未完成）或折叠（已完成）
 * 
 * @since Phase 1-A (骨架) → Phase 1-D (启用)
 */

// ==================== 设计 Token ====================

/**
 * 首页设计规范参数 (DES-023: 使用统一 Token)
 *
 * 定义首页组件的尺寸、间距等设计常量。
 * 与 PicZenTokens 保持一致。
 *
 * @see [INTERACTION_SPEC.md] 设计规范文档
 */
object HomeDesignTokens {
    /** 卡片圆角半径 - 使用 XL Token */
    val CardCornerRadius: Dp = PicZenTokens.Radius.XL

    /** 卡片内边距 - 使用 XL Token */
    val CardPadding: Dp = PicZenTokens.Spacing.XL

    /** 组件间距 - 使用 L Token */
    val SectionSpacing: Dp = PicZenTokens.Spacing.L

    /** 快捷入口图标容器尺寸 */
    val QuickActionIconSize: Dp = PicZenTokens.ComponentSize.QuickActionIcon

    /** 主操作按钮高度 */
    val MainActionButtonHeight: Dp = PicZenTokens.ComponentSize.MainButtonHeight

    /** DES-024: 主卡片阴影高度 */
    val MainCardElevation: Dp = PicZenTokens.Elevation.Level4

    /** DES-022: 主数字字体大小 (放大) */
    val MainNumberFontSize = 72.sp
}

// ==================== 主操作区 ====================

/**
 * 首页主操作区 - 显示待整理数量和开始按钮
 * 
 * 首页最核心的组件，突出显示待整理照片数量，
 * 提供一个显眼的"开始整理"按钮。
 * 
 * ## 设计规范
 * 
 * - 背景色: primaryContainer
 * - 数字样式: displayLarge, 粗体, primary 色
 * - 按钮: 全宽, 56dp 高, 16dp 圆角
 * 
 * ## 状态说明
 * 
 * - unsortedCount > 0: 显示待整理数量，按钮文字"开始整理"
 * - unsortedCount == 0: 显示"0"，文字"所有照片已整理完成"，按钮禁用显示"已完成"
 * 
 * @param unsortedCount 待整理照片数量
 * @param onStartClick 开始按钮点击回调
 * @param modifier Modifier
 * @param enabled 按钮是否可点击（默认 true）
 */
@Composable
fun HomeMainAction(
    unsortedCount: Int,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // DES-024: 主卡片阴影增强
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = HomeDesignTokens.MainCardElevation,
                shape = RoundedCornerShape(HomeDesignTokens.CardCornerRadius),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(HomeDesignTokens.CardCornerRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HomeDesignTokens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // DES-022: 待整理数量 - 放大字号
            Text(
                text = unsortedCount.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = HomeDesignTokens.MainNumberFontSize
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (unsortedCount > 0) "张照片待整理" else "所有照片已整理完成",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 主按钮
            Button(
                onClick = onStartClick,
                enabled = enabled && unsortedCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeDesignTokens.MainActionButtonHeight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (unsortedCount > 0) "开始整理" else "已完成",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// ==================== 快捷入口区 ====================

/**
 * 首页快捷入口区 - 显示快捷操作入口
 *
 * 提供已保留、对比、回收站等快捷入口。
 *
 * ## 设计说明
 *
 * 启用底部导航后，时间线和相册入口将移除（由底部导航提供），
 * 保留已保留、对比和回收站三个入口。
 *
 * ## 禁用状态
 *
 * - 已保留按钮：当 keepCount == 0 时禁用（半透明显示）
 * - 对比按钮：当 maybeCount == 0 时禁用（半透明显示）
 * - 回收站按钮：始终可点击
 *
 * @param onKeepClick 已保留入口点击回调
 * @param onCompareClick 对比入口点击回调
 * @param onTrashClick 回收站入口点击回调
 * @param keepCount 已保留照片数量（用于显示角标，为0时禁用按钮）
 * @param maybeCount 待定照片数量（用于显示角标，为0时禁用对比按钮）
 * @param trashCount 回收站照片数量（用于显示角标）
 * @param modifier Modifier
 */
@Composable
fun HomeQuickActions(
    onKeepClick: () -> Unit = {},
    onCompareClick: () -> Unit,
    onTrashClick: () -> Unit,
    keepCount: Int = 0,
    maybeCount: Int = 0,
    trashCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionItem(
            icon = Icons.Default.Favorite,
            label = "已保留",
            badge = if (keepCount > 0) keepCount.toString() else null,
            onClick = onKeepClick,
            tint = KeepGreen,
            enabled = keepCount > 0
        )
        QuickActionItem(
            icon = Icons.AutoMirrored.Filled.CompareArrows,
            label = "对比",
            badge = if (maybeCount > 0) maybeCount.toString() else null,
            onClick = onCompareClick,
            tint = MaybeAmber,
            enabled = maybeCount > 0
        )
        QuickActionItem(
            icon = Icons.Default.Delete,
            label = "回收站",
            badge = if (trashCount > 0) trashCount.toString() else null,
            onClick = onTrashClick,
            tint = TrashRed,
            enabled = true
        )
    }
}

/**
 * 快捷入口单项
 * 
 * @param icon 图标
 * @param label 标签文字
 * @param onClick 点击回调
 * @param tint 图标和角标颜色
 * @param badge 角标文字（null 则不显示）
 * @param enabled 是否可点击（禁用时显示半透明）
 */
@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color,
    badge: String? = null,
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.5f
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp)
            .alpha(alpha)
    ) {
        Box {
            // 图标容器
            Box(
                modifier = Modifier
                    .size(HomeDesignTokens.QuickActionIconSize)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = label, 
                    tint = tint
                )
            }
            
            // Badge 角标
            if (badge != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp),
                    shape = CircleShape,
                    color = tint
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== 每日任务区 ====================

/**
 * 每日任务显示状态数据类
 * 
 * 封装每日任务的展示所需数据。
 * 
 * @property current 当前已完成数量
 * @property target 目标数量
 * @property isEnabled 是否启用每日任务功能
 * @property isCompleted 是否已完成今日任务
 */
data class DailyTaskDisplayStatus(
    val current: Int,
    val target: Int,
    val isEnabled: Boolean,
    val isCompleted: Boolean
) {
    /**
     * 计算进度百分比
     * 
     * @return 0.0 ~ 1.0 之间的进度值
     */
    val progress: Float
        get() = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
}

/**
 * 首页每日任务卡片 - 可折叠
 * 
 * 显示每日任务进度，支持折叠/展开。
 * 
 * ## 折叠规则
 * 
 * - 任务未完成: 默认展开
 * - 任务已完成: 默认折叠
 * - 用户可点击头部切换折叠状态
 * - 当任务完成状态变化时，自动重置展开状态
 * 
 * ## 设计规范
 * 
 * - 已完成背景: KeepGreen 10% 透明度
 * - 进度条: 6dp 高, 3dp 圆角
 * - 展开/折叠动画: expandVertically + fadeIn/fadeOut
 * 
 * @param status 每日任务状态
 * @param onStartClick 继续任务按钮点击回调
 * @param modifier Modifier
 * @param defaultExpanded 默认展开状态（默认根据完成状态决定）
 */
@Composable
fun HomeDailyTask(
    status: DailyTaskDisplayStatus,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    defaultExpanded: Boolean = !status.isCompleted
) {
    // 折叠状态，未完成时默认展开
    var expanded by rememberSaveable { mutableStateOf(defaultExpanded) }
    
    // 当状态变化时，重置展开状态
    LaunchedEffect(status.isCompleted) {
        expanded = !status.isCompleted
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isCompleted) 
                KeepGreen.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 头部 - 始终显示，可点击切换折叠
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：图标 + 标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (status.isCompleted) 
                            Icons.Default.Check 
                        else 
                            Icons.Default.Assignment,
                        contentDescription = null,
                        tint = if (status.isCompleted) KeepGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (status.isCompleted) "今日任务已完成" else "每日任务",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 右侧：进度 + 展开/折叠图标
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${status.current}/${status.target}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (status.isCompleted) KeepGreen else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (expanded) 
                            Icons.Default.ExpandLess 
                        else 
                            Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "折叠" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 展开内容
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                ) {
                    // 进度条
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (status.isCompleted) KeepGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    // 继续任务按钮（仅未完成时显示）
                    if (!status.isCompleted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = onStartClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("继续任务")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "明天继续保持！",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeepGreen
                        )
                    }
                }
            }
        }
    }
}

/**
 * 首页核心操作卡片 - 整合每日任务和整理全部入口
 *
 * 这是首页的核心功能区，包含：
 * - 每日任务进度（如果启用）
 * - 两个操作按钮：继续任务（主）+ 整理全部（次）
 * - 如果每日任务未启用，只显示整理全部按钮
 *
 * @param dailyTaskStatus 每日任务状态（可为null表示未启用或加载中）
 * @param unsortedCount 待整理照片数量
 * @param onStartDailyTask 开始/继续每日任务回调
 * @param onStartSortAll 整理全部回调
 * @param modifier Modifier
 */
@Composable
fun HomeDailyTaskWithSortAll(
    dailyTaskStatus: com.example.photozen.domain.usecase.DailyTaskStatus?,
    unsortedCount: Int,
    onStartDailyTask: () -> Unit,
    onStartSortAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDailyTaskEnabled = dailyTaskStatus?.isEnabled == true
    val isCompleted = dailyTaskStatus?.isCompleted == true

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                KeepGreen.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 每日任务进度区（仅当启用时显示）
            if (isDailyTaskEnabled && dailyTaskStatus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：图标 + 标题
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isCompleted)
                                Icons.Default.Check
                            else
                                Icons.Default.Assignment,
                            contentDescription = null,
                            tint = if (isCompleted) KeepGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCompleted) "今日任务已完成" else "每日任务",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 右侧：进度数字
                    Text(
                        text = "${dailyTaskStatus.current}/${dailyTaskStatus.target}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) KeepGreen else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 进度条
                LinearProgressIndicator(
                    progress = { dailyTaskStatus.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isCompleted) KeepGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 待整理照片数量提示 + 预计完成天数
            if (unsortedCount > 0) {
                // 计算预计完成天数（仅在每日任务启用时显示）
                val daysRemaining = if (isDailyTaskEnabled && dailyTaskStatus != null && dailyTaskStatus.target > 0) {
                    val remaining = unsortedCount - (if (isCompleted) 0 else dailyTaskStatus.current)
                    kotlin.math.ceil(remaining.toFloat() / dailyTaskStatus.target).toInt().coerceAtLeast(0)
                } else null

                val daysText = when {
                    daysRemaining == null -> ""
                    daysRemaining <= 0 -> "，今天可完成"
                    daysRemaining == 1 -> "，约 1 天可完成"
                    else -> "，约 $daysRemaining 天可完成"
                }

                Text(
                    text = "${unsortedCount} 张照片待整理$daysText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 操作按钮区
            if (isDailyTaskEnabled && !isCompleted) {
                // 每日任务启用且未完成：显示两个按钮
                // 主按钮：整理全部，次按钮：继续任务
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 主按钮：整理全部
                    Button(
                        onClick = onStartSortAll,
                        modifier = Modifier.weight(1f),
                        enabled = unsortedCount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("整理全部")
                    }

                    // 次按钮：继续任务
                    FilledTonalButton(
                        onClick = onStartDailyTask,
                        modifier = Modifier.weight(1f),
                        enabled = unsortedCount > 0
                    ) {
                        Text("继续任务")
                    }
                }
            } else if (isCompleted) {
                // 每日任务已完成：只显示整理全部按钮
                Text(
                    text = "今日目标已达成！继续整理可以累积更多成就。",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeepGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onStartSortAll,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = unsortedCount > 0
                ) {
                    Text("继续整理全部")
                }
            } else {
                // 每日任务未启用：只显示整理全部按钮
                Button(
                    onClick = onStartSortAll,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = unsortedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("开始整理")
                }
            }
        }
    }
}

/**
 * 分享功能提示卡片
 *
 * 提示用户可以从系统相册分享照片到 PhotoZen 进行复制或对比。
 * 用户关闭后不再显示。
 *
 * @param onDismiss 关闭回调
 * @param modifier Modifier
 */
@Composable
fun ShareFeatureTipCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 分享图标
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 文案区域
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "从相册分享照片到 PhotoZen",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "在系统相册选择照片 → 点击分享 → 选择「用 PhotoZen 复制」或「用 PhotoZen 对比」",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }

            // 关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭提示",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
