package com.example.photozen.ui.util

import com.example.photozen.data.local.entity.PhotoEntity

/**
 * 选择辅助工具
 * 
 * 提供全选、取消全选等常用操作的辅助函数。
 */
object SelectionUtils {
    
    /**
     * 全选所有照片
     * 
     * @param photos 照片列表
     * @return 所有照片 ID 的集合
     */
    fun selectAll(photos: List<PhotoEntity>): Set<String> = 
        photos.map { it.id }.toSet()
    
    /**
     * 全选指定 ID 列表
     */
    fun selectAllIds(ids: List<String>): Set<String> = ids.toSet()
    
    /**
     * 取消全选
     */
    fun deselectAll(): Set<String> = emptySet()
    
    /**
     * 切换单个选择
     * 
     * @param currentSelection 当前选择集合
     * @param id 要切换的 ID
     * @return 更新后的选择集合
     */
    fun toggleSelection(currentSelection: Set<String>, id: String): Set<String> =
        if (id in currentSelection) {
            currentSelection - id
        } else {
            currentSelection + id
        }
    
    /**
     * 批量添加选择
     */
    fun addToSelection(currentSelection: Set<String>, ids: Collection<String>): Set<String> =
        currentSelection + ids
    
    /**
     * 批量移除选择
     */
    fun removeFromSelection(currentSelection: Set<String>, ids: Collection<String>): Set<String> =
        currentSelection - ids.toSet()
    
    /**
     * 检查是否已全选
     * 
     * @param selectedCount 已选数量
     * @param totalCount 总数量
     */
    fun isAllSelected(selectedCount: Int, totalCount: Int): Boolean =
        selectedCount == totalCount && totalCount > 0
}
