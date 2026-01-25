package com.example.photozen.ui.components

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Staggered (waterfall) photo grid that preserves original aspect ratios.
 *
 * Features:
 * - Supports 1-5 column layouts
 * - Photos displayed in their original aspect ratio
 * - Optional selection mode with checkmarks
 * - Long press and click callbacks
 *
 * @param columns Number of columns (1-5), defaults to 2
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StaggeredPhotoGrid(
    photos: List<PhotoEntity>,
    onPhotoClick: (String, Int) -> Unit,
    onPhotoLongPress: (String, String) -> Unit, // photoId, photoUri
    modifier: Modifier = Modifier,
    columns: Int = 2,
    selectedIds: Set<String> = emptySet(),
    selectionMode: Boolean = false
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns.coerceIn(1, 5)),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
            StaggeredPhotoItem(
                photo = photo,
                isSelected = selectedIds.contains(photo.id),
                selectionMode = selectionMode,
                onClick = { onPhotoClick(photo.id, index) },
                onLongPress = { onPhotoLongPress(photo.id, photo.systemUri) }
            )
        }
    }
}

/**
 * Single photo item in the staggered grid.
 * Calculates aspect ratio from photo dimensions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StaggeredPhotoItem(
    photo: PhotoEntity,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    // Calculate aspect ratio from photo dimensions
    val aspectRatio = remember(photo.width, photo.height) {
        if (photo.width > 0 && photo.height > 0) {
            photo.width.toFloat() / photo.height.toFloat()
        } else {
            1f // Default to square if dimensions unknown
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(photo.systemUri))
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        )
        
        // Selection overlay
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        } else {
                            Color.Transparent
                        }
                    )
            )
            
            // Selection indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White.copy(alpha = 0.7f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已选择",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Staggered photo grid with selection support for batch operations.
 * Supports 1-5 column layouts with drag-to-select functionality.
 * 
 * Features:
 * - Long press to enter selection mode
 * - Drag to select multiple photos
 * - Auto-scroll when dragging near edges
 * - Haptic feedback on selection changes
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableStaggeredPhotoGrid(
    photos: List<PhotoEntity>,
    selectedIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onPhotoClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onPhotoLongClick: ((String, Int) -> Unit)? = null  // Optional callback for context menu
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    
    // Grid size for auto-scroll calculation
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Drag selection state
    var isDragging by remember { mutableStateOf(false) }
    var dragStartIndex by remember { mutableStateOf(-1) }
    var dragCurrentIndex by remember { mutableStateOf(-1) }
    var initialSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Auto-scroll state
    var autoScrollDirection by remember { mutableStateOf(0) } // -1: up, 0: none, 1: down
    
    // Auto-scroll threshold in pixels
    val scrollThreshold = with(density) { 80.dp.toPx() }
    val scrollSpeed = 10f
    
    // Auto-scroll effect
    if (autoScrollDirection != 0 && isDragging) {
        coroutineScope.launch {
            while (isActive && autoScrollDirection != 0 && isDragging) {
                gridState.animateScrollBy(scrollSpeed * autoScrollDirection)
                delay(16) // ~60fps
            }
        }
    }
    
    // Helper function to get item index at position
    fun getItemIndexAtPosition(position: Offset): Int {
        val visibleItems = gridState.layoutInfo.visibleItemsInfo
        for (itemInfo in visibleItems) {
            val itemLeft = itemInfo.offset.x
            val itemTop = itemInfo.offset.y
            val itemRight = itemLeft + itemInfo.size.width
            val itemBottom = itemTop + itemInfo.size.height
            
            if (position.x >= itemLeft && position.x <= itemRight &&
                position.y >= itemTop && position.y <= itemBottom) {
                return itemInfo.index
            }
        }
        return -1
    }
    
    // Helper function to select range between two indices
    fun selectRange(startIndex: Int, endIndex: Int): Set<String> {
        if (startIndex < 0 || endIndex < 0) return initialSelection
        
        val minIndex = minOf(startIndex, endIndex)
        val maxIndex = maxOf(startIndex, endIndex)
        
        val rangeIds = photos
            .filterIndexed { index, _ -> index in minIndex..maxIndex }
            .map { it.id }
            .toSet()
        
        return initialSelection + rangeIds
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { gridSize = it }
            .pointerInput(photos, selectedIds) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val index = getItemIndexAtPosition(offset)
                        if (index >= 0 && index < photos.size) {
                            isDragging = true
                            dragStartIndex = index
                            dragCurrentIndex = index
                            initialSelection = selectedIds
                            
                            // Add initial item to selection
                            val photoId = photos[index].id
                            val newSelection = if (selectedIds.contains(photoId)) {
                                selectedIds
                            } else {
                                selectedIds + photoId
                            }
                            onSelectionChanged(newSelection)
                            
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { change, _ ->
                        if (!isDragging) return@detectDragGesturesAfterLongPress
                        
                        change.consume()
                        val position = change.position
                        
                        // Check for auto-scroll
                        autoScrollDirection = when {
                            position.y < scrollThreshold -> -1
                            position.y > gridSize.height - scrollThreshold -> 1
                            else -> 0
                        }
                        
                        // Find item at current position
                        val currentIndex = getItemIndexAtPosition(position)
                        if (currentIndex >= 0 && currentIndex != dragCurrentIndex) {
                            dragCurrentIndex = currentIndex
                            
                            // Select range from start to current
                            val newSelection = selectRange(dragStartIndex, dragCurrentIndex)
                            if (newSelection != selectedIds) {
                                onSelectionChanged(newSelection)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        autoScrollDirection = 0
                        dragStartIndex = -1
                        dragCurrentIndex = -1
                    },
                    onDragCancel = {
                        isDragging = false
                        autoScrollDirection = 0
                        dragStartIndex = -1
                        dragCurrentIndex = -1
                    }
                )
            }
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns.coerceIn(1, 5)),
            state = gridState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                val isSelected = selectedIds.contains(photo.id)
                val selectionMode = selectedIds.isNotEmpty() || isDragging
                val isInDragRange = isDragging && 
                    dragStartIndex >= 0 && 
                    dragCurrentIndex >= 0 &&
                    index in minOf(dragStartIndex, dragCurrentIndex)..maxOf(dragStartIndex, dragCurrentIndex)
                
                SelectableStaggeredPhotoItem(
                    photo = photo,
                    isSelected = isSelected || isInDragRange,
                    selectionMode = selectionMode,
                    onClick = {
                        if (selectionMode && !isDragging) {
                            // Toggle selection
                            val newSelection = if (isSelected) {
                                selectedIds - photo.id
                            } else {
                                selectedIds + photo.id
                            }
                            onSelectionChanged(newSelection)
                        } else if (!isDragging) {
                            onPhotoClick(photo.id, index)
                        }
                    },
                    onLongPress = {
                        // Long press without drag
                        if (!isDragging) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // If custom callback provided, use it; otherwise enter selection mode
                            if (onPhotoLongClick != null && !selectionMode) {
                                onPhotoLongClick(photo.id, index)
                            } else if (!selectionMode) {
                                onSelectionChanged(setOf(photo.id))
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableStaggeredPhotoItem(
    photo: PhotoEntity,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    val aspectRatio = remember(photo.width, photo.height) {
        if (photo.width > 0 && photo.height > 0) {
            photo.width.toFloat() / photo.height.toFloat()
        } else {
            1f
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(photo.systemUri))
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        )
        
        // Selection overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
        }
        
        // Selection indicator
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Black.copy(alpha = 0.5f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
