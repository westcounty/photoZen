package com.example.photozen.ui.screens.albums

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.source.Album
import com.example.photozen.ui.components.SystemAlbumPickerDialog
import com.example.photozen.ui.components.bubble.BubbleGraphView
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import com.example.photozen.ui.util.FeatureFlags

/**
 * Album Bubble Screen - Visualizes user's album bubble list.
 * Similar to TagBubbleScreen but for albums.
 * 
 * Phase 1-C: 作为底部导航主 Tab 之一
 * - onNavigateBack 标记为可选（由底部导航处理切换）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumBubbleScreen(
    onNavigateToAlbumPhotos: (String, String) -> Unit,
    onNavigateToQuickSort: (String) -> Unit,
    // Phase 1-C: 底部导航模式不需要返回按钮
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AlbumBubbleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Context menu state
    var selectedAlbumForMenu by remember { mutableStateOf<AlbumBubbleData?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // Show error/message
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Show undo snackbar for remove operation
    LaunchedEffect(uiState.showUndoSnackbar) {
        val removedAlbum = uiState.lastRemovedAlbum
        if (uiState.showUndoSnackbar && removedAlbum != null) {
            val result = snackbarHostState.showSnackbar(
                message = "已从列表移除「${removedAlbum.displayName}」",
                actionLabel = "撤回",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoRemoveAlbum()
            } else {
                viewModel.clearUndoState()
            }
        }
    }
    
    LaunchedEffect(uiState.message) {
        // Skip if we're showing the undo snackbar
        if (!uiState.showUndoSnackbar) {
            uiState.message?.let { message ->
                snackbarHostState.showSnackbar(message)
                viewModel.clearMessage()
            }
        }
    }
    
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "我的相册",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${uiState.albums.size} 个相册",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    // Phase 1-C: 底部导航模式不显示返回按钮
                    if (!FeatureFlags.USE_BOTTOM_NAV) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                },
                actions = {
                    // View mode toggle
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (uiState.viewMode == AlbumViewMode.BUBBLE) 
                                Icons.Default.FormatListBulleted else Icons.Default.BubbleChart,
                            contentDescription = "切换视图"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAlbumPicker() }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "编辑相册列表")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.albums.isEmpty() -> {
                    EmptyAlbumList(
                        onAddAlbum = { viewModel.showAlbumPicker() }
                    )
                }
                else -> {
                    when (uiState.viewMode) {
                        AlbumViewMode.BUBBLE -> {
                            AlbumBubbleView(
                                bubbleNodes = uiState.bubbleNodes,
                                albums = uiState.albums,
                                onAlbumClick = { albumId ->
                                    val album = uiState.albums.find { it.bucketId == albumId }
                                    album?.let {
                                        onNavigateToAlbumPhotos(it.bucketId, it.displayName)
                                    }
                                },
                                onAlbumLongClick = { albumId ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val album = uiState.albums.find { it.bucketId == albumId }
                                    selectedAlbumForMenu = album
                                }
                            )
                        }
                        AlbumViewMode.LIST -> {
                            AlbumListView(
                                albums = uiState.albums,
                                onAlbumClick = { album ->
                                    onNavigateToAlbumPhotos(album.bucketId, album.displayName)
                                },
                                onAlbumLongClick = { album ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedAlbumForMenu = album
                                },
                                onSortAlbum = { album ->
                                    if (album.unsortedCount > 0) {
                                        onNavigateToQuickSort(album.bucketId)
                                    }
                                },
                                onApplyNewOrder = { newOrder ->
                                    viewModel.applyNewAlbumOrder(newOrder)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Album context menu
    selectedAlbumForMenu?.let { album ->
        AlbumContextMenu(
            album = album,
            onRemoveFromList = {
                viewModel.removeAlbumFromList(album.bucketId)
                selectedAlbumForMenu = null
            },
            onDeleteAlbum = {
                showDeleteConfirmDialog = true
            },
            onSortAlbum = {
                selectedAlbumForMenu = null
                if (album.unsortedCount > 0) {
                    onNavigateToQuickSort(album.bucketId)
                } else {
                    // Show toast for completed albums
                    viewModel.showMessage("该相册已完成整理哦")
                }
            },
            onDismiss = { selectedAlbumForMenu = null }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmDialog && selectedAlbumForMenu != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmDialog = false 
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("删除相册") },
            text = { 
                Text("确定要删除「${selectedAlbumForMenu?.displayName}」吗？\n\n此操作将永久删除相册中的所有照片，无法撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedAlbumForMenu?.let {
                            viewModel.deleteAlbum(it.bucketId)
                        }
                        showDeleteConfirmDialog = false
                        selectedAlbumForMenu = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteConfirmDialog = false 
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // Album picker dialog (using unified component)
    if (uiState.showAlbumPicker) {
        SystemAlbumPickerDialog(
            albums = uiState.availableAlbums,
            selectedIds = uiState.selectedAlbumIds,
            isLoading = uiState.isLoadingAlbums,
            onToggleSelection = { viewModel.toggleAlbumSelection(it) },
            onConfirm = { viewModel.confirmAlbumSelection() },
            onDismiss = { viewModel.hideAlbumPicker() },
            onCreateAlbum = { albumName ->
                viewModel.createAlbum(albumName)
            }
        )
    }
}

@Composable
private fun EmptyAlbumList(
    onAddAlbum: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoAlbum,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有添加相册",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "添加相册后，可以在这里快速管理和整理照片",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        FilledTonalButton(onClick = onAddAlbum) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加相册")
        }
    }
}

@Composable
private fun AlbumBubbleView(
    bubbleNodes: List<com.example.photozen.ui.components.bubble.BubbleNode>,
    albums: List<AlbumBubbleData>,
    onAlbumClick: (String) -> Unit,
    onAlbumLongClick: (String) -> Unit
) {
    BubbleGraphView(
        nodes = bubbleNodes,
        onBubbleClick = { node -> onAlbumClick(node.id) },
        onBubbleLongClick = { node -> onAlbumLongClick(node.id) }
    )
}

@Composable
private fun AlbumListView(
    albums: List<AlbumBubbleData>,
    onAlbumClick: (AlbumBubbleData) -> Unit,
    onAlbumLongClick: (AlbumBubbleData) -> Unit,
    onSortAlbum: (AlbumBubbleData) -> Unit,
    onApplyNewOrder: (List<String>) -> Unit
) {
    // Use local mutable state list for smooth dragging
    // This allows immediate visual feedback without waiting for ViewModel updates
    val localAlbums = remember { mutableStateListOf<AlbumBubbleData>() }
    
    // Track if we're currently dragging to prevent sync during drag
    var isDraggingAny by remember { mutableStateOf(false) }
    
    // Sync from external albums when they change (but not during drag)
    LaunchedEffect(albums, isDraggingAny) {
        if (isDraggingAny) return@LaunchedEffect  // Don't sync during drag
        
        // Filter and deduplicate
        val seen = mutableSetOf<String>()
        val filtered = albums.filter { album ->
            album.bucketId.isNotBlank() && seen.add(album.bucketId)
        }
        // Only update if content actually changed
        if (localAlbums.map { it.bucketId } != filtered.map { it.bucketId }) {
            localAlbums.clear()
            localAlbums.addAll(filtered)
        }
    }
    
    // Guard against empty list
    if (localAlbums.isEmpty() && albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无相册",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Move item in local list immediately for smooth visual feedback
        localAlbums.apply {
            add(to.index, removeAt(from.index))
        }
    }
    val haptic = LocalHapticFeedback.current
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(localAlbums.toList(), key = { it.bucketId }) { album ->
            ReorderableItem(reorderableState, key = album.bucketId) { isDragging ->
                // Track dragging state and persist order when drag ends
                LaunchedEffect(isDragging) {
                    if (isDragging) {
                        isDraggingAny = true
                    } else if (isDraggingAny) {
                        // Drag ended - persist new order
                        isDraggingAny = false
                        onApplyNewOrder(localAlbums.map { it.bucketId })
                    }
                }
                
                AlbumListItemWithDrag(
                    album = album,
                    isDragging = isDragging,
                    onClick = { onAlbumClick(album) },
                    onLongClick = { onAlbumLongClick(album) },
                    onSortClick = { onSortAlbum(album) },
                    onDragStarted = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumListItem(
    album: AlbumBubbleData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSortClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover image or placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (album.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(android.net.Uri.parse(album.coverUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoAlbum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Album info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.sortedCount}/${album.totalCount} 已整理",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Start sorting button (only if unsorted photos exist)
            if (album.unsortedCount > 0) {
                FilledTonalIconButton(
                    onClick = onSortClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "开始整理"
                    )
                }
            }
        }
    }
}

/**
 * Album list item with drag handle for reordering.
 * Note: This must be called inside a ReorderableCollectionItemScope.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun sh.calvin.reorderable.ReorderableCollectionItemScope.AlbumListItemWithDrag(
    album: AlbumBubbleData,
    isDragging: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSortClick: () -> Unit,
    onDragStarted: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Animate scale when dragging for visual feedback
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "drag_scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isDragging) Modifier.shadow(8.dp, RoundedCornerShape(12.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) 
                MaterialTheme.colorScheme.surfaceVariant
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            IconButton(
                onClick = {},
                modifier = Modifier.draggableHandle(
                    onDragStarted = { onDragStarted() },
                    interactionSource = interactionSource
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "拖动排序",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            // Cover image or placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (album.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(android.net.Uri.parse(album.coverUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoAlbum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Album info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.sortedCount}/${album.totalCount} 已整理",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Start sorting button (only if unsorted photos exist)
            if (album.unsortedCount > 0) {
                FilledTonalIconButton(
                    onClick = onSortClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "开始整理"
                    )
                }
            }
        }
    }
}

/**
 * Context menu for album actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumContextMenu(
    album: AlbumBubbleData,
    onRemoveFromList: () -> Unit,
    onDeleteAlbum: () -> Unit,
    onSortAlbum: () -> Unit,
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
            // Album header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = album.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${album.sortedCount}/${album.totalCount} 已整理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sort this album
            ListItem(
                headlineContent = { Text("整理该相册") },
                supportingContent = { 
                    Text(
                        if (album.unsortedCount > 0) "还有 ${album.unsortedCount} 张照片待整理"
                        else "已全部整理完成"
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable(onClick = onSortAlbum)
            )
            
            // Remove from list
            ListItem(
                headlineContent = { Text("从列表移除") },
                supportingContent = { Text("仅从气泡列表中移除，不删除相册") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = null,
                        tint = MaybeAmber
                    )
                },
                modifier = Modifier.clickable(onClick = onRemoveFromList)
            )
            
            // Delete album
            ListItem(
                headlineContent = { 
                    Text(
                        text = "删除相册",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                supportingContent = { 
                    Text(
                        text = "永久删除相册及其中的所有照片",
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable(onClick = onDeleteAlbum)
            )
        }
    }
}
