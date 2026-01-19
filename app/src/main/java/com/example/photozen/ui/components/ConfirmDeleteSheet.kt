package com.example.photozen.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.TrashRed

/**
 * 删除操作类型
 */
enum class DeleteType {
    /** 移入回收站（可撤销） */
    MOVE_TO_TRASH,
    /** 永久删除（不可撤销） */
    PERMANENT_DELETE
}

/**
 * 删除确认底部弹窗 (Phase 3-9)
 * 
 * 显示待删除照片的预览，根据删除类型显示不同的警告和按钮样式。
 * 
 * ## 设计说明
 * 
 * - **照片预览**: 最多显示 9 张缩略图
 * - **操作区分**:
 *   - 移入回收站：红色按钮，提示可撤销
 *   - 永久删除：深红色按钮，强警告
 * 
 * @param photos 待删除的照片列表
 * @param deleteType 删除类型
 * @param onConfirm 确认删除回调
 * @param onDismiss 取消回调
 * @param isLoading 是否正在执行删除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteSheet(
    photos: List<PhotoEntity>,
    deleteType: DeleteType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photoCount = photos.size
    
    // 最多显示 9 张预览
    val previewPhotos = photos.take(9)
    val hasMore = photos.size > 9
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 警告图标
            Icon(
                imageVector = if (deleteType == DeleteType.PERMANENT_DELETE) {
                    Icons.Default.DeleteForever
                } else {
                    Icons.Default.Delete
                },
                contentDescription = null,
                tint = TrashRed,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题
            Text(
                text = when (deleteType) {
                    DeleteType.MOVE_TO_TRASH -> "移入回收站"
                    DeleteType.PERMANENT_DELETE -> "永久删除"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 描述文案
            Text(
                text = when (deleteType) {
                    DeleteType.MOVE_TO_TRASH -> 
                        "确定要将 $photoCount 张照片移入回收站吗？\n你可以在回收站中恢复它们。"
                    DeleteType.PERMANENT_DELETE -> 
                        "确定要永久删除 $photoCount 张照片吗？\n此操作不可撤销！"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 照片预览网格
            if (previewPhotos.isNotEmpty()) {
                PhotoPreviewGrid(
                    photos = previewPhotos,
                    hasMore = hasMore,
                    moreCount = photos.size - 9,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 永久删除的额外警告
            if (deleteType == DeleteType.PERMANENT_DELETE) {
                Surface(
                    color = TrashRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TrashRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "照片将从设备中彻底删除，无法恢复",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TrashRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 取消按钮
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消")
                }
                
                // 确认按钮
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deleteType == DeleteType.PERMANENT_DELETE) {
                            TrashRed
                        } else {
                            TrashRed.copy(alpha = 0.85f)
                        },
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = if (deleteType == DeleteType.PERMANENT_DELETE) {
                                Icons.Default.DeleteForever
                            } else {
                                Icons.Default.Delete
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (deleteType) {
                                DeleteType.MOVE_TO_TRASH -> "移入回收站"
                                DeleteType.PERMANENT_DELETE -> "永久删除"
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 照片预览网格
 */
@Composable
private fun PhotoPreviewGrid(
    photos: List<PhotoEntity>,
    hasMore: Boolean,
    moreCount: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 计算列数：1-2张显示2列，3+张显示3列
    val columns = if (photos.size <= 2) 2 else 3
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.heightIn(max = 200.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(photos) { photo ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(photo.systemUri))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        
        // 显示更多数量
        if (hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+$moreCount",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 简化的删除确认对话框（用于单张照片删除）
 */
@Composable
fun ConfirmDeleteDialog(
    photoUri: String?,
    deleteType: DeleteType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (deleteType == DeleteType.PERMANENT_DELETE) {
                    Icons.Default.DeleteForever
                } else {
                    Icons.Default.Delete
                },
                contentDescription = null,
                tint = TrashRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = when (deleteType) {
                    DeleteType.MOVE_TO_TRASH -> "移入回收站"
                    DeleteType.PERMANENT_DELETE -> "永久删除"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 照片预览
                if (photoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(photoUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                
                Text(
                    text = when (deleteType) {
                        DeleteType.MOVE_TO_TRASH -> "确定要将这张照片移入回收站吗？"
                        DeleteType.PERMANENT_DELETE -> "确定要永久删除这张照片吗？此操作不可撤销！"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrashRed,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = when (deleteType) {
                            DeleteType.MOVE_TO_TRASH -> "移入"
                            DeleteType.PERMANENT_DELETE -> "删除"
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}
