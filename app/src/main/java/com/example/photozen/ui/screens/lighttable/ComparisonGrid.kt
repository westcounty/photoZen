package com.example.photozen.ui.screens.lighttable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.PhotoEntity

/**
 * Comparison grid layout for Light Table.
 * Arranges 2-4 photos in a synchronized zoom grid.
 * 
 * Layout patterns:
 * - 2 photos: Side by side
 * - 3 photos: 2 on top, 1 on bottom (centered)
 * - 4 photos: 2x2 grid
 * 
 * @param photos List of photos to compare (max 4)
 * @param transformState Shared transformation state for sync zoom
 * @param selectedPhotoId Currently selected photo ID
 * @param onSelectPhoto Callback when a photo is tapped
 * @param modifier Modifier for the grid
 */
@Composable
fun ComparisonGrid(
    photos: List<PhotoEntity>,
    transformState: TransformState,
    selectedPhotoId: String?,
    onSelectPhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val photoCount = photos.size.coerceAtMost(4)
    
    Box(modifier = modifier.fillMaxSize()) {
        when (photoCount) {
            1 -> SinglePhotoLayout(
                photo = photos[0],
                transformState = transformState,
                isSelected = photos[0].id == selectedPhotoId,
                onSelect = { onSelectPhoto(photos[0].id) }
            )
            2 -> TwoPhotoLayout(
                photos = photos.take(2),
                transformState = transformState,
                selectedPhotoId = selectedPhotoId,
                onSelectPhoto = onSelectPhoto
            )
            3 -> ThreePhotoLayout(
                photos = photos.take(3),
                transformState = transformState,
                selectedPhotoId = selectedPhotoId,
                onSelectPhoto = onSelectPhoto
            )
            4 -> FourPhotoLayout(
                photos = photos.take(4),
                transformState = transformState,
                selectedPhotoId = selectedPhotoId,
                onSelectPhoto = onSelectPhoto
            )
        }
    }
}

/**
 * Single photo - full screen
 */
@Composable
private fun SinglePhotoLayout(
    photo: PhotoEntity,
    transformState: TransformState,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    SyncZoomImage(
        photo = photo,
        transformState = transformState,
        isSelected = isSelected,
        onSelect = onSelect,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    )
}

/**
 * Two photos - side by side
 */
@Composable
private fun TwoPhotoLayout(
    photos: List<PhotoEntity>,
    transformState: TransformState,
    selectedPhotoId: String?,
    onSelectPhoto: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        photos.forEach { photo ->
            SyncZoomImage(
                photo = photo,
                transformState = transformState,
                isSelected = photo.id == selectedPhotoId,
                onSelect = { onSelectPhoto(photo.id) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
        }
    }
}

/**
 * Three photos - 2 on top, 1 on bottom centered
 */
@Composable
private fun ThreePhotoLayout(
    photos: List<PhotoEntity>,
    transformState: TransformState,
    selectedPhotoId: String?,
    onSelectPhoto: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top row - 2 photos
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            photos.take(2).forEach { photo ->
                SyncZoomImage(
                    photo = photo,
                    transformState = transformState,
                    isSelected = photo.id == selectedPhotoId,
                    onSelect = { onSelectPhoto(photo.id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }
        }
        
        // Bottom row - 1 photo centered
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SyncZoomImage(
                photo = photos[2],
                transformState = transformState,
                isSelected = photos[2].id == selectedPhotoId,
                onSelect = { onSelectPhoto(photos[2].id) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 64.dp)
            )
        }
    }
}

/**
 * Four photos - 2x2 grid
 */
@Composable
private fun FourPhotoLayout(
    photos: List<PhotoEntity>,
    transformState: TransformState,
    selectedPhotoId: String?,
    onSelectPhoto: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top row
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            photos.take(2).forEach { photo ->
                SyncZoomImage(
                    photo = photo,
                    transformState = transformState,
                    isSelected = photo.id == selectedPhotoId,
                    onSelect = { onSelectPhoto(photo.id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }
        }
        
        // Bottom row
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            photos.drop(2).take(2).forEach { photo ->
                SyncZoomImage(
                    photo = photo,
                    transformState = transformState,
                    isSelected = photo.id == selectedPhotoId,
                    onSelect = { onSelectPhoto(photo.id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }
        }
    }
}
