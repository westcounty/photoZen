package com.example.photozen.ui.screens.editor

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber

/**
 * Photo Editor Screen - Non-destructive cropping and virtual copy management.
 * 
 * Features:
 * - Interactive crop with pinch-to-zoom and pan
 * - Save crop state (non-destructive, metadata only)
 * - Create virtual copies
 * - View and manage virtual copies
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }
    
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
                            text = "编辑照片",
                            style = MaterialTheme.typography.titleLarge
                        )
                        uiState.photo?.let { photo ->
                            Text(
                                text = photo.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // Save button
                    AnimatedVisibility(
                        visible = uiState.hasChanges && !uiState.isSaving,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = { viewModel.saveCropState() }) {
                            Icon(Icons.Default.Save, "保存", tint = KeepGreen)
                        }
                    }
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Crop preview area
                uiState.photo?.let { photo ->
                    CropPreviewArea(
                        photo = photo,
                        scale = uiState.currentCropState.scale,
                        offsetX = uiState.currentCropState.offsetX,
                        offsetY = uiState.currentCropState.offsetY,
                        onTransform = { scale, offsetX, offsetY ->
                            viewModel.updateCropScale(scale)
                            viewModel.updateCropOffset(offsetX, offsetY)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                
                // Crop tools
                CropToolsBar(
                    hasChanges = uiState.hasChanges,
                    isSaving = uiState.isSaving,
                    onReset = { viewModel.resetCrop() },
                    onSave = { viewModel.saveCropState() }
                )
                
                // Virtual copy section
                VirtualCopySection(
                    photo = uiState.photo,
                    virtualCopies = uiState.virtualCopies,
                    isSaving = uiState.isSaving,
                    onCreateCopy = { viewModel.createVirtualCopy() },
                    onCopyClick = onNavigateToPhoto
                )
            }
        }
    }
}

@Composable
private fun CropPreviewArea(
    photo: PhotoEntity,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onTransform: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScale by remember { mutableFloatStateOf(scale) }
    var currentOffsetX by remember { mutableFloatStateOf(offsetX) }
    var currentOffsetY by remember { mutableFloatStateOf(offsetY) }
    
    // Update local state when external state changes
    LaunchedEffect(scale, offsetX, offsetY) {
        currentScale = scale
        currentOffsetX = offsetX
        currentOffsetY = offsetY
    }
    
    // Calculate aspect ratio
    val aspectRatio = if (photo.width > 0 && photo.height > 0) {
        photo.width.toFloat() / photo.height.toFloat()
    } else {
        4f / 3f
    }
    
    Box(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // Crop frame indicator
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
                .border(2.dp, MaybeAmber, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        currentScale = (currentScale * zoom).coerceIn(0.5f, 3f)
                        currentOffsetX += pan.x
                        currentOffsetY += pan.y
                        
                        // Limit offset
                        val maxOffset = 300f * currentScale
                        currentOffsetX = currentOffsetX.coerceIn(-maxOffset, maxOffset)
                        currentOffsetY = currentOffsetY.coerceIn(-maxOffset, maxOffset)
                        
                        onTransform(currentScale, currentOffsetX, currentOffsetY)
                    }
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(photo.systemUri))
                    .crossfade(true)
                    .build(),
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = currentScale
                        scaleY = currentScale
                        translationX = currentOffsetX
                        translationY = currentOffsetY
                    }
            )
        }
        
        // Crop instructions
        Text(
            text = "双指缩放和平移调整裁切区域",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun CropToolsBar(
    hasChanges: Boolean,
    isSaving: Boolean,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Reset button
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            enabled = hasChanges && !isSaving
        ) {
            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("重置")
        }
        
        // Save button
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            enabled = hasChanges && !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = KeepGreen)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Done, null, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text("保存裁切")
        }
    }
}

@Composable
private fun VirtualCopySection(
    photo: PhotoEntity?,
    virtualCopies: List<PhotoEntity>,
    isSaving: Boolean,
    onCreateCopy: () -> Unit,
    onCopyClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "虚拟副本",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "创建多个版本，不占用额外存储空间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                FilledTonalButton(
                    onClick = onCreateCopy,
                    enabled = !isSaving && photo != null
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("创建副本")
                }
            }
            
            if (virtualCopies.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "${virtualCopies.size} 个虚拟副本",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(virtualCopies, key = { it.id }) { copy ->
                        VirtualCopyThumbnail(
                            copy = copy,
                            onClick = { onCopyClick(copy.id) }
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "没有虚拟副本。创建副本可以让您为同一张照片保存不同的裁切效果。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun VirtualCopyThumbnail(
    copy: PhotoEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(copy.systemUri))
                    .crossfade(true)
                    .build(),
                contentDescription = copy.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = copy.cropState.scale
                        scaleY = copy.cropState.scale
                        translationX = copy.cropState.offsetX
                        translationY = copy.cropState.offsetY
                    }
            )
            
            // Virtual copy badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
