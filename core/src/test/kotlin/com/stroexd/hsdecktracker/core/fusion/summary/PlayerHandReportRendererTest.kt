package com.stroexd.hsdecktracker.core.fusion.summary

import com.stroexd.hsdecktracker.core.fusion.*
import kotlin.test.*

class PlayerHandReportRendererTest {
    private fun fixture(count: Int = 2): FusedMatch {
        val tags = (1..count).associate { "entity$it" to mapOf("ZONE" to "HAND", "CONTROLLER" to "1", "ZONE_POSITION" to "$it") } +
            ("other" to mapOf("ZONE" to "HAND", "CONTROLLER" to "2", "ZONE_POSITION" to "1"))
        val entities = tags.mapValues { (id, values) -> EntityState(id, cardId = Claim("PRIVATE", reason="test"),
            tags = mapOf("COST" to "999"), tagHistory = values.map { (name, value) ->
                TagObservation(1, 1, "", name, value, EvidenceRef("power", 1, rule="power-descriptor-position/1"))
            }) }
        return FusedMatch(FUSION_SCHEMA, "match", fusionVersion="offline-r2/4",
            sources=listOf(FusionSource("power", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod="test")),
            pairing=PairingDecision(PairingStatus.ACCEPTED, "power", listOf(PairingCandidate("power", 100, listOf("test"), evidence=listOf(EvidenceRef("power", 1)))), "test"),
            events=emptyList(), entities=entities, choices=emptyList(),
            snapshots=listOf(FusionSnapshot("checkpoint", 1, "BEFORE_ACTION", entityTags=tags,
                evidence=listOf(EvidenceRef("power", 10)))), entityEpochs=listOf(EntityEpoch(0, entities)))
    }
    private fun render(match: FusedMatch = fixture(), owner: Int = 1) = PlayerHandReportRenderer.render(match, "checkpoint", owner)

    @Test fun selectedPlayerOnlyAndIdentityCostWithheldWithoutMutation() {
        val input = fixture()
        val before = FusionArtifactCodec.encode(input)
        val report = render(input)
        assertTrue(report.indexOf("entity1") < report.indexOf("entity2"))
        assertFalse(report.contains("Entity reference: other"))
        assertFalse(report.contains("PRIVATE")); assertFalse(report.contains("999"))
        assertTrue(report.contains("CALLER_SELECTED")); assertTrue(report.contains("Card identity: UNKNOWN"))
        assertTrue(report.contains("HAND&#95;SLOT&#95;SUPPLEMENTAL&#95;DESCRIPTOR"))
        assertEquals(before, FusionArtifactCodec.encode(input))
        val second = render(input, 2)
        assertTrue(second.contains("Entity reference: other")); assertFalse(second.contains("Entity reference: entity1"))
    }
    @Test fun opponentProblemsCannotSuppressPlayerAndEmptyIsNotEmptyHand() {
        val input = fixture()
        val snapshot = input.snapshots.single()
        val changed = input.copy(snapshots=listOf(snapshot.copy(entityTags=snapshot.entityTags +
            ("other" to (snapshot.entityTags.getValue("other") + ("ZONE_POSITION" to "bad"))))))
        assertEquals(render(input), render(changed))
        val empty = render(input.copy(entityEpochs=emptyList()))
        assertTrue(empty.contains("ENTITY&#95;EPOCH&#95;ARCHIVE&#95;UNAVAILABLE"))
        assertTrue(empty.contains("No evidenced slots")); assertTrue(empty.contains("Hand completeness: UNKNOWN"))
    }
    @Test fun duplicateAndGapWarningsArePreserved() {
        val input = fixture()
        fun at(position: String): FusedMatch {
            val snapshot = input.snapshots.single()
            val archive = input.entityEpochs.single()
            return input.copy(snapshots=listOf(snapshot.copy(entityTags=snapshot.entityTags +
                ("entity2" to (snapshot.entityTags.getValue("entity2") + ("ZONE_POSITION" to position))))),
                entityEpochs=listOf(archive.copy(entities=archive.entities + ("entity2" to archive.entities.getValue("entity2").copy(
                    tagHistory=archive.entities.getValue("entity2").tagHistory.map { if(it.name=="ZONE_POSITION") it.copy(value=position) else it })))))
        }
        assertTrue(render(at("1")).contains("DUPLICATE&#95;HAND&#95;POSITIONS"))
        assertTrue(render(at("3")).contains("HAND&#95;POSITION&#95;GAPS"))
    }
    @Test fun escapedDataCannotCreateMarkdownOrControlInstructions() {
        val report = render(fixture().copy(matchId="<script>\n&#42;\u202e", diagnostics=listOf("not rendered")))
        assertFalse(report.contains("<script>")); assertTrue(report.contains("&#60;script&#62;\\u000a&#38;&#35;42&#59;\\u202e"))
        val input = fixture()
        val malicious = input.copy(snapshots=listOf(input.snapshots.single().copy(phase="## bad\n")))
        assertFalse(render(malicious).contains("## bad\n"))
    }
    @Test fun validationCancellationAndSizeLimitsAreExplicit() {
        val input = fixture()
        assertFailsWith<IllegalArgumentException> { render(input, 0) }
        assertFailsWith<IllegalArgumentException> { render(input.copy(schema="unsupported")) }
        assertFailsWith<IllegalArgumentException> { render(input.copy(pairing=input.pairing.copy(status=PairingStatus.PENDING))) }
        assertFailsWith<IllegalStateException> { PlayerHandReportRenderer.render(input, "missing", 1) }
        assertFailsWith<IllegalArgumentException> { PlayerHandReportRenderer.render(input, "checkpoint", 1, maxChars=10) }
        val length = render(input).length
        assertEquals(render(input), PlayerHandReportRenderer.render(input, "checkpoint", 1, maxChars=length))
        assertFailsWith<IllegalArgumentException> { PlayerHandReportRenderer.render(input, "checkpoint", 1, maxChars=length-1) }
        var calls=0
        assertFailsWith<java.util.concurrent.CancellationException> {
            PlayerHandReportRenderer.render(input, "checkpoint", 1) { if (++calls > 20) throw java.util.concurrent.CancellationException() }
        }
        assertTrue(render(fixture(100), 1).contains("entity100"))
    }
}
