package com.example.photozen.ui.state

/**
 * 可选择状态接口
 * 
 * 定义照片列表页面的通用选择状态。
 * 实现此接口的 UiState 可以与 SelectionTopBar 配合使用。
 * 
 * ## 使用示例
 * ```kotlin
 * data class PhotoListUiState(
 *     val photos: List<PhotoEntity> = emptyList(),
 *     val isSelectionMode: Boolean = false,
 *     val selectedIds: Set<String> = emptySet()
 * ) : SelectableState {
 *     override val selectableItemCount: Int get() = photos.size
 * }
 * ```
 */
interface SelectableState {
    /** 是否处于选择模式 */
    val isSelectionMode: Boolean
    
    /** 已选择的项目 ID 集合 */
    val selectedIds: Set<String>
    
    /** 可选择的项目总数 */
    val selectableItemCount: Int
    
    /** 已选择的数量 */
    val selectedCount: Int get() = selectedIds.size
    
    /** 是否已全选 */
    val isAllSelected: Boolean get() = selectedCount == selectableItemCount && selectableItemCount > 0
}
