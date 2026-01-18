package com.example.photozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * Photo status badge component - displays a small circular badge indicating photo status.
 * 
 * Position: Top-left corner of the photo
 * Style: Small circular badge with color-coded status:
 *   - KEEP → Green ✓
 *   - MAYBE → Amber ?
 *   - TRASH → Red ✗
 *   - UNSORTED → Not displayed (returns null)
 *
 * @param status The photo status to display
 * @param size The size of the badge (default: 24.dp for grid view, use 32.dp for fullscreen)
 * @param showForUnsorted Whether to show badge for UNSORTED status (default: false)
 * @param modifier Modifier for positioning
 */
@Composable
fun PhotoStatusBadge(
    status: PhotoStatus,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    showForUnsorted: Boolean = false
) {
    // Don't display badge for UNSORTED status unless explicitly requested
    if (status == PhotoStatus.UNSORTED && !showForUnsorted) {
        return
    }
    
    val (backgroundColor, icon, iconTint) = when (status) {
        PhotoStatus.KEEP -> Triple(KeepGreen, Icons.Default.Check, Color.White)
        PhotoStatus.MAYBE -> Triple(MaybeAmber, Icons.Default.QuestionMark, Color.White)
        PhotoStatus.TRASH -> Triple(TrashRed, Icons.Default.Close, Color.White)
        PhotoStatus.UNSORTED -> Triple(Color.Gray.copy(alpha = 0.6f), null, Color.White)
    }
    
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 2.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = status.name,
                tint = iconTint,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

/**
 * Convenience composable that wraps content with a status badge overlay.
 * 
 * @param status The photo status
 * @param badgeSize Size of the badge
 * @param showBadge Whether to show the badge at all
 * @param content The content to overlay with the badge
 */
@Composable
fun PhotoWithStatusBadge(
    status: PhotoStatus,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 24.dp,
    showBadge: Boolean = true,
    badgePadding: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        
        if (showBadge && status != PhotoStatus.UNSORTED) {
            PhotoStatusBadge(
                status = status,
                size = badgeSize,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(badgePadding)
            )
        }
    }
}
