package com.example.photozen.ui.screens.trash

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.photozen.ui.components.ConfirmDeleteSheet
import com.example.photozen.ui.components.DeleteType
import com.example.photozen.ui.components.DragSelectPhotoGrid
import com.example.photozen.ui.components.EmptyStates
import com.example.photozen.ui.components.SelectionTopBar
import com.example.photozen.ui.components.SelectionBottomBar
import com.example.photozen.ui.components.BottomBarConfigs
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    
    // Phase 3-9: 删除确认弹窗状态
    var showDeleteConfirmSheet by remember { mutableStateOf(false) }
    
    // Launcher for system delete request
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeleteComplete(result.resultCode == Activity.RESULT_OK)
    }
    
    // Launch system delete dialog when intent sender is available
    LaunchedEffect(uiState.deleteIntentSender) {
        uiState.deleteIntentSender?.let { intentSender ->
            deleteLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
            viewModel.clearIntentSender()
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
                                text = "回收站",
                                style = MaterialTheme.typography.titleLarge
                            )
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
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "返回"
                            )
                        }
                    },
                    actions = {
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
                        
                        // Batch management mode
                        if (uiState.photos.isNotEmpty()) {
                            IconButton(onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.enterSelectionMode()
                            }) {
                                Icon(
                                    Icons.Default.Checklist,
                                    "批量管理"
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
            // Phase 4: 使用 BottomBarConfigs
            if (uiState.isSelectionMode && uiState.selectedCount > 0) {
                val actions = BottomBarConfigs.trashListMultiSelect(
                    onKeep = { viewModel.keepSelected() },
                    onMaybe = { viewModel.maybeSelected() },
                    onReset = { viewModel.restoreSelected() },
                    onPermanentDelete = { showDeleteConfirmSheet = true }
                )
                SelectionBottomBar(actions = actions)
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
                    EmptyStates.EmptyTrash(modifier = Modifier.fillMaxSize())
                }
                else -> {
                    DragSelectPhotoGrid(
                        photos = uiState.photos,
                        selectedIds = uiState.selectedIds,
                        onSelectionChanged = { newSelection ->
                            viewModel.updateSelection(newSelection)
                        },
                        onPhotoClick = { _, _ ->
                            // Non-selection mode click - no action in trash
                        },
                        onPhotoLongPress = { _, _ ->
                            // No action sheet for trash items
                        },
                        columns = uiState.gridColumns,
                        selectionColor = TrashRed
                    )
                }
            }
        }
    }
    
    // Phase 3-9: 永久删除确认弹窗
    if (showDeleteConfirmSheet) {
        val selectedPhotos = uiState.photos.filter { it.id in uiState.selectedIds }
        ConfirmDeleteSheet(
            photos = selectedPhotos,
            deleteType = DeleteType.PERMANENT_DELETE,
            onConfirm = {
                showDeleteConfirmSheet = false
                viewModel.requestPermanentDelete()
            },
            onDismiss = { showDeleteConfirmSheet = false },
            isLoading = uiState.isDeleting
        )
    }
}

// Phase 4: TrashSelectionBottomBar 和 TrashBottomBarActionItem 已迁移到 BottomActionBar.kt
