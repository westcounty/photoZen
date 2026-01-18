package com.example.photozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.source.Album

/**
 * A reusable bottom sheet for selecting an album.
 * Supports single-select mode with optional "Add Album" button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPickerBottomSheet(
    albums: List<AlbumBubbleEntity>,
    title: String = "选择相册",
    showAddAlbum: Boolean = true,
    excludeBucketId: String? = null,
    onAlbumSelected: (AlbumBubbleEntity) -> Unit,
    onAddAlbumClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val filteredAlbums = remember(albums, excludeBucketId) {
        if (excludeBucketId != null) {
            albums.filter { it.bucketId != excludeBucketId }
        } else {
            albums
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (showAddAlbum && onAddAlbumClick != null) {
                    TextButton(onClick = onAddAlbumClick) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加相册")
                    }
                }
            }
            
            HorizontalDivider()
            
            // Album list
            if (filteredAlbums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "没有可选相册",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (showAddAlbum && onAddAlbumClick != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FilledTonalButton(onClick = onAddAlbumClick) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("添加相册")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(filteredAlbums, key = { it.bucketId }) { album ->
                        AlbumPickerItem(
                            album = album,
                            onClick = { onAlbumSelected(album) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AlbumPickerItem(
    album: AlbumBubbleEntity,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = album.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PhotoAlbum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * System album picker dialog with grid layout.
 * Shows all system albums with cover images, supports multi-select and creating new albums.
 * 
 * This is the unified album picker component used across the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemAlbumPickerDialog(
    title: String = "编辑我的相册",
    albums: List<Album>,
    selectedIds: Set<String>,
    isLoading: Boolean = false,
    onToggleSelection: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onCreateAlbum: ((String) -> Unit)? = null
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title)
                // Create album button (only show if callback provided)
                if (onCreateAlbum != null) {
                    TextButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建")
                    }
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
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
                    Text("没有找到相册")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(albums.sortedByDescending { it.photoCount }, key = { it.id }) { album ->
                        val isSelected = album.id in selectedIds
                        
                        AlbumGridItem(
                            album = album,
                            isSelected = isSelected,
                            onClick = { onToggleSelection(album.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    // Create album dialog
    if (showCreateDialog && onCreateAlbum != null) {
        AlertDialog(
            onDismissRequest = { 
                showCreateDialog = false 
                newAlbumName = ""
            },
            title = { Text("新建相册") },
            text = {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    label = { Text("相册名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newAlbumName.isNotBlank()) {
                            onCreateAlbum(newAlbumName.trim())
                            showCreateDialog = false
                            newAlbumName = ""
                        }
                    },
                    enabled = newAlbumName.isNotBlank()
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showCreateDialog = false 
                        newAlbumName = ""
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * Album grid item with cover image and selection indicator.
 */
@Composable
private fun AlbumGridItem(
    album: Album,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                else Modifier
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cover image
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(android.net.Uri.parse(album.coverUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhotoAlbum,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Album name
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        
        // Photo count
        Text(
            text = "${album.photoCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Legacy dialog for simple multi-select (kept for compatibility).
 * @deprecated Use SystemAlbumPickerDialog instead for better UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumMultiSelectDialog(
    title: String = "选择相册",
    availableAlbums: List<AlbumInfo>,
    selectedBucketIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (availableAlbums.isEmpty()) {
                Text("没有找到相册")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(availableAlbums, key = { it.bucketId }) { album ->
                        val isSelected = album.bucketId in selectedBucketIds
                        ListItem(
                            headlineContent = { Text(album.displayName) },
                            supportingContent = { Text("${album.photoCount} 张照片") },
                            leadingContent = {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                            },
                            modifier = Modifier.clickable {
                                val newSelection = if (isSelected) {
                                    selectedBucketIds - album.bucketId
                                } else {
                                    selectedBucketIds + album.bucketId
                                }
                                onSelectionChanged(newSelection)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedBucketIds.isNotEmpty()
            ) {
                Text("确定 (${selectedBucketIds.size})")
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
 * Simple album info for picker display.
 */
data class AlbumInfo(
    val bucketId: String,
    val displayName: String,
    val photoCount: Int,
    val coverUri: String? = null
)
