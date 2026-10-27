package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.captureSkipReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Retains the earlier characterization for accessibility and unknown/stale projection fallback. */
class CaptureEligibilityTest {
    private fun decision(physicalWidth: Int, physicalHeight: Int, content: Pair<Int, Int>?, interactive: Boolean = true, active: Boolean = true): Int? {
        // These cases deliberately select the unchanged physical-metric fallback; projection cases are separate.
        content?.let { require(it.first > 0 && it.second > 0) }
        return captureSkipReason(interactive, physicalWidth, physicalHeight, isActive = { active })
    }

    @Test fun `reported portrait metrics skip despite independently known landscape app`() {
        assertEquals(2, decision(1080, 2520, 2520 to 1080))
    }
    @Test fun `unknown geometry does not bypass current portrait guard`() {
        assertEquals(2, decision(1080, 2520, null))
    }
    @Test fun `actual portrait content on portrait display remains skipped`() {
        assertEquals(2, decision(1080, 2520, 1080 to 2520))
    }
    @Test fun `physical orientation transition pauses then resumes landscape app`() {
        assertEquals(listOf(null, 2, null), listOf(2520 to 1080, 1080 to 2520, 2520 to 1080).map {
            decision(it.first, it.second, 2520 to 1080)
        })
    }
    @Test fun `screen off takes precedence and active supplier is not called`() {
        var calls = 0
        assertEquals(1, captureSkipReason(false, 1080, 2520) { calls++; true })
        assertEquals(0, calls)
    }
    @Test fun `inactive landscape capture remains skipped and active capture allowed`() {
        assertEquals(3, decision(2520, 1080, 2520 to 1080, active = false))
        assertNull(decision(2520, 1080, 2520 to 1080))
    }
    @Test fun `unknown captured geometry is currently allowed on landscape physical display`() {
        assertNull(decision(2520, 1080, null))
    }
}
