package com.stroexd.hsdecktracker.core.fusion

import kotlin.test.*

class DecisionHandViewTest {
    @Test fun supplementalDescriptorSupportIsVisibleToConsumers() {
        val input = fixture()
        val archive = input.entityEpochs.single()
        val changed = input.copy(entityEpochs = listOf(archive.copy(entities = archive.entities.mapValues { (_, entity) ->
            entity.copy(tagHistory = entity.tagHistory.map { it.copy(evidence = it.evidence.copy(rule = "power-descriptor-position/1")) })
        })))
        val result = DecisionHandProjector.project(changed, "before")
        assertTrue(result.hands[0].diagnostics.contains("HAND_SLOT_SUPPLEMENTAL_DESCRIPTOR:a"))
        assertEquals(listOf("b", "a"), result.hands[0].slots.map { it.entityId })
    }
    private val source = FusionSource("power", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod = "test")
    private fun entity(id: String, owner: Int, position: Int) = EntityState(id, tagHistory = listOf(
        "ZONE" to "HAND", "CONTROLLER" to "$owner", "ZONE_POSITION" to "$position",
    ).map { (name, value) -> TagObservation(1, 1, "", name, value, EvidenceRef("power", 1)) })
    private fun fixture() = FusedMatch(FUSION_SCHEMA, "test", fusionVersion = "offline-r2/4", sources = listOf(source),
        pairing = PairingDecision(PairingStatus.ACCEPTED, "power", emptyList(), "fixture"), events = emptyList(),
        entities = emptyMap(), choices = emptyList(), snapshots = listOf(FusionSnapshot("before", 1, "BEFORE_ACTION",
            entityTags = mapOf("a" to mapOf("ZONE" to "HAND", "CONTROLLER" to "1", "ZONE_POSITION" to "2"),
                "b" to mapOf("ZONE" to "HAND", "CONTROLLER" to "1", "ZONE_POSITION" to "1"),
                "c" to mapOf("ZONE" to "HAND", "CONTROLLER" to "2", "ZONE_POSITION" to "1")),
            evidence = listOf(EvidenceRef("power", 10)))),
        entityEpochs = listOf(EntityEpoch(0, mapOf("a" to entity("a", 1, 2), "b" to entity("b", 1, 1), "c" to entity("c", 2, 1)))))

    @Test fun bothHandsPreserveUnknownSlotsAndOriginalIsUnchanged() {
        val input = fixture()
        val result = DecisionHandProjector.project(input, "before")
        assertEquals(listOf("b", "a"), result.hands[0].slots.map { it.entityId })
        assertEquals(listOf("c"), result.hands[1].slots.map { it.entityId })
        assertTrue(result.hands.flatMap { it.slots }.all { it.cardId == null && it.effectiveCost == null && it.evidence.isNotEmpty() })
        assertTrue(result.hands.all { it.complete == null && it.expectedHandSize == null })
        assertEquals(fixture(), input)
    }
    @Test fun laterAndHiddenIdentitiesNeverFillEarlierSlots() {
        val input = fixture()
        val changed = input.copy(entityEpochs = input.entityEpochs.map { archive -> archive.copy(entities = archive.entities.mapValues { (_, e) ->
            e.copy(cardId = Claim("LATE", reason = "later"), identityHistory = listOf(
                IdentityObservation("HIDDEN", EvidenceRef("power", 2)), IdentityObservation("LATE", EvidenceRef("power", 30))),
                tags = mapOf("COST" to "0"))
        }) })
        assertEquals(DecisionHandProjector.project(input, "before"), DecisionHandProjector.project(changed, "before"))
    }
    @Test fun transitionsRequireFullAndUnknownArchivesDoNotInventSlots() {
        val input = fixture()
        val extra = input.snapshots.single().copy(id = "after", stateMode = SnapshotStateMode.DELTA, chronologyEpoch = 1)
        assertFailsWith<IllegalArgumentException> { DecisionHandProjector.project(input.copy(snapshots = input.snapshots + extra), "after") }
        assertTrue(DecisionHandProjector.project(input.copy(entityEpochs = emptyList()), "before").hands.all { it.slots.isEmpty() })
        assertFailsWith<IllegalStateException> { DecisionHandProjector.project(input.copy(snapshots = input.snapshots + input.snapshots), "before") }
    }
    @Test fun invalidDuplicateAndUnprovenPositionsAreDisclosed() {
        val input = fixture()
        val tags = input.snapshots.single().entityTags.toMutableMap()
        tags["a"] = tags.getValue("a") + ("ZONE_POSITION" to "0")
        val result = DecisionHandProjector.project(input.copy(snapshots = listOf(input.snapshots.single().copy(entityTags = tags))), "before")
        assertEquals(listOf("b"), result.hands[0].slots.map { it.entityId })
        assertTrue(result.hands[0].diagnostics.any { it.contains("HAND_POSITION_INVALID:a") })
    }
    @Test fun cancellationAndUnacceptedFusionAreNotHidden() {
        assertFailsWith<java.util.concurrent.CancellationException> {
            DecisionHandProjector.project(fixture(), "before") { throw java.util.concurrent.CancellationException() }
        }
        assertFailsWith<IllegalArgumentException> {
            DecisionHandProjector.project(fixture().copy(pairing = PairingDecision(PairingStatus.PENDING, candidates = emptyList(), reason = "pending")), "before")
        }
    }

