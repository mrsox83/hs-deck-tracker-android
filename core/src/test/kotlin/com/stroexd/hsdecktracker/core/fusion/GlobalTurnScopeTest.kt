package com.stroexd.hsdecktracker.core.fusion

import kotlinx.serialization.json.*
import kotlin.test.*

class GlobalTurnScopeTest {
    private fun reduce(vararg lines: String): ReducedPowerMatch {
        val events = lines.mapIndexed { i, raw -> buildJsonObject {
            put("sequence", i + 1); put("source_line", i + 1); put("log_time", "00:00:00"); put("raw", raw)
            Regex("TAG_CHANGE Entity=(.*?) tag=(\\S+) value=(.*)").matchEntire(raw)?.let {
                put("entity", it.groupValues[1]); put("tag", it.groupValues[2]); put("value", it.groupValues[3])
            }
            if (raw.startsWith("BLOCK_START")) put("block_type", if (raw.contains("GAME_RESET")) "GAME_RESET" else "PLAY")
        } }
        return PowerEvidenceReducer.reduce(FusionSource("p", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod = "test"),
            buildJsonObject { putJsonObject("match") { put("events", JsonArray(events)) } })
    }
    private val play = "BLOCK_START BlockType=PLAY Entity=0"

    @Test fun bothStartingSidesKeepGlobalAndPersonalCountersSeparate() {
        for (side in listOf(1, 2)) {
            val r = reduce("GameEntity EntityID=7", "Player EntityID=3 PlayerID=$side",
                "TAG_CHANGE Entity=7 tag=TURN value=1", "TAG_CHANGE Entity=3 tag=CURRENT_PLAYER value=1",
                "TAG_CHANGE Entity=GameEntity tag=TURN value=18", "TAG_CHANGE Entity=3 tag=TURN value=9", play, "BLOCK_END")
            assertEquals(18, r.events.single().rawTurn)
            assertEquals(side, r.events.single().activeController)
            assertEquals("9", r.entities.getValue("3").tags["TURN"])
            assertEquals(1, r.events.single().playerTurnIndex) // Existing signal-based policy, not personal TURN.
            assertEquals(1, r.snapshots.first { it.phase == "TURN_SIGNAL" }.rawTurn) // No future TURN backfill.
        }
    }
    @Test fun playerOrGameNameWithoutDeclarationCannotSupplyGlobalCounter() {
        val r = reduce("Player EntityID=2 PlayerID=1", "TAG_CHANGE Entity=2 tag=TURN value=9",
            "TAG_CHANGE Entity=GameEntity tag=TURN value=18", play, "BLOCK_END")
        assertNull(r.events.single().rawTurn)
    }
    @Test fun inlineGameCounterIsAcceptedButInlinePlayerCounterIsNotGlobal() {
        val r = reduce("GameEntity EntityID=7", "tag=TURN value=18", "Player EntityID=2 PlayerID=1",
            "tag=TURN value=9", play, "BLOCK_END")
        assertEquals(18, r.events.single().rawTurn)
    }
    @Test fun resetRequiresFreshDeclarationAndDoesNotBackfillEarlierAction() {
        val r = reduce("GameEntity EntityID=7", "TAG_CHANGE Entity=GameEntity tag=TURN value=18",
            "BLOCK_START BlockType=GAME_RESET Entity=0", "BLOCK_END",
            "TAG_CHANGE Entity=GameEntity tag=TURN value=19", play, "BLOCK_END",
            "GameEntity EntityID=8", "TAG_CHANGE Entity=8 tag=TURN value=3", play, "BLOCK_END")
        assertNull(r.events[1].rawTurn)
        assertEquals(3, r.events[2].rawTurn)
        assertEquals(18, r.entityEpochs.first().entities.getValue("7").tags["TURN"]?.toInt())
    }
    @Test fun conflictingGameDeclarationsAndInvalidTurnStayUnknown() {
        val r = reduce("GameEntity EntityID=7", "TAG_CHANGE Entity=GameEntity tag=TURN value=18",
            "GameEntity EntityID=8", "TAG_CHANGE Entity=7 tag=TURN value=19", play, "BLOCK_END")
        assertNull(r.events.single().rawTurn)
        val invalid = reduce("GameEntity EntityID=7", "TAG_CHANGE Entity=7 tag=TURN value=-1", play, "BLOCK_END")
        assertNull(invalid.events.single().rawTurn)
    }
    @Test fun unknownOpponentAndRepeatedSignalsDoNotInventPersonalIndex() {
        val r = reduce("GameEntity EntityID=7", "Player EntityID=2 PlayerID=1",
            "TAG_CHANGE Entity=7 tag=TURN value=18", "TAG_CHANGE Entity=unknown tag=CURRENT_PLAYER value=1",
            "TAG_CHANGE Entity=2 tag=CURRENT_PLAYER value=1", "TAG_CHANGE Entity=2 tag=CURRENT_PLAYER value=1", play, "BLOCK_END")
        assertEquals(18, r.events.single().rawTurn)
        assertNull(r.events.single().playerTurnIndex)
        assertEquals(1, r.diagnostics.count { it.startsWith("Turn owner unresolved") })
    }
}
