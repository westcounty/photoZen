package com.example.photozen.ui.components

/**
 * PhotoZen 照片列表手势规范 v1.0
 * ================================
 * 
 * 所有照片列表页面必须遵循以下手势规范：
 * 
 * ## 1. 点击 (Tap)
 * - 非选择模式：进入全屏预览
 * - 选择模式：切换该照片的选中状态
 * 
 * ## 2. 长按不移动 (Long Press without Drag)
 * - 选中当前照片
 * - 进入选择模式
 * - 显示底部操作栏
 * - 触发震动反馈 (HapticFeedbackType.LongPress, ~50ms)
 * 
 * ## 3. 长按 + 拖动 (Long Press + Drag)
 * - 从长按位置开始批量选择
 * - 拖动经过的所有照片都被选中（范围选择）
 * - 自动滚动（当拖动到网格边缘时）
 * - 每新增一个选中项触发震动反馈 (HapticFeedbackType.TextHandleMove, ~20ms)
 * 
 * ## 4. 退出选择模式
 * - 点击顶栏关闭按钮
 * - 按返回键
 * - 取消所有选择（选中数归零时自动退出）
 * 
 * ## 5. 使用页面
 * - PhotoListScreen (已使用)
 * - TrashScreen (已使用)
 * - AlbumPhotoListScreen (Phase 1-B 改造)
 * - TimelineScreen (Phase 1-B 改造，使用简化版本 TimelineEventPhotoRow)
 * 
 * @see DragSelectConfig 拖动选择配置
 * @see DragSelectPhotoGridDefaults 预设配置
 * @see TimelineEventPhotoRow 时间线照片行组件（简化版，仅支持长按选择）
 */

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ==================== 配置类 ====================

/**
 * 拖动选择配置
 * 
 * 定义 DragSelectPhotoGrid 的行为参数，可用于针对不同场景进行微调。
 * 
 * @property hapticEnabled 是否启用震动反馈
 * @property autoScrollEnabled 是否启用自动滚动（拖动到边缘时）
 * @property autoScrollThresholdDp 触发自动滚动的边缘距离（dp）
 * @property autoScrollSpeed 自动滚动速度
 * @property dragThresholdDp 区分"长按不动"和"拖动选择"的移动阈值（dp）
 */
data class DragSelectConfig(
    val hapticEnabled: Boolean = true,
    val autoScrollEnabled: Boolean = true,
    val autoScrollThresholdDp: Float = 80f,
    val autoScrollSpeed: Float = 10f,
    val dragThresholdDp: Float = 10f
)

/**
 * DragSelectPhotoGrid 预设配置对象
 * 
 * 提供针对不同场景优化的预设配置。
 */
object DragSelectPhotoGridDefaults {
    /**
     * 标准配置 - 适用于大多数照片网格
     * 
     * 启用所有功能：震动反馈、自动滚动、拖动选择
     */
    val StandardConfig = DragSelectConfig()
    
    /**
     * 相册配置 - 与标准配置相同
     * 
     * 用于 AlbumPhotoListScreen
     */
    val AlbumConfig = StandardConfig
    
    /**
     * 时间线配置 - 禁用自动滚动
     * 
     * 时间线使用水平布局，不适合垂直自动滚动
     */
    val TimelineConfig = DragSelectConfig(
        autoScrollEnabled = false
    )
    
    /**
     * 紧凑配置 - 更小的拖动阈值
     * 
     * 适用于小尺寸网格或需要更灵敏响应的场景
     */
    val CompactConfig = DragSelectConfig(
        dragThresholdDp = 6f,
        autoScrollThresholdDp = 60f
    )
}

// ==================== 网格模式枚举 ====================

/**
 * 照片网格显示模式
 *
 * @property SQUARE 正方形网格 - 照片裁切为正方形，整齐排列，支持拖动多选
 * @property WATERFALL 瀑布流 - 照片按原比例显示，禁用拖动多选，仅支持长按选中+点击切换
 */
enum class PhotoGridMode {
    /** 正方形网格 - 支持拖动多选 */
    SQUARE,
    /** 瀑布流 - 禁用拖动多选 */
    WATERFALL
}

// ==================== 组件 ====================

