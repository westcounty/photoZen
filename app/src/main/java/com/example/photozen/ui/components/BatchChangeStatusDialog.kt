package com.example.photozen.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * 批量修改筛选状态弹窗 (REQ-048, REQ-055)
 *
 * 用于相册照片列表和时间线照片列表中批量修改选中照片的状态
 *
 * 选项:
 * - 标记为保留 (KEEP)
 * - 设置为待定 (MAYBE)
 * - 移至回收站 (TRASH)
 * - 重置为未筛选 (UNSORTED)
 *
 * @param selectedCount 选中的照片数量
 * @param onStatusSelected 状态选中回调
 * @param onDismiss 关闭弹窗回调
 */
@Composable
fun BatchChangeStatusDialog(
    selectedCount: Int,
    onStatusSelected: (PhotoStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改筛选状态") },
        text = {
            Column {
                Text(
                    text = "将选中的 $selectedCount 张照片改为:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                StatusOptionItem(
                    icon = Icons.Default.CheckCircle,
                    label = "标记为保留",
                    color = KeepGreen,
                    onClick = { onStatusSelected(PhotoStatus.KEEP) }
                )
                StatusOptionItem(
                    icon = Icons.Default.QuestionMark,
                    label = "设置为待定",
                    color = MaybeAmber,
                    onClick = { onStatusSelected(PhotoStatus.MAYBE) }
                )
                StatusOptionItem(
                    icon = Icons.Default.Delete,
                    label = "移至回收站",
                    color = TrashRed,
                    onClick = { onStatusSelected(PhotoStatus.TRASH) }
                )
                StatusOptionItem(
                    icon = Icons.Default.Refresh,
                    label = "重置为未筛选",
                    color = MaterialTheme.colorScheme.outline,
                    onClick = { onStatusSelected(PhotoStatus.UNSORTED) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun StatusOptionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
