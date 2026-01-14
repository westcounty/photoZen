package com.example.photozen.ui.screens.lighttable

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * Light Table Screen - Photo comparison with synchronized zoom.
 * 
 * Two modes:
 * 1. SELECTION: Grid of "Maybe" photos, select 2-4 for comparison
 * 2. COMPARISON: Synchronized zoom/pan view, select "best" photo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightTableScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LightTableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    
    // Shared transform state for synchronized zoom
    val transformState = rememberTransformState()
    
    // Handle back press in comparison mode
    BackHandler(enabled = uiState.mode == LightTableMode.COMPARISON) {
        viewModel.exitComparison()
        transformState.reset()
    }
    
    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
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
                            text = when (uiState.mode) {
                                LightTableMode.SELECTION -> "Light Table"
                                LightTableMode.COMPARISON -> "照片对比"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = when (uiState.mode) {
                                LightTableMode.SELECTION -> "${uiState.allMaybePhotos.size} 张待定照片"
                                LightTableMode.COMPARISON -> "双击缩放 · 拖动平移"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.mode == LightTableMode.COMPARISON) {
                                viewModel.exitComparison()
                                transformState.reset()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (uiState.mode == LightTableMode.SELECTION) {
                        // Select all button
                        IconButton(
                            onClick = { viewModel.selectAll() },
                            enabled = uiState.allMaybePhotos.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "全选"
                            )
                        }
                    } else {
                        // Reset zoom button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                transformState.reset()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "重置缩放"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            when (uiState.mode) {
                LightTableMode.SELECTION -> SelectionBottomBar(
                    selectionCount = uiState.selectionCount,
                    canCompare = uiState.canCompare,
                    onClearSelection = { viewModel.clearSelection() },
                    onStartComparison = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.startComparison()
                    }
                )
                LightTableMode.COMPARISON -> ComparisonBottomBar(
                    hasBestSelected = uiState.bestPhotoId != null,
                    onKeepBest = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.keepBestTrashRest()
                    },
                    onKeepAll = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.keepAllSelected()
                    },
                    onTrashAll = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.trashAllSelected()
                    }
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
                    LoadingContent()
                }
                uiState.allMaybePhotos.isEmpty() -> {
                    EmptyContent(onNavigateBack = onNavigateBack)
                }
                else -> {
                    AnimatedContent(
                        targetState = uiState.mode,
                        transitionSpec = {
                            fadeIn() + scaleIn(initialScale = 0.95f) togetherWith
                                fadeOut() + scaleOut(targetScale = 0.95f)
                        },
                        label = "mode_transition"
                    ) { mode ->
                        when (mode) {
                            LightTableMode.SELECTION -> {
                                PhotoThumbnailGrid(
                                    photos = uiState.allMaybePhotos,
                                    selectedIds = uiState.selectedForComparison,
                                    onToggleSelection = { viewModel.toggleSelection(it) }
                                )
                            }
                            LightTableMode.COMPARISON -> {
                                ComparisonGrid(
                                    photos = uiState.comparisonPhotos,
                                    transformState = transformState,
                                    selectedPhotoId = uiState.bestPhotoId,
                                    onSelectPhoto = { viewModel.selectBestPhoto(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom bar for selection mode.
 */
@Composable
private fun SelectionBottomBar(
    selectionCount: Int,
    canCompare: Boolean,
    onClearSelection: () -> Unit,
    onStartComparison: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaybeAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectionCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaybeAmber
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "已选择 (2-4张)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedVisibility(
                    visible = selectionCount > 0,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    OutlinedButton(onClick = onClearSelection) {
                        Text("清除")
                    }
                }
                
                Button(
                    onClick = onStartComparison,
                    enabled = canCompare,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaybeAmber
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("对比", color = Color.Black)
                }
            }
        }
    }
}

/**
 * Bottom bar for comparison mode.
 */
@Composable
private fun ComparisonBottomBar(
    hasBestSelected: Boolean,
    onKeepBest: () -> Unit,
    onKeepAll: () -> Unit,
    onTrashAll: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trash all
            FilledTonalButton(
                onClick = onTrashAll,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = TrashRed.copy(alpha = 0.15f),
                    contentColor = TrashRed
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("全部删除")
            }
            
            // Keep best (only if one selected)
            Button(
                onClick = onKeepBest,
                enabled = hasBestSelected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeepGreen
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Star, null, Modifier.size(18.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("保留最佳", color = Color.White)
            }
            
            // Keep all
            FilledTonalButton(
                onClick = onKeepAll,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = KeepGreen.copy(alpha = 0.15f),
                    contentColor = KeepGreen
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("全部保留")
            }
        }
    }
}

/**
 * Loading state content.
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在加载...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Empty state when no "Maybe" photos.
 */
@Composable
private fun EmptyContent(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaybeAmber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = MaybeAmber,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "没有待定照片",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "在 Flow Sorter 中向上滑动可将照片\n标记为「待定」以便稍后比较",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onNavigateBack) {
            Text("返回")
        }
    }
}

