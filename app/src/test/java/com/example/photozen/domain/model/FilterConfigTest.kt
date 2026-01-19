package com.example.photozen.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * FilterConfig 单元测试
 */
class FilterConfigTest {
    
    // ==================== isEmpty 测试 ====================
    
    @Test
    fun `EMPTY config isEmpty returns true`() {
        assertTrue(FilterConfig.EMPTY.isEmpty)
    }
    
    @Test
    fun `config with albumIds isEmpty returns false`() {
        val config = FilterConfig(albumIds = listOf("album1"))
        assertFalse(config.isEmpty)
    }
    
    @Test
    fun `config with startDate isEmpty returns false`() {
        val config = FilterConfig(startDate = 1000L)
        assertFalse(config.isEmpty)
    }
    
    @Test
    fun `config with endDate isEmpty returns false`() {
        val config = FilterConfig(endDate = 2000L)
        assertFalse(config.isEmpty)
    }
    
    @Test
    fun `config with empty albumIds list isEmpty returns true`() {
        val config = FilterConfig(albumIds = emptyList())
        assertTrue(config.isEmpty)
    }
    
    // ==================== activeFilterCount 测试 ====================
    
    @Test
    fun `EMPTY config activeFilterCount is 0`() {
        assertEquals(0, FilterConfig.EMPTY.activeFilterCount)
    }
    
    @Test
    fun `config with albumIds has count 1`() {
        val config = FilterConfig(albumIds = listOf("album1"))
        assertEquals(1, config.activeFilterCount)
    }
    
    @Test
    fun `config with dates has count 1`() {
        val config = FilterConfig(startDate = 1000L, endDate = 2000L)
        assertEquals(1, config.activeFilterCount)
    }
    
    @Test
    fun `config with both albumIds and dates has count 2`() {
        val config = FilterConfig(
            albumIds = listOf("album1"),
            startDate = 1000L,
            endDate = 2000L
        )
        assertEquals(2, config.activeFilterCount)
    }
    
    // ==================== hasXxxFilter 测试 ====================
    
    @Test
    fun `hasAlbumFilter returns true with albums`() {
        val config = FilterConfig(albumIds = listOf("album1"))
        assertTrue(config.hasAlbumFilter)
    }
    
    @Test
    fun `hasAlbumFilter returns false with empty list`() {
        val config = FilterConfig(albumIds = emptyList())
        assertFalse(config.hasAlbumFilter)
    }
    
    @Test
    fun `hasAlbumFilter returns false with null`() {
        val config = FilterConfig(albumIds = null)
        assertFalse(config.hasAlbumFilter)
    }
    
    @Test
    fun `hasDateFilter returns true with startDate`() {
        val config = FilterConfig(startDate = 1000L)
        assertTrue(config.hasDateFilter)
    }
    
    @Test
    fun `hasDateFilter returns true with endDate`() {
        val config = FilterConfig(endDate = 2000L)
        assertTrue(config.hasDateFilter)
    }
    
    @Test
    fun `hasDateFilter returns true with both dates`() {
        val config = FilterConfig(startDate = 1000L, endDate = 2000L)
        assertTrue(config.hasDateFilter)
    }
    
    @Test
    fun `hasDateFilter returns false with no dates`() {
        val config = FilterConfig()
        assertFalse(config.hasDateFilter)
    }
    
    // ==================== clearXxxFilter 测试 ====================
    
    @Test
    fun `clearAlbumFilter removes only album filter`() {
        val config = FilterConfig(
            albumIds = listOf("album1"),
            startDate = 1000L
        )
        
        val cleared = config.clearAlbumFilter()
        
        assertNull(cleared.albumIds)
        assertEquals(1000L, cleared.startDate)
    }
    
    @Test
    fun `clearDateFilter removes both dates`() {
        val config = FilterConfig(
            startDate = 1000L,
            endDate = 2000L,
            albumIds = listOf("album1")
        )
        
        val cleared = config.clearDateFilter()
        
        assertNull(cleared.startDate)
        assertNull(cleared.endDate)
        assertEquals(listOf("album1"), cleared.albumIds)
    }
    
    // ==================== 数据类测试 ====================
    
    @Test
    fun `config copy works correctly`() {
        val original = FilterConfig(albumIds = listOf("album1"))
        val modified = original.copy(startDate = 1000L)
        
        assertEquals(listOf("album1"), modified.albumIds)
        assertEquals(1000L, modified.startDate)
    }
    
    @Test
    fun `config equals works correctly`() {
        val config1 = FilterConfig(albumIds = listOf("album1"), startDate = 1000L)
        val config2 = FilterConfig(albumIds = listOf("album1"), startDate = 1000L)
        
        assertEquals(config1, config2)
    }
}
