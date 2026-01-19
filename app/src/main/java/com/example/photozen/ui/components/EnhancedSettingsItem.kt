package com.example.photozen.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 增强的设置项组件
 * 
 * 提供统一的设置项视觉样式，支持图标、标题、副标题和自定义尾部内容。
 * 
 * @param icon 左侧图标
 * @param title 标题
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param subtitle 副标题（可选）
 * @param trailing 右侧自定义内容（默认为箭头图标）
 * @param enabled 是否可用
 * @param iconTint 图标颜色（可选，默认使用主题色）
 */
@Composable
fun EnhancedSettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    iconTint: Color? = null
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    val resolvedIconTint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
    
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedIconTint.copy(alpha = contentAlpha)
            )
        },
        headlineContent = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
        },
        supportingContent = subtitle?.let { {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        } },
        trailingContent = trailing ?: {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f * contentAlpha)
            )
        }
    )
}

/**
 * 带 Switch 开关的设置项
 * 
 * 用于布尔值设置，如开启/关闭功能。
 */
@Composable
fun SwitchSettingsItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    iconTint: Color? = null
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    val resolvedIconTint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
    
    ListItem(
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedIconTint.copy(alpha = contentAlpha)
            )
        },
        headlineContent = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
        },
        supportingContent = subtitle?.let { {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

/**
 * 带值显示的设置项
 * 
 * 用于显示当前设置值，如 "语言: 中文"。
 */
@Composable
fun ValueSettingsItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    iconTint: Color? = null
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    val resolvedIconTint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
    
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedIconTint.copy(alpha = contentAlpha)
            )
        },
        headlineContent = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
        },
        supportingContent = subtitle?.let { {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f * contentAlpha)
                )
            }
        }
    )
}

/**
 * 带 Slider 的设置项
 * 
 * 用于数值范围设置，如音量、亮度。
 */
@Composable
fun SliderSettingsItem(
    icon: ImageVector,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    valueLabel: String? = null,
    enabled: Boolean = true,
    iconTint: Color? = null
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    val resolvedIconTint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedIconTint.copy(alpha = contentAlpha),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
            }
            valueLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 带单选按钮组的设置项
 * 
 * 用于在多个选项中选择一个。
 */
@Composable
fun <T> RadioSettingsItem(
    icon: ImageVector,
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconTint: Color? = null
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    val resolvedIconTint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedIconTint.copy(alpha = contentAlpha),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onOptionSelected(value) }
                    .padding(vertical = 8.dp, horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOption == value,
                    onClick = { onOptionSelected(value) },
                    enabled = enabled
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
            }
        }
    }
}

/**
 * 信息展示设置项（不可点击）
 * 
 * 用于显示只读信息，如版本号、存储使用等。
 */
@Composable
fun InfoSettingsItem(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color? = null
) {
    val resolvedIconTint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
    
    ListItem(
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedIconTint
            )
        },
        headlineContent = {
            Text(text = title)
        },
        supportingContent = subtitle?.let { {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/**
 * 危险操作设置项
 * 
 * 用于删除数据、重置等危险操作，使用红色强调。
 */
@Composable
fun DangerSettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    val dangerColor = MaterialTheme.colorScheme.error
    
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = dangerColor.copy(alpha = contentAlpha)
            )
        },
        headlineContent = {
            Text(
                text = title,
                color = dangerColor.copy(alpha = contentAlpha),
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = subtitle?.let { {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        } },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = dangerColor.copy(alpha = 0.5f * contentAlpha)
            )
        }
    )
}