    @Test fun deltaControllerTransferUsesHistoricalOwnerAndPositionEvidence() {
        val input = fixture()
        val first = DecisionHandProjector.project(input, "before")
        val epoch = input.entityEpochs.single()
        val a = epoch.entities.getValue("a")
        val changed = input.copy(entityEpochs = listOf(epoch.copy(entities = epoch.entities + ("a" to a.copy(
            tagHistory = a.tagHistory + TagObservation(11, 11, "", "CONTROLLER", "2", EvidenceRef("power", 11)))))),
            snapshots = input.snapshots + input.snapshots.single().copy(id = "after", stateMode = SnapshotStateMode.DELTA,
                entityTags = mapOf("a" to mapOf("CONTROLLER" to "2")), evidence = listOf(EvidenceRef("power", 11))))
        assertEquals(first, DecisionHandProjector.project(changed, "before"))
        val after = DecisionHandProjector.project(changed, "after")
        assertEquals(listOf("b"), after.hands[0].slots.map { it.entityId })
        assertEquals(listOf("c", "a"), after.hands[1].slots.map { it.entityId })
    }

    @Test fun duplicatePositionsAndSameLineConflictsNeverChooseAnInventedOrder() {
        val input = fixture()
        val epoch = input.entityEpochs.single()
        val a = epoch.entities.getValue("a")
        val duplicate = input.copy(entityEpochs = listOf(epoch.copy(entities = epoch.entities + ("a" to a.copy(
            tagHistory = a.tagHistory.map { if (it.name == "ZONE_POSITION") it.copy(value = "1") else it })))),
            snapshots = listOf(input.snapshots.single().copy(entityTags = input.snapshots.single().entityTags +
                ("a" to (input.snapshots.single().entityTags.getValue("a") + ("ZONE_POSITION" to "1"))))))
        assertTrue(DecisionHandProjector.project(duplicate, "before").hands[0].diagnostics.contains("DUPLICATE_HAND_POSITIONS"))
        val conflict = input.copy(entityEpochs = listOf(epoch.copy(entities = epoch.entities + ("a" to a.copy(
            tagHistory = a.tagHistory + a.tagHistory.last().copy(value = "1"))))))
        val view = DecisionHandProjector.project(conflict, "before")
        assertEquals(listOf("b"), view.hands[0].slots.map { it.entityId })
        assertTrue(view.hands[0].diagnostics.contains("HAND_SLOT_PROVENANCE_UNESTABLISHED:a"))
    }
}
