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
import com.example.photozen.util.ThumbnailSizePolicy
import com.example.photozen.util.withThumbnailPolicy
import com.example.photozen.ui.components.EdgeGlowOverlay
import com.example.photozen.ui.components.GlowingDirectionIndicator
import com.example.photozen.ui.components.SwipeIndicatorDirection
import com.example.photozen.util.OfflineGeocoder
import kotlin.math.abs
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
 * @param showInfoOnImage When true, photo info is shown on the TOP-LEFT of the image with compact layout:
 *                        - Line 1: File name
 *                        - Line 2: Date/Time, Resolution, File Size, Location (smaller text)
 *                        The zoom icon stays at TOP-RIGHT. Used when album tags are displayed at bottom
 *                        to avoid overlapping.
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
                // Phase 4: 使用 ThumbnailSizePolicy 优化图片加载
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(photo.systemUri))
                        .memoryCacheKey(photo.id) // Use photo.id for better cache hits
                        .diskCacheKey(photo.id) // Also use for disk cache
                        .memoryCachePolicy(CachePolicy.ENABLED) // Prioritize memory cache
                        .diskCachePolicy(CachePolicy.ENABLED) // Use disk cache as fallback
                        .placeholderMemoryCacheKey(photo.id) // Use preloaded image as placeholder
                        .withThumbnailPolicy(ThumbnailSizePolicy.Context.CARD_PREVIEW) // Phase 4: 优化缩略图大小
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
                
                // When showInfoOnImage is true, display info at the TOP-LEFT of the IMAGE
                // This ensures info doesn't overlap with bottom album tags
                // Zoom icon stays at TOP-RIGHT, info is positioned to avoid overlap
                if (showInfoOnImage) {
                    // Gradient overlay at the TOP of the IMAGE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    // Info text at the top-left of the IMAGE (leave space for zoom icon on right)
                    PhotoInfoOverlayCompact(
                        photo = photo,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 10.dp, end = 52.dp) // Leave space for zoom icon
                    )
                }
                
                // Tap hint badge - Always at TOP-RIGHT
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
            
            // 边缘发光效果 (Phase 3-6)
            EdgeGlowOverlay(
                swipeProgressX = swipeProgress,
                swipeProgressY = swipeProgressY,
                hasReachedThreshold = hasReachedThreshold,
                modifier = Modifier.fillMaxSize()
            )
            
            // Swipe indicator overlays (增强版)
            EnhancedSwipeIndicatorOverlay(
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
 * Compact photo info overlay for top-left display when album tags are at bottom.
 * Two-line layout:
 * - Line 1: File name (medium weight)
 * - Line 2: Date/Time, Resolution, File Size, Location (smaller text, spaced)
 * 
 * Designed to coexist with zoom icon at top-right.
 */
@Composable
private fun PhotoInfoOverlayCompact(
    photo: PhotoEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val offlineGeocoder = remember { OfflineGeocoder(context) }
    var locationText by remember { mutableStateOf<String?>(null) }
    
    // Load location text asynchronously
    LaunchedEffect(photo.id, photo.latitude, photo.longitude, photo.gpsScanned) {
        locationText = when {
            photo.latitude != null && photo.longitude != null -> {
                offlineGeocoder.getLocationText(photo.latitude, photo.longitude)
            }
            !photo.gpsScanned -> {
                offlineGeocoder.getLocationTextFromUri(photo.systemUri)
            }
            else -> null
        }
    }
    
    Column(modifier = modifier) {
        // Line 1: File name
        Text(
            text = photo.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Line 2: Date/Time, Resolution, File Size, Location (all in one row, smaller text)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date and Time
            val displayTime = when {
                photo.dateTaken > 0 -> photo.dateTaken
                photo.dateAdded > 0 -> photo.dateAdded * 1000
                else -> null
            }
            displayTime?.let { timestamp ->
                Text(
                    text = formatDateTime(timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            
            // Resolution
            if (photo.width > 0 && photo.height > 0) {
                Text(
                    text = "${photo.width}×${photo.height}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            
            // File size
            if (photo.size > 0) {
                val sizeMB = photo.size / (1024.0 * 1024.0)
                Text(
                    text = String.format("%.1fMB", sizeMB),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            
            // Location (with ellipsis if too long)
            locationText?.let { location ->
                Text(
                    text = location,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

/**
 * 增强版滑动方向指示器 (Phase 3-6)
 * 
 * 使用 GlowingDirectionIndicator 替代原来的简单图标，特性：
 * - 带发光光晕效果
 * - 脉冲动画（到达阈值时）
 * - 平滑的进入/退出动画
 * 
 * Gesture mapping:
 * - LEFT/RIGHT → Keep (Green) - Heart icons
 * - UP → Trash (Red) - Delete icons  
 * - DOWN → Maybe (Amber) - Help icons
 */
@Composable
private fun EnhancedSwipeIndicatorOverlay(
    swipeDirection: SwipeDirection,
    swipeProgressX: Float,
    swipeProgressY: Float,
    hasReachedThreshold: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 转换 SwipeDirection 为 SwipeIndicatorDirection
    val indicatorDirection = when (swipeDirection) {
        SwipeDirection.LEFT -> SwipeIndicatorDirection.LEFT
        SwipeDirection.RIGHT -> SwipeIndicatorDirection.RIGHT
        SwipeDirection.UP -> SwipeIndicatorDirection.UP
        SwipeDirection.DOWN -> SwipeIndicatorDirection.DOWN
        SwipeDirection.NONE -> SwipeIndicatorDirection.NONE
    }
    
    // 计算进度
    val progress = when (indicatorDirection) {
        SwipeIndicatorDirection.LEFT, SwipeIndicatorDirection.RIGHT -> abs(swipeProgressX)
        SwipeIndicatorDirection.UP, SwipeIndicatorDirection.DOWN -> abs(swipeProgressY)
        SwipeIndicatorDirection.NONE -> 0f
    }
    
    Box(modifier = modifier) {
        // 使用发光方向指示器
        if (indicatorDirection != SwipeIndicatorDirection.NONE || progress > 0.1f) {
            // 根据实际进度选择显示的方向
            val displayDirection = when {
                indicatorDirection != SwipeIndicatorDirection.NONE -> indicatorDirection
                swipeProgressX > 0.1f -> SwipeIndicatorDirection.RIGHT
                swipeProgressX < -0.1f -> SwipeIndicatorDirection.LEFT
                swipeProgressY < -0.1f -> SwipeIndicatorDirection.UP
                swipeProgressY > 0.1f -> SwipeIndicatorDirection.DOWN
                else -> SwipeIndicatorDirection.NONE
            }
            
            val displayProgress = when (displayDirection) {
                SwipeIndicatorDirection.LEFT, SwipeIndicatorDirection.RIGHT -> abs(swipeProgressX)
                SwipeIndicatorDirection.UP, SwipeIndicatorDirection.DOWN -> abs(swipeProgressY)
                SwipeIndicatorDirection.NONE -> 0f
            }
            
            if (displayDirection != SwipeIndicatorDirection.NONE) {
                GlowingDirectionIndicator(
                    direction = displayDirection,
                    progress = displayProgress,
                    hasReachedThreshold = hasReachedThreshold,
                    size = 64.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 72.dp)
                )
            }
        }
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
