package com.example.photozen.ui.screens.flowsorter

import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoSortOrder
import com.example.photozen.data.repository.AlbumAddAction
import com.example.photozen.data.repository.PhotoFilterMode

/**
 * FlowSorterUiState 子状态定义
 * 
 * 将原来 30+ 字段的 FlowSorterUiState 拆分为多个职责清晰的子状态：
 * - SorterPhotosState: 照片列表相关
 * - SorterCountersState: 计数器相关
 * - SorterConfigState: 配置相关
 * - SorterAlbumState: 相册模式相关
 * - SorterDailyTaskState: 每日任务相关
 * - SorterPermissionState: 权限对话框相关
 * 
 * ## 迁移策略
 * 
 * 这些子状态类与现有 FlowSorterUiState 并存，不修改现有代码。
 * 后续阶段二会逐步迁移 FlowSorterViewModel 使用这些新结构。
 * 
 * @since Phase 4 - 状态管理重构
 */

// ============== 照片列表子状态 ==============

/**
 * 照片列表子状态
 * 
 * 包含照片列表和当前位置相关的字段。
 * 
 * @property photos 当前显示的照片列表
 * @property currentIndex 当前索引（卡片模式）
 * @property totalCount 总数量（用于进度显示）
 * @property isLoading 是否正在加载
 * @property isSyncing 是否正在同步
 * @property isReloading 是否正在重新加载（防止完成页面闪烁）
 */
data class SorterPhotosState(
    val photos: List<PhotoEntity> = emptyList(),
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isReloading: Boolean = false
) {
    /**
     * 当前照片
     */
    val currentPhoto: PhotoEntity?
        get() = photos.getOrNull(currentIndex)
    
    /**
     * 下一张照片（用于预加载）
     */
    val nextPhoto: PhotoEntity?
        get() = photos.getOrNull(currentIndex + 1)
    
    /**
     * 是否有照片
     */
    val hasPhotos: Boolean
        get() = photos.isNotEmpty()
    
    /**
     * 是否整理完成
     */
    val isComplete: Boolean
        get() = photos.isEmpty() || currentIndex >= photos.size
    
    /**
     * 剩余照片数量
     */
    val remainingCount: Int
        get() = (photos.size - currentIndex).coerceAtLeast(0)
    
    companion object {
        val EMPTY = SorterPhotosState()
    }
}

// ============== 计数器子状态 ==============

/**
 * 计数器子状态
 * 
 * 包含整理计数相关的字段。
 * 
 * @property sortedCount 已整理数量
 * @property keepCount 保留数量
 * @property trashCount 回收站数量
 * @property maybeCount 待定数量
 * @property combo 连击状态
 */
data class SorterCountersState(
    val sortedCount: Int = 0,
    val keepCount: Int = 0,
    val trashCount: Int = 0,
    val maybeCount: Int = 0,
    val combo: ComboState = ComboState()
) {
    /**
     * 进度（0.0 - 1.0）
     * 
     * @param totalCount 总数量
     */
    fun progress(totalCount: Int): Float =
        if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
    
    /**
     * 验证计数一致性
     */
    val isValid: Boolean
        get() = sortedCount == keepCount + trashCount + maybeCount
    
    companion object {
        val EMPTY = SorterCountersState()
    }
}

// ============== 配置子状态 ==============

/**
 * 配置子状态
 * 
 * 包含整理配置相关的字段。
 * 
 * @property viewMode 视图模式（卡片/列表）
 * @property sortOrder 排序方式
 * @property gridColumns 网格列数
 * @property filterMode 筛选模式
 * @property cameraAlbumsLoaded 相机相册是否加载完成
 * @property cardZoomEnabled 卡片缩放是否启用
 * @property swipeSensitivity 滑动灵敏度
 * @property hapticFeedbackEnabled 震动反馈是否启用
 */
