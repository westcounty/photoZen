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
 * @param photo The photo to display
 * @param transformState Shared state for synchronized transformations
 * @param isSelected Whether this photo is selected as "best"
 * @param onSelect Callback when photo is tapped to select
 * @param onFullscreenClick Callback when fullscreen button is clicked
 * @param onTransformGesture Callback when user performs zoom/pan gesture
 * @param modifier Modifier for the component
 */
@Composable
fun SyncZoomImage(
    photo: PhotoEntity,
    transformState: TransformState,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onFullscreenClick: (() -> Unit)? = null,
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
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { centroid, pan, zoom, _ ->
                        // Calculate new scale
                        val newScale = transformState.scale * zoom
                        
                        // Update transform state (this affects all synced images)
                        if (zoom != 1f) {
                            transformState.updateScale(newScale, centroid)
                        }
                        
                        // Apply pan
                        if (transformState.scale > 1f) {
                            transformState.updateOffset(pan.x, pan.y)
                        }
                        
                        // Notify parent of gesture
                        onTransformGesture(newScale, pan)
                    }
                )
            }
            .pointerInput(Unit) {
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
                    scaleX = transformState.scale
                    scaleY = transformState.scale
                    translationX = transformState.offsetX
                    translationY = transformState.offsetY
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
        
        // Fullscreen button
        if (onFullscreenClick != null) {
            IconButton(
                onClick = onFullscreenClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
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
