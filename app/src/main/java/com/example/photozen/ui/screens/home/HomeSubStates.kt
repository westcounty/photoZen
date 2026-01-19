package com.example.photozen.ui.screens.home

import com.example.photozen.data.repository.AchievementData
import com.example.photozen.data.repository.PhotoClassificationMode
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.StatsSummary
import com.example.photozen.domain.usecase.DailyTaskStatus
import com.example.photozen.ui.state.AsyncState

/**
 * HomeUiState 子状态定义
 * 
 * 将原来 25+ 字段的 HomeUiState 拆分为 4 个职责清晰的子状态：
 * - PhotoStatsState: 照片统计相关
 * - UiControlState: UI 控制相关
 * - FeatureConfigState: 功能配置相关
 * - SmartGalleryState: 智能画廊相关（实验性功能）
 * 
 * ## 迁移策略
 * 
 * 这些子状态类与现有 HomeUiState 并存，不修改现有代码。
 * 后续阶段二会逐步迁移 HomeViewModel 使用这些新结构。
 * 
 * ## 便捷访问器
 * 
 * 每个子状态提供计算属性，便于 UI 层直接使用，无需额外计算。
 * 
 * @since Phase 4 - 状态管理重构
 */

// ============== 照片统计子状态 ==============

/**
 * 照片统计子状态
 * 
 * 包含所有与照片数量统计相关的字段。
 * 
 * @property totalPhotos 照片总数
 * @property unsortedCount 未整理数量
 * @property keepCount 保留数量
 * @property trashCount 回收站数量
 * @property maybeCount 待定数量
 * @property filteredTotal 筛选后总数
 * @property filteredSorted 筛选后已整理数
 */
data class PhotoStatsState(
    val totalPhotos: Int = 0,
    val unsortedCount: Int = 0,
    val keepCount: Int = 0,
    val trashCount: Int = 0,
    val maybeCount: Int = 0,
    val filteredTotal: Int = 0,
    val filteredSorted: Int = 0
) {
    /**
     * 已整理总数（保留 + 回收站 + 待定）
     */
    val sortedCount: Int
        get() = keepCount + trashCount + maybeCount
    
    /**
     * 整理进度（0.0 - 1.0）
     */
    val progress: Float
        get() = if (totalPhotos > 0) sortedCount.toFloat() / totalPhotos else 0f
    
    /**
     * 是否有照片
     */
    val hasPhotos: Boolean
        get() = totalPhotos > 0
    
    /**
     * 筛选后的整理进度
     */
    val filteredProgress: Float
        get() = if (filteredTotal > 0) filteredSorted.toFloat() / filteredTotal else 0f
    
    /**
     * 筛选后的未整理数量
     */
    val filteredUnsorted: Int
        get() = filteredTotal - filteredSorted
    
    /**
     * 是否全部整理完成
     */
    val isAllSorted: Boolean
        get() = totalPhotos > 0 && unsortedCount == 0
    
    /**
     * 整理进度百分比文本
     */
    val progressPercentText: String
        get() = "${(progress * 100).toInt()}%"
    
    companion object {
        val EMPTY = PhotoStatsState()
    }
}

// ============== UI 控制子状态 ==============

/**
 * UI 控制子状态
 * 
 * 包含所有与 UI 显示控制相关的字段。
 * 
 * @property hasPermission 是否有存储权限
 * @property syncState 同步状态（使用 AsyncState 替代多个字段）
 * @property shouldShowChangelog 是否显示更新日志
 * @property shouldShowQuickStart 是否显示快速入门
 * @property showSortModeSheet 是否显示整理模式选择
 */
data class UiControlState(
    val hasPermission: Boolean = false,
    val syncState: AsyncState<String?> = AsyncState.Idle,
    val shouldShowChangelog: Boolean = false,
    val shouldShowQuickStart: Boolean = false,
    val showSortModeSheet: Boolean = false
) {
    /**
     * 是否正在同步
     */
    val isSyncing: Boolean
        get() = syncState.isLoading
    
    /**
     * 是否正在加载（等待权限或同步中）
     */
    val isLoading: Boolean
        get() = !hasPermission || syncState.isLoading
    
    /**
     * 同步结果消息
     */
    val syncResult: String?
        get() = syncState.getOrNull()
    
    /**
     * 同步错误消息
     */
    val syncError: String?
        get() = syncState.errorOrNull()
    
    /**
     * 是否有任何对话框需要显示
     */
    val hasDialogToShow: Boolean
        get() = shouldShowChangelog || shouldShowQuickStart || showSortModeSheet
    
    companion object {
        val EMPTY = UiControlState()
    }
}

