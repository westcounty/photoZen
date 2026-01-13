package com.example.photozen.ui.screens.editor

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.AspectRatio
import com.example.photozen.data.model.CropState
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.launch

/**
 * Photo Editor Screen - Non-destructive cropping and virtual copy management.
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
    
    var photoToDelete by remember { mutableStateOf<PhotoEntity?>(null) }
    var editingVirtualCopy by remember { mutableStateOf<PhotoEntity?>(null) }
    
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
    
    // Delete confirmation dialog
    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("删除虚拟副本") },
            text = { Text("确定要删除这个虚拟副本吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVirtualCopy(photo.id)
                        if (editingVirtualCopy?.id == photo.id) {
                            editingVirtualCopy = null
                        }
                        photoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrashRed)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
    
    // Use Box to layer preview on top of everything
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when {
                                uiState.isVirtualCopy -> "预览虚拟副本"
                                editingVirtualCopy != null -> "编辑副本"
                                else -> "编辑照片"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = editingVirtualCopy?.let { "副本 #${uiState.virtualCopies.indexOf(it) + 1}" }
                                ?: uiState.photo?.displayName ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (editingVirtualCopy != null) {
                            editingVirtualCopy = null
                            viewModel.resetCrop()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
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
                val displayPhoto = editingVirtualCopy ?: uiState.photo
                
                displayPhoto?.let { photo ->
                    CropPreviewArea(
                        photo = photo,
                        cropState = uiState.currentCropState,
                        aspectRatio = uiState.selectedAspectRatio,
                        isEditable = !uiState.isVirtualCopy,
                        onTransform = { scale, offsetX, offsetY ->
                            viewModel.updateCropScale(scale)
                            viewModel.updateCropOffset(offsetX, offsetY)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                
                if (!uiState.isVirtualCopy) {
                    AspectRatioSelector(
                        selectedRatio = uiState.selectedAspectRatio,
                        onSelectRatio = { viewModel.selectAspectRatio(it) }
                    )
                    
                    RotationSlider(
                        rotation = uiState.currentCropState.rotation,
                        onRotationChange = { viewModel.updateRotation(it) }
                    )
                    
                    CropToolsBar(
                        hasChanges = uiState.hasChanges,
                        isSaving = uiState.isSaving,
                        isEditingCopy = editingVirtualCopy != null,
                        onReset = { viewModel.resetCrop() },
                        onSave = { 
                            if (editingVirtualCopy != null) {
                                viewModel.saveVirtualCopyCropState(editingVirtualCopy!!.id)
                                editingVirtualCopy = null
                            } else {
                                viewModel.saveCropState() 
                            }
                        }
                    )
                    
                    VirtualCopySection(
                        photo = uiState.photo,
                        virtualCopies = uiState.virtualCopies,
                        editingCopyId = editingVirtualCopy?.id,
                        isSaving = uiState.isSaving,
                        onCreateCopy = { viewModel.createVirtualCopy() },
                        onCopySelect = { copy ->
                            editingVirtualCopy = copy
                            viewModel.loadCropStateFromCopy(copy)
                        },
                        onCopyPreview = { copy ->
                            viewModel.previewVirtualCopy(copy)
                        },
                        onCopyDelete = { copy ->
                            photoToDelete = copy
                        }
                    )
                } else {
                    VirtualCopyActionsCard(
                        isSaving = uiState.isSavingToFile,
                        onSaveToFile = { 
                            uiState.photo?.let { viewModel.saveToNewImage(it) }
                        }
                    )
                }
            }
        }
        }
        
        // Fullscreen preview overlay - on top of everything
        uiState.previewingVirtualCopy?.let { initialCopy ->
            if (uiState.virtualCopies.isNotEmpty()) {
                VirtualCopyPagerPreview(
                    virtualCopies = uiState.virtualCopies,
                    initialCopy = initialCopy,
                    isSaving = uiState.isSavingToFile,
                    onDismiss = { viewModel.dismissPreview() },
                    onSaveToFile = { photo -> viewModel.saveToNewImage(photo) }
                )
            }
        }
    }
}

@Composable
private fun CropPreviewArea(
    photo: PhotoEntity,
    cropState: CropState,
    aspectRatio: AspectRatio,
    isEditable: Boolean,
    onTransform: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScale by remember { mutableFloatStateOf(cropState.scale) }
    var currentOffsetX by remember { mutableFloatStateOf(cropState.offsetX) }
    var currentOffsetY by remember { mutableFloatStateOf(cropState.offsetY) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    LaunchedEffect(cropState) {
        currentScale = cropState.scale
        currentOffsetX = cropState.offsetX
        currentOffsetY = cropState.offsetY
    }
    
    val originalRatio = if (photo.width > 0 && photo.height > 0) {
        photo.width.toFloat() / photo.height.toFloat()
    } else {
        4f / 3f
    }
    
    val effectiveFrameRatio = when (aspectRatio) {
        AspectRatio.ORIGINAL -> originalRatio
        else -> aspectRatio.ratio ?: originalRatio
    }
    
    Box(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val maxWidth = maxWidth * 0.9f
            val maxHeight = maxHeight * 0.85f
            
            val frameWidth: androidx.compose.ui.unit.Dp
            val frameHeight: androidx.compose.ui.unit.Dp
            
            if (effectiveFrameRatio > maxWidth / maxHeight) {
                frameWidth = maxWidth
                frameHeight = maxWidth / effectiveFrameRatio
            } else {
                frameHeight = maxHeight
                frameWidth = maxHeight * effectiveFrameRatio
            }
            
            // Crop frame
            Box(
                modifier = Modifier
                    .size(frameWidth, frameHeight)
                    .drawCropFrame()
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (isEditable) {
                            Modifier.pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    currentScale = (currentScale * zoom).coerceIn(0.5f, 10f)
                                    currentOffsetX += pan.x
                                    currentOffsetY += pan.y
                                    
                                    val maxOffset = 500f * currentScale
                                    currentOffsetX = currentOffsetX.coerceIn(-maxOffset, maxOffset)
                                    currentOffsetY = currentOffsetY.coerceIn(-maxOffset, maxOffset)
                                    
                                    onTransform(currentScale, currentOffsetX, currentOffsetY)
                                }
                            }
                        } else Modifier
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(photo.systemUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = currentScale
                            scaleY = currentScale
                            translationX = currentOffsetX
                            translationY = currentOffsetY
                            rotationZ = cropState.rotation
                        }
                )
            }
        }
        
        if (isEditable) {
            Text(
                text = "双指缩放(最大10倍)和平移",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }
    }
}

private fun Modifier.drawCropFrame(): Modifier = this.drawBehind {
    drawRoundRect(
        color = Color.White.copy(alpha = 0.6f),
        size = size,
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

@Composable
private fun AspectRatioSelector(
    selectedRatio: AspectRatio,
    onSelectRatio: (AspectRatio) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "长宽比",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AspectRatio.entries.forEach { ratio ->
                FilterChip(
                    selected = selectedRatio == ratio,
                    onClick = { onSelectRatio(ratio) },
                    label = { Text(ratio.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaybeAmber,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }
    }
}

@Composable
private fun RotationSlider(
    rotation: Float,
    onRotationChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "旋转",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${rotation.toInt()}°",
                style = MaterialTheme.typography.labelMedium,
                color = MaybeAmber,
                fontWeight = FontWeight.Bold
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onRotationChange((rotation - 5f).coerceAtLeast(-45f)) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.RotateLeft,
                    "向左旋转",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Slider(
                value = rotation,
                onValueChange = onRotationChange,
                valueRange = -45f..45f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaybeAmber,
                    activeTrackColor = MaybeAmber
                )
            )
            
            IconButton(
                onClick = { onRotationChange((rotation + 5f).coerceAtMost(45f)) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.RotateRight,
                    "向右旋转",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CropToolsBar(
    hasChanges: Boolean,
    isSaving: Boolean,
    isEditingCopy: Boolean,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            enabled = hasChanges && !isSaving
        ) {
            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("重置")
        }
        
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
            Text(if (isEditingCopy) "保存副本" else "保存裁切")
        }
    }
}

@Composable
private fun VirtualCopySection(
    photo: PhotoEntity?,
    virtualCopies: List<PhotoEntity>,
    editingCopyId: String?,
    isSaving: Boolean,
    onCreateCopy: () -> Unit,
    onCopySelect: (PhotoEntity) -> Unit,
    onCopyPreview: (PhotoEntity) -> Unit,
    onCopyDelete: (PhotoEntity) -> Unit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "虚拟副本",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "保存不同裁切版本",
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
                    Text("创建")
                }
            }
            
            if (virtualCopies.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(virtualCopies, key = { it.id }) { copy ->
                        VirtualCopyItem(
                            copy = copy,
                            index = virtualCopies.indexOf(copy) + 1,
                            isSelected = editingCopyId == copy.id,
                            onEditClick = { onCopySelect(copy) },
                            onPreviewClick = { onCopyPreview(copy) },
                            onDeleteClick = { onCopyDelete(copy) }
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "调整裁切后点击「创建」保存为副本",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Material Design 3 style virtual copy item
 */
