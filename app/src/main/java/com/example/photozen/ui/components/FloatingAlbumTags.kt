package com.example.photozen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photozen.data.local.entity.AlbumBubbleEntity

/**
 * Floating album tags displayed at the bottom of card sorting screens.
 * Allows quick photo assignment to albums during sorting.
 * 
 * Tags are displayed floating on the photo, in a FlowRow layout from bottom up.
 * No background, no hint text, no icons - just clean pill buttons with album names.
 * 
 * @param albums List of albums from the album bubble list
 * @param tagSize Scale factor for tag size (0.6 - 1.5)
 * @param maxCount Maximum number of albums to display (0 = unlimited)
 * @param onAlbumClick Callback when an album is clicked
 * @param onAddAlbumClick Optional callback when "Add Album" button is clicked (shows edit dialog)
 * @param visible Whether the floating tags are visible
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingAlbumTags(
    albums: List<AlbumBubbleEntity>,
    tagSize: Float = 1.0f,
    maxCount: Int = 0,
    onAlbumClick: (AlbumBubbleEntity) -> Unit,
    onAddAlbumClick: (() -> Unit)? = null,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val displayAlbums = if (maxCount > 0) {
        albums.take(maxCount)
    } else {
        albums
    }
    
    // Show component if there are albums OR if add button is enabled
    val shouldShow = visible && (displayAlbums.isNotEmpty() || onAddAlbumClick != null)
    
    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        // FlowRow to wrap tags from bottom, no background
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            // Fix: Use CenterVertically for proper vertical alignment of all items
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            displayAlbums.forEach { album ->
                FloatingAlbumTag(
                    album = album,
                    tagSize = tagSize,
                    onClick = { onAlbumClick(album) }
                )
            }
            
            // Add album button at the end
            if (onAddAlbumClick != null) {
                AddAlbumButton(
                    tagSize = tagSize,
                    onClick = onAddAlbumClick
                )
            }
        }
    }
}

/**
 * Individual album tag - clean pill with just the album name.
 */
@Composable
private fun FloatingAlbumTag(
    album: AlbumBubbleEntity,
    tagSize: Float,
    onClick: () -> Unit
) {
    // Scale text size based on tagSize setting (more noticeable range)
    val baseTextSize = 14.sp
    val scaledTextSize = baseTextSize * tagSize
    val basePaddingHorizontal = 16.dp
    val basePaddingVertical = 10.dp
    val scaledPaddingHorizontal = basePaddingHorizontal * tagSize
    val scaledPaddingVertical = basePaddingVertical * tagSize
    
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape((24 * tagSize).dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        shadowElevation = 4.dp
    ) {
        // Just the album name, no icon
        Text(
            text = album.displayName,
            fontSize = scaledTextSize,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                horizontal = scaledPaddingHorizontal,
                vertical = scaledPaddingVertical
            )
        )
    }
}

/**
 * Add Album button - shows at the end of floating tags.
 * Opens the album picker dialog when clicked.
 * 
 * Fixed: Use same padding as album tags for proper vertical alignment.
 */
@Composable
private fun AddAlbumButton(
    tagSize: Float,
    onClick: () -> Unit
) {
    val baseIconSize = 18.dp
    val scaledIconSize = baseIconSize * tagSize
    // Fix: Use same vertical padding as album tags for proper alignment
    val basePaddingHorizontal = 14.dp
    val basePaddingVertical = 10.dp
    val scaledPaddingHorizontal = basePaddingHorizontal * tagSize
    val scaledPaddingVertical = basePaddingVertical * tagSize
    
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape((24 * tagSize).dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = scaledPaddingHorizontal,
                vertical = scaledPaddingVertical
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加相册",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(scaledIconSize)
            )
        }
    }
}
