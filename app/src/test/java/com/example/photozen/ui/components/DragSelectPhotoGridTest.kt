package com.example.photozen.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DragSelectPhotoGrid 配置和默认值单元测试
 * 
 * Phase 1-B: 测试配置类和预设对象
 */
class DragSelectPhotoGridTest {
    
    // ==================== DragSelectConfig 测试 ====================
    
    @Test
    fun `dragSelectConfig default values are correct`() {
        val config = DragSelectConfig()
        
        assertTrue(config.hapticEnabled)
        assertTrue(config.autoScrollEnabled)
        assertEquals(80f, config.autoScrollThresholdDp, 0.001f)
        assertEquals(10f, config.autoScrollSpeed, 0.001f)
        assertEquals(10f, config.dragThresholdDp, 0.001f)
    }
    
    @Test
    fun `dragSelectConfig can be created with custom values`() {
        val config = DragSelectConfig(
            hapticEnabled = false,
            autoScrollEnabled = false,
            autoScrollThresholdDp = 100f,
            autoScrollSpeed = 20f,
            dragThresholdDp = 15f
        )
        
        assertFalse(config.hapticEnabled)
        assertFalse(config.autoScrollEnabled)
        assertEquals(100f, config.autoScrollThresholdDp, 0.001f)
        assertEquals(20f, config.autoScrollSpeed, 0.001f)
        assertEquals(15f, config.dragThresholdDp, 0.001f)
    }
    
    @Test
    fun `dragSelectConfig copy works correctly`() {
        val original = DragSelectConfig()
        val copied = original.copy(hapticEnabled = false)
        
        assertFalse(copied.hapticEnabled)
        assertTrue(copied.autoScrollEnabled)  // 其他值保持不变
    }
    
    @Test
    fun `dragSelectConfig equality works`() {
        val config1 = DragSelectConfig()
        val config2 = DragSelectConfig()
        val config3 = DragSelectConfig(hapticEnabled = false)
        
        assertEquals(config1, config2)
        assertFalse(config1 == config3)
    }
    
    // ==================== DragSelectPhotoGridDefaults 测试 ====================
    
    @Test
    fun `defaults standardConfig has correct values`() {
        val config = DragSelectPhotoGridDefaults.StandardConfig
        
        assertTrue(config.hapticEnabled)
        assertTrue(config.autoScrollEnabled)
        assertEquals(80f, config.autoScrollThresholdDp, 0.001f)
        assertEquals(10f, config.autoScrollSpeed, 0.001f)
        assertEquals(10f, config.dragThresholdDp, 0.001f)
    }
    
    @Test
    fun `defaults albumConfig equals standardConfig`() {
        val standard = DragSelectPhotoGridDefaults.StandardConfig
        val album = DragSelectPhotoGridDefaults.AlbumConfig
        
        assertEquals(standard, album)
    }
    
    @Test
    fun `defaults timelineConfig disables autoScroll`() {
        val config = DragSelectPhotoGridDefaults.TimelineConfig
        
        assertFalse(config.autoScrollEnabled)
        assertTrue(config.hapticEnabled)  // 震动反馈仍然启用
    }
    
    @Test
    fun `defaults compactConfig has smaller dragThreshold`() {
        val compact = DragSelectPhotoGridDefaults.CompactConfig
        val standard = DragSelectPhotoGridDefaults.StandardConfig
        
        assertTrue(compact.dragThresholdDp < standard.dragThresholdDp)
        assertTrue(compact.autoScrollThresholdDp < standard.autoScrollThresholdDp)
    }
    
    @Test
    fun `defaults compactConfig specific values`() {
        val config = DragSelectPhotoGridDefaults.CompactConfig
        
        assertEquals(6f, config.dragThresholdDp, 0.001f)
        assertEquals(60f, config.autoScrollThresholdDp, 0.001f)
    }
    
    // ==================== 配置独立性测试 ====================
    
    @Test
    fun `modifying one config does not affect others`() {
        // 由于是 data class，每次访问都是同一个实例
        // 但 copy 会创建新实例
        val original = DragSelectPhotoGridDefaults.StandardConfig
        val modified = original.copy(hapticEnabled = false)
        
        // 原始配置应该不变
        assertTrue(DragSelectPhotoGridDefaults.StandardConfig.hapticEnabled)
        assertFalse(modified.hapticEnabled)
    }
    
    @Test
    fun `all default configs are singleton instances`() {
        // 多次访问返回相同实例
        val standard1 = DragSelectPhotoGridDefaults.StandardConfig
        val standard2 = DragSelectPhotoGridDefaults.StandardConfig
        
        assertTrue(standard1 === standard2)
    }
}
