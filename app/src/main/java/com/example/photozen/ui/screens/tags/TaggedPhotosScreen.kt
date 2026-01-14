@file:OptIn(ExperimentalFoundationApi::class)

package com.example.photozen.ui.screens.tags

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.TagEntity
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Screen for displaying photos that have a specific tag.
 * 
 * Features:
 * - Grid view of tagged photos
 * - Option to view on map
 * - Navigation to photo editor
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaggedPhotosScreen(
    tagId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaggedPhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenStartIndex by remember { mutableIntStateOf(0) }
    var selectedPhotoId by remember { mutableStateOf<String?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showChangeTagDialog by remember { mutableStateOf(false) }
    
    // Load photos for this tag
    androidx.compose.runtime.LaunchedEffect(tagId) {
        viewModel.loadPhotosForTag(tagId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.tagName ?: "标签照片",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (uiState.photos.isNotEmpty()) {
                            Text(
                                text = "${uiState.photos.size} 张照片",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.photos.isEmpty() -> {
                    EmptyContent(tagName = uiState.tagName ?: "该标签")
                }
                else -> {
                    PhotoGrid(
                        photos = uiState.photos,
                        onPhotoClick = { photoId, index ->
                            fullscreenStartIndex = index
                            showFullscreen = true
                        },
                        onPhotoLongPress = { photoId ->
                            selectedPhotoId = photoId
                            showActionDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showFullscreen && uiState.photos.isNotEmpty()) {
        BackHandler { showFullscreen = false }
        TaggedPhotosFullscreenViewer(
            photos = uiState.photos,
            initialIndex = fullscreenStartIndex,
            onDismiss = { showFullscreen = false }
        )
    }

    if (showActionDialog && selectedPhotoId != null) {
        val photoId = selectedPhotoId!!
        ActionSheetDialog(
            onDismiss = { showActionDialog = false },
            onEdit = {
                showActionDialog = false
                selectedPhotoId = null
                onNavigateToEditor(photoId)
            },
            onRemoveTag = {
                viewModel.removeTagFromPhoto(photoId)
                showActionDialog = false
                selectedPhotoId = null
            },
            onChangeTag = {
                showActionDialog = false
                showChangeTagDialog = true
            }
        )
    }

    if (showChangeTagDialog) {
        ChangeTagDialog(
            currentTagId = uiState.tagId,
            tags = uiState.allTags,
            onDismiss = {
                showChangeTagDialog = false
                selectedPhotoId = null
            },
            onSelect = { tag ->
                selectedPhotoId?.let { photoId ->
                    viewModel.changeTagForPhoto(photoId, tag.id)
                }
                showChangeTagDialog = false
                selectedPhotoId = null
            }
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun EmptyContent(tagName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "「$tagName」还没有照片",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "在整理照片时可以为照片添加标签",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<PhotoEntity>,
    onPhotoClick: (String, Int) -> Unit,
    onPhotoLongPress: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
            PhotoGridItem(
                photo = photo,
                onClick = { onPhotoClick(photo.id, index) },
                onLongPress = { onPhotoLongPress(photo.id) }
            )
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: PhotoEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.systemUri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ActionSheetDialog(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRemoveTag: () -> Unit,
    onChangeTag: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("照片操作") },
        text = {
            Column {
                TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("编辑照片")
                }
                TextButton(onClick = onRemoveTag, modifier = Modifier.fillMaxWidth()) {
                    Text("移除标签")
                }
                TextButton(onClick = onChangeTag, modifier = Modifier.fillMaxWidth()) {
                    Text("修改标签")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun ChangeTagDialog(
    currentTagId: String?,
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
    onSelect: (TagEntity) -> Unit
) {
    val availableTags = tags.filter { it.id != currentTagId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择新标签") },
        text = {
            Column {
                if (availableTags.isEmpty()) {
                    Text(
                        text = "没有可用标签",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    availableTags.forEach { tag ->
                        TextButton(
                            onClick = { onSelect(tag) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tag.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TaggedPhotosFullscreenViewer(
    photos: List<PhotoEntity>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (photos.isEmpty()) return
    val virtualPageCount = if (photos.size > 1) photos.size * 1000 else 1
    val initialPage = if (photos.size > 1) {
        (virtualPageCount / 2) - ((virtualPageCount / 2) % photos.size) + initialIndex
    } else {
        0
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { virtualPageCount }
    val scope = rememberCoroutineScope()
    val currentRealIndex = if (photos.isNotEmpty()) pagerState.currentPage % photos.size else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val realIndex = if (photos.isNotEmpty()) page % photos.size else 0
            val photo = photos[realIndex]
            val isCurrentPage = page == pagerState.currentPage
            ZoomableImage(
                photo = photo,
                isCurrentPage = isCurrentPage,
                onRequestNextPage = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                onRequestPrevPage = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${currentRealIndex + 1} / ${photos.size}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ZoomableImage(
    photo: PhotoEntity,
    isCurrentPage: Boolean,
    onRequestNextPage: () -> Unit,
    onRequestPrevPage: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var boundarySwipeDistance by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 100f

    androidx.compose.runtime.LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            boundarySwipeDistance = 0f
        }
    }

    fun calculateBounds(): Pair<Float, Float> {
        if (containerSize.width == 0 || containerSize.height == 0) return 0f to 0f
        val scaledWidth = containerSize.width * scale
        val scaledHeight = containerSize.height * scale
        val maxOffsetX = ((scaledWidth - containerSize.width) / 2f).coerceAtLeast(0f)
        val maxOffsetY = ((scaledHeight - containerSize.height) / 2f).coerceAtLeast(0f)
        return maxOffsetX to maxOffsetY
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.5f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            val newScale = 2.5f
                            val centerX = containerSize.width / 2f
                            val centerY = containerSize.height / 2f
                            val scaledWidth = containerSize.width * newScale
                            val scaledHeight = containerSize.height * newScale
                            val maxX = ((scaledWidth - containerSize.width) / 2f).coerceAtLeast(0f)
                            val maxY = ((scaledHeight - containerSize.height) / 2f).coerceAtLeast(0f)
                            val newOffsetX = ((centerX - tapOffset.x) * (newScale - 1)).coerceIn(-maxX, maxX)
                            val newOffsetY = ((centerY - tapOffset.y) * (newScale - 1)).coerceIn(-maxY, maxY)
                            scale = newScale
                            offsetX = newOffsetX
                            offsetY = newOffsetY
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    boundarySwipeDistance = 0f
                    var event: androidx.compose.ui.input.pointer.PointerEvent
                    do {
                        event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val pointerCount = event.changes.size

                        if (pointerCount >= 2 && zoomChange != 1f) {
                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                            if (scale != newScale) {
                                val scaleRatio = newScale / scale
                                offsetX *= scaleRatio
                                offsetY *= scaleRatio
                                scale = newScale
                                val (maxX, maxY) = calculateBounds()
                                offsetX = offsetX.coerceIn(-maxX, maxX)
                                offsetY = offsetY.coerceIn(-maxY, maxY)
                                event.changes.forEach { it.consume() }
                            }
                        } else if (pointerCount == 1 && scale > 1.05f) {
                            offsetX += panChange.x
                            offsetY += panChange.y
                            val (maxX, maxY) = calculateBounds()
                            val atLeft = offsetX <= -maxX + 1f
                            val atRight = offsetX >= maxX - 1f
                            val hitEdge = (panChange.x > 0 && atLeft) || (panChange.x < 0 && atRight)
                            if (hitEdge) {
                                boundarySwipeDistance += panChange.x
                                if (kotlin.math.abs(boundarySwipeDistance) > swipeThreshold) {
                                    if (boundarySwipeDistance > 0) {
                                        onRequestPrevPage()
                                    } else {
                                        onRequestNextPage()
                                    }
                                    boundarySwipeDistance = 0f
                                }
                            } else {
                                boundarySwipeDistance = 0f
                            }
                            offsetX = offsetX.coerceIn(-maxX, maxX)
                            offsetY = offsetY.coerceIn(-maxY, maxY)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.systemUri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}