@Composable
private fun VirtualCopyItem(
    copy: PhotoEntity,
    index: Int,
    isSelected: Boolean,
    onEditClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cropState = copy.cropState
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thumbnail card - using Card instead of Surface for better touch handling
        Card(
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 4.dp else 1.dp
            ),
            border = if (isSelected) {
                androidx.compose.foundation.BorderStroke(2.dp, MaybeAmber)
            } else null
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Image with crop applied
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(copy.systemUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = cropState.scale
                            scaleY = cropState.scale
                            translationX = cropState.offsetX * 0.1f
                            translationY = cropState.offsetY * 0.1f
                            rotationZ = cropState.rotation
                        }
                )
                
                // Overlay gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
                
                // Selected indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(22.dp)
                            .background(MaybeAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                // Preview button - use Box with clickable for reliable touch
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        )
                        .clip(CircleShape)
                        .clickable { onPreviewClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "预览",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Action row - use Box with clickable for reliable touch
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isSelected) MaybeAmber.copy(alpha = 0.3f) 
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(onClick = onEditClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    "编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Delete button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(TrashRed.copy(alpha = 0.15f), CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    "删除",
                    tint = TrashRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Label
        Text(
            text = "副本 $index",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun VirtualCopyActionsCard(
    isSaving: Boolean,
    onSaveToFile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ContentCopy,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "这是一个虚拟副本",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "可导出为新图片保存到相册",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onSaveToFile,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = KeepGreen)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.SaveAlt, null, Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("导出为新图片")
            }
        }
    }
}

