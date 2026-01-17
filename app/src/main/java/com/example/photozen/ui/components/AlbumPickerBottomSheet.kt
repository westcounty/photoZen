package com.example.photozen.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.AlbumBubbleEntity

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
 * Dialog for adding albums to the bubble list.
 * Shows all system albums and allows multi-select.
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
