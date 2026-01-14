package com.example.photozen.ui.screens.photolist

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit = {},
    onNavigateToQuickTag: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PhotoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
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
    
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // Show Quick Tag FAB only for KEEP status
            if (uiState.status == PhotoStatus.KEEP && uiState.photos.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToQuickTag,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Label, contentDescription = null)
                    Text(
                        text = "快速分类",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.photos.isEmpty() -> {
                    EmptyState(status = uiState.status, color = color)
                }
                else -> {
                    PhotoGrid(
                        photos = uiState.photos,
                        currentStatus = uiState.status,
                        onMoveToKeep = { viewModel.moveToKeep(it) },
                        onMoveToTrash = { viewModel.moveToTrash(it) },
                        onMoveToMaybe = { viewModel.moveToMaybe(it) },
                        onResetToUnsorted = { viewModel.resetToUnsorted(it) },
                        onEdit = onNavigateToEditor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    photos: List<PhotoEntity>,
    currentStatus: PhotoStatus,
    onMoveToKeep: (String) -> Unit,
    onMoveToTrash: (String) -> Unit,
    onMoveToMaybe: (String) -> Unit,
    onResetToUnsorted: (String) -> Unit,
    onEdit: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(photos, key = { it.id }) { photo ->
            PhotoGridItem(
                photo = photo,
                currentStatus = currentStatus,
                onMoveToKeep = onMoveToKeep,
                onMoveToTrash = onMoveToTrash,
                onMoveToMaybe = onMoveToMaybe,
                onResetToUnsorted = onResetToUnsorted,
                onEdit = onEdit
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: PhotoEntity,
    currentStatus: PhotoStatus,
    onMoveToKeep: (String) -> Unit,
    onMoveToTrash: (String) -> Unit,
    onMoveToMaybe: (String) -> Unit,
    onResetToUnsorted: (String) -> Unit,
    onEdit: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(photo.systemUri))
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // Edit option - always show first
            DropdownMenuItem(
                text = { Text("编辑照片") },
                onClick = {
                    onEdit(photo.id)
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                }
            )
            
            if (currentStatus != PhotoStatus.KEEP) {
                DropdownMenuItem(
                    text = { Text("移至保留") },
                    onClick = {
                        onMoveToKeep(photo.id)
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Favorite, null, tint = KeepGreen)
                    }
                )
            }
            if (currentStatus != PhotoStatus.MAYBE) {
                DropdownMenuItem(
                    text = { Text("标记待定") },
                    onClick = {
                        onMoveToMaybe(photo.id)
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.QuestionMark, null, tint = MaybeAmber)
                    }
                )
            }
            if (currentStatus != PhotoStatus.TRASH) {
                DropdownMenuItem(
                    text = { Text("移至回收站") },
                    onClick = {
                        onMoveToTrash(photo.id)
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, null, tint = TrashRed)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("恢复未整理") },
                onClick = {
                    onResetToUnsorted(photo.id)
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Restore, null)
                }
            )
        }
    }
}

@Composable
private fun EmptyState(status: PhotoStatus, color: Color) {
    val (icon, text) = when (status) {
        PhotoStatus.KEEP -> Icons.Default.Favorite to "没有保留的照片"
        PhotoStatus.TRASH -> Icons.Default.Delete to "回收站为空"
        PhotoStatus.MAYBE -> Icons.Default.QuestionMark to "没有待定的照片"
        PhotoStatus.UNSORTED -> Icons.Default.Check to "所有照片已整理完成"
    }
    
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
