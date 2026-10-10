package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.*
import kotlin.test.Test
import kotlin.test.assertEquals

class VisualRegionPilotTest {
    @Test fun `region boundaries and bounded candidates remain diagnostic`() {
        val region = PilotRegion("test", "hero_state", NormalizedRegion(.25f, .25f, .75f, .75f))
        val inside = OcrLine("30", .4f, .4f, .6f, .6f)
        val outside = OcrLine("99", .8f, .8f, .9f, .9f)
        val result = VisualRegionPilot.probe(OcrFrame(123, List(40) { inside } + outside), listOf(region)).single()
        assertEquals(32, result.lines.size)
        assertEquals(123, result.observedAt)
        assertEquals("experimental", result.stage)
        assertEquals(listOf("30"), result.lines.map { it.text }.distinct())
    }

    @Test fun `empty windows preserve unknown observations in all three groups`() {
        val result = VisualRegionPilot.probe(OcrFrame(456, emptyList()))
        assertEquals(setOf("hero_state", "turn_resources", "event_recovery"), result.map { it.region.group }.toSet())
        assertEquals(0, result.sumOf { it.lines.size })
    }
}
