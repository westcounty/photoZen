package com.example.photozen.domain.model

import com.example.photozen.data.model.PhotoStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * UndoAction 单元测试
 */
class UndoActionTest {
    
    // ==================== StatusChange 测试 ====================
    
    @Test
    fun `StatusChange affectedCount returns correct count`() {
        val action = UndoAction.StatusChange(
            photoIds = listOf("1", "2", "3"),
            previousStatus = emptyMap(),
            newStatus = PhotoStatus.TRASH
        )
        
        assertEquals(3, action.affectedCount)
    }
    
    @Test
    fun `StatusChange getDescription for TRASH`() {
        val action = UndoAction.StatusChange(
            photoIds = listOf("1", "2"),
            previousStatus = emptyMap(),
            newStatus = PhotoStatus.TRASH
        )
        
        assertEquals("已删除 2 张照片", action.getDescription())
    }
    
    @Test
    fun `StatusChange getDescription for KEEP`() {
        val action = UndoAction.StatusChange(
            photoIds = listOf("1"),
            previousStatus = emptyMap(),
            newStatus = PhotoStatus.KEEP
        )
        
        assertEquals("已保留 1 张照片", action.getDescription())
    }
    
    @Test
    fun `StatusChange getDescription for MAYBE`() {
        val action = UndoAction.StatusChange(
            photoIds = listOf("1", "2", "3", "4", "5"),
            previousStatus = emptyMap(),
            newStatus = PhotoStatus.MAYBE
        )
        
        assertEquals("已标记 5 张照片为待定", action.getDescription())
    }
    
    @Test
    fun `StatusChange getDescription for UNSORTED`() {
        val action = UndoAction.StatusChange(
            photoIds = listOf("1"),
            previousStatus = emptyMap(),
            newStatus = PhotoStatus.UNSORTED
        )
        
        assertEquals("已重置 1 张照片", action.getDescription())
    }
    
    @Test
    fun `StatusChange stores previous status correctly`() {
        val previousStatus = mapOf(
            "1" to PhotoStatus.UNSORTED,
            "2" to PhotoStatus.KEEP
        )
        val action = UndoAction.StatusChange(
            photoIds = listOf("1", "2"),
            previousStatus = previousStatus,
            newStatus = PhotoStatus.TRASH
        )
        
        assertEquals(PhotoStatus.UNSORTED, action.previousStatus["1"])
        assertEquals(PhotoStatus.KEEP, action.previousStatus["2"])
    }
    
    // ==================== MoveToAlbum 测试 ====================
    
    @Test
    fun `MoveToAlbum affectedCount returns correct count`() {
        val action = UndoAction.MoveToAlbum(
            photoIds = listOf("1", "2"),
            fromAlbumId = "album1",
            toAlbumId = "album2"
        )
        
        assertEquals(2, action.affectedCount)
    }
    
    @Test
    fun `MoveToAlbum getDescription`() {
        val action = UndoAction.MoveToAlbum(
            photoIds = listOf("1", "2", "3"),
            fromAlbumId = null,
            toAlbumId = "album1"
        )
        
        assertEquals("已移动 3 张照片", action.getDescription())
    }
    
    @Test
    fun `MoveToAlbum stores album IDs correctly`() {
        val action = UndoAction.MoveToAlbum(
            photoIds = listOf("1"),
            fromAlbumId = "source",
            toAlbumId = "destination"
        )
        
        assertEquals("source", action.fromAlbumId)
        assertEquals("destination", action.toAlbumId)
    }
    
    @Test
    fun `MoveToAlbum handles null fromAlbumId`() {
        val action = UndoAction.MoveToAlbum(
            photoIds = listOf("1"),
            fromAlbumId = null,
            toAlbumId = "album1"
        )
        
        assertNull(action.fromAlbumId)
    }
    
    // ==================== RestoreFromTrash 测试 ====================
    
    @Test
    fun `RestoreFromTrash affectedCount returns correct count`() {
        val action = UndoAction.RestoreFromTrash(
            photoIds = listOf("1"),
            previousStatus = mapOf("1" to PhotoStatus.TRASH)
        )
        
        assertEquals(1, action.affectedCount)
    }
    
    @Test
    fun `RestoreFromTrash getDescription`() {
        val action = UndoAction.RestoreFromTrash(
            photoIds = listOf("1", "2"),
            previousStatus = emptyMap()
        )
        
        assertEquals("已恢复 2 张照片", action.getDescription())
    }
    
    @Test
    fun `RestoreFromTrash stores previous status correctly`() {
        val action = UndoAction.RestoreFromTrash(
            photoIds = listOf("1", "2"),
            previousStatus = mapOf(
                "1" to PhotoStatus.TRASH,
                "2" to PhotoStatus.TRASH
            )
        )
        
        assertEquals(PhotoStatus.TRASH, action.previousStatus["1"])
        assertEquals(PhotoStatus.TRASH, action.previousStatus["2"])
    }
}
