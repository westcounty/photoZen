package com.example.photozen.ui.guide

import com.example.photozen.domain.model.GuideKey
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GuideState and GuideSequenceState.
 */
class GuideStateTest {

    // ==================== GuideState 测试 ====================

    @Test
    fun `GuideState with shouldShow true`() {
        var dismissed = false
        val state = GuideState(
            shouldShow = true,
            dismiss = { dismissed = true }
        )

        assertTrue(state.shouldShow)
        assertFalse(dismissed)

        state.dismiss()
        assertTrue(dismissed)
    }

    @Test
    fun `GuideState with shouldShow false`() {
        val state = GuideState(
            shouldShow = false,
            dismiss = { }
        )

        assertFalse(state.shouldShow)
    }

    // ==================== GuideSequenceState 测试 ====================

    @Test
    fun `GuideSequenceState isActive when currentGuide is not null`() {
        val state = GuideSequenceState(
            currentGuide = GuideKey.PHOTO_LIST_LONG_PRESS,
            currentStep = 1,
            totalSteps = 3,
            dismissCurrent = { },
            skipAll = { }
        )

        assertTrue(state.isActive)
    }

    @Test
    fun `GuideSequenceState isActive false when currentGuide is null`() {
        val state = GuideSequenceState(
            currentGuide = null,
            currentStep = 3,
            totalSteps = 3,
            dismissCurrent = { },
            skipAll = { }
        )

        assertFalse(state.isActive)
    }

    @Test
    fun `GuideSequenceState step information is correct`() {
        val state = GuideSequenceState(
            currentGuide = GuideKey.HOME_START_BUTTON,
            currentStep = 2,
            totalSteps = 3,
            dismissCurrent = { },
            skipAll = { }
        )

        assertEquals(2, state.currentStep)
        assertEquals(3, state.totalSteps)
        assertEquals(GuideKey.HOME_START_BUTTON, state.currentGuide)
    }

    @Test
    fun `GuideSequenceState dismissCurrent callback works`() {
        var dismissCalled = false
        val state = GuideSequenceState(
            currentGuide = GuideKey.PHOTO_LIST_LONG_PRESS,
            currentStep = 1,
            totalSteps = 3,
            dismissCurrent = { dismissCalled = true },
            skipAll = { }
        )

        state.dismissCurrent()
        assertTrue(dismissCalled)
    }

    @Test
    fun `GuideSequenceState skipAll callback works`() {
        var skipCalled = false
        val state = GuideSequenceState(
            currentGuide = GuideKey.PHOTO_LIST_LONG_PRESS,
            currentStep = 1,
            totalSteps = 3,
            dismissCurrent = { },
            skipAll = { skipCalled = true }
        )

        state.skipAll()
        assertTrue(skipCalled)
    }

    @Test
    fun `GuideSequenceState at final step`() {
        val state = GuideSequenceState(
            currentGuide = GuideKey.FILTER_PANEL,
            currentStep = 3,
            totalSteps = 3,
            dismissCurrent = { },
            skipAll = { }
        )

        assertEquals(3, state.currentStep)
        assertEquals(state.currentStep, state.totalSteps)
        assertTrue(state.isActive)
    }

    @Test
    fun `GuideSequenceState after completion`() {
        val state = GuideSequenceState(
            currentGuide = null,
            currentStep = 3,
            totalSteps = 3,
            dismissCurrent = { },
            skipAll = { }
        )

        assertNull(state.currentGuide)
        assertFalse(state.isActive)
    }

    // ==================== 数据类测试 ====================

    @Test
    fun `GuideState equals works correctly`() {
        val dismiss = { }
        val state1 = GuideState(shouldShow = true, dismiss = dismiss)
        val state2 = GuideState(shouldShow = true, dismiss = dismiss)
        val state3 = GuideState(shouldShow = false, dismiss = dismiss)

        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }

    @Test
    fun `GuideSequenceState equals works correctly`() {
        val dismiss = { }
        val skip = { }

        val state1 = GuideSequenceState(
            currentGuide = GuideKey.PHOTO_LIST_LONG_PRESS,
            currentStep = 1,
            totalSteps = 3,
            dismissCurrent = dismiss,
            skipAll = skip
        )
        val state2 = GuideSequenceState(
            currentGuide = GuideKey.PHOTO_LIST_LONG_PRESS,
            currentStep = 1,
            totalSteps = 3,
            dismissCurrent = dismiss,
            skipAll = skip
        )

        assertEquals(state1, state2)
    }

    @Test
    fun `GuideSequenceState copy works correctly`() {
        val state = GuideSequenceState(
            currentGuide = GuideKey.PHOTO_LIST_LONG_PRESS,
            currentStep = 1,
            totalSteps = 3,
            dismissCurrent = { },
            skipAll = { }
        )

        val copied = state.copy(currentStep = 2, currentGuide = GuideKey.HOME_START_BUTTON)

        assertEquals(GuideKey.HOME_START_BUTTON, copied.currentGuide)
        assertEquals(2, copied.currentStep)
        assertEquals(3, copied.totalSteps)
    }
}