// ============== 功能配置子状态 ==============

/**
 * 功能配置子状态
 * 
 * 包含所有与功能配置相关的字段。
 * 
 * @property photoFilterMode 照片筛选模式
 * @property photoClassificationMode 照片分类模式
 * @property dailyTaskStatus 每日任务状态
 * @property achievementData 成就数据
 * @property statsSummary 统计摘要
 */
data class FeatureConfigState(
    val photoFilterMode: PhotoFilterMode = PhotoFilterMode.ALL,
    val photoClassificationMode: PhotoClassificationMode = PhotoClassificationMode.TAG,
    val dailyTaskStatus: DailyTaskStatus? = null,
    val achievementData: AchievementData = AchievementData(),
    val statsSummary: StatsSummary = StatsSummary.EMPTY
) {
    /**
     * 是否需要用户选择自定义筛选条件
     */
    val needsFilterSelection: Boolean
        get() = photoFilterMode == PhotoFilterMode.CUSTOM
    
    /**
     * 每日任务是否启用
     */
    val isDailyTaskEnabled: Boolean
        get() = dailyTaskStatus != null
    
    /**
     * 每日任务进度
     */
    val dailyTaskProgress: Float
        get() = dailyTaskStatus?.let { 
            if (it.target > 0) it.current.toFloat() / it.target else 0f 
        } ?: 0f
    
    /**
     * 每日任务是否完成
     */
    val isDailyTaskComplete: Boolean
        get() = dailyTaskStatus?.let { it.current >= it.target } ?: false
    
    /**
     * 每日任务剩余数量
     */
    val dailyTaskRemaining: Int
        get() = dailyTaskStatus?.let { (it.target - it.current).coerceAtLeast(0) } ?: 0
    
    companion object {
        val EMPTY = FeatureConfigState()
    }
}

// ============== 智能画廊子状态 ==============

/**
 * 智能画廊子状态（实验性功能）
 * 
 * 包含所有与智能画廊相关的字段。
 * 仅在 ENABLE_SMART_GALLERY 构建标志启用时有意义。
 * 
 * @property enabled 是否启用智能画廊
 * @property personCount 识别的人物数量
 * @property labelCount 识别的标签数量
 * @property gpsPhotoCount 带 GPS 信息的照片数量
 * @property analysisProgress 分析进度（0.0 - 1.0）
 * @property isAnalyzing 是否正在分析
 */
data class SmartGalleryState(
    val enabled: Boolean = false,
    val personCount: Int = 0,
    val labelCount: Int = 0,
    val gpsPhotoCount: Int = 0,
    val analysisProgress: Float = 0f,
    val isAnalyzing: Boolean = false
) {
    /**
     * 是否有分析数据
     */
    val hasAnalysisData: Boolean
        get() = personCount > 0 || labelCount > 0 || gpsPhotoCount > 0
    
    /**
     * 分析进度百分比文本
     */
    val analysisProgressText: String
        get() = "${(analysisProgress * 100).toInt()}%"
    
    /**
     * 是否分析完成
     */
    val isAnalysisComplete: Boolean
        get() = analysisProgress >= 1.0f && !isAnalyzing
    
    companion object {
        val EMPTY = SmartGalleryState()
    }
}

// ============== 组合状态（供 Phase 4 阶段二使用） ==============

/**
 * 重构后的 HomeUiState 结构预览
 * 
 * 此类用于 Phase 4 阶段二迁移，目前与现有 HomeUiState 并存。
 * 
 * @property photoStats 照片统计
 * @property uiControl UI 控制
 * @property featureConfig 功能配置
 * @property smartGallery 智能画廊
 */
