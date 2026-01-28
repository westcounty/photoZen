package com.example.photozen.ui.screens.share

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.example.photozen.ui.components.AlbumPickerBottomSheet
import com.example.photozen.ui.screens.lighttable.TransformState
import com.example.photozen.ui.screens.lighttable.TransformSnapshot
import com.example.photozen.ui.screens.lighttable.rememberTransformState
import com.example.photozen.ui.components.fullscreen.UnifiedFullscreenViewer
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.launch

/**
 * Screen for comparing shared photos from external apps.
 * 
 * Features:
 * - Immersive full-screen with floating buttons
 * - Photo selection with visual feedback
 * - Add selected photos to album
 * - Delete selected photos via system API
 * - Fullscreen photo viewer with circular paging
 * - Sync zoom across all photos
 */
@Composable
fun ShareCompareScreen(
    urisJson: String,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShareCompareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Transform state for sync zoom
    val transformState = rememberTransformState()

    // Individual transform states for each photo (max 6 photos)
    val individualTransformStates = remember {
        List(6) { TransformState() }
    }

    // Helper function to reset all zoom states
    fun resetAllZoomStates() {
        transformState.reset()
        individualTransformStates.forEach { it.reset() }
    }

    // Fullscreen viewer state
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenStartIndex by remember { mutableIntStateOf(0) }
    
    // System delete launcher
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onPhotosDeleted(uiState.selectedUris)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("删除已取消")
            }
        }
    }
    
    // Initialize URIs on first composition
    LaunchedEffect(urisJson) {
        viewModel.setPhotoUris(urisJson)
    }
    
    // Show error toast and finish if invalid
    LaunchedEffect(uiState.isLoading, uiState.validation) {
        if (!uiState.isLoading && !uiState.isValid) {
            uiState.errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
            onFinish()
        }
    }
    
    // Show snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }
    
    // Don't render if not valid and not loading
    if (!uiState.isValid && !uiState.isLoading) {
        return
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.isEmpty -> {
                // Empty state - all photos deleted
                EmptyStateContent(onFinish = onFinish)
            }
            
            else -> {
                // Main comparison content
                ExternalComparisonGrid(
                    photos = uiState.photos,
                    selectedUris = uiState.selectedUris,
                    transformState = transformState,
                    syncZoomEnabled = uiState.syncZoomEnabled,
                    individualTransformStates = individualTransformStates.take(uiState.photos.size),
                    onSelectPhoto = { uri ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleSelection(uri)
                    },
                    onFullscreenClick = { index ->
                        fullscreenStartIndex = index
                        showFullscreen = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Floating top-left: Close button
                SmallFloatingActionButton(
                    onClick = onFinish,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "返回"
                    )
                }
                
                // Floating top-right: Select all, Sync zoom toggle, Reset zoom
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Select all button
                    SmallFloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleSelectAll()
                        },
                        containerColor = if (uiState.isAllSelected) 
                            KeepGreen 
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = if (uiState.isAllSelected)
                            Color.White
                        else MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = if (uiState.isAllSelected) "取消全选" else "全选"
                        )
                    }
                    
                    // Sync zoom toggle
                    SmallFloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleSyncZoom()
                        },
                        containerColor = if (uiState.syncZoomEnabled) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = if (uiState.syncZoomEnabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(
                            imageVector = if (uiState.syncZoomEnabled) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = if (uiState.syncZoomEnabled) "联动缩放已开启" else "联动缩放已关闭"
                        )
                    }
                    
                    // Reset zoom
                    SmallFloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            resetAllZoomStates()
                        },
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOutMap,
                            contentDescription = "重置缩放"
                        )
                    }
                }
                
                // Floating bottom: Action buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Selection actions (shown when photos selected)
                    AnimatedVisibility(
                        visible = uiState.hasSelection,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Add to album button
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.showAlbumPicker()
                                },
                                shape = RoundedCornerShape(24.dp),
                                color = KeepGreen.copy(alpha = 0.95f),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, null, Modifier.size(18.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("添加到相册 (${uiState.selectionCount})", fontWeight = FontWeight.Medium, color = Color.White)
                                }
                            }
                            
                            // Delete button
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.showDeleteConfirm()
                                },
                                shape = RoundedCornerShape(24.dp),
                                color = TrashRed.copy(alpha = 0.95f),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("删除所选", fontWeight = FontWeight.Medium, color = Color.White)
                                }
                            }
                        }
                    }
                    
                    // Always visible: Finish comparison button
                    Surface(
                        onClick = onFinish,
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check, 
                                null, 
                                Modifier.size(20.dp), 
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "完成对比", 
                                fontWeight = FontWeight.Medium, 
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Snackbar host
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 140.dp)
                )
            }
        }
        
        // Loading overlay when copying
        if (uiState.isCopying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在复制...", color = Color.White)
                }
            }
        }
        
        // Fullscreen viewer - 使用统一的全屏预览组件
        if (showFullscreen && uiState.photos.isNotEmpty()) {
            // 将 ExternalPhoto 转换为 PhotoEntity
            val photosForViewer = remember(uiState.photos) {
                uiState.photos.mapIndexed { index, externalPhoto ->
                    PhotoEntity(
                        id = "external_$index",
                        systemUri = externalPhoto.uri.toString(),
                        displayName = "照片 ${index + 1}",
                        dateTaken = System.currentTimeMillis(),
                        size = 0L,
                        mimeType = "image/*",
                        width = externalPhoto.width,
                        height = externalPhoto.height,
                        bucketId = null
                    )
                }
            }
            UnifiedFullscreenViewer(
                photos = photosForViewer,
                initialIndex = fullscreenStartIndex.coerceIn(0, photosForViewer.lastIndex),
                onExit = { showFullscreen = false },
                onAction = { _, _ ->
                    // 外部分享的照片不支持编辑等操作
                },
                showPhotoInfo = false,  // 外部分享的照片没有详细信息
                showBottomBar = false   // 不显示操作栏，只保留预览条
            )
        }
    }
    
    // Album picker bottom sheet
    if (uiState.showAlbumPicker) {
        AlbumPickerBottomSheet(
            albums = uiState.albums,
            title = "选择目标相册",
            showAddAlbum = false,
            onAlbumSelected = { album ->
                viewModel.copyToAlbum(album)
            },
            onDismiss = { viewModel.hideAlbumPicker() }
        )
    }
    
    // Delete confirmation dialog
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = TrashRed
                )
            },
            title = {
                Text("确认删除")
            },
            text = {
                Text("确定要删除 ${uiState.selectionCount} 张照片吗？此操作不可撤销。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.hideDeleteConfirm()
                        // Launch system delete request
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                val deleteRequest = MediaStore.createDeleteRequest(
                                    context.contentResolver,
                                    uiState.selectedUris.toList()
                                )
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(deleteRequest.intentSender).build()
                                )
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("删除失败: ${e.message}")
                                }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("系统版本不支持此操作")
                            }
                        }
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * Empty state when all photos have been deleted.
 */
