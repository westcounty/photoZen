package com.example.photozen.ui.screens.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimePicker
import com.example.photozen.data.repository.DailyTaskMode
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.repository.PhotoFilterMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Settings Screen - App preferences and achievements.
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
    var showAboutDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daily Task Settings Section
            SectionTitle(title = "每日任务")
            
            DailyTaskSettingsCard(
                uiState = uiState,
                onEnabledChange = { viewModel.setDailyTaskEnabled(it) },
                onTargetChange = { viewModel.setDailyTaskTarget(it) },
                onModeChange = { viewModel.setDailyTaskMode(it) },
                onReminderEnabledChange = { viewModel.setDailyTaskEnabled(true); viewModel.setDailyReminderEnabled(it) },
                onReminderTimeChange = { h, m -> viewModel.setDailyReminderTime(h, m) }
            )
            
            // Photo Filter Settings Section
            SectionTitle(title = "待整理照片")
            
            PhotoFilterSettingsCard(
                currentMode = uiState.photoFilterMode,
                onModeSelected = { viewModel.setPhotoFilterMode(it) }
            )
            
            // Acknowledgement Section
            SectionTitle(title = "鸣谢")
            
            AcknowledgementCard()
            
            // About Section
            SectionTitle(title = "关于")
            
            AboutCard(
                onInfoClick = { showAboutDialog = true },
                onVersionClick = { showChangelogDialog = true }
            )
        }
    }
    
    // About Dialog (App Introduction)
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    
    // Changelog Dialog (Version History)
    if (showChangelogDialog) {
        ChangelogDialog(onDismiss = { showChangelogDialog = false })
    }
}

/**
 * Daily Task settings card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyTaskSettingsCard(
    uiState: SettingsUiState,
    onEnabledChange: (Boolean) -> Unit,
    onTargetChange: (Int) -> Unit,
    onModeChange: (DailyTaskMode) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "每日整理任务",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = uiState.dailyTaskEnabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
            
            if (uiState.dailyTaskEnabled) {
                // Target Slider
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = "每日目标: ${uiState.dailyTaskTarget} 张",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = uiState.dailyTaskTarget.toFloat(),
                        onValueChange = { onTargetChange(it.toInt()) },
                        valueRange = 10f..1000f,
                        steps = 98, // (1000-10)/10 - 1
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Mode Selection
                Text(
                    text = "任务模式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
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
                
                // Reminder Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "每日提醒",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (uiState.dailyReminderEnabled) {
                            Text(
                                text = String.format("%02d:%02d", uiState.dailyReminderTime.first, uiState.dailyReminderTime.second),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showTimePicker = true }
                            )
                        }
                    }
                    Switch(
                        checked = uiState.dailyReminderEnabled,
                        onCheckedChange = onReminderEnabledChange
                    )
                }
            } else {
                Text(
                    text = "开启每日任务，养成整理好习惯",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = uiState.dailyReminderTime.first,
            initialMinute = uiState.dailyReminderTime.second,
            is24Hour = true
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onReminderTimeChange(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            },
            text = {
                TimePicker(state = timeState)
            }
        )
    }
}

/**
 * Section title.
 */
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

/**
 * Photo filter settings card.
 */
@Composable
private fun PhotoFilterSettingsCard(
    currentMode: PhotoFilterMode,
    onModeSelected: (PhotoFilterMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "选择要整理的照片范围",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            
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
    }
}

/**
 * Filter option row with radio button.
 */
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
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * App introduction dialog showing features and highlights.
 */
@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
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
                // Version header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "v1.0.0.001",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "PhotoZen 图禅 —— 让整理照片变成一种享受。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider()
                
                // Core features list
                Text(
                    text = "核心功能",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                ChangelogItem("🎴 滑动整理", "Tinder 风格滑动、Spring 动画、批量选择、1/2/3 列切换")
                ChangelogItem("🔍 照片对比", "同时对比 2-4 张照片、同步缩放、快速决策")
                ChangelogItem("🏷️ 标签气泡", "物理模拟拖拽、弹性碰撞、位置记忆、层级结构")
                ChangelogItem("✂️ 无损编辑", "非破坏性裁切、虚拟副本、图片导出")
                ChangelogItem("🚀 心流模式", "一站式整理、连击系统、胜利动画")
                ChangelogItem("🏆 成就系统", "50+ 成就、5 个稀有度等级、进度追踪")
                ChangelogItem("📁 照片管理", "智能筛选、批量操作、回收站、外部删除同步")
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
 * Acknowledgement card for early testers with floating hearts animation.
 */
@Composable
private fun AcknowledgementCard() {
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
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Clickable heart icon (no ripple effect)
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                                distance = 150f + Random.nextFloat() * 100f,  // Increased distance for faster visual
                                duration = 3000 + Random.nextInt(1500),       // Much longer duration (3-4.5 seconds)
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
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "感谢以下早期体验者",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "土土酱 · 涵涵酱",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "感谢你们的宝贵建议和反馈\n让 PhotoZen 变得更好",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
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
 * About card with clickable info icon and version number.
 */
@Composable
private fun AboutCard(
    onInfoClick: () -> Unit,
    onVersionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clickable info icon for app introduction
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onInfoClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "了解更多",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "PhotoZen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Clickable version number for changelog
            Text(
                text = "版本 1.0.0.001",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onVersionClick)
                    .padding(vertical = 4.dp)
            )

            Text(
                text = "点击图标了解功能 · 点击版本号查看更新",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "高效的照片整理工具\n让照片管理更轻松",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
