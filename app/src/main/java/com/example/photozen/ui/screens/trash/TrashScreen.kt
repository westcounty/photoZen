package com.example.photozen.ui.screens.trash

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
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
import com.example.photozen.ui.components.DragSelectPhotoGrid
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

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
    
    // Handle back press in selection mode
    val handleBack: () -> Unit = {
        if (uiState.isSelectionMode) {
            viewModel.clearSelection()
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
                    Column {
                        Text(
                            text = if (uiState.isSelectionMode) "已选择 ${uiState.selectedCount} 项" else "回收站",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!uiState.isSelectionMode) {
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
                            if (uiState.isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            "返回"
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        // Select all / Deselect all
                        TextButton(
                            onClick = {
                                if (uiState.allSelected) viewModel.clearSelection() else viewModel.selectAll()
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (uiState.isSelectionMode && uiState.selectedCount > 0) {
                TrashSelectionBottomBar(
                    onRestore = { viewModel.restoreSelected() },
                    onKeep = { viewModel.keepSelected() },
                    onMaybe = { viewModel.maybeSelected() },
                    onDelete = { viewModel.requestPermanentDelete() },
                    isDeleting = uiState.isDeleting
                )
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
                    EmptyTrashState()
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
}

@Composable
private fun TrashSelectionBottomBar(
    onRestore: () -> Unit,
    onKeep: () -> Unit,
    onMaybe: () -> Unit,
    onDelete: () -> Unit,
    isDeleting: Boolean
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
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Keep button
            FilledTonalButton(
                onClick = onKeep,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = KeepGreen.copy(alpha = 0.15f),
                    contentColor = KeepGreen
                )
            ) {
                Icon(Icons.Default.Favorite, null, Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("保留", maxLines = 1)
            }
            
            // Maybe button
            FilledTonalButton(
                onClick = onMaybe,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaybeAmber.copy(alpha = 0.15f),
                    contentColor = MaybeAmber
                )
            ) {
                Icon(Icons.Default.QuestionMark, null, Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("待定", maxLines = 1)
            }
            
            // Restore button
            FilledTonalButton(
                onClick = onRestore,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("重置")
            }
            
            // Delete button
            Button(
                onClick = onDelete,
                enabled = !isDeleting,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = TrashRed)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.DeleteForever, null, Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("彻底删除")
            }
        }
    }
}

@Composable
private fun EmptyTrashState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(TrashRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Delete,
                null,
                tint = TrashRed,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = "回收站为空",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "整理时向左滑动的照片会显示在这里",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
        )
    }
}
