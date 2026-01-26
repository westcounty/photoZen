package com.example.photozen.ui.screens.lighttable

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.KeepGreen

/**
 * Zoomable/pannable image that syncs with a shared TransformState.
 * Multiple instances of this component will zoom/pan together when
 * sharing the same TransformState.
 *
 * 支持基准偏移模式：当提供 baseSnapshot 时，最终变换 = base * delta
 * 这允许在同步模式下，每张照片从各自的基准位置开始，然后应用相同的增量变换
 *
 * @param photo The photo to display
 * @param transformState Shared state for synchronized transformations (作为增量使用)
 * @param baseSnapshot 基准状态快照，如果为null则不使用基准模式
 * @param isSelected Whether this photo is selected as "best"
 * @param onSelect Callback when photo is tapped to select
 * @param onFullscreenClick Callback when fullscreen button is clicked
 * @param fullscreenButtonPosition Position of fullscreen button (default TopEnd, use BottomEnd for top row to avoid status bar)
 * @param onTransformGesture Callback when user performs zoom/pan gesture
 * @param modifier Modifier for the component
 */
@Composable
fun SyncZoomImage(
    photo: PhotoEntity,
    transformState: TransformState,
    baseSnapshot: TransformSnapshot? = null,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onFullscreenClick: (() -> Unit)? = null,
    fullscreenButtonPosition: Alignment = Alignment.TopEnd,
    onTransformGesture: (scale: Float, offset: Offset) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = KeepGreen,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .onSizeChanged { containerSize = it }
            // Use transformState as key to ensure gesture handlers are recreated when state reference changes
            // This fixes the issue where zoomed photo becomes unresponsive after toggling sync mode
            .pointerInput(transformState) {
                detectTransformGestures(
                    onGesture = { centroid, pan, zoom, _ ->
                        // Calculate new scale
                        val newScale = transformState.scale * zoom

                        // Update transform state (this affects all synced images)
                        if (zoom != 1f) {
                            transformState.updateScale(newScale, centroid)
                        }

                        // Apply pan with bounds checking to prevent photo from moving too far out
                        if (transformState.scale > 1f) {
                            transformState.updateOffset(pan.x, pan.y)
                            // Apply bounds to prevent panning mostly outside the container
                            // This ensures the photo remains controllable
                            if (containerSize.width > 0 && containerSize.height > 0) {
                                transformState.applyBounds(
                                    containerSize.width.toFloat(),
                                    containerSize.height.toFloat()
                                )
                            }
                        }

                        // Notify parent of gesture
                        onTransformGesture(newScale, pan)
                    }
                )
            }
            .pointerInput(transformState) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        // Double tap to toggle zoom
                        if (transformState.scale > 1.5f) {
                            transformState.reset()
                        } else {
                            transformState.updateScale(2.5f, offset)
                        }
                    },
                    onTap = {
                        onSelect()
                    }
                )
            }
    ) {
        // Image with synchronized transformation
        // 计算最终变换值：如果有基准快照，则 最终值 = 基准 * 增量
        val effectiveScale = if (baseSnapshot != null) {
            baseSnapshot.scale * transformState.scale
        } else {
            transformState.scale
        }
        val effectiveOffsetX = if (baseSnapshot != null) {
            // 基准偏移 + 增量偏移（增量偏移需要按基准缩放比例调整）
            baseSnapshot.offsetX + transformState.offsetX * baseSnapshot.scale
        } else {
            transformState.offsetX
        }
        val effectiveOffsetY = if (baseSnapshot != null) {
            baseSnapshot.offsetY + transformState.offsetY * baseSnapshot.scale
        } else {
            transformState.offsetY
        }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(photo.systemUri))
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = effectiveScale
                    scaleY = effectiveScale
                    translationX = effectiveOffsetX
                    translationY = effectiveOffsetY
                }
        )
        
        // Top row: Selection indicator (left) and Fullscreen button (right)
        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(KeepGreen)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = Color.White,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
        
        // Fullscreen button (position configurable, use BottomEnd for top row photos to avoid status bar)
        if (onFullscreenClick != null) {
            IconButton(
                onClick = onFullscreenClick,
                modifier = Modifier
                    .align(fullscreenButtonPosition)
                    .padding(4.dp)
                    .size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "全屏预览",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
