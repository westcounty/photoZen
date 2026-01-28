package com.example.photozen.widget.memory

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photozen.data.repository.MemoryLanePhotoSource
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.Album
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.ui.components.SwitchSettingsItem
import com.example.photozen.ui.components.ValueSettingsItem
import com.example.photozen.ui.theme.PicZenTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 时光拾遗小部件配置页面
 *
 * 允许用户配置每个小部件实例的独立设置:
 * - 刷新间隔
 * - "那年今日"优先开关
 * - 诗意时间格式开关
 */
@AndroidEntryPoint
class MemoryLaneWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var mediaStoreDataSource: MediaStoreDataSource

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    // Whether this is a global settings mode (opened from Settings page)
    private var isGlobalSettingsMode = false

    companion object {
        /**
         * Extra to indicate this is opened from Settings page for global configuration.
         * When true, uses widgetId = 0 as global default configuration.
         */
        const val EXTRA_GLOBAL_SETTINGS_MODE = "extra_global_settings_mode"

        /**
         * Widget ID for global default settings (used when opened from Settings page).
         * New widgets will inherit these settings.
         */
        const val GLOBAL_DEFAULT_WIDGET_ID = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if this is global settings mode (from Settings page)
        isGlobalSettingsMode = intent?.getBooleanExtra(EXTRA_GLOBAL_SETTINGS_MODE, false) ?: false

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        // Get the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If global settings mode, use the global default widget ID
        if (isGlobalSettingsMode) {
            appWidgetId = GLOBAL_DEFAULT_WIDGET_ID
        } else if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // If no valid widget ID and not global mode, finish the activity
            finish()
            return
        }

        setContent {
            PicZenTheme(darkTheme = true) {
                MemoryLaneConfigScreen(
                    widgetId = appWidgetId,
                    preferencesRepository = preferencesRepository,
                    mediaStoreDataSource = mediaStoreDataSource,
                    onConfirm = { confirmConfiguration() },
                    onCancel = { finish() },
                    isGlobalMode = isGlobalSettingsMode
                )
            }
        }
    }

    private fun confirmConfiguration() {
        // Schedule periodic refresh with current interval
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val intervalMinutes = preferencesRepository.getWidgetRefreshInterval(appWidgetId)
            MemoryLaneWidgetWorker.schedule(this@MemoryLaneWidgetConfigActivity, intervalMinutes)
        }

        // Update all Memory Lane widgets
        MemoryLaneWidget.triggerUpdate(this)
        MemoryLaneWidgetCompact.triggerUpdate(this)
        MemoryLaneWidgetLarge.triggerUpdate(this)

        // Return the result
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryLaneConfigScreen(
    widgetId: Int,
    preferencesRepository: PreferencesRepository,
    mediaStoreDataSource: MediaStoreDataSource,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isGlobalMode: Boolean = false
) {
    val scope = rememberCoroutineScope()

    // Load per-widget settings (with global fallback)
    var refreshInterval by remember { mutableIntStateOf(120) }
    var thisDayPriority by remember { mutableStateOf(true) }
    var poeticTime by remember { mutableStateOf(true) }
    var photoSource by remember { mutableStateOf(MemoryLanePhotoSource.ALL) }
    var selectedAlbums by remember { mutableStateOf<Set<String>>(emptySet()) }
    var excludedAlbums by remember { mutableStateOf<Set<String>>(emptySet()) }
    var allAlbums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load initial values
    LaunchedEffect(widgetId) {
        refreshInterval = preferencesRepository.getWidgetRefreshInterval(widgetId)
        thisDayPriority = preferencesRepository.getWidgetThisDayPriority(widgetId)
        poeticTime = preferencesRepository.getWidgetPoeticTime(widgetId)
        photoSource = preferencesRepository.getWidgetPhotoSource(widgetId)
        selectedAlbums = preferencesRepository.getWidgetSelectedAlbums(widgetId)
        excludedAlbums = preferencesRepository.getWidgetExcludedAlbums(widgetId)
        allAlbums = mediaStoreDataSource.getAllAlbums()
        isLoading = false
    }

    // Dialog state for refresh interval picker
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showAlbumSelectorDialog by remember { mutableStateOf(false) }
    var albumSelectorMode by remember { mutableStateOf(MemoryLanePhotoSource.ONLY_ALBUMS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "时光拾遗设置",
                            fontWeight = FontWeight.Bold
                        )
                        if (isGlobalMode) {
                            Text(
                                text = "新添加的小组件将使用这些设置",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "取消"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onConfirm, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "确认",
                            tint = if (isLoading)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Preview card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🏞️",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = if (poeticTime) "三年前的夏天" else "2023年7月28日",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (thisDayPriority) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "那年今日",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Refresh interval setting
                ValueSettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "刷新间隔",
                    value = formatInterval(refreshInterval),
                    onClick = { showIntervalDialog = true },
                    subtitle = "小部件自动更换照片的间隔"
                )

                // Photo source setting
                ValueSettingsItem(
                    icon = Icons.Default.Photo,
                    title = "照片来源",
                    value = formatPhotoSource(photoSource),
                    onClick = { showPhotoSourceDialog = true },
                    subtitle = "选择小部件展示的照片范围"
                )

                // Album selection (only show when source requires albums)
                if (photoSource == MemoryLanePhotoSource.ONLY_ALBUMS) {
                    val albumCount = selectedAlbums.size
                    val albumNames = allAlbums
                        .filter { it.id in selectedAlbums }
                        .take(3)
                        .joinToString("、") { it.name }
                    val displayText = if (albumCount == 0) "点击选择相册"
                        else if (albumCount <= 3) albumNames
                        else "$albumNames 等 $albumCount 个相册"

                    ValueSettingsItem(
                        icon = Icons.Default.Photo,
                        title = "选择相册",
                        value = displayText,
                        onClick = {
                            albumSelectorMode = MemoryLanePhotoSource.ONLY_ALBUMS
                            showAlbumSelectorDialog = true
                        },
                        subtitle = "仅从这些相册中选取照片"
                    )
                }

                if (photoSource == MemoryLanePhotoSource.EXCLUDE_ALBUMS) {
                    val albumCount = excludedAlbums.size
                    val albumNames = allAlbums
                        .filter { it.id in excludedAlbums }
                        .take(3)
                        .joinToString("、") { it.name }
                    val displayText = if (albumCount == 0) "点击选择相册"
                        else if (albumCount <= 3) albumNames
                        else "$albumNames 等 $albumCount 个相册"

                    ValueSettingsItem(
                        icon = Icons.Default.Photo,
                        title = "排除相册",
                        value = displayText,
                        onClick = {
                            albumSelectorMode = MemoryLanePhotoSource.EXCLUDE_ALBUMS
                            showAlbumSelectorDialog = true
                        },
                        subtitle = "不从这些相册中选取照片"
                    )
                }

                // This day in history priority
                SwitchSettingsItem(
                    icon = Icons.Default.CalendarToday,
                    title = "\"那年今日\"优先",
                    checked = thisDayPriority,
                    onCheckedChange = { enabled ->
                        thisDayPriority = enabled
                        scope.launch {
                            preferencesRepository.setWidgetThisDayPriority(widgetId, enabled)
                        }
                    },
                    subtitle = "优先展示历史上同一天的照片"
                )

                // Poetic time format
                SwitchSettingsItem(
                    icon = Icons.Default.FormatQuote,
                    title = "诗意时间格式",
                    checked = poeticTime,
                    onCheckedChange = { enabled ->
                        poeticTime = enabled
                        scope.launch {
                            preferencesRepository.setWidgetPoeticTime(widgetId, enabled)
                        }
                    },
                    subtitle = "使用\"三年前的夏天\"代替具体日期"
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Confirm button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("确认")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Interval picker dialog
    if (showIntervalDialog) {
        RefreshIntervalDialog(
            currentInterval = refreshInterval,
            onDismiss = { showIntervalDialog = false },
            onConfirm = { interval ->
                refreshInterval = interval
                scope.launch {
                    preferencesRepository.setWidgetRefreshInterval(widgetId, interval)
                }
                showIntervalDialog = false
            }
        )
    }

    // Photo source picker dialog
    if (showPhotoSourceDialog) {
        PhotoSourceDialog(
            currentSource = photoSource,
            onDismiss = { showPhotoSourceDialog = false },
            onConfirm = { source ->
                photoSource = source
                scope.launch {
                    preferencesRepository.setWidgetPhotoSource(widgetId, source)
                }
                showPhotoSourceDialog = false
            }
        )
    }

    // Album selector dialog
    if (showAlbumSelectorDialog) {
        val currentSelection = if (albumSelectorMode == MemoryLanePhotoSource.ONLY_ALBUMS) {
            selectedAlbums
        } else {
            excludedAlbums
        }

        AlbumSelectorDialog(
            albums = allAlbums,
            selectedAlbumIds = currentSelection,
            title = if (albumSelectorMode == MemoryLanePhotoSource.ONLY_ALBUMS) "选择相册" else "排除相册",
            onDismiss = { showAlbumSelectorDialog = false },
            onConfirm = { newSelection ->
                if (albumSelectorMode == MemoryLanePhotoSource.ONLY_ALBUMS) {
                    selectedAlbums = newSelection
                    scope.launch {
                        preferencesRepository.setWidgetSelectedAlbums(widgetId, newSelection)
                    }
                } else {
                    excludedAlbums = newSelection
                    scope.launch {
                        preferencesRepository.setWidgetExcludedAlbums(widgetId, newSelection)
                    }
                }
                showAlbumSelectorDialog = false
            }
        )
    }
}

@Composable
private fun RefreshIntervalDialog(
    currentInterval: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val intervals = listOf(
        30 to "30 分钟",
        60 to "1 小时",
        120 to "2 小时",
        240 to "4 小时",
        480 to "8 小时",
        1440 to "每天"
    )

    var selectedInterval by remember { mutableIntStateOf(currentInterval) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("刷新间隔") },
        text = {
            Column {
                intervals.forEach { (minutes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedInterval == minutes,
                            onClick = { selectedInterval = minutes }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedInterval) }) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun formatInterval(minutes: Int): String {
    return when (minutes) {
        30 -> "30 分钟"
        60 -> "1 小时"
        120 -> "2 小时"
        240 -> "4 小时"
        480 -> "8 小时"
        1440 -> "每天"
        else -> "$minutes 分钟"
    }
}

private fun formatPhotoSource(source: MemoryLanePhotoSource): String {
    return when (source) {
        MemoryLanePhotoSource.ALL -> "全部照片"
        MemoryLanePhotoSource.ONLY_ALBUMS -> "仅指定相册"
        MemoryLanePhotoSource.EXCLUDE_ALBUMS -> "排除指定相册"
    }
}

@Composable
private fun PhotoSourceDialog(
    currentSource: MemoryLanePhotoSource,
    onDismiss: () -> Unit,
    onConfirm: (MemoryLanePhotoSource) -> Unit
) {
    val sources = listOf(
        MemoryLanePhotoSource.ALL to "全部照片" to "从所有未分类照片中随机选取",
        MemoryLanePhotoSource.ONLY_ALBUMS to "仅指定相册" to "仅从指定相册中选取照片",
        MemoryLanePhotoSource.EXCLUDE_ALBUMS to "排除指定相册" to "排除指定相册中的照片"
    )

    var selectedSource by remember { mutableStateOf(currentSource) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("照片来源") },
        text = {
            Column {
                sources.forEach { (sourceWithLabel, description) ->
                    val (source, label) = sourceWithLabel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSource == source,
                            onClick = { selectedSource = source }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedSource) }) {
                Text("确认")
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
private fun AlbumSelectorDialog(
    albums: List<Album>,
    selectedAlbumIds: Set<String>,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var currentSelection by remember { mutableStateOf(selectedAlbumIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (albums.isEmpty()) {
                Text(
                    text = "没有找到相册",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(albums) { album ->
                        val isSelected = album.id in currentSelection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentSelection = if (isSelected) {
                                        currentSelection - album.id
                                    } else {
                                        currentSelection + album.id
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    currentSelection = if (checked) {
                                        currentSelection + album.id
                                    } else {
                                        currentSelection - album.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${album.photoCount} 张照片",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentSelection) }) {
                Text("确认 (${currentSelection.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