/**
 * Photo grid with drag-to-select functionality.
 *
 * Features:
 * - Long press to enter selection mode
 * - Drag to select multiple photos (only in SQUARE mode)
 * - Auto-scroll when dragging near edges
 * - Haptic feedback on selection changes
 *
 * Grid Modes:
 * - SQUARE: Photos cropped to squares, supports drag-to-select
 * - WATERFALL: Photos in original aspect ratio, no drag-to-select
 *
 * @param photos List of photos to display
 * @param selectedIds Set of selected photo IDs
 * @param onSelectionChanged Callback when selection changes
 * @param onPhotoClick Callback when a photo is clicked (non-selection mode)
 * @param onPhotoLongPress Callback when a photo is long-pressed (for action sheet)
 * @param modifier Modifier for the grid
 * @param columns Number of columns (1-4)
 * @param selectionColor Color for selection indicator
 * @param enableDragSelect Whether drag-to-select is enabled (only works in SQUARE mode)
 * @param gridMode Grid display mode (SQUARE or WATERFALL)
 * @param staggeredGridState LazyStaggeredGridState for WATERFALL mode
 * @param squareGridState LazyGridState for SQUARE mode
 * @param config 拖动选择配置，用于自定义行为参数
 * @param showStatusBadge 是否显示照片状态徽章（非选择模式时）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DragSelectPhotoGrid(
    photos: List<PhotoEntity>,
    selectedIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onPhotoClick: (String, Int) -> Unit,
    onPhotoLongPress: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    selectionColor: Color = MaterialTheme.colorScheme.primary,
    enableDragSelect: Boolean = true,
    gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,
    staggeredGridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    squareGridState: LazyGridState = rememberLazyGridState(),
    config: DragSelectConfig = DragSelectPhotoGridDefaults.StandardConfig,
    showStatusBadge: Boolean = false,
    clickAlwaysTogglesSelection: Boolean = false,
    onSelectionToggle: ((String) -> Unit)? = null
) {
    // Note: Drag select functionality has been removed (Phase 3-10)
    // Long press is now handled by individual items only

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Render item content (shared between both grid types)
        @Composable
        fun PhotoItemContent(index: Int, photo: PhotoEntity, forceSquare: Boolean) {
            val isSelected = selectedIds.contains(photo.id)

            DragSelectPhotoItem(
                photo = photo,
                isSelected = isSelected,
                isSelectionMode = selectedIds.isNotEmpty(),
                selectionColor = selectionColor,
                enableLongPressOnItem = true, // Always handle long press on item
                showStatusBadge = showStatusBadge,
                hapticEnabled = config.hapticEnabled,
                forceSquare = forceSquare,
                onClick = {
                    if (clickAlwaysTogglesSelection || selectedIds.isNotEmpty()) {
                        // Use onSelectionToggle if provided to avoid closure capture issues
                        if (onSelectionToggle != null) {
                            onSelectionToggle(photo.id)
                        } else {
                            // Fallback to computing newSelection locally
                            val newSelection = if (isSelected) {
                                selectedIds - photo.id
                            } else {
                                selectedIds + photo.id
                            }
                            onSelectionChanged(newSelection)
                        }
                    } else {
                        onPhotoClick(photo.id, index)
                    }
                },
                onLongPress = {
                    // Long press selects the photo and enters selection mode
                    if (!selectedIds.contains(photo.id)) {
                        onSelectionChanged(selectedIds + photo.id)
                    }
                    onPhotoLongPress(photo.id, photo.systemUri)
                }
            )
        }

        when (gridMode) {
            PhotoGridMode.SQUARE -> {
                // Square grid with uniform cells - supports drag select
                // REQ-002: 网格视图支持 2-5 列切换
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns.coerceIn(2, 5)),
                    state = squareGridState,
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                        PhotoItemContent(index, photo, forceSquare = true)
                    }
                }
            }
            PhotoGridMode.WATERFALL -> {
                // Staggered grid with natural aspect ratios - no drag select
                // REQ-007: 瀑布流视图支持 1-5 列切换
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns.coerceIn(1, 5)),
                    state = staggeredGridState,
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalItemSpacing = 2.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                        PhotoItemContent(index, photo, forceSquare = false)
                    }
                }
            }
        }
    }
}

/**
 * Individual photo item with selection indicator.
 *
 * @param enableLongPressOnItem If true, long press is handled by this item.
 *        If false, long press is handled by the parent grid (for drag select).
 * @param showStatusBadge 是否显示照片状态徽章（非选择模式时）
 * @param hapticEnabled 是否启用震动反馈
 * @param forceSquare 是否强制正方形显示（用于网格模式）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DragSelectPhotoItem(
    photo: PhotoEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    selectionColor: Color,
    enableLongPressOnItem: Boolean,
    showStatusBadge: Boolean,
    hapticEnabled: Boolean,
    forceSquare: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // 使用 rememberUpdatedState 确保 pointerInput 中总是使用最新的回调
    // 这解决了闭包捕获旧值导致选择状态不更新的问题
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    // DES-036: 按压状态追踪，用于缩放动画
    var isPressed by remember { mutableStateOf(false) }

    // DES-036: 按压缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "photoItemScale"
    )

    // Calculate aspect ratio from photo dimensions (1:1 if forceSquare)
    val aspectRatio = remember(photo.width, photo.height, forceSquare) {
        if (forceSquare) {
            1f // Force square for grid mode
        } else if (photo.width > 0 && photo.height > 0) {
            photo.width.toFloat() / photo.height.toFloat()
        } else {
            1f // Default to square if dimensions unknown
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // DES-036: 应用缩放效果
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = selectionColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            // Unified gesture handling: use pointerInput to handle both tap and long press
            .pointerInput(enableLongPressOnItem, hapticEnabled) {
                detectTapGestures(
                    onPress = {
                        // DES-036: 开始按压时缩放
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { currentOnClick() },
                    onLongPress = if (enableLongPressOnItem) {
                        {
                            if (hapticEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            currentOnLongPress()
                        }
                    } else null
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(photo.systemUri))
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        )
        
        // Selection overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(selectionColor.copy(alpha = 0.2f))
            )
        }
        
        // Photo status badge (top-left, only show when not in selection mode)
        if (showStatusBadge && !isSelectionMode && photo.status != PhotoStatus.UNSORTED) {
            PhotoStatusBadge(
                status = photo.status,
                size = 20.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )
        }
        
        // Selection indicator
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) selectionColor
                        else Color.Black.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

