package com.example.photozen.ui.screens.lighttable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.PhotoEntity

/**
 * Comparison grid layout for Light Table.
 * Arranges 2-4 photos in a synchronized zoom grid.
 * 
 * Layout patterns (auto-adapts based on aspect ratio):
 * - 2 photos: Horizontal (side by side) for portrait, Vertical (stacked) for landscape
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
    
    // Calculate average aspect ratio to determine optimal layout
    val isLandscape = remember(photos) {
        if (photos.isEmpty()) false
        else {
            val avgAspectRatio = photos.map { photo ->
                if (photo.height > 0) photo.width.toFloat() / photo.height else 1f
            }.average()
            avgAspectRatio > 1.0 // Landscape if width > height
        }
    }
    
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
                onSelectPhoto = onSelectPhoto,
                useVerticalLayout = isLandscape
            )
            3 -> ThreePhotoLayout(
                photos = photos.take(3),
                transformState = transformState,
                selectedPhotoId = selectedPhotoId,
                onSelectPhoto = onSelectPhoto,
                useVerticalLayout = isLandscape
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
 * Two photos - side by side (horizontal) or stacked (vertical)
 * @param useVerticalLayout When true, photos are stacked vertically (better for landscape photos)
 */
@Composable
private fun TwoPhotoLayout(
    photos: List<PhotoEntity>,
    transformState: TransformState,
    selectedPhotoId: String?,
    onSelectPhoto: (String) -> Unit,
    useVerticalLayout: Boolean = false
) {
    if (useVerticalLayout) {
        // Vertical layout (stacked) - better for landscape photos
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            photos.forEach { photo ->
                SyncZoomImage(
                    photo = photo,
                    transformState = transformState,
                    isSelected = photo.id == selectedPhotoId,
                    onSelect = { onSelectPhoto(photo.id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    } else {
        // Horizontal layout (side by side) - better for portrait photos
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
                        .fillMaxHeight()
                )
            }
        }
    }
}

/**
 * Three photos - adapts layout based on aspect ratio
 * @param useVerticalLayout When true, uses 1 on left + 2 on right (better for landscape)
 *                          When false, uses 2 on top + 1 on bottom (better for portrait)
 */
@Composable
private fun ThreePhotoLayout(
    photos: List<PhotoEntity>,
    transformState: TransformState,
    selectedPhotoId: String?,
    onSelectPhoto: (String) -> Unit,
    useVerticalLayout: Boolean = false
) {
    if (useVerticalLayout) {
        // Horizontal arrangement: 1 on left, 2 stacked on right (better for landscape photos)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left - 1 photo
            SyncZoomImage(
                photo = photos[0],
                transformState = transformState,
                isSelected = photos[0].id == selectedPhotoId,
                onSelect = { onSelectPhoto(photos[0].id) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            
            // Right - 2 photos stacked
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                photos.drop(1).take(2).forEach { photo ->
                    SyncZoomImage(
                        photo = photo,
                        transformState = transformState,
                        isSelected = photo.id == selectedPhotoId,
                        onSelect = { onSelectPhoto(photo.id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    } else {
        // Vertical arrangement: 2 on top, 1 on bottom (better for portrait photos)
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
                            .fillMaxHeight()
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
                        .fillMaxHeight()
                        .padding(horizontal = 64.dp)
                )
            }
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
