package com.example.photozen.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.example.photozen.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.repository.DailyTaskMode
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.ui.components.ChangelogDialog
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material.icons.filled.CalendarMonth

/**
 * Settings Screen - App preferences and achievements.
 * Refactored to use a menu-based structure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // 通知权限请求 launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // 权限结果已处理，无需额外操作
        // 通知权限用于状态栏进度通知
    }
    
    // 检查并请求通知权限的辅助函数
    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    // Dialog states
    var showDailyTaskDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var showAcknowledgementDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClassificationModeDialog by remember { mutableStateOf(false) }
    
    // Show error messages
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
                title = { Text("设置") },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Function Settings
            SettingsSectionHeader("功能设置")
            
            SettingsMenuItem(
                icon = Icons.Default.Assignment,
                title = "每日整理任务",
                subtitle = if (uiState.dailyTaskEnabled) "已开启 · 目标 ${uiState.dailyTaskTarget}" else "已关闭",
                onClick = { showDailyTaskDialog = true }
            )
            
            SettingsMenuItem(
                icon = Icons.Default.PhotoLibrary,
                title = "待整理照片范围",
                subtitle = when (uiState.photoFilterMode) {
                    PhotoFilterMode.ALL -> "整理全部照片"
                    PhotoFilterMode.CAMERA_ONLY -> "仅整理相机照片"
                    PhotoFilterMode.EXCLUDE_CAMERA -> "排除相机照片"
                    PhotoFilterMode.CUSTOM -> "每次整理前选择"
                },
                onClick = { showFilterDialog = true }
            )
            
            // Theme Settings
            SettingsMenuItem(
                icon = Icons.Default.Palette,
                title = "外观样式",
                subtitle = when (uiState.themeMode) {
                    com.example.photozen.data.repository.ThemeMode.DARK -> "深色模式"
                    com.example.photozen.data.repository.ThemeMode.LIGHT -> "浅色模式"
                    com.example.photozen.data.repository.ThemeMode.SYSTEM -> "跟随系统"
                },
                onClick = { showThemeDialog = true }
            )
            
            // Quick Album Classification Settings
            SettingsMenuItem(
                icon = Icons.Default.PhotoAlbum,
                title = "快速相册分类设置",
                subtitle = "配置相册分类的相关选项",
                onClick = { showClassificationModeDialog = true }
            )
            
            // Swipe Sensitivity Settings
            SwipeSensitivitySetting(
                sensitivity = uiState.swipeSensitivity,
                onSensitivityChange = { viewModel.setSwipeSensitivity(it) }
            )
            
            // 实验性功能开关仅在 explore 分支显示
            // main 分支隐藏此开关，专注于照片整理功能
            if (BuildConfig.SHOW_EXPERIMENTAL_SETTINGS) {
                SettingsSwitchItem(
                    icon = Icons.Default.Science,
                    title = "实验性功能",
                    subtitle = "AI相关功能，施工中，不确定在您的手机上是什么样",
                    checked = uiState.experimentalEnabled,
                    onCheckedChange = { viewModel.setExperimentalEnabled(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Acknowledgement Card - Flat display
            AcknowledgementCard(
                onHeartClick = { showAcknowledgementDialog = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // About Card - Flat display
            AboutCard(
                onInfoClick = { showAboutDialog = true },
                onVersionClick = { showChangelogDialog = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Feedback Link
            FeedbackLink()
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Dialogs
    if (showDailyTaskDialog) {
        DailyTaskSettingsDialog(
            uiState = uiState,
            onDismiss = { 
                showDailyTaskDialog = false
                // 关闭对话框时，如果进度通知已开启，检查并请求通知权限
                if (uiState.progressNotificationEnabled) {
                    checkAndRequestNotificationPermission()
                }
            },
            onEnabledChange = { viewModel.setDailyTaskEnabled(it) },
            onTargetChange = { viewModel.setDailyTaskTarget(it) },
            onModeChange = { viewModel.setDailyTaskMode(it) },
            onProgressNotificationChange = { enabled ->
                viewModel.setProgressNotificationEnabled(enabled)
                // 开启进度通知时，检查并请求通知权限
                if (enabled) {
                    checkAndRequestNotificationPermission()
                }
            }
        )
    }
    
    if (showFilterDialog) {
        PhotoFilterSettingsDialog(
            currentMode = uiState.photoFilterMode,
            onDismiss = { showFilterDialog = false },
            onModeSelected = { 
                viewModel.setPhotoFilterMode(it)
                showFilterDialog = false
            }
        )
    }
    
    
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false },
            onVersionClick = { showChangelogDialog = true }
        )
    }
    
    if (showChangelogDialog) {
        ChangelogDialog(onDismiss = { showChangelogDialog = false })
    }
    
    if (showAcknowledgementDialog) {
        AcknowledgementDialog(onDismiss = { showAcknowledgementDialog = false })
    }
    
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = uiState.themeMode,
            onDismiss = { showThemeDialog = false },
            onModeSelected = { mode ->
                viewModel.setThemeMode(mode)
                showThemeDialog = false
            }
        )
    }
    
    if (showClassificationModeDialog) {
        QuickAlbumSettingsDialog(
            albumAddAction = uiState.albumAddAction,
            cardSortingAlbumEnabled = uiState.cardSortingAlbumEnabled,
            albumTagSize = uiState.albumTagSize,
            maxAlbumTagCount = uiState.maxAlbumTagCount,
            hasManageStoragePermission = uiState.hasManageStoragePermission,
            isPermissionApplicable = viewModel.isManageStoragePermissionApplicable(),
            onDismiss = { showClassificationModeDialog = false },
            onAlbumAddActionSelected = { viewModel.setAlbumAddAction(it) },
            onCardSortingAlbumEnabledChanged = { viewModel.setCardSortingAlbumEnabled(it) },
            onAlbumTagSizeChanged = { viewModel.setAlbumTagSize(it) },
            onMaxAlbumTagCountChanged = { viewModel.setMaxAlbumTagCount(it) },
            onRequestPermission = {
                // Open settings to grant permission
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                ).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Settings item with a toggle switch.
 */