data class SorterConfigState(
    val viewMode: FlowSorterViewMode = FlowSorterViewMode.CARD,
    val sortOrder: PhotoSortOrder = PhotoSortOrder.DATE_DESC,
    val gridColumns: Int = 2,
    val filterMode: PhotoFilterMode = PhotoFilterMode.ALL,
    val cameraAlbumsLoaded: Boolean = false,
    val cardZoomEnabled: Boolean = true,
    val swipeSensitivity: Float = 1.0f,
    val hapticFeedbackEnabled: Boolean = true
) {
    /**
     * 是否为卡片模式
     */
    val isCardMode: Boolean
        get() = viewMode == FlowSorterViewMode.CARD
    
    /**
     * 是否为列表模式
     */
    val isListMode: Boolean
        get() = viewMode == FlowSorterViewMode.LIST
    
    /**
     * 排序图标资源名称
     */
    val sortOrderIconName: String
        get() = when (sortOrder) {
            PhotoSortOrder.DATE_DESC -> "arrow_downward"
            PhotoSortOrder.DATE_ASC -> "arrow_upward"
            PhotoSortOrder.SIZE_DESC -> "storage"
            PhotoSortOrder.SIZE_ASC -> "storage"
            PhotoSortOrder.RANDOM -> "shuffle"
        }
    
    companion object {
        val EMPTY = SorterConfigState()
    }
}

// ============== 选择子状态 ==============

/**
 * 选择子状态
 * 
 * 包含照片选择相关的字段（列表模式使用）。
 * 
 * @property selectedPhotoIds 选中的照片 ID 集合
 */
data class SorterSelectionState(
    val selectedPhotoIds: Set<String> = emptySet()
) {
    /**
     * 是否处于选择模式
     */
    val isSelectionMode: Boolean
        get() = selectedPhotoIds.isNotEmpty()
    
    /**
     * 选中数量
     */
    val selectedCount: Int
        get() = selectedPhotoIds.size
    
    /**
     * 检查是否选中
     */
    fun isSelected(photoId: String): Boolean = selectedPhotoIds.contains(photoId)
    
    /**
     * 是否全选
     */
    fun isAllSelected(totalCount: Int): Boolean =
        totalCount > 0 && selectedCount == totalCount
    
    companion object {
        val EMPTY = SorterSelectionState()
    }
}

// ============== 相册模式子状态 ==============

/**
 * 相册模式子状态
 * 
 * 包含卡片整理时的相册分类相关字段。
 * 
 * @property enabled 是否启用相册模式
 * @property albumBubbleList 快捷相册列表
 * @property albumTagSize 相册标签大小
 * @property maxAlbumTagCount 最大标签显示数量
 * @property albumAddAction 添加到相册的操作类型（复制/移动）
 * @property albumMessage 相册操作消息
 */
data class SorterAlbumState(
    val enabled: Boolean = false,
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumTagSize: Float = 1.0f,
    val maxAlbumTagCount: Int = 0,
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val albumMessage: String? = null
) {
    /**
     * 是否有可用相册
     */
    val hasAlbums: Boolean
        get() = albumBubbleList.isNotEmpty()
    
    /**
     * 显示的相册数量（受限于 maxAlbumTagCount）
     */
    val displayedAlbumCount: Int
        get() = if (maxAlbumTagCount > 0) {
            minOf(albumBubbleList.size, maxAlbumTagCount)
        } else {
            albumBubbleList.size
        }
    
    /**
     * 是否有更多相册（未显示）
     */
    val hasMoreAlbums: Boolean
        get() = maxAlbumTagCount > 0 && albumBubbleList.size > maxAlbumTagCount
    
    companion object {
        val EMPTY = SorterAlbumState()
    }
}

// ============== 每日任务子状态 ==============

/**
 * 每日任务子状态
 * 
 * 包含每日任务模式相关的字段。
 * 
 * @property enabled 是否为每日任务模式
 * @property target 目标数量
 * @property current 当前完成数量
 * @property isComplete 是否完成
 */
