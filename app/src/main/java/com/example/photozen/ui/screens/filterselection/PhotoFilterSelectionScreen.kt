package com.example.photozen.ui.screens.filterselection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.photozen.data.source.Album
import com.example.photozen.domain.model.GuideKey
import com.example.photozen.ui.components.ArrowDirection
import com.example.photozen.ui.components.GuideTooltip
import com.example.photozen.ui.guide.rememberGuideState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Photo Filter Selection Screen.
 * Allows users to select albums and date range for filtering photos to sort.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoFilterSelectionScreen(
    onNavigateBack: () -> Unit,
    onConfirm: (selectedAlbumIds: List<String>?, startDate: Long?, endDate: Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoFilterSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("选择整理范围") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomActionBar(
                selectedCount = uiState.selectedAlbumIds.size,
                totalPhotos = uiState.filteredPhotoCount,
                hasDateFilter = uiState.startDate != null || uiState.endDate != null,
                onConfirm = {
                    // Save the session filter before navigating
                    viewModel.saveSessionFilter()
                    onConfirm(
                        if (uiState.selectedAlbumIds.isEmpty()) null else uiState.selectedAlbumIds.toList(),
                        uiState.startDate,
                        uiState.endDate
                    )
                },
                onSelectAll = { viewModel.selectAllAlbums() },
                onClearSelection = { viewModel.clearSelection() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)  // 系统导航栏 padding
        ) {
            // Date Range Section
            DateRangeSection(
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                onOpenDatePicker = { showDatePicker = true },
                onClearDate = { viewModel.clearDateRange() }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Albums Section
            AlbumsSection(
                albums = uiState.albums,
                selectedIds = uiState.selectedAlbumIds,
                isLoading = uiState.isLoading,
                onAlbumToggle = { viewModel.toggleAlbum(it) }
            )
        }
    }
    
    // Date Range Picker Dialog
    if (showDatePicker) {
        DateRangePickerDialog(
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                viewModel.setDateRange(start, end)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun DateRangeSection(
    startDate: Long?,
    endDate: Long?,
    onOpenDatePicker: () -> Unit,
    onClearDate: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "日期范围",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (startDate != null || endDate != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "已选择日期范围",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = buildString {
                                append(startDate?.let { dateFormat.format(Date(it)) } ?: "不限")
                                append(" ~ ")
                                append(endDate?.let { dateFormat.format(Date(it)) } ?: "不限")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row {
                        TextButton(onClick = onOpenDatePicker) {
                            Text("修改")
                        }
                        TextButton(onClick = onClearDate) {
                            Text("清除")
                        }
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = onOpenDatePicker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("选择日期范围（可选）")
            }
        }
    }
}

@Composable
private fun AlbumsSection(
    albums: List<Album>,
    selectedIds: Set<String>,
    isLoading: Boolean,
    onAlbumToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "相册（${albums.size}个）",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            if (selectedIds.isNotEmpty()) {
                Text(
                    text = "已选${selectedIds.size}个",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "不选择任何相册则整理全部照片",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "正在加载相册...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Album Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumItem(
                        album = album,
                        isSelected = album.id in selectedIds,
                        onClick = { onAlbumToggle(album.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumItem(
    album: Album,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (album.coverUri != null) {
                    AsyncImage(
                        model = album.coverUri,
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Selection indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                    )
                }
                
                // Camera badge
                if (album.isCamera) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "相机相册",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .size(16.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                            .padding(2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "${album.photoCount}张",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BottomActionBar(
    selectedCount: Int,
    totalPhotos: Int,
    hasDateFilter: Boolean,
    onConfirm: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()  // 处理系统导航栏
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildString {
                            if (selectedCount > 0) {
                                append("已选择 $selectedCount 个相册")
                            } else {
                                append("全部相册")
                            }
                            if (hasDateFilter) {
                                append(" · 日期已筛选")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "共 $totalPhotos 张照片待整理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (selectedCount > 0) {
                    TextButton(onClick = onClearSelection) {
                        Text("清除选择")
                    }
                } else {
                    TextButton(onClick = onSelectAll) {
                        Text("全选")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = totalPhotos > 0
            ) {
                Text("开始整理")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    startDate: Long?,
    endDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDate,
        initialSelectedEndDateMillis = endDate
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "选择日期范围",
                    modifier = Modifier.padding(16.dp)
                )
            },
            headline = {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }
                    Text(
                        text = dateRangePickerState.selectedStartDateMillis?.let { 
                            dateFormat.format(Date(it)) 
                        } ?: "开始日期"
                    )
                    Text(" - ")
                    Text(
                        text = dateRangePickerState.selectedEndDateMillis?.let { 
                            dateFormat.format(Date(it)) 
                        } ?: "结束日期"
                    )
                }
            },
            showModeToggle = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        )
    }
}
