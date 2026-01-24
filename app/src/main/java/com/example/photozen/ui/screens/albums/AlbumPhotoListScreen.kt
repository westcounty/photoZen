package com.example.photozen.ui.screens.albums

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.components.ConfirmDeleteSheet
import com.example.photozen.ui.components.DeleteType
import com.example.photozen.ui.components.DragSelectPhotoGrid
import com.example.photozen.ui.components.DragSelectPhotoGridDefaults
import com.example.photozen.ui.components.EmptyStates
import com.example.photozen.ui.components.PhotoStatusBadge
import com.example.photozen.ui.components.shareImage
import com.example.photozen.ui.util.FeatureFlags
import com.example.photozen.ui.components.SelectionTopBar
import com.example.photozen.ui.components.SelectionBottomBar
import com.example.photozen.ui.components.BottomBarConfigs
import com.example.photozen.ui.theme.KeepGreen
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.style.TextAlign
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * Album Photo List Screen - displays photos in a specific album.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPhotoListScreen(
    bucketId: String,
    albumName: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToQuickSort: (String) -> Unit = {},
    onNavigateToFlowSorter: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AlbumPhotoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Initialize ViewModel
    LaunchedEffect(bucketId, albumName) {
        viewModel.initialize(bucketId, albumName)
    }
    
    // Fullscreen viewer state
    var fullscreenStartIndex by remember { mutableIntStateOf(0) }
    var showFullscreen by remember { mutableStateOf(false) }
    
    // Album picker state
    var showAlbumPicker by remember { mutableStateOf(false) }
    var pickerMode by remember { mutableStateOf("move") }  // "move" or "copy"
    
    // Phase 3-9: 删除确认弹窗状态
    var showDeleteConfirmSheet by remember { mutableStateOf(false) }
    
    // Delete confirmation launcher
    val deleteResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onDeleteConfirmed()
        }
    }
    
    // Show messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }
    
    // BackHandler 处理返回键退出选择模式
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }
    
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                // 使用统一的选择模式顶栏
                SelectionTopBar(
                    selectedCount = uiState.selectedCount,
                    totalCount = uiState.photos.size,
                    onClose = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll() },
                    onDeselectAll = { viewModel.clearSelection() }
                )
            } else {
                // 普通模式顶栏
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = albumName,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.totalCount} 张照片",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    actions = {
                        // Refresh
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        // View mode toggle
                        IconButton(onClick = { viewModel.cycleViewMode() }) {
                            Icon(
                                imageVector = when (uiState.viewMode) {
                                    AlbumPhotoListViewMode.GRID_1 -> Icons.Default.ViewColumn
                                    AlbumPhotoListViewMode.GRID_2 -> Icons.Default.GridView
                                    AlbumPhotoListViewMode.GRID_3 -> Icons.Default.ViewModule
                                    AlbumPhotoListViewMode.GRID_4 -> Icons.Default.ViewComfy
                                },
                                contentDescription = "切换视图"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats card (placed above filter chips because filter doesn't affect stats)
            if (!uiState.isSelectionMode && uiState.allPhotos.isNotEmpty()) {
                AlbumStatsCard(
                    totalCount = uiState.totalCount,
                    sortedCount = uiState.sortedCount,
                    sortedPercentage = uiState.sortedPercentage,
                    unsortedCount = uiState.totalCount - uiState.sortedCount,
                    onStartSorting = if (uiState.totalCount > uiState.sortedCount) {
                        { onNavigateToQuickSort(bucketId) }
                    } else null
                )
            }
            
            // Phase 7.2: Status filter chips (filters the photo list below)
            if (!uiState.isSelectionMode && uiState.allPhotos.isNotEmpty()) {
                StatusFilterChips(
                    selectedStatuses = uiState.statusFilter,
                    onStatusToggle = { status -> viewModel.toggleStatusFilter(status) },
                    onSelectAll = { viewModel.selectAllStatuses() }
                )
            }
            
            // Photo grid
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.photos.isEmpty() -> {
                        EmptyStates.EmptyAlbum(
                            albumName = albumName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        // Phase 1-B: 使用 Feature Flag 控制手势实现
                        if (FeatureFlags.USE_UNIFIED_GESTURE) {
                            // 新实现：使用 DragSelectPhotoGrid（支持拖动批量选择）
                            DragSelectPhotoGrid(
                                photos = uiState.photos,
                                selectedIds = uiState.selectedIds,
                                onSelectionChanged = { newSelection ->
                                    viewModel.updateSelection(newSelection)
                                },
                                onPhotoClick = { photoId, index ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.togglePhotoSelection(photoId)
                                    } else {
                                        fullscreenStartIndex = index
                                        showFullscreen = true
                                    }
                                },
                                onPhotoLongPress = { photoId, photoUri ->
                                    // 长按不动时已经选中，此回调用于额外操作（如显示提示）
                                },
                                columns = when (uiState.viewMode) {
                                    AlbumPhotoListViewMode.GRID_1 -> 1
                                    AlbumPhotoListViewMode.GRID_2 -> 2
                                    AlbumPhotoListViewMode.GRID_3 -> 3
                                    AlbumPhotoListViewMode.GRID_4 -> 4
                                },
                                selectionColor = MaterialTheme.colorScheme.primary,
                                enableDragSelect = true,
                                config = DragSelectPhotoGridDefaults.AlbumConfig,
                                showStatusBadge = true
                            )
                        } else {
                            // 旧实现：保留现有代码
                            PhotoGrid(
                                photos = uiState.photos,
                                columnCount = when (uiState.viewMode) {
                                    AlbumPhotoListViewMode.GRID_1 -> 1
                                    AlbumPhotoListViewMode.GRID_2 -> 2
                                    AlbumPhotoListViewMode.GRID_3 -> 3
                                    AlbumPhotoListViewMode.GRID_4 -> 4
                                },
                                isSelectionMode = uiState.isSelectionMode,
                                selectedIds = uiState.selectedIds,
                                onPhotoClick = { index ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.togglePhotoSelection(uiState.photos[index].id)
                                    } else {
                                        fullscreenStartIndex = index
                                        showFullscreen = true
                                    }
                                },
                                onPhotoLongClick = { index ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!uiState.isSelectionMode) {
                                        // Enter selection mode and select this photo
                                        viewModel.enterSelectionMode(uiState.photos[index].id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // Selection action bar - Phase 4: 使用 BottomBarConfigs
            AnimatedVisibility(
                visible = uiState.isSelectionMode && uiState.selectedCount > 0,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                // Get the single selected photo for single-select actions
                val singleSelectedPhoto = if (uiState.selectedCount == 1) {
                    uiState.photos.find { it.id in uiState.selectedIds }
                } else null
                
                // Phase 4: 使用 BottomBarConfigs.adaptive 根据选择数量自动配置
                val bottomBarActions = BottomBarConfigs.adaptive(
                    selectedCount = uiState.selectedCount,
                    singleSelectActions = {
                        BottomBarConfigs.albumPhotosSingleSelect(
                            onEdit = { singleSelectedPhoto?.let { onNavigateToEditor(it.id) } },
                            onShare = { singleSelectedPhoto?.let { shareImage(context, Uri.parse(it.systemUri)) } },
                            onMove = {
                                pickerMode = "move"
                                showAlbumPicker = true
                            },
                            onCopy = {
                                pickerMode = "copy"
                                showAlbumPicker = true
                            },
                            onStartFromHere = {
                                // 从选中照片开始，将该照片及之后的所有照片加入待筛选列表
                                val firstSelectedIndex = uiState.photos.indexOfFirst { it.id in uiState.selectedIds }
                                if (firstSelectedIndex >= 0) {
                                    val photoIdsFromHere = uiState.photos
                                        .drop(firstSelectedIndex)
                                        .map { it.id }

                                    if (photoIdsFromHere.isNotEmpty()) {
                                        scope.launch {
                                            viewModel.setFilterSessionAndNavigate(photoIdsFromHere)
                                            onNavigateToFlowSorter()
                                        }
                                    }
                                }
                            },
                            onDelete = { showDeleteConfirmSheet = true }
                        )
                    },
                    multiSelectActions = {
                        BottomBarConfigs.albumPhotosMultiSelect(
                            onMove = {
                                pickerMode = "move"
                                showAlbumPicker = true
                            },
                            onCopy = {
                                pickerMode = "copy"
                                showAlbumPicker = true
                            },
                            onDelete = { showDeleteConfirmSheet = true }
                        )
                    }
                )
                
                SelectionBottomBar(actions = bottomBarActions)
            }
        }
    }
    
    // Fullscreen viewer
    if (showFullscreen && uiState.photos.isNotEmpty()) {
        AlbumPhotoFullscreenViewer(
            photos = uiState.photos,
            initialIndex = fullscreenStartIndex,
            onDismiss = { showFullscreen = false }
        )
    }
    
    // Album picker bottom sheet
    if (showAlbumPicker) {
        AlbumPickerSheet(
            albums = uiState.albumBubbleList.filter { it.bucketId != bucketId },
            onAlbumSelected = { album ->
                showAlbumPicker = false
                if (pickerMode == "move") {
                    viewModel.moveSelectedToAlbum(album.bucketId)
                } else {
                    viewModel.copySelectedToAlbum(album.bucketId)
                }
            },
            onDismiss = { showAlbumPicker = false }
        )
    }
    
    // Phase 3-9: 永久删除确认弹窗
    if (showDeleteConfirmSheet) {
        val selectedPhotos = uiState.photos.filter { it.id in uiState.selectedIds }
        ConfirmDeleteSheet(
            photos = selectedPhotos,
            deleteType = DeleteType.PERMANENT_DELETE,
            onConfirm = {
                showDeleteConfirmSheet = false
                // 使用 MediaStore API 进行永久删除
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val uris = viewModel.getSelectedPhotoUris()
                    if (uris.isNotEmpty()) {
                        try {
                            val deleteRequest = MediaStore.createDeleteRequest(
                                context.contentResolver,
                                uris
                            )
                            deleteResultLauncher.launch(
                                IntentSenderRequest.Builder(deleteRequest.intentSender).build()
                            )
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                }
            },
            onDismiss = { showDeleteConfirmSheet = false }
        )
    }
}

@Composable
private fun AlbumStatsCard(
    totalCount: Int,
    sortedCount: Int,
    sortedPercentage: Float,
    unsortedCount: Int = totalCount - sortedCount,
    onStartSorting: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress (moved to left)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                CircularProgressIndicator(
                    progress = { sortedPercentage },
                    modifier = Modifier.size(56.dp),
                    color = when {
                        sortedPercentage >= 0.8f -> KeepGreen
                        sortedPercentage >= 0.5f -> MaybeAmber
                        else -> TrashRed
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 6.dp
                )
                Text(
                    text = "${(sortedPercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "整理进度",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$sortedCount / $totalCount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Start sorting button (moved to right)
            if (onStartSorting != null && unsortedCount > 0) {
                FilledTonalButton(
                    onClick = onStartSorting,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("开始整理")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    photos: List<PhotoEntity>,
    columnCount: Int,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onPhotoClick: (Int) -> Unit,
    onPhotoLongClick: (Int) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columnCount),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalItemSpacing = 4.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
            val isSelected = photo.id in selectedIds
            // Calculate aspect ratio from photo dimensions, default to 1f if unavailable
            val photoAspectRatio = if (photo.height > 0) {
                photo.width.toFloat() / photo.height.toFloat()
            } else {
                1f
            }.coerceIn(0.5f, 2f) // Limit aspect ratio to reasonable range
            
            Box(
                modifier = Modifier
                    .animateItem()
                    .aspectRatio(photoAspectRatio)
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { onPhotoClick(index) },
                        onLongClick = { onPhotoLongClick(index) }
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else Modifier
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(photo.systemUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Photo status badge (top-left, only show when not in selection mode)
                if (!isSelectionMode && photo.status != PhotoStatus.UNSORTED) {
                    PhotoStatusBadge(
                        status = photo.status,
                        size = 20.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    )
                }
                
                // Selection indicator
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
                            .border(
                                width = 2.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Phase 4: SelectionActionBar 和 AlbumBottomBarActionItem 已迁移到使用 BottomBarConfigs 和 SelectionBottomBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumPickerSheet(
    albums: List<AlbumBubbleEntity>,
    onAlbumSelected: (AlbumBubbleEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "选择目标相册",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (albums.isEmpty()) {
                Text(
                    text = "没有其他相册可选",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                albums.forEach { album ->
                    ListItem(
                        headlineContent = { Text(album.displayName) },
                        leadingContent = {
                            Icon(
                                Icons.Default.PhotoAlbum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable { onAlbumSelected(album) }
                    )
                }
            }
        }
    }
}

/**
 * Fullscreen photo viewer with pager support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumPhotoFullscreenViewer(
    photos: List<PhotoEntity>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (photos.isEmpty()) return
    
    // Use virtual paging for infinite scroll effect
    val virtualPageCount = if (photos.size > 1) photos.size * 1000 else 1
    val initialPage = if (photos.size > 1) {
        (virtualPageCount / 2) - ((virtualPageCount / 2) % photos.size) + initialIndex
    } else 0
    
    val pagerState = rememberPagerState(initialPage = initialPage) { virtualPageCount }
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
            
            ZoomableAlbumImage(
                photo = photo,
                isCurrentPage = isCurrentPage,
                onTap = onDismiss
            )
        }
        
        // Page indicator
        if (photos.size > 1) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "${currentRealIndex + 1} / ${photos.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Zoomable image with gesture support.
 */
@Composable
private fun ZoomableAlbumImage(
    photo: PhotoEntity,
    isCurrentPage: Boolean,
    onTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Reset zoom when page changes
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onTap = { onTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(photo.systemUri))
                .crossfade(true)
                .build(),
            contentDescription = null,
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

/**
 * Phase 7.2: Status filter chips for album photo list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterChips(
    selectedStatuses: Set<PhotoStatus>,
    onStatusToggle: (PhotoStatus) -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllSelected = selectedStatuses.size == PhotoStatus.entries.size
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status chips
        PhotoStatus.entries.forEach { status ->
            val isSelected = status in selectedStatuses
            FilterChip(
                selected = isSelected,
                onClick = { onStatusToggle(status) },
                label = {
                    Text(
                        text = when (status) {
                            PhotoStatus.KEEP -> "保留"
                            PhotoStatus.MAYBE -> "待定"
                            PhotoStatus.TRASH -> "回收站"
                            PhotoStatus.UNSORTED -> "未筛选"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (status) {
                        PhotoStatus.KEEP -> KeepGreen.copy(alpha = 0.2f)
                        PhotoStatus.MAYBE -> MaybeAmber.copy(alpha = 0.2f)
                        PhotoStatus.TRASH -> TrashRed.copy(alpha = 0.2f)
                        PhotoStatus.UNSORTED -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    selectedLabelColor = when (status) {
                        PhotoStatus.KEEP -> KeepGreen
                        PhotoStatus.MAYBE -> MaybeAmber
                        PhotoStatus.TRASH -> TrashRed
                        PhotoStatus.UNSORTED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ),
                modifier = Modifier.height(32.dp)
            )
        }
        
        // Reset button (show only when filter is active)
        if (!isAllSelected) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onSelectAll,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = "全部",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