data class HomeUiStateV2(
    val photoStats: PhotoStatsState = PhotoStatsState.EMPTY,
    val uiControl: UiControlState = UiControlState.EMPTY,
    val featureConfig: FeatureConfigState = FeatureConfigState.EMPTY,
    val smartGallery: SmartGalleryState = SmartGalleryState.EMPTY
) {
    // ============== 便捷访问器（保持与旧 API 兼容） ==============
    
    val totalPhotos: Int get() = photoStats.totalPhotos
    val unsortedCount: Int get() = photoStats.unsortedCount
    val keepCount: Int get() = photoStats.keepCount
    val trashCount: Int get() = photoStats.trashCount
    val maybeCount: Int get() = photoStats.maybeCount
    val sortedCount: Int get() = photoStats.sortedCount
    val progress: Float get() = photoStats.progress
    val hasPhotos: Boolean get() = photoStats.hasPhotos
    val filteredTotal: Int get() = photoStats.filteredTotal
    val filteredSorted: Int get() = photoStats.filteredSorted
    val filteredProgress: Float get() = photoStats.filteredProgress
    val filteredUnsorted: Int get() = photoStats.filteredUnsorted
    
    val hasPermission: Boolean get() = uiControl.hasPermission
    val isLoading: Boolean get() = uiControl.isLoading
    val isSyncing: Boolean get() = uiControl.isSyncing
    val syncResult: String? get() = uiControl.syncResult
    val error: String? get() = uiControl.syncError
    val shouldShowChangelog: Boolean get() = uiControl.shouldShowChangelog
    val shouldShowQuickStart: Boolean get() = uiControl.shouldShowQuickStart
    val showSortModeSheet: Boolean get() = uiControl.showSortModeSheet
    
    val photoFilterMode: PhotoFilterMode get() = featureConfig.photoFilterMode
    val photoClassificationMode: PhotoClassificationMode get() = featureConfig.photoClassificationMode
    val dailyTaskStatus: DailyTaskStatus? get() = featureConfig.dailyTaskStatus
    val achievementData: AchievementData get() = featureConfig.achievementData
    val statsSummary: StatsSummary get() = featureConfig.statsSummary
    val needsFilterSelection: Boolean get() = featureConfig.needsFilterSelection
    
    val experimentalEnabled: Boolean get() = smartGallery.enabled
    val smartGalleryPersonCount: Int get() = smartGallery.personCount
    val smartGalleryLabelCount: Int get() = smartGallery.labelCount
    val smartGalleryGpsPhotoCount: Int get() = smartGallery.gpsPhotoCount
    val smartGalleryAnalysisProgress: Float get() = smartGallery.analysisProgress
    val smartGalleryIsAnalyzing: Boolean get() = smartGallery.isAnalyzing
    
    companion object {
        val EMPTY = HomeUiStateV2()
    }
}

// ============== 从现有 HomeUiState 转换的扩展函数 ==============

/**
 * 从现有 HomeUiState 创建子状态
 * 
 * 用于渐进式迁移：可以在不修改现有代码的情况下，
 * 提取子状态用于新功能开发。
 */
fun HomeUiState.toPhotoStatsState(): PhotoStatsState = PhotoStatsState(
    totalPhotos = totalPhotos,
    unsortedCount = unsortedCount,
    keepCount = keepCount,
    trashCount = trashCount,
    maybeCount = maybeCount,
    filteredTotal = filteredTotal,
    filteredSorted = filteredSorted
)

fun HomeUiState.toUiControlState(): UiControlState = UiControlState(
    hasPermission = hasPermission,
    syncState = when {
        isSyncing -> AsyncState.Loading
        error != null -> AsyncState.Error(error)
        syncResult != null -> AsyncState.Success(syncResult)
        else -> AsyncState.Idle
    },
    shouldShowChangelog = shouldShowChangelog,
    shouldShowQuickStart = shouldShowQuickStart,
    showSortModeSheet = showSortModeSheet
)

fun HomeUiState.toFeatureConfigState(): FeatureConfigState = FeatureConfigState(
    photoFilterMode = photoFilterMode,
    photoClassificationMode = photoClassificationMode,
    dailyTaskStatus = dailyTaskStatus,
    achievementData = achievementData,
    statsSummary = statsSummary
)

fun HomeUiState.toSmartGalleryState(): SmartGalleryState = SmartGalleryState(
    enabled = experimentalEnabled,
    personCount = smartGalleryPersonCount,
    labelCount = smartGalleryLabelCount,
    gpsPhotoCount = smartGalleryGpsPhotoCount,
    analysisProgress = smartGalleryAnalysisProgress,
    isAnalyzing = smartGalleryIsAnalyzing
)

/**
 * 转换为新版 UiState
 */
fun HomeUiState.toV2(): HomeUiStateV2 = HomeUiStateV2(
    photoStats = toPhotoStatsState(),
    uiControl = toUiControlState(),
    featureConfig = toFeatureConfigState(),
    smartGallery = toSmartGalleryState()
)
