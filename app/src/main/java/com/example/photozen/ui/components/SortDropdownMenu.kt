package com.example.photozen.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

/**
 * PhotoZen 统一排序下拉菜单组件 (REQ-022)
 * ========================================
 *
 * 功能特性:
 * - 统一的排序按钮交互形式
 * - 点击显示下拉菜单
 * - 当前选中项有勾选标记
 * - 支持自定义排序选项
 * - 每个选项可配置图标
 *
 * 适用页面:
 * - 待定列表 (MaybeList)
 * - 回收站列表 (TrashList)
 * - 已保留列表 (KeepList)
 * - 相册照片列表 (AlbumPhotoList)
 * - 时间线列表 (TimelineList)
 * - 筛选列表 (FilterList)
 */

/**
 * 排序选项数据类
 *
 * @param id 唯一标识符
 * @param displayName 显示名称
 * @param icon 可选图标
 */
data class SortOption(
    val id: String,
    val displayName: String,
    val icon: ImageVector? = null
)

/**
 * 预定义排序选项集合
 *
 * 根据 REQUIREMENTS_LISTING.md 中的需求定义：
 * - 待定列表: 照片真实时间正序/倒序、添加时间正序/倒序
 * - 回收站列表: 同待定列表
 * - 已保留列表: 照片真实时间正序/倒序、添加时间正序/倒序、随机排序
 * - 相册/时间线列表: 照片真实时间正序/倒序、随机排序
 * - 筛选列表: 照片真实时间正序/倒序、随机排序
 */
object SortOptions {
    // 基础排序选项
    val photoTimeDesc = SortOption(
        id = "photo_time_desc",
        displayName = "时间倒序",
        icon = Icons.Default.ArrowDownward
    )
    val photoTimeAsc = SortOption(
        id = "photo_time_asc",
        displayName = "时间正序",
        icon = Icons.Default.ArrowUpward
    )
    val addedTimeDesc = SortOption(
        id = "added_time_desc",
        displayName = "添加时间倒序",
        icon = Icons.Default.Schedule
    )
    val addedTimeAsc = SortOption(
        id = "added_time_asc",
        displayName = "添加时间正序",
        icon = Icons.Default.Schedule
    )
    val random = SortOption(
        id = "random",
        displayName = "随机排序",
        icon = Icons.Default.Shuffle
    )

    // 回收站专用排序选项（显示"移至回收站时间"）
    val trashAddedTimeDesc = SortOption(
        id = "added_time_desc",
        displayName = "移至回收站时间倒序",
        icon = Icons.Default.Schedule
    )
    val trashAddedTimeAsc = SortOption(
        id = "added_time_asc",
        displayName = "移至回收站时间正序",
        icon = Icons.Default.Schedule
    )

    // 已保留列表专用排序选项（显示"标记为保留时间"）
    val keepAddedTimeDesc = SortOption(
        id = "added_time_desc",
        displayName = "标记为保留时间倒序",
        icon = Icons.Default.Schedule
    )
    val keepAddedTimeAsc = SortOption(
        id = "added_time_asc",
        displayName = "标记为保留时间正序",
        icon = Icons.Default.Schedule
    )

    // 各列表的排序选项预设
    val maybeListOptions = listOf(photoTimeDesc, photoTimeAsc, addedTimeDesc, addedTimeAsc)
    val trashListOptions = listOf(photoTimeDesc, photoTimeAsc, trashAddedTimeDesc, trashAddedTimeAsc)
    val keepListOptions = listOf(photoTimeDesc, photoTimeAsc, keepAddedTimeDesc, keepAddedTimeAsc, random)
    val albumListOptions = listOf(photoTimeDesc, photoTimeAsc, random)
    val timelineListOptions = listOf(photoTimeDesc, photoTimeAsc, random)
    val filterListOptions = listOf(photoTimeDesc, photoTimeAsc, random)
}

/**
 * 统一排序下拉菜单按钮
 *
 * @param currentSort 当前选中的排序选项
 * @param options 可用的排序选项列表
 * @param onSortSelected 排序选项选中回调
 * @param modifier Modifier
 * @param enabled 是否启用
 */
@Composable
fun SortDropdownButton(
    currentSort: SortOption,
    options: List<SortOption>,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "排序: ${currentSort.displayName}"
                // 不设置 tint，使用 IconButton 默认样式
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                val isSelected = option.id == currentSort.id
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "当前选中",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                    }
                )
            }
        }
    }
}

/**
 * 带文字的排序下拉菜单按钮
 *
 * 适用于需要明确显示当前排序状态的场景
 *
 * @param currentSort 当前选中的排序选项
 * @param options 可用的排序选项列表
 * @param onSortSelected 排序选项选中回调
 * @param modifier Modifier
 * @param enabled 是否启用
 */
@Composable
fun SortDropdownButtonWithLabel(
    currentSort: SortOption,
    options: List<SortOption>,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        androidx.compose.material3.TextButton(
            onClick = { expanded = true },
            enabled = enabled
        ) {
            currentSort.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.then(Modifier)
                )
            }
            Text(
                text = currentSort.displayName,
                style = MaterialTheme.typography.labelMedium
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                val isSelected = option.id == currentSort.id
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "当前选中",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                    }
                )
            }
        }
    }
}
