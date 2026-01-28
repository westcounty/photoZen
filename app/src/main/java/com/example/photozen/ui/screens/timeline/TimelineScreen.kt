package com.example.photozen.ui.screens.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.domain.GroupingMode
import com.example.photozen.domain.PhotoEvent
import com.example.photozen.ui.components.ArrowDirection
import com.example.photozen.ui.components.CompactSortingButton
import com.example.photozen.ui.components.EmptyStates
import com.example.photozen.ui.components.GuideTooltip
import com.example.photozen.ui.components.TimelineEventPhotoRow
import com.example.photozen.ui.components.SelectionTopBar
import com.example.photozen.ui.guide.rememberGuideState
import com.example.photozen.domain.model.GuideKey
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.photozen.ui.components.fullscreen.UnifiedFullscreenViewer
import com.example.photozen.ui.components.fullscreen.FullscreenActionType
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.photozen.ui.components.ConfirmDeleteSheet
import com.example.photozen.ui.components.DeleteType
import com.example.photozen.ui.components.shareImage
import com.example.photozen.ui.components.openImageWithChooser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Timeline Screen - Display photos grouped by time/events.
 * 
 * Phase 1-C: 作为底部导航主 Tab 之一
 * - onNavigateBack 标记为可选（由底部导航处理切换）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onPhotoClick: (String) -> Unit = {},
    onNavigateToSorter: () -> Unit = {},
    onNavigateToSorterListMode: () -> Unit = {},
    // Phase 1-C: 底部导航模式不需要返回按钮
    onNavigateBack: () -> Unit = {},
    // 导航到时间线详情页
    onNavigateToTimelineDetail: (title: String, startTime: Long, endTime: Long) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val albumBubbleList by viewModel.albumBubbleList.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Fullscreen preview state
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenPhotos by remember { mutableStateOf<List<PhotoEntity>>(emptyList()) }
    var fullscreenStartIndex by remember { mutableIntStateOf(0) }

    // 跟踪全屏预览位置（事件级别）
    var lastFullscreenEventIndex by remember { mutableIntStateOf(0) }
    var hasViewedFullscreen by remember { mutableStateOf(false) }

    // 删除确认状态
    var showDeleteConfirmSheet by remember { mutableStateOf(false) }
    var pendingDeletePhoto by remember { mutableStateOf<PhotoEntity?>(null) }

    // 删除结果 launcher
    val deleteResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingDeletePhoto?.let { photo ->
                viewModel.onPhotoDeleted(photo.id)
                // 从当前全屏预览列表中移除
                fullscreenPhotos = fullscreenPhotos.filter { it.id != photo.id }
                if (fullscreenPhotos.isEmpty()) {
                    showFullscreen = false
                }
            }
        }
        pendingDeletePhoto = null
    }
    
    // Handle navigation to sorter (card mode)
    LaunchedEffect(uiState.navigateToSorter) {
        if (uiState.navigateToSorter) {
            onNavigateToSorter()
            viewModel.onNavigationComplete()
        }
    }
    
    // Handle navigation to sorter (list mode)
    LaunchedEffect(uiState.navigateToSorterListMode) {
        if (uiState.navigateToSorterListMode) {
            onNavigateToSorterListMode()
            viewModel.onNavigationComplete()
        }
    }
    
    // Handle scroll to index
    LaunchedEffect(uiState.scrollToIndex) {
        uiState.scrollToIndex?.let { index ->
            listState.animateScrollToItem(index)
            viewModel.clearScrollTarget()
        }
    }
    
    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Show messages (Phase 1-B)
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // 退出全屏时恢复滚动位置（居中显示目标事件卡片）
    LaunchedEffect(showFullscreen) {
        if (!showFullscreen && hasViewedFullscreen) {
            hasViewedFullscreen = false  // 重置标志
            delay(50)  // 等待列表组合完成

            // 计算居中滚动位置：向前偏移约 1 个事件卡片
            // 因为事件卡片较大，偏移 1 个通常就足够居中
            val scrollToIndex = (lastFullscreenEventIndex - 1).coerceAtLeast(0)
                .coerceAtMost(uiState.events.lastIndex.coerceAtLeast(0))
            listState.scrollToItem(scrollToIndex)
        }
    }
    
    // Calculate current visible year-month
    val currentYearMonth by remember {
        derivedStateOf {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            if (uiState.events.isNotEmpty() && firstVisibleIndex < uiState.events.size) {
                val event = uiState.events[firstVisibleIndex]
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = event.startTime
                YearMonth(
                    year = calendar.get(java.util.Calendar.YEAR),
                    month = calendar.get(java.util.Calendar.MONTH) + 1,
                    eventCount = 0,
                    photoCount = 0,
                    firstEventIndex = firstVisibleIndex
                )
            } else null
        }
    }
    
    // Show quick navigation for AUTO and DAY modes
    val showQuickNav = uiState.groupingMode == GroupingMode.AUTO || uiState.groupingMode == GroupingMode.DAY
    
    // BackHandler 处理返回键退出选择模式
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }
    
    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                // 使用统一的选择模式顶栏
                SelectionTopBar(
                    selectedCount = uiState.selectedCount,
                    totalCount = uiState.totalPhotos,
                    onClose = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll() },
                    onDeselectAll = { viewModel.clearSelection() }
                )
            } else {
                // 普通模式顶栏
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = Color(0xFFEC4899)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "时间线",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.totalPhotos > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${uiState.totalPhotos}",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        // 底部导航模式不显示返回按钮
                    },
                    actions = {
                        // Sort order toggle button
                        IconButton(onClick = viewModel::toggleSortOrder) {
                            Icon(
                                imageVector = if (uiState.isDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = if (uiState.isDescending) "最新优先" else "最早优先"
                            )
                        }
                        
                        // Expand/Collapse all button
                        if (uiState.hasEvents) {
                            val allExpanded = uiState.expandedEventIds.size == uiState.events.size
                            IconButton(
                                onClick = {
                                    if (allExpanded) viewModel.collapseAll() else viewModel.expandAll()
                                }
                            ) {
                                Icon(
                                    imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                    contentDescription = if (allExpanded) "折叠全部" else "展开全部"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Grouping mode selector
                GroupingModeSelector(
                    currentMode = uiState.groupingMode,
                    onModeSelected = viewModel::setGroupingMode
                )
                
                if (uiState.isLoading) {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("加载时间线...")
                        }
                    }
                } else if (!uiState.hasEvents) {
                    // Empty state
                    EmptyStates.EmptyTimeline(
                        onGoSort = onNavigateToSorter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Event list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.navigationBars),  // 系统导航栏 padding
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 16.dp + 80.dp  // 额外的底部 padding 避免被 app 导航栏遮挡
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.events, key = { it.id }) { event ->
                            TimelineEventCard(
                                event = event,
                                isExpanded = viewModel.isEventExpanded(event.id),
                                // Phase 1-B: 选择模式参数
                                isSelectionMode = uiState.isSelectionMode,
                                selectedIds = uiState.selectedPhotoIds,
                                onToggleExpanded = { viewModel.toggleEventExpanded(event.id) },
                                onPhotoClick = { photoId ->
                                    // 记录事件索引用于退出全屏后恢复滚动位置
                                    lastFullscreenEventIndex = uiState.events.indexOf(event)
                                    hasViewedFullscreen = true
                                    // Open fullscreen preview with current event's photos
                                    val clickedIndex = event.photos.indexOfFirst { it.id == photoId }
                                    fullscreenPhotos = event.photos
                                    fullscreenStartIndex = if (clickedIndex >= 0) clickedIndex else 0
                                    showFullscreen = true
                                },
                                // Phase 1-B: 长按进入选择模式
                                onPhotoLongPress = { photoId ->
                                    viewModel.enterSelectionMode(photoId)
                                },
                                // Phase 1-B: 选择模式下切换选中状态
                                onSelectionToggle = { photoId ->
                                    viewModel.togglePhotoSelection(photoId)
                                },
                                onSortGroup = { viewModel.sortGroup(event.startTime, event.endTime) },
                                onViewMore = {
                                    // 导航到时间线详情页
                                    onNavigateToTimelineDetail(event.title, event.startTime, event.endTime)
                                }
                            )
                        }
                    }
                }
            }
            
            // Phase 1-B: 选择模式底部操作栏
            AnimatedVisibility(
                visible = uiState.isSelectionMode && uiState.selectedCount > 0,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                TimelineSelectionBottomBar(
                    selectedCount = uiState.selectedCount,
                    onKeep = { viewModel.batchUpdateStatus(PhotoStatus.KEEP) },
                    onMaybe = { viewModel.batchUpdateStatus(PhotoStatus.MAYBE) },
                    onTrash = { viewModel.batchUpdateStatus(PhotoStatus.TRASH) }
                )
            }
            
            // Year-Month Navigator (right side panel)
            AnimatedVisibility(
                visible = uiState.showNavigator && showQuickNav,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                TimelineNavigator(
                    yearMonths = uiState.availableYearMonths,
                    currentYearMonth = currentYearMonth,
                    onYearMonthSelected = { year, month ->
                        viewModel.scrollToYearMonth(year, month)
                    },
                    onDismiss = { viewModel.hideNavigator() }
                )
            }
            
            // Current position FAB (only for AUTO and DAY modes)
            // 底部需要额外 padding 避免被 app 底部导航栏遮挡
            if (showQuickNav && uiState.hasEvents && !uiState.showNavigator) {
                CurrentPositionFab(
                    currentYearMonth = currentYearMonth,
                    onClick = { viewModel.toggleNavigator() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
                )
            }
        }
    }
    
    // Fullscreen preview overlay - 使用统一的 UnifiedFullscreenViewer
    // 使用 Dialog 包裹，确保在窗口级别渲染，覆盖底部导航栏
    if (showFullscreen && fullscreenPhotos.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            UnifiedFullscreenViewer(
                photos = fullscreenPhotos,
                initialIndex = fullscreenStartIndex.coerceIn(0, fullscreenPhotos.lastIndex),
                onExit = { showFullscreen = false },
                onAction = { actionType, photo ->
                    when (actionType) {
                        FullscreenActionType.COPY -> {
                            viewModel.copyPhoto(photo.id)
                        }
                        FullscreenActionType.OPEN_WITH -> {
                            openImageWithChooser(context, Uri.parse(photo.systemUri))
                        }
                        FullscreenActionType.EDIT -> {
                            // 时间线暂不支持编辑
                        }
                        FullscreenActionType.SHARE -> {
                            shareImage(context, Uri.parse(photo.systemUri))
                        }
                        FullscreenActionType.DELETE -> {
                            // 彻底删除 - 显示确认面板
                            pendingDeletePhoto = photo
                            showDeleteConfirmSheet = true
                        }
                    }
                },
                overlayContent = {
                    // 全屏预览删除确认面板
                    if (showDeleteConfirmSheet && pendingDeletePhoto != null) {
                        ConfirmDeleteSheet(
                            photos = listOf(pendingDeletePhoto!!),
                            deleteType = DeleteType.PERMANENT_DELETE,
                            onConfirm = {
                                showDeleteConfirmSheet = false
                                pendingDeletePhoto?.let { photo ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        try {
                                            val uri = Uri.parse(photo.systemUri)
                                            val deleteRequest = MediaStore.createDeleteRequest(
                                                context.contentResolver,
                                                listOf(uri)
                                            )
                                            deleteResultLauncher.launch(
                                                IntentSenderRequest.Builder(deleteRequest.intentSender).build()
                                            )
                                        } catch (e: Exception) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("删除失败: ${e.message}")
                                            }
                                            pendingDeletePhoto = null
                                        }
                                    } else {
                                        // Android 10 及以下
                                        viewModel.onPhotoDeleted(photo.id)
                                        fullscreenPhotos = fullscreenPhotos.filter { it.id != photo.id }
                                        if (fullscreenPhotos.isEmpty()) {
                                            showFullscreen = false
                                        }
                                        pendingDeletePhoto = null
                                    }
                                }
                            },
                            onDismiss = {
                                showDeleteConfirmSheet = false
                                pendingDeletePhoto = null
                            }
                        )
                    }
                }
            )
        }
    }
}

