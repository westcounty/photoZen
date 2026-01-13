package com.example.photozen.ui.screens.lighttable

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber

/**
 * Grid of photo thumbnails for selecting photos to compare.
 * Supports multi-select (up to 4 photos).
 * 
 * @param photos List of "Maybe" photos to display
 * @param selectedIds Set of currently selected photo IDs
 * @param onToggleSelection Callback when a photo is tapped
 * @param modifier Modifier for the grid
 */
@Composable
fun PhotoThumbnailGrid(
    photos: List<PhotoEntity>,
    selectedIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = photos,
            key = { it.id }
        ) { photo ->
            PhotoThumbnail(
                photo = photo,
                isSelected = photo.id in selectedIds,
                selectionIndex = selectedIds.toList().indexOf(photo.id).let { 
                    if (it >= 0) it + 1 else null 
                },
                onToggle = { onToggleSelection(photo.id) }
            )
        }
    }
}

/**
 * Single photo thumbnail with selection state.
 */
@Composable
private fun PhotoThumbnail(
    photo: PhotoEntity,
    isSelected: Boolean,
    selectionIndex: Int?,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaybeAmber,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable { onToggle() }
    ) {
        // Thumbnail image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(photo.systemUri))
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Selection indicator with number
        if (isSelected && selectionIndex != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaybeAmber),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = selectionIndex.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black
                )
            }
        }
    }
}
