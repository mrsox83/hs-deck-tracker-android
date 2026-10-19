package com.stroexd.hsdecktracker.core.fusion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChronologyEpochTest {
    @Test fun gameResetPreservesPriorEventsAsHistoricalAndStartsUnknownCurrentEpoch() {
        fun event(sequence: Int, line: Int, raw: String, extra: String = "") =
            """{"sequence":$sequence,"source_line":$line,"log_time":"12:00:0$sequence.0","raw":"$raw"$extra}"""
        val events = listOf(
            event(1, 1, "Player EntityID=2 PlayerID=1"),
            event(2, 2, "tag", ", \"entity\":\"2\",\"tag\":\"CURRENT_PLAYER\",\"value\":\"1\""),
            event(3, 3, "BLOCK_START", ", \"block_type\":\"PLAY\",\"actor\":\"2\""),
            event(4, 4, "BLOCK_END"),
            event(5, 5, "tag", ", \"entity\":\"83\",\"tag\":\"USED_REWIND\",\"value\":\"1\""),
            event(6, 6, "BLOCK_START", ", \"block_type\":\"GAME_RESET\",\"actor\":\"83\""),
            event(7, 7, "BLOCK_END"),
            event(8, 8, "BLOCK_START", ", \"block_type\":\"PLAY\",\"actor\":\"2\""),
            event(9, 9, "BLOCK_END"),
        )
        val json = """{"schema":"hs-power-evidence/0.3","source_session":"session-a","match":{"index":1,"events":[${events.joinToString()}]}}"""
        val (source, document) = EvidenceAdapters.powerDocument(json.toByteArray())

        val reduced = PowerEvidenceReducer.reduce(source, document)

        assertEquals(listOf("PLAY", "GAME_RESET", "PLAY"), reduced.events.map { it.kind })
        assertEquals(listOf(0, 1, 1), reduced.events.map { it.chronologyEpoch })
        assertEquals(
            listOf(ChronologyState.HISTORICAL, ChronologyState.CURRENT, ChronologyState.CURRENT),
            reduced.events.map { it.chronologyState },
        )
        assertEquals(null, reduced.events.last().activeController)
        assertEquals(null, reduced.events.last().playerTurnIndex)
        assertEquals(null, reduced.events.last().rawTurn)
        assertTrue(reduced.snapshots.filter { it.chronologyEpoch == 0 }.all { it.chronologyState == ChronologyState.HISTORICAL })
        assertTrue(reduced.snapshots.filter { it.chronologyEpoch == 1 }.all { it.chronologyState == ChronologyState.CURRENT })
        assertEquals(SnapshotStateMode.FULL, reduced.snapshots.first { it.chronologyEpoch == 1 }.stateMode)
        assertTrue(reduced.snapshots.filter { it.chronologyEpoch == 1 }.all { "83" !in it.entityTags })
        val historicalIds = reduced.events.filter { it.chronologyState == ChronologyState.HISTORICAL }.map { it.id }.toSet()
        assertTrue(reduced.events.filter { it.chronologyState == ChronologyState.CURRENT }.none { it.parentEventId in historicalIds })
        assertTrue(reduced.diagnostics.any { it.contains("observed gameplay rewind") })
    }

    @Test fun unclassifiedGameResetDoesNotClaimGameplayRewind() {
        val json = """{"schema":"hs-power-evidence/0.3","source_session":"session-a","match":{"index":1,"events":[
            {"sequence":1,"source_line":1,"log_time":"12:00:01.0","raw":"BLOCK_START","block_type":"GAME_RESET","actor":"83"},
            {"sequence":2,"source_line":2,"log_time":"12:00:02.0","raw":"BLOCK_END"}
        ]}}"""
        val (source, document) = EvidenceAdapters.powerDocument(json.toByteArray())

        val reduced = PowerEvidenceReducer.reduce(source, document)

        assertEquals(1, reduced.events.single().chronologyEpoch)
        assertTrue(reduced.diagnostics.any { it.contains("reset cause is unclassified") })
        assertTrue(reduced.diagnostics.none { it.contains("observed gameplay rewind") })
    }

    @Test fun staleRewindTagDoesNotClassifyALaterResetAsGameplayRewind() {
        val json = """{"schema":"hs-power-evidence/0.3","source_session":"session-a","match":{"index":1,"events":[
            {"sequence":1,"source_line":1,"log_time":"12:00:01.0","raw":"tag","entity":"83","tag":"USED_REWIND","value":"1"},
            {"sequence":2,"source_line":2,"log_time":"12:00:02.0","raw":"unrelated"},
            {"sequence":3,"source_line":3,"log_time":"12:00:03.0","raw":"BLOCK_START","block_type":"GAME_RESET","actor":"83"},
            {"sequence":4,"source_line":4,"log_time":"12:00:04.0","raw":"BLOCK_END"}
        ]}}"""
        val (source, document) = EvidenceAdapters.powerDocument(json.toByteArray())

        val reduced = PowerEvidenceReducer.reduce(source, document)

        assertTrue(reduced.diagnostics.any { it.contains("reset cause is unclassified") })
        assertTrue(reduced.diagnostics.none { it.contains("observed gameplay rewind") })
    }
}
