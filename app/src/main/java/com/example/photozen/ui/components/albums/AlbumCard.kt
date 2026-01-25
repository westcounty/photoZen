package com.example.photozen.ui.components.albums

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.ui.screens.albums.AlbumBubbleData
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.theme.PicZenTokens
import kotlinx.coroutines.delay

/**
 * Album card component - Core component for grid layout
 *
 * Modern card design with cover image, progress ring, and status badge.
 * References Google Photos and Apple Photos design language.
 *
 * ## Design Specs
 * | Property | Value | Description |
 * |----------|-------|-------------|
 * | Aspect Ratio | 1:1 | Square card |
 * | Corner Radius | 16dp (L) | Modern feel |
 * | Shadow-Default | Level2 (3dp) | Moderate depth |
 * | Shadow-Pressed | Level1 (1dp) | Compressed feedback |
 * | Shadow-Dragging | Level4 (8dp) | Floating effect |
 * | Press Scale | 0.96f | Noticeable but not exaggerated |
 * | Long Press Scale | 0.94f | Indicates draggable |
 *
 * ## Visual Layers
 * ```
 * ┌─────────────────────────────┐
 * │  [Status]       [Progress]  │  ← Top info layer (z-index: 3)
 * │                             │
 * │     Cover Photo             │  ← Background layer (z-index: 1)
 * │     (ContentScale.Crop)     │    + Vignette gradient
 * │                             │
 * ├─────────────────────────────┤
 * │ ▓▓▓▓ Frosted glass ▓▓▓▓▓▓▓ │  ← Bottom info layer (z-index: 2)
 * │  Album Name                 │    height: 56dp
 * │  128 photos                 │
 * └─────────────────────────────┘
 * ```
 *
 * @param album Album data including stats
 * @param onClick Click callback
 * @param onLongClick Long click callback
 * @param modifier Modifier
 * @param animationDelay Entry animation delay in ms (for stagger effect)
 * @param isDragging Whether the card is being dragged
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumCard(
    album: AlbumBubbleData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationDelay: Int = 0,
    isDragging: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Entry animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    // === Animation values ===
    // Entry animation
    val entryAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Moderate,
            easing = PicZenMotion.Easing.EmphasizedDecelerate
        ),
        label = "entryAlpha"
    )
    val entryScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = PicZenMotion.Springs.playful(),
        label = "entryScale"
    )
    val entryOffsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 100.dp,
        animationSpec = PicZenMotion.Springs.playful(),
        label = "entryOffsetY"
    )

    // Press/drag scale
    val pressScale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.02f
            isPressed -> 0.96f
            else -> 1f
        },
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "pressScale"
    )

    // Shadow
    val shadowElevation by animateDpAsState(
        targetValue = when {
            isDragging -> PicZenTokens.Elevation.Level4
            isPressed -> PicZenTokens.Elevation.Level1
            else -> PicZenTokens.Elevation.Level2
        },
        animationSpec = tween(PicZenMotion.Duration.Quick),
        label = "shadowElevation"
    )

    // Progress percentage
    val progress = if (album.totalCount > 0) {
        album.sortedCount.toFloat() / album.totalCount
    } else 0f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                alpha = entryAlpha
                scaleX = entryScale * pressScale
                scaleY = entryScale * pressScale
                translationY = entryOffsetY.toPx()
            }
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(PicZenTokens.Radius.L),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(PicZenTokens.Radius.L))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        // === Layer 1: Cover photo ===
        if (album.coverUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(album.coverUri))
                    .crossfade(true)
                    .build(),
                contentDescription = album.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder icon
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoAlbum,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // === Layer 2: Vignette gradient overlay ===
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // === Layer 3: Top info (status badge + progress ring) ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PicZenTokens.Spacing.S),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Top left: Status badge
            AlbumStatusBadge(
                status = when {
                    progress >= 1f -> AlbumStatus.COMPLETED
                    progress > 0f -> AlbumStatus.IN_PROGRESS
                    else -> AlbumStatus.NOT_STARTED
                }
            )

            // Top right: Progress ring
            AlbumProgressRing(
                progress = progress,
                size = 36.dp,
                strokeWidth = 3.dp
            )
        }

        // === Layer 4: Bottom info layer (frosted glass effect) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.BottomCenter)
                .background(
                    // Frosted glass effect simulation (real blur requires API 31+)
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
                .padding(horizontal = PicZenTokens.Spacing.M)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = album.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${formatNumber(album.totalCount)} photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Format number for display
 * Shows k/w suffix for large numbers
 */
private fun formatNumber(num: Int): String {
    return when {
        num >= 10000 -> String.format("%.1fw", num / 10000.0)
        num >= 1000 -> String.format("%.1fk", num / 1000.0)
        else -> num.toString()
    }
}
