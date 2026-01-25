package com.example.photozen.ui.screens.flowsorter

import androidx.activity.compose.BackHandler
import com.example.photozen.data.model.PhotoSortOrder
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.photozen.ui.components.FloatingAlbumTags
import com.example.photozen.ui.components.StoragePermissionDialog
import com.example.photozen.ui.components.SystemAlbumPickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.ui.components.ComboOverlay
import com.example.photozen.ui.components.DailyTaskTitle
import com.example.photozen.ui.components.FlowSorterTitle
import com.example.photozen.ui.components.fullscreen.UnifiedFullscreenViewer
import com.example.photozen.ui.components.fullscreen.FullscreenActionType
import com.example.photozen.ui.components.GuideTooltip
import com.example.photozen.ui.components.ArrowDirection
import com.example.photozen.ui.components.SelectableStaggeredPhotoGrid
import com.example.photozen.ui.components.SelectionModeTitle
import com.example.photozen.ui.guide.rememberGuideState
import com.example.photozen.domain.model.GuideKey
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.components.FilterButton
import com.example.photozen.ui.components.FilterChipRow
import com.example.photozen.ui.components.FilterBottomSheet
import com.example.photozen.ui.components.shareImage
import com.example.photozen.domain.model.FilterType
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import com.example.photozen.ui.util.rememberHapticFeedbackManager
import com.example.photozen.ui.components.onboarding.SwipeSortOnboarding
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Flow Sorter Screen - Tinder-style swipe interface for sorting photos.
 * 
 * Gestures:
 * - Swipe Left → Trash (delete)
 * - Swipe Right → Keep (preserve)
 * - Swipe Up → Maybe (review later in Light Table)
 * - Tap Photo → Fullscreen view with pinch-to-zoom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowSorterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLightTable: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlowSorterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Use immediate counter for instant UI feedback (bypasses combine flow delay)
    val sortedCountImmediate by viewModel.sortedCountImmediate.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Phase 3-7: 使用设置中的震动反馈开关
    val hapticManager = rememberHapticFeedbackManager(uiState.hapticFeedbackEnabled)

    // 视图切换引导状态
    val viewToggleGuide = rememberGuideState(
        guideKey = GuideKey.FLOW_SORTER_VIEW_TOGGLE,
        guideRepository = viewModel.guideRepository
    )
    var viewToggleBounds by remember { mutableStateOf<Rect?>(null) }

    // 筛选相关状态
    val filterConfig by viewModel.filterConfig.collectAsState()
    val filterPresets by viewModel.filterPresets.collectAsState()
    val albumNames by viewModel.albumNames.collectAsState()
    val albumsForFilter by viewModel.albumBubblesForFilter.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    
    // Fullscreen viewer state - 使用索引而非单个照片对象
    var fullscreenPhotoIndex by remember { mutableIntStateOf(-1) }
    val showFullscreen = fullscreenPhotoIndex >= 0

    // Handle back press - exit selection mode first, then fullscreen, then navigate back
    BackHandler(enabled = showFullscreen || uiState.isSelectionMode) {
        when {
            showFullscreen -> fullscreenPhotoIndex = -1
            uiState.isSelectionMode -> viewModel.clearSelection()
        }
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        // REQ-064: 优化后的标题布局
                        if (uiState.isSelectionMode) {
                            SelectionModeTitle(selectedCount = uiState.selectedCount)
                        } else if (uiState.isDailyTask) {
                            DailyTaskTitle(
                                currentCount = uiState.dailyTaskCurrent,
                                targetCount = uiState.dailyTaskTarget
                            )
                        } else if (uiState.totalCount > 0) {
                            FlowSorterTitle(
                                source = uiState.sourceName,
                                currentCount = sortedCountImmediate,
                                totalCount = uiState.totalCount
                            )
                        } else {
                            Text(
                                text = if (uiState.viewMode == FlowSorterViewMode.CARD) "快速整理" else "列表整理",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (uiState.isSelectionMode) {
                                viewModel.clearSelection()
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = if (uiState.isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (uiState.isSelectionMode) "取消选择" else "返回"
                            )
                        }
                    },
                    actions = {
                        if (uiState.isSelectionMode) {
                            // Select all button
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "全选"
                                )
                            }
                        } else {
                            // 筛选按钮
                            FilterButton(
                                activeFilterCount = filterConfig.activeFilterCount,
                                onClick = { showFilterSheet = true }
                            )
                            
                            // Sort order button - dropdown menu for consistency with other screens
                            var showSortMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        imageVector = when (uiState.sortOrder) {
                                            PhotoSortOrder.DATE_DESC -> Icons.Default.ArrowDownward
                                            PhotoSortOrder.DATE_ASC -> Icons.Default.ArrowUpward
                                            PhotoSortOrder.RANDOM -> Icons.Default.Shuffle
                                        },
                                        contentDescription = "排序: ${uiState.sortOrder.displayName}"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("时间正序") },
                                        onClick = {
                                            viewModel.setSortOrder(PhotoSortOrder.DATE_ASC)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (uiState.sortOrder == PhotoSortOrder.DATE_ASC) {
                                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("时间倒序") },
                                        onClick = {
                                            viewModel.setSortOrder(PhotoSortOrder.DATE_DESC)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (uiState.sortOrder == PhotoSortOrder.DATE_DESC) {
                                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("随机") },
                                        onClick = {
                                            viewModel.setSortOrder(PhotoSortOrder.RANDOM)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (uiState.sortOrder == PhotoSortOrder.RANDOM) {
                                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                }
                            }
                            
                            // View mode toggle with column selector (merged for list mode)
                            FlowSorterViewModeDropdown(
                                viewMode = uiState.viewMode,
                                gridColumns = uiState.gridColumns,
                                onViewModeChanged = { viewModel.setViewMode(it) },
                                onColumnsChanged = { viewModel.setGridColumns(it) },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    viewToggleBounds = coordinates.boundsInRoot()
                                }
                            )

                            // Undo button (only in card mode)
                            AnimatedVisibility(
                                visible = uiState.lastAction != null && uiState.viewMode == FlowSorterViewMode.CARD,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                IconButton(
                                    onClick = {
                                        hapticManager.performClick()
                                        viewModel.undoLastAction()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "撤销"
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
            // NOTE: bottomBar is NOT added here because FlowSorterContent already has BatchActionBar
            // Adding it here would cause duplicate action bars in list view
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 筛选条件 Chip 行
                FilterChipRow(
                    config = filterConfig,
                    albumNames = albumNames,
                    onEditFilter = { showFilterSheet = true },
                    onClearFilter = viewModel::clearFilter,
                    onClearAll = viewModel::clearAllFilters
                )
                
                // Main content
                FlowSorterContent(
                    isWorkflowMode = false,
                    onNavigateBack = onNavigateBack,
                    onNavigateToLightTable = onNavigateToLightTable,
                    viewModel = viewModel
                )
            }
        }
    }
    
    // 筛选面板
    if (showFilterSheet) {
        FilterBottomSheet(
            currentConfig = filterConfig,
            presets = filterPresets,
            albums = albumsForFilter,
            onConfigChange = viewModel::applyFilter,
            onSavePreset = viewModel::saveFilterPreset,
            onApplyPreset = { viewModel.applyFilter(it.config) },
            onDeletePreset = viewModel::deleteFilterPreset,
            onDismiss = { showFilterSheet = false }
        )
    }

    // 视图切换引导提示
    if (viewToggleGuide.shouldShow &&
        uiState.viewMode == FlowSorterViewMode.CARD && !uiState.isComplete) {
        GuideTooltip(
            visible = true,
            message = "📱 切换视图\n点击可在卡片和列表视图间切换",
            targetBounds = viewToggleBounds,
            arrowDirection = ArrowDirection.UP,
            onDismiss = viewToggleGuide.dismiss
        )
    }
}

/**
 * Flow Sorter Content - Reusable content for both standalone and workflow modes.
 *
 * @param isWorkflowMode When true, hides top bar and uses callback instead of navigation
 * @param onPhotoSorted Callback when a photo is sorted (with photoId, status and current combo)
 * @param onComplete Callback when all photos are sorted
 * @param onNavigateBack Callback for navigation back (standalone mode only)
 * @param onNavigateToLightTable Callback for navigation to Light Table (standalone mode only)
 */
@Composable
fun FlowSorterContent(
    isWorkflowMode: Boolean = false,
    onPhotoSorted: ((String, PhotoStatus, Int) -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    onNavigateBack: () -> Unit,
    onNavigateToLightTable: () -> Unit = {},
    viewModel: FlowSorterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Phase 3-7: 使用设置中的震动反馈开关
    val hapticManager = rememberHapticFeedbackManager(uiState.hapticFeedbackEnabled)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Fullscreen viewer state - 使用索引而非单个照片对象
    var fullscreenPhotoIndex by remember { mutableIntStateOf(-1) }
    val showFullscreen = fullscreenPhotoIndex >= 0

    // Album picker state
    var showAlbumPicker by remember { mutableStateOf(false) }
    val availableAlbums by viewModel.availableAlbums.collectAsState()
    var selectedAlbumIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // REQ-067: 滑动筛选全屏新手引导（替代分步引导）
    val swipeSortFullscreenGuide = rememberGuideState(
        guideKey = GuideKey.SWIPE_SORT_FULLSCREEN_GUIDE,
        guideRepository = viewModel.guideRepository
    )

    // Local view mode state for workflow mode (since we don't have TopAppBar)
    var localViewMode by remember { mutableStateOf(FlowSorterViewMode.CARD) }
    val effectiveViewMode = if (isWorkflowMode) localViewMode else uiState.viewMode

    // Handle back press in fullscreen or selection mode
    BackHandler(enabled = showFullscreen || uiState.isSelectionMode) {
        when {
            showFullscreen -> fullscreenPhotoIndex = -1
            uiState.isSelectionMode -> viewModel.clearSelection()
        }
    }
    
    // Notify workflow of completion
    // Only trigger when truly complete, not during reload
    LaunchedEffect(uiState.isComplete, uiState.isReloading) {
        if (uiState.isComplete && !uiState.isReloading && isWorkflowMode) {
            onComplete?.invoke()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Progress bar
            if (uiState.totalCount > 0 || uiState.isDailyTask) {
                val progress = if (uiState.isDailyTask && uiState.dailyTaskTarget > 0) {
                    uiState.dailyTaskCurrent.toFloat() / uiState.dailyTaskTarget
                } else {
                    uiState.progress
                }
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = KeepGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // Main content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isLoading || uiState.isReloading -> {
                        // Show loading during initial load or when reloading (e.g., sort order change)
                        // This prevents the "complete" screen from flashing during reload
                        LoadingContent()
                    }
                    uiState.isComplete || uiState.isDailyTaskComplete -> {
                        if (isWorkflowMode && !uiState.isDailyTaskComplete) {
                            // In workflow mode (normal), show minimal completion (will auto-advance)
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = KeepGreen,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "整理完成",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            CompletionContent(
                                keepCount = uiState.keepCount,
                                trashCount = uiState.trashCount,
                                maybeCount = uiState.maybeCount,
                                isDailyTask = uiState.isDailyTask,
                                dailyTarget = uiState.dailyTaskTarget,
                                onNavigateToLightTable = if (isWorkflowMode) { {} } else onNavigateToLightTable,
                                onGoBack = onNavigateBack
                            )
                        }
                    }
                    effectiveViewMode == FlowSorterViewMode.LIST -> {
                        // List view with staggered grid
                        // Long press enters selection mode directly (no context menu popup)
                        // "从此张开始筛选" is now in the bottom action bar when single photo is selected
                        SelectableStaggeredPhotoGrid(
                            photos = uiState.photos,
                            selectedIds = uiState.selectedPhotoIds,
                            onSelectionChanged = { viewModel.updateSelection(it) },
                            onPhotoClick = { photoId, index ->
                                // 点击进入全屏预览
                                fullscreenPhotoIndex = index
                            },
                            columns = uiState.gridColumns
                            // onPhotoLongClick removed - long press now only enters selection mode
                        )
                    }
                    else -> {
                        // Card stack with combo overlay
                        Box(modifier = Modifier.fillMaxSize()) {
                            CardStack(
                                uiState = uiState,
                                onSwipeLeft = { photoId ->
                                    // Left swipe = Keep - use photoId from callback
                                    val combo = viewModel.keepPhoto(photoId)
                                    hapticManager.performSwipeFeedback(combo, uiState.combo.level)
                                    onPhotoSorted?.invoke(photoId, PhotoStatus.KEEP, combo)
                                },
                                onSwipeRight = { photoId ->
                                    // Right swipe = Keep - use photoId from callback
                                    val combo = viewModel.keepPhoto(photoId)
                                    hapticManager.performSwipeFeedback(combo, uiState.combo.level)
                                    onPhotoSorted?.invoke(photoId, PhotoStatus.KEEP, combo)
                                },
                                onSwipeUp = { photoId ->
                                    // Up swipe = Trash - use photoId from callback
                                    val combo = viewModel.trashPhoto(photoId)
                                    hapticManager.performSwipeFeedback(combo, uiState.combo.level)
                                    onPhotoSorted?.invoke(photoId, PhotoStatus.TRASH, combo)
                                },
                                onSwipeDown = { photoId ->
                                    // Down swipe = Maybe - use photoId from callback
                                    val combo = viewModel.maybePhoto(photoId)
                                    hapticManager.performSwipeFeedback(combo, uiState.combo.level)
                                    onPhotoSorted?.invoke(photoId, PhotoStatus.MAYBE, combo)
                                },
                                // When album tags are shown, move photo info to the image itself
                                showInfoOnImage = uiState.cardSortingAlbumEnabled
                            )
                            
                            // Combo overlay
                            ComboOverlay(
                                comboState = uiState.combo,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 32.dp)
                            )
                            
                            // Floating album tags when enabled
                            FloatingAlbumTags(
                                albums = uiState.albumBubbleList,
                                tagSize = uiState.albumTagSize,
                                maxCount = uiState.maxAlbumTagCount,
                                onAlbumClick = { album ->
                                    hapticManager.performClick()
                                    viewModel.keepAndAddToAlbum(album.bucketId)
                                },
                                onAddAlbumClick = {
                                    hapticManager.performClick()
                                    // Load system albums and set existing bubble list as selected
                                    viewModel.loadSystemAlbums()
                                    selectedAlbumIds = uiState.albumBubbleList.map { it.bucketId }.toSet()
                                    showAlbumPicker = true
                                },
                                visible = uiState.cardSortingAlbumEnabled && 
                                          uiState.currentPhoto != null,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
            
            // Bottom bar for batch actions when in selection mode (list view)
            AnimatedVisibility(
                visible = uiState.isSelectionMode,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                // Calculate the index of the single selected photo (for "从此张开始筛选")
                val singleSelectedIndex = if (uiState.selectedCount == 1) {
                    val selectedId = uiState.selectedPhotoIds.first()
                    uiState.photos.indexOfFirst { it.id == selectedId }
                } else -1

                BatchActionBar(
                    selectedCount = uiState.selectedCount,
                    onKeep = { viewModel.keepSelectedPhotos() },
                    onTrash = { viewModel.trashSelectedPhotos() },
                    onMaybe = { viewModel.maybeSelectedPhotos() },
                    onStartFromHere = if (singleSelectedIndex >= 0) {
                        {
                            // Start from this photo and switch to card mode
                            viewModel.startFromIndex(singleSelectedIndex)
                            viewModel.clearSelection()
                            if (!isWorkflowMode) {
                                viewModel.setViewMode(FlowSorterViewMode.CARD)
                            } else {
                                localViewMode = FlowSorterViewMode.CARD
                            }
                        }
                    } else null
                )
            }
        }
        
        // View mode toggle button - shown in top right corner for workflow mode
        if (isWorkflowMode && !uiState.isComplete && !uiState.isDailyTaskComplete && !uiState.isLoading && !uiState.isReloading) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                // View mode toggle with selection mode actions
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show selection mode controls when in selection mode
                    if (uiState.isSelectionMode) {
                        // Clear selection button
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "取消选择",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        // Selected count badge
                        Text(
                            text = "${uiState.selectedCount}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Select all button
                        IconButton(
                            onClick = { viewModel.selectAll() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "全选",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Sort order button - dropdown menu for consistency with other screens
                        var showWorkflowSortMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showWorkflowSortMenu = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                            ) {
                                Icon(
                                    imageVector = when (uiState.sortOrder) {
                                        PhotoSortOrder.DATE_DESC -> Icons.Default.ArrowDownward
                                        PhotoSortOrder.DATE_ASC -> Icons.Default.ArrowUpward
                                        PhotoSortOrder.RANDOM -> Icons.Default.Shuffle
                                    },
                                    contentDescription = "排序: ${uiState.sortOrder.displayName}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showWorkflowSortMenu,
                                onDismissRequest = { showWorkflowSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("时间正序") },
                                    onClick = {
                                        viewModel.setSortOrder(PhotoSortOrder.DATE_ASC)
                                        showWorkflowSortMenu = false
                                    },
                                    leadingIcon = if (uiState.sortOrder == PhotoSortOrder.DATE_ASC) {
                                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                                DropdownMenuItem(
                                    text = { Text("时间倒序") },
                                    onClick = {
                                        viewModel.setSortOrder(PhotoSortOrder.DATE_DESC)
                                        showWorkflowSortMenu = false
                                    },
                                    leadingIcon = if (uiState.sortOrder == PhotoSortOrder.DATE_DESC) {
                                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                                DropdownMenuItem(
                                    text = { Text("随机") },
                                    onClick = {
                                        viewModel.setSortOrder(PhotoSortOrder.RANDOM)
                                        showWorkflowSortMenu = false
                                    },
                                    leadingIcon = if (uiState.sortOrder == PhotoSortOrder.RANDOM) {
                                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        // View mode dropdown (unified)
                        WorkflowViewModeDropdown(
                            viewMode = effectiveViewMode,
                            gridColumns = uiState.gridColumns,
                            onViewModeChanged = { newMode ->
                                localViewMode = newMode
                                viewModel.clearSelection()
                            },
                            onColumnsChanged = { viewModel.setGridColumns(it) }
                        )
                        // Undo button (card mode only)
                        AnimatedVisibility(
                            visible = uiState.lastAction != null && effectiveViewMode == FlowSorterViewMode.CARD,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.undoLastAction() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "撤销",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Snackbar host for messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // REQ-067: 滑动筛选全屏新手引导（首次进入卡片模式时显示）
        if (swipeSortFullscreenGuide.shouldShow &&
            effectiveViewMode == FlowSorterViewMode.CARD &&
            uiState.photos.isNotEmpty()) {
            SwipeSortOnboarding(
                onDismiss = swipeSortFullscreenGuide.dismiss
            )
        }

        // Fullscreen viewer - 使用统一的全屏预览组件
        if (showFullscreen && uiState.photos.isNotEmpty()) {
            UnifiedFullscreenViewer(
                photos = uiState.photos,
                initialIndex = fullscreenPhotoIndex.coerceIn(0, uiState.photos.lastIndex),
                onExit = { fullscreenPhotoIndex = -1 },
                onAction = { action, photo ->
                    when (action) {
                        FullscreenActionType.COPY -> { /* 复制功能 */ }
                        FullscreenActionType.OPEN_WITH -> { /* 用其他app打开 */ }
                        FullscreenActionType.EDIT -> { /* 编辑功能 */ }
                        FullscreenActionType.SHARE -> {
                            shareImage(context, android.net.Uri.parse(photo.systemUri))
                        }
                        FullscreenActionType.DELETE -> {
                            // 筛选页面不支持删除，可以提示用户
                        }
                    }
                }
            )
        }
    }

    // Album picker dialog for managing quick album list (using unified component)
    if (showAlbumPicker) {
        SystemAlbumPickerDialog(
            title = "管理快捷相册列表",
            albums = availableAlbums,
            selectedIds = selectedAlbumIds,
            isLoading = availableAlbums.isEmpty(),
            onToggleSelection = { albumId ->
                selectedAlbumIds = if (albumId in selectedAlbumIds) {
                    selectedAlbumIds - albumId
                } else {
                    selectedAlbumIds + albumId
                }
            },
            onConfirm = {
                // Add newly selected albums to quick list
                val existingIds = uiState.albumBubbleList.map { it.bucketId }.toSet()
                val toAdd = selectedAlbumIds - existingIds
                toAdd.forEach { bucketId ->
                    viewModel.addAlbumToQuickList(bucketId)
                }
                // Remove deselected albums
                val toRemove = existingIds - selectedAlbumIds
                toRemove.forEach { bucketId ->
                    viewModel.removeAlbumFromQuickList(bucketId)
                }
                showAlbumPicker = false
            },
            onDismiss = { showAlbumPicker = false },
            onCreateAlbum = { albumName ->
                viewModel.createAlbumAndAdd(albumName)
            }
        )
    }
    
    // Storage permission dialog for move operations
    if (uiState.showPermissionDialog) {
        StoragePermissionDialog(
            onOpenSettings = { viewModel.onOpenPermissionSettings() },
            onPermissionGranted = { viewModel.onPermissionGranted() },
            onDismiss = { viewModel.dismissPermissionDialog() },
            showRetryError = uiState.permissionRetryError
        )
    }
    
}

/**
 * Card stack showing current and upcoming photos with instant gesture response.
 * 
 * CRITICAL: Callbacks now receive the photo ID of the swiped card.
 * This ensures the correct photo is processed even during rapid swiping.
 */
@Composable
private fun CardStack(
    uiState: FlowSorterUiState,
    onSwipeLeft: (String) -> Unit,
    onSwipeRight: (String) -> Unit,
    onSwipeUp: (String) -> Unit,
    onSwipeDown: (String) -> Unit,
    showInfoOnImage: Boolean = false,
    modifier: Modifier = Modifier
) {
    SwipeableCardStack(
        photos = uiState.photos,
        swipeSensitivity = uiState.swipeSensitivity,
        hapticFeedbackEnabled = uiState.hapticFeedbackEnabled,  // Phase 3-7
        onSwipeLeft = onSwipeLeft,
        onSwipeRight = onSwipeRight,
        onSwipeUp = onSwipeUp,
        onSwipeDown = onSwipeDown,
        showInfoOnImage = showInfoOnImage,
        modifier = modifier
    )
}

/**
 * Loading state content.
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
 * Completion state content.
 */
@Composable
private fun CompletionContent(
    keepCount: Int,
    trashCount: Int,
    maybeCount: Int,
    isDailyTask: Boolean = false,
    dailyTarget: Int = 0,
    onNavigateToLightTable: () -> Unit,
    onGoBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Confetti animation for daily task completion
        if (isDailyTask) {
            ConfettiAnimation(
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(KeepGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = KeepGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (isDailyTask) "🎉 今日任务完成！" else "整理完成！",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isDailyTask) "已达成 ${dailyTarget} 张整理目标" else "所有照片已分类完毕",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Statistics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(count = keepCount, label = "保留", color = KeepGreen)
                StatItem(count = trashCount, label = "删除", color = TrashRed)
                StatItem(count = maybeCount, label = "待定", color = MaybeAmber)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Actions
            if (maybeCount > 0 && !isDailyTask) {
                Button(
                    onClick = onNavigateToLightTable,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaybeAmber
                    )
                ) {
                    Text(
                        text = "查看待定照片 ($maybeCount)",
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Button(
                onClick = onGoBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("返回首页")
            }
        }
    }
}

/**
 * Statistics item.
 */
@Composable
private fun StatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Data class for confetti particle.
 */
private data class ConfettiParticle(
    val id: Int,
    val startX: Float,        // Start position (0-1 of width)
    val color: Color,
    val size: Float,          // Size of the confetti piece
    val rotation: Float,      // Initial rotation
    val rotationSpeed: Float, // Rotation speed
    val fallSpeed: Float,     // Fall speed multiplier
    val swayAmplitude: Float, // Horizontal sway amplitude
    val swayFrequency: Float  // Horizontal sway frequency
)

/**
 * Confetti animation for celebration.
 * Displays colorful paper pieces falling from the top of the screen.
 */
@Composable
private fun ConfettiAnimation(
    modifier: Modifier = Modifier,
    particleCount: Int = 100
) {
    // Confetti colors
    val colors = listOf(
        Color(0xFFFF6B6B),  // Red
        Color(0xFFFFD93D),  // Yellow
        Color(0xFF6BCB77),  // Green
        Color(0xFF4D96FF),  // Blue
        Color(0xFFC9B1FF),  // Purple
        Color(0xFFFF9F45),  // Orange
        Color(0xFFFF6B9C),  // Pink
        Color(0xFF00D9FF),  // Cyan
    )
    
    // Generate particles once
    val particles = remember {
        List(particleCount) { index ->
            ConfettiParticle(
                id = index,
                startX = Random.nextFloat(),
                color = colors.random(),
                size = 8f + Random.nextFloat() * 12f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = 100f + Random.nextFloat() * 300f,
                fallSpeed = 0.6f + Random.nextFloat() * 0.8f,
                swayAmplitude = 20f + Random.nextFloat() * 40f,
                swayFrequency = 1f + Random.nextFloat() * 2f
            )
        }
    }
    
    // Animation progress
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Small delay before starting
        delay(200)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 4000,
                easing = LinearEasing
            )
        )
    }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val currentProgress = progress.value
        
        particles.forEach { particle ->
            // Calculate particle position
            val startY = -50f // Start above the screen
            val endY = height + 100f // End below the screen
            
            // Each particle has a delay based on its index
            val particleDelay = (particle.id % 30) * 0.02f
            val adjustedProgress = ((currentProgress - particleDelay) / (1f - particleDelay)).coerceIn(0f, 1f)
            
            if (adjustedProgress > 0f) {
                val y = startY + (endY - startY) * adjustedProgress * particle.fallSpeed
                
                // Horizontal sway
                val swayOffset = kotlin.math.sin(
                    adjustedProgress * particle.swayFrequency * 2 * Math.PI.toFloat()
                ) * particle.swayAmplitude
                
                val x = particle.startX * width + swayOffset
                
                // Rotation
                val rotation = particle.rotation + adjustedProgress * particle.rotationSpeed
                
                // Fade out at the bottom
                val alpha = when {
                    adjustedProgress > 0.8f -> 1f - (adjustedProgress - 0.8f) / 0.2f
                    else -> 1f
                }.coerceIn(0f, 1f)
                
                // Draw confetti piece
                rotate(rotation, pivot = Offset(x, y)) {
                    drawRect(
                        color = particle.color.copy(alpha = alpha),
                        topLeft = Offset(x - particle.size / 2, y - particle.size / 4),
                        size = androidx.compose.ui.geometry.Size(particle.size, particle.size / 2)
                    )
                }
            }
        }
    }
}

/**
 * Bottom action bar for batch operations with vertical icon+text layout.
 *
 * @param selectedCount Number of selected photos
 * @param onKeep Callback for keep action
 * @param onTrash Callback for trash action
 * @param onMaybe Callback for maybe action
 * @param onStartFromHere Callback for "从此张开始筛选" action (only shown when single photo selected)
 */
@Composable
private fun BatchActionBar(
    selectedCount: Int,
    onKeep: () -> Unit,
    onTrash: () -> Unit,
    onMaybe: () -> Unit,
    onStartFromHere: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "从此张开始筛选" button (only when single photo is selected)
            if (onStartFromHere != null) {
                FlowSorterBottomBarActionItem(
                    icon = Icons.Default.FilterList,
                    label = "从此开始",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onStartFromHere
                )
            }

            // Keep button
            FlowSorterBottomBarActionItem(
                icon = Icons.Default.Favorite,
                label = "保留",
                color = KeepGreen,
                onClick = onKeep
            )

            // Maybe button
            FlowSorterBottomBarActionItem(
                icon = Icons.Default.QuestionMark,
                label = "待定",
                color = MaybeAmber,
                onClick = onMaybe
            )

            // Trash button
            FlowSorterBottomBarActionItem(
                icon = Icons.Default.Delete,
                label = "删除",
                color = TrashRed,
                onClick = onTrash
            )
        }
    }
}

@Composable
private fun FlowSorterBottomBarActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Workflow 模式下的视图模式下拉菜单（浮动按钮样式）
 *
 * 提供卡片/列表模式切换和列数调整功能：
 * - 卡片模式: 滑动整理
 * - 列表模式: 网格浏览，支持 1-4 列
 */
@Composable
private fun WorkflowViewModeDropdown(
    viewMode: FlowSorterViewMode,
    gridColumns: Int,
    onViewModeChanged: (FlowSorterViewMode) -> Unit,
    onColumnsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
        ) {
            Icon(
                imageVector = if (viewMode == FlowSorterViewMode.CARD)
                    Icons.Default.ViewCarousel else Icons.Default.GridView,
                contentDescription = "视图模式",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 卡片模式选项
            DropdownMenuItem(
                text = { Text("卡片模式") },
                onClick = {
                    onViewModeChanged(FlowSorterViewMode.CARD)
                    expanded = false
                },
                leadingIcon = {
                    if (viewMode == FlowSorterViewMode.CARD) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                },
                trailingIcon = { Icon(Icons.Default.ViewCarousel, null) }
            )

            // 列表模式选项
            DropdownMenuItem(
                text = { Text("列表模式") },
                onClick = {
                    onViewModeChanged(FlowSorterViewMode.LIST)
                    expanded = false
                },
                leadingIcon = {
                    if (viewMode == FlowSorterViewMode.LIST) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                },
                trailingIcon = { Icon(Icons.Default.GridView, null) }
            )

            // 列数选项（仅列表模式时显示）
            if (viewMode == FlowSorterViewMode.LIST) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "列数",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 1..4) {
                        FilterChip(
                            selected = col == gridColumns,
                            onClick = {
                                onColumnsChanged(col)
                                expanded = false
                            },
                            label = { Text("$col") }
                        )
                    }
                }
            }
        }
    }
}

/**
 * FlowSorter 视图模式下拉菜单
 *
 * 提供卡片/列表模式切换和列数调整功能：
 * - 卡片模式: 滑动整理
 * - 列表模式: 网格浏览，支持 1-4 列
 *
 * @param viewMode 当前视图模式
 * @param gridColumns 当前列数（仅列表模式有效）
 * @param onViewModeChanged 视图模式改变回调
 * @param onColumnsChanged 列数改变回调
 * @param modifier Modifier
 */
@Composable
private fun FlowSorterViewModeDropdown(
    viewMode: FlowSorterViewMode,
    gridColumns: Int,
    onViewModeChanged: (FlowSorterViewMode) -> Unit,
    onColumnsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = if (viewMode == FlowSorterViewMode.CARD)
                    Icons.Default.ViewCarousel else Icons.Default.GridView,
                contentDescription = "视图模式"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 卡片模式选项
            DropdownMenuItem(
                text = {
                    Text(
                        text = "卡片模式",
                        color = if (viewMode == FlowSorterViewMode.CARD) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                },
                onClick = {
                    onViewModeChanged(FlowSorterViewMode.CARD)
                    expanded = false
                },
                leadingIcon = {
                    if (viewMode == FlowSorterViewMode.CARD) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ViewCarousel,
                        contentDescription = null,
                        tint = if (viewMode == FlowSorterViewMode.CARD) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )

            // 列表模式选项
            DropdownMenuItem(
                text = {
                    Text(
                        text = "列表模式",
                        color = if (viewMode == FlowSorterViewMode.LIST) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                },
                onClick = {
                    onViewModeChanged(FlowSorterViewMode.LIST)
                    expanded = false
                },
                leadingIcon = {
                    if (viewMode == FlowSorterViewMode.LIST) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = if (viewMode == FlowSorterViewMode.LIST) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )

            // 列数选项（仅列表模式时显示）
            if (viewMode == FlowSorterViewMode.LIST) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "列数",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (col in 1..4) {
                        val isSelected = col == gridColumns
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onColumnsChanged(col)
                                expanded = false
                            },
                            label = { Text("$col") },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