/**
 * Fullscreen preview with horizontal pager
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VirtualCopyPagerPreview(
    virtualCopies: List<PhotoEntity>,
    initialCopy: PhotoEntity,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveToFile: (PhotoEntity) -> Unit
) {
    val initialIndex = virtualCopies.indexOfFirst { it.id == initialCopy.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex) { virtualCopies.size }
    val scope = rememberCoroutineScope()
    
    val currentPhoto by remember(pagerState.currentPage) {
        derivedStateOf { virtualCopies.getOrNull(pagerState.currentPage) }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = virtualCopies[page]
            SingleCopyPreview(photo = photo)
        }
        
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Close, "关闭")
            }
            
            currentPhoto?.let { photo ->
                Button(
                    onClick = { onSaveToFile(photo) },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = KeepGreen)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.SaveAlt, null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("导出图片")
                }
            }
        }
        
        // Navigation and info
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Navigation
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.05f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一个")
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text(
                    text = "${pagerState.currentPage + 1} / ${virtualCopies.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.width(16.dp))
                
                FilledTonalIconButton(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage < virtualCopies.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage < virtualCopies.size - 1,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.05f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一个")
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            currentPhoto?.let { photo ->
                Text(
                    text = "缩放 ${(photo.cropState.scale * 100).toInt()}% · 旋转 ${photo.cropState.rotation.toInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Text(
                text = "左右滑动切换 · 双指缩放查看",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Single copy preview with proper gesture handling to not conflict with HorizontalPager.
 * Shows a crop frame border to indicate the actual cropped area.
 */
