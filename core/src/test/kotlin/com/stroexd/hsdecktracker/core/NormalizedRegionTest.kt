package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.NormalizedRegion
import com.stroexd.hsdecktracker.core.vision.ScreenBox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NormalizedRegionTest {
    @Test
    fun `region scales to each captured content size`() {
        val region = NormalizedRegion(0.25f, 0.10f, 0.75f, 0.90f)

        assertEquals(ScreenBox(500, 100, 1500, 900), region.pixels(2000, 1000))
        assertEquals(ScreenBox(250, 50, 750, 450), region.pixels(1000, 500))
    }

    @Test
    fun `region rejects invalid or empty bounds`() {
        assertFailsWith<IllegalArgumentException> { NormalizedRegion(-0.1f, 0f, 1f, 1f) }
        assertFailsWith<IllegalArgumentException> { NormalizedRegion(0.5f, 0f, 0.5f, 1f) }
        assertFailsWith<IllegalArgumentException> { NormalizedRegion(0f, 1f, 1f, 0f) }
    }
}
