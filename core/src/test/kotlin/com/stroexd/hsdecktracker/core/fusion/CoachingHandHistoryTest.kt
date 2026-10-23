package com.stroexd.hsdecktracker.core.fusion

import java.util.concurrent.CancellationException
import kotlin.test.*
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.serialization.json.jsonObject

class CoachingHandHistoryTest {
    @Test fun codecRejectsFabricatedDerivedCardsValuesAndUnrelatedActionsOnEncodeAndDecode() {
        val original = fixture()
        val history = CoachingHandHistoryBuilder.build(original)
        val point = history.checkpoints.first()
        val card = point.hands.single().cards.single()
        val fabricated = card.copy(entityId = "invented", positionEvidence = emptyList())
        val unrelated = original.events.single().copy(id = "unrelated", sourceSequence = 99, evidence = listOf(ref(99)))
        val mutations = listOf(
            point.copy(actorCardBeforeAction = fabricated), point.copy(targetCardBeforeAction = fabricated),
            point.copy(actionEventId = "unrelated"),
            point.copy(hands = listOf(point.hands.single().copy(cards = listOf(card.copy(cardId = "FALSE"))))),
            point.copy(hands = listOf(point.hands.single().copy(cards = listOf(card.copy(recordedCost = 99))))),
            point.copy(hands = listOf(point.hands.single().copy(cards = listOf(card.copy(positionEvidence = listOf(ref(999))))))),
            point.copy(hands = listOf(point.hands.single().copy(cards = listOf(card.copy(identityEvidence = listOf(EvidenceRef("other", 1))))))),
        )
        for (changedPoint in mutations) {
            val changed = original.copy(events = original.events + unrelated,
                handHistory = history.copy(checkpoints = listOf(changedPoint) + history.checkpoints.drop(1)))
            assertFailsWith<FusionArtifactException> { FusionArtifactCodec.encode(changed) }
            val unvalidated = AppJson.encodeToString(FusedMatch.serializer(), changed)
            assertFailsWith<FusionArtifactException> { FusionArtifactCodec.decode(unvalidated) }
        }
    }
    @Test fun canonicalTagDescriptorPreservesIdentityAtItsLineOnly() {
        val document = AppJson.parseToJsonElement("""{"match":{"events":[
            {"sequence":1,"source_line":10,"raw":"TAG_CHANGE Entity=[entityName=Test id=44 zone=HAND zonePos=1 cardId=TEST_CARD player=2] tag=NUM_TURNS_IN_HAND value=1","entity":"[entityName=Test id=44 zone=HAND zonePos=1 cardId=TEST_CARD player=2]","tag":"NUM_TURNS_IN_HAND","value":"1"}
        ]}}""").jsonObject
        val source = FusionSource("power", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod = "test")
        val reduced = PowerEvidenceReducer.reduce(source, document)
        val history = reduced.entities.getValue("44").identityHistory
        assertEquals("TEST_CARD", history.single().cardId)
        assertEquals(10, history.single().evidence.sourceLine)
        assertEquals("power-descriptor-identity/1", history.single().evidence.rule)
        assertFalse(reduced.entities.getValue("44").tags.containsKey("COST"))
    }
    private fun ref(line: Int) = EvidenceRef("power", line)
    private fun fixture(): FusedMatch {
        fun observed(name: String, value: String, line: Int) = TagObservation(line, line, "", name, value, ref(line))
        val initial = mapOf("ZONE" to "HAND", "CONTROLLER" to "2", "ZONE_POSITION" to "1", "COST" to "7")
        val entity = EntityState("44", tagHistory = initial.map { observed(it.key, it.value, 1) } + observed("COST", "2", 11),
            identityHistory = listOf(IdentityObservation("UMBRA", ref(1)), IdentityObservation("FUTURE", ref(30))))
        return FusedMatch(FUSION_SCHEMA, "match", fusionVersion = "test",
            sources = listOf(FusionSource("power", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod = "test")),
            pairing = PairingDecision(PairingStatus.ACCEPTED, "power", listOf(PairingCandidate("power", 1, emptyList(), evidence = listOf(ref(1)))), "test"),
            events = listOf(CanonicalEvent("play", 1, 10, "PLAY", "40", "44", evidence = listOf(ref(10)))),
            entities = mapOf("44" to entity), choices = emptyList(),
            snapshots = listOf(FusionSnapshot("before", 10, "BEFORE_ACTION", entityTags = mapOf("44" to initial), evidence = listOf(ref(10))),
                FusionSnapshot("after", 12, "AFTER_OUTER_ACTION", stateMode = SnapshotStateMode.DELTA,
                    entityTags = mapOf("44" to mapOf("COST" to "2")), evidence = listOf(ref(12)))),
            entityEpochs = listOf(EntityEpoch(0, mapOf("44" to entity))))
    }

    @Test fun exportsIdentityPositionCostAndExactOuterActionWithoutFutureIdentity() {
        val match = fixture()
        val history = CoachingHandHistoryBuilder.build(match)
        assertEquals(listOf(7, 2), history.checkpoints.map { it.hands.single().cards.single().recordedCost })
        assertEquals(listOf("UMBRA", "UMBRA"), history.checkpoints.map { it.hands.single().cards.single().cardId })
        assertEquals(listOf("play", "play"), history.checkpoints.map { it.actionEventId })
        assertEquals("44", history.checkpoints.first().targetCardBeforeAction?.entityId)
        assertEquals(listOf(7 to 2), history.checkpoints.last().costChanges.map { it.costBefore to it.costAfter })
        assertEquals(listOf(2, 2), history.checkpoints.map { it.hands.single().controller })
        val serialized = FusionArtifactCodec.encode(match.copy(handHistory = history))
        assertEquals(history, FusionArtifactCodec.decode(serialized).handHistory)
    }

    @Test fun unknownMissingCostsConflictsAndInvalidPositionsAreNotFilledFromFinalCatalog() {
        val match = fixture()
        val entity = match.entityEpochs.single().entities.getValue("44")
        val conflicting = entity.copy(identityHistory = listOf(IdentityObservation("A", ref(1)), IdentityObservation("B", ref(1))),
            tagHistory = entity.tagHistory + TagObservation(1, 1, "", "COST", "99", ref(1)))
        val changed = match.copy(entityEpochs = listOf(EntityEpoch(0, mapOf("44" to conflicting))))
        val card = CoachingHandHistoryBuilder.build(changed).checkpoints.first().hands.single().cards.single()
        assertNull(card.cardId)
        assertNull(card.recordedCost)
        val missing = match.copy(entityEpochs = emptyList())
        assertTrue(CoachingHandHistoryBuilder.build(missing).checkpoints.all { it.hands.flatMap { side -> side.cards }.isEmpty() })
    }

    @Test fun resetDoesNotCarryOldHandOrPendingActionAndDeltaWithoutNewBaselineFails() {
        val match = fixture()
        val newPoint = FusionSnapshot("reset", 20, "AFTER_OUTER_ACTION", chronologyEpoch = 1,
            entityTags = emptyMap(), evidence = listOf(ref(20)))
        val history = CoachingHandHistoryBuilder.build(match.copy(snapshots = match.snapshots.take(1) + newPoint))
        assertNull(history.checkpoints.last().actionEventId)
        assertTrue(history.checkpoints.last().hands.isEmpty())
        assertFails { CoachingHandHistoryBuilder.build(match.copy(snapshots = match.snapshots.take(1) +
            newPoint.copy(stateMode = SnapshotStateMode.DELTA))) }
        assertFailsWith<CancellationException> { CoachingHandHistoryBuilder.build(match) { throw CancellationException() } }
    }
}
