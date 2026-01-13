package com.example.photozen.ui.screens.flowsorter

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Photo card composable for Flow Sorter.
 * Displays a photo with metadata overlay and swipe indicators.
 * Preserves the original aspect ratio of the photo.
 * 
 * @param photo The photo entity to display
 * @param swipeProgress Current swipe progress (-1 to 1 for horizontal, 0 to -1 for vertical)
 * @param swipeDirection Current swipe direction based on gesture
 * @param onPhotoClick Called when photo is clicked (for fullscreen view)
 * @param modifier Modifier for the card
 */
@Composable
fun PhotoCard(
    photo: PhotoEntity,
    swipeProgress: Float = 0f,
    swipeDirection: SwipeDirection = SwipeDirection.NONE,
    onPhotoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Calculate aspect ratio from photo dimensions
    val aspectRatio = if (photo.width > 0 && photo.height > 0) {
        photo.width.toFloat() / photo.height.toFloat()
    } else {
        // Default to 4:3 if dimensions unknown
        4f / 3f
    }
    
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Photo container with aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .then(
                        if (onPhotoClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onPhotoClick
                            )
                        } else Modifier
                    )
            ) {
                // Photo with preserved aspect ratio
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(photo.systemUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio.coerceIn(0.3f, 3f))
                        .clip(RoundedCornerShape(16.dp))
                )
                
                // Tap hint badge
                if (onPhotoClick != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "点击查看大图",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Gradient overlay at bottom for text readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
            
            // Photo info at bottom
            PhotoInfoOverlay(
                photo = photo,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            )
            
            // Swipe indicator overlays
            SwipeIndicatorOverlay(
                swipeDirection = swipeDirection,
                swipeProgress = swipeProgress,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Displays photo metadata at the bottom of the card.
 */
@Composable
private fun PhotoInfoOverlay(
    photo: PhotoEntity,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // File name
        Text(
            text = photo.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Date and dimensions
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date
            if (photo.dateTaken > 0) {
                Text(
                    text = formatDate(photo.dateTaken),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Dimensions
            if (photo.width > 0 && photo.height > 0) {
                Text(
                    text = "${photo.width} × ${photo.height}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Camera model
            photo.cameraModel?.let { model ->
                Text(
                    text = model,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Displays swipe direction indicators with animated opacity.
 */
@Composable
private fun SwipeIndicatorOverlay(
    swipeDirection: SwipeDirection,
    swipeProgress: Float,
    modifier: Modifier = Modifier
) {
    val absProgress = kotlin.math.abs(swipeProgress).coerceIn(0f, 1f)
    
    Box(modifier = modifier) {
        // Keep indicator (right swipe) - Green
        if (swipeDirection == SwipeDirection.RIGHT || swipeProgress > 0.1f) {
            SwipeIndicatorBadge(
                icon = Icons.Default.Check,
                label = "保留",
                color = KeepGreen,
                alpha = if (swipeProgress > 0) absProgress else 0f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
            )
        }
        
        // Trash indicator (left swipe) - Red
        if (swipeDirection == SwipeDirection.LEFT || swipeProgress < -0.1f) {
            SwipeIndicatorBadge(
                icon = Icons.Default.Close,
                label = "删除",
                color = TrashRed,
                alpha = if (swipeProgress < 0) absProgress else 0f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
            )
        }
        
        // Maybe indicator (up swipe) - Amber
        if (swipeDirection == SwipeDirection.UP) {
            SwipeIndicatorBadge(
                icon = Icons.Default.QuestionMark,
                label = "待定",
                color = MaybeAmber,
                alpha = absProgress,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
            )
        }
    }
}

/**
 * Individual swipe indicator badge.
 */
@Composable
private fun SwipeIndicatorBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Format timestamp to readable date string.
 */
private fun formatDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}
