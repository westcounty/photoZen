package com.example.photozen.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.example.photozen.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
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
import com.example.photozen.data.repository.PhotoClassificationMode
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.DailyTaskMode
import com.example.photozen.domain.usecase.DailyTaskStatus
import com.example.photozen.ui.components.AchievementSummaryCard
import com.example.photozen.ui.components.ChangelogDialog
import com.example.photozen.ui.components.MiniStatsCard
import com.example.photozen.ui.components.DailyTaskDisplayStatus
import com.example.photozen.ui.components.HomeDesignTokens
import com.example.photozen.ui.components.HomeDailyTask
import com.example.photozen.ui.components.HomeMainAction
import com.example.photozen.ui.components.HomeQuickActions
import com.example.photozen.ui.components.QuickStartSheet
import com.example.photozen.ui.components.SortModeBottomSheet
import com.example.photozen.ui.components.generateAchievements
import com.example.photozen.ui.components.GuideTooltip
import com.example.photozen.ui.components.ArrowDirection
import com.example.photozen.ui.guide.rememberGuideState
import com.example.photozen.domain.model.GuideKey
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import com.example.photozen.ui.util.FeatureFlags

/**
 * Home Screen - Entry point for PicZen app.
 * Shows statistics and navigation to main features.
 * 
 * Phase 1-C: 部分导航回调由底部导航处理，标记为可选参数：
 * - onNavigateToSettings (由底部导航 Settings Tab 处理)
 * - onNavigateToTimeline (由底部导航 Timeline Tab 处理)
 * - onNavigateToAlbumBubble (由底部导航 Albums Tab 处理)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFlowSorter: (Boolean, Int) -> Unit,
    onNavigateToLightTable: () -> Unit,
    onNavigateToPhotoList: (PhotoStatus) -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToWorkflow: (Boolean, Int) -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToFilterSelection: (String, Int) -> Unit = { _, _ -> },
    onNavigateToSmartGallery: () -> Unit = { },
    // Phase 1-C: 以下参数标记为可选，由底部导航处理
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTimeline: () -> Unit = {},
    onNavigateToAlbumBubble: () -> Unit = {},
    // Phase 3: 统计页面入口
    onNavigateToStats: () -> Unit = {},
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
    
    // Request permission on launch (only media permissions, not notifications)
    // POST_NOTIFICATIONS is requested when user enables daily reminder
    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            // 移除：POST_NOTIFICATIONS 改为在用户开启每日提醒时请求
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
        // Phase 1-D: Feature Flag 控制新旧布局
        if (FeatureFlags.USE_NEW_HOME_LAYOUT) {
            NewHomeLayout(
                uiState = uiState,
                paddingValues = paddingValues,
                onStartSorting = { viewModel.showSortModeSheet() },
                onNavigateToLightTable = onNavigateToLightTable,
                onNavigateToTrash = onNavigateToTrash,
                onNavigateToAchievements = onNavigateToAchievements,
                onStartDailyTask = {
                    val status = uiState.dailyTaskStatus
                    if (status != null) {
                        val mode = status.mode
                        val target = status.target
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
                },
                onNavigateToSmartGallery = onNavigateToSmartGallery,
                onNavigateToStats = onNavigateToStats,  // Phase 3
                permissionLauncher = permissionLauncher,
                guideRepository = viewModel.guideRepository
            )
        } else {
            LegacyHomeLayout(
                uiState = uiState,
                paddingValues = paddingValues,
                onNavigateToFlowSorter = onNavigateToFlowSorter,
                onNavigateToLightTable = onNavigateToLightTable,
                onNavigateToPhotoList = onNavigateToPhotoList,
                onNavigateToTrash = onNavigateToTrash,
                onNavigateToWorkflow = onNavigateToWorkflow,
                onNavigateToAchievements = onNavigateToAchievements,
                onNavigateToFilterSelection = onNavigateToFilterSelection,
                onNavigateToSmartGallery = onNavigateToSmartGallery,
                onNavigateToTimeline = onNavigateToTimeline,
                onNavigateToAlbumBubble = onNavigateToAlbumBubble,
                onNavigateToStats = onNavigateToStats,  // Phase 3
                permissionLauncher = permissionLauncher
            )
        }
    }
    
    // Quick Start Sheet - Higher priority than Changelog
    if (uiState.shouldShowQuickStart) {
        QuickStartSheet(
            onComplete = { dailyTaskEnabled, dailyTaskTarget, swipeSensitivity, cardSortingAlbumEnabled ->
                viewModel.completeQuickStartWithSettings(
                    dailyTaskEnabled = dailyTaskEnabled,
                    dailyTaskTarget = dailyTaskTarget,
                    swipeSensitivity = swipeSensitivity,
                    cardSortingAlbumEnabled = cardSortingAlbumEnabled
                )
            },
            onDismiss = {
                viewModel.dismissQuickStart()
            }
        )
    }
    
    // Changelog Dialog - Only shown if quick start is completed
    if (uiState.shouldShowChangelog) {
        ChangelogDialog(
            onDismiss = {
                viewModel.markChangelogSeen()
            }
        )
    }
    
    // Phase 1-D: 整理模式选择弹窗
    if (uiState.showSortModeSheet) {
        SortModeBottomSheet(
            onDismiss = { viewModel.hideSortModeSheet() },
            onQuickSortSelected = { onNavigateToFlowSorter(false, -1) },
            onWorkflowSelected = { onNavigateToWorkflow(false, -1) },
            unsortedCount = uiState.unsortedCount,
            needsFilterSelection = uiState.needsFilterSelection,
            onFilterSelectionRequired = { mode ->
                onNavigateToFilterSelection(mode, -1)
            }
        )
    }
}

// ==================== Phase 1-D: 新首页布局 ====================

/**
 * 新首页布局 - Phase 1-D
 * 
 * 采用分层卡片设计：主操作区 + 快捷入口 + 每日任务 + 智能画廊 + 成就预览
 * 
 * ## 设计变化
 * 
 * - 合并"快速整理"和"一站式整理"为统一的"开始整理"按钮
 * - 移除时间线、相册入口（由底部导航处理）
 * - 每日任务改为可折叠卡片
 */
