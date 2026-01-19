package com.example.photozen.ui.util

import org.junit.Assert.*
import org.junit.Test

/**
 * SelectionUtils 单元测试
 */
class SelectionUtilsTest {
    
    // ==================== selectAllIds 测试 ====================
    
    @Test
    fun `selectAllIds from ids returns set`() {
        val ids = listOf("a", "b", "c")
        
        val result = SelectionUtils.selectAllIds(ids)
        
        assertEquals(setOf("a", "b", "c"), result)
    }
    
    @Test
    fun `selectAllIds handles empty list`() {
        val ids = emptyList<String>()
        
        val result = SelectionUtils.selectAllIds(ids)
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `selectAllIds handles duplicates`() {
        val ids = listOf("a", "b", "a", "c", "b")
        
        val result = SelectionUtils.selectAllIds(ids)
        
        assertEquals(setOf("a", "b", "c"), result)
    }
    
    // ==================== deselectAll 测试 ====================
    
    @Test
    fun `deselectAll returns empty set`() {
        val result = SelectionUtils.deselectAll()
        
        assertTrue(result.isEmpty())
    }
    
    // ==================== toggleSelection 测试 ====================
    
    @Test
    fun `toggleSelection adds when not present`() {
        val current = setOf("1", "2")
        
        val result = SelectionUtils.toggleSelection(current, "3")
        
        assertEquals(setOf("1", "2", "3"), result)
    }
    
    @Test
    fun `toggleSelection removes when present`() {
        val current = setOf("1", "2", "3")
        
        val result = SelectionUtils.toggleSelection(current, "2")
        
        assertEquals(setOf("1", "3"), result)
    }
    
    @Test
    fun `toggleSelection handles empty set`() {
        val current = emptySet<String>()
        
        val result = SelectionUtils.toggleSelection(current, "1")
        
        assertEquals(setOf("1"), result)
    }
    
    @Test
    fun `toggleSelection removes last item`() {
        val current = setOf("1")
        
        val result = SelectionUtils.toggleSelection(current, "1")
        
        assertTrue(result.isEmpty())
    }
    
    // ==================== addToSelection 测试 ====================
    
    @Test
    fun `addToSelection adds multiple ids`() {
        val current = setOf("1")
        
        val result = SelectionUtils.addToSelection(current, listOf("2", "3"))
        
        assertEquals(setOf("1", "2", "3"), result)
    }
    
    @Test
    fun `addToSelection handles duplicates`() {
        val current = setOf("1", "2")
        
        val result = SelectionUtils.addToSelection(current, listOf("2", "3"))
        
        assertEquals(setOf("1", "2", "3"), result)
    }
    
    @Test
    fun `addToSelection handles empty addition`() {
        val current = setOf("1", "2")
        
        val result = SelectionUtils.addToSelection(current, emptyList())
        
        assertEquals(setOf("1", "2"), result)
    }
    
    // ==================== removeFromSelection 测试 ====================
    
    @Test
    fun `removeFromSelection removes multiple ids`() {
        val current = setOf("1", "2", "3", "4")
        
        val result = SelectionUtils.removeFromSelection(current, listOf("2", "4"))
        
        assertEquals(setOf("1", "3"), result)
    }
    
    @Test
    fun `removeFromSelection handles non-existent ids`() {
        val current = setOf("1", "2")
        
        val result = SelectionUtils.removeFromSelection(current, listOf("3", "4"))
        
        assertEquals(setOf("1", "2"), result)
    }
    
    @Test
    fun `removeFromSelection removes all`() {
        val current = setOf("1", "2")
        
        val result = SelectionUtils.removeFromSelection(current, listOf("1", "2"))
        
        assertTrue(result.isEmpty())
    }
    
    // ==================== isAllSelected 测试 ====================
    
    @Test
    fun `isAllSelected returns true when counts match`() {
        assertTrue(SelectionUtils.isAllSelected(10, 10))
    }
    
    @Test
    fun `isAllSelected returns false when counts differ`() {
        assertFalse(SelectionUtils.isAllSelected(5, 10))
    }
    
    @Test
    fun `isAllSelected returns false when total is zero`() {
        assertFalse(SelectionUtils.isAllSelected(0, 0))
    }
    
    @Test
    fun `isAllSelected returns false when selected exceeds total`() {
        assertFalse(SelectionUtils.isAllSelected(15, 10))
    }
    
    @Test
    fun `isAllSelected returns true for single item`() {
        assertTrue(SelectionUtils.isAllSelected(1, 1))
    }
}