@Composable
private fun EmptyStateContent(
    onFinish: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "照片已全部删除",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = onFinish) {
                Text("返回原应用")
            }
        }
        
        // Close button still available
        SmallFloatingActionButton(
            onClick = onFinish,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "返回"
            )
        }
    }
}

/**
 * Comparison grid for external photos with selection support.
 */
@Composable
private fun ExternalComparisonGrid(
    photos: List<ExternalPhoto>,
    selectedUris: Set<Uri>,
    transformState: TransformState,
    syncZoomEnabled: Boolean,
    individualTransformStates: List<TransformState>,
    onSelectPhoto: (Uri) -> Unit,
    onFullscreenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val photoCount = photos.size.coerceAtMost(6)
    if (photoCount == 0) return

    val density = LocalDensity.current
    val spacing = 4.dp
    val spacingPx = with(density) { spacing.toPx() }

    // Calculate average aspect ratio
    val avgAspectRatio = remember(photos) {
        if (photos.isEmpty()) 1f
        else photos.map { it.aspectRatio }.average().toFloat()
    }

    // Track the previous sync mode to detect transitions
    val previousSyncEnabled = remember { mutableStateOf(syncZoomEnabled) }

    // 保存每张照片在进入同步模式时的基准状态
    val baseSnapshots = remember { mutableStateOf<List<TransformSnapshot>>(emptyList()) }

    // Handle state synchronization when switching between sync and individual modes
    if (syncZoomEnabled != previousSyncEnabled.value) {
        if (syncZoomEnabled) {
            // 切换到同步模式：保存当前状态作为基准，重置共享状态
            baseSnapshots.value = individualTransformStates.map { it.toSnapshot() }
            transformState.reset()
        } else {
            // 切换到独立模式：合并变换到独立状态
            individualTransformStates.forEachIndexed { index, state ->
                val base = baseSnapshots.value.getOrNull(index) ?: TransformSnapshot()
                val finalScale = base.scale * transformState.scale
                val finalOffsetX = base.offsetX + transformState.offsetX * base.scale
                val finalOffsetY = base.offsetY + transformState.offsetY * base.scale
                state.setAll(finalScale, finalOffsetX, finalOffsetY)
            }
            baseSnapshots.value = emptyList()
        }
        previousSyncEnabled.value = syncZoomEnabled
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing)
    ) {
        val containerWidth = constraints.maxWidth.toFloat() - spacingPx
        val containerHeight = constraints.maxHeight.toFloat() - spacingPx

        // Calculate optimal layout
        val layout = remember(photoCount, containerWidth, containerHeight, avgAspectRatio) {
            calculateOptimalLayout(photoCount, containerWidth, containerHeight, avgAspectRatio, spacingPx)
        }

        // Render grid
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var photoIndex = 0

            layout.rowDistribution.forEachIndexed { rowIndex, colsInRow ->
                val isFirstRow = rowIndex == 0

                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(colsInRow) {
                        if (photoIndex < photos.size) {
                            val photo = photos[photoIndex]
                            val currentIndex = photoIndex
                            val isSelected = photo.uri in selectedUris

                            // 选择有效的变换状态
                            val effectiveTransformState = if (syncZoomEnabled) {
                                transformState
                            } else {
                                individualTransformStates.getOrNull(currentIndex) ?: transformState
                            }

                            val baseSnapshot = if (syncZoomEnabled) {
                                baseSnapshots.value.getOrNull(currentIndex)
                            } else {
                                null
                            }

                            ExternalSyncZoomImage(
                                photo = photo,
                                transformState = effectiveTransformState,
                                baseSnapshot = baseSnapshot,
                                isSelected = isSelected,
                                onSelect = { onSelectPhoto(photo.uri) },
                                onFullscreenClick = { onFullscreenClick(currentIndex) },
                                fullscreenButtonPosition = if (isFirstRow) Alignment.BottomEnd else Alignment.TopEnd,
                                modifier = Modifier
                                    .width(with(density) { layout.photoWidth.toDp() })
                                    .height(with(density) { layout.photoHeight.toDp() })
                            )

                            photoIndex++
                        }
                    }
                }
            }
        }
    }
}

