package com.example.photozen.ui.components.fullscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.PicZenTokens

/**
 * PhotoZen 照片序号指示器 (REQ-016, DES-025)
 * ==========================================
 *
 * 功能: 始终显示当前照片序号和总数
 * 位置: 顶部居中
 * 格式: "当前序号 / 总数"
 *
 * DES-025 增强:
 * - 渐变背景增加层次感
 * - 微阴影提升浮动感
 * - 突出显示当前序号
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
    Row(
        modifier = modifier
            .shadow(
                elevation = PicZenTokens.Elevation.Level2,
                shape = RoundedCornerShape(PicZenTokens.Radius.Full),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Black.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(PicZenTokens.Radius.Full)
            )
            .padding(horizontal = PicZenTokens.Spacing.L, vertical = PicZenTokens.Spacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // DES-025: 突出显示当前序号
        Text(
            text = current.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(PicZenTokens.Spacing.XS))
        Text(
            text = "/",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(PicZenTokens.Spacing.XS))
        Text(
            text = total.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/**
 * 紧凑型序号指示器
 * 用于空间有限的场景
 */
@Composable
fun CompactPhotoIndexIndicator(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$current/$total",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(PicZenTokens.Radius.S)
            )
            .padding(horizontal = PicZenTokens.Spacing.S, vertical = PicZenTokens.Spacing.XS)
    )
}