@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTaskSettingsDialog(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onTargetChange: (Int) -> Unit,
    onModeChange: (DailyTaskMode) -> Unit,
    onProgressNotificationChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("每日任务设置") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Enable Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("开启每日任务")
                    Switch(
                        checked = uiState.dailyTaskEnabled,
                        onCheckedChange = onEnabledChange
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                if (uiState.dailyTaskEnabled) {
                    // Daily Target (Preset Options + Custom)
                    Text(
                        text = "每日目标数量",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    TargetSelector(
                        currentValue = uiState.dailyTaskTarget,
                        onValueChange = onTargetChange
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Task Mode
                    Text(
                        text = "任务模式",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = uiState.dailyTaskMode == DailyTaskMode.FLOW,
                            onClick = { onModeChange(DailyTaskMode.FLOW) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("一站式整理")
                        }
                        SegmentedButton(
                            selected = uiState.dailyTaskMode == DailyTaskMode.QUICK,
                            onClick = { onModeChange(DailyTaskMode.QUICK) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("快速整理")
                        }
                    }
                    
                    // Mode description
                    Text(
                        text = if (uiState.dailyTaskMode == DailyTaskMode.FLOW) {
                            "完整的照片整理流程，包括筛选、分类到相册、清理回收站"
                        } else {
                            "专注于照片去留的快速决策，适合批量处理大量照片"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 状态栏进度通知（前台服务）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "状态栏进度通知",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "时刻督促自己完成每日目标",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.progressNotificationEnabled,
                            onCheckedChange = { enabled ->
                                onProgressNotificationChange(enabled)
                                // 启动或停止前台服务
                                if (enabled) {
                                    com.example.photozen.service.DailyProgressService.start(context)
                                } else {
                                    com.example.photozen.service.DailyProgressService.stop(context)
                                }
                            }
                        )
                    }
                } else {
                    Text(
                        text = "开启每日任务，养成整理好习惯",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

/**
 * Wheel-style time picker dialog with hour and minute columns.
 */
@Composable
private fun WheelTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "设置提醒时间",
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Current selected time display
                Text(
                    text = String.format("%02d:%02d", selectedHour, selectedMinute),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour wheel
                    WheelPicker(
                        items = (0..23).toList(),
                        selectedItem = selectedHour,
                        onItemSelected = { selectedHour = it },
                        modifier = Modifier.weight(1f),
                        label = "时"
                    )
                    
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    // Minute wheel
                    WheelPicker(
                        items = (0..59).toList(),
                        selectedItem = selectedMinute,
                        onItemSelected = { selectedMinute = it },
                        modifier = Modifier.weight(1f),
                        label = "分"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                Text("确定")
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
 * Single wheel picker column with snapping behavior.
 * Fixed initialization issue: prevents incorrect scroll to 00:00 on dialog open.
 */
@Composable
private fun WheelPicker(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    // 关键：追踪是否已完成初始化，防止在布局完成前触发错误更新
    var isInitialized by remember { mutableStateOf(false) }
    
    // 直接使用 selectedItem 作为初始位置
    // contentPadding = 76.dp 刚好让 item 出现在高亮区域中心
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedItem.coerceIn(items.indices)
    )
    val hapticFeedback = LocalHapticFeedback.current
    
    // Track the center item - 只有初始化后才计算真正的 centerIndex
    val centerIndex by remember {
        derivedStateOf {
            // 未初始化时，直接返回 selectedItem，避免错误更新
            if (!isInitialized) return@derivedStateOf selectedItem
            
            val layoutInfo = listState.layoutInfo
            // 布局信息为空时，返回 selectedItem
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf selectedItem
            
            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs(item.offset + item.size / 2 - viewportCenter)
            }?.index ?: selectedItem
        }
    }
    
    // 初始滚动 + 延迟启用 centerIndex 更新
    LaunchedEffect(Unit) {
        // 直接滚动到 selectedItem，contentPadding 会自动使其居中
        listState.scrollToItem(selectedItem.coerceIn(items.indices))
        // 等待布局稳定后再启用 centerIndex 更新
        delay(150)
        isInitialized = true
    }
    
    // 只有初始化完成后才响应 centerIndex 变化
    LaunchedEffect(centerIndex) {
        if (isInitialized && centerIndex in items.indices && items[centerIndex] != selectedItem) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onItemSelected(items[centerIndex])
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Selection highlight background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
        )
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            contentPadding = PaddingValues(vertical = 76.dp) // Centers items
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = index == centerIndex
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { 
                            onItemSelected(item)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", item),
                        style = if (isSelected) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                }
            }
        }
        
        // Label at the top right
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Target selector with preset options and custom input.
 * Presets: 10, 20, 50, 100, 200, 500, 1000
 * Custom: 1-2000
 */
@Composable
fun TargetSelector(
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    val presetValues = listOf(10, 20, 50, 100, 200, 500, 1000)
    var showCustomInput by remember { mutableStateOf(currentValue !in presetValues) }
    var customText by remember { mutableStateOf(if (currentValue !in presetValues) currentValue.toString() else "") }
    var customError by remember { mutableStateOf<String?>(null) }
    
    Column {
        // Preset chips in a flow layout
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetValues.forEach { value ->
                FilterChip(
                    selected = currentValue == value && !showCustomInput,
                    onClick = {
                        showCustomInput = false
                        customError = null
                        onValueChange(value)
                    },
                    label = { Text(value.toString()) }
                )
            }
            
            // Custom option
            FilterChip(
                selected = showCustomInput,
                onClick = {
                    showCustomInput = true
                    if (customText.isNotEmpty()) {
                        customText.toIntOrNull()?.let { onValueChange(it) }
                    }
                },
                label = { Text("自定义") },
                leadingIcon = if (showCustomInput) {
                    { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
        
        // Custom input field
        if (showCustomInput) {
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = customText,
                onValueChange = { newText ->
                    customText = newText
                    val parsed = newText.toIntOrNull()
                    when {
                        newText.isEmpty() -> {
                            customError = null
                        }
                        parsed == null -> {
                            customError = "请输入有效数字"
                        }
                        parsed < 1 || parsed > 2000 -> {
                            customError = "范围: 1-2000"
                        }
                        else -> {
                            customError = null
                            onValueChange(parsed)
                        }
                    }
                },
                label = { Text("输入目标数量 (1-2000)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = customError != null,
                supportingText = customError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Show current value if custom and not in text field
        if (!showCustomInput && currentValue !in presetValues) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "当前目标: $currentValue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

@Composable
fun PhotoFilterSettingsDialog(
    currentMode: PhotoFilterMode,
    onDismiss: () -> Unit,
    onModeSelected: (PhotoFilterMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("待整理照片范围") },
        text = {
            Column {
                FilterOption(
                    title = "整理全部照片",
                    description = "整理设备上的所有照片",
                    selected = currentMode == PhotoFilterMode.ALL,
                    onClick = { onModeSelected(PhotoFilterMode.ALL) }
                )
                
                FilterOption(
                    title = "仅整理相机照片",
                    description = "只整理由手机相机拍摄的照片",
                    selected = currentMode == PhotoFilterMode.CAMERA_ONLY,
                    onClick = { onModeSelected(PhotoFilterMode.CAMERA_ONLY) }
                )
                
                FilterOption(
                    title = "排除相机照片",
                    description = "整理除相机照片外的所有照片",
                    selected = currentMode == PhotoFilterMode.EXCLUDE_CAMERA,
                    onClick = { onModeSelected(PhotoFilterMode.EXCLUDE_CAMERA) }
                )
                
                FilterOption(
                    title = "每次整理前选择",
                    description = "开始整理时选择日期范围和相册",
                    selected = currentMode == PhotoFilterMode.CUSTOM,
                    onClick = { onModeSelected(PhotoFilterMode.CUSTOM) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * Album picker dialog for selecting multiple albums.
 */
@Composable
private fun AlbumPickerDialog(
    albums: List<com.example.photozen.data.source.Album>,
    selectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var localSelectedIds by remember { mutableStateOf(selectedIds) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择相册") },
        text = {
            if (albums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "正在加载相册...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column {
                    Text(
                        text = "已选择 ${localSelectedIds.size} 个相册",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(albums.size) { index ->
                            val album = albums[index]
                            val isSelected = album.id in localSelectedIds
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        localSelectedIds = if (isSelected) {
                                            localSelectedIds - album.id
                                        } else {
                                            localSelectedIds + album.id
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        localSelectedIds = if (checked) {
                                            localSelectedIds + album.id
                                        } else {
                                            localSelectedIds - album.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${album.photoCount} 张照片",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (album.isCamera) {
                                    Text(
                                        text = "相机",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(localSelectedIds) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun FilterOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * App introduction dialog showing features and highlights.
 */
@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onVersionClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("关于 PhotoZen")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // App positioning
                Text(
                    text = "📷 让整理照片变成一种享受",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "PhotoZen 是一款专为摄影爱好者设计的照片整理神器。告别繁琐的相册管理，用最自然的方式筛选你的照片。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                HorizontalDivider()
                
                // Core features
                FeatureSection(
                    title = "🎴 滑动整理",
                    description = "像刷 Tinder 一样筛选照片！左右滑保留，上滑删除，下滑待定。丝滑动画 + 触感反馈，让整理变成解压游戏。"
                )
                
                FeatureSection(
                    title = "🔍 对比抉择",
                    description = "纠结哪张更好？同时对比多张照片，同步缩放查看细节，轻松做出取舍。"
                )
                
                FeatureSection(
                    title = "🏷️ 标签气泡",
                    description = "可拖拽的物理气泡图！标签越大说明照片越多，拖来拖去还有弹性碰撞，谁说管理标签不能好玩？"
                )
                
                FeatureSection(
                    title = "✂️ 无损编辑",
                    description = "裁切照片不伤原图，还能创建虚拟副本。一张照片多种构图，随时恢复，尽情尝试。"
                )
                
                FeatureSection(
                    title = "🚀 心流模式",
                    description = "一键进入沉浸式整理：滑动→对比→打标签→完成！连击系统让你越整理越上瘾。"
                )
                
                FeatureSection(
                    title = "🏆 成就系统",
                    description = "50+ 成就等你解锁！从整理新手到传说大师，每一步都有惊喜。"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

/**
 * Feature section in the about dialog.
 */
@Composable
private fun FeatureSection(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Acknowledgement Card - Flat display with heart animation.
 */
@Composable
private fun AcknowledgementCard(
    onHeartClick: () -> Unit
) {
    // Heart colors palette
    val heartColors = listOf(
        Color(0xFFFF6B6B),  // Coral red
        Color(0xFFFF8E8E),  // Light red
        Color(0xFFFFB3B3),  // Pink
        Color(0xFFFF69B4),  // Hot pink
        Color(0xFFFF1493),  // Deep pink
        Color(0xFFE91E63),  // Material pink
        Color(0xFFF48FB1),  // Light pink
    )
    
    // List of floating hearts
    val floatingHearts = remember { mutableStateListOf<FloatingHeart>() }
    var heartIconColor by remember { mutableStateOf(Color(0xFFE91E63)) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Floating hearts layer
            floatingHearts.forEach { heart ->
                FloatingHeartAnimation(
                    heart = heart,
                    onAnimationEnd = { floatingHearts.remove(heart) }
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable heart icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(heartIconColor.copy(alpha = 0.1f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            heartIconColor = heartColors.random()
                            val newHeart = FloatingHeart(
                                id = System.currentTimeMillis() + Random.nextLong(1000),
                                angle = Random.nextFloat() * 2 * Math.PI.toFloat(),
                                distance = 100f + Random.nextFloat() * 80f,
                                duration = 2500 + Random.nextInt(1000),
                                startDelay = 0,
                                color = heartColors.random(),
                                maxScale = 1.0f + Random.nextFloat() * 0.5f
                            )
                            floatingHearts.add(newHeart)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "点击发送爱心",
                        tint = heartIconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "鸣谢",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "感谢宝子们的积极体验与宝贵建议",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onHeartClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "查看详情",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Feedback link - Text link to feedback page.
 */
@Composable
private fun FeedbackLink() {
    val uriHandler = LocalUriHandler.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "💬 意见反馈与功能许愿",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                uriHandler.openUri("https://xhslink.com/m/2Mb9Y6fyvMS")
            }
        )
    }
}

/**
 * About Card - Flat display with version info.
 */
@Composable
private fun AboutCard(
    onInfoClick: () -> Unit,
    onVersionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable(onClick = onInfoClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PhotoZen 图禅",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "让整理照片变成一种享受",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            TextButton(onClick = onVersionClick) {
                Text(
                    text = "v${com.example.photozen.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Data class for floating heart animation.
 */
private data class FloatingHeart(
    val id: Long,
    val angle: Float,      // Direction in radians
    val distance: Float,   // How far to travel
    val duration: Int,     // Animation duration in ms
    val startDelay: Int,   // Delay before starting
    val color: Color,      // Heart color
    val maxScale: Float    // Maximum scale
)

/**
 * Acknowledgement dialog with floating hearts animation.
 */
@Composable
private fun AcknowledgementDialog(onDismiss: () -> Unit) {
    // List of floating hearts
    val floatingHearts = remember { mutableStateListOf<FloatingHeart>() }
    
    // Heart colors palette
    val heartColors = listOf(
        Color(0xFFFF6B6B),  // Coral red
        Color(0xFFFF8E8E),  // Light red
        Color(0xFFFFB3B3),  // Pink
        Color(0xFFFF69B4),  // Hot pink
        Color(0xFFFF1493),  // Deep pink
        Color(0xFFE91E63),  // Material pink
        Color(0xFFF48FB1),  // Light pink
    )
    
    // Current heart icon color (changes on each click)
    var heartIconColor by remember { mutableStateOf(Color(0xFFE91E63)) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Floating hearts layer
                floatingHearts.forEach { heart ->
                    FloatingHeartAnimation(
                        heart = heart,
                        onAnimationEnd = { floatingHearts.remove(heart) }
                    )
                }
                
                // Main content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Clickable heart icon
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                // Change the heart icon color
                                heartIconColor = heartColors.random()
                                
                                // Create a new floating heart with longer duration and faster movement
                                val newHeart = FloatingHeart(
                                    id = System.currentTimeMillis() + Random.nextLong(1000),
                                    angle = Random.nextFloat() * 2 * Math.PI.toFloat(),
                                    distance = 150f + Random.nextFloat() * 100f,
                                    duration = 3000 + Random.nextInt(1500),
                                    startDelay = 0,
                                    color = heartColors.random(),
                                    maxScale = 1.2f + Random.nextFloat() * 0.6f
                                )
                                floatingHearts.add(newHeart)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "点击发送爱心",
                            tint = heartIconColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "感谢以下早期体验者",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "土土酱 · 涵涵酱 · hi\n霁光 · momo · 晚菀第三声\nAdobe PS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "感谢你们的积极体验与宝贵建议\n让 PhotoZen 变得更好",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

/**
 * Animated floating heart that grows, floats away, and fades out.
 */
@Composable
private fun FloatingHeartAnimation(
    heart: FloatingHeart,
    onAnimationEnd: () -> Unit
) {
    // Animation progress (0 to 1)
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(heart.id) {
        delay(heart.startDelay.toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = heart.duration,
                easing = LinearEasing
            )
        )
        onAnimationEnd()
    }
    
    val currentProgress = progress.value
    
    // Scale: starts small, grows to max, then slightly shrinks
    val scale = when {
        currentProgress < 0.3f -> currentProgress / 0.3f * heart.maxScale
        currentProgress < 0.7f -> heart.maxScale
        else -> heart.maxScale * (1f - (currentProgress - 0.7f) / 0.3f * 0.3f)
    }
    
    // Alpha: fully visible until 60%, then fade out
    val alpha = when {
        currentProgress < 0.6f -> 1f
        else -> 1f - (currentProgress - 0.6f) / 0.4f
    }
    
    // Position: move outward from center
    val distance = heart.distance * currentProgress
    val offsetX = (cos(heart.angle) * distance).roundToInt()
    val offsetY = (sin(heart.angle) * distance - currentProgress * 30f).roundToInt() // Slight upward drift
    
    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = null,
        tint = heart.color,
        modifier = Modifier
            .offset { IntOffset(offsetX, offsetY) }
            .scale(scale)
            .alpha(alpha)
            .size(20.dp)
    )
}

/**
 * Theme selection dialog.
 */
@Composable
private fun ThemeSelectionDialog(
    currentMode: com.example.photozen.data.repository.ThemeMode,
    onDismiss: () -> Unit,
    onModeSelected: (com.example.photozen.data.repository.ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("外观样式") },
        text = {
            Column {
                com.example.photozen.data.repository.ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = { onModeSelected(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (mode) {
                                    com.example.photozen.data.repository.ThemeMode.DARK -> "深色模式"
                                    com.example.photozen.data.repository.ThemeMode.LIGHT -> "浅色模式"
                                    com.example.photozen.data.repository.ThemeMode.SYSTEM -> "跟随系统"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (mode) {
                                    com.example.photozen.data.repository.ThemeMode.DARK -> "深色背景，适合夜间使用"
                                    com.example.photozen.data.repository.ThemeMode.LIGHT -> "浅色背景，适合白天使用"
                                    com.example.photozen.data.repository.ThemeMode.SYSTEM -> "根据系统设置自动切换"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * Swipe sensitivity setting with slider.
 */
@Composable
private fun SwipeSensitivitySetting(
    sensitivity: Float,
    onSensitivityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "滑动灵敏度",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = when {
                        sensitivity <= 0.6f -> "非常灵敏"
                        sensitivity <= 0.8f -> "灵敏"
                        sensitivity <= 1.1f -> "正常"
                        sensitivity <= 1.3f -> "不灵敏"
                        else -> "非常不灵敏"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "灵敏",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = sensitivity,
                onValueChange = onSensitivityChange,
                valueRange = 0.5f..1.5f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = "不灵敏",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Quick album classification settings dialog.
 */
@Composable
private fun QuickAlbumSettingsDialog(
    albumAddAction: com.example.photozen.data.repository.AlbumAddAction,
    cardSortingAlbumEnabled: Boolean,
    albumTagSize: Float,
    maxAlbumTagCount: Int,
    hasManageStoragePermission: Boolean,
    isPermissionApplicable: Boolean,
    onDismiss: () -> Unit,
    onAlbumAddActionSelected: (com.example.photozen.data.repository.AlbumAddAction) -> Unit,
    onCardSortingAlbumEnabledChanged: (Boolean) -> Unit,
    onAlbumTagSizeChanged: (Float) -> Unit,
    onMaxAlbumTagCountChanged: (Int) -> Unit,
    onRequestPermission: () -> Unit
) {
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快速相册分类设置") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Card sorting album enabled
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "滑动筛选照片时可分类到相册",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "筛选界面底部将显示快捷添加到相册的入口",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cardSortingAlbumEnabled,
                        onCheckedChange = onCardSortingAlbumEnabledChanged
                    )
                }
                
                // Album tag settings (only if enabled)
                if (cardSortingAlbumEnabled) {
                    // Album tag size
                    Text(
                        text = "相册标签大小",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "小",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = albumTagSize,
                            onValueChange = onAlbumTagSizeChanged,
                            valueRange = 0.6f..1.5f,
                            // Remove steps to allow continuous sliding to edges
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text(
                            text = "大",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Max album tag count
                    Text(
                        text = "最大显示数量: ${if (maxAlbumTagCount == 0) "不限" else maxAlbumTagCount.toString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    Slider(
                        value = maxAlbumTagCount.toFloat(),
                        onValueChange = { onMaxAlbumTagCountChanged(it.toInt()) },
                        valueRange = 0f..20f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Album add action
                Text(
                    text = "添加到相册时默认操作",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                com.example.photozen.data.repository.AlbumAddAction.entries.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (action == com.example.photozen.data.repository.AlbumAddAction.MOVE &&
                                    !hasManageStoragePermission && isPermissionApplicable) {
                                    showPermissionDialog = true
                                }
                                onAlbumAddActionSelected(action)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = action == albumAddAction,
                            onClick = {
                                if (action == com.example.photozen.data.repository.AlbumAddAction.MOVE &&
                                    !hasManageStoragePermission && isPermissionApplicable) {
                                    showPermissionDialog = true
                                }
                                onAlbumAddActionSelected(action)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (action) {
                                    com.example.photozen.data.repository.AlbumAddAction.COPY -> "复制到相册"
                                    com.example.photozen.data.repository.AlbumAddAction.MOVE -> "移动到相册"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = when (action) {
                                    com.example.photozen.data.repository.AlbumAddAction.COPY -> "照片保留在原位置"
                                    com.example.photozen.data.repository.AlbumAddAction.MOVE -> "照片从原位置移除"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Permission warning - only show when permission is needed and not granted
                if (albumAddAction == com.example.photozen.data.repository.AlbumAddAction.MOVE &&
                    !hasManageStoragePermission && isPermissionApplicable) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            .clickable { onRequestPermission() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "需要文件管理权限",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "点击前往设置授权",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("保存")
            }
        }
    )
    
    // Permission guidance dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要权限") },
            text = {
                Text("移动照片需要文件管理权限。授权后，移动操作将无需每次确认。\n\n如不授权，系统会在每次移动时请求确认。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    onRequestPermission()
                }) {
                    Text("去授权")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("稍后再说")
                }
            }
        )
    }
}