/**
 * Grouping mode selector chips.
 */
@Composable
private fun GroupingModeSelector(
    currentMode: GroupingMode,
    onModeSelected: (GroupingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(GroupingMode.entries) { mode ->
            FilterChip(
                selected = mode == currentMode,
                onClick = { onModeSelected(mode) },
                label = {
                    Text(
                        when (mode) {
                            GroupingMode.AUTO -> "智能分组"
                            GroupingMode.DAY -> "按天"
                            GroupingMode.MONTH -> "按月"
                            GroupingMode.YEAR -> "按年"
                        }
                    )
                },
                leadingIcon = if (mode == currentMode) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    {
                        Icon(
                            imageVector = when (mode) {
                                GroupingMode.AUTO -> Icons.Default.AutoAwesome
                                GroupingMode.DAY -> Icons.Default.Today
                                GroupingMode.MONTH -> Icons.Default.CalendarMonth
                                GroupingMode.YEAR -> Icons.Default.CalendarToday
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }
    }
}

/**
 * Timeline event card with expandable photo grid.
 * 
 * Phase 1-B: 支持选择模式
 */
@Composable
private fun TimelineEventCard(
    event: PhotoEvent,
    isExpanded: Boolean,
    isSelectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleExpanded: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onPhotoLongPress: (String) -> Unit = {},
    onSelectionToggle: (String) -> Unit = {},
    onSortGroup: () -> Unit,
    onViewMore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            // Header (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeline indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFEC4899), Color(0xFFF43F5E))
                            )
                        )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Cover image
                AsyncImage(
                    model = event.coverPhotoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Event info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Show sorted count / total count
                    Text(
                        text = "${event.sortedCount}/${event.photoCount} 张已整理",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (event.sortedCount == event.photoCount) 
                            Color(0xFF10B981) // Green when all sorted
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (event.location != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Sort group button (REQ-062: 统一样式)
                CompactSortingButton(
                    totalCount = event.photoCount,
                    sortedCount = event.sortedCount,
                    onClick = onSortGroup
                )
            }
            
            // Expanded content - Photo grid
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    )
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    // Photo grid (show max 12 photos in collapsed view)
                    TimelineEventPhotoRow(
                        photos = event.photos,
                        selectedIds = selectedIds,
                        isSelectionMode = isSelectionMode,
                        onPhotoClick = onPhotoClick,
                        onPhotoLongPress = onPhotoLongPress,
                        onSelectionToggle = onSelectionToggle,
                        maxDisplay = 12,
                        onViewMore = if (event.photos.size > 12) onViewMore else null
                    )
                }
            }
        }
    }
}

