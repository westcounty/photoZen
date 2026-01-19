package com.example.photozen.domain.usecase

import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.repository.StatsRepository
import com.example.photozen.domain.model.UndoAction
import com.example.photozen.ui.state.SnackbarManager
import com.example.photozen.ui.state.UndoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 照片批量操作 UseCase
 * 
 * 统一处理照片的批量操作，包括：
 * - 状态批量更新（保留/删除/待定/重置）
 * - 撤销支持
 * - Snackbar 反馈
 * - 统计记录
 * 
 * ## 设计目的
 * 
 * 解决批量操作逻辑分散在各 ViewModel 中的问题，提供统一的批量操作接口，
 * 确保：
 * - 操作逻辑一致性
 * - 撤销功能正确性
 * - 统计数据准确性
 * - 用户反馈及时性
 * 
 * ## 使用示例
 * 
 * ```kotlin
 * @HiltViewModel
 * class PhotoListViewModel @Inject constructor(
 *     private val batchOperationUseCase: PhotoBatchOperationUseCase,
 *     val selectionStateHolder: PhotoSelectionStateHolder
 * ) : ViewModel() {
 *     
 *     fun markSelectedAsKeep() {
 *         viewModelScope.launch {
 *             val ids = selectionStateHolder.getSelectedList()
 *             batchOperationUseCase.batchUpdateStatus(ids, PhotoStatus.KEEP)
 *             selectionStateHolder.clear()
 *         }
 *     }
 * }
 * ```
 * 
 * @since Phase 4 - 代码复用与组件化
 */
class PhotoBatchOperationUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val undoManager: UndoManager,
    private val snackbarManager: SnackbarManager,
    private val preferencesRepository: PreferencesRepository,
    private val statsRepository: StatsRepository
) {
    
    /**
     * 批量更新照片状态
     * 
     * @param photoIds 照片 ID 列表
     * @param newStatus 新状态
     * @param showUndo 是否显示撤销按钮（默认 true）
     * @param showSnackbar 是否显示 Snackbar（默认 true）
     * @return 操作结果，包含成功更新的数量
     */
    suspend fun batchUpdateStatus(
        photoIds: List<String>,
        newStatus: PhotoStatus,
        showUndo: Boolean = true,
        showSnackbar: Boolean = true
    ): Result<BatchOperationResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (photoIds.isEmpty()) {
                return@runCatching BatchOperationResult(0)
            }
            
            // 1. 记录原状态（用于撤销）
            val previousStates = if (showUndo) {
                photoIds.associateWith { id ->
                    photoRepository.getPhotoById(id)?.status ?: PhotoStatus.UNSORTED
                }
            } else {
                emptyMap()
            }
            
            // 2. 执行批量更新
            photoRepository.updatePhotoStatusBatch(photoIds, newStatus)
            
            // 3. 记录撤销操作
            if (showUndo && previousStates.isNotEmpty()) {
                undoManager.recordAction(
                    UndoAction.StatusChange(
                        photoIds = photoIds,
                        previousStatus = previousStates,
                        newStatus = newStatus
                    )
                )
            }
            
            // 4. 更新统计
            val counts = countStatusChanges(previousStates.values.toList(), newStatus)
            recordStats(counts)
            
            // 5. 更新成就计数（仅统计从 UNSORTED 变化的）
            if (counts.fromUnsorted > 0) {
                preferencesRepository.incrementSortedCount(counts.fromUnsorted)
            }
            
            // 6. 显示反馈
            if (showSnackbar) {
                val message = buildStatusChangeMessage(photoIds.size, newStatus)
                if (showUndo) {
                    snackbarManager.showSuccessWithUndo(message) {
                        // 撤销回调
                        kotlinx.coroutines.runBlocking {
                            undoManager.undo()
                        }
                    }
                } else {
                    snackbarManager.showSuccess(message)
                }
            }
            
            BatchOperationResult(
                affectedCount = photoIds.size,
                newStatus = newStatus,
                previousStates = previousStates
            )
        }
    }
    
    /**
     * 批量标记为保留
     */
    suspend fun batchKeep(
        photoIds: List<String>,
        showUndo: Boolean = true
    ): Result<BatchOperationResult> = batchUpdateStatus(photoIds, PhotoStatus.KEEP, showUndo)
    
    /**
     * 批量标记为待定
     */
    suspend fun batchMaybe(
        photoIds: List<String>,
        showUndo: Boolean = true
    ): Result<BatchOperationResult> = batchUpdateStatus(photoIds, PhotoStatus.MAYBE, showUndo)
    
    /**
     * 批量移入回收站
     */
    suspend fun batchTrash(
        photoIds: List<String>,
        showUndo: Boolean = true
    ): Result<BatchOperationResult> = batchUpdateStatus(photoIds, PhotoStatus.TRASH, showUndo)
    
    /**
     * 批量重置为未整理
     */
    suspend fun batchReset(
        photoIds: List<String>,
        showUndo: Boolean = true
    ): Result<BatchOperationResult> = batchUpdateStatus(photoIds, PhotoStatus.UNSORTED, showUndo)
    
    /**
     * 批量永久删除
     * 
     * 注意：永久删除不可撤销
     * 
     * @param photoIds 照片 ID 列表
     * @param confirmMessage 是否显示确认消息
     * @return 操作结果
     */
    suspend fun batchPermanentDelete(
        photoIds: List<String>,
        confirmMessage: Boolean = true
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (photoIds.isEmpty()) {
                return@runCatching 0
            }
            
            // 执行永久删除
            photoRepository.deletePhotosByIds(photoIds)
            val deletedCount = photoIds.size
            
            // 更新成就
            if (deletedCount > 0) {
                preferencesRepository.incrementTrashEmptied()
            }
            
            // 显示反馈
            if (confirmMessage) {
                snackbarManager.showSuccess("已永久删除 $deletedCount 张照片")
            }
            
            deletedCount
        }
    }
    
    /**
     * 批量从回收站恢复
     * 
     * @param photoIds 照片 ID 列表
     * @param targetStatus 恢复到的目标状态（默认 KEEP）
     * @return 操作结果
     */
    suspend fun batchRestoreFromTrash(
        photoIds: List<String>,
        targetStatus: PhotoStatus = PhotoStatus.KEEP
    ): Result<BatchOperationResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (photoIds.isEmpty()) {
                return@runCatching BatchOperationResult(0)
            }
            
            // 记录原状态
            val previousStates = photoIds.associateWith { PhotoStatus.TRASH }
            
            // 执行恢复
            photoRepository.updatePhotoStatusBatch(photoIds, targetStatus)
            
            // 记录撤销
            undoManager.recordAction(
                UndoAction.RestoreFromTrash(
                    photoIds = photoIds,
                    previousStatus = previousStates
                )
            )
            
            // 显示反馈
            snackbarManager.showSuccessWithUndo("已恢复 ${photoIds.size} 张照片") {
                kotlinx.coroutines.runBlocking {
                    undoManager.undo()
                }
            }
            
            BatchOperationResult(
                affectedCount = photoIds.size,
                newStatus = targetStatus,
                previousStates = previousStates
            )
        }
    }
    
    // ============== 私有辅助方法 ==============
    
    /**
     * 统计状态变化
     */
    private fun countStatusChanges(
        previousStates: List<PhotoStatus>,
        newStatus: PhotoStatus
    ): StatusChangeCounts {
        val fromUnsorted = previousStates.count { it == PhotoStatus.UNSORTED }
        
        return StatusChangeCounts(
            total = previousStates.size,
            fromUnsorted = fromUnsorted,
            toKeep = if (newStatus == PhotoStatus.KEEP) fromUnsorted else 0,
            toTrash = if (newStatus == PhotoStatus.TRASH) fromUnsorted else 0,
            toMaybe = if (newStatus == PhotoStatus.MAYBE) fromUnsorted else 0
        )
    }
    
    /**
     * 记录统计数据
     */
    private suspend fun recordStats(counts: StatusChangeCounts) {
        if (counts.toKeep > 0) {
            statsRepository.recordKeep(counts.toKeep)
        }
        if (counts.toTrash > 0) {
            statsRepository.recordTrash(counts.toTrash)
        }
        if (counts.toMaybe > 0) {
            statsRepository.recordMaybe(counts.toMaybe)
        }
    }
    
    /**
     * 构建状态变化消息
     */
    private fun buildStatusChangeMessage(count: Int, status: PhotoStatus): String {
        val statusLabel = when (status) {
            PhotoStatus.KEEP -> "保留"
            PhotoStatus.TRASH -> "回收站"
            PhotoStatus.MAYBE -> "待定"
            PhotoStatus.UNSORTED -> "未整理"
        }
        return "已将 $count 张照片标记为$statusLabel"
    }
    
    /**
     * 状态变化计数
     */
    private data class StatusChangeCounts(
        val total: Int,
        val fromUnsorted: Int,
        val toKeep: Int,
        val toTrash: Int,
        val toMaybe: Int
    )
}

/**
 * 批量操作结果
 */
data class BatchOperationResult(
    val affectedCount: Int,
    val newStatus: PhotoStatus? = null,
    val previousStates: Map<String, PhotoStatus> = emptyMap()
) {
    val isSuccess: Boolean get() = affectedCount > 0
    val isEmpty: Boolean get() = affectedCount == 0
}
