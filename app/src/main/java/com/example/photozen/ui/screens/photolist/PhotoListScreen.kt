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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import com.example.photozen.ui.components.shareImage
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
import com.example.photozen.ui.components.DragSelectPhotoGrid
import com.example.photozen.ui.components.PhotoListActionSheet
import com.example.photozen.ui.components.openImageWithApp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
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
    
    // Action sheet state
    var showActionSheet by remember { mutableStateOf(false) }
    var selectedPhotoId by remember { mutableStateOf<String?>(null) }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    
    // Single photo album picker state (for long-press add to album)
    var showSinglePhotoAlbumPicker by remember { mutableStateOf(false) }
    var singlePhotoIdForAlbum by remember { mutableStateOf<String?>(null) }
    
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
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text(
                            text = "已选择 ${uiState.selectedCount} 张",
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else {
                        Column {
                            Text(text = title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "${uiState.photos.size} 张照片",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = if (uiState.isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (uiState.isSelectionMode) "取消" else "返回"
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        // Select all / Deselect all
                        TextButton(
                            onClick = {
                                if (uiState.allSelected) {
                                    viewModel.deselectAll()
                                } else {
                                    viewModel.selectAll()
                                }
                            }
                        ) {
                            Text(if (uiState.allSelected) "取消全选" else "全选")
                        }
                    } else {
                        // Compare mode button for MAYBE status
                        if (uiState.status == PhotoStatus.MAYBE && uiState.photos.isNotEmpty()) {
                            IconButton(onClick = onNavigateToLightTable) {
                                Icon(
                                    imageVector = Icons.Default.CompareArrows,
                                    contentDescription = "对比模式"
                                )
                            }
                        }
                        
                        // Grid columns toggle
                        if (uiState.photos.isNotEmpty()) {
                            IconButton(onClick = { viewModel.cycleGridColumns() }) {
                                Icon(
                                    imageVector = when (uiState.gridColumns) {
                                        1 -> Icons.Default.ViewColumn
                                        2 -> Icons.Default.GridView
                                        else -> Icons.Default.ViewModule
                                    },
                                    contentDescription = "${uiState.gridColumns}列视图"
                                )
                            }
                        }
                        
                        // Sort button
                        if (uiState.photos.isNotEmpty()) {
                            IconButton(onClick = { viewModel.cycleSortOrder() }) {
                                Icon(
                                    imageVector = when (uiState.sortOrder) {
                                        PhotoListSortOrder.DATE_DESC -> Icons.Default.ArrowDownward
                                        PhotoListSortOrder.DATE_ASC -> Icons.Default.ArrowUpward
                                        PhotoListSortOrder.RANDOM -> Icons.Default.Shuffle
                                    },
                                    contentDescription = "排序: ${uiState.sortOrder.displayName}"
                                )
                            }
                        }
                        
                        // Phase 6.2: Album filter toggle button (KEEP status only)
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
                        
                        // Batch selection mode button
                        if (uiState.canBatchManage && uiState.photos.isNotEmpty()) {
                            IconButton(onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleSelectionMode() 
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "批量管理"
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Selection mode bottom bar
            AnimatedVisibility(
                visible = uiState.isSelectionMode && uiState.selectedCount > 0 && uiState.canBatchManage,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                // Get the single selected photo for single-select actions
                val singleSelectedPhoto = if (uiState.selectedCount == 1) {
                    uiState.photos.find { it.id in uiState.selectedPhotoIds }
                } else null
                
                SelectionBottomBar(
                    status = uiState.status,
                    selectedCount = uiState.selectedCount,
                    onEdit = if (singleSelectedPhoto != null) {
                        { onNavigateToEditor(singleSelectedPhoto.id) }
                    } else null,
                    onShare = if (singleSelectedPhoto != null) {
                        { shareImage(context, Uri.parse(singleSelectedPhoto.systemUri)) }
                    } else null,
                    onMoveToKeep = if (uiState.status != PhotoStatus.KEEP) {{ viewModel.moveSelectedToKeep() }} else null,
                    onMoveToMaybe = if (uiState.status != PhotoStatus.MAYBE) {{ viewModel.moveSelectedToMaybe() }} else null,
                    onMoveToTrash = if (uiState.status != PhotoStatus.TRASH) {{ viewModel.moveSelectedToTrash() }} else null,
                    onResetToUnsorted = { viewModel.resetSelectedToUnsorted() },
                    onAddToAlbum = if (uiState.canBatchAlbum) {{ viewModel.showAlbumDialog() }} else null
                )
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
                        EmptyState(status = uiState.status, color = color)
                    }
                    else -> {
                        DragSelectPhotoGrid(
                            photos = uiState.photos,
                            selectedIds = uiState.selectedPhotoIds,
                            onSelectionChanged = { newSelection ->
                                viewModel.updateSelection(newSelection)
                            },
                            onPhotoClick = { photoId, _ ->
                                // Non-selection mode click - could open fullscreen viewer
                            },
                            onPhotoLongPress = { photoId, photoUri ->
                                // Show action sheet for single photo
                                selectedPhotoId = photoId
                                selectedPhotoUri = photoUri
                                showActionSheet = true
                            },
                            columns = uiState.gridColumns,
                            selectionColor = color
                        )
                    }
                }
            }
        }
    }
    
    // Action Sheet
    if (showActionSheet && selectedPhotoId != null && selectedPhotoUri != null) {
        val photoId = selectedPhotoId!!
        val photoUri = selectedPhotoUri!!
        
        PhotoListActionSheet(
            imageUri = photoUri,
            onDismiss = {
                showActionSheet = false
                selectedPhotoId = null
                selectedPhotoUri = null
            },
            onEdit = { onNavigateToEditor(photoId) },
            onMoveToKeep = if (uiState.status != PhotoStatus.KEEP) {
                { viewModel.moveToKeep(photoId) }
            } else null,
            onMoveToMaybe = if (uiState.status != PhotoStatus.MAYBE) {
                { viewModel.moveToMaybe(photoId) }
            } else null,
            onMoveToTrash = if (uiState.status != PhotoStatus.TRASH) {
                { viewModel.moveToTrash(photoId) }
            } else null,
            onResetToUnsorted = { viewModel.resetToUnsorted(photoId) },
            onOpenWithApp = { packageName ->
                openImageWithApp(context, Uri.parse(photoUri), packageName)
            },
            defaultAppPackage = uiState.defaultExternalApp,
            onSetDefaultApp = { packageName ->
                scope.launch {
                    viewModel.setDefaultExternalApp(packageName)
                }
            },
            // Only show duplicate option for KEEP status photos
            onDuplicatePhoto = if (uiState.status == PhotoStatus.KEEP) {
                { viewModel.duplicatePhoto(photoId) }
            } else null,
            // Add to album option for KEEP status
            onAddToAlbum = if (uiState.status == PhotoStatus.KEEP && uiState.albumBubbleList.isNotEmpty()) {
                {
                    singlePhotoIdForAlbum = photoId
                    showSinglePhotoAlbumPicker = true
                }
            } else null
        )
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
    
    // Album picker for single photo (long-press menu)
    if (showSinglePhotoAlbumPicker && singlePhotoIdForAlbum != null) {
        val photoIdToAdd = singlePhotoIdForAlbum!!
        AlbumPickerBottomSheet(
            albums = uiState.albumBubbleList,
            title = "添加到相册",
            showAddAlbum = false,
            onAlbumSelected = { album ->
                viewModel.addPhotoToAlbum(photoIdToAdd, album.bucketId)
                showSinglePhotoAlbumPicker = false
                singlePhotoIdForAlbum = null
            },
            onDismiss = {
                showSinglePhotoAlbumPicker = false
                singlePhotoIdForAlbum = null
            }
        )
    }
}

/**
 * Selection mode bottom bar with batch actions.
 * Shows different actions based on current status and selection count.
 * Single selection shows more individual actions (edit, share).
 * Uses vertical icon+text layout to prevent crowding.
 */
@Composable
private fun SelectionBottomBar(
    status: PhotoStatus,
    selectedCount: Int,
    onEdit: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onMoveToKeep: (() -> Unit)? = null,
    onMoveToMaybe: (() -> Unit)? = null,
    onMoveToTrash: (() -> Unit)? = null,
    onResetToUnsorted: () -> Unit,
    onAddToAlbum: (() -> Unit)? = null
) {
    val isSingleSelect = selectedCount == 1
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit button (single select only)
            if (isSingleSelect && onEdit != null) {
                BottomBarActionItem(
                    icon = Icons.Default.Edit,
                    label = "编辑",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onEdit
                )
            }
            
            // Share button (single select only)
            if (isSingleSelect && onShare != null) {
                BottomBarActionItem(
                    icon = Icons.Default.Share,
                    label = "分享",
                    color = Color(0xFF1E88E5),
                    onClick = onShare
                )
            }
            
            // Add to album button (for KEEP status)
            if (onAddToAlbum != null) {
                BottomBarActionItem(
                    icon = Icons.Default.PhotoAlbum,
                    label = "相册",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onAddToAlbum
                )
            }
            
            // Keep button (for MAYBE and TRASH status)
            if (onMoveToKeep != null) {
                BottomBarActionItem(
                    icon = Icons.Default.Favorite,
                    label = "保留",
                    color = KeepGreen,
                    onClick = onMoveToKeep
                )
            }
            
            // Maybe button (not for MAYBE status)
            if (onMoveToMaybe != null) {
                BottomBarActionItem(
                    icon = Icons.Default.QuestionMark,
                    label = "待定",
                    color = MaybeAmber,
                    onClick = onMoveToMaybe
                )
            }
            
            // Trash button (not for TRASH status)
            if (onMoveToTrash != null) {
                BottomBarActionItem(
                    icon = Icons.Default.Delete,
                    label = "删除",
                    color = TrashRed,
                    onClick = onMoveToTrash
                )
            }
            
            // Reset button (always available)
            BottomBarActionItem(
                icon = Icons.Default.Undo,
                label = "重置",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onResetToUnsorted
            )
        }
    }
}

/**
 * Individual action item for bottom bar with vertical icon+text layout.
 */
@Composable
private fun BottomBarActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyState(status: PhotoStatus, color: Color) {
    val iconAndText: Pair<androidx.compose.ui.graphics.vector.ImageVector, String> = when (status) {
        PhotoStatus.KEEP -> Icons.Default.Favorite to "没有保留的照片"
        PhotoStatus.TRASH -> Icons.Default.Delete to "回收站为空"
        PhotoStatus.MAYBE -> Icons.Default.QuestionMark to "没有待定的照片"
        PhotoStatus.UNSORTED -> Icons.Default.Check to "所有照片已整理完成"
    }
    val icon = iconAndText.first
    val text = iconAndText.second
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(40.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "长按照片可进行操作",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

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
