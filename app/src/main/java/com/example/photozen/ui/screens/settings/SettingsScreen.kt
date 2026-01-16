package com.example.photozen.ui.screens.settings

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.repository.DailyTaskMode
import com.example.photozen.data.repository.PhotoFilterMode
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
    
    // Dialog states
    var showDailyTaskDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var showAcknowledgementDialog by remember { mutableStateOf(false) }
    
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Feedback Link
            FeedbackLink()
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Dialogs
    if (showDailyTaskDialog) {
        DailyTaskSettingsDialog(
            uiState = uiState,
            onDismiss = { showDailyTaskDialog = false },
            onEnabledChange = { viewModel.setDailyTaskEnabled(it) },
            onTargetChange = { viewModel.setDailyTaskTarget(it) },
            onModeChange = { viewModel.setDailyTaskMode(it) },
            onReminderEnabledChange = { enabled -> 
                if (enabled) viewModel.setDailyTaskEnabled(true)
                viewModel.setDailyReminderEnabled(enabled) 
            },
            onReminderTimeChange = { h, m -> viewModel.setDailyReminderTime(h, m) }
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
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

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
                            Text("心流模式")
                        }
                        SegmentedButton(
                            selected = uiState.dailyTaskMode == DailyTaskMode.QUICK,
                            onClick = { onModeChange(DailyTaskMode.QUICK) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("快速整理")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Reminder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "每日提醒",
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (uiState.dailyReminderEnabled) {
                                Text(
                                    text = String.format("%02d:%02d", uiState.dailyReminderTime.first, uiState.dailyReminderTime.second),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Switch(
                            checked = uiState.dailyReminderEnabled,
                            onCheckedChange = onReminderEnabledChange
                        )
                    }
                    
                    if (uiState.dailyReminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Alarm, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "设置提醒时间: ${String.format("%02d:%02d", uiState.dailyReminderTime.first, uiState.dailyReminderTime.second)}")
                        }
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
    
    if (showTimePicker) {
        WheelTimePickerDialog(
            initialHour = uiState.dailyReminderTime.first,
            initialMinute = uiState.dailyReminderTime.second,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                onReminderTimeChange(hour, minute)
                showTimePicker = false
            }
        )
    }
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
 */
@Composable
private fun WheelPicker(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedItem - 2).coerceAtLeast(0)
    )
    val hapticFeedback = LocalHapticFeedback.current
    
    // Track the center item
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs(item.offset + item.size / 2 - viewportCenter)
            }?.index ?: selectedItem
        }
    }
    
    // Update selection when center item changes
    LaunchedEffect(centerIndex) {
        if (centerIndex in items.indices && items[centerIndex] != selectedItem) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onItemSelected(items[centerIndex])
        }
    }
    
    // Scroll to initial position
    LaunchedEffect(Unit) {
        listState.scrollToItem((selectedItem - 2).coerceAtLeast(0))
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
 * Changelog dialog showing version history from CHANGELOG.md.
 */
@Composable
private fun ChangelogDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("更新日志")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // v1.1 Version header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "v1.1.0.018",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "2026-01-17",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "🚀 体验优化版本",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "针对用户反馈进行全面体验优化",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // v1.1 features
                ChangelogItem("🎯 首页布局", "每日任务升级为核心入口，一站式整理变为次要入口")
                ChangelogItem("⚡ 快速滑动", "彻底解决快速滑动崩溃，恢复丝滑动画")
                ChangelogItem("📊 进度显示", "修复分母显示，正确显示真实待整理总数")
                ChangelogItem("📱 桌面小组件", "每日任务进度实时更新，布局优化")
                ChangelogItem("⚙️ 默认设置", "快速整理模式、默认开启提醒、晚上10点")
                
                HorizontalDivider()
                
                // v1.0 Version header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "v1.0.0.001",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "2026-01-16",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "🎉 第一个正式版本！",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Core features list (abbreviated)
                Text(
                    text = "包含核心功能：滑动整理、照片对比、标签气泡、无损编辑、心流模式、成就系统、照片管理等",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
 * Single changelog item.
 */
@Composable
private fun ChangelogItem(title: String, description: String) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
                        text = "感谢 土土酱 · 涵涵酱 的宝贵建议",
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
 * Feedback link - Small text link to feedback page.
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
            text = "意见反馈与功能许愿",
            style = MaterialTheme.typography.labelSmall,
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
                    text = "v1.1.0.018",
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
                        text = "土土酱 · 涵涵酱",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "感谢你们的宝贵建议和反馈\n让 PhotoZen 变得更好",
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
