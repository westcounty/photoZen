package com.example.photozen.ui.screens.photolist

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.photozen.ui.components.PhotoListActionSheet
import com.example.photozen.ui.components.openImageWithApp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Action sheet state
    var showActionSheet by remember { mutableStateOf(false) }
    var selectedPhotoId by remember { mutableStateOf<String?>(null) }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    
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
                actions = {
                    // Sort button - only show when there are photos
                    if (uiState.photos.isNotEmpty()) {
                        IconButton(onClick = { viewModel.cycleSortOrder() }) {
                            Icon(
                                imageVector = when (uiState.sortOrder) {
                                    PhotoListSortOrder.DATE_DESC -> Icons.Default.ArrowDownward
                                    PhotoListSortOrder.DATE_ASC -> Icons.Default.ArrowUpward
                                    PhotoListSortOrder.RANDOM -> Icons.Default.Shuffle
                                },
                                contentDescription = "排序: ${uiState.sortOrder.displayName}"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Prominent Quick Tag Banner for KEEP status - show untagged count
            if (uiState.status == PhotoStatus.KEEP && uiState.untaggedCount > 0) {
                QuickTagBanner(
                    photoCount = uiState.untaggedCount,
                    onClick = onNavigateToQuickTag
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
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
                            sortOrder = uiState.sortOrder,
                            onPhotoLongPress = { photoId, photoUri ->
                                selectedPhotoId = photoId
                                selectedPhotoUri = photoUri
                                showActionSheet = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Action Sheet
    if (showActionSheet && selectedPhotoId != null && selectedPhotoUri != null) {
        val photoId = selectedPhotoId!!
        val photoUri = selectedPhotoUri!!
        
        PhotoListActionSheet(
            imageUri = photoUri,
            onDismiss = {
                showActionSheet = false
                selectedPhotoId = null
                selectedPhotoUri = null
            },
            onEdit = { onNavigateToEditor(photoId) },
            onMoveToKeep = if (uiState.status != PhotoStatus.KEEP) {
                { viewModel.moveToKeep(photoId) }
            } else null,
            onMoveToMaybe = if (uiState.status != PhotoStatus.MAYBE) {
                { viewModel.moveToMaybe(photoId) }
            } else null,
            onMoveToTrash = if (uiState.status != PhotoStatus.TRASH) {
                { viewModel.moveToTrash(photoId) }
            } else null,
            onResetToUnsorted = { viewModel.resetToUnsorted(photoId) },
            onOpenWithApp = { packageName ->
                openImageWithApp(context, Uri.parse(photoUri), packageName)
            },
            defaultAppPackage = uiState.defaultExternalApp,
            onSetDefaultApp = { packageName ->
                scope.launch {
                    viewModel.setDefaultExternalApp(packageName)
                }
            },
            // Only show duplicate option for KEEP status photos
            onDuplicatePhoto = if (uiState.status == PhotoStatus.KEEP) {
                { viewModel.duplicatePhoto(photoId) }
            } else null
        )
    }
}

/**
 * Prominent Quick Tag Banner - highly visible call-to-action.
 */
@Composable
private fun QuickTagBanner(
    photoCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = "快速分类",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "为 $photoCount 张照片添加标签",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("开始")
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    photos: List<PhotoEntity>,
    sortOrder: PhotoListSortOrder,
    onPhotoLongPress: (String, String) -> Unit // photoId, photoUri
) {
    // Use sortOrder in key to force recomposition when sort changes
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        // Use index-based keys to ensure proper reordering when sort changes
        itemsIndexed(
            items = photos,
            key = { index, photo -> "${sortOrder.name}_${index}_${photo.id}" }
        ) { _, photo ->
            PhotoGridItem(
                photo = photo,
                onLongPress = { onPhotoLongPress(photo.id, photo.systemUri) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: PhotoEntity,
    onLongPress: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    // Calculate aspect ratio from photo dimensions
    val aspectRatio = if (photo.width > 0 && photo.height > 0) {
        photo.width.toFloat() / photo.height.toFloat()
    } else {
        1f // Default to square if dimensions unknown
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(photo.systemUri))
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        )
    }
}

@Composable
private fun EmptyState(status: PhotoStatus, color: Color) {
    val iconAndText: Pair<androidx.compose.ui.graphics.vector.ImageVector, String> = when (status) {
        PhotoStatus.KEEP -> Icons.Default.Favorite to "没有保留的照片"
        PhotoStatus.TRASH -> Icons.Default.Delete to "回收站为空"
        PhotoStatus.MAYBE -> Icons.Default.QuestionMark to "没有待定的照片"
        PhotoStatus.UNSORTED -> Icons.Default.Check to "所有照片已整理完成"
    }
    val icon = iconAndText.first
    val text = iconAndText.second
    
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
