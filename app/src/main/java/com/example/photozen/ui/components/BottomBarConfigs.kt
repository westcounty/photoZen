package com.example.photozen.ui.components

import androidx.compose.runtime.Composable

/**
 * 底栏按钮组合配置
 * 
 * 预定义各场景下的按钮组合，确保 UI 一致性，减少重复代码。
 * 
 * ## 设计原则
 * 
 * 1. **场景化配置**：每个页面场景有预定义的按钮组合
 * 2. **单选/多选区分**：单选和多选模式可能显示不同的按钮
 * 3. **自适应切换**：提供 adaptive 方法根据选择数量自动切换配置
 * 
 * ## 使用示例
 * 
 * ```kotlin
 * // 在 Screen 中使用
 * val actions = BottomBarConfigs.adaptive(
 *     selectedCount = selectedCount,
 *     singleSelectActions = { BottomBarConfigs.keepListSingleSelect(...) },
 *     multiSelectActions = { BottomBarConfigs.keepListMultiSelect(...) }
 * )
 * SelectionBottomBar(actions = actions)
 * 
 * // 或直接使用预定义配置
 * val actions = BottomBarConfigs.trashListMultiSelect(
 *     onKeep = { viewModel.restoreSelected() },
 *     onMaybe = { viewModel.markAsMaybe() },
 *     onReset = { viewModel.resetSelected() },
 *     onPermanentDelete = { showDeleteConfirm = true }
 * )
 * ```
 * 
 * @since Phase 4 - 代码复用与组件化
 */
object BottomBarConfigs {
    
    // ============== 保留列表 (PhotoListScreen - KEEP) ==============
    
    /**
     * 保留列表 - 单选模式
     *
     * 显示所有可用操作：编辑、分享、相册、待定、回收站、重置
     */
    @Composable
    fun keepListSingleSelect(
        onEdit: () -> Unit,
        onShare: () -> Unit,
        onAlbum: () -> Unit,
        onMaybe: () -> Unit,
        onTrash: () -> Unit,
        onReset: () -> Unit
    ): List<BottomBarAction> = listOf(
        editAction(onEdit),
        shareAction(onShare),
        albumAction(onAlbum),
        maybeAction(onMaybe),
        deleteAction(onTrash),
        resetAction(onReset)
    )

    /**
     * 保留列表 - 多选模式 (REQ-041)
     *
     * 5项操作：添加到相册、设置为待定、移至回收站、重置为未筛选、彻底删除
     */
    @Composable
    fun keepListMultiSelect(
        onAlbum: () -> Unit,
        onMaybe: () -> Unit,
        onTrash: () -> Unit,
        onReset: () -> Unit,
        onPermanentDelete: () -> Unit
    ): List<BottomBarAction> = listOf(
        albumAction(onAlbum),
        maybeAction(onMaybe),
        deleteAction(onTrash),
        resetAction(onReset),
        permanentDeleteAction(onPermanentDelete)
    )
    
    // ============== 待定列表 (PhotoListScreen - MAYBE) ==============

    /**
     * 待定列表 - 单选模式
     */
    @Composable
    fun maybeListSingleSelect(
        onEdit: () -> Unit,
        onShare: () -> Unit,
        onKeep: () -> Unit,
        onTrash: () -> Unit,
        onReset: () -> Unit
    ): List<BottomBarAction> = listOf(
        editAction(onEdit),
        shareAction(onShare),
        keepAction(onKeep),
        deleteAction(onTrash),
        resetAction(onReset)
    )

    /**
     * 待定列表 - 多选模式
     */
    @Composable
    fun maybeListMultiSelect(
        onKeep: () -> Unit,
        onTrash: () -> Unit,
        onReset: () -> Unit
    ): List<BottomBarAction> = listOf(
        keepAction(onKeep),
        deleteAction(onTrash),
        resetAction(onReset)
    )

