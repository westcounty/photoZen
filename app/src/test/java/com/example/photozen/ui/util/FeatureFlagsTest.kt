package com.example.photozen.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FeatureFlags 单元测试
 * 
 * 验证 Feature Flag 框架的基本功能：
 * - 默认值
 * - 设置/获取
 * - 重置
 */
class FeatureFlagsTest {
    
    @Before
    fun setup() {
        // 每个测试前重置所有 Flag
        FeatureFlags.resetAll()
    }
    
    // ==================== 默认值测试 ====================
    
    @Test
    fun `featureFlags default values are all false`() {
        assertFalse(FeatureFlags.USE_UNIFIED_GESTURE)
        assertFalse(FeatureFlags.USE_BOTTOM_NAV)
        assertFalse(FeatureFlags.USE_NEW_HOME_LAYOUT)
    }
    
    @Test
    fun `featureFlags USE_UNIFIED_GESTURE default is false`() {
        assertFalse(FeatureFlags.USE_UNIFIED_GESTURE)
    }
    
    @Test
    fun `featureFlags USE_BOTTOM_NAV default is false`() {
        assertFalse(FeatureFlags.USE_BOTTOM_NAV)
    }
    
    @Test
    fun `featureFlags USE_NEW_HOME_LAYOUT default is false`() {
        assertFalse(FeatureFlags.USE_NEW_HOME_LAYOUT)
    }
    
    // ==================== 设置测试 ====================
    
    @Test
    fun `featureFlags USE_UNIFIED_GESTURE can be set to true`() {
        FeatureFlags.USE_UNIFIED_GESTURE = true
        assertTrue(FeatureFlags.USE_UNIFIED_GESTURE)
    }
    
    @Test
    fun `featureFlags USE_BOTTOM_NAV can be set to true`() {
        FeatureFlags.USE_BOTTOM_NAV = true
        assertTrue(FeatureFlags.USE_BOTTOM_NAV)
    }
    
    @Test
    fun `featureFlags USE_NEW_HOME_LAYOUT can be set to true`() {
        FeatureFlags.USE_NEW_HOME_LAYOUT = true
        assertTrue(FeatureFlags.USE_NEW_HOME_LAYOUT)
    }
    
    @Test
    fun `featureFlags set to true persists value`() {
        FeatureFlags.USE_BOTTOM_NAV = true
        assertTrue(FeatureFlags.USE_BOTTOM_NAV)
        
        // 读取多次仍然保持
        assertTrue(FeatureFlags.USE_BOTTOM_NAV)
        assertTrue(FeatureFlags.USE_BOTTOM_NAV)
    }
    
    @Test
    fun `featureFlags can be set to false after being true`() {
        FeatureFlags.USE_BOTTOM_NAV = true
        assertTrue(FeatureFlags.USE_BOTTOM_NAV)
        
        FeatureFlags.USE_BOTTOM_NAV = false
        assertFalse(FeatureFlags.USE_BOTTOM_NAV)
    }
    
    // ==================== resetAll 测试 ====================
    
    @Test
    fun `featureFlags resetAll resets all to false`() {
        // 先设置为 true
        FeatureFlags.USE_UNIFIED_GESTURE = true
        FeatureFlags.USE_BOTTOM_NAV = true
        FeatureFlags.USE_NEW_HOME_LAYOUT = true
        
        // 验证设置成功
        assertTrue(FeatureFlags.USE_UNIFIED_GESTURE)
        assertTrue(FeatureFlags.USE_BOTTOM_NAV)
        assertTrue(FeatureFlags.USE_NEW_HOME_LAYOUT)
        
        // 重置
        FeatureFlags.resetAll()
        
        // 验证重置成功
        assertFalse(FeatureFlags.USE_UNIFIED_GESTURE)
        assertFalse(FeatureFlags.USE_BOTTOM_NAV)
        assertFalse(FeatureFlags.USE_NEW_HOME_LAYOUT)
    }
    
    @Test
    fun `featureFlags resetAll works when already all false`() {
        // 已经是 false
        assertFalse(FeatureFlags.USE_UNIFIED_GESTURE)
        assertFalse(FeatureFlags.USE_BOTTOM_NAV)
        assertFalse(FeatureFlags.USE_NEW_HOME_LAYOUT)
        
        // 重置不会出错
        FeatureFlags.resetAll()
        
        // 仍然是 false
        assertFalse(FeatureFlags.USE_UNIFIED_GESTURE)
        assertFalse(FeatureFlags.USE_BOTTOM_NAV)
        assertFalse(FeatureFlags.USE_NEW_HOME_LAYOUT)
    }
    
