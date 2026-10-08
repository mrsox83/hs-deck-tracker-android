package com.stroexd.hsdecktracker.core.fusion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import java.time.Duration
import java.time.ZoneId

class PowerClockTest {
    @Test
    fun `resolves fixture clock in recorded timezone`() {
        val end = PowerClock.resolve("Hearthstone_2026_10_07_14_03_50", "14:26:19.4602720", ZoneId.of("America/Chicago"))
        val trackerEnd = java.time.Instant.ofEpochMilli(1_791_401_186_774)
        assertEquals(7, Duration.between(end, trackerEnd).seconds)
    }

    @Test
    fun `moves early clock across midnight only when session anchor supports it`() {
        val resolved = PowerClock.resolve("Hearthstone_2026_10_07_23_58_00", "00:02:00.0000000", ZoneId.of("America/Chicago"))
        assertEquals("2026-10-08T05:02:00Z", resolved.toString())
        assertNull(PowerClock.resolve("unknown", "00:02:00.0", ZoneId.of("UTC")))
    }
}
