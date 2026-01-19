package com.example.photozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.photozen.data.local.entity.PhotoEntity

/**
 * 时间线事件照片行组件
 * 
 * 用于在时间线事件卡片中展示照片，支持手势交互。
 * 
 * ## 手势规范
 * 
 * - **点击（非选择模式）**：进入全屏预览
 * - **点击（选择模式）**：切换选中状态
 * - **长按**：选中该照片并进入选择模式
 * 
 * ## 注意
 * 
 * 由于水平布局特性，**不支持拖动批量选择**。
 * 用户需要通过多次长按/点击来选择多张照片。
 * 
 * @param photos 照片列表
 * @param selectedIds 已选中的照片ID集合
 * @param isSelectionMode 是否处于选择模式
 * @param onPhotoClick 照片点击回调（非选择模式时，传入照片ID）
 * @param onPhotoLongPress 照片长按回调（进入选择模式并选中该照片）
 * @param onSelectionToggle 切换选中状态回调（选择模式时）
 * @param maxDisplay 最大显示数量，超过时显示"查看更多"
 * @param onViewMore 查看更多回调
 * @param selectionColor 选中颜色
 * @param modifier Modifier
 * 
 * @since Phase 1-B
 */
@Composable
fun TimelineEventPhotoRow(
    photos: List<PhotoEntity>,
    selectedIds: Set<String>,
    isSelectionMode: Boolean,
    onPhotoClick: (String) -> Unit,
    onPhotoLongPress: (String) -> Unit,
    onSelectionToggle: (String) -> Unit,
    maxDisplay: Int = 12,
    onViewMore: (() -> Unit)? = null,
    selectionColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val displayPhotos = photos.take(maxDisplay)
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(displayPhotos, key = { it.id }) { photo ->
            val isSelected = photo.id in selectedIds
            
            TimelinePhotoItem(
                photo = photo,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                selectionColor = selectionColor,
                onClick = {
                    if (isSelectionMode) {
                        onSelectionToggle(photo.id)
                    } else {
                        onPhotoClick(photo.id)
                    }
                },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPhotoLongPress(photo.id)
                }
            )
        }
        
        // 查看更多按钮
        if (photos.size > maxDisplay && onViewMore != null) {
            item {
                ViewMoreItem(
                    remainingCount = photos.size - maxDisplay,
                    onClick = onViewMore
                )
            }
        }
    }
}

/**
 * 时间线照片单项
 * 
 * @param photo 照片实体
 * @param isSelected 是否选中
 * @param isSelectionMode 是否处于选择模式
 * @param selectionColor 选中颜色
 * @param onClick 点击回调
 * @param onLongPress 长按回调
 */
@Composable
private fun TimelinePhotoItem(
    photo: PhotoEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    selectionColor: Color,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = selectionColor,
                        shape = RoundedCornerShape(6.dp)
                    )
                } else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        // 照片
        AsyncImage(
            model = photo.systemUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // 选中状态覆盖层
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(selectionColor.copy(alpha = 0.2f))
            )
        }
        
        // 选择指示器（选择模式时显示）
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) selectionColor
                        else Color.Black.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * "查看更多" 按钮项
 * 
 * @param remainingCount 剩余照片数量
 * @param onClick 点击回调
 */
@Composable
private fun ViewMoreItem(
    remainingCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+$remainingCount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "查看全部",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
