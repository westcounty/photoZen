package com.example.photozen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.KeepGreen

/**
 * 统一的"开始整理"按钮组件 (REQ-062)
 *
 * 用于: 相册列表视图、时间线分组
 *
 * 状态:
 * - 未开始: 显示进度和整理按钮
 * - 进行中: 显示进度环和已整理数量
 * - 已完成: 绿色勾号和"已完成"文字
 *
 * @param totalCount 总照片数
 * @param sortedCount 已整理照片数
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 */
@Composable
fun StartSortingButton(
    totalCount: Int,
    sortedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val progress = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
    val isComplete = sortedCount >= totalCount && totalCount > 0
    val hasUnsorted = sortedCount < totalCount

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && hasUnsorted,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isComplete)
                KeepGreen.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface,
            disabledContainerColor = if (isComplete)
                KeepGreen.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isComplete) 
                KeepGreen 
            else if (hasUnsorted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isComplete) {
                // 已完成状态
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = KeepGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "已完成",
                    color = KeepGreen
                )
            } else if (hasUnsorted) {
                // 有待整理的照片
                // 小进度指示器
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "$sortedCount/$totalCount",
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                // 没有照片
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("整理")
            }
        }
    }
}

/**
 * 紧凑版开始整理按钮
 *
 * 仅显示图标，适用于空间有限的场景
 *
 * @param totalCount 总照片数
 * @param sortedCount 已整理照片数
 * @param onClick 点击回调
 * @param modifier Modifier
 */
@Composable
fun CompactSortingButton(
    totalCount: Int,
    sortedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isComplete = sortedCount >= totalCount && totalCount > 0
    val hasUnsorted = sortedCount < totalCount

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = hasUnsorted,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isComplete)
                KeepGreen.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isComplete) KeepGreen else MaterialTheme.colorScheme.primary
        )
    ) {
        if (isComplete) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "已完成",
                tint = KeepGreen,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "开始整理",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
