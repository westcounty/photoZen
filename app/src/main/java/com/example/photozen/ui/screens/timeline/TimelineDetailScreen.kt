package com.example.photozen.ui.screens.timeline

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.components.*
import com.example.photozen.ui.components.fullscreen.UnifiedFullscreenViewer
import com.example.photozen.ui.components.fullscreen.FullscreenActionType
import com.example.photozen.ui.screens.photolist.PhotoListSortOrder
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.launch

/**
 * Timeline Detail Screen - displays all photos in a specific time range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineDetailScreen(
    title: String,
    startTime: Long,
    endTime: Long,
    onNavigateBack: () -> Unit,
    onNavigateToFlowSorter: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TimelineDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Initialize ViewModel
    LaunchedEffect(title, startTime, endTime) {
        viewModel.initialize(title, startTime, endTime)
    }

    // Fullscreen viewer state
    var fullscreenStartIndex by remember { mutableIntStateOf(0) }
    var showFullscreen by remember { mutableStateOf(false) }

    // Album picker state
    var showAlbumPicker by remember { mutableStateOf(false) }

    // Delete confirmation state
    var showDeleteConfirmSheet by remember { mutableStateOf(false) }

    // Batch change status dialog
    var showBatchChangeStatusDialog by remember { mutableStateOf(false) }

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

    // BackHandler for selection mode
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedCount,
                    totalCount = uiState.photos.size,
                    onClose = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll() },
                    onDeselectAll = { viewModel.clearSelection() }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.title.ifEmpty { title },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.totalCount > 0) {
                                Text(
                                    text = "${uiState.sortedCount}/${uiState.totalCount} 张已整理",
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
                    actions = {
                        // Sort dropdown button
                        if (uiState.photos.isNotEmpty()) {
                            val currentSortOption = when (uiState.sortOrder) {
                                PhotoListSortOrder.DATE_DESC -> SortOptions.photoTimeDesc
                                PhotoListSortOrder.DATE_ASC -> SortOptions.photoTimeAsc
                                PhotoListSortOrder.RANDOM -> SortOptions.random
                                else -> SortOptions.photoTimeDesc
                            }
                            val sortOptions = listOf(
                                SortOptions.photoTimeDesc,
                                SortOptions.photoTimeAsc,
                                SortOptions.random
                            )

                            SortDropdownButton(
                                currentSort = currentSortOption,
                                options = sortOptions,
                                onSortSelected = { selectedOption ->
                                    val order = when (selectedOption.id) {
                                        "photo_time_desc" -> PhotoListSortOrder.DATE_DESC
                                        "photo_time_asc" -> PhotoListSortOrder.DATE_ASC
                                        "random" -> PhotoListSortOrder.RANDOM
                                        else -> PhotoListSortOrder.DATE_DESC
                                    }
                                    viewModel.setSortOrder(order)
                                }
                            )
                        }

                        // View mode dropdown button
                        ViewModeDropdownButton(
                            currentMode = uiState.gridMode,
                            currentColumns = uiState.columnCount,
                            onModeChanged = { viewModel.setGridMode(it) },
                            onColumnsChanged = { viewModel.setGridColumns(it) }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            // Selection action bar
            AnimatedVisibility(
                visible = uiState.isSelectionMode && uiState.selectedCount > 0,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                val bottomBarActions = BottomBarConfigs.adaptive(
                    selectedCount = uiState.selectedCount,
                    singleSelectActions = {
                        BottomBarConfigs.albumPhotosSingleSelect(
                            onAddToOtherAlbum = { showAlbumPicker = true },
                            onBatchChangeStatus = { showBatchChangeStatusDialog = true },
                            onCopy = { viewModel.copySelectedPhotos() },
                            onStartFromHere = {
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
                            onAddToOtherAlbum = { showAlbumPicker = true },
                            onBatchChangeStatus = { showBatchChangeStatusDialog = true },
                            onCopy = { viewModel.copySelectedPhotos() },
                            onDelete = { showDeleteConfirmSheet = true }
                        )
                    }
                )

                SelectionBottomBar(
                    actions = bottomBarActions,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats card
            if (!uiState.isSelectionMode && uiState.totalCount > 0) {
                StatsCard(
                    totalCount = uiState.totalCount,
                    sortedCount = uiState.sortedCount,
                    onStartSort = {
                        // 设置当前事件的所有照片为筛选范围，然后导航
                        val photoIds = uiState.photos.map { it.id }
                        if (photoIds.isNotEmpty()) {
                            scope.launch {
                                viewModel.setFilterSessionAndNavigate(photoIds)
                                onNavigateToFlowSorter()
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Status filter chips
            if (!uiState.isSelectionMode && uiState.totalCount > 0) {
                StatusFilterChips(
                    currentFilter = uiState.statusFilter,
                    onToggleStatus = { viewModel.toggleStatusFilter(it) },
                    onSelectAll = { viewModel.selectAllStatuses() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Photo grid
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.photos.isEmpty()) {
                EmptyStates.EmptyTimeline(
                    onGoSort = onNavigateToFlowSorter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                DragSelectPhotoGrid(
                    photos = uiState.photos,
                    selectedIds = uiState.selectedIds,
                    onSelectionChanged = { newSelection ->
                        viewModel.updateSelection(newSelection)
                    },
                    onPhotoClick = { photoId, index ->
                        if (!uiState.isSelectionMode) {
                            fullscreenStartIndex = index
                            showFullscreen = true
                        }
                        // 选择模式由 onSelectionToggle 处理
                    },
                    onPhotoLongPress = { photoId, _ ->
                        viewModel.enterSelectionMode(photoId)
                    },
                    columns = uiState.columnCount,
                    gridMode = uiState.gridMode,
                    showStatusBadge = true,
                    modifier = Modifier.fillMaxSize(),
                    onSelectionToggle = { photoId ->
                        // Toggle selection using ViewModel to ensure fresh state
                        viewModel.togglePhotoSelection(photoId)
                    }
                )
            }
        }
    }

    // Fullscreen viewer
    if (showFullscreen && uiState.photos.isNotEmpty()) {
        UnifiedFullscreenViewer(
            photos = uiState.photos,
            initialIndex = fullscreenStartIndex.coerceIn(0, uiState.photos.lastIndex),
            onExit = { showFullscreen = false },
            onAction = { actionType, photo ->
                when (actionType) {
                    FullscreenActionType.COPY -> viewModel.copyPhoto(photo.id)
                    FullscreenActionType.OPEN_WITH -> {
                        openImageWithChooser(context, Uri.parse(photo.systemUri))
                    }
                    FullscreenActionType.EDIT -> { /* 编辑 */ }
                    FullscreenActionType.SHARE -> shareImage(context, Uri.parse(photo.systemUri))
                    FullscreenActionType.DELETE -> {
                        viewModel.enterSelectionMode(photo.id)
                        showDeleteConfirmSheet = true
                    }
                }
            },
            overlayContent = {
                // 全屏预览删除确认面板
                if (showDeleteConfirmSheet) {
                    val selectedPhotos = uiState.photos.filter { it.id in uiState.selectedIds }
                    ConfirmDeleteSheet(
                        photos = selectedPhotos,
                        deleteType = DeleteType.PERMANENT_DELETE,
                        onConfirm = {
                            val uris = viewModel.getSelectedPhotoUris()
                            if (uris.isNotEmpty()) {
                                scope.launch {
                                    try {
                                        val intentSender = android.provider.MediaStore.createDeleteRequest(
                                            context.contentResolver,
                                            uris
                                        ).intentSender
                                        deleteResultLauncher.launch(
                                            IntentSenderRequest.Builder(intentSender).build()
                                        )
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("删除失败: ${e.message}")
                                    }
                                }
                            }
                            showDeleteConfirmSheet = false
                        },
                        onDismiss = { showDeleteConfirmSheet = false }
                    )
                }
            }
        )
    }

    // Album picker dialog
    if (showAlbumPicker) {
        AlbumPickerSheet(
            albums = uiState.albumBubbleList,
            onAlbumSelected = { album ->
                viewModel.copySelectedToAlbum(album.bucketId)
                showAlbumPicker = false
            },
            onDismiss = { showAlbumPicker = false }
        )
    }

    // Delete confirmation sheet (非全屏预览场景，如选择模式下的删除)
    if (!showFullscreen && showDeleteConfirmSheet) {
        val selectedPhotos = uiState.photos.filter { it.id in uiState.selectedIds }
        ConfirmDeleteSheet(
            photos = selectedPhotos,
            deleteType = DeleteType.PERMANENT_DELETE,
            onConfirm = {
                val uris = viewModel.getSelectedPhotoUris()
                if (uris.isNotEmpty()) {
                    scope.launch {
                        try {
                            val intentSender = android.provider.MediaStore.createDeleteRequest(
                                context.contentResolver,
                                uris
                            ).intentSender
                            deleteResultLauncher.launch(
                                IntentSenderRequest.Builder(intentSender).build()
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("删除失败: ${e.message}")
                        }
                    }
                }
                showDeleteConfirmSheet = false
            },
            onDismiss = { showDeleteConfirmSheet = false }
        )
    }

    // Batch change status dialog
    if (showBatchChangeStatusDialog) {
        BatchChangeStatusDialog(
            selectedCount = uiState.selectedCount,
            onStatusSelected = { status ->
                viewModel.changeSelectedPhotosStatus(status)
                showBatchChangeStatusDialog = false
            },
            onDismiss = { showBatchChangeStatusDialog = false }
        )
    }
}

