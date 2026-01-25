package com.example.photozen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.AlbumBubbleEntity

/**
 * 相册多选组件
 *
 * 以 FlowRow 形式展示相册列表，支持多选。
 * 使用 FilterChip 替代原有的网格卡片布局，只显示相册名称。
 *
 * ## 设计规范
 * - FlowRow 自动换行布局（解决与 BottomSheet 的手势冲突）
 * - 每个相册只显示名称 + 选中状态
 * - 最大高度 180dp，超出可滚动
 *
 * @param albums 相册列表
 * @param selectedIds 已选中的相册 ID 集合
 * @param onSelectionChange 选择变更回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlbumMultiSelector(
    albums: List<AlbumBubbleEntity>,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 相册列表（使用 FlowRow 避免与 BottomSheet 手势冲突）
        // 注：全选/清除按钮已移至 FilterSection 标题行右侧
        if (albums.isEmpty()) {
            Text(
                text = "暂无可选相册",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            // 使用 Column + verticalScroll 包裹 FlowRow，避免 LazyVerticalGrid 的嵌套滚动冲突
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    albums.forEach { album ->
                        val isSelected = album.bucketId in selectedIds
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newSelection = if (isSelected) {
                                    selectedIds - album.bucketId
                                } else {
                                    selectedIds + album.bucketId
                                }
                                onSelectionChange(newSelection)
                            },
                            label = {
                                Text(
                                    text = album.displayName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已选中"
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // 已选提示
        if (selectedIds.isNotEmpty()) {
            Text(
                text = "已选择 ${selectedIds.size} 个相册",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * 相册选择器状态
 */
data class AlbumSelectorState(
    val albums: List<AlbumBubbleEntity>,
    val selectedIds: Set<String>
) {
    val selectedCount: Int get() = selectedIds.size
    val isAllSelected: Boolean get() = selectedIds.size == albums.size && albums.isNotEmpty()
}