    @Test
    fun `featureFlags resetAll only affects the flags that were true`() {
        // 只设置部分
        FeatureFlags.USE_BOTTOM_NAV = true
        
        // 重置
        FeatureFlags.resetAll()
        
        // 全部是 false
        assertFalse(FeatureFlags.USE_UNIFIED_GESTURE)
        assertFalse(FeatureFlags.USE_BOTTOM_NAV)
        assertFalse(FeatureFlags.USE_NEW_HOME_LAYOUT)
    }
    
    // ==================== enableAllPhase1 测试 ====================
    
    @Test
    fun `featureFlags enableAllPhase1 enables all flags`() {
        // 默认全部 false
        assertFalse(FeatureFlags.USE_UNIFIED_GESTURE)
        assertFalse(FeatureFlags.USE_BOTTOM_NAV)
        assertFalse(FeatureFlags.USE_NEW_HOME_LAYOUT)
        
        // 启用所有
        FeatureFlags.enableAllPhase1()
        
        // 全部是 true
        assertTrue(FeatureFlags.USE_UNIFIED_GESTURE)
        assertTrue(FeatureFlags.USE_BOTTOM_NAV)
        assertTrue(FeatureFlags.USE_NEW_HOME_LAYOUT)
    }
    
    @Test
    fun `featureFlags enableAllPhase1 then resetAll works`() {
        FeatureFlags.enableAllPhase1()
        assertTrue(FeatureFlags.USE_UNIFIED_GESTURE)
        assertTrue(FeatureFlags.USE_BOTTOM_NAV)
        assertTrue(FeatureFlags.USE_NEW_HOME_LAYOUT)
        
        FeatureFlags.resetAll()
        assertFalse(FeatureFlags.USE_UNIFIED_GESTURE)
        assertFalse(FeatureFlags.USE_BOTTOM_NAV)
        assertFalse(FeatureFlags.USE_NEW_HOME_LAYOUT)
    }
    
    // ==================== getStatusDescription 测试 ====================
    
    @Test
    fun `featureFlags getStatusDescription contains all flag names`() {
        val description = FeatureFlags.getStatusDescription()
        
        assertTrue(description.contains("USE_UNIFIED_GESTURE"))
        assertTrue(description.contains("USE_BOTTOM_NAV"))
        assertTrue(description.contains("USE_NEW_HOME_LAYOUT"))
    }
    
    @Test
    fun `featureFlags getStatusDescription shows false when all disabled`() {
        val description = FeatureFlags.getStatusDescription()
        
        assertTrue(description.contains("USE_UNIFIED_GESTURE = false"))
        assertTrue(description.contains("USE_BOTTOM_NAV = false"))
        assertTrue(description.contains("USE_NEW_HOME_LAYOUT = false"))
    }
    
    @Test
    fun `featureFlags getStatusDescription shows true when enabled`() {
        FeatureFlags.USE_BOTTOM_NAV = true
        
        val description = FeatureFlags.getStatusDescription()
        
        assertTrue(description.contains("USE_UNIFIED_GESTURE = false"))
        assertTrue(description.contains("USE_BOTTOM_NAV = true"))
        assertTrue(description.contains("USE_NEW_HOME_LAYOUT = false"))
    }
    
    // ==================== 独立性测试 ====================
    
    @Test
    fun `featureFlags each flag is independent`() {
        // 只设置一个
        FeatureFlags.USE_BOTTOM_NAV = true
        
        // 其他不受影响
        assertFalse(FeatureFlags.USE_UNIFIED_GESTURE)
        assertTrue(FeatureFlags.USE_BOTTOM_NAV)
        assertFalse(FeatureFlags.USE_NEW_HOME_LAYOUT)
    }
    
    @Test
    fun `featureFlags multiple flags can be set independently`() {
        FeatureFlags.USE_UNIFIED_GESTURE = true
        FeatureFlags.USE_NEW_HOME_LAYOUT = true
        
        assertTrue(FeatureFlags.USE_UNIFIED_GESTURE)
        assertFalse(FeatureFlags.USE_BOTTOM_NAV)  // 没有设置
        assertTrue(FeatureFlags.USE_NEW_HOME_LAYOUT)
    }
}