/**
 * Stats card showing sorting progress.
 */
@Composable
private fun StatsCard(
    totalCount: Int,
    sortedCount: Int,
    onStartSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unsortedCount = totalCount - sortedCount
    val progress = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "整理进度",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$sortedCount/$totalCount 张已整理，剩余 $unsortedCount 张",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (unsortedCount > 0) {
                Button(
                    onClick = onStartSort,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("开始整理")
                }
            }
        }
    }
}

/**
 * Status filter chips.
 */
@Composable
private fun StatusFilterChips(
    currentFilter: Set<PhotoStatus>,
    onToggleStatus: (PhotoStatus) -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllSelected = currentFilter.size == PhotoStatus.entries.size

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All filter
        FilterChip(
            selected = isAllSelected,
            onClick = onSelectAll,
            label = { Text("全部") },
            leadingIcon = if (isAllSelected) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
            } else null
        )

        // Status filters
        PhotoStatus.entries.forEach { status ->
            val isSelected = status in currentFilter && !isAllSelected
            val color = when (status) {
                PhotoStatus.KEEP -> KeepGreen
                PhotoStatus.MAYBE -> MaybeAmber
                PhotoStatus.TRASH -> TrashRed
                PhotoStatus.UNSORTED -> MaterialTheme.colorScheme.outline
            }
            val label = when (status) {
                PhotoStatus.KEEP -> "保留"
                PhotoStatus.MAYBE -> "待定"
                PhotoStatus.TRASH -> "回收站"
                PhotoStatus.UNSORTED -> "未筛选"
            }

            FilterChip(
                selected = isSelected,
                onClick = { onToggleStatus(status) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f),
                    selectedLabelColor = color
                ),
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = color) }
                } else null
            )
        }
    }
}

/**
 * Album picker bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumPickerSheet(
    albums: List<AlbumBubbleEntity>,
    onAlbumSelected: (AlbumBubbleEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "选择目标相册",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            albums.forEach { album ->
                ListItem(
                    headlineContent = { Text(album.displayName) },
                    leadingContent = {
                        Icon(Icons.Default.Folder, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumSelected(album) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
