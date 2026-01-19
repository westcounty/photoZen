package com.example.photozen.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GuideKey enum.
 */
class GuideKeyTest {
    
    // ==================== flowSorterSequence 测试 ====================
    
    @Test
    fun `flowSorterSequence has three guides`() {
        val sequence = GuideKey.flowSorterSequence
        assertEquals(3, sequence.size)
    }
    
    @Test
    fun `flowSorterSequence is in correct order`() {
        val sequence = GuideKey.flowSorterSequence
        
        assertEquals(GuideKey.SWIPE_RIGHT, sequence[0])
        assertEquals(GuideKey.SWIPE_LEFT, sequence[1])
        assertEquals(GuideKey.SWIPE_UP, sequence[2])
    }
    
    @Test
    fun `flowSorterSequence contains only swipe guides`() {
        val sequence = GuideKey.flowSorterSequence
        
        assertTrue(sequence.all { it.name.startsWith("SWIPE_") })
    }
    
    // ==================== fromString 测试 ====================
    
    @Test
    fun `fromString returns correct GuideKey for valid string`() {
        assertEquals(GuideKey.SWIPE_RIGHT, GuideKey.fromString("SWIPE_RIGHT"))
        assertEquals(GuideKey.SWIPE_LEFT, GuideKey.fromString("SWIPE_LEFT"))
        assertEquals(GuideKey.SWIPE_UP, GuideKey.fromString("SWIPE_UP"))
        assertEquals(GuideKey.PHOTO_LIST_LONG_PRESS, GuideKey.fromString("PHOTO_LIST_LONG_PRESS"))
        assertEquals(GuideKey.HOME_START_BUTTON, GuideKey.fromString("HOME_START_BUTTON"))
    }
    
    @Test
    fun `fromString returns null for invalid string`() {
        assertNull(GuideKey.fromString("INVALID_KEY"))
        assertNull(GuideKey.fromString(""))
        assertNull(GuideKey.fromString("swipe_right")) // Case sensitive
    }
    
    @Test
    fun `fromString returns null for partial match`() {
        assertNull(GuideKey.fromString("SWIPE"))
        assertNull(GuideKey.fromString("SWIPE_"))
    }
    
    // ==================== description 测试 ====================
    
    @Test
    fun `all guide keys have non-empty description`() {
        GuideKey.entries.forEach { key ->
            assertTrue("${key.name} should have non-empty description", key.description.isNotEmpty())
        }
    }
    
    @Test
    fun `swipe guides have swipe-related descriptions`() {
        assertTrue(GuideKey.SWIPE_RIGHT.description.contains("滑动"))
        assertTrue(GuideKey.SWIPE_LEFT.description.contains("滑动"))
        assertTrue(GuideKey.SWIPE_UP.description.contains("滑动"))
    }
    
    // ==================== priority 测试 ====================
    
    @Test
    fun `flowSorterSequence guides have lowest priority values`() {
        val maxFlowPriority = GuideKey.flowSorterSequence.maxOf { it.priority }
        val minOtherPriority = GuideKey.entries
            .filter { it !in GuideKey.flowSorterSequence }
            .minOf { it.priority }
        
        assertTrue(
            "Flow sorter guides should have lower priority than others",
            maxFlowPriority < minOtherPriority
        )
    }
    
    @Test
    fun `flowSorterSequence guides are ordered by priority`() {
        val sequence = GuideKey.flowSorterSequence
        
        for (i in 0 until sequence.size - 1) {
            assertTrue(
                "${sequence[i].name} should have lower or equal priority than ${sequence[i + 1].name}",
                sequence[i].priority <= sequence[i + 1].priority
            )
        }
    }
    
    @Test
    fun `SWIPE_RIGHT has priority 0`() {
        assertEquals(0, GuideKey.SWIPE_RIGHT.priority)
    }
    
    @Test
    fun `SWIPE_LEFT has priority 1`() {
        assertEquals(1, GuideKey.SWIPE_LEFT.priority)
    }
    
    @Test
    fun `SWIPE_UP has priority 2`() {
        assertEquals(2, GuideKey.SWIPE_UP.priority)
    }
    
    // ==================== entries 测试 ====================
    
    @Test
    fun `entries contains all expected keys`() {
        val expectedKeys = setOf(
            "SWIPE_RIGHT",
            "SWIPE_LEFT",
            "SWIPE_UP",
            "PHOTO_LIST_LONG_PRESS",
            "HOME_START_BUTTON",
            "SELECTION_SELECT_ALL",
            "FILTER_PANEL"
        )
        
        val actualKeys = GuideKey.entries.map { it.name }.toSet()
        
        assertEquals(expectedKeys, actualKeys)
    }
    
    @Test
    fun `entries count is 7`() {
        assertEquals(7, GuideKey.entries.size)
    }
    
    // ==================== name 和 valueOf 测试 ====================
    
    @Test
    fun `name returns correct string`() {
        assertEquals("SWIPE_RIGHT", GuideKey.SWIPE_RIGHT.name)
        assertEquals("PHOTO_LIST_LONG_PRESS", GuideKey.PHOTO_LIST_LONG_PRESS.name)
    }
    
    @Test
    fun `valueOf returns correct enum`() {
        assertEquals(GuideKey.SWIPE_RIGHT, GuideKey.valueOf("SWIPE_RIGHT"))
        assertEquals(GuideKey.HOME_START_BUTTON, GuideKey.valueOf("HOME_START_BUTTON"))
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `valueOf throws for invalid string`() {
        GuideKey.valueOf("INVALID")
    }
}
