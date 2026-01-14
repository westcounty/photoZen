package com.example.photozen.ui.screens.workflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.TagEntity
import com.example.photozen.ui.theme.KeepGreen

/**
 * Tagging stage content for quickly assigning tags to keep photos.
 * 
 * Features:
 * - Shows current photo with tag chips below
 * - Tap chip to assign/unassign tag
 * - Create new tags inline
 * - Navigate between photos with buttons
 */
@Composable
fun TaggingStageContent(
    keepPhotos: List<PhotoEntity>,
    onPhotoTagged: () -> Unit,
    onComplete: () -> Unit,
    viewModel: TaggingViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsState()
    val currentPhotoTags by viewModel.currentPhotoTags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var currentIndex by remember { mutableIntStateOf(0) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    
    // Track which photos have been tagged
    val taggedPhotoIds = remember { mutableStateListOf<String>() }
    
    // Update current photo in ViewModel when index changes
    LaunchedEffect(currentIndex, keepPhotos) {
        if (keepPhotos.isNotEmpty() && currentIndex < keepPhotos.size) {
            viewModel.setCurrentPhoto(keepPhotos[currentIndex].id)
        }
    }
    
    if (keepPhotos.isEmpty()) {
        // No photos to tag
        EmptyTaggingContent(onComplete = onComplete)
    } else {
        val currentPhoto = keepPhotos.getOrNull(currentIndex)
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Photo counter
                Text(
                    text = "${currentIndex + 1} / ${keepPhotos.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Current photo display
                currentPhoto?.let { photo ->
                    PhotoPreviewCard(
                        photo = photo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tag selection area
                TagSelectionArea(
                    availableTags = tags,
                    selectedTagIds = currentPhotoTags.map { it.id }.toSet(),
                    isLoading = isLoading,
                    onTagClick = { tag ->
                        if (currentPhoto != null) {
                            viewModel.togglePhotoTag(currentPhoto.id, tag)
                            if (currentPhoto.id !in taggedPhotoIds) {
                                taggedPhotoIds.add(currentPhoto.id)
                                onPhotoTagged()
                            }
                        }
                    },
                    onAddTagClick = { showAddTagDialog = true }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Navigation buttons
                NavigationButtons(
                    currentIndex = currentIndex,
                    totalCount = keepPhotos.size,
                    onPrevious = { if (currentIndex > 0) currentIndex-- },
                    onNext = { 
                        if (currentIndex < keepPhotos.size - 1) {
                            currentIndex++
                        }
                    },
                    onSkip = {
                        if (currentIndex < keepPhotos.size - 1) {
                            currentIndex++
                        } else {
                            onComplete()
                        }
                    },
                    onComplete = onComplete
                )
            }
            
            // Progress indicator
            if (taggedPhotoIds.isNotEmpty()) {
                TaggingProgress(
                    tagged = taggedPhotoIds.size,
                    total = keepPhotos.size,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
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
    }
}

@Composable
private fun PhotoPreviewCard(
    photo: PhotoEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.systemUri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSelectionArea(
    availableTags: List<TagEntity>,
    selectedTagIds: Set<String>,
    isLoading: Boolean,
    onTagClick: (TagEntity) -> Unit,
    onAddTagClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择标签",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            TextButton(onClick = onAddTagClick) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("新建标签")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (availableTags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "暂无标签",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onAddTagClick) {
                        Text("创建第一个标签")
                    }
                }
            }
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTags.forEach { tag ->
                    val isSelected = tag.id in selectedTagIds
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        label = "chip_scale"
                    )
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTagClick(tag) },
                        label = { Text(tag.name) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(tag.color).copy(alpha = 0.3f),
                            selectedLabelColor = Color(tag.color)
                        ),
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationButtons(
    currentIndex: Int,
    totalCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        TextButton(
            onClick = onPrevious,
            enabled = currentIndex > 0
        ) {
            Text("上一张")
        }
        
        // Skip / Next button
        if (currentIndex < totalCount - 1) {
            Row {
                TextButton(onClick = onSkip) {
                    Text("跳过")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onNext) {
                    Text("下一张")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // Last photo - show complete button
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = KeepGreen)
            ) {
                Text("完成分类")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TaggingProgress(
    tagged: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = KeepGreen.copy(alpha = 0.9f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$tagged/$total 已标记",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyTaggingContent(onComplete: () -> Unit) {
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
                text = "没有需要分类的照片",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "所有保留的照片都已分类完成",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = KeepGreen)
            ) {
                Text("查看结果")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int) -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    
    val colorOptions = listOf(
        Color(0xFF5EEAD4), // Teal
        Color(0xFF7DD3FC), // Sky
        Color(0xFFA78BFA), // Violet
        Color(0xFFFBBF24), // Amber
        Color(0xFFF472B6), // Pink
        Color(0xFF34D399), // Green
        Color(0xFFFB7185), // Rose
        Color(0xFF60A5FA)  // Blue
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建标签") },
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
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colorOptions.size) { index ->
                        val color = colorOptions[index]
                        val isSelected = index == selectedColorIndex
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (tagName.isNotBlank()) {
                        onConfirm(tagName.trim(), colorOptions[selectedColorIndex].toArgb())
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
