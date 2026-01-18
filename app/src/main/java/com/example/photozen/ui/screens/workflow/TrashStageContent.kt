package com.example.photozen.ui.screens.workflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.components.DragSelectPhotoGrid
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * Content for the TRASH stage in the workflow.
 * Displays trash photos for cleanup (restore or permanent delete).
 */
@Composable
fun TrashStageContent(
    photos: List<PhotoEntity>,
    selectedIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRestoreToKeep: () -> Unit,
    onRestoreToMaybe: () -> Unit,
    onPermanentDelete: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    // Empty state - no trash photos
    if (photos.isEmpty()) {
        EmptyTrashState(
            onContinue = onComplete
        )
        return
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "回收站照片",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (selectedIds.isEmpty()) {
                            "${photos.size} 张照片待清理"
                        } else {
                            "已选择 ${selectedIds.size} 张"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Select all / Clear selection
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (selectedIds.size == photos.size) {
                            onClearSelection()
                        } else {
                            onSelectAll()
                        }
                    }
                ) {
                    Text(
                        text = if (selectedIds.size == photos.size) "取消全选" else "全选"
                    )
                }
            }
            
            // Photo grid with drag select
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                DragSelectPhotoGrid(
                    photos = photos,
                    selectedIds = selectedIds,
                    onSelectionChanged = onSelectionChanged,
                    onPhotoClick = { _, _ -> },
                    onPhotoLongPress = { _, _ -> },
                    columns = 3,
                    selectionColor = TrashRed
                )
            }
        }
        
        // Bottom action bar
        AnimatedVisibility(
            visible = selectedIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TrashActionBar(
                onRestoreToKeep = onRestoreToKeep,
                onRestoreToMaybe = onRestoreToMaybe,
                onPermanentDelete = onPermanentDelete,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

/**
 * Action bar for trash stage.
 */
@Composable
private fun TrashActionBar(
    onRestoreToKeep: () -> Unit,
    onRestoreToMaybe: () -> Unit,
    onPermanentDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Restore to Keep
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilledTonalButton(
                    onClick = onRestoreToKeep,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = KeepGreen.copy(alpha = 0.15f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = KeepGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("恢复为保留", color = KeepGreen)
                }
            }
            
            // Restore to Maybe
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilledTonalButton(
                    onClick = onRestoreToMaybe,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaybeAmber.copy(alpha = 0.15f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QuestionMark,
                        contentDescription = null,
                        tint = MaybeAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("恢复为待定", color = MaybeAmber)
                }
            }
            
            // Permanent Delete
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onPermanentDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrashRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("永久删除")
                }
            }
        }
    }
}

/**
 * Empty state for trash stage.
 */
@Composable
private fun EmptyTrashState(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = KeepGreen.copy(alpha = 0.1f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = KeepGreen,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "太棒了！",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "没有需要清理的照片",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            FilledTonalButton(onClick = onContinue) {
                Text("完成整理")
            }
        }
    }
}
