package com.example.photozen.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.theme.PicZenTokens
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
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.example.photozen.ui.components.ShareFeatureTipCard
import com.example.photozen.ui.components.HomeTipStrip
import com.example.photozen.ui.components.HomeTipData
import com.example.photozen.ui.components.MiniStatsCard
import com.example.photozen.ui.components.DailyTaskDisplayStatus
import com.example.photozen.ui.components.HomeDesignTokens
import com.example.photozen.ui.components.HomeDailyTask
import com.example.photozen.ui.components.HomeDailyTaskWithSortAll
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
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
        // 禁用底部 insets 处理，因为外层 MainScaffold 的 NavigationBar 已经处理了
        // 但保留顶部状态栏的处理（TopAppBar 需要）
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    // 设置入口已移至底部导航
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        NewHomeLayout(
            uiState = uiState,
            paddingValues = paddingValues,
            onStartSorting = { viewModel.showSortModeSheet() },
            onNavigateToLightTable = onNavigateToLightTable,
            onNavigateToTrash = onNavigateToTrash,
            onNavigateToPhotoList = onNavigateToPhotoList,
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
            onNavigateToStats = onNavigateToStats,
            permissionLauncher = permissionLauncher,
            guideRepository = viewModel.guideRepository
        )
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
    onNavigateToPhotoList: (PhotoStatus) -> Unit,
    onNavigateToAchievements: () -> Unit,
    onStartDailyTask: () -> Unit,
    onNavigateToStats: () -> Unit,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    guideRepository: com.example.photozen.data.repository.GuideRepository
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)  // 系统导航栏 padding
            .padding(
                start = HomeDesignTokens.SectionSpacing,
                end = HomeDesignTokens.SectionSpacing,
                top = HomeDesignTokens.SectionSpacing,
                bottom = HomeDesignTokens.SectionSpacing + 80.dp  // NavigationBar 高度
            ),
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
            // 1. 整理统计（移到最顶部）
            MiniStatsCard(
                totalSorted = uiState.statsSummary.totalSorted,
                weekSorted = uiState.statsSummary.weekSorted,
                consecutiveDays = uiState.statsSummary.consecutiveDays,
                onClick = onNavigateToStats
            )

            // 2. 每日任务/整理入口（核心功能，突出显示）
            // 包含"继续任务"和"整理全部"两个操作
            HomeDailyTaskWithSortAll(
                dailyTaskStatus = uiState.dailyTaskStatus,
                unsortedCount = uiState.unsortedCount,
                onStartDailyTask = onStartDailyTask,
                onStartSortAll = onStartSorting
            )

            // 3. 快捷入口（已保留、对比、回收站）
            HomeQuickActions(
                onKeepClick = { onNavigateToPhotoList(PhotoStatus.KEEP) },
                onCompareClick = onNavigateToLightTable,
                onTrashClick = onNavigateToTrash,
                keepCount = uiState.keepCount,
                maybeCount = uiState.maybeCount,
                trashCount = uiState.trashCount
            )

            // 4. 渐进式引导提示（成就区域上方）
            // 提示1：对比功能
            val compareTip = rememberGuideState(
                guideKey = GuideKey.HOME_TIP_COMPARE,
                guideRepository = guideRepository
            )
            AnimatedVisibility(
                visible = compareTip.shouldShow,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
            ) {
                HomeTipStrip(
                    icon = HomeTipData.compareTip.icon,
                    title = HomeTipData.compareTip.title,
                    description = HomeTipData.compareTip.description,
                    accentColor = HomeTipData.compareTip.accentColor(),
                    onDismiss = { compareTip.dismiss() }
                )
            }

            // 提示2：复制功能
            val copyTip = rememberGuideState(
                guideKey = GuideKey.HOME_TIP_COPY,
                guideRepository = guideRepository
            )
            AnimatedVisibility(
                visible = copyTip.shouldShow,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
            ) {
                HomeTipStrip(
                    icon = HomeTipData.copyTip.icon,
                    title = HomeTipData.copyTip.title,
                    description = HomeTipData.copyTip.description,
                    accentColor = HomeTipData.copyTip.accentColor(),
                    onDismiss = { copyTip.dismiss() }
                )
            }

            // 提示3：桌面小部件
            val widgetTip = rememberGuideState(
                guideKey = GuideKey.HOME_TIP_WIDGET,
                guideRepository = guideRepository
            )
            AnimatedVisibility(
                visible = widgetTip.shouldShow,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
            ) {
                HomeTipStrip(
                    icon = HomeTipData.widgetTip.icon,
                    title = HomeTipData.widgetTip.title,
                    description = HomeTipData.widgetTip.description,
                    accentColor = HomeTipData.widgetTip.accentColor(),
                    onDismiss = { widgetTip.dismiss() }
                )
            }

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

// ==================== Helper Composables ====================

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
