package com.example.photozen.domain.model

import com.example.photozen.data.model.PhotoStatus

/**
 * 可撤销的操作
 * 
 * 使用密封类表示不同类型的可撤销操作。
 * 每种操作类型包含撤销所需的全部信息。
 * 
 * ## 支持的操作类型
 * - 状态变更：保留、删除、待定、重置
 * - 移动相册：将照片移动到另一个相册
 * - 从回收站恢复
 * 
 * ## 使用示例
 * ```kotlin
 * val action = UndoAction.StatusChange(
 *     photoIds = listOf("id1", "id2"),
 *     previousStatus = mapOf("id1" to UNSORTED, "id2" to KEEP),
 *     newStatus = TRASH
 * )
 * ```
 */
sealed class UndoAction {
    /**
     * 状态变更操作
     * 
     * @property photoIds 受影响的照片 ID 列表
     * @property previousStatus 每张照片的原始状态映射
     * @property newStatus 变更后的新状态
     */
    data class StatusChange(
        val photoIds: List<String>,
        val previousStatus: Map<String, PhotoStatus>,
        val newStatus: PhotoStatus
    ) : UndoAction() {
        val affectedCount: Int get() = photoIds.size
        
        /**
         * 生成撤销描述
         */
        fun getDescription(): String = when (newStatus) {
            PhotoStatus.TRASH -> "已删除 $affectedCount 张照片"
            PhotoStatus.KEEP -> "已保留 $affectedCount 张照片"
            PhotoStatus.MAYBE -> "已标记 $affectedCount 张照片为待定"
            PhotoStatus.UNSORTED -> "已重置 $affectedCount 张照片"
        }
    }
    
    /**
     * 移动相册操作
     * 
     * @property photoIds 受影响的照片 ID 列表
     * @property fromAlbumId 原相册 ID（null 表示未分类）
     * @property toAlbumId 目标相册 ID
     */
    data class MoveToAlbum(
        val photoIds: List<String>,
        val fromAlbumId: String?,
        val toAlbumId: String
    ) : UndoAction() {
        val affectedCount: Int get() = photoIds.size
        
        fun getDescription(): String = "已移动 $affectedCount 张照片"
    }
    
    /**
     * 从回收站恢复操作
     *
     * @property photoIds 恢复的照片 ID 列表
     * @property previousStatus 原始状态（恢复前的状态，即恢复到哪个状态）
     */
    data class RestoreFromTrash(
        val photoIds: List<String>,
        val previousStatus: Map<String, PhotoStatus>
    ) : UndoAction() {
        val affectedCount: Int get() = photoIds.size

        fun getDescription(): String = "已恢复 $affectedCount 张照片"
    }

    // ============== REQ-060: 新增类型 ==============

    /**
     * 筛选操作: 用于 FlowSorter 的单张照片筛选
     * 替代原有的 FlowSorterViewModel 内部 SortAction
     *
     * @property photoId 照片 ID
     * @property previousStatus 操作前的状态
     * @property newStatus 变更后的新状态
     */
    data class SortPhoto(
        val photoId: String,
        val previousStatus: PhotoStatus,
        val newStatus: PhotoStatus
    ) : UndoAction() {
        fun getDescription(): String = when (newStatus) {
            PhotoStatus.KEEP -> "已保留照片"
            PhotoStatus.TRASH -> "已移入回收站"
            PhotoStatus.MAYBE -> "已标记为待定"
            PhotoStatus.UNSORTED -> "已重置照片"
        }
    }

    /**
     * 相册操作类型
     */
    enum class AlbumOperationType {
        COPY,   // 复制到相册
        MOVE    // 移动到相册
    }

    /**
     * 相册操作 - 统一处理复制和移动
     *
     * @property photoId 照片 ID
     * @property targetAlbumId 目标相册 ID
     * @property operationType 操作类型（复制/移动）
     * @property sourceAlbumId 源相册 ID（移动操作时使用）
     * @property createdFilePath 复制操作创建的新文件路径（用于撤销时删除）
     * @property previousStatus 操作前的照片状态
     */
    data class AlbumOperation(
        val photoId: String,
        val targetAlbumId: String,
        val operationType: AlbumOperationType,
        val sourceAlbumId: String? = null,
        val createdFilePath: String? = null,
        val previousStatus: PhotoStatus
    ) : UndoAction() {
        fun getDescription(): String = when (operationType) {
            AlbumOperationType.COPY -> "已复制照片到相册"
            AlbumOperationType.MOVE -> "已移动照片到相册"
        }
    }

    /**
     * 复合操作: 标记保留 + 添加到相册
     * 用于 FlowSorter 的 keepAndAddToAlbum
     *
     * @property photoId 照片 ID
     * @property albumId 目标相册 ID
     * @property previousStatus 操作前的状态
     * @property operationType 相册操作类型（复制/移动）
     * @property sourceAlbumId 源相册 ID
     * @property createdFilePath 复制操作创建的新文件路径
     */
    data class KeepAndAddToAlbum(
        val photoId: String,
        val albumId: String,
        val previousStatus: PhotoStatus,
        val operationType: AlbumOperationType,
        val sourceAlbumId: String? = null,
        val createdFilePath: String? = null
    ) : UndoAction() {
        fun getDescription(): String = "已保留并添加到相册"
    }
}