/**
 * Light Table Content - Reusable content for both standalone and workflow modes.
 * 
 * @param isWorkflowMode When true, auto-completes when all photos are processed
 * @param onComplete Callback when all "Maybe" photos have been processed
 * @param onNavigateBack Callback for navigation back (standalone mode only)
 */
@Composable
fun LightTableContent(
    isWorkflowMode: Boolean = false,
    onComplete: (() -> Unit)? = null,
    onNavigateBack: () -> Unit,
    viewModel: LightTableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val transformState = rememberTransformState()
    
    // Handle back press in comparison mode
    BackHandler(enabled = uiState.mode == LightTableMode.COMPARISON) {
        viewModel.exitComparison()
        transformState.reset()
    }
    
    // Auto-complete when no more "Maybe" photos in workflow mode
    LaunchedEffect(uiState.allMaybePhotos.isEmpty(), uiState.isLoading) {
        if (isWorkflowMode && !uiState.isLoading && uiState.allMaybePhotos.isEmpty()) {
            onComplete?.invoke()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        LoadingContent()
                    }
                    uiState.allMaybePhotos.isEmpty() -> {
                        if (isWorkflowMode) {
                            // In workflow mode, show completion message
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = KeepGreen,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "对比完成",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            EmptyContent(onNavigateBack = onNavigateBack)
                        }
                    }
                    else -> {
                        AnimatedContent(
                            targetState = uiState.mode,
                            transitionSpec = {
                                fadeIn() + scaleIn(initialScale = 0.95f) togetherWith
                                    fadeOut() + scaleOut(targetScale = 0.95f)
                            },
                            label = "mode_transition"
                        ) { mode ->
                            when (mode) {
                                LightTableMode.SELECTION -> {
                                    PhotoThumbnailGrid(
                                        photos = uiState.allMaybePhotos,
                                        selectedIds = uiState.selectedForComparison,
                                        onToggleSelection = { viewModel.toggleSelection(it) }
                                    )
                                }
                                LightTableMode.COMPARISON -> {
                                    ComparisonGrid(
                                        photos = uiState.comparisonPhotos,
                                        transformState = transformState,
                                        selectedPhotoId = uiState.bestPhotoId,
                                        onSelectPhoto = { viewModel.selectBestPhoto(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom bar (only when there are photos)
            if (!uiState.isLoading && uiState.allMaybePhotos.isNotEmpty()) {
                when (uiState.mode) {
                    LightTableMode.SELECTION -> SelectionBottomBar(
                        selectionCount = uiState.selectionCount,
                        canCompare = uiState.canCompare,
                        onClearSelection = { viewModel.clearSelection() },
                        onStartComparison = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startComparison()
                        }
                    )
                    LightTableMode.COMPARISON -> ComparisonBottomBar(
                        hasBestSelected = uiState.bestPhotoId != null,
                        onKeepBest = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.keepBestTrashRest()
                        },
                        onKeepAll = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.keepAllSelected()
                        },
                        onTrashAll = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.trashAllSelected()
                        }
                    )
                }
            }
        }
    }
}