@Composable
private fun NewHomeLayout(
    uiState: HomeUiState,
    paddingValues: PaddingValues,
    onStartSorting: () -> Unit,
    onNavigateToLightTable: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onStartDailyTask: () -> Unit,
    onNavigateToSmartGallery: () -> Unit,
    onNavigateToStats: () -> Unit,  // Phase 3: 统计页面入口
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    guideRepository: com.example.photozen.data.repository.GuideRepository
) {
    // 开始按钮引导状态
    val startButtonGuide = rememberGuideState(
        guideKey = GuideKey.HOME_START_BUTTON,
        guideRepository = guideRepository
    )
    var mainActionBounds by remember { mutableStateOf<Rect?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(HomeDesignTokens.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(HomeDesignTokens.SectionSpacing)
    ) {
        // 加载状态
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LoadingCard()
        }
        
        if (!uiState.isLoading) {
            // 1. 主操作区（带引导）
            Box {
                HomeMainAction(
                    unsortedCount = uiState.unsortedCount,
                    onStartClick = onStartSorting,
                    enabled = uiState.unsortedCount > 0,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        mainActionBounds = coordinates.boundsInRoot()
                    }
                )
                
                // 开始按钮引导
                GuideTooltip(
                    visible = startButtonGuide.shouldShow && uiState.unsortedCount > 0,
                    message = "🚀 点击开始\n从这里开始整理你的照片",
                    targetBounds = mainActionBounds,
                    arrowDirection = ArrowDirection.UP,
                    onDismiss = startButtonGuide.dismiss
                )
            }
            
            // 2. 快捷入口（仅对比和回收站）
            HomeQuickActions(
                onCompareClick = onNavigateToLightTable,
                onTrashClick = onNavigateToTrash,
                maybeCount = uiState.maybeCount,
                trashCount = uiState.trashCount
            )
            
            // 3. 每日任务（如果启用）
            if (uiState.dailyTaskStatus?.isEnabled == true) {
                HomeDailyTask(
                    status = DailyTaskDisplayStatus(
                        current = uiState.dailyTaskStatus!!.current,
                        target = uiState.dailyTaskStatus!!.target,
                        isEnabled = true,
                        isCompleted = uiState.dailyTaskStatus!!.isCompleted
                    ),
                    onStartClick = onStartDailyTask
                )
            }
            
            // 4. 智能画廊（实验功能）
            if (BuildConfig.ENABLE_SMART_GALLERY) {
                AnimatedVisibility(
                    visible = uiState.experimentalEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SmartGalleryCard(
                        onClick = onNavigateToSmartGallery,
                        personCount = uiState.smartGalleryPersonCount,
                        labelCount = uiState.smartGalleryLabelCount,
                        gpsPhotoCount = uiState.smartGalleryGpsPhotoCount,
                        analysisProgress = uiState.smartGalleryAnalysisProgress,
                        isAnalyzing = uiState.smartGalleryIsAnalyzing
                    )
                }
            }
            
            // 5. 整理统计入口
            MiniStatsCard(
                totalSorted = uiState.statsSummary.totalSorted,
                weekSorted = uiState.statsSummary.weekSorted,
                consecutiveDays = uiState.statsSummary.consecutiveDays,
                onClick = onNavigateToStats
            )
            
            // 6. 成就预览
            val achievements = generateAchievements(uiState.achievementData)
            AchievementSummaryCard(
                achievements = achievements,
                onClick = onNavigateToAchievements
            )
        }
        
        // 空状态
        if (!uiState.isLoading && !uiState.hasPhotos && uiState.hasPermission) {
            EmptyStateCard()
        }
        
        // 权限拒绝状态
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

// ==================== 旧首页布局（保留向后兼容） ====================

/**
 * 旧首页布局 - 向后兼容
 * 
 * 保留原有布局，当 FeatureFlags.USE_NEW_HOME_LAYOUT = false 时使用
 */
@Composable
private fun LegacyHomeLayout(
    uiState: HomeUiState,
    paddingValues: PaddingValues,
    onNavigateToFlowSorter: (Boolean, Int) -> Unit,
    onNavigateToLightTable: () -> Unit,
    onNavigateToPhotoList: (PhotoStatus) -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToWorkflow: (Boolean, Int) -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToFilterSelection: (String, Int) -> Unit,
    onNavigateToSmartGallery: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToAlbumBubble: () -> Unit,
    onNavigateToStats: () -> Unit,  // Phase 3: 统计页面入口
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
) {
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
            // Compact stats header
            if (uiState.hasPhotos) {
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
            
            // Smart Gallery Card - Only shown when:
            // 1. BuildConfig.ENABLE_SMART_GALLERY is true (compile-time flag)
            // 2. Experimental features are enabled in settings (runtime flag)
            if (BuildConfig.ENABLE_SMART_GALLERY) {
                AnimatedVisibility(
                    visible = uiState.experimentalEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SmartGalleryCard(
                        onClick = onNavigateToSmartGallery,
                        personCount = uiState.smartGalleryPersonCount,
                        labelCount = uiState.smartGalleryLabelCount,
                        gpsPhotoCount = uiState.smartGalleryGpsPhotoCount,
                        analysisProgress = uiState.smartGalleryAnalysisProgress,
                        isAnalyzing = uiState.smartGalleryIsAnalyzing
                    )
                }
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
            
            // Timeline Card
            ActionCard(
                title = "时间线",
                subtitle = "按时间分组浏览和整理照片",
                icon = Icons.Default.Timeline,
                iconTint = Color(0xFFEC4899), // Pink
                enabled = uiState.hasPhotos,
                onClick = onNavigateToTimeline
            )
            
            // Album Bubble Card
            ActionCard(
                title = "我的相册",
                subtitle = "可视化管理我的相册",
                icon = Icons.Default.Collections,
                iconTint = Color(0xFF4FC3F7), // Light Blue
                enabled = true,
                onClick = onNavigateToAlbumBubble
            )
            
            // Stats Card
            MiniStatsCard(
                totalSorted = uiState.statsSummary.totalSorted,
                weekSorted = uiState.statsSummary.weekSorted,
                consecutiveDays = uiState.statsSummary.consecutiveDays,
                onClick = onNavigateToStats
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

// ==================== Helper Composables ====================

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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Unsorted count - left aligned
        Row(
            verticalAlignment = Alignment.CenterVertically
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
        
        // Sorted count - right aligned
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sortedCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KeepGreen
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "已整理",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = KeepGreen,
                modifier = Modifier.size(18.dp)
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

/**
 * Smart Gallery Card - Entry point to AI-powered features.
 * Enhanced with quick stats preview and analysis progress.
 */
@Composable
private fun SmartGalleryCard(
    onClick: () -> Unit,
    personCount: Int = 0,
    labelCount: Int = 0,
    gpsPhotoCount: Int = 0,
    analysisProgress: Float = 0f,
    isAnalyzing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "智能画廊",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick stats row - 4 preview icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SmartGalleryStatItem(
                    icon = Icons.Default.Person,
                    count = personCount,
                    label = "人物",
                    tint = Color(0xFF8B5CF6) // Purple
                )
                SmartGalleryStatItem(
                    icon = Icons.Default.Sell,
                    count = labelCount,
                    label = "标签",
                    tint = Color(0xFF10B981) // Green
                )
                SmartGalleryStatItem(
                    icon = Icons.Default.Search,
                    count = null,
                    label = "搜索",
                    tint = Color(0xFF3B82F6) // Blue
                )
                SmartGalleryStatItem(
                    icon = Icons.Filled.Place,
                    count = gpsPhotoCount,
                    label = "位置",
                    tint = Color(0xFFF59E0B) // Amber
                )
            }
            
            // Analysis progress (only show if there's progress or analyzing)
            if (isAnalyzing || analysisProgress > 0f) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { analysisProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAnalyzing) "分析中 ${(analysisProgress * 100).toInt()}%" else "已分析 ${(analysisProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Single stat item for Smart Gallery preview.
 */
@Composable
private fun SmartGalleryStatItem(
    icon: ImageVector,
    count: Int?,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (count != null && count > 0) {
                Text(
                    text = if (count > 999) "999+" else count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tint
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
