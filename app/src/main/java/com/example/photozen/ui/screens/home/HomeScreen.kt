package com.example.photozen.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.DailyTaskMode
import com.example.photozen.domain.usecase.DailyTaskStatus
import com.example.photozen.ui.components.AchievementSummaryCard
import com.example.photozen.ui.components.generateAchievements
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * Home Screen - Entry point for PicZen app.
 * Shows statistics and navigation to main features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFlowSorter: (Boolean, Int) -> Unit,
    onNavigateToLightTable: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPhotoList: (PhotoStatus) -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToWorkflow: (Boolean, Int) -> Unit,
    onNavigateToTagBubble: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToFilterSelection: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readImagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] == true || 
                                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        
        if (readImagesGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }
    
    // Request permission on launch
    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        permissionLauncher.launch(permissions.toTypedArray())
    }
    
    // Show messages
    LaunchedEffect(uiState.syncResult) {
        uiState.syncResult?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSyncResult()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PhotoZen 图禅",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.syncPhotos() },
                        enabled = !uiState.isSyncing && uiState.hasPermission
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新"
                            )
                        }
                    }
                    
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Loading state
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LoadingCard()
            }
            
            // Action Cards
            if (!uiState.isLoading) {
                // Compact stats header - shown when one-stop is disabled
                if (!uiState.onestopEnabled && uiState.hasPhotos) {
                    CompactStatsHeader(
                        unsortedCount = uiState.unsortedCount,
                        sortedCount = uiState.sortedCount
                    )
                }
                
                // Daily Task Card - Now the PRIMARY prominent card at top
                if (uiState.dailyTaskStatus?.isEnabled == true) {
                    PrimaryDailyTaskCard(
                        status = uiState.dailyTaskStatus!!,
                        onStartClick = {
                            val mode = uiState.dailyTaskStatus!!.mode
                            val target = uiState.dailyTaskStatus!!.target
                            if (mode == DailyTaskMode.FLOW) {
                                if (uiState.needsFilterSelection) {
                                    onNavigateToFilterSelection("workflow_daily", target)
                                } else {
                                    onNavigateToWorkflow(true, target)
                                }
                            } else {
                                if (uiState.needsFilterSelection) {
                                    onNavigateToFilterSelection("flow_daily", target)
                                } else {
                                    onNavigateToFlowSorter(true, target)
                                }
                            }
                        }
                    )
                }
                
                // Mission Card - Only shown when onestopEnabled is true
                if (uiState.onestopEnabled && uiState.filteredUnsorted > 0) {
                    SecondaryMissionCard(
                        unsortedCount = uiState.filteredUnsorted,
                        sortedCount = uiState.filteredSorted,
                        totalCount = uiState.filteredTotal,
                        progress = uiState.filteredProgress,
                        filterMode = uiState.photoFilterMode,
                        onStartFlow = {
                            // Check if custom filter selection is needed before starting Flow
                            if (uiState.needsFilterSelection) {
                                onNavigateToFilterSelection("workflow", -1)
                            } else {
                                onNavigateToWorkflow(false, -1)
                            }
                        }
                    )
                }
            }
            
            // Quick Stats Row - clickable
            if (!uiState.isLoading && uiState.hasPhotos) {
                QuickStatsRow(
                    uiState = uiState,
                    onKeepClick = { onNavigateToPhotoList(PhotoStatus.KEEP) },
                    onTrashClick = { onNavigateToTrash() },
                    onMaybeClick = { onNavigateToPhotoList(PhotoStatus.MAYBE) }
                )
            }
            
            // Action Cards
            if (!uiState.isLoading) {
                // Quick Action: Flow Sorter (standalone)
                ActionCard(
                    title = "快速整理",
                    subtitle = if (uiState.unsortedCount > 0) {
                        "${uiState.unsortedCount} 张照片待整理"
                    } else {
                        "所有照片已整理完成"
                    },
                    icon = Icons.Default.SwipeRight,
                    iconTint = MaterialTheme.colorScheme.primary,
                    enabled = uiState.unsortedCount > 0,
                    onClick = {
                        if (uiState.needsFilterSelection) {
                            onNavigateToFilterSelection("flow", -1)
                        } else {
                            onNavigateToFlowSorter(false, -1)
                        }
                    }
                )
                
                // Light Table Card
                ActionCard(
                    title = "照片对比",
                    subtitle = if (uiState.maybeCount > 0) {
                        "${uiState.maybeCount} 张待定照片可对比"
                    } else {
                        "没有待定照片"
                    },
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    iconTint = MaybeAmber,
                    enabled = uiState.maybeCount > 0,
                    onClick = onNavigateToLightTable
                )
                
                // Tag Bubble Card
                ActionCard(
                    title = "标签气泡",
                    subtitle = "可视化浏览和管理标签",
                    icon = Icons.Default.Sell,
                    iconTint = Color(0xFFA78BFA), // Purple
                    enabled = true,
                    onClick = onNavigateToTagBubble
                )
                
                // Achievement Card
                val achievements = generateAchievements(uiState.achievementData)
                AchievementSummaryCard(
                    achievements = achievements,
                    onClick = onNavigateToAchievements
                )
            }
            
            // Empty state
            if (!uiState.isLoading && !uiState.hasPhotos && uiState.hasPermission) {
                EmptyStateCard()
            }
            
            // Permission denied state
            if (!uiState.hasPermission && !uiState.isLoading) {
                PermissionDeniedCard(
                    onRequestPermission = {
                        val permissions = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                )
            }
        }
    }
}

