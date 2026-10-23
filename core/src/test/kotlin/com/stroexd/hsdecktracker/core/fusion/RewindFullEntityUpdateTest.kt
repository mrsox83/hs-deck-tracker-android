package com.stroexd.hsdecktracker.core.fusion

import kotlinx.serialization.json.*
import kotlin.test.*

class RewindFullEntityUpdateTest {
    private fun reduce(vararg lines: String): ReducedPowerMatch {
        val events = lines.mapIndexed { index, raw -> buildJsonObject {
            put("raw", raw); put("sequence", index + 1); put("source_line", index + 1); put("log_time", "00:00:00")
            if (raw.startsWith("BLOCK_START")) put("block_type", if (raw.contains("GAME_RESET")) "GAME_RESET" else "PLAY")
        } }
        val document = buildJsonObject { putJsonObject("match") { put("events", JsonArray(events)) } }
        return PowerEvidenceReducer.reduce(FusionSource("power", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod = "test"), document)
    }

    @Test fun restorationReadsFreshCostsWithoutCarryingEarlierEpochState() {
        val result = reduce("FULL_ENTITY - Creating ID=44 CardID=OLD", "tag=COST value=99", "tag=DAMAGE value=5",
            "BLOCK_START BlockType=GAME_RESET Entity=0", "BLOCK_END",
            "FULL_ENTITY - Updating [entityName=Card id=44 zone=HAND zonePos=1 cardId=NEW player=2] CardID=NEW",
            "tag=CONTROLLER value=2", "tag=COST value=7", "tag=ZONE value=HAND", "tag=ZONE_POSITION value=1")
        val current = result.entities.getValue("44")
        assertEquals("7", current.tags["COST"])
        assertNull(current.tags["DAMAGE"])
        assertEquals("99", result.entityEpochs.single { it.chronologyEpoch == 0 }.entities.getValue("44").tags["COST"])
        assertEquals("NEW", current.identityHistory.single().cardId)
        assertEquals(6, current.identityHistory.single().evidence.sourceLine)
        assertEquals(8, current.tagHistory.single { it.name == "COST" }.sourceLine)
    }

    @Test fun unsupportedAndAmbiguousHeadersClearRecipientInsteadOfChangingPreviousCard() {
        for (header in listOf("FULL_ENTITY - Updating UnresolvedPlayer CardID=",
            "FULL_ENTITY - Updating [id=44 id=53] CardID=BAD", "FULL_ENTITY - malformed")) {
            val result = reduce("FULL_ENTITY - Creating ID=44 CardID=A", "tag=COST value=7", header, "tag=COST value=0")
            assertEquals("7", result.entities.getValue("44").tags["COST"])
            assertEquals(setOf("44"), result.entities.keys)
            assertTrue(result.diagnostics.any { it.contains("Unassigned FULL_ENTITY header at source line 3") })
        }
    }

    @Test fun consecutiveUpdatesAssignInlineTagsToTheirExplicitInstance() {
        val result = reduce("FULL_ENTITY - Updating [entityName=A id=44 zone=HAND zonePos=1 cardId=A player=2] CardID=A", "tag=COST value=2",
            "FULL_ENTITY - Updating [entityName=B id=53 zone=HAND zonePos=2 cardId=B player=2] CardID=B", "tag=COST value=3")
        assertEquals("2", result.entities.getValue("44").tags["COST"])
        assertEquals("3", result.entities.getValue("53").tags["COST"])
    }
}
