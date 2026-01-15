package com.example.photozen.ui.screens.tags

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.photozen.data.local.entity.AlbumCopyMode
import com.example.photozen.data.local.entity.TagEntity
import com.example.photozen.data.source.Album
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.ui.components.bubble.BubbleGraphView
import com.example.photozen.ui.components.bubble.BubbleNode
import kotlinx.coroutines.launch

/**
 * Tag Bubble Screen - Interactive bubble graph visualization of tags.
 * 
 * Features:
 * - Physics-based bubble layout
 * - Hierarchical tag navigation (tap center to go back, tap child to drill down)
 * - Visual size based on photo count
 * - Add new tags with FAB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagBubbleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPhotoList: (tagId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagBubbleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Dialog state
    var showAddTagDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<BubbleNode?>(null) }
    var tagToLinkAlbum by remember { mutableStateOf<BubbleNode?>(null) }
    var showTagOptionsSheet by remember { mutableStateOf<BubbleNode?>(null) }
    
    // Launcher for delete confirmation
    val deleteConfirmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User confirmed deletion
            viewModel.onDeleteConfirmed()
        } else {
            // User cancelled
            viewModel.onDeleteCancelled()
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
                viewModel.onDeleteCancelled()
            }
        }
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Show success messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTagDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加标签"
                )
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.bubbleNodes.isEmpty() -> {
                    EmptyContent(
                        message = "还没有创建任何标签\n\n点击 + 创建标签",
                        onAddClick = { showAddTagDialog = true }
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Simple hint text
                        Text(
                            text = "💡 点击查看照片，长按管理标签",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                        
                        BubbleGraphView(
                            nodes = uiState.bubbleNodes,
                            onBubbleClick = { node ->
                                // Tap = view photos for this tag
                                onNavigateToPhotoList(node.id)
                            },
                            onBubbleLongClick = { node ->
                                // Long press = show options sheet
                                showTagOptionsSheet = node
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
    
    // Add tag dialog
    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onConfirm = { name, color ->
                viewModel.createTag(name, color)
                showAddTagDialog = false
            }
        )
    }
    
    // Tag options bottom sheet
    showTagOptionsSheet?.let { node ->
        TagOptionsSheet(
            node = node,
            onDismiss = { showTagOptionsSheet = null },
            onLinkAlbum = {
                showTagOptionsSheet = null
                tagToLinkAlbum = node
                viewModel.loadAvailableAlbums()
            },
            onUnlinkAlbum = {
                viewModel.unlinkAlbum(node.id)
                showTagOptionsSheet = null
            },
            onDelete = {
                showTagOptionsSheet = null
                tagToDelete = node
            }
        )
    }
    
    // Delete tag confirmation dialog
    tagToDelete?.let { node ->
        DeleteTagDialog(
            tagName = node.label,
            photoCount = node.photoCount,
            isLinkedToAlbum = node.isLinkedToAlbum,
            linkedAlbumName = node.linkedAlbumName,
            onDismiss = { tagToDelete = null },
            onConfirm = { deleteAlbum ->
                viewModel.deleteTag(node.id, deleteAlbum)
                tagToDelete = null
            }
        )
    }
    
    // Album linking dialog
    tagToLinkAlbum?.let { node ->
        LinkAlbumDialog(
            tagName = node.label,
            availableAlbums = uiState.availableAlbums,
            isLoadingAlbums = uiState.isLoadingAlbums,
            onDismiss = { tagToLinkAlbum = null },
            onCreateNewAlbum = { albumName, copyMode ->
                viewModel.createAndLinkAlbum(node.id, albumName, copyMode)
                tagToLinkAlbum = null
            },
            onLinkExistingAlbum = { album, copyMode ->
                viewModel.linkExistingAlbum(node.id, album, copyMode)
                tagToLinkAlbum = null
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载标签中...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyContent(
    message: String,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🏷️",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("创建第一个标签")
            }
        }
    }
}

/**
 * Bottom sheet for tag options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagOptionsSheet(
    node: BubbleNode,
    onDismiss: () -> Unit,
    onLinkAlbum: () -> Unit,
    onUnlinkAlbum: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(node.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = node.label.take(1),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = node.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${node.photoCount} 张照片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (node.isLinkedToAlbum) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = " 已关联: ${node.linkedAlbumName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Options
            if (node.isLinkedToAlbum) {
                // Unlink option
                OptionItem(
                    icon = Icons.Default.LinkOff,
                    title = "解除相册关联",
                    subtitle = "保留标签，仅解除与系统相册的关联",
                    onClick = onUnlinkAlbum
                )
            } else {
                // Link option
                OptionItem(
                    icon = Icons.Default.Link,
                    title = "关联系统相册",
                    subtitle = "将标签照片同步到系统相册，方便其他应用访问",
                    onClick = onLinkAlbum
                )
            }
            
            OptionItem(
                icon = Icons.Default.Delete,
                title = "删除标签",
                subtitle = "删除此标签，照片不会被删除",
                iconTint = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (iconTint == MaterialTheme.colorScheme.error) iconTint else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Dialog for confirming tag deletion.
 */
