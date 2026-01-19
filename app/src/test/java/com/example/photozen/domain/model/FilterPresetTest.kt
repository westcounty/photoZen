package com.example.photozen.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * FilterPreset 单元测试
 */
class FilterPresetTest {
    
    @Test
    fun `create generates valid preset`() {
        val preset = FilterPreset.create(
            name = "测试预设",
            config = FilterConfig(albumIds = listOf("album1"))
        )
        
        assertEquals("测试预设", preset.name)
        assertNotNull(preset.id)
        assertTrue(preset.createdAt > 0)
    }
    
    @Test
    fun `create trims long name`() {
        val preset = FilterPreset.create(
            name = "这是一个非常长的预设名称超过限制",
            config = FilterConfig(startDate = 1000L)
        )
        
        assertEquals(FilterPreset.MAX_NAME_LENGTH, preset.name.length)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `create throws for empty name`() {
        FilterPreset.create(
            name = "   ",
            config = FilterConfig(startDate = 1000L)
        )
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `create throws for empty config`() {
        FilterPreset.create(
            name = "预设",
            config = FilterConfig.EMPTY
        )
    }
    
    @Test
    fun `getDescription returns album count for single album`() {
        val preset = FilterPreset.create(
            name = "测试",
            config = FilterConfig(albumIds = listOf("a1"))
        )
        
        assertEquals("1 个相册", preset.getDescription())
    }
    
    @Test
    fun `getDescription returns album count for multiple albums`() {
        val preset = FilterPreset.create(
            name = "测试",
            config = FilterConfig(albumIds = listOf("a1", "a2", "a3"))
        )
        
        assertEquals("3 个相册", preset.getDescription())
    }
    
    @Test
    fun `getDescription returns date info`() {
        val preset = FilterPreset.create(
            name = "测试",
            config = FilterConfig(startDate = 1000L, endDate = 2000L)
        )
        
        assertEquals("指定日期", preset.getDescription())
    }
    
    @Test
    fun `getDescription returns date info for startDate only`() {
        val preset = FilterPreset.create(
            name = "测试",
            config = FilterConfig(startDate = 1000L)
        )
        
        assertEquals("指定日期", preset.getDescription())
    }
    
    @Test
    fun `getDescription combines multiple conditions`() {
        val preset = FilterPreset.create(
            name = "测试",
            config = FilterConfig(
                albumIds = listOf("album1"),
                startDate = 1000L
            )
        )
        
        assertEquals("1 个相册, 指定日期", preset.getDescription())
    }
    
    @Test
    fun `MAX_PRESETS is 3`() {
        assertEquals(3, FilterPreset.MAX_PRESETS)
    }
    
    @Test
    fun `MAX_NAME_LENGTH is 10`() {
        assertEquals(10, FilterPreset.MAX_NAME_LENGTH)
    }
    
    @Test
    fun `preset with same data are equal`() {
        val config = FilterConfig(albumIds = listOf("album1"))
        val preset1 = FilterPreset(
            id = "test-id",
            name = "测试",
            config = config,
            createdAt = 1000L
        )
        val preset2 = FilterPreset(
            id = "test-id",
            name = "测试",
            config = config,
            createdAt = 1000L
        )
        
        assertEquals(preset1, preset2)
    }
}
