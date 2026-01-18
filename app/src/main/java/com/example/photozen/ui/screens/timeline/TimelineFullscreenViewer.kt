package com.example.photozen.ui.screens.timeline

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.components.AlbumPickerBottomSheet
import com.example.photozen.ui.components.PhotoStatusBadge
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures

/**
 * Timeline Fullscreen Viewer - 时间线全屏预览组件
 * 
 * 功能：
 * - 支持缩放（双击/双指）
 * - 支持循环切换照片
 * - 底部悬浮操作栏：添加到相册、删除、标记状态
 * 
 * @param photos 当前分组内的所有照片
 * @param initialIndex 初始显示的照片索引
 * @param albums 可选择的相册列表
 * @param onDismiss 关闭全屏预览
 * @param onAddToAlbum 添加到相册回调
 * @param onRequestDelete 请求删除照片回调（触发获取 IntentSender）
 * @param deleteIntentSender 当前待删除照片的 IntentSender（由父组件提供）
 * @param onToggleStatus 切换状态回调
 * @param onDeleteConfirmed 删除确认后的回调
 */
@Composable
fun TimelineFullscreenViewer(
    photos: List<PhotoEntity>,
    initialIndex: Int,
    albums: List<AlbumBubbleEntity>,
    onDismiss: () -> Unit,
    onAddToAlbum: (photoId: String, albumBucketId: String) -> Unit,
    onRequestDelete: (photoId: String) -> Unit,
    deleteIntentSender: android.content.IntentSender?,
    onToggleStatus: (photoId: String) -> Unit,
    onDeleteConfirmed: (photoId: String) -> Unit = {}
) {
    if (photos.isEmpty()) {
        onDismiss()
        return
    }
    
    // Use large page count for "infinite" circular scrolling
    val virtualPageCount = photos.size * 1000
    val initialPage = (virtualPageCount / 2) - ((virtualPageCount / 2) % photos.size) + initialIndex.coerceIn(0, photos.size - 1)
    
    val pagerState = rememberPagerState(initialPage = initialPage) { virtualPageCount }
    val scope = rememberCoroutineScope()
    
    // Current real index
    val currentRealIndex = pagerState.currentPage % photos.size
    val currentPhoto = photos.getOrNull(currentRealIndex)
    
    // Dialog states
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAlbumPicker by remember { mutableStateOf(false) }
    var showActionBar by remember { mutableStateOf(true) }
    var pendingDeletePhotoId by remember { mutableStateOf<String?>(null) }
    
    // Delete request launcher
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingDeletePhotoId?.let { photoId ->
                onDeleteConfirmed(photoId)
                pendingDeletePhotoId = null
            }
        } else {
            pendingDeletePhotoId = null
        }
    }
    
    // Launch delete intent when intentSender is available
    LaunchedEffect(deleteIntentSender, pendingDeletePhotoId) {
        if (deleteIntentSender != null && pendingDeletePhotoId != null) {
            deleteLauncher.launch(
                IntentSenderRequest.Builder(deleteIntentSender).build()
            )
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val realIndex = page % photos.size
            val photo = photos[realIndex]
            val isCurrentPage = page == pagerState.currentPage
            
            Box(modifier = Modifier.fillMaxSize()) {
                TimelineZoomableImage(
                    photo = photo,
                    isCurrentPage = isCurrentPage,
                    onRequestNextPage = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    onRequestPrevPage = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    onTap = { showActionBar = !showActionBar }
                )
                
                // Photo status badge (top-left, larger size for fullscreen)
                PhotoStatusBadge(
                    status = photo.status,
                    size = 32.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 56.dp) // Below close button area
                )
            }
        }
        
        // Close button (always visible)
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
        
        // Page indicator (top center)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
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
        
        // Bottom action bar (floating)
        AnimatedVisibility(
            visible = showActionBar && currentPhoto != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            currentPhoto?.let { photo ->
                TimelineActionBar(
                    photo = photo,
                    onAddToAlbum = { showAlbumPicker = true },
                    onDelete = { showDeleteConfirmDialog = true },
                    onToggleStatus = { onToggleStatus(photo.id) }
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmDialog && currentPhoto != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要永久删除这张照片吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        currentPhoto?.let { photo ->
                            pendingDeletePhotoId = photo.id
                            onRequestDelete(photo.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrashRed)
                ) {
                    Text("删除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // Album picker bottom sheet
    if (showAlbumPicker && currentPhoto != null) {
        AlbumPickerBottomSheet(
            albums = albums,
            title = "添加到相册",
            showAddAlbum = false,
            onAlbumSelected = { album ->
                currentPhoto?.let { photo ->
                    onAddToAlbum(photo.id, album.bucketId)
                }
                showAlbumPicker = false
            },
            onDismiss = { showAlbumPicker = false }
        )
    }
}

/**
 * Bottom action bar for timeline fullscreen viewer.
 */
@Composable
private fun TimelineActionBar(
    photo: PhotoEntity,
    onAddToAlbum: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add to Album
            ActionButton(
                icon = Icons.Default.PhotoAlbum,
                label = "添加到相册",
                color = MaterialTheme.colorScheme.primary,
                onClick = onAddToAlbum
            )
            
            // Delete
            ActionButton(
                icon = Icons.Default.Delete,
                label = "删除",
                color = TrashRed,
                onClick = onDelete
            )
            
            // Toggle Status (Maybe <-> Keep)
            val isMaybe = photo.status == PhotoStatus.MAYBE
            ActionButton(
                icon = if (isMaybe) Icons.Default.Favorite else Icons.Default.QuestionMark,
                label = if (isMaybe) "标为保留" else "标为待定",
                color = if (isMaybe) KeepGreen else MaybeAmber,
                onClick = onToggleStatus
            )
        }
    }
}

/**
 * Individual action button in the bottom bar.
 */
@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalButton(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

/**
 * Zoomable image component for timeline fullscreen viewer.
 * 
 * 核心逻辑：
 * 1. scale = 1: 让 Pager 处理滑动
 * 2. scale > 1: 处理平移，边界时触发页面切换
 * 3. 双击：在 1x 和 2.5x 之间切换
 * 4. 双指：实时缩放
 * 5. 单击：切换操作栏显示
 */
@Composable
private fun TimelineZoomableImage(
    photo: PhotoEntity,
    isCurrentPage: Boolean,
    onRequestNextPage: () -> Unit,
    onRequestPrevPage: () -> Unit,
    onTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Boundary swipe distance for page switching
    var boundarySwipeDistance by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 100f
    
    // Reset zoom state when page changes
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            boundarySwipeDistance = 0f
        }
    }
    
    // Calculate pan bounds
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
                    onTap = { onTap() },
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.5f) {
                            // Reset
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            // Zoom in to tap position
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
                    
                    do {
                        val event = awaitPointerEvent()
                        
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val pointerCount = event.changes.size
                        
                        // Two-finger zoom
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
                            }
                            
                            event.changes.forEach { it.consume() }
                        }
                        // Single finger pan (only when zoomed)
                        else if (pointerCount == 1 && scale > 1.05f) {
                            val (maxX, maxY) = calculateBounds()
                            
                            val newOffsetX = offsetX + panChange.x
                            val newOffsetY = offsetY + panChange.y
                            
                            val atLeftEdge = offsetX >= maxX - 1f
                            val atRightEdge = offsetX <= -maxX + 1f
                            
                            when {
                                atRightEdge && panChange.x < -5f -> {
                                    boundarySwipeDistance += abs(panChange.x)
                                    if (boundarySwipeDistance > swipeThreshold) {
                                        onRequestNextPage()
                                        boundarySwipeDistance = 0f
                                    }
                                }
                                atLeftEdge && panChange.x > 5f -> {
                                    boundarySwipeDistance += abs(panChange.x)
                                    if (boundarySwipeDistance > swipeThreshold) {
                                        onRequestPrevPage()
                                        boundarySwipeDistance = 0f
                                    }
                                }
                                else -> {
                                    boundarySwipeDistance = 0f
                                    offsetX = newOffsetX.coerceIn(-maxX, maxX)
                                    offsetY = newOffsetY.coerceIn(-maxY, maxY)
                                }
                            }
                            
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    
                    // Reset if scale too small
                    if (scale < 1.1f && scale != 1f) {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(photo.systemUri))
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