/**
 * Layout configuration for comparison grid.
 */
private data class LayoutConfig(
    val rows: Int,
    val cols: Int,
    val photoWidth: Float,
    val photoHeight: Float,
    val totalArea: Float,
    val rowDistribution: List<Int>
)

/**
 * Calculate optimal layout considering photo aspect ratio.
 */
private fun calculateOptimalLayout(
    photoCount: Int,
    containerWidth: Float,
    containerHeight: Float,
    photoAspectRatio: Float,
    spacing: Float
): LayoutConfig {
    val possibleLayouts = mutableListOf<LayoutConfig>()
    
    when (photoCount) {
        1 -> {
            possibleLayouts.add(createLayout(1, 1, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(1)))
        }
        2 -> {
            possibleLayouts.add(createLayout(1, 2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(2)))
            possibleLayouts.add(createLayout(2, 1, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(1, 1)))
        }
        3 -> {
            possibleLayouts.add(createLayout(1, 3, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(3)))
            possibleLayouts.add(createLayout(3, 1, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(1, 1, 1)))
            possibleLayouts.add(createLayout(2, 2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(2, 1)))
            possibleLayouts.add(createLayout(2, 2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(1, 2)))
        }
        4 -> {
            possibleLayouts.add(createLayout(2, 2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(2, 2)))
            possibleLayouts.add(createLayout(1, 4, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(4)))
            possibleLayouts.add(createLayout(4, 1, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(1, 1, 1, 1)))
        }
        5 -> {
            possibleLayouts.add(createLayout(2, 3, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(3, 2)))
            possibleLayouts.add(createLayout(2, 3, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(2, 3)))
        }
        6 -> {
            possibleLayouts.add(createLayout(2, 3, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(3, 3)))
            possibleLayouts.add(createLayout(3, 2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(2, 2, 2)))
        }
        else -> {
            possibleLayouts.add(createLayout(2, 3, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(3, 3)))
        }
    }
    
    return possibleLayouts.maxByOrNull { it.totalArea } ?: possibleLayouts.first()
}