data class SorterDailyTaskState(
    val enabled: Boolean = false,
    val target: Int = -1,
    val current: Int = 0,
    val isComplete: Boolean = false
) {
    /**
     * 进度（0.0 - 1.0）
     */
    val progress: Float
        get() = if (target > 0) current.toFloat() / target else 0f
    
    /**
     * 剩余数量
     */
    val remaining: Int
        get() = (target - current).coerceAtLeast(0)
    
    /**
     * 进度文本（如 "5/10"）
     */
    val progressText: String
        get() = "$current/$target"
    
    companion object {
        val EMPTY = SorterDailyTaskState()
        
        /**
         * 创建非每日任务状态
         */
        val DISABLED = SorterDailyTaskState(enabled = false)
    }
}

// ============== 权限对话框子状态 ==============

/**
 * 权限对话框子状态
 * 
 * 包含权限请求对话框相关的字段。
 * 
 * @property showDialog 是否显示对话框
 * @property retryError 是否显示重试错误
 * @property pendingAlbumBucketId 待处理的相册 ID
 */
data class SorterPermissionState(
    val showDialog: Boolean = false,
    val retryError: Boolean = false,
    val pendingAlbumBucketId: String? = null
) {
    /**
     * 是否有待处理的操作
     */
    val hasPendingOperation: Boolean
        get() = pendingAlbumBucketId != null
    
    companion object {
        val EMPTY = SorterPermissionState()
    }
}

// ============== 组合状态（供 Phase 4 阶段二使用） ==============

/**
 * 重构后的 FlowSorterUiState 结构预览
 * 
 * 此类用于 Phase 4 阶段二迁移，目前与现有 FlowSorterUiState 并存。
 */
data class FlowSorterUiStateV2(
    val photos: SorterPhotosState = SorterPhotosState.EMPTY,
    val counters: SorterCountersState = SorterCountersState.EMPTY,
    val config: SorterConfigState = SorterConfigState.EMPTY,
    val selection: SorterSelectionState = SorterSelectionState.EMPTY,
    val album: SorterAlbumState = SorterAlbumState.EMPTY,
    val dailyTask: SorterDailyTaskState = SorterDailyTaskState.EMPTY,
    val permission: SorterPermissionState = SorterPermissionState.EMPTY,
    val lastAction: SortAction? = null,
    val error: String? = null
) {
    // ============== 便捷访问器（保持与旧 API 兼容） ==============
    
    // Photos
    val currentPhoto: PhotoEntity? get() = photos.currentPhoto
    val nextPhoto: PhotoEntity? get() = photos.nextPhoto
    val hasPhotos: Boolean get() = photos.hasPhotos
    val isComplete: Boolean get() = photos.isComplete
    val isLoading: Boolean get() = photos.isLoading
    val isSyncing: Boolean get() = photos.isSyncing
    val isReloading: Boolean get() = photos.isReloading
    val totalCount: Int get() = photos.totalCount
    val currentIndex: Int get() = photos.currentIndex
    
    // Counters
    val sortedCount: Int get() = counters.sortedCount
    val keepCount: Int get() = counters.keepCount
    val trashCount: Int get() = counters.trashCount
    val maybeCount: Int get() = counters.maybeCount
    val combo: ComboState get() = counters.combo
    val progress: Float get() = counters.progress(photos.totalCount)
    
    // Config
    val viewMode: FlowSorterViewMode get() = config.viewMode
    val sortOrder: PhotoSortOrder get() = config.sortOrder
    val gridColumns: Int get() = config.gridColumns
    val filterMode: PhotoFilterMode get() = config.filterMode
    val cameraAlbumsLoaded: Boolean get() = config.cameraAlbumsLoaded
    val cardZoomEnabled: Boolean get() = config.cardZoomEnabled
    val swipeSensitivity: Float get() = config.swipeSensitivity
    val hapticFeedbackEnabled: Boolean get() = config.hapticFeedbackEnabled
    
    // Selection
    val selectedPhotoIds: Set<String> get() = selection.selectedPhotoIds
    val isSelectionMode: Boolean get() = selection.isSelectionMode
    val selectedCount: Int get() = selection.selectedCount
    
    // Album
    val cardSortingAlbumEnabled: Boolean get() = album.enabled
    val albumBubbleList: List<AlbumBubbleEntity> get() = album.albumBubbleList
    val albumTagSize: Float get() = album.albumTagSize
    val maxAlbumTagCount: Int get() = album.maxAlbumTagCount
    val albumAddAction: AlbumAddAction get() = album.albumAddAction
    val albumMessage: String? get() = album.albumMessage
    
    // Daily Task
    val isDailyTask: Boolean get() = dailyTask.enabled
    val dailyTaskTarget: Int get() = dailyTask.target
    val dailyTaskCurrent: Int get() = dailyTask.current
    val isDailyTaskComplete: Boolean get() = dailyTask.isComplete
    
    // Permission
    val showPermissionDialog: Boolean get() = permission.showDialog
    val permissionRetryError: Boolean get() = permission.retryError
    val pendingAlbumBucketId: String? get() = permission.pendingAlbumBucketId
    
    companion object {
        val EMPTY = FlowSorterUiStateV2()
    }
}

