package com.example.photozen.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

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
            Text(
                text = formatNumber(totalSorted),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
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
            Text(
                text = formatNumber(count),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
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
        Text(
            text = formatNumber(count),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
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
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaybeAmber,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "连续整理 $days 天",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
 */
@Composable
fun MiniStatsCard(
    totalSorted: Int,
    weekSorted: Int,
    consecutiveDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onClick
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
                Text(
                    text = formatNumber(totalSorted),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
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
            )
            
            // 本周整理
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatNumber(weekSorted),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
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
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaybeAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$consecutiveDays",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaybeAmber
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