/**
 * Compact stats header - Small display of unsorted/sorted counts.
 * Shown when one-stop sorting is disabled.
 */
@Composable
private fun CompactStatsHeader(
    unsortedCount: Int,
    sortedCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Unsorted count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "待整理",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unsortedCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Sorted count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = KeepGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "已整理",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = sortedCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KeepGreen
            )
        }
    }
}

/**
 * Primary Daily Task Card - The main prominent card (big style).
 * Now styled like the old MissionCard.
 */
@Composable
private fun PrimaryDailyTaskCard(
    status: DailyTaskStatus,
    onStartClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "每日任务",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (status.isCompleted) "🎉 今日任务已完成！" else "保持整理习惯，每天进步一点",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Progress percentage or completed icon
                if (status.isCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(KeepGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = KeepGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${(status.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (status.isCompleted) KeepGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Current - Highlighted
                Column {
                    Text(
                        text = status.current.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "已完成",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Target
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = status.target.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "今日目标",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (!status.isCompleted) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Start button
                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "开始今日任务",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Loading state card.
 */
@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在加载照片...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Quick statistics row - clickable to navigate to photo lists.
 */
@Composable
private fun QuickStatsRow(
    uiState: HomeUiState,
    onKeepClick: () -> Unit,
    onTrashClick: () -> Unit,
    onMaybeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(
            count = uiState.keepCount,
            label = "保留",
            icon = Icons.Default.Check,
            color = KeepGreen,
            onClick = onKeepClick,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            count = uiState.maybeCount,
            label = "待定",
            icon = Icons.Default.QuestionMark,
            color = MaybeAmber,
            onClick = onMaybeClick,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            count = uiState.trashCount,
            label = "回收站",
            icon = Icons.Default.Delete,
            color = TrashRed,
            onClick = onTrashClick,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Single stat chip - clickable.
 */
@Composable
private fun StatChip(
    count: Int,
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Main content centered
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Arrow positioned at right center
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = color.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * Secondary Mission Card - Now styled as the secondary card (smaller style).
 * For "一站式整理" workflow.
 */
@Composable
private fun SecondaryMissionCard(
    unsortedCount: Int,
    sortedCount: Int,
    totalCount: Int,
    progress: Float,
    filterMode: PhotoFilterMode,
    onStartFlow: () -> Unit
) {
    val filterModeText = when (filterMode) {
        PhotoFilterMode.ALL -> "全部照片"
        PhotoFilterMode.CAMERA_ONLY -> "相机照片"
        PhotoFilterMode.EXCLUDE_CAMERA -> "非相机照片"
        PhotoFilterMode.CUSTOM -> "自定义范围"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Rocket,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "一站式整理",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$filterModeText · $unsortedCount 张待整理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$sortedCount / $totalCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = KeepGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onStartFlow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("进入心流")
            }
        }
    }
}

/**
 * Action card for navigation.
 */
@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Button(
                onClick = onClick,
                enabled = enabled
            ) {
                Text("开始")
            }
        }
    }
}

/**
 * Empty state card.
 */
@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "没有找到照片",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "设备上没有可整理的照片",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Permission denied card.
 */
@Composable
private fun PermissionDeniedCard(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TrashRed.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Collections,
                contentDescription = null,
                tint = TrashRed,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "需要存储权限",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "PhotoZen 需要访问您的照片才能进行整理",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onRequestPermission) {
                Text("授予权限")
            }
        }
    }
}
