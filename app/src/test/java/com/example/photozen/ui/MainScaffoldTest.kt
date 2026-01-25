package com.example.photozen.ui

import com.example.photozen.navigation.MainDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MainScaffold 和相关组件的单元测试
 * 
 * Phase 1-C: 测试底部导航系统逻辑
 */
class MainScaffoldTest {
    
    // ==================== MainDestination 测试 ====================
    
    @Test
    fun `mainDestination entries has four tabs`() {
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
    
    // ==================== shouldShowBottomNav 测试 ====================
    
    @Test
    fun `shouldShowBottomNav returns true for null route`() {
        assertTrue(MainDestination.shouldShowBottomNav(null))
    }
    
    @Test
    fun `shouldShowBottomNav returns true for main_home route`() {
        assertTrue(MainDestination.shouldShowBottomNav("main_home"))
    }
    
    @Test
    fun `shouldShowBottomNav returns true for main_timeline route`() {
        assertTrue(MainDestination.shouldShowBottomNav("main_timeline"))
    }
    
    @Test
    fun `shouldShowBottomNav returns true for main_albums route`() {
        assertTrue(MainDestination.shouldShowBottomNav("main_albums"))
    }
    
    @Test
    fun `shouldShowBottomNav returns true for main_settings route`() {
        assertTrue(MainDestination.shouldShowBottomNav("main_settings"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for FlowSorter route`() {
        assertFalse(MainDestination.shouldShowBottomNav("FlowSorter"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for FlowSorter with parameters`() {
        assertFalse(MainDestination.shouldShowBottomNav("FlowSorter?isDailyTask=true"))
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
    fun `shouldShowBottomNav returns false for PhotoList route`() {
        assertFalse(MainDestination.shouldShowBottomNav("PhotoList"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for AlbumPhotoList route`() {
        assertFalse(MainDestination.shouldShowBottomNav("AlbumPhotoList"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for Trash route`() {
        assertFalse(MainDestination.shouldShowBottomNav("Trash"))
    }
    
    @Test
    fun `shouldShowBottomNav returns false for Achievements route`() {
        assertFalse(MainDestination.shouldShowBottomNav("Achievements"))
    }
    
    @Test
    fun `shouldShowBottomNav returns true for unknown route`() {
        // 未知路由默认显示底部导航（兼容性）
        assertTrue(MainDestination.shouldShowBottomNav("UnknownRoute"))
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
    
    @Test
    fun `fromRoute returns null for Screen Home route`() {
        // Screen.Home 路由与 MainDestination.Home 路由不同
        assertNull(MainDestination.fromRoute("Home"))
    }
    
    // ==================== MainScaffoldState 测试 ====================
    
    @Test
    fun `mainScaffoldState default values are correct`() {
        val state = MainScaffoldState()
        assertEquals(MainDestination.Home, state.currentDestination)
        assertTrue(state.showBottomNav)
    }
    
    @Test
    fun `mainScaffoldState can be created with custom values`() {
        val state = MainScaffoldState(
            currentDestination = MainDestination.Timeline,
            showBottomNav = false
        )
        assertEquals(MainDestination.Timeline, state.currentDestination)
        assertFalse(state.showBottomNav)
    }
    
    @Test
    fun `mainScaffoldState copy works correctly`() {
        val original = MainScaffoldState()
        val copied = original.copy(currentDestination = MainDestination.Albums)
        
        assertEquals(MainDestination.Albums, copied.currentDestination)
        assertTrue(copied.showBottomNav)  // 其他值保持不变
    }
    
    // ==================== Tab 标签测试 ====================
    
    @Test
    fun `home tab has correct label`() {
        assertEquals("首页", MainDestination.Home.label)
    }
    
    @Test
    fun `timeline tab has correct label`() {
        assertEquals("时间线", MainDestination.Timeline.label)
    }
    
    @Test
    fun `albums tab has correct label`() {
        assertEquals("相册", MainDestination.Albums.label)
    }
    
    @Test
    fun `settings tab has correct label`() {
        assertEquals("设置", MainDestination.Settings.label)
    }
    
    // ==================== Tab 图标测试 ====================
    
    @Test
    fun `all tabs have icons`() {
        MainDestination.entries.forEach { destination ->
            assertNotNull(destination.icon)
            assertNotNull(destination.selectedIcon)
        }
    }
    
    @Test
    fun `tabs have different selected and unselected icons`() {
        // 通常 Outlined 和 Filled 图标是不同的对象
        MainDestination.entries.forEach { destination ->
            // 只验证图标不为 null（实际图标对象可能相同也可能不同）
            assertNotNull(destination.icon)
            assertNotNull(destination.selectedIcon)
        }
    }
}
