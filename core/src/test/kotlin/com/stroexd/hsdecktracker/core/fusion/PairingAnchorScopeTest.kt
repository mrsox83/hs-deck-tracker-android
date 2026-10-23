package com.stroexd.hsdecktracker.core.fusion

import kotlin.test.*

class PairingAnchorScopeTest {
    private fun ref(line: Int) = EvidenceRef("power", line)
    private fun entity(card: String, line: Int, owner: Int = 2) = EntityState("44",
        cardId = Claim(card, ClaimStatus.OBSERVED, Confidence.HIGH, "test", listOf(ref(line))),
        identityHistory = listOf(IdentityObservation(card, ref(line))),
        tagHistory = listOf(TagObservation(1, 1, "", "CONTROLLER", "$owner", ref(1))))
    private fun facts(epochs: List<EntityEpoch>, kind: String = "PLAY", controller: Int = 2): PowerPairingFacts {
        val reduced = ReducedPowerMatch(
            source = FusionSource("power", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod = "test"),
            index = 1, startedLogTime = "", completed = true, sourceTruncated = false,
            events = listOf(CanonicalEvent("play", 1, 10, kind, "44", evidence = listOf(ref(10)))),
            entities = epochs.last().entities, choices = emptyList(), snapshots = emptyList(), entityEpochs = epochs)
        return PowerPairingFacts.fromReduced(reduced, null, controller)
    }
    @Test fun laterIdentityCannotBecomeEarlierAnchor() {
        assertTrue(facts(listOf(EntityEpoch(0, mapOf("44" to entity("LATER", 20))))).anchorCardIds.isEmpty())
    }
    @Test fun resetEntityReuseDoesNotReplaceEarlierActorIdentity() {
        val result = facts(listOf(EntityEpoch(0, mapOf("44" to entity("EARLIER", 1))),
            EntityEpoch(1, mapOf("44" to entity("LATER", 20)))))
        assertEquals(setOf("EARLIER"), result.anchorCardIds)
        assertTrue(ref(1) in result.anchorEvidence.getValue("EARLIER"))
        assertTrue(ref(10) in result.anchorEvidence.getValue("EARLIER"))
    }
    @Test fun opponentAndNonPlayActorsDoNotCountAsLocalPlayAnchors() {
        val epoch = EntityEpoch(0, mapOf("44" to entity("CARD", 1, owner = 1)))
        assertTrue(facts(listOf(epoch), controller = 2).anchorCardIds.isEmpty())
        assertTrue(facts(listOf(epoch), kind = "TRIGGER", controller = 1).anchorCardIds.isEmpty())
        assertEquals(setOf("CARD"), facts(listOf(epoch), controller = 1).anchorCardIds)
    }
}
