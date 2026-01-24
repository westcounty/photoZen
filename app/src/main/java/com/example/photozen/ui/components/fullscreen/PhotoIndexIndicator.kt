package com.example.photozen.ui.components.fullscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * PhotoZen 照片序号指示器 (REQ-016)
 * ==================================
 *
 * 功能: 始终显示当前照片序号和总数
 * 位置: 顶部居中
 * 格式: "当前序号 / 总数"
 *
 * @param current 当前照片序号 (从1开始)
 * @param total 照片总数
 * @param modifier Modifier
 */
@Composable
fun PhotoIndexIndicator(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$current / $total",
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
