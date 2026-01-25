package com.example.photozen.ui.components

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.PicZenMotion
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
    
    // 警告图标摇晃动画 (±3°, 1.5秒循环)
    val infiniteTransition = rememberInfiniteTransition(label = "warningShake")
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeRotation"
    )

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
            // 警告图标 - 带摇晃动画
            Icon(
                imageVector = if (deleteType == DeleteType.PERMANENT_DELETE) {
                    Icons.Default.DeleteForever
                } else {
                    Icons.Default.Delete
                },
                contentDescription = null,
                tint = TrashRed,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        rotationZ = iconRotation
                    }
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
                // 取消按钮 - 带按压缩放动画
                val cancelInteractionSource = remember { MutableInteractionSource() }
                val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
                val cancelScale by animateFloatAsState(
                    targetValue = if (isCancelPressed) 0.97f else 1f,
                    animationSpec = PicZenMotion.Springs.snappy(),
                    label = "cancelScale"
                )

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = cancelScale
                            scaleY = cancelScale
                        },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    interactionSource = cancelInteractionSource
                ) {
                    Text("取消")
                }

                // 确认按钮 - 带脉冲动画和按压反馈
                DangerPulseButton(
                    text = when (deleteType) {
                        DeleteType.MOVE_TO_TRASH -> "移入回收站"
                        DeleteType.PERMANENT_DELETE -> "永久删除"
                    },
                    icon = if (deleteType == DeleteType.PERMANENT_DELETE) {
                        Icons.Default.DeleteForever
                    } else {
                        Icons.Default.Delete
                    },
                    isPermanentDelete = deleteType == DeleteType.PERMANENT_DELETE,
                    isLoading = isLoading,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 危险操作脉冲按钮
 *
 * 特效:
 * - 脉冲缩放动画 (1.0 → 1.02 → 1.0, 2秒循环)
 * - 红色光晕呼吸效果
 * - 按压时脉冲暂停，缩放到0.95f
 */
@Composable
private fun DangerPulseButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPermanentDelete: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 脉冲动画 (按压时暂停)
    val infiniteTransition = rememberInfiniteTransition(label = "dangerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // 光晕透明度
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // 按压缩放
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "pressScale"
    )

    // 最终缩放 = 脉冲 * 按压 (按压时用按压缩放替代脉冲)
    val finalScale = if (isPressed) pressScale else pulseScale

    Button(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = finalScale
                scaleY = finalScale
            }
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = TrashRed.copy(alpha = glowAlpha),
                spotColor = TrashRed.copy(alpha = glowAlpha * 1.5f)
            ),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPermanentDelete) TrashRed else TrashRed.copy(alpha = 0.85f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        interactionSource = interactionSource
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text)
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