private fun createLayout(
    rows: Int,
    cols: Int,
    containerWidth: Float,
    containerHeight: Float,
    photoAspectRatio: Float,
    spacing: Float,
    distribution: List<Int>
): LayoutConfig {
    val maxCols = distribution.maxOrNull() ?: cols
    val cellWidth = (containerWidth - spacing * (maxCols - 1)) / maxCols
    val cellHeight = (containerHeight - spacing * (rows - 1)) / rows
    
    // Fit photo in cell keeping aspect ratio
    val (photoWidth, photoHeight) = if (photoAspectRatio > cellWidth / cellHeight) {
        cellWidth to (cellWidth / photoAspectRatio)
    } else {
        (cellHeight * photoAspectRatio) to cellHeight
    }
    
    val totalPhotos = distribution.sum()
    
    return LayoutConfig(
        rows = rows,
        cols = maxCols,
        photoWidth = photoWidth,
        photoHeight = photoHeight,
        totalArea = photoWidth * photoHeight * totalPhotos,
        rowDistribution = distribution
    )
}

/**
 * Zoomable image for external URIs with selection support.
 * 参考 SyncZoomImage 实现，支持同步和独立缩放模式。
 */
@Composable
private fun ExternalSyncZoomImage(
    photo: ExternalPhoto,
    transformState: TransformState,
    baseSnapshot: TransformSnapshot? = null,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onFullscreenClick: () -> Unit,
    fullscreenButtonPosition: Alignment = Alignment.TopEnd,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = KeepGreen,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .onSizeChanged { containerSize = it }
            // 使用 transformState 作为 key，确保切换模式时手势处理器被重新创建
            .pointerInput(transformState) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        // 双指缩放
                        if (event.changes.size >= 2 && zoomChange != 1f) {
                            val newScale = (transformState.scale * zoomChange).coerceIn(1f, 5f)
                            if (transformState.scale != newScale) {
                                transformState.updateScale(newScale)
                                event.changes.forEach { it.consume() }
                            }
                        }
                        // 单指平移（仅在缩放状态）
                        else if (event.changes.size == 1 && transformState.scale > 1.05f) {
                            transformState.updateOffset(panChange.x, panChange.y)
                            // 应用边界限制
                            if (containerSize.width > 0 && containerSize.height > 0) {
                                transformState.applyBounds(
                                    containerSize.width.toFloat(),
                                    containerSize.height.toFloat()
                                )
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(transformState) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        // 双击切换缩放
                        if (transformState.scale > 1.5f) {
                            transformState.reset()
                        } else {
                            transformState.updateScale(2.5f, offset)
                        }
                    },
                    onTap = {
                        onSelect()
                    }
                )
            }
    ) {
        // 计算最终变换值：如果有基准快照，则 最终值 = 基准 * 增量
        val effectiveScale = if (baseSnapshot != null) {
            baseSnapshot.scale * transformState.scale
        } else {
            transformState.scale
        }
        val effectiveOffsetX = if (baseSnapshot != null) {
            baseSnapshot.offsetX + transformState.offsetX * baseSnapshot.scale
        } else {
            transformState.offsetX
        }
        val effectiveOffsetY = if (baseSnapshot != null) {
            baseSnapshot.offsetY + transformState.offsetY * baseSnapshot.scale
        } else {
            transformState.offsetY
        }

        // Image with transformation - use ORIGINAL size for full resolution when zooming
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.uri)
                .crossfade(true)
                .size(Size.ORIGINAL)  // Load full resolution for zoom clarity
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = effectiveScale
                    scaleY = effectiveScale
                    translationX = effectiveOffsetX
                    translationY = effectiveOffsetY
                }
        )

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(KeepGreen)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = Color.White,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }

        // Fullscreen button
        IconButton(
            onClick = onFullscreenClick,
            modifier = Modifier
                .align(fullscreenButtonPosition)
                .padding(4.dp)
                .size(32.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "全屏预览",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

