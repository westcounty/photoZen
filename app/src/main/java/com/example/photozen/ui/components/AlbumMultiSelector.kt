package com.example.photozen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.photozen.data.local.entity.AlbumBubbleEntity

/**
 * 相册多选组件
 * 
 * 以网格形式展示相册列表，支持多选。
 * 
 * ## 设计规范
 * - 3 列网格布局
 * - 每个相册显示缩略图 + 名称 + 选中状态
 * - 最大高度 200dp，超出可滚动
 * 
 * @param albums 相册列表
 * @param selectedIds 已选中的相册 ID 集合
 * @param onSelectionChange 选择变更回调
 */
@Composable
fun AlbumMultiSelector(
    albums: List<AlbumBubbleEntity>,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 快捷操作
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { onSelectionChange(albums.map { it.bucketId }.toSet()) },
                enabled = selectedIds.size < albums.size
            ) {
                Text("全选")
            }
            TextButton(
                onClick = { onSelectionChange(emptySet()) },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("清除")
            }
        }
        
        // 相册网格
        if (albums.isEmpty()) {
            Text(
                text = "暂无可选相册",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(albums, key = { it.bucketId }) { album ->
                    AlbumSelectItem(
                        album = album,
                        isSelected = album.bucketId in selectedIds,
                        onToggle = {
                            val newSelection = if (album.bucketId in selectedIds) {
                                selectedIds - album.bucketId
                            } else {
                                selectedIds + album.bucketId
                            }
                            onSelectionChange(newSelection)
                        }
                    )
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
 * 单个相册选择项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumSelectItem(
    album: AlbumBubbleEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 相册名称（由于没有封面图，显示相册名首字）
            Text(
                text = album.displayName.take(1),
                style = MaterialTheme.typography.headlineMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.align(Alignment.Center)
            )
            
            // 选中指示
            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            
            // 相册名称
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Text(
                    text = album.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(4.dp)
                )
            }
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
