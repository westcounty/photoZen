@file:OptIn(ExperimentalFoundationApi::class)

package com.example.photozen.ui.screens.tags

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.photozen.ui.components.TaggedPhotoActionSheet
import com.example.photozen.ui.components.openImageWithApp
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.graphicsLayer

// Preset colors for new tags
private val TAG_COLORS = listOf(
    Color(0xFFE57373), Color(0xFFFF8A65), Color(0xFFFFB74D), Color(0xFFFFD54F),
    Color(0xFFAED581), Color(0xFF4DB6AC), Color(0xFF4FC3F7), Color(0xFF7986CB),
    Color(0xFFBA68C8), Color(0xFFF06292)
)

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenStartIndex by remember { mutableIntStateOf(0) }
    var selectedPhotoId by remember { mutableStateOf<String?>(null) }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showChangeTagDialog by remember { mutableStateOf(false) }
    var showBatchChangeTagDialog by remember { mutableStateOf(false) }
    var showRemoveTagDialog by remember { mutableStateOf(false) }
    var photoToRemoveTag by remember { mutableStateOf<String?>(null) }
    
    // Launcher for delete confirmation
    val deleteConfirmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onPhotoDeleteConfirmed()
        } else {
            viewModel.onPhotoDeleteCancelled()
        }
    }
    
    // Handle pending delete request
    LaunchedEffect(uiState.pendingDeleteRequest) {
        uiState.pendingDeleteRequest?.let { request ->
            try {
                deleteConfirmLauncher.launch(
                    IntentSenderRequest.Builder(request.intentSender).build()
                )
            } catch (e: Exception) {
                viewModel.onPhotoDeleteCancelled()
            }
        }
    }
    
    // Show messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }
    
    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Load photos for this tag
    LaunchedEffect(tagId) {
        viewModel.loadPhotosForTag(tagId)
    }
    
    // Handle back press
    val handleBack: () -> Unit = {
        when {
            showFullscreen -> showFullscreen = false
            uiState.isSelectionMode -> viewModel.exitSelectionMode()
            else -> onNavigateBack()
        }
    }
    
    BackHandler(enabled = showFullscreen || uiState.isSelectionMode) {
        handleBack()
    }
    
    Scaffold(
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
                        TextButton(
                            onClick = {
                                if (uiState.allSelected) viewModel.deselectAll() else viewModel.selectAll()
                            }
                        ) {
                            Text(if (uiState.allSelected) "取消全选" else "全选")
                        }
                    } else {
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
                        
                        // Refresh
                        IconButton(
                            onClick = { viewModel.refreshPhotos() },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        
                        // Batch management
                        if (uiState.photos.isNotEmpty()) {
                            IconButton(onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.enterSelectionMode()
                            }) {
                                Icon(Icons.Default.Checklist, contentDescription = "批量管理")
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
            if (uiState.isSelectionMode && uiState.selectedCount > 0) {
                TaggedPhotosSelectionBottomBar(
                    onChangeTag = { showBatchChangeTagDialog = true },
                    onRemoveTag = { viewModel.batchRemoveTag() }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        columns = uiState.gridColumns,
                        isSelectionMode = uiState.isSelectionMode,
                        selectedPhotoIds = uiState.selectedPhotoIds,
                        onPhotoClick = { photoId, index ->
                            if (uiState.isSelectionMode) {
                                viewModel.togglePhotoSelection(photoId)
                            } else {
                                fullscreenStartIndex = index
                                showFullscreen = true
                            }
                        },
                        onPhotoLongPress = { photoId, photoUri ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (uiState.isSelectionMode) {
                                viewModel.togglePhotoSelection(photoId)
                            } else {
                                selectedPhotoId = photoId
                                selectedPhotoUri = photoUri
                                showActionSheet = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showFullscreen && uiState.photos.isNotEmpty()) {
        TaggedPhotosFullscreenViewer(
            photos = uiState.photos,
            initialIndex = fullscreenStartIndex,
            onDismiss = { showFullscreen = false }
        )
    }

    if (showActionSheet && selectedPhotoId != null && selectedPhotoUri != null) {
        val photoId = selectedPhotoId!!
        val photoUri = selectedPhotoUri!!
        TaggedPhotoActionSheet(
            imageUri = photoUri,
            onDismiss = { 
                showActionSheet = false
                selectedPhotoId = null
                selectedPhotoUri = null
            },
            onEdit = { onNavigateToEditor(photoId) },
            onRemoveTag = {
                if (viewModel.isTagLinkedToAlbum()) {
                    photoToRemoveTag = photoId
                    showActionSheet = false
                    showRemoveTagDialog = true
                } else {
                    viewModel.removeTagFromPhoto(photoId)
                }
            },
            onChangeTag = {
                showActionSheet = false
                showChangeTagDialog = true
            },
            onOpenWithApp = { packageName ->
                openImageWithApp(context, Uri.parse(photoUri), packageName)
            },
            defaultAppPackage = uiState.defaultExternalApp,
            onSetDefaultApp = { packageName ->
                scope.launch { viewModel.setDefaultExternalApp(packageName) }
            }
        )
    }
    
    // Remove tag confirmation dialog
    if (showRemoveTagDialog && photoToRemoveTag != null) {
        RemoveTagConfirmDialog(
            onDismiss = {
                showRemoveTagDialog = false
                photoToRemoveTag = null
            },
            onConfirm = { alsoDeletePhoto ->
                val photoId = photoToRemoveTag!!
                if (alsoDeletePhoto) {
                    viewModel.removeTagAndDeletePhoto(photoId)
                } else {
                    viewModel.removeTagFromPhoto(photoId)
                }
                showRemoveTagDialog = false
                photoToRemoveTag = null
            }
        )
    }

    // Single photo change tag dialog (with create new option)
    if (showChangeTagDialog && selectedPhotoId != null) {
        ChangeTagDialogWithCreate(
            currentTagId = uiState.tagId,
            tags = uiState.allTags,
            onDismiss = {
                showChangeTagDialog = false
                selectedPhotoId = null
            },
            onSelectTag = { tag ->
                selectedPhotoId?.let { photoId ->
                    viewModel.changeTagForPhoto(photoId, tag.id)
                }
                showChangeTagDialog = false
                selectedPhotoId = null
            },
            onCreateTag = { name, color ->
                selectedPhotoId?.let { photoId ->
                    viewModel.createTagAndApplyToPhoto(photoId, name, color)
                }
                showChangeTagDialog = false
                selectedPhotoId = null
            }
        )
    }
    
    // Batch change tag dialog (with create new option)
    if (showBatchChangeTagDialog) {
        ChangeTagDialogWithCreate(
            currentTagId = uiState.tagId,
            tags = uiState.allTags,
            onDismiss = { showBatchChangeTagDialog = false },
            onSelectTag = { tag ->
                viewModel.batchChangeTag(tag.id)
                showBatchChangeTagDialog = false
            },
            onCreateTag = { name, color ->
                viewModel.batchCreateTagAndApply(name, color)
                showBatchChangeTagDialog = false
            }
        )
    }
}

@Composable
private fun TaggedPhotosSelectionBottomBar(
    onChangeTag: () -> Unit,
    onRemoveTag: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onChangeTag,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Label, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("修改标签")
            }
            
            FilledTonalButton(
                onClick = onRemoveTag,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.RemoveCircleOutline, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("移除标签")
            }
        }
    }
}

/**
 * Change Tag Dialog with Create New Tag option.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChangeTagDialogWithCreate(
    currentTagId: String?,
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
    onSelectTag: (TagEntity) -> Unit,
    onCreateTag: (String, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val availableTags = tags.filter { it.id != currentTagId }
    var showCreateSection by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "修改标签",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (!showCreateSection) {
                // Available tags section
                if (availableTags.isNotEmpty()) {
                    Text(
                        text = "选择已有标签",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTags.forEach { tag ->
                            FilterChip(
                                selected = false,
                                onClick = { onSelectTag(tag) },
                                label = { Text(tag.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(tag.color))
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(tag.color).copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }
                
                // Create new tag button
                FilledTonalButton(
                    onClick = { showCreateSection = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("创建新标签")
                }
            } else {
                // Create new tag section
                Text(
                    text = "创建新标签",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TAG_COLORS.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (index == selectedColorIndex) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColorIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == selectedColorIndex) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { showCreateSection = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("返回")
                    }
                    
                    Button(
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                onCreateTag(newTagName.trim(), TAG_COLORS[selectedColorIndex].toArgb())
                            }
                        },
                        enabled = newTagName.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("创建并应用")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
    columns: Int,
    isSelectionMode: Boolean,
    selectedPhotoIds: Set<String>,
    onPhotoClick: (String, Int) -> Unit,
    onPhotoLongPress: (String, String) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns.coerceIn(1, 3)),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
            PhotoGridItem(
                photo = photo,
                isSelectionMode = isSelectionMode,
                isSelected = photo.id in selectedPhotoIds,
                onClick = { onPhotoClick(photo.id, index) },
                onLongPress = { onPhotoLongPress(photo.id, photo.systemUri) }
            )
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: PhotoEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    val aspectRatio = if (photo.width > 0 && photo.height > 0) {
        photo.width.toFloat() / photo.height.toFloat()
    } else 1f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.systemUri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        )
        
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check, "已选择",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoveTagConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: (alsoDeletePhoto: Boolean) -> Unit
) {
    var alsoDeletePhoto by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移除标签") },
        text = {
            Column {
                Text("确定要从这张照片移除标签吗？", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = alsoDeletePhoto, onCheckedChange = { alsoDeletePhoto = it })
                    Text(
                        "同时删除这张照片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (alsoDeletePhoto) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (alsoDeletePhoto) {
                    Text(
                        "⚠️ 照片将从设备中永久删除",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 48.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(alsoDeletePhoto) }) {
                Text(
                    if (alsoDeletePhoto) "删除" else "移除",
                    color = if (alsoDeletePhoto) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
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
    } else 0
    val pagerState = rememberPagerState(initialPage = initialPage) { virtualPageCount }
    val scope = rememberCoroutineScope()
    val currentRealIndex = if (photos.isNotEmpty()) pagerState.currentPage % photos.size else 0

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            val realIndex = if (photos.isNotEmpty()) page % photos.size else 0
            val photo = photos[realIndex]
            val isCurrentPage = page == pagerState.currentPage
            ZoomableImage(
                photo = photo,
                isCurrentPage = isCurrentPage,
                onRequestNextPage = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                onRequestPrevPage = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
            )
        }

        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Icon(Icons.Default.Close, "关闭", tint = Color.White)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("${currentRealIndex + 1} / ${photos.size}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
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

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f; offsetX = 0f; offsetY = 0f; boundarySwipeDistance = 0f
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
                            scale = 1f; offsetX = 0f; offsetY = 0f
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
                            scale = newScale; offsetX = newOffsetX; offsetY = newOffsetY
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
                                offsetX *= scaleRatio; offsetY *= scaleRatio
                                scale = newScale
                                val (maxX, maxY) = calculateBounds()
                                offsetX = offsetX.coerceIn(-maxX, maxX)
                                offsetY = offsetY.coerceIn(-maxY, maxY)
                                event.changes.forEach { it.consume() }
                            }
                        } else if (pointerCount == 1 && scale > 1.05f) {
                            offsetX += panChange.x; offsetY += panChange.y
                            val (maxX, maxY) = calculateBounds()
                            val atLeft = offsetX <= -maxX + 1f
                            val atRight = offsetX >= maxX - 1f
                            val hitEdge = (panChange.x > 0 && atLeft) || (panChange.x < 0 && atRight)
                            if (hitEdge) {
                                boundarySwipeDistance += panChange.x
                                if (kotlin.math.abs(boundarySwipeDistance) > swipeThreshold) {
                                    if (boundarySwipeDistance > 0) onRequestPrevPage() else onRequestNextPage()
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
            model = ImageRequest.Builder(LocalContext.current).data(photo.systemUri).crossfade(true).build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale; scaleY = scale; translationX = offsetX; translationY = offsetY
            }
        )
    }
}
