package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.stats.MatchResult
import java.util.zip.ZipFile
import kotlin.test.*

/** Private evidence stays in the supplied ZIP, never copied into repository fixtures. */
class RealCoachingHandPackageTest {
    @Test fun motherSequenceFromPinnedRawSourceHasOrderedNamedCostsAndActionLinks() {
        val started = System.nanoTime()
        val rawBytes = ZipFile(checkNotNull(System.getProperty("hsHandReviewPackage"))).use { zip ->
            val entry = zip.entries().asSequence().single { it.name.endsWith("/evidence/rat_waste_rogue_diagnostics_original.zip") }
            require(entry.size in 1..10_000_000)
            zip.getInputStream(entry).use { it.readBytes() }
        }
        assertEquals("9392b19815be64f0e053b1edc50933d5bd06749a311369ad0e09da4b21bd8d69", EvidenceAdapters.sha256(rawBytes))
        val pair = EvidenceAdapters.exporterBundle(rawBytes).matches.single()
        val reduced = PowerEvidenceReducer.reduce(pair.first, pair.second)
        // Explicit fixture pairing, not an assertion of automatic saved-game matching/player mapping.
        val tracker = TrackerEvidence(FusionSource("tracker:test", "test", SourceType.ANDROID_TRACKER_JSON, captureMethod = "fixture"),
            MatchRecord(id = "private-hand-review", timestamp = 1L, result = MatchResult.WIN))
        val decision = PairingDecision(PairingStatus.ACCEPTED, reduced.source.id,
            listOf(PairingCandidate(reduced.source.id, 1, listOf("Pinned private raw fixture"), evidence = reduced.snapshots.first().evidence)), "Explicit fixture")
        val fused = FusionCoordinator.assemble(tracker, listOf(reduced), decision)
        val history = assertNotNull(fused.handHistory)
        val before = history.checkpoints.single { it.phase == "BEFORE_ACTION" && it.evidence.any { e -> e.sourceLine == 34937 } }
        val cards = before.hands.single { it.controller == 2 }.cards
        assertEquals(listOf("38", "53", "44", "126", "48", "46", "234", "40"), cards.map { it.entityId })
        assertEquals((1..8).toList(), cards.map { it.position })
        assertEquals(listOf(9, 7, 7, 7, 3, 1, 2, 9), cards.map { it.recordedCost })
        assertTrue(cards.all { !it.cardId.isNullOrBlank() }, "Own-side identities need source evidence")
        val action = fused.events.single { it.id == before.actionEventId }
        assertEquals("40", action.actorEntityId)
        assertEquals("44", action.targetEntityId)
        assertEquals(9, before.actorCardBeforeAction?.recordedCost)
        assertEquals(3, before.targetCardBeforeAction?.position)
        val after = history.checkpoints.single { it.phase == "AFTER_OUTER_ACTION" && it.actionEventId == action.id }
        val costs = after.hands.single { it.controller == 2 }.cards.associate { it.entityId to it.recordedCost }
        assertEquals(3, costs["53"])
        assertEquals(2, costs["44"])
        assertEquals(7 to 3, after.costChanges.single { it.entityId == "53" }.let { it.costBefore to it.costAfter })
        assertEquals(7 to 2, after.costChanges.single { it.entityId == "44" }.let { it.costBefore to it.costAfter })
        assertFalse(cards.any { it.entityId == "67" })
        assertEquals(history, FusionArtifactCodec.decode(FusionArtifactCodec.encode(fused)).handHistory)
        println("HAND_EXPORT eightSlots=true identities=true costs14to5=true actionLinked=true elapsedMillis=${(System.nanoTime()-started)/1_000_000}")
    }
}
