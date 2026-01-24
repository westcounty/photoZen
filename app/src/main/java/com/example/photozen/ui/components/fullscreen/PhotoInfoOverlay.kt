package com.example.photozen.ui.components.fullscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.PhotoEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PhotoZen 图片信息悬浮层 (REQ-013)
 * ==================================
 *
 * 显示内容:
 * 1. 文件名
 * 2. 拍摄时间（年-月-日 时:分:秒）
 * 3. 地理位置（省份+城市）- 如果有
 * 4. 分辨率（宽×高）
 * 5. 文件大小（MB）
 *
 * 位置: 左上角，避开系统状态栏
 *
 * @param photo 照片实体
 * @param modifier Modifier
 */
@Composable
fun PhotoInfoOverlay(
    photo: PhotoEntity,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. 文件名
        Text(
            text = photo.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 2. 拍摄时间
        val dateTime = photo.dateTaken ?: photo.dateAdded * 1000 // dateAdded is in seconds
        InfoRow(
            icon = Icons.Default.Schedule,
            text = formatDateTime(dateTime)
        )

        // 3. 地理位置 (如果有)
        // PhotoEntity 中可能没有 location 字段，这里预留
        // photo.location?.let { location ->
        //     InfoRow(
        //         icon = Icons.Default.LocationOn,
        //         text = "${location.province} ${location.city}"
        //     )
        // }

        // 4. 分辨率
        if (photo.width > 0 && photo.height > 0) {
            InfoRow(
                icon = Icons.Default.AspectRatio,
                text = "${photo.width} × ${photo.height}"
            )
        }

        // 5. 文件大小
        InfoRow(
            icon = Icons.Default.Storage,
            text = formatFileSize(photo.size)
        )
    }
}

/**
 * 信息行组件 - 图标 + 文字
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/**
 * 格式化日期时间
 * 格式: yyyy-MM-dd HH:mm:ss
 */
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 格式化文件大小
 * @param bytes 字节数
 * @return 格式化的字符串 (如 "2.35 MB")
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
        else -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
