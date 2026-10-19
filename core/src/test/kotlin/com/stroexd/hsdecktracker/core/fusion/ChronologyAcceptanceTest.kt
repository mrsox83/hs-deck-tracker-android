package com.stroexd.hsdecktracker.core.fusion

import kotlin.test.*

class ChronologyAcceptanceTest {
    @Test fun archiveGapsAndWrongEpochAlignmentAreRejected() {
        val input = match(emptyMap(), listOf(EntityEpoch(0, emptyMap()), EntityEpoch(2, emptyMap())))
        assertTrue(FusionProvenanceValidator.validate(input).any { it.contains("cover every chronology epoch") })
        val wrong = match(emptyMap(), listOf(EntityEpoch(0, emptyMap()), EntityEpoch(1, emptyMap())))
            .copy(events = listOf(CanonicalEvent("e", 1, 1, "PLAY", evidence = listOf(EvidenceRef("power", 1)))))
        assertTrue(FusionProvenanceValidator.validate(wrong).any { it.contains("cover every chronology epoch") })
    }
    private val source = FusionSource("power", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod = "test")
    private fun match(entities: Map<String, EntityState>, archives: List<EntityEpoch>) = FusedMatch(
        schema = FUSION_SCHEMA, matchId = "test", fusionVersion = "offline-r2/3", sources = listOf(source),
        pairing = PairingDecision(PairingStatus.PENDING, candidates = emptyList(), reason = "test"),
        events = emptyList(), entities = entities, choices = emptyList(), snapshots = emptyList(), entityEpochs = archives,
    )

    @Test fun futureRevealAndOtherEpochCannotResolveEarlierIdentity() {
        val entity = EntityState("7", identityHistory = listOf(
            IdentityObservation("EARLIER", EvidenceRef("power", 10)),
            IdentityObservation("LATER", EvidenceRef("power", 30)),
        ))
        val input = match(mapOf("7" to entity), listOf(EntityEpoch(0, mapOf("7" to entity))))
        assertNull(HistoricalIdentity.cardIdAt(input, "7", 0, "power", 9))
        assertEquals("EARLIER", HistoricalIdentity.cardIdAt(input, "7", 0, "power", 20))
        assertEquals("LATER", HistoricalIdentity.cardIdAt(input, "7", 0, "power", 30))
        assertNull(HistoricalIdentity.cardIdAt(input, "7", 1, "power", 30))
        assertNull(HistoricalIdentity.cardIdAt(input, "7", 0, "other", 30))
    }

    @Test fun invalidArchivedProvenanceIsRejectedEvenWhenCurrentEntitiesAreEmpty() {
        val bad = EntityState("7", identityHistory = listOf(IdentityObservation("CARD", EvidenceRef("missing", 10))))
        val input = match(emptyMap(), listOf(EntityEpoch(0, mapOf("7" to bad)), EntityEpoch(1, emptyMap())))
        assertTrue(FusionProvenanceValidator.validate(input).any { it.contains("entityEpochs[0]") && it.contains("unknown source") })
    }
}
