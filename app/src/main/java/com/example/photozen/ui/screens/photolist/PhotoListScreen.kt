package com.example.photozen.ui.screens.photolist

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.toArgb
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
import com.example.photozen.data.repository.PhotoClassificationMode
import com.example.photozen.ui.components.PhotoListActionSheet
import com.example.photozen.ui.components.openImageWithApp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.launch

// Preset colors for new tags
private val TAG_COLORS = listOf(
    Color(0xFFE57373), // Red
    Color(0xFFFF8A65), // Deep Orange
    Color(0xFFFFB74D), // Orange
    Color(0xFFFFD54F), // Amber
    Color(0xFFAED581), // Light Green
    Color(0xFF4DB6AC), // Teal
    Color(0xFF4FC3F7), // Light Blue
    Color(0xFF7986CB), // Indigo
    Color(0xFFBA68C8), // Purple
    Color(0xFFF06292), // Pink
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit = {},
    onNavigateToQuickTag: () -> Unit = {},
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
                SelectionBottomBar(
                    status = uiState.status,
                    selectedCount = uiState.selectedCount,
                    onSetTag = if (uiState.canBatchTag) {{ viewModel.showTagDialog() }} else null,
                    onMoveToKeep = if (uiState.status != PhotoStatus.KEEP) {{ viewModel.moveSelectedToKeep() }} else null,
                    onMoveToMaybe = if (uiState.status != PhotoStatus.MAYBE) {{ viewModel.moveSelectedToMaybe() }} else null,
                    onMoveToTrash = if (uiState.status != PhotoStatus.TRASH) {{ viewModel.moveSelectedToTrash() }} else null,
                    onResetToUnsorted = { viewModel.resetSelectedToUnsorted() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Prominent Quick Classify Banner for KEEP status - show untagged count (only when not in selection mode)
            if (!uiState.isSelectionMode && uiState.status == PhotoStatus.KEEP && uiState.untaggedCount > 0) {
                QuickClassifyBanner(
                    photoCount = uiState.untaggedCount,
                    isAlbumMode = uiState.classificationMode == PhotoClassificationMode.ALBUM,
                    onClick = onNavigateToQuickTag
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
                        PhotoGrid(
                            photos = uiState.photos,
                            sortOrder = uiState.sortOrder,
                            columns = uiState.gridColumns,
                            isSelectionMode = uiState.isSelectionMode,
                            selectedPhotoIds = uiState.selectedPhotoIds,
                            onPhotoClick = { photoId ->
                                if (uiState.isSelectionMode) {
                                    viewModel.togglePhotoSelection(photoId)
                                }
                            },
                            onPhotoLongPress = { photoId, photoUri ->
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
    }
    
    // Tag Selection Dialog
    if (uiState.showTagDialog) {
        TagSelectionDialog(
            availableTags = uiState.availableTags,
            onDismiss = { viewModel.hideTagDialog() },
            onSelectTag = { tagId -> viewModel.applyTagToSelected(tagId) },
            onCreateTag = { name, color -> viewModel.createTagAndApplyToSelected(name, color) }
        )
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
            } else null
        )
    }
}

/**
 * Tag Selection Dialog - allows selecting existing tag or creating new one.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagSelectionDialog(
    availableTags: List<TagInfo>,
    onDismiss: () -> Unit,
    onSelectTag: (String) -> Unit,
    onCreateTag: (String, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCreateTagSection by remember { mutableStateOf(false) }
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
            // Header
            Text(
                text = "设置标签",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Existing tags section
            if (availableTags.isNotEmpty() && !showCreateTagSection) {
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
                            onClick = { onSelectTag(tag.id) },
                            label = { 
                                Text("${tag.name} (${tag.photoCount})")
                            },
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
            
            // Create new tag section
            if (showCreateTagSection) {
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedColorIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == selectedColorIndex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { showCreateTagSection = false },
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
            } else {
                // Button to show create tag section
                FilledTonalButton(
                    onClick = { showCreateTagSection = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("创建新标签")
                }
            }
        }
    }
}

/**
 * Selection mode bottom bar with batch actions.
 * Shows different actions based on current status.
 */
@Composable
private fun SelectionBottomBar(
    status: PhotoStatus,
    selectedCount: Int,
    onSetTag: (() -> Unit)? = null,
    onMoveToKeep: (() -> Unit)? = null,
    onMoveToMaybe: (() -> Unit)? = null,
    onMoveToTrash: (() -> Unit)? = null,
    onResetToUnsorted: () -> Unit
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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tag button (only for KEEP status)
            if (onSetTag != null) {
                FilledTonalButton(
                    onClick = onSetTag,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.LocalOffer, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("标签")
                }
            }
            
            // Keep button (for MAYBE and TRASH status)
            if (onMoveToKeep != null) {
                FilledTonalButton(
                    onClick = onMoveToKeep,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = KeepGreen.copy(alpha = 0.15f),
                        contentColor = KeepGreen
                    )
                ) {
                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("保留")
                }
            }
            
            // Maybe button (not for MAYBE status)
            if (onMoveToMaybe != null) {
                FilledTonalButton(
                    onClick = onMoveToMaybe,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaybeAmber.copy(alpha = 0.15f),
                        contentColor = MaybeAmber
                    )
                ) {
                    Icon(Icons.Default.QuestionMark, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("待定")
                }
            }
            
            // Trash button (not for TRASH status)
            if (onMoveToTrash != null) {
                FilledTonalButton(
                    onClick = onMoveToTrash,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TrashRed.copy(alpha = 0.15f),
                        contentColor = TrashRed
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除")
                }
            }
            
            // Reset button (always available)
            FilledTonalButton(
                onClick = onResetToUnsorted,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Undo, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("重置")
            }
        }
    }
}

/**
 * Prominent Quick Classify Banner - adapts to classification mode (Tag or Album).
 */
@Composable
private fun QuickClassifyBanner(
    photoCount: Int,
    isAlbumMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAlbumMode) Icons.Default.PhotoAlbum else Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = "快速分类",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isAlbumMode) {
                            "将 $photoCount 张照片分类到不同相册"
                        } else {
                            "为 $photoCount 张照片添加标签"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("开始")
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    photos: List<PhotoEntity>,
    sortOrder: PhotoListSortOrder,
    columns: Int = 2,
    isSelectionMode: Boolean,
    selectedPhotoIds: Set<String>,
    onPhotoClick: (String) -> Unit,
    onPhotoLongPress: (String, String) -> Unit // photoId, photoUri
) {
    // Use sortOrder and columns in key to force recomposition when they change
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns.coerceIn(1, 4)),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        // Use index-based keys to ensure proper reordering when sort changes
        itemsIndexed(
            items = photos,
            key = { index, photo -> "${sortOrder.name}_${columns}_${index}_${photo.id}" }
        ) { _, photo ->
            PhotoGridItem(
                photo = photo,
                isSelectionMode = isSelectionMode,
                isSelected = photo.id in selectedPhotoIds,
                onClick = { onPhotoClick(photo.id) },
                onLongPress = { onPhotoLongPress(photo.id, photo.systemUri) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: PhotoEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    // Calculate aspect ratio from photo dimensions
    val aspectRatio = if (photo.width > 0 && photo.height > 0) {
        photo.width.toFloat() / photo.height.toFloat()
    } else {
        1f // Default to square if dimensions unknown
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onClick()
                    }
                },
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
        
        // Selection indicator
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
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
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
