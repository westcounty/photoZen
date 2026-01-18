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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.HelpOutline
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
import coil3.request.CachePolicy
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
 * @param hasReachedThreshold Whether the swipe has reached the action threshold
 * @param onPhotoClick Called when photo is clicked (for fullscreen view)
 * @param showInfoOnImage When true, photo info is shown on the image itself (bottom-left corner)
 *                        instead of at the card bottom. Used when album tags are displayed at bottom.
 * @param modifier Modifier for the card
 */
@Composable
fun PhotoCard(
    photo: PhotoEntity,
    swipeProgress: Float = 0f,
    swipeProgressY: Float = 0f,
    swipeDirection: SwipeDirection = SwipeDirection.NONE,
    hasReachedThreshold: Boolean = false,
    onPhotoClick: (() -> Unit)? = null,
    showInfoOnImage: Boolean = false,
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
                // Use AsyncImage WITHOUT conditional rendering to prevent flash on recomposition
                // The background is always visible, and the image renders on top when ready
                // This eliminates the flash when switching cards
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(photo.systemUri))
                        .memoryCacheKey(photo.id) // Use photo.id for better cache hits
                        .diskCacheKey(photo.id) // Also use for disk cache
                        .memoryCachePolicy(CachePolicy.ENABLED) // Prioritize memory cache
                        .diskCachePolicy(CachePolicy.ENABLED) // Use disk cache as fallback
                        .placeholderMemoryCacheKey(photo.id) // Use preloaded image as placeholder
                        .build(),
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio.coerceIn(0.3f, 3f))
                        .clip(RoundedCornerShape(16.dp))
                        // Background is ALWAYS visible - prevents flash during state transitions
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                
                // When showInfoOnImage is true, display info on the IMAGE itself (not card)
                // This ensures info is positioned relative to the photo, not the entire card
                if (showInfoOnImage) {
                    // Gradient overlay at the bottom of the IMAGE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                    
                    // Info text at the bottom-left of the IMAGE
                    PhotoInfoOverlay(
                        photo = photo,
                        compact = true,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 8.dp, end = 12.dp)
                    )
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
            
            // Photo info display - default position (when showInfoOnImage is false)
            if (!showInfoOnImage) {
                // Default: info at card bottom with surface-colored gradient
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
                
                PhotoInfoOverlay(
                    photo = photo,
                    compact = false,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                )
            }
            
            // Swipe indicator overlays
            SwipeIndicatorOverlay(
                swipeDirection = swipeDirection,
                swipeProgressX = swipeProgress,
                swipeProgressY = swipeProgressY,
                hasReachedThreshold = hasReachedThreshold,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Displays photo metadata at the bottom of the card.
 * @param compact When true, uses smaller text and white color (for overlay on image)
 */
@Composable
private fun PhotoInfoOverlay(
    photo: PhotoEntity,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val offlineGeocoder = remember { OfflineGeocoder(context) }
    var locationText by remember { mutableStateOf<String?>(null) }
    
    // Load location text asynchronously
    // If photo has GPS coordinates, use them directly
    // Otherwise, if GPS hasn't been scanned yet, try to read from EXIF lazily
    LaunchedEffect(photo.id, photo.latitude, photo.longitude, photo.gpsScanned) {
        locationText = when {
            // Photo already has GPS coordinates
            photo.latitude != null && photo.longitude != null -> {
                offlineGeocoder.getLocationText(photo.latitude, photo.longitude)
            }
            // GPS not yet scanned - try to read from EXIF lazily
            !photo.gpsScanned -> {
                offlineGeocoder.getLocationTextFromUri(photo.systemUri)
            }
            // GPS was scanned but no data found
            else -> null
        }
    }
    
    // Text colors based on compact mode
    val primaryTextColor = if (compact) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (compact) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(modifier = modifier) {
        // File name
        Text(
            text = photo.displayName,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = primaryTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
        
        // Date/Time and Location row
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp)
        ) {
            // Date and Time (combined)
            // Prefer dateTaken (EXIF), fallback to dateAdded (file creation time)
            val displayTime = when {
                photo.dateTaken > 0 -> photo.dateTaken
                photo.dateAdded > 0 -> photo.dateAdded * 1000  // dateAdded is in seconds, convert to millis
                else -> null
            }
            displayTime?.let { timestamp ->
                Text(
                    text = formatDateTime(timestamp),
                    style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
            }
            
            // Dimensions (compact: show here instead of separate row)
            if (compact && photo.width > 0 && photo.height > 0) {
                Text(
                    text = "${photo.width}×${photo.height}",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor
                )
            }
            
            // File size (in MB, one decimal place) - compact mode
            if (compact && photo.size > 0) {
                val sizeMB = photo.size / (1024.0 * 1024.0)
                Text(
                    text = String.format("%.1fMB", sizeMB),
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor
                )
            }
            
            // Location - show in both compact and non-compact modes
            locationText?.let { location ->
                Text(
                    text = location,
                    style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Dimensions, File size, and Camera row (only in non-compact mode)
        if (!compact) {
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dimensions
                if (photo.width > 0 && photo.height > 0) {
                    Text(
                        text = "${photo.width} × ${photo.height}",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }
                
                // File size (in MB, one decimal place)
                if (photo.size > 0) {
                    val sizeMB = photo.size / (1024.0 * 1024.0)
                    Text(
                        text = String.format("%.1f MB", sizeMB),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }
                
                // Camera model
                photo.cameraModel?.let { model ->
                    Text(
                        text = model,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }
            }
        }
    }
}

/**
 * Displays swipe direction indicators with animated opacity.
 * All indicators are centered at top of the photo for better visibility.
 * Icon only, no text label.
 * Shows different icons based on whether threshold has been reached.
 * 
 * Gesture mapping:
 * - LEFT/RIGHT → Keep (Green) - Heart icons
 * - UP → Trash (Red) - Delete icons
 * - DOWN → Maybe (Amber) - Help icons
 */
@Composable
private fun SwipeIndicatorOverlay(
    swipeDirection: SwipeDirection,
    swipeProgressX: Float,
    swipeProgressY: Float,
    hasReachedThreshold: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Consistent alpha for all indicators (0.85 = clearly visible)
    val indicatorAlpha = 0.85f
    
    // Scale animation when reaching threshold
    val iconScale by animateFloatAsState(
        targetValue = if (hasReachedThreshold) 1.2f else 1.0f,
        label = "iconScale"
    )
    
    Box(modifier = modifier) {
        // Keep indicator (right swipe) - Green with heart icon
        if (swipeDirection == SwipeDirection.RIGHT || (swipeDirection == SwipeDirection.NONE && swipeProgressX > 0.1f)) {
            SwipeIndicatorIcon(
                icon = if (hasReachedThreshold) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                color = KeepGreen,
                alpha = indicatorAlpha,
                scale = iconScale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
        
        // Keep indicator (left swipe) - Green (same as right)
        if (swipeDirection == SwipeDirection.LEFT || (swipeDirection == SwipeDirection.NONE && swipeProgressX < -0.1f)) {
            SwipeIndicatorIcon(
                icon = if (hasReachedThreshold) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                color = KeepGreen,
                alpha = indicatorAlpha,
                scale = iconScale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
        
        // Trash indicator (up swipe) - Red with delete icon
        if (swipeDirection == SwipeDirection.UP) {
            SwipeIndicatorIcon(
                icon = if (hasReachedThreshold) Icons.Default.Delete else Icons.Default.DeleteOutline,
                color = TrashRed,
                alpha = indicatorAlpha,
                scale = iconScale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
        
        // Maybe indicator (down swipe) - Amber with help icon
        if (swipeDirection == SwipeDirection.DOWN) {
            SwipeIndicatorIcon(
                icon = if (hasReachedThreshold) Icons.Default.Help else Icons.Default.HelpOutline,
                color = MaybeAmber,
                alpha = indicatorAlpha,
                scale = iconScale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
    }
}

/**
 * Individual swipe indicator icon (no text label).
 * Supports scale animation for threshold feedback.
 */
@Composable
private fun SwipeIndicatorIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    scale: Float = 1.0f,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val iconSize = (64 * scale).dp
    val innerIconSize = (32 * scale).dp
    
    Box(
        modifier = modifier
            .graphicsLayer { 
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .size(iconSize)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(innerIconSize)
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
