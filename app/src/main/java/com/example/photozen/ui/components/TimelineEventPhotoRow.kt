package com.example.photozen.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.theme.PicZenTokens
import kotlinx.coroutines.delay

/**
 * 时间线事件照片行组件
 *
 * 用于在时间线事件卡片中展示照片，支持手势交互。
 *
 * ## 手势规范
 *
 * - **点击（非选择模式）**：进入全屏预览
 * - **点击（选择模式）**：切换选中状态
 * - **长按**：无反应（时间线不支持长按进入选择模式）
 *
 * ## 注意
 *
 * 由于水平布局特性，**不支持拖动批量选择**。
 * 时间线照片不支持长按选择，用户需要通过其他方式进入选择模式。
 *
 * @param photos 照片列表
 * @param selectedIds 已选中的照片ID集合
 * @param isSelectionMode 是否处于选择模式
 * @param onPhotoClick 照片点击回调（非选择模式时，传入照片ID）
 * @param onPhotoLongPress 照片长按回调（保留参数兼容性，但不会触发）
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
        itemsIndexed(displayPhotos, key = { _, photo -> photo.id }) { index, photo ->
            val isSelected = photo.id in selectedIds

            TimelinePhotoItem(
                photo = photo,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                selectionColor = selectionColor,
                onClick = {
                    if (isSelectionMode) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectionToggle(photo.id)
                    } else {
                        onPhotoClick(photo.id)
                    }
                },
                // 时间线不支持长按进入选择模式，传空回调
                onLongPress = null,
                animationDelay = index * 30  // 错开30ms入场
            )
        }

        // 查看更多按钮
        if (photos.size > maxDisplay && onViewMore != null) {
            item {
                ViewMoreItem(
                    remainingCount = photos.size - maxDisplay,
                    onClick = onViewMore,
                    animationDelay = displayPhotos.size * 30
                )
            }
        }
    }
}

/**
 * 时间线照片单项
 *
 * 增强动画:
 * - 按压缩放 (0.95f)
 * - 阴影动态变化
 * - 错开入场动画
 *
 * @param photo 照片实体
 * @param isSelected 是否选中
 * @param isSelectionMode 是否处于选择模式
 * @param selectionColor 选中颜色
 * @param onClick 点击回调
 * @param onLongPress 长按回调（可为 null，不触发长按事件）
 * @param animationDelay 入场动画延迟（毫秒）
 */
@Composable
private fun TimelinePhotoItem(
    photo: PhotoEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    selectionColor: Color,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    animationDelay: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 入场动画
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val entryAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(PicZenMotion.Duration.Fast),
        label = "entryAlpha"
    )
    val entryOffsetX by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "entryOffsetX"
    )

    // 按压缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "pressScale"
    )

    // 阴影动画
    val elevation by animateDpAsState(
        targetValue = if (isPressed) PicZenTokens.Elevation.Level1 else PicZenTokens.Elevation.Level2,
        animationSpec = tween(PicZenMotion.Duration.Quick),
        label = "elevation"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer {
                alpha = entryAlpha
                scaleX = scale
                scaleY = scale
                translationX = entryOffsetX
            }
            .shadow(elevation, RoundedCornerShape(6.dp))
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerInput(onLongPress) {
                if (onLongPress != null) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
                    )
                }
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
 * 增强动画:
 * - 按压缩放 (0.95f)
 * - 入场动画
 *
 * @param remainingCount 剩余照片数量
 * @param onClick 点击回调
 * @param animationDelay 入场动画延迟（毫秒）
 */
@Composable
private fun ViewMoreItem(
    remainingCount: Int,
    onClick: () -> Unit,
    animationDelay: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 入场动画
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val entryAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(PicZenMotion.Duration.Fast),
        label = "entryAlpha"
    )
    val entryOffsetX by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "entryOffsetX"
    )

    // 按压缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer {
                alpha = entryAlpha
                scaleX = scale
                scaleY = scale
                translationX = entryOffsetX
            }
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
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
