package com.example.photozen.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GuideKey enum.
 */
class GuideKeyTest {

    // ==================== fromString 测试 ====================

    @Test
    fun `fromString returns correct GuideKey for valid string`() {
        assertEquals(GuideKey.PHOTO_LIST_LONG_PRESS, GuideKey.fromString("PHOTO_LIST_LONG_PRESS"))
        assertEquals(GuideKey.HOME_START_BUTTON, GuideKey.fromString("HOME_START_BUTTON"))
        assertEquals(GuideKey.FLOW_SORTER_VIEW_TOGGLE, GuideKey.fromString("FLOW_SORTER_VIEW_TOGGLE"))
        assertEquals(GuideKey.FILTER_PANEL, GuideKey.fromString("FILTER_PANEL"))
        assertEquals(GuideKey.PINCH_ZOOM_GUIDE, GuideKey.fromString("PINCH_ZOOM_GUIDE"))
        assertEquals(GuideKey.SWIPE_SORT_FULLSCREEN_GUIDE, GuideKey.fromString("SWIPE_SORT_FULLSCREEN_GUIDE"))
    }

    @Test
    fun `fromString returns null for invalid string`() {
        assertNull(GuideKey.fromString("INVALID_KEY"))
        assertNull(GuideKey.fromString(""))
        assertNull(GuideKey.fromString("photo_list_long_press")) // Case sensitive
    }

    @Test
    fun `fromString returns null for partial match`() {
        assertNull(GuideKey.fromString("PHOTO_LIST"))
        assertNull(GuideKey.fromString("HOME_"))
    }

    // ==================== description 测试 ====================

    @Test
    fun `all guide keys have non-empty description`() {
        GuideKey.entries.forEach { key ->
            assertTrue("${key.name} should have non-empty description", key.description.isNotEmpty())
        }
    }

    @Test
    fun `new onboarding guides have appropriate descriptions`() {
        assertTrue(GuideKey.PINCH_ZOOM_GUIDE.description.contains("缩放"))
        assertTrue(GuideKey.SWIPE_SORT_FULLSCREEN_GUIDE.description.contains("滑动"))
    }

    // ==================== priority 测试 ====================

    @Test
    fun `SWIPE_SORT_FULLSCREEN_GUIDE has highest priority`() {
        val minPriority = GuideKey.entries.minOf { it.priority }
        assertEquals(GuideKey.SWIPE_SORT_FULLSCREEN_GUIDE.priority, minPriority)
    }

    @Test
    fun `PINCH_ZOOM_GUIDE has second highest priority`() {
        val sortedByPriority = GuideKey.entries.sortedBy { it.priority }
        assertEquals(GuideKey.SWIPE_SORT_FULLSCREEN_GUIDE, sortedByPriority[0])
        assertEquals(GuideKey.PINCH_ZOOM_GUIDE, sortedByPriority[1])
    }

    @Test
    fun `priority values are distinct for new guides`() {
        assertNotEquals(
            GuideKey.PINCH_ZOOM_GUIDE.priority,
            GuideKey.SWIPE_SORT_FULLSCREEN_GUIDE.priority
        )
    }

    // ==================== entries 测试 ====================

    @Test
    fun `entries contains all expected keys`() {
        val expectedKeys = setOf(
            "PHOTO_LIST_LONG_PRESS",
            "HOME_START_BUTTON",
            "FLOW_SORTER_VIEW_TOGGLE",
            "TIMELINE_GROUPING",
            "FILTER_PANEL",
            "STATS_CALENDAR",
            "SHARE_FEATURE_TIP",
            "PINCH_ZOOM_GUIDE",
            "SWIPE_SORT_FULLSCREEN_GUIDE"
        )

        val actualKeys = GuideKey.entries.map { it.name }.toSet()

        assertEquals(expectedKeys, actualKeys)
    }

    @Test
    fun `entries count is 9`() {
        assertEquals(9, GuideKey.entries.size)
    }

    // ==================== name 和 valueOf 测试 ====================

    @Test
    fun `name returns correct string`() {
        assertEquals("PHOTO_LIST_LONG_PRESS", GuideKey.PHOTO_LIST_LONG_PRESS.name)
        assertEquals("PINCH_ZOOM_GUIDE", GuideKey.PINCH_ZOOM_GUIDE.name)
        assertEquals("SWIPE_SORT_FULLSCREEN_GUIDE", GuideKey.SWIPE_SORT_FULLSCREEN_GUIDE.name)
    }

    @Test
    fun `valueOf returns correct enum`() {
        assertEquals(GuideKey.PHOTO_LIST_LONG_PRESS, GuideKey.valueOf("PHOTO_LIST_LONG_PRESS"))
        assertEquals(GuideKey.HOME_START_BUTTON, GuideKey.valueOf("HOME_START_BUTTON"))
        assertEquals(GuideKey.PINCH_ZOOM_GUIDE, GuideKey.valueOf("PINCH_ZOOM_GUIDE"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `valueOf throws for invalid string`() {
        GuideKey.valueOf("INVALID")
    }

    // ==================== 优先级排序测试 ====================

    @Test
    fun `guides are ordered correctly by priority`() {
        val sortedByPriority = GuideKey.entries.sortedBy { it.priority }

        // REQ-067 新手引导应该有最高优先级（最小的数值）
        assertEquals(GuideKey.SWIPE_SORT_FULLSCREEN_GUIDE, sortedByPriority[0]) // priority = 3
        assertEquals(GuideKey.PINCH_ZOOM_GUIDE, sortedByPriority[1]) // priority = 5
        assertEquals(GuideKey.PHOTO_LIST_LONG_PRESS, sortedByPriority[2]) // priority = 10
    }
}