/**
 * Year-Month quick navigation panel.
 * Displays a scrollable list of year-month options for quick jumping.
 */
@Composable
private fun TimelineNavigator(
    yearMonths: List<YearMonth>,
    currentYearMonth: YearMonth?,
    onYearMonthSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Group by year
    val yearGroups = yearMonths.groupBy { it.year }
    var expandedYear by remember { mutableStateOf(currentYearMonth?.year) }
    
    Card(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .padding(vertical = 48.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            // Year-Month list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                yearGroups.entries.sortedByDescending { it.key }.forEach { (year, months) ->
                    val isExpanded = expandedYear == year
                    val isCurrentYear = currentYearMonth?.year == year
                    
                    // Year header
                    Surface(
                        onClick = { expandedYear = if (isExpanded) null else year },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCurrentYear) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isCurrentYear) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrentYear) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    
                    // Month list (expanded)
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            months.sortedByDescending { it.month }.forEach { ym ->
                                val isCurrentMonth = currentYearMonth?.year == ym.year && 
                                                     currentYearMonth.month == ym.month
                                Surface(
                                    onClick = { onYearMonthSelected(ym.year, ym.month) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCurrentMonth) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                                    else 
                                        Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${ym.month}月",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isCurrentMonth) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${ym.photoCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating action button showing current position and opening navigator.
 */
@Composable
private fun CurrentPositionFab(
    currentYearMonth: YearMonth?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    currentYearMonth?.let { ym ->
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${ym.year}年${ym.month}月",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Phase 1-B: 时间线选择模式底部操作栏
 * 
 * 提供批量标记照片状态的操作按钮。
 * 
 * @param selectedCount 选中的照片数量
 * @param onKeep 标记为保留
 * @param onMaybe 标记为待定
 * @param onTrash 标记为删除
 */
@Composable
private fun TimelineSelectionBottomBar(
    selectedCount: Int,
    onKeep: () -> Unit,
    onMaybe: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 保留
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onKeep)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "保留",
                    tint = Color(0xFF22C55E),  // KeepGreen
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "保留",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 待定
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onMaybe)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QuestionMark,
                    contentDescription = "待定",
                    tint = Color(0xFFFBBF24),  // MaybeAmber
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "待定",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 删除
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onTrash)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color(0xFFEF4444),  // TrashRed
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "删除",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
