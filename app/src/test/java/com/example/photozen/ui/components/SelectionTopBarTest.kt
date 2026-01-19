package com.example.photozen.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * SelectionTopBarState 单元测试
 */
class SelectionTopBarTest {
    
    // ==================== isAllSelected 测试 ====================
    
    @Test
    fun `isAllSelected returns true when all selected`() {
        val state = SelectionTopBarState(
            selectedCount = 10,
            totalCount = 10,
            onClose = {},
            onSelectAll = {},
            onDeselectAll = {}
        )
        
        assertTrue(state.isAllSelected)
    }
    
    @Test
    fun `isAllSelected returns false when partially selected`() {
        val state = SelectionTopBarState(
            selectedCount = 5,
            totalCount = 10,
            onClose = {},
            onSelectAll = {},
            onDeselectAll = {}
        )
        
        assertFalse(state.isAllSelected)
    }
    
    @Test
    fun `isAllSelected returns false when total is zero`() {
        val state = SelectionTopBarState(
            selectedCount = 0,
            totalCount = 0,
            onClose = {},
            onSelectAll = {},
            onDeselectAll = {}
        )
        
        assertFalse(state.isAllSelected)
    }
    
    @Test
    fun `isAllSelected returns false when selected count exceeds total`() {
        // 这种情况不应该发生，但应该正确处理
        val state = SelectionTopBarState(
            selectedCount = 15,
            totalCount = 10,
            onClose = {},
            onSelectAll = {},
            onDeselectAll = {}
        )
        
        assertFalse(state.isAllSelected)
    }
    
    // ==================== hasSelection 测试 ====================
    
    @Test
    fun `hasSelection returns true when has selection`() {
        val state = SelectionTopBarState(
            selectedCount = 1,
            totalCount = 10,
            onClose = {},
            onSelectAll = {},
            onDeselectAll = {}
        )
        
        assertTrue(state.hasSelection)
    }
    
    @Test
    fun `hasSelection returns false when no selection`() {
        val state = SelectionTopBarState(
            selectedCount = 0,
            totalCount = 10,
            onClose = {},
            onSelectAll = {},
            onDeselectAll = {}
        )
        
        assertFalse(state.hasSelection)
    }
    
    @Test
    fun `hasSelection returns true when all selected`() {
        val state = SelectionTopBarState(
            selectedCount = 10,
            totalCount = 10,
            onClose = {},
            onSelectAll = {},
            onDeselectAll = {}
        )
        
        assertTrue(state.hasSelection)
    }
    
    // ==================== 边界条件测试 ====================
    
    @Test
    fun `state handles negative values gracefully`() {
        val state = SelectionTopBarState(
            selectedCount = -1,
            totalCount = 10,
            onClose = {},
            onSelectAll = {},
            onDeselectAll = {}
        )
        
        assertFalse(state.isAllSelected)
        assertFalse(state.hasSelection)
    }
    
    @Test
    fun `state handles single item selection`() {
        val state = SelectionTopBarState(
            selectedCount = 1,
            totalCount = 1,
            onClose = {},
            onSelectAll = {},
            onDeselectAll = {}
        )
        
        assertTrue(state.isAllSelected)
        assertTrue(state.hasSelection)
    }
}