// ============== 从现有 FlowSorterUiState 转换的扩展函数 ==============

/**
 * 从现有 FlowSorterUiState 创建子状态
 */
fun FlowSorterUiState.toPhotosState(): SorterPhotosState = SorterPhotosState(
    photos = photos,
    currentIndex = currentIndex,
    totalCount = totalCount,
    isLoading = isLoading,
    isSyncing = isSyncing,
    isReloading = isReloading
)

fun FlowSorterUiState.toCountersState(): SorterCountersState = SorterCountersState(
    sortedCount = sortedCount,
    keepCount = keepCount,
    trashCount = trashCount,
    maybeCount = maybeCount,
    combo = combo
)

fun FlowSorterUiState.toConfigState(): SorterConfigState = SorterConfigState(
    viewMode = viewMode,
    sortOrder = sortOrder,
    gridColumns = gridColumns,
    filterMode = filterMode,
    cameraAlbumsLoaded = cameraAlbumsLoaded,
    cardZoomEnabled = cardZoomEnabled,
    swipeSensitivity = swipeSensitivity,
    hapticFeedbackEnabled = hapticFeedbackEnabled
)

fun FlowSorterUiState.toSelectionState(): SorterSelectionState = SorterSelectionState(
    selectedPhotoIds = selectedPhotoIds
)

fun FlowSorterUiState.toAlbumState(): SorterAlbumState = SorterAlbumState(
    enabled = cardSortingAlbumEnabled,
    albumBubbleList = albumBubbleList,
    albumTagSize = albumTagSize,
    maxAlbumTagCount = maxAlbumTagCount,
    albumAddAction = albumAddAction,
    albumMessage = albumMessage
)

fun FlowSorterUiState.toDailyTaskState(): SorterDailyTaskState = SorterDailyTaskState(
    enabled = isDailyTask,
    target = dailyTaskTarget,
    current = dailyTaskCurrent,
    isComplete = isDailyTaskComplete
)

fun FlowSorterUiState.toPermissionState(): SorterPermissionState = SorterPermissionState(
    showDialog = showPermissionDialog,
    retryError = permissionRetryError,
    pendingAlbumBucketId = pendingAlbumBucketId
)

/**
 * 转换为新版 UiState
 */
fun FlowSorterUiState.toV2(): FlowSorterUiStateV2 = FlowSorterUiStateV2(
    photos = toPhotosState(),
    counters = toCountersState(),
    config = toConfigState(),
    selection = toSelectionState(),
    album = toAlbumState(),
    dailyTask = toDailyTaskState(),
    permission = toPermissionState(),
    lastAction = lastAction,
    error = error
)
