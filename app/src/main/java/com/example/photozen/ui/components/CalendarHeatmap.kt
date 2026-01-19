package com.example.photozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photozen.ui.theme.KeepGreen
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日历热力图组件 (Phase 3-2)
 * 
 * 类似 GitHub 贡献图的设计，展示整理活动热度。
 * 颜色深浅表示当日整理数量。
 * 
 * ## 性能优化
 * - 使用 `remember` 缓存日期计算结果
 * - `LazyRow` 配合 `key` 实现高效复用
 * - 最大值计算使用 `remember(data)` 避免重复计算
 * 
 * @param data 日期到数量的映射，格式 "YYYY-MM-DD" -> count
 * @param weeks 显示的周数（默认12周，约3个月）
 * @param onDayClick 日期点击回调
 */
@Composable
fun CalendarHeatmap(
    data: Map<String, Int>,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
    onDayClick: (date: String, count: Int) -> Unit = { _, _ -> }
) {
    val colorScheme = MaterialTheme.colorScheme
    val maxCount = remember(data) { data.values.maxOrNull()?.coerceAtLeast(1) ?: 1 }
    val weekDates = remember(weeks) { generateWeekDates(weeks) }
    val visibleMonths = remember(weeks) { getVisibleMonths(weeks) }
    
    Column(modifier = modifier) {
        // 月份标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            visibleMonths.forEach { month ->
                Text(
                    text = month,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth()) {
            // 星期标签
            Column(
                modifier = Modifier.width(28.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val weekLabels = listOf("", "一", "", "三", "", "五", "")
                weekLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (label.isNotEmpty()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 热力图网格
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(weekDates, key = { it.firstOrNull() ?: "" }) { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        week.forEach { date ->
                            val count = data[date] ?: 0
                            HeatmapCell(
                                count = count,
                                maxCount = maxCount,
                                date = date,
                                colorScheme = colorScheme,
                                onClick = { onDayClick(date, count) }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 图例
        HeatmapLegend(colorScheme = colorScheme)
    }
}

/**
 * 热力图单元格
 */
@Composable
private fun HeatmapCell(
    count: Int,
    maxCount: Int,
    date: String,
    colorScheme: ColorScheme,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val isToday = remember(date) { 
        date == dateFormat.format(Date()) 
    }
    
    val level = when {
        count <= 0 -> 0
        maxCount <= 0 -> 0
        else -> ((count.toFloat() / maxCount) * 4).toInt().coerceIn(1, 4)
    }
    
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(getHeatmapColor(level, colorScheme))
            .then(
                if (isToday) Modifier.border(
                    width = 1.dp,
                    color = colorScheme.primary,
                    shape = RoundedCornerShape(2.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
    )
}

/**
 * 热力图图例
 */
@Composable
private fun HeatmapLegend(colorScheme: ColorScheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "少",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        (0..4).forEach { level ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(getHeatmapColor(level, colorScheme))
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(
            text = "多",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 根据等级获取热力图颜色
 */
private fun getHeatmapColor(level: Int, colorScheme: ColorScheme): Color {
    return when (level) {
        0 -> colorScheme.surfaceVariant.copy(alpha = 0.5f)
        1 -> KeepGreen.copy(alpha = 0.25f)
        2 -> KeepGreen.copy(alpha = 0.5f)
        3 -> KeepGreen.copy(alpha = 0.75f)
        else -> KeepGreen
    }
}

/**
 * 生成最近 N 周的日期列表
 * 
 * @return List<List<String>>，外层是周（从旧到新），内层是该周的 7 天（周日到周六）
 */
private fun generateWeekDates(weeks: Int): List<List<String>> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    // 先移动到今天所在周的周六
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysUntilSaturday = Calendar.SATURDAY - dayOfWeek
    if (daysUntilSaturday > 0) {
        calendar.add(Calendar.DAY_OF_YEAR, daysUntilSaturday)
    }
    
    val result = mutableListOf<List<String>>()
    
    repeat(weeks) {
        val week = mutableListOf<String>()
        // 从周六往前到周日
        val endOfWeek = calendar.clone() as Calendar
        repeat(7) { dayIndex ->
            val dayCalendar = endOfWeek.clone() as Calendar
            dayCalendar.add(Calendar.DAY_OF_YEAR, -(6 - dayIndex))
            week.add(dateFormat.format(dayCalendar.time))
        }
        result.add(0, week)
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
    }
    
    return result
}

/**
 * 获取可见的月份标签
 */
private fun getVisibleMonths(weeks: Int): List<String> {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.WEEK_OF_YEAR, -weeks + 1)
    
    val months = mutableListOf<String>()
    val monthFormat = SimpleDateFormat("M月", Locale.getDefault())
    var lastMonth = -1
    
    repeat(weeks) {
        val currentMonth = calendar.get(Calendar.MONTH)
        if (currentMonth != lastMonth) {
            months.add(monthFormat.format(calendar.time))
            lastMonth = currentMonth
        }
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
    }
    
    // 最多显示4个月份标签
    return if (months.size > 4) months.takeLast(4) else months
}
