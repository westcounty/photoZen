package com.example.photozen.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.photozen.domain.model.FilterConfig
import com.example.photozen.domain.model.FilterType
import com.example.photozen.ui.theme.PicZenMotion
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 筛选条件 Chip 行
 * 
 * 在页面顶部显示当前生效的筛选条件，支持快速清除。
 * 
 * ## 设计规范
 * - 横向滚动，Chip 间距 8dp
 * - 左右边距 16dp，上下边距 8dp
 * - 每个 Chip 显示图标 + 文字 + 关闭按钮
 * - 多个条件时显示"清除全部"按钮
 * 
 * @param config 当前筛选配置
 * @param albumNames 相册 ID 到名称的映射
 * @param onEditFilter 点击 Chip 编辑筛选
 * @param onClearFilter 清除单个筛选条件
 * @param onClearAll 清除所有筛选条件
 */
@Composable
fun FilterChipRow(
    config: FilterConfig,
    albumNames: Map<String, String>,
    onEditFilter: () -> Unit,
    onClearFilter: (FilterType) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 无筛选条件时不显示
    if (config.isEmpty) return
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 筛选图标
        item {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "筛选",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // 相册筛选 Chip
        config.albumIds?.takeIf { it.isNotEmpty() }?.let { ids ->
            item {
                FilterConditionChip(
                    icon = Icons.Default.Collections,
                    label = formatAlbumFilter(ids, albumNames),
                    onClick = onEditFilter,
                    onClear = { onClearFilter(FilterType.ALBUM) }
                )
            }
        }
        
        // 日期筛选 Chip
        if (config.hasDateFilter) {
            item {
                FilterConditionChip(
                    icon = Icons.Default.DateRange,
                    label = formatDateRange(config.startDate, config.endDate),
                    onClick = onEditFilter,
                    onClear = { onClearFilter(FilterType.DATE) }
                )
            }
        }
        
        // 清除全部按钮（多个条件时显示）
        if (config.activeFilterCount > 1) {
            item {
                TextButton(
                    onClick = onClearAll,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "清除全部",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * 单个筛选条件 Chip
 *
 * ## Enhanced Features
 * - Press scale animation (0.95f) for tactile feedback
 * - Border width animation (1dp → 2dp) on press
 * - Enhanced close button with scale animation
 */
@Composable
private fun FilterConditionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press scale animation - 0.95f for chips
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "chipScale"
    )

    // Border width animation
    val borderWidth by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 1.dp,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "chipBorderWidth"
    )

    FilterChip(
        selected = true,
        onClick = onClick,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            EnhancedCloseButton(
                onClick = onClear
            )
        },
        interactionSource = interactionSource,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = true,
            borderWidth = borderWidth,
            selectedBorderWidth = borderWidth,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * Enhanced close button with press scale animation
 */
@Composable
private fun EnhancedCloseButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press scale animation - 0.85f for small buttons
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "closeButtonScale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "清除",
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
 * 格式化相册筛选显示文本
 */
private fun formatAlbumFilter(albumIds: List<String>, albumNames: Map<String, String>): String {
    return when {
        albumIds.size == 1 -> albumNames[albumIds.first()] ?: "1 个相册"
        else -> "${albumIds.size} 个相册"
    }
}

/**
 * 格式化日期范围显示文本
 */
private fun formatDateRange(startDate: Long?, endDate: Long?): String {
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    
    return when {
        startDate != null && endDate != null -> {
            "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
        }
        startDate != null -> "${dateFormat.format(startDate)} 起"
        endDate != null -> "至 ${dateFormat.format(endDate)}"
        else -> ""
    }
}

/**
 * 带计数的筛选按钮
 * 
 * 用于在 TopAppBar 中显示筛选入口
 */
@Composable
fun FilterButton(
    activeFilterCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(
            badge = {
                if (activeFilterCount > 0) {
                    Badge {
                        Text(activeFilterCount.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "筛选"
            )
        }
    }
}
