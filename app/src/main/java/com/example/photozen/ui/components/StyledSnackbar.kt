package com.example.photozen.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.state.SnackbarEvent
import com.example.photozen.ui.state.SnackbarType
import com.example.photozen.ui.state.getType
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * 样式化 Snackbar 组件 (Phase 3-8)
 * 
 * 根据 SnackbarEvent 类型显示不同样式的 Snackbar：
 * - 普通：默认样式
 * - 成功：绿色主题
 * - 错误：红色主题
 * - 警告：黄色主题
 * 
 * @param snackbarData Snackbar 数据
 * @param snackbarEvent 当前的 Snackbar 事件（用于判断类型）
 * @param modifier Modifier
 */
@Composable
fun StyledSnackbar(
    snackbarData: SnackbarData,
    snackbarEvent: SnackbarEvent?,
    modifier: Modifier = Modifier
) {
    val type = snackbarEvent?.getType() ?: SnackbarType.DEFAULT
    val colors = getSnackbarColors(type)
    val icon = getSnackbarIcon(type)
    
    Snackbar(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = colors.containerColor,
        contentColor = colors.contentColor,
        actionContentColor = colors.actionColor,
        action = snackbarData.visuals.actionLabel?.let { actionLabel ->
            {
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.actionColor
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissAction = {
            TextButton(
                onClick = { snackbarData.dismiss() },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.contentColor.copy(alpha = 0.7f)
                )
            ) {
                Text("关闭")
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 类型图标
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 消息文本
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Snackbar 颜色配置
 */
data class SnackbarColors(
    val containerColor: Color,
    val contentColor: Color,
    val actionColor: Color,
    val iconColor: Color
)

/**
 * 根据类型获取 Snackbar 颜色
 */
@Composable
fun getSnackbarColors(type: SnackbarType): SnackbarColors {
    return when (type) {
        SnackbarType.SUCCESS -> SnackbarColors(
            containerColor = KeepGreen.copy(alpha = 0.95f),
            contentColor = Color.White,
            actionColor = Color.White,
            iconColor = Color.White
        )
        SnackbarType.ERROR -> SnackbarColors(
            containerColor = TrashRed.copy(alpha = 0.95f),
            contentColor = Color.White,
            actionColor = Color.White,
            iconColor = Color.White
        )
        SnackbarType.WARNING -> SnackbarColors(
            containerColor = MaybeAmber.copy(alpha = 0.95f),
            contentColor = Color.Black,
            actionColor = Color.Black,
            iconColor = Color.Black
        )
        SnackbarType.DEFAULT -> SnackbarColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary,
            iconColor = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}

/**
 * 根据类型获取 Snackbar 图标
 */
fun getSnackbarIcon(type: SnackbarType): ImageVector? {
    return when (type) {
        SnackbarType.SUCCESS -> Icons.Default.Check
        SnackbarType.ERROR -> Icons.Default.Error
        SnackbarType.WARNING -> Icons.Default.Warning
        SnackbarType.DEFAULT -> null // 普通消息不显示图标
    }
}

/**
 * 简单的成功 Snackbar
 */
@Composable
fun SuccessSnackbar(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Snackbar(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = KeepGreen.copy(alpha = 0.95f),
        contentColor = Color.White,
        actionContentColor = Color.White,
        action = actionLabel?.let {
            {
                TextButton(
                    onClick = { onAction?.invoke() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text(text = it, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissAction = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
            ) {
                Text("关闭")
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * 简单的错误 Snackbar
 */
@Composable
fun ErrorSnackbar(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = TrashRed.copy(alpha = 0.95f),
        contentColor = Color.White,
        dismissAction = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
            ) {
                Text("关闭")
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * 带进度的 Snackbar
 */
@Composable
fun ProgressSnackbar(
    message: String,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
