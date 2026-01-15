package com.example.photozen.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.repository.PhotoFilterMode

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
            
            AboutCard(onVersionClick = { showChangelogDialog = true })
        }
    }
    
    // Changelog Dialog
    if (showChangelogDialog) {
        ChangelogDialog(onDismiss = { showChangelogDialog = false })
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
 * Changelog dialog showing version history.
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Version 1.5.2
                ChangelogVersion(
                    version = "1.5.2",
                    date = "2026-01-15",
                    changes = listOf(
                        "✨ 保留照片列表新增排序功能",
                        "🔢 快速分类横幅显示未标签照片数量",
                        "🎯 修复排序图标区分：正序↑、倒序↓、随机🔀",
                        "⏰ 排序改为基于创建时间",
                        "🏆 修复清洁工/清理大师成就触发",
                        "📊 成就页面新增统计模块",
                        "🐛 修复照片列表排序时首张固定的问题"
                    )
                )
                
                HorizontalDivider()
                
                // Version 1.5.0
                ChangelogVersion(
                    version = "1.5.0",
                    date = "2026-01-15",
                    changes = listOf(
                        "🔄 筛选模式新增排序功能（时间正序/倒序/随机）",
                        "📋 复制照片功能完整保留EXIF信息",
                        "🔗 移动模式优化为直接移动文件",
                        "🐛 修复多选删除崩溃问题"
                    )
                )
                
                HorizontalDivider()
                
                // Version 1.4.0
                ChangelogVersion(
                    version = "1.4.0",
                    date = "2026-01-14",
                    changes = listOf(
                        "🎨 全新简洁现代的应用图标",
                        "✨ 首页名称更新为 PhotoZen",
                        "🔄 标签照片数量现在实时更新",
                        "📝 新增更新日志查看入口"
                    )
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
 * Single version changelog entry.
 */
@Composable
private fun ChangelogVersion(
    version: String,
    date: String,
    changes: List<String>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "v$version",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        changes.forEach { change ->
            Text(
                text = change,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

/**
 * Acknowledgement card for early testers.
 */
@Composable
private fun AcknowledgementCard() {
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
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            
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

/**
 * About card with clickable version number to show changelog.
 */
@Composable
private fun AboutCard(onVersionClick: () -> Unit) {
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
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "PhotoZen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Clickable version number
            Text(
                text = "版本 1.5.2",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onVersionClick)
                    .padding(vertical = 4.dp)
            )
            
            Text(
                text = "点击查看更新日志",
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
