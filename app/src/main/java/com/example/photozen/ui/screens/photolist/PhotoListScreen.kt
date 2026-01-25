package com.example.photozen.ui.screens.photolist

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import com.example.photozen.ui.components.shareImage
import com.example.photozen.ui.components.PhotoGridMode
import com.example.photozen.ui.components.ViewModeDropdownButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.components.AlbumPickerBottomSheet
import com.example.photozen.ui.components.ConfirmDeleteSheet
import com.example.photozen.ui.components.DeleteType
import com.example.photozen.ui.components.DragSelectPhotoGrid
import com.example.photozen.ui.components.EmptyStates
import com.example.photozen.ui.components.GuideTooltip
import com.example.photozen.ui.components.ArrowDirection
import com.example.photozen.ui.guide.rememberGuideState
import com.example.photozen.domain.model.GuideKey
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.photozen.ui.components.openImageWithApp
import com.example.photozen.ui.components.SelectionTopBar
import com.example.photozen.ui.components.SelectionBottomBar
import com.example.photozen.ui.components.BottomBarConfigs
import com.example.photozen.ui.components.SortDropdownButton
import com.example.photozen.ui.components.SortOption
import com.example.photozen.ui.components.SortOptions
import com.example.photozen.ui.components.fullscreen.UnifiedFullscreenViewer
import com.example.photozen.ui.components.fullscreen.FullscreenActionType
import com.example.photozen.ui.state.UiEvent
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit = {},
    onNavigateToLightTable: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PhotoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    
    // Phase 3-9: 删除确认弹窗状态
    var showDeleteConfirmSheet by remember { mutableStateOf(false) }

    // REQ-032, REQ-034: 全屏预览状态
    var showFullscreenViewer by remember { mutableStateOf(false) }
    var fullscreenInitialIndex by remember { mutableStateOf(0) }
    
    // 长按引导状态
    val longPressGuide = rememberGuideState(
        guideKey = GuideKey.PHOTO_LIST_LONG_PRESS,
        guideRepository = viewModel.guideRepository
    )
    var gridBounds by remember { mutableStateOf<Rect?>(null) }

    // 处理 UI 事件（撤销等）
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = event.duration
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onAction?.invoke()
                    }
                }
                else -> { /* 其他事件暂不处理 */ }
            }
        }
    }
    
    // BackHandler 处理返回键退出选择模式
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.exitSelectionMode()
    }
    
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }
    
    val (title, color) = when (uiState.status) {
        PhotoStatus.KEEP -> "保留的照片" to KeepGreen
        PhotoStatus.TRASH -> "回收站" to TrashRed
        PhotoStatus.MAYBE -> "待定的照片" to MaybeAmber
        PhotoStatus.UNSORTED -> "未整理的照片" to MaterialTheme.colorScheme.primary
    }

    // REQ-032, REQ-034: 全屏预览界面
    if (showFullscreenViewer && uiState.photos.isNotEmpty()) {
        UnifiedFullscreenViewer(
            photos = uiState.photos,
            initialIndex = fullscreenInitialIndex.coerceIn(0, uiState.photos.lastIndex),
            onExit = { showFullscreenViewer = false },
            onAction = { actionType, photo ->
                when (actionType) {
                    FullscreenActionType.COPY -> viewModel.duplicatePhoto(photo.id)
                    FullscreenActionType.OPEN_WITH -> {
                        uiState.defaultExternalApp?.let { pkg ->
                            openImageWithApp(context, Uri.parse(photo.systemUri), pkg)
                        }
                    }
                    FullscreenActionType.EDIT -> onNavigateToEditor(photo.id)
                    FullscreenActionType.SHARE -> shareImage(context, Uri.parse(photo.systemUri))
                    FullscreenActionType.DELETE -> viewModel.moveToTrash(photo.id)
                }
            }
        )
        return
    }

    // Handle back press in selection mode
    val handleBack: () -> Unit = {
        if (uiState.isSelectionMode) {
            viewModel.exitSelectionMode()
        } else {
            onNavigateBack()
        }
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
                    onClose = { viewModel.exitSelectionMode() },
                    onSelectAll = { viewModel.selectAll() },
                    onDeselectAll = { viewModel.deselectAll() }
                )
            } else {
                // 普通模式顶栏
                TopAppBar(
                    title = {
                        Column {
                            Text(text = title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "${uiState.photos.size} 张照片",
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
                        if (uiState.photos.isNotEmpty()) {
                            // 1. 排序按钮 (REQ-022, REQ-028, REQ-033, REQ-038)
                            // 将 PhotoListSortOrder 转换为 SortOption
                            // KEEP 状态使用专用的排序选项文字
                            val isKeepStatus = uiState.status == PhotoStatus.KEEP
                            val currentSortOption = when (uiState.sortOrder) {
                                PhotoListSortOrder.DATE_DESC -> SortOptions.photoTimeDesc
                                PhotoListSortOrder.DATE_ASC -> SortOptions.photoTimeAsc
                                PhotoListSortOrder.ADDED_DESC -> if (isKeepStatus) SortOptions.keepAddedTimeDesc else SortOptions.addedTimeDesc
                                PhotoListSortOrder.ADDED_ASC -> if (isKeepStatus) SortOptions.keepAddedTimeAsc else SortOptions.addedTimeAsc
                                PhotoListSortOrder.RANDOM -> SortOptions.random
                            }
                            val sortOptions = uiState.availableSortOptions.map { order ->
                                when (order) {
                                    PhotoListSortOrder.DATE_DESC -> SortOptions.photoTimeDesc
                                    PhotoListSortOrder.DATE_ASC -> SortOptions.photoTimeAsc
                                    PhotoListSortOrder.ADDED_DESC -> if (isKeepStatus) SortOptions.keepAddedTimeDesc else SortOptions.addedTimeDesc
                                    PhotoListSortOrder.ADDED_ASC -> if (isKeepStatus) SortOptions.keepAddedTimeAsc else SortOptions.addedTimeAsc
                                    PhotoListSortOrder.RANDOM -> SortOptions.random
                                }
                            }

                            SortDropdownButton(
                                currentSort = currentSortOption,
                                options = sortOptions,
                                onSortSelected = { option ->
                                    val order = when (option.id) {
                                        "photo_time_desc" -> PhotoListSortOrder.DATE_DESC
                                        "photo_time_asc" -> PhotoListSortOrder.DATE_ASC
                                        "added_time_desc" -> PhotoListSortOrder.ADDED_DESC
                                        "added_time_asc" -> PhotoListSortOrder.ADDED_ASC
                                        "random" -> PhotoListSortOrder.RANDOM
                                        else -> PhotoListSortOrder.DATE_DESC
                                    }
                                    viewModel.setSortOrder(order)
                                }
                            )

                            // 2. 视图模式切换（统一下拉菜单）
                            ViewModeDropdownButton(
                                currentMode = uiState.gridMode,
                                currentColumns = uiState.gridColumns,
                                onModeChanged = { mode -> viewModel.setGridMode(mode) },
                                onColumnsChanged = { cols -> viewModel.setGridColumns(cols) }
                            )
                        }

                        // 3. 已分类至相册照片过滤开关 (KEEP status only)
                        if (uiState.status == PhotoStatus.KEEP && uiState.myAlbumBucketIds.isNotEmpty()) {
                            IconButton(onClick = { viewModel.toggleShowPhotosInAlbum() }) {
                                Icon(
                                    imageVector = if (uiState.showPhotosInAlbum)
                                        Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                                    contentDescription = if (uiState.showPhotosInAlbum)
                                        "隐藏已在相册中的照片" else "显示所有照片"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            // Selection mode bottom bar - Phase 4: 使用 BottomBarConfigs
            AnimatedVisibility(
                visible = uiState.isSelectionMode && uiState.selectedCount > 0 && uiState.canBatchManage,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                // Phase 4: 使用 BottomBarConfigs 根据状态配置
                // 保留列表单选和多选使用相同的5个按钮
                val bottomBarActions = when (uiState.status) {
                    PhotoStatus.KEEP -> BottomBarConfigs.keepListMultiSelect(
                        onAlbum = { viewModel.showAlbumDialog() },
                        onMaybe = { viewModel.moveSelectedToMaybe() },
                        onTrash = { showDeleteConfirmSheet = true },
                        onReset = { viewModel.resetSelectedToUnsorted() },
                        onPermanentDelete = { showDeleteConfirmSheet = true }
                    )
                    PhotoStatus.MAYBE -> {
                        // REQ-031: 待定列表使用清除+对比按钮
                        BottomBarConfigs.maybeListCompareSelect(
                            selectedCount = uiState.selectedCount,
                            onClear = { viewModel.exitSelectionMode() },
                            onCompare = { onNavigateToLightTable() }
                        )
                    }
                    PhotoStatus.TRASH -> BottomBarConfigs.adaptive(
                        selectedCount = uiState.selectedCount,
                        singleSelectActions = {
                            BottomBarConfigs.trashListSingleSelect(
                                onKeep = { viewModel.moveSelectedToKeep() },
                                onMaybe = { viewModel.moveSelectedToMaybe() },
                                onReset = { viewModel.resetSelectedToUnsorted() },
                                onPermanentDelete = { /* Handled separately in TrashScreen */ }
                            )
                        },
                        multiSelectActions = {
                            BottomBarConfigs.trashListMultiSelect(
                                onKeep = { viewModel.moveSelectedToKeep() },
                                onMaybe = { viewModel.moveSelectedToMaybe() },
                                onReset = { viewModel.resetSelectedToUnsorted() },
                                onPermanentDelete = { /* Handled separately in TrashScreen */ }
                            )
                        }
                    )
                    PhotoStatus.UNSORTED -> emptyList()
                }
                
                if (bottomBarActions.isNotEmpty()) {
                    SelectionBottomBar(actions = bottomBarActions)
                }
            }
        }
    ) { paddingValues ->
        // Phase 6.1: Album Classify Mode (Fullscreen Dialog)
        if (uiState.isClassifyMode && uiState.currentClassifyPhoto != null) {
            Dialog(
                onDismissRequest = { viewModel.exitClassifyMode() },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                AlbumClassifyModeContent(
                    photo = uiState.currentClassifyPhoto!!,
                    currentIndex = uiState.classifyModeIndex,
                    totalCount = uiState.classifyModePhotos.size,
                    albums = uiState.albumBubbleList,
                    onAddToAlbum = { bucketId -> viewModel.classifyPhotoToAlbum(bucketId) },
                    onSkip = { viewModel.skipClassifyPhoto() },
                    onExit = { viewModel.exitClassifyMode() },
                    onRefreshAlbums = { viewModel.refreshAlbums() }
                )
            }
        }
        
        // Normal content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Phase 6.1: "Classify to album" prompt card (KEEP status only)
            if (uiState.status == PhotoStatus.KEEP && 
                uiState.notInAlbumCount > 0 && 
                !uiState.isSelectionMode &&
                uiState.myAlbumBucketIds.isNotEmpty()) {
                ClassifyToAlbumCard(
                    count = uiState.notInAlbumCount,
                    onClick = { viewModel.enterClassifyMode() }
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.photos.isEmpty() -> {
                        // 根据状态显示对应的空状态
                        when (uiState.status) {
                            PhotoStatus.KEEP -> EmptyStates.NoKeep(modifier = Modifier.fillMaxSize())
                            PhotoStatus.MAYBE -> EmptyStates.NoMaybe(modifier = Modifier.fillMaxSize())
                            PhotoStatus.TRASH -> EmptyStates.EmptyTrash(modifier = Modifier.fillMaxSize())
                            PhotoStatus.UNSORTED -> EmptyStates.AllSorted(
                                onRefresh = { /* Already sorted, no action needed */ },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            DragSelectPhotoGrid(
                                photos = uiState.photos,
                                selectedIds = uiState.selectedPhotoIds,
                                onSelectionChanged = { newSelection ->
                                    // REQ-029, REQ-030: 待定列表选择限制
                                    val applied = viewModel.updateSelection(newSelection)
                                    if (!applied) {
                                        // 超过限制，显示提示
                                        scope.launch {
                                            snackbarHostState.showSnackbar("最多可对比${MAYBE_LIST_SELECTION_LIMIT}张照片")
                                        }
                                    }
                                },
                                onPhotoClick = { photoId, index ->
                                    // 待定列表：点击始终切换选中状态
                                    // 其他列表：根据 isSelectionMode 判断
                                    if (uiState.status == PhotoStatus.MAYBE || uiState.isSelectionMode) {
                                        // 切换选中状态
                                        val success = viewModel.togglePhotoSelectionWithLimit(photoId)
                                        if (!success) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("最多可对比${MAYBE_LIST_SELECTION_LIMIT}张照片")
                                            }
                                        }
                                    } else {
                                        // 非选择模式 - 进入全屏预览 (REQ-032)
                                        fullscreenInitialIndex = index
                                        showFullscreenViewer = true
                                    }
                                },
                                onPhotoLongPress = { photoId, _ ->
                                    // 长按不动时，进入选择模式并选中该照片
                                    // 注意：onDragStart 已经选中该照片，此处仅在未选中时才添加
                                    // 避免 toggle 导致选中又取消的问题
                                    if (photoId !in uiState.selectedPhotoIds) {
                                        viewModel.togglePhotoSelectionWithLimit(photoId)
                                    }
                                },
                                columns = uiState.gridColumns,
                                gridMode = uiState.gridMode,
                                selectionColor = color,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    if (gridBounds == null && coordinates.size.width > 0) {
                                        gridBounds = coordinates.boundsInRoot()
                                    }
                                }
                            )
                            
                            // 长按引导（仅在非选择模式下且有照片时显示）
                            if (!uiState.isSelectionMode && uiState.photos.isNotEmpty()) {
                                GuideTooltip(
                                    visible = longPressGuide.shouldShow,
                                    message = "📱 长按选择\n长按照片进入多选模式\n可拖动批量选择",
                                    targetBounds = gridBounds?.let { bounds ->
                                        // 指向网格第一张照片的位置
                                        Rect(
                                            left = bounds.left + 16f,
                                            top = bounds.top + 16f,
                                            right = bounds.left + 116f,
                                            bottom = bounds.top + 116f
                                        )
                                    },
                                    arrowDirection = ArrowDirection.UP,
                                    onDismiss = longPressGuide.dismiss
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Album picker for batch add to album (selection mode)
    if (uiState.showAlbumDialog) {
        AlbumPickerBottomSheet(
            albums = uiState.albumBubbleList,
            title = "添加到相册",
            showAddAlbum = false,
            onAlbumSelected = { album ->
                viewModel.addSelectedToAlbum(album.bucketId)
            },
            onDismiss = { viewModel.hideAlbumDialog() }
        )
    }
    
    // Phase 3-9: 移入回收站确认弹窗
    if (showDeleteConfirmSheet) {
        val selectedPhotos = uiState.photos.filter { it.id in uiState.selectedPhotoIds }
        ConfirmDeleteSheet(
            photos = selectedPhotos,
            deleteType = DeleteType.MOVE_TO_TRASH,
            onConfirm = {
                showDeleteConfirmSheet = false
                viewModel.moveSelectedToTrash()
            },
            onDismiss = { showDeleteConfirmSheet = false }
        )
    }
}

// Phase 4: SelectionBottomBar 和 BottomBarActionItem 已迁移到 BottomActionBar.kt

/**
 * Phase 6.1: Card prompting user to classify photos to albums.
 */
@Composable
private fun ClassifyToAlbumCard(
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "有 $count 张照片尚未分类到相册",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "点击快速分类到我的相册",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.PhotoAlbum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Phase 6.1: Album classify mode content.
 * Shows one photo at a time with album selection at the bottom.
 * Now displays in fullscreen dialog mode.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlbumClassifyModeContent(
    photo: PhotoEntity,
    currentIndex: Int,
    totalCount: Int,
    albums: List<com.example.photozen.data.local.entity.AlbumBubbleEntity>,
    onAddToAlbum: (String) -> Unit,
    onSkip: () -> Unit,
    onExit: () -> Unit,
    onRefreshAlbums: () -> Unit = {}
) {
    val context = LocalContext.current
    var showAlbumManagerDialog by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Top bar with progress and exit (simplified - no skip button here)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExit) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "退出分类模式"
                    )
                }
                
                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Placeholder to balance the layout
                Spacer(modifier = Modifier.size(48.dp))
            }
            
            // Photo display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(photo.systemUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            
            // Photo info with skip button on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = photo.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = "选择要添加到的相册",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Skip button moved here for easy access
                TextButton(onClick = onSkip) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("跳过")
                }
            }
            
            // Album selection grid
            if (albums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "还没有添加我的相册",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FilledTonalButton(onClick = { showAlbumManagerDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加相册")
                        }
                    }
                }
            } else {
                // Album buttons in a flow layout (without + icon prefix)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    albums.forEach { album ->
                        FilledTonalButton(
                            onClick = { onAddToAlbum(album.bucketId) },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(
                                text = album.displayName,
                                maxLines = 1
                            )
                        }
                    }
                    
                    // Add album button at the end
                    OutlinedButton(
                        onClick = { showAlbumManagerDialog = true },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加相册")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Album manager dialog
    if (showAlbumManagerDialog) {
        AlbumManagerDialog(
            onDismiss = { 
                showAlbumManagerDialog = false
                onRefreshAlbums()
            }
        )
    }
}

/**
 * Dialog for managing (adding) albums from classify mode.
 */
@Composable
private fun AlbumManagerDialog(
    onDismiss: () -> Unit
) {
    // This is a simplified dialog - in a real implementation,
    // you would integrate with the album management system
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理我的相册") },
        text = {
            Text("请前往「我的相册」页面添加或管理相册，然后返回继续分类。")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}
