package com.example.photozen.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HomeComponents 单元测试
 * 
 * Phase 1-A: 测试骨架，验证数据类逻辑和设计规范参数
 * Phase 1-D: 补充完整 UI 测试（Compose Testing）
 */
class HomeComponentsTest {
    
    // ==================== DailyTaskDisplayStatus 测试 ====================
    
    @Test
    fun `dailyTaskDisplayStatus progress calculates correctly for normal case`() {
        val status = DailyTaskDisplayStatus(
            current = 5,
            target = 10,
            isEnabled = true,
            isCompleted = false
        )
        assertEquals(0.5f, status.progress, 0.001f)
    }
    
    @Test
    fun `dailyTaskDisplayStatus progress returns zero for zero target`() {
        val status = DailyTaskDisplayStatus(
            current = 5,
            target = 0,
            isEnabled = true,
            isCompleted = false
        )
        assertEquals(0f, status.progress, 0.001f)
    }
    
    @Test
    fun `dailyTaskDisplayStatus progress clamped to one when exceeds target`() {
        val status = DailyTaskDisplayStatus(
            current = 15,
            target = 10,
            isEnabled = true,
            isCompleted = true
        )
        assertEquals(1f, status.progress, 0.001f)
    }
    
    @Test
    fun `dailyTaskDisplayStatus progress returns zero for zero current`() {
        val status = DailyTaskDisplayStatus(
            current = 0,
            target = 10,
            isEnabled = true,
            isCompleted = false
        )
        assertEquals(0f, status.progress, 0.001f)
    }
    
    @Test
    fun `dailyTaskDisplayStatus progress returns one for equal current and target`() {
        val status = DailyTaskDisplayStatus(
            current = 10,
            target = 10,
            isEnabled = true,
            isCompleted = true
        )
        assertEquals(1f, status.progress, 0.001f)
    }
    
    @Test
    fun `dailyTaskDisplayStatus progress handles large numbers`() {
        val status = DailyTaskDisplayStatus(
            current = 500,
            target = 1000,
            isEnabled = true,
            isCompleted = false
        )
        assertEquals(0.5f, status.progress, 0.001f)
    }
    
    @Test
    fun `dailyTaskDisplayStatus isEnabled property works`() {
        val enabledStatus = DailyTaskDisplayStatus(
            current = 5,
            target = 10,
            isEnabled = true,
            isCompleted = false
        )
        assertTrue(enabledStatus.isEnabled)
        
        val disabledStatus = DailyTaskDisplayStatus(
            current = 5,
            target = 10,
            isEnabled = false,
            isCompleted = false
        )
        assertFalse(disabledStatus.isEnabled)
    }
    
    @Test
    fun `dailyTaskDisplayStatus isCompleted property works`() {
        val completedStatus = DailyTaskDisplayStatus(
            current = 10,
            target = 10,
            isEnabled = true,
            isCompleted = true
        )
        assertTrue(completedStatus.isCompleted)
        
        val notCompletedStatus = DailyTaskDisplayStatus(
            current = 5,
            target = 10,
            isEnabled = true,
            isCompleted = false
        )
        assertFalse(notCompletedStatus.isCompleted)
    }
    
    // ==================== HomeDesignTokens 测试 ====================
    
    @Test
    fun `homeDesignTokens cardCornerRadius is 24dp`() {
        assertEquals(24, HomeDesignTokens.CardCornerRadius.value.toInt())
    }
    
    @Test
    fun `homeDesignTokens cardPadding is 24dp`() {
        assertEquals(24, HomeDesignTokens.CardPadding.value.toInt())
    }
    
    @Test
    fun `homeDesignTokens sectionSpacing is 16dp`() {
        assertEquals(16, HomeDesignTokens.SectionSpacing.value.toInt())
    }
    
    @Test
    fun `homeDesignTokens quickActionIconSize is 48dp`() {
        assertEquals(48, HomeDesignTokens.QuickActionIconSize.value.toInt())
    }
    
    @Test
    fun `homeDesignTokens mainActionButtonHeight is 56dp`() {
        assertEquals(56, HomeDesignTokens.MainActionButtonHeight.value.toInt())
    }
    
    // ==================== DailyTaskDisplayStatus 数据类测试 ====================
    
    @Test
    fun `dailyTaskDisplayStatus data class equality works`() {
        val status1 = DailyTaskDisplayStatus(
            current = 5,
            target = 10,
            isEnabled = true,
            isCompleted = false
        )
        val status2 = DailyTaskDisplayStatus(
            current = 5,
            target = 10,
            isEnabled = true,
            isCompleted = false
        )
        assertEquals(status1, status2)
    }
    
    @Test
    fun `dailyTaskDisplayStatus copy works correctly`() {
        val original = DailyTaskDisplayStatus(
            current = 5,
            target = 10,
            isEnabled = true,
            isCompleted = false
        )
        val copied = original.copy(current = 7)
        
        assertEquals(7, copied.current)
        assertEquals(10, copied.target)
        assertTrue(copied.isEnabled)
        assertFalse(copied.isCompleted)
    }
}
