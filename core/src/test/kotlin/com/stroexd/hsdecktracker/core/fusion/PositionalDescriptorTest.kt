package com.stroexd.hsdecktracker.core.fusion

import kotlinx.serialization.json.*
import kotlin.test.*

class PositionalDescriptorTest {
    private fun descriptor(id: String = "38", position: String = "1", owner: String = "2") =
        "[entityName=Unknown id=$id zone=HAND zonePos=$position cardId=PRIVATE player=$owner]"

    private fun change(text: String, tag: String = "NUM_TURNS_IN_HAND", value: String = "3") = buildJsonObject {
        put("raw", "TAG_CHANGE Entity=$text tag=$tag value=$value")
        put("entity", text); put("tag", tag); put("value", value)
    }

    private fun reduce(vararg events: JsonObject): ReducedPowerMatch {
        val document = buildJsonObject {
            putJsonObject("match") {
                put("index", 1); put("started_log_time", "12:00:00"); put("completed", false)
                put("events", JsonArray(events.mapIndexed { i, e -> JsonObject(e + mapOf(
                    "sequence" to JsonPrimitive(i + 1), "source_line" to JsonPrimitive(i + 1), "log_time" to JsonPrimitive("12:00:00"))) }))
            }
        }
        return PowerEvidenceReducer.reduce(FusionSource("power", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod = "test"), document)
    }

    @Test fun explicitPositionsHaveExactProvenanceWithoutPrivateIdentityOrCost() {
        val result = reduce(change(descriptor()))
        val e = result.entities.getValue("38")
        assertEquals("HAND", e.tags["ZONE"]); assertEquals("1", e.tags["ZONE_POSITION"]); assertEquals("2", e.tags["CONTROLLER"])
        assertNull(e.cardId.value); assertTrue(e.identityHistory.isEmpty()); assertNull(e.tags["COST"])
        val proofs = e.tagHistory.filter { it.name in setOf("ZONE", "ZONE_POSITION", "CONTROLLER") }
        assertEquals(3, proofs.size)
        assertTrue(proofs.all { it.sourceLine == 1 && it.evidence.sourceLine == 1 && it.evidence.sourceId == "power" && it.evidence.rule == "power-descriptor-position/1" })
    }

    @Test fun namedTagOverridesPrechangeDescriptorWithoutArtificialConflict() {
        for ((tag, value) in listOf("ZONE" to "PLAY", "ZONE_POSITION" to "0", "CONTROLLER" to "1")) {
            val e = reduce(change(descriptor(), tag, value)).entities.getValue("38")
            assertEquals(value, e.tags[tag])
            assertEquals(listOf(value), e.tagHistory.filter { it.name == tag }.map { it.value })
        }
    }

    @Test fun incompleteMalformedOrUnattributedDescriptorsNeverFillPositions() {
        for (text in listOf("[entityName=Unknown id=38 zone=HAND]", descriptor(position = "-1"),
            descriptor(position = "999999999999999999"), descriptor(owner = "0"), descriptor(owner = "unknown"))) {
            val e = reduce(change(text)).entities.getValue("38")
            assertNull(e.tags["ZONE_POSITION"]); assertNull(e.tags["CONTROLLER"]); assertNull(e.tags["ZONE"])
            assertEquals("3", e.tags["NUM_TURNS_IN_HAND"])
        }
        val mismatched = change(descriptor()).let { JsonObject(it + ("raw" to JsonPrimitive("unrelated raw"))) }
        assertNull(reduce(mismatched).entities.getValue("38").tags["ZONE_POSITION"])
    }

    @Test fun resetNeedsNewObservationAndLaterPositionsDoNotRewriteEarlierSnapshots() {
        val reset = buildJsonObject { put("raw", "BLOCK_START BlockType=GAME_RESET Entity=0"); put("block_type", "GAME_RESET") }
        val end = buildJsonObject { put("raw", "BLOCK_END") }
        val result = reduce(change(descriptor()), reset, end, change(descriptor(position = "3", owner = "1")))
        val old = result.entityEpochs.single { it.chronologyEpoch == 0 }.entities.getValue("38")
        assertEquals("1", old.tags["ZONE_POSITION"]); assertEquals("2", old.tags["CONTROLLER"])
        val before = result.snapshots.first { it.chronologyEpoch == 1 }
        assertFalse("38" in before.entityTags)
        assertEquals("3", result.entities.getValue("38").tags["ZONE_POSITION"])
        assertTrue(result.entities.getValue("38").tagHistory.all { it.sourceLine == 4 })
    }

    @Test fun laterDescriptorCannotReplaceExplicitTagsAndDisagreementIsDisclosed() {
        val result = reduce(change(descriptor(), "ZONE", "PLAY"), change(descriptor(position = "0"), "ZONE_POSITION", "2"),
            change(descriptor(position = "3")))
        val e = result.entities.getValue("38")
        assertEquals("PLAY", e.tags["ZONE"]); assertEquals("2", e.tags["ZONE_POSITION"])
        assertTrue(result.diagnostics.any { it.contains("disagree with explicit ZONE tags") })
        assertTrue(result.diagnostics.any { it.contains("disagree with explicit ZONE_POSITION tags") })
        assertEquals(listOf("2"), e.tagHistory.filter { it.name == "ZONE_POSITION" && it.sourceLine >= 2 }.map { it.value })
    }
}
