package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.PowerClock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.*

class PowerClockTest {
    private val zone = ZoneId.of("America/Chicago")
    @Test fun earlyMorningSessionDoesNotWrapTheTwelveHourThreshold() {
        assertEquals(Instant.parse("2026-10-12T05:14:24.133624Z"),
            PowerClock.resolve("Hearthstone_2026_10_12_00_08_50", "00:14:24.1336240", zone))
        assertEquals(Instant.parse("2026-10-12T10:01:00Z"),
            PowerClock.resolve("Hearthstone_2026_10_12_05_00_00", "05:01:00", zone))
    }
    @Test fun actualMidnightRolloverStillAdvancesTheDate() {
        assertEquals(Instant.parse("2026-10-13T05:01:00Z"),
            PowerClock.resolve("Hearthstone_2026_10_12_23_50_00", "00:01:00", zone))
        assertEquals(Instant.parse("2026-10-13T04:55:00Z"),
            PowerClock.resolve("Hearthstone_2026_10_12_23_50_00", "23:55:00", zone))
    }
    @Test fun smallBackwardClockAndInvalidInputAreNotGuessedIntoTomorrow() {
        assertEquals(Instant.parse("2026-10-12T05:07:00Z"),
            PowerClock.resolve("Hearthstone_2026_10_12_00_08_50", "00:07:00", zone))
        assertNull(PowerClock.resolve("unknown", "00:14:00", zone))
        assertNull(PowerClock.resolve("Hearthstone_2026_02_30_00_08_50", "00:14:00", zone))
    }
}
