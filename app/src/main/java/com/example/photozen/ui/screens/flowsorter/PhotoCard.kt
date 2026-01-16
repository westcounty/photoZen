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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import com.example.photozen.util.OfflineGeocoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Photo card composable for Flow Sorter.
 * Displays a photo with metadata overlay and swipe indicators.
 * Preserves the original aspect ratio of the photo.
 * 
 * @param photo The photo entity to display
 * @param swipeProgress Current horizontal swipe progress (-1 to 1)
 * @param swipeProgressY Current vertical swipe progress (-1 to 1)
 * @param swipeDirection Current swipe direction based on gesture
 * @param onPhotoClick Called when photo is clicked (for fullscreen view)
 * @param modifier Modifier for the card
 */
@Composable
fun PhotoCard(
    photo: PhotoEntity,
    swipeProgress: Float = 0f,
    swipeProgressY: Float = 0f,
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
                // Use SubcomposeAsyncImage to show a placeholder background while loading
                // This prevents the "flash to black" issue when transitioning between cards
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(photo.systemUri))
                        .memoryCacheKey(photo.id) // Use photo.id for better cache hits
                        .diskCacheKey(photo.id) // Also use for disk cache
                        .build(),
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio.coerceIn(0.3f, 3f))
                        .clip(RoundedCornerShape(16.dp))
                        // Add a subtle background so there's no "black flash" while loading
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            // Show a subtle loading placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                        is AsyncImagePainter.State.Error -> {
                            // Show error state
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                            )
                        }
                        else -> {
                            // Show the actual image
                            SubcomposeAsyncImageContent()
                        }
                    }
                }
                
                // Tap hint badge - Only shown when zoom is enabled
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
                swipeProgressX = swipeProgress,
                swipeProgressY = swipeProgressY,
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
    val context = LocalContext.current
    val offlineGeocoder = remember { OfflineGeocoder(context) }
    var locationText by remember { mutableStateOf<String?>(null) }
    
    // Load location text asynchronously
    LaunchedEffect(photo.latitude, photo.longitude) {
        locationText = offlineGeocoder.getLocationText(photo.latitude, photo.longitude)
    }
    
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
        
        // Date/Time and Location row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date and Time (combined)
            if (photo.dateTaken > 0) {
                Text(
                    text = formatDateTime(photo.dateTaken),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Location
            locationText?.let { location ->
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Dimensions and Camera row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
 * All indicators are centered at top of the photo for better visibility.
 * Icon only, no text label.
 * 
 * Gesture mapping:
 * - LEFT/RIGHT → Keep (Green)
 * - UP → Trash (Red)
 * - DOWN → Maybe (Amber)
 */
@Composable
private fun SwipeIndicatorOverlay(
    swipeDirection: SwipeDirection,
    swipeProgressX: Float,
    swipeProgressY: Float,
    modifier: Modifier = Modifier
) {
    // Consistent alpha for all indicators (0.85 = clearly visible)
    val indicatorAlpha = 0.85f
    
    Box(modifier = modifier) {
        // Keep indicator (right swipe) - Green
        if (swipeDirection == SwipeDirection.RIGHT || (swipeDirection == SwipeDirection.NONE && swipeProgressX > 0.1f)) {
            SwipeIndicatorIcon(
                icon = Icons.Default.Check,
                color = KeepGreen,
                alpha = indicatorAlpha,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
        
        // Keep indicator (left swipe) - Green (same as right)
        if (swipeDirection == SwipeDirection.LEFT || (swipeDirection == SwipeDirection.NONE && swipeProgressX < -0.1f)) {
            SwipeIndicatorIcon(
                icon = Icons.Default.Check,
                color = KeepGreen,
                alpha = indicatorAlpha,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
        
        // Trash indicator (up swipe) - Red
        if (swipeDirection == SwipeDirection.UP) {
            SwipeIndicatorIcon(
                icon = Icons.Default.Close,
                color = TrashRed,
                alpha = indicatorAlpha,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
        
        // Maybe indicator (down swipe) - Amber
        if (swipeDirection == SwipeDirection.DOWN) {
            SwipeIndicatorIcon(
                icon = Icons.Default.QuestionMark,
                color = MaybeAmber,
                alpha = indicatorAlpha,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
    }
}

/**
 * Individual swipe indicator icon (no text label).
 */
@Composable
private fun SwipeIndicatorIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .size(64.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * Format timestamp to readable date and time string.
 */
private fun formatDateTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}