    /**
     * 待定列表 - 对比选择模式 (REQ-031)
     *
     * 用于待定列表中的照片对比选择场景：
     * - 清除：取消所有选中
     * - 对比：进入对比模式（仅在选中2-6张时可用）
     *
     * @param selectedCount 当前选中数量
     * @param onClear 清除选中回调
     * @param onCompare 进入对比模式回调
     */
    @Composable
    fun maybeListCompareSelect(
        selectedCount: Int,
        onClear: () -> Unit,
        onCompare: () -> Unit
    ): List<BottomBarAction> = listOf(
        clearAction(onClear),
        compareAction(onCompare, enabled = selectedCount in 2..6)
    )
    
    // ============== 回收站列表 (TrashScreen) ==============
    
    /**
     * 回收站列表 - 单选模式
     */
    @Composable
    fun trashListSingleSelect(
        onKeep: () -> Unit,
        onMaybe: () -> Unit,
        onReset: () -> Unit,
        onPermanentDelete: () -> Unit
    ): List<BottomBarAction> = listOf(
        keepAction(onKeep),
        maybeAction(onMaybe),
        resetAction(onReset),
        permanentDeleteAction(onPermanentDelete)
    )
    
    /**
     * 回收站列表 - 多选模式
     */
    @Composable
    fun trashListMultiSelect(
        onKeep: () -> Unit,
        onMaybe: () -> Unit,
        onReset: () -> Unit,
        onPermanentDelete: () -> Unit
    ): List<BottomBarAction> = listOf(
        keepAction(onKeep),
        maybeAction(onMaybe),
        resetAction(onReset),
        permanentDeleteAction(onPermanentDelete)
    )
    
    // ============== 相册照片列表 (AlbumPhotoListScreen) ==============
    
    /**
     * 相册照片列表 - 单选模式 (REQ-047)
     *
     * 操作: 编辑、分享、添加到其他相册、批量修改筛选状态、复制、从此开始筛选、彻底删除
     */
    @Composable
    fun albumPhotosSingleSelect(
        onEdit: () -> Unit,
        onShare: () -> Unit,
        onAddToOtherAlbum: () -> Unit,
        onBatchChangeStatus: () -> Unit,
        onCopy: () -> Unit,
        onStartFromHere: () -> Unit,
        onDelete: () -> Unit
    ): List<BottomBarAction> = listOf(
        editAction(onEdit),
        shareAction(onShare),
        albumAction(onAddToOtherAlbum),
        changeStatusAction(onBatchChangeStatus),
        copyAction(onCopy),
        startFromHereAction(onStartFromHere),
        permanentDeleteAction(onDelete)
    )

    /**
     * 相册照片列表 - 多选模式 (REQ-047)
     *
     * 操作: 添加到其他相册、批量修改筛选状态、复制照片、彻底删除
     */
    @Composable
    fun albumPhotosMultiSelect(
        onAddToOtherAlbum: () -> Unit,
        onBatchChangeStatus: () -> Unit,
        onCopy: () -> Unit,
        onDelete: () -> Unit
    ): List<BottomBarAction> = listOf(
        albumAction(onAddToOtherAlbum),
        changeStatusAction(onBatchChangeStatus),
        copyAction(onCopy),
        permanentDeleteAction(onDelete)
    )
    
    // ============== 时间线 (TimelineScreen) ==============
    
    /**
     * 时间线 - 单选模式
     */
    @Composable
    fun timelineSingleSelect(
        onEdit: () -> Unit,
        onShare: () -> Unit,
        onKeep: () -> Unit,
        onMaybe: () -> Unit,
        onTrash: () -> Unit
    ): List<BottomBarAction> = listOf(
        editAction(onEdit),
        shareAction(onShare),
        keepAction(onKeep),
        maybeAction(onMaybe),
        deleteAction(onTrash)
    )
    
    /**
     * 时间线 - 多选模式
     */
    @Composable
    fun timelineMultiSelect(
        onKeep: () -> Unit,
        onMaybe: () -> Unit,
        onTrash: () -> Unit
    ): List<BottomBarAction> = listOf(
        keepAction(onKeep),
        maybeAction(onMaybe),
        deleteAction(onTrash)
    )
    
    // ============== Light Table (LightTableScreen) ==============
    
    /**
     * Light Table 比较模式
     */
    @Composable
    fun lightTableCompare(
        onKeep: () -> Unit,
        onDiscard: () -> Unit
    ): List<BottomBarAction> = listOf(
        keepAction(onKeep),
        discardAction(onDiscard)
    )
    