@Composable
private fun DeleteTagDialog(
    tagName: String,
    photoCount: Int,
    isLinkedToAlbum: Boolean,
    linkedAlbumName: String?,
    onDismiss: () -> Unit,
    onConfirm: (deleteAlbum: Boolean) -> Unit
) {
    var deleteAlbum by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("删除标签")
        },
        text = {
            Column {
                Text("确定要删除标签「$tagName」吗？")
                if (photoCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "该标签下有 $photoCount 张照片，删除后照片不会被删除，仅移除标签关联。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isLinkedToAlbum && linkedAlbumName != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteAlbum = !deleteAlbum }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = deleteAlbum,
                                onClick = { deleteAlbum = !deleteAlbum }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = "同时删除系统相册「$linkedAlbumName」",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "⚠️ 此操作会彻底删除相册中的照片",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(deleteAlbum) }
            ) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * Dialog for adding a new tag.
 */
@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int) -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    
    // Predefined colors
    val colors = listOf(
        0xFF5EEAD4.toInt(), // Teal
        0xFFF472B6.toInt(), // Pink
        0xFFFBBF24.toInt(), // Amber
        0xFF60A5FA.toInt(), // Blue
        0xFFA78BFA.toInt(), // Purple
        0xFF34D399.toInt(), // Emerald
        0xFFFB7185.toInt(), // Rose
        0xFF38BDF8.toInt()  // Sky
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("创建新标签")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEachIndexed { index, color ->
                        ColorOption(
                            color = Color(color),
                            isSelected = index == selectedColorIndex,
                            onClick = { selectedColorIndex = index },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tagName.isNotBlank()) {
                        onConfirm(tagName.trim(), colors[selectedColorIndex])
                    }
                },
                enabled = tagName.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.background(
                        Color.White.copy(alpha = 0.3f),
                        CircleShape
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
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

/**
 * Dialog for linking a tag to a system album.
 * Default: Create new album
 * Secondary option: Link existing album
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkAlbumDialog(
    tagName: String,
    availableAlbums: List<Album>,
    isLoadingAlbums: Boolean,
    onDismiss: () -> Unit,
    onCreateNewAlbum: (albumName: String, copyMode: AlbumCopyMode) -> Unit,
    onLinkExistingAlbum: (album: Album, copyMode: AlbumCopyMode) -> Unit
) {
    var mode by remember { mutableStateOf(LinkMode.CREATE_NEW) }
    var albumName by remember { mutableStateOf(tagName) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var copyMode by remember { mutableStateOf(AlbumCopyMode.COPY) }
    var showExistingAlbumPicker by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "关联系统相册",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "将「$tagName」标签关联到系统相册，方便在其他应用中快速访问这些照片",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Mode selection
            Text(
                text = "选择关联方式",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Create new album option (default, highlighted)
            ModeCard(
                icon = Icons.Default.CreateNewFolder,
                title = "新建相册",
                subtitle = "创建一个新的系统相册",
                isSelected = mode == LinkMode.CREATE_NEW,
                isRecommended = true,
                onClick = { mode = LinkMode.CREATE_NEW }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Link existing album option
            ModeCard(
                icon = Icons.Default.PhotoAlbum,
                title = "关联已有相册",
                subtitle = if (selectedAlbum != null) "已选择: ${selectedAlbum?.name}" else "从现有系统相册中选择",
                isSelected = mode == LinkMode.LINK_EXISTING,
                onClick = { 
                    mode = LinkMode.LINK_EXISTING
                    showExistingAlbumPicker = true
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Mode-specific content
            if (mode == LinkMode.CREATE_NEW) {
                // Album name input
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    label = { Text("相册名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Copy mode selection
                Text(
                    text = "照片处理方式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                CopyModeOption(
                    icon = Icons.Default.ContentCopy,
                    title = "复制照片",
                    subtitle = "将照片复制到新相册，原照片保留在原位置",
                    isSelected = copyMode == AlbumCopyMode.COPY,
                    onClick = { copyMode = AlbumCopyMode.COPY }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                CopyModeOption(
                    icon = Icons.Default.DriveFileMove,
                    title = "移动照片",
                    subtitle = "将照片移动到新相册，原位置不再保留",
                    isSelected = copyMode == AlbumCopyMode.MOVE,
                    onClick = { copyMode = AlbumCopyMode.MOVE }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                
                FilledTonalButton(
                    onClick = {
                        when (mode) {
                            LinkMode.CREATE_NEW -> {
                                if (albumName.isNotBlank()) {
                                    onCreateNewAlbum(albumName.trim(), copyMode)
                                }
                            }
                            LinkMode.LINK_EXISTING -> {
                                selectedAlbum?.let { album ->
                                    onLinkExistingAlbum(album, copyMode)
                                }
                            }
                        }
                    },
                    enabled = when (mode) {
                        LinkMode.CREATE_NEW -> albumName.isNotBlank()
                        LinkMode.LINK_EXISTING -> selectedAlbum != null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("关联")
                }
            }
        }
    }
    
    // Existing album picker dialog
    if (showExistingAlbumPicker) {
        AlbumPickerDialog(
            albums = availableAlbums,
            isLoading = isLoadingAlbums,
            selectedAlbum = selectedAlbum,
            onSelectAlbum = { album ->
                selectedAlbum = album
                showExistingAlbumPicker = false
            },
            onDismiss = { showExistingAlbumPicker = false }
        )
    }
}

private enum class LinkMode {
    CREATE_NEW,
    LINK_EXISTING
}

@Composable
private fun ModeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isRecommended: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (isRecommended) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "推荐",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CopyModeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(20.dp)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Dialog for picking an existing album.
 */
@Composable
private fun AlbumPickerDialog(
    albums: List<Album>,
    isLoading: Boolean,
    selectedAlbum: Album?,
    onSelectAlbum: (Album) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("选择相册")
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (albums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoAlbum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "没有可用的相册",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "所有相册已被关联或没有找到系统相册",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(albums) { album ->
                        AlbumItem(
                            album = album,
                            isSelected = selectedAlbum?.id == album.id,
                            onClick = { onSelectAlbum(album) }
                        )
                    }
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
private fun AlbumItem(
    album: Album,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (album.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(album.coverUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.photoCount} 张照片",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
