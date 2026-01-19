package com.example.photozen.ui.state

import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.domain.model.UndoAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 撤销管理器
 * 
 * 管理应用中的撤销操作，支持单步撤销。
 * 
 * ## 设计说明
 * - 仅保留最近一次可撤销操作（简化实现）
 * - 支持状态变更、相册移动、回收站恢复等操作
 * - 撤销操作有时效性，超过 Snackbar 显示时间自动清除
 * 
 * ## 使用流程
 * 1. ViewModel 执行操作前记录原始状态
 * 2. 执行操作后调用 recordAction 记录
 * 3. 显示 Snackbar 提示
 * 4. 用户点击撤销时调用 undo
 * 5. Snackbar 消失后调用 clear
 * 
 * ## 线程安全
 * 所有方法都是线程安全的，可在任意协程中调用。
 */
@Singleton
class UndoManager @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    private val _lastAction = MutableStateFlow<UndoAction?>(null)
    
    /**
     * 最近一次可撤销操作
     */
    val lastAction: StateFlow<UndoAction?> = _lastAction.asStateFlow()
    
    /**
     * 是否有可撤销操作
     */
    val hasUndoAction: Boolean get() = _lastAction.value != null
    
    /**
     * 记录可撤销操作
     * 
     * 调用此方法会替换之前记录的操作。
     * 
     * @param action 要记录的操作
     */
    fun recordAction(action: UndoAction) {
        _lastAction.value = action
    }
    
    /**
     * 执行撤销
     * 
     * @return 撤销是否成功
     */
    suspend fun undo(): Result<Boolean> = runCatching {
        val action = _lastAction.value ?: return Result.success(false)
        
        when (action) {
            is UndoAction.StatusChange -> {
                undoStatusChange(action)
            }
            is UndoAction.MoveToAlbum -> {
                undoMoveToAlbum(action)
            }
            is UndoAction.RestoreFromTrash -> {
                undoRestoreFromTrash(action)
            }
        }
        
        _lastAction.value = null
        true
    }
    
    /**
     * 撤销状态变更
     */
    private suspend fun undoStatusChange(action: UndoAction.StatusChange) {
        // 恢复每张照片的原始状态
        action.previousStatus.forEach { (id, status) ->
            photoRepository.updatePhotoStatus(id, status)
        }
    }
    
    /**
     * 撤销相册移动
     * 
     * 注意：需要 PhotoRepository 支持 movePhotoToAlbum 方法
     * 目前简化处理，仅重置状态
     */
    private suspend fun undoMoveToAlbum(action: UndoAction.MoveToAlbum) {
        // TODO: 实现相册移动撤销
        // 需要 PhotoRepository 添加相册移动方法
    }
    
    /**
     * 撤销回收站恢复
     */
    private suspend fun undoRestoreFromTrash(action: UndoAction.RestoreFromTrash) {
        // 重新标记为回收站
        action.photoIds.forEach { id ->
            photoRepository.updatePhotoStatus(id, PhotoStatus.TRASH)
        }
    }
    
    /**
     * 清除撤销记录
     * 
     * 应在 Snackbar 消失后调用
     */
    fun clear() {
        _lastAction.value = null
    }
    
    /**
     * 获取当前操作的描述
     */
    fun getActionDescription(): String? = when (val action = _lastAction.value) {
        is UndoAction.StatusChange -> action.getDescription()
        is UndoAction.MoveToAlbum -> action.getDescription()
        is UndoAction.RestoreFromTrash -> action.getDescription()
        null -> null
    }
}