    // ============== 滑动整理列表模式 (FlowSorterScreen - LIST) ==============
    
    /**
     * 滑动整理列表模式 - 单选
     */
    @Composable
    fun flowSorterListSingleSelect(
        onEdit: () -> Unit,
        onShare: () -> Unit,
        onKeep: () -> Unit,
        onMaybe: () -> Unit,
        onTrash: () -> Unit
    ): List<BottomBarAction> = listOf(
        editAction(onEdit),
        shareAction(onShare),
        keepAction(onKeep),
        maybeAction(onMaybe),
        deleteAction(onTrash)
    )
    
    /**
     * 滑动整理列表模式 - 多选
     */
    @Composable
    fun flowSorterListMultiSelect(
        onKeep: () -> Unit,
        onMaybe: () -> Unit,
        onTrash: () -> Unit
    ): List<BottomBarAction> = listOf(
        keepAction(onKeep),
        maybeAction(onMaybe),
        deleteAction(onTrash)
    )
    
    // ============== Workflow 阶段 (WorkflowScreen) ==============
    
    /**
     * 工作流 - 分类阶段
     */
    @Composable
    fun workflowClassify(
        onAlbum: () -> Unit,
        onSkip: () -> Unit,
        onKeep: () -> Unit
    ): List<BottomBarAction> = listOf(
        albumAction(onAlbum),
        resetAction(onSkip),
        keepAction(onKeep)
    )
    
    /**
     * 工作流 - 清理阶段
     */
    @Composable
    fun workflowTrash(
        onRestore: () -> Unit,
        onPermanentDelete: () -> Unit
    ): List<BottomBarAction> = listOf(
        keepAction(onRestore),
        permanentDeleteAction(onPermanentDelete)
    )
    
    // ============== 自适应配置 ==============
    
    /**
     * 根据选择数量自动选择配置
     * 
     * @param selectedCount 选中数量
     * @param singleSelectActions 单选时的按钮配置
     * @param multiSelectActions 多选时的按钮配置
     * @return 对应的按钮列表
     */
    @Composable
    fun adaptive(
        selectedCount: Int,
        singleSelectActions: @Composable () -> List<BottomBarAction>,
        multiSelectActions: @Composable () -> List<BottomBarAction>
    ): List<BottomBarAction> {
        return if (selectedCount == 1) {
            singleSelectActions()
        } else {
            multiSelectActions()
        }
    }
    
    /**
     * 根据选择数量自动选择配置（带空选择处理）
     * 
     * @param selectedCount 选中数量
     * @param emptyActions 无选择时的按钮配置（可选）
     * @param singleSelectActions 单选时的按钮配置
     * @param multiSelectActions 多选时的按钮配置
     * @return 对应的按钮列表
     */
    @Composable
    fun adaptiveWithEmpty(
        selectedCount: Int,
        emptyActions: (@Composable () -> List<BottomBarAction>)? = null,
        singleSelectActions: @Composable () -> List<BottomBarAction>,
        multiSelectActions: @Composable () -> List<BottomBarAction>
    ): List<BottomBarAction> {
        return when {
            selectedCount == 0 -> emptyActions?.invoke() ?: emptyList()
            selectedCount == 1 -> singleSelectActions()
            else -> multiSelectActions()
        }
    }
}

// ============== 便捷扩展函数 ==============

/**
 * 筛选启用的按钮
 */
fun List<BottomBarAction>.filterEnabled(): List<BottomBarAction> =
    filter { it.enabled }

/**
 * 设置所有按钮的启用状态
 */
fun List<BottomBarAction>.setAllEnabled(enabled: Boolean): List<BottomBarAction> =
    map { it.copy(enabled = enabled) }

/**
 * 按标签查找按钮
 */
fun List<BottomBarAction>.findByLabel(label: String): BottomBarAction? =
    find { it.label == label }

/**
 * 替换指定标签的按钮
 */
fun List<BottomBarAction>.replaceByLabel(
    label: String,
    replacement: BottomBarAction
): List<BottomBarAction> = map { 
    if (it.label == label) replacement else it 
}
