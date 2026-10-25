package com.stroexd.hsdecktracker.core.fusion

import kotlinx.serialization.json.*
import kotlin.test.*

class RevealRecipientTest {
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

    @Test fun numericRevealsDoNotContaminatePreviousHandCard() {
        val r = reduce("FULL_ENTITY - Creating ID=68 CardID=",
            "tag=CONTROLLER value=1", "tag=ZONE value=HAND", "tag=ZONE_POSITION value=5",
            "SHOW_ENTITY - Updating Entity=57 CardID=CARD_A", "tag=CONTROLLER value=2", "tag=COST value=3",
            "tag=ENTITY_ID value=57", "SHOW_ENTITY - Updating Entity=52 CardID=CARD_B", "tag=COST value=2",
            "CHANGE_ENTITY - Updating Entity=63 CardID=CARD_C", "tag=CONTROLLER value=2", "tag=COST value=1")
        val previous = r.entities.getValue("68")
        assertEquals("1", previous.tags["CONTROLLER"])
        assertEquals("5", previous.tags["ZONE_POSITION"])
        assertNull(previous.tags["COST"])
        assertNull(previous.cardId.value)
        for ((id, card, cost) in listOf(Triple("57", "CARD_A", "3"), Triple("52", "CARD_B", "2"), Triple("63", "CARD_C", "1"))) {
            assertEquals(card, r.entities.getValue(id).cardId.value)
            assertEquals(cost, r.entities.getValue(id).tags["COST"])
        }
        assertEquals(5, r.entities.getValue("57").cardId.evidence.single().sourceLine)
    }

    @Test fun unsupportedRevealHeadersClearInlineRecipient() {
        for (kind in listOf("SHOW_ENTITY", "CHANGE_ENTITY")) for (recipient in listOf(
            "0", "-1", "2147483648", "unknown", "[entityName=A id=7 id=8 zone=HAND]",
            "[entityName=A id=7 entity=8 zone=HAND]", "[entityName=A id=7 zone=HAND", "prefix id=7")) {
            val r = reduce("FULL_ENTITY - Creating ID=68 CardID=OLD", "tag=COST value=4",
                "$kind - Updating Entity=$recipient CardID=NEW", "tag=COST value=99", "tag=CONTROLLER value=2",
                "SHOW_ENTITY - Updating Entity=9 CardID=SAFE", "tag=COST value=1")
            assertEquals("4", r.entities.getValue("68").tags["COST"], recipient)
            assertNull(r.entities.getValue("68").tags["CONTROLLER"], recipient)
            assertEquals(setOf("68", "9"), r.entities.keys, recipient)
            assertEquals("1", r.entities.getValue("9").tags["COST"])
            assertTrue(r.diagnostics.any { it.startsWith("Unassigned reveal header") })
        }
    }

    @Test fun descriptorFormAndEmptyIdentityStillReceiveInlineTags() {
        for (kind in listOf("SHOW_ENTITY", "CHANGE_ENTITY")) for (name in listOf("A", "UNKNOWN ENTITY [cardType=INVALID]")) {
            val r = reduce("$kind - Updating Entity=[entityName=$name id=7 zone=HAND zonePos=2 cardId= player=1] CardID=",
                "tag=CONTROLLER value=1", "tag=COST value=4")
            assertNull(r.entities.getValue("7").cardId.value)
            assertEquals("4", r.entities.getValue("7").tags["COST"])
        }
    }

    @Test fun numericRevealAfterResetDoesNotBackfillPreviousEpoch() {
        val r = reduce("FULL_ENTITY - Creating ID=7 CardID=OLD", "tag=COST value=4",
            "BLOCK_START BlockType=GAME_RESET Entity=0", "BLOCK_END",
            "SHOW_ENTITY - Updating Entity=7 CardID=NEW", "tag=COST value=1")
        assertEquals("OLD", r.entityEpochs.first().entities.getValue("7").cardId.value)
        assertEquals("4", r.entityEpochs.first().entities.getValue("7").tags["COST"])
        assertEquals("NEW", r.entities.getValue("7").cardId.value)
        assertEquals("1", r.entities.getValue("7").tags["COST"])
    }

    @Test fun rejectedRevealCannotRegisterAliasForLaterTags() {
        for (kind in listOf("SHOW_ENTITY", "CHANGE_ENTITY")) {
            val r = reduce("FULL_ENTITY - Creating ID=68 CardID=OLD", "tag=COST value=4",
                "$kind - Updating Entity=[entityName=A id=7 id=8 zone=HAND] CardID=NEW",
                "tag=COST value=99", "TAG_CHANGE Entity=A tag=COST value=99")
            assertEquals(setOf("68"), r.entities.keys)
            assertEquals("4", r.entities.getValue("68").tags["COST"])
        }
    }
}
