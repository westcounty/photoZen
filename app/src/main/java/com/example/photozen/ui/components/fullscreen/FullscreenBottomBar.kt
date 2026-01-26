package com.example.photozen.ui.components.fullscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.PhotoEntity

/**
 * 全屏预览操作类型 (REQ-015)
 */
enum class FullscreenActionType {
    /** 复制 - 保留EXIF */
    COPY,
    /** 用其他app打开 */
    OPEN_WITH,
    /** PhotoZen内置编辑 */
    EDIT,
    /** 分享到其他App */
    SHARE,
    /** 彻底删除 - 系统删除，需二次确认 */
    DELETE
}

/**
 * PhotoZen 全屏预览底部操作栏 (REQ-015)
 * =====================================
 *
 * 默认展示5个操作:
 * 1. 复制（保留EXIF）
 * 2. 用其他app打开
 * 3. 编辑（PhotoZen内置）
 * 4. 分享到其他App
 * 5. 彻底删除（系统删除，需二次确认）
 *
 * @param photo 当前照片
 * @param onAction 操作回调
 * @param modifier Modifier
 */
@Composable
fun FullscreenBottomBar(
    photo: PhotoEntity,
    onAction: (FullscreenActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 1. 复制
        ActionButton(
            icon = Icons.Default.ContentCopy,
            label = "复制",
            onClick = { onAction(FullscreenActionType.COPY) }
        )

        // 2. 打开
        ActionButton(
            icon = Icons.Default.OpenInNew,
            label = "打开",
            onClick = { onAction(FullscreenActionType.OPEN_WITH) }
        )

        // 3. 编辑
        ActionButton(
            icon = Icons.Default.Edit,
            label = "编辑",
            onClick = { onAction(FullscreenActionType.EDIT) }
        )

        // 4. 分享
        ActionButton(
            icon = Icons.Default.Share,
            label = "分享",
            onClick = { onAction(FullscreenActionType.SHARE) }
        )

        // 5. 删除 - 直接触发，由外部处理确认逻辑
        ActionButton(
            icon = Icons.Default.Delete,
            label = "删除",
            tint = MaterialTheme.colorScheme.error,
            onClick = { onAction(FullscreenActionType.DELETE) }
        )
    }
}

/**
 * 操作按钮
 */
@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}