@Composable
private fun SingleCopyPreview(photo: PhotoEntity) {
    val cropState = photo.cropState
    val density = LocalDensity.current
    
    var viewScale by remember { mutableFloatStateOf(1f) }
    var viewOffsetX by remember { mutableFloatStateOf(0f) }
    var viewOffsetY by remember { mutableFloatStateOf(0f) }
    
    // Track if user is actively zooming (multi-touch)
    var isZooming by remember { mutableStateOf(false) }
    
    val animatedScale by animateFloatAsState(
        targetValue = viewScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Determine if we should handle panning (only when zoomed in)
    val shouldHandlePan = viewScale > 1.05f
    
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxWidth = maxWidth
            val maxHeight = maxHeight
            
            // Calculate crop frame size based on aspect ratio
            val aspectRatio = AspectRatio.fromId(cropState.aspectRatioId)
            val originalRatio = if (photo.width > 0 && photo.height > 0) {
                photo.width.toFloat() / photo.height.toFloat()
            } else {
                4f / 3f
            }
            val effectiveRatio = aspectRatio.ratio ?: originalRatio
            
            val frameWidth: androidx.compose.ui.unit.Dp
            val frameHeight: androidx.compose.ui.unit.Dp
            
            if (effectiveRatio > maxWidth / maxHeight) {
                frameWidth = maxWidth * 0.85f
                frameHeight = frameWidth / effectiveRatio
            } else {
                frameHeight = maxHeight * 0.75f
                frameWidth = frameHeight * effectiveRatio
            }
            
            // Crop frame container - shows the actual visible area with a subtle border
            Box(
                modifier = Modifier
                    .size(frameWidth, frameHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    // Subtle dashed-style border to indicate crop area
                    .drawBehind {
                        // Draw subtle corner indicators instead of full border
                        val strokeWidth = 2.dp.toPx()
                        val cornerLength = 20.dp.toPx()
                        val indicatorColor = Color.White.copy(alpha = 0.4f)
                        
                        // Top-left corner
                        drawLine(
                            color = indicatorColor,
                            start = Offset(0f, strokeWidth / 2),
                            end = Offset(cornerLength, strokeWidth / 2),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = indicatorColor,
                            start = Offset(strokeWidth / 2, 0f),
                            end = Offset(strokeWidth / 2, cornerLength),
                            strokeWidth = strokeWidth
                        )
                        
                        // Top-right corner
                        drawLine(
                            color = indicatorColor,
                            start = Offset(size.width - cornerLength, strokeWidth / 2),
                            end = Offset(size.width, strokeWidth / 2),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = indicatorColor,
                            start = Offset(size.width - strokeWidth / 2, 0f),
                            end = Offset(size.width - strokeWidth / 2, cornerLength),
                            strokeWidth = strokeWidth
                        )
                        
                        // Bottom-left corner
                        drawLine(
                            color = indicatorColor,
                            start = Offset(0f, size.height - strokeWidth / 2),
                            end = Offset(cornerLength, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = indicatorColor,
                            start = Offset(strokeWidth / 2, size.height - cornerLength),
                            end = Offset(strokeWidth / 2, size.height),
                            strokeWidth = strokeWidth
                        )
                        
                        // Bottom-right corner
                        drawLine(
                            color = indicatorColor,
                            start = Offset(size.width - cornerLength, size.height - strokeWidth / 2),
                            end = Offset(size.width, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = indicatorColor,
                            start = Offset(size.width - strokeWidth / 2, size.height - cornerLength),
                            end = Offset(size.width - strokeWidth / 2, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                    // Only handle multi-finger gestures (zoom) or pan when zoomed in
                    .pointerInput(shouldHandlePan) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // Always handle zoom (multi-finger gesture)
                            if (zoom != 1f) {
                                isZooming = true
                                val newScale = (viewScale * zoom).coerceIn(0.5f, 5f)
                                viewScale = newScale
                            }
                            
                            // Only handle pan when zoomed in, to allow HorizontalPager to work at 1x scale
                            if (shouldHandlePan) {
                                viewOffsetX += pan.x
                                viewOffsetY += pan.y
                                
                                val maxOffset = (viewScale - 1f) * 500f
                                viewOffsetX = viewOffsetX.coerceIn(-maxOffset, maxOffset)
                                viewOffsetY = viewOffsetY.coerceIn(-maxOffset, maxOffset)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (viewScale > 1.5f) {
                                    viewScale = 1f
                                    viewOffsetX = 0f
                                    viewOffsetY = 0f
                                } else {
                                    viewScale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(photo.systemUri))
                        .crossfade(300)
                        .build(),
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Apply crop state transformations first, then view transformations
                            val combinedScale = cropState.scale * animatedScale
                            scaleX = combinedScale
                            scaleY = combinedScale
                            translationX = cropState.offsetX + viewOffsetX
                            translationY = cropState.offsetY + viewOffsetY
                            rotationZ = cropState.rotation
                        }
                )
            }
        }
    }
}
