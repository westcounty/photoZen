package com.example.photozen.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 优化后的筛选进度标题 (REQ-064)
 *
 * 设计要点:
 * - 来源显示在上方（小字）
 * - 进度显示在下方（主标题）
 * - 限制最大宽度避免与右侧按钮冲突
 * - 单行显示避免换行
 *
 * @param source 来源名称 ("今日任务" / 相册名 / 时间线分组名)
 * @param currentCount 当前进度
 * @param totalCount 总数
 * @param modifier Modifier
 */
@Composable
fun FlowSorterTitle(
    source: String?,
    currentCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.widthIn(max = 160.dp)) {
        // 来源 - 小字（可选）
        if (!source.isNullOrBlank()) {
            Text(
                text = source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 进度 - 主标题
        Text(
            text = "$currentCount / $totalCount",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 每日任务专用标题
 *
 * @param currentCount 当前完成数
 * @param targetCount 目标数
 * @param modifier Modifier
 */
@Composable
fun DailyTaskTitle(
    currentCount: Int,
    targetCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.widthIn(max = 160.dp)) {
        Text(
            text = "今日目标",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = "$currentCount / $targetCount",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 选择模式标题
 *
 * @param selectedCount 已选数量
 * @param modifier Modifier
 */
@Composable
fun SelectionModeTitle(
    selectedCount: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "已选择 $selectedCount 张",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
    )
}
