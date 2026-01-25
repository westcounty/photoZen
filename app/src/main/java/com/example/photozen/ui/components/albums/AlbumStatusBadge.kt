package com.example.photozen.ui.components.albums

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.PicZenMotion

/**
 * Album status enumeration
 */
enum class AlbumStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

/**
 * Album status badge component
 *
 * Displays visual status indicator:
 * - NOT_STARTED: Gray hollow circle
 * - IN_PROGRESS: Amber clock icon
 * - COMPLETED: Green checkmark
 *
 * ## Animation Specs
 * - Status change triggers scale animation using playful spring
 *
 * @param status Current album status
 * @param modifier Modifier
 * @param size Badge size (default 24dp)
 */
@Composable
fun AlbumStatusBadge(
    status: AlbumStatus,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    // Status switch scale animation
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = PicZenMotion.Springs.playful(),
        label = "badgeScale"
    )

    // Determine icon and color based on status
    val (icon, tint, backgroundColor) = when (status) {
        AlbumStatus.NOT_STARTED -> Triple(
            Icons.Outlined.Circle,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            Color.Black.copy(alpha = 0.3f)
        )
        AlbumStatus.IN_PROGRESS -> Triple(
            Icons.Default.Schedule,
            MaybeAmber,
            MaybeAmber.copy(alpha = 0.2f)
        )
        AlbumStatus.COMPLETED -> Triple(
            Icons.Default.CheckCircle,
            KeepGreen,
            KeepGreen.copy(alpha = 0.2f)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = when (status) {
                AlbumStatus.NOT_STARTED -> "Not started"
                AlbumStatus.IN_PROGRESS -> "In progress"
                AlbumStatus.COMPLETED -> "Completed"
            },
            modifier = Modifier.size(size * 0.7f),
            tint = tint
        )
    }
}
