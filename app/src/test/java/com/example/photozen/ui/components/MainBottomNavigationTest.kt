package com.example.photozen.ui.components

import com.example.photozen.navigation.MainDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MainBottomNavigation 及 MainDestination 单元测试
 * 
 * Phase 1-A: 测试骨架，验证基础导航逻辑
 * Phase 1-C: 补充完整测试用例（包含 Compose UI 测试）
 */
class MainBottomNavigationTest {
    
    // ==================== MainDestination.entries 测试 ====================
    
    @Test
    fun `mainDestination entries has four items`() {
        assertEquals(4, MainDestination.entries.size)
    }
    
    @Test
    fun `mainDestination entries are in correct order`() {
        val entries = MainDestination.entries
        assertEquals(MainDestination.Home, entries[0])
        assertEquals(MainDestination.Timeline, entries[1])
        assertEquals(MainDestination.Albums, entries[2])
        assertEquals(MainDestination.Settings, entries[3])
    }
    
    // ==================== MainDestination 路由测试 ====================
    
    @Test
    fun `mainDestination home has correct route`() {
        assertEquals("main_home", MainDestination.Home.route)
    }
    
    @Test
    fun `mainDestination timeline has correct route`() {
        assertEquals("main_timeline", MainDestination.Timeline.route)
    }
    
    @Test
    fun `mainDestination albums has correct route`() {
        assertEquals("main_albums", MainDestination.Albums.route)
    }
    
    @Test
    fun `mainDestination settings has correct route`() {
        assertEquals("main_settings", MainDestination.Settings.route)
    }
    
    // ==================== MainDestination 标签测试 ====================
    
    @Test
    fun `mainDestination home has correct label`() {
        assertEquals("首页", MainDestination.Home.label)
    }
    
    @Test
    fun `mainDestination timeline has correct label`() {
        assertEquals("时间线", MainDestination.Timeline.label)
    }
    
    @Test
    fun `mainDestination albums has correct label`() {
        assertEquals("相册", MainDestination.Albums.label)
    }
    
    @Test
    fun `mainDestination settings has correct label`() {
        assertEquals("设置", MainDestination.Settings.label)
    }
    
    // ==================== shouldShowBottomNav 测试 ====================
    
    @Test
    fun `shouldShowBottomNav returns true for home route`() {
        assertTrue(MainDestination.shouldShowBottomNav("main_home"))
    }
    
    @Test
    fun `shouldShowBottomNav returns true for timeline route`() {
        assertTrue(MainDestination.shouldShowBottomNav("main_timeline"))
    }
    
    @Test
    fun `shouldShowBottomNav returns true for albums route`() {
        assertTrue(MainDestination.shouldShowBottomNav("main_albums"))
    }
    
    @Test
    fun `shouldShowBottomNav returns true for settings route`() {
        assertTrue(MainDestination.shouldShowBottomNav("main_settings"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for FlowSorter route`() {
        assertFalse(MainDestination.shouldShowBottomNav("FlowSorter"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for FlowSorter with params`() {
        assertFalse(MainDestination.shouldShowBottomNav("FlowSorter/{photoId}"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for Workflow route`() {
        assertFalse(MainDestination.shouldShowBottomNav("Workflow"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for PhotoEditor route`() {
        assertFalse(MainDestination.shouldShowBottomNav("PhotoEditor"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for LightTable route`() {
        assertFalse(MainDestination.shouldShowBottomNav("LightTable"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for ShareCopy route`() {
        assertFalse(MainDestination.shouldShowBottomNav("ShareCopy"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for ShareCompare route`() {
        assertFalse(MainDestination.shouldShowBottomNav("ShareCompare"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for PhotoFilterSelection route`() {
        assertFalse(MainDestination.shouldShowBottomNav("PhotoFilterSelection"))
    }
    
    @Test
    fun `shouldShowBottomNav returns true for null route`() {
        assertTrue(MainDestination.shouldShowBottomNav(null))
    }
    
    @Test
    fun `shouldShowBottomNav returns true for unknown route`() {
        assertTrue(MainDestination.shouldShowBottomNav("SomeUnknownRoute"))
    }
    
    // ==================== fromRoute 测试 ====================
    
    @Test
    fun `fromRoute returns Home for main_home`() {
        assertEquals(MainDestination.Home, MainDestination.fromRoute("main_home"))
    }
    
    @Test
    fun `fromRoute returns Timeline for main_timeline`() {
        assertEquals(MainDestination.Timeline, MainDestination.fromRoute("main_timeline"))
    }
    
    @Test
    fun `fromRoute returns Albums for main_albums`() {
        assertEquals(MainDestination.Albums, MainDestination.fromRoute("main_albums"))
    }
    
    @Test
    fun `fromRoute returns Settings for main_settings`() {
        assertEquals(MainDestination.Settings, MainDestination.fromRoute("main_settings"))
    }
    
    @Test
    fun `fromRoute returns null for unknown route`() {
        assertNull(MainDestination.fromRoute("unknown_route"))
    }
    
    @Test
    fun `fromRoute returns null for null route`() {
        assertNull(MainDestination.fromRoute(null))
    }
    
    // ==================== fullscreenRoutes 测试 ====================
    // Phase 1-C: fullscreenRoutes 已改为 private
    // 全屏路由的测试通过 shouldShowBottomNav 函数进行
    // 详细测试见 MainScaffoldTest.kt
}
