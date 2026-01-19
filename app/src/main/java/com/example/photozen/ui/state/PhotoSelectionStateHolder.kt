package com.example.photozen.ui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 照片选择状态持有者
 * 
 * 在多个 ViewModel 间共享选择状态，避免重复定义。
 * 使用 @Singleton 确保全局唯一实例。
 * 
 * ## 设计目的
 * 
 * 解决多个页面（PhotoListScreen、TrashScreen、AlbumPhotoListScreen 等）
 * 都需要照片选择功能，导致选择状态代码在各 ViewModel 中重复定义的问题。
 * 
 * ## 使用方式
 * 
 * ```kotlin
 * // 在 ViewModel 中注入
 * @HiltViewModel
 * class PhotoListViewModel @Inject constructor(
 *     val selectionStateHolder: PhotoSelectionStateHolder
 * ) : ViewModel() {
 *     
 *     // 直接使用 selectionStateHolder 的方法和状态
 *     fun onPhotoLongPress(photoId: String) {
 *         selectionStateHolder.toggle(photoId)
 *     }
 *     
 *     fun onSelectAll(allIds: List<String>) {
 *         selectionStateHolder.selectAll(allIds)
 *     }
 * }
 * 
 * // 在 Composable 中观察状态
 * @Composable
 * fun PhotoListScreen(viewModel: PhotoListViewModel) {
 *     val selectedIds by viewModel.selectionStateHolder.selectedIds.collectAsState()
 *     val isSelectionMode by viewModel.selectionStateHolder.isSelectionMode.collectAsState()
 *     // ...
 * }
 * ```
 * 
 * ## 注意事项
 * 
 * - 页面离开时应调用 `clear()` 清空选择状态
 * - 批量操作完成后应调用 `clear()` 退出选择模式
 * - 此类是全局单例，但每个页面通常在进入时重置状态
 * 
 * @since Phase 4 - 状态管理重构
 */
@Singleton
class PhotoSelectionStateHolder @Inject constructor() {
    
    // 用于 StateFlow 的作用域
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * 当前选中的照片 ID 集合
     */
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()
    
    /**
     * 是否处于选择模式（有任何选中的照片）
     */
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    
    /**
     * 选中的数量
     * 
     * 使用 stateIn 将 map 结果转为 StateFlow，避免每次收集时重新计算
     */
    val selectedCount: StateFlow<Int> = selectedIds
        .map { it.size }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    /**
     * 切换单个照片的选中状态
     * 
     * - 如果已选中，则取消选中
     * - 如果未选中，则选中
     * 
     * @param photoId 照片 ID
     */
    fun toggle(photoId: String) {
        _selectedIds.update { current ->
            if (current.contains(photoId)) {
                current - photoId
            } else {
                current + photoId
            }
        }
        updateSelectionMode()
    }
    
    /**
     * 选中指定照片
     * 
     * @param photoId 照片 ID
     */
    fun select(photoId: String) {
        _selectedIds.update { it + photoId }
        updateSelectionMode()
    }
    
    /**
     * 选中多个照片
     * 
     * @param photoIds 照片 ID 集合
     */
    fun selectMultiple(photoIds: Collection<String>) {
        if (photoIds.isEmpty()) return
        _selectedIds.update { it + photoIds }
        updateSelectionMode()
    }
    
    /**
     * 全选
     * 
     * @param allPhotoIds 所有可选照片的 ID 列表
     */
    fun selectAll(allPhotoIds: List<String>) {
        _selectedIds.value = allPhotoIds.toSet()
        _isSelectionMode.value = allPhotoIds.isNotEmpty()
    }
    
    /**
     * 取消选中指定照片
     * 
     * @param photoId 照片 ID
     */
    fun deselect(photoId: String) {
        _selectedIds.update { it - photoId }
        updateSelectionMode()
    }
    
    /**
     * 取消选中多个照片
     * 
     * @param photoIds 照片 ID 集合
     */
    fun deselectMultiple(photoIds: Collection<String>) {
        if (photoIds.isEmpty()) return
        _selectedIds.update { it - photoIds.toSet() }
        updateSelectionMode()
    }
    
    /**
     * 清空选择（退出选择模式）
     * 
     * 应在以下场景调用：
     * - 页面离开时
     * - 批量操作完成后
     * - 用户点击"取消"时
     */
    fun clear() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }
    
    /**
     * 替换选中集合
     * 
     * 用于拖拽多选等场景，直接设置选中集合
     * 
     * @param newSelection 新的选中集合
     */
    fun setSelection(newSelection: Set<String>) {
        _selectedIds.value = newSelection
        updateSelectionMode()
    }
    
    /**
     * 检查指定照片是否选中
     * 
     * @param photoId 照片 ID
     * @return 是否选中
     */
    fun isSelected(photoId: String): Boolean = _selectedIds.value.contains(photoId)
    
    /**
     * 获取选中的 ID 列表
     * 
     * @return 选中的照片 ID 列表（可用于批量操作）
     */
    fun getSelectedList(): List<String> = _selectedIds.value.toList()
    
    /**
     * 获取当前选中数量
     * 
     * @return 选中的照片数量
     */
    fun getCount(): Int = _selectedIds.value.size
    
    /**
     * 是否有选中的照片
     * 
     * @return 是否有选中
     */
    fun hasSelection(): Boolean = _selectedIds.value.isNotEmpty()
    
    /**
     * 是否全选
     * 
     * @param totalCount 总数量
     * @return 是否已全选
     */
    fun isAllSelected(totalCount: Int): Boolean = 
        totalCount > 0 && _selectedIds.value.size == totalCount
    
    /**
     * 反选
     * 
     * @param allPhotoIds 所有可选照片的 ID 列表
     */
    fun invertSelection(allPhotoIds: List<String>) {
        val allSet = allPhotoIds.toSet()
        val currentSelected = _selectedIds.value
        _selectedIds.value = allSet - currentSelected
        updateSelectionMode()
    }
    
    /**
     * 更新选择模式状态
     */
    private fun updateSelectionMode() {
        _isSelectionMode.value = _selectedIds.value.isNotEmpty()
    }
}

/**
 * 选择状态数据类
 * 
 * 用于需要同时访问多个选择状态属性的场景
 */
data class SelectionState(
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
) {
    val selectedCount: Int get() = selectedIds.size
    val hasSelection: Boolean get() = selectedIds.isNotEmpty()
    
    fun isSelected(photoId: String): Boolean = selectedIds.contains(photoId)
    fun isAllSelected(totalCount: Int): Boolean = totalCount > 0 && selectedCount == totalCount
}

/**
 * 从 StateHolder 创建 SelectionState 快照
 * 
 * 用于需要在单个时间点获取完整选择状态的场景
 */
fun PhotoSelectionStateHolder.toSelectionState(): SelectionState = SelectionState(
    selectedIds = selectedIds.value,
    isSelectionMode = isSelectionMode.value
)
