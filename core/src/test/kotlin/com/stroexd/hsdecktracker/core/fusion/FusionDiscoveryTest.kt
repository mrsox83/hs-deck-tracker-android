package com.stroexd.hsdecktracker.core.fusion

import java.time.ZoneId
import java.time.Instant
import kotlin.test.*

class FusionDiscoveryTest {
    private fun candidate(id: String, score: Int = 4, rejected: List<String> = emptyList()) =
        PairingCandidate(id, score, listOf("endpoint", "result"), rejected, listOf(EvidenceRef(id, sourceLine = 1)))
    private fun decision(vararg candidates: PairingCandidate) = PairingDecision(
        PairingStatus.ACCEPTED, candidates.first().powerSourceId, candidates.toList(), "existing gate")

    @Test fun completeUniquePreviewIsDeterministicAndDeduplicated() {
        val d = decision(candidate("a"))
        assertEquals(FusionDiscoveryPreview.decide(listOf(d), true), FusionDiscoveryPreview.decide(listOf(d, d), true))
        assertEquals("a", FusionDiscoveryPreview.decide(listOf(d), true).acceptedSourceId)
    }
    @Test fun scoreAdvantageNeverGuessesBetweenPlausibleGames() {
        assertEquals(PairingStatus.CONFLICTED, FusionDiscoveryPreview.decide(listOf(decision(candidate("a", 9), candidate("b"))), true).status)
    }
    @Test fun incompleteAndFailedSearchNeverRecommendFusion() {
        val result = FusionDiscoveryPreview.decide(listOf(decision(candidate("a"))), false)
        assertEquals(PairingStatus.PENDING, result.status)
        assertNull(result.acceptedSourceId)
        assertEquals(1, result.candidates.size)
    }
    @Test fun conflictAndUnevidencedCandidatesStayPending() {
        for (c in listOf(candidate("a", rejected = listOf("result conflicts")), candidate("a").copy(evidence = emptyList())))
            assertEquals(PairingStatus.PENDING, FusionDiscoveryPreview.decide(listOf(decision(c)), true).status)
        assertEquals(PairingStatus.PENDING, FusionDiscoveryPreview.decide(emptyList(), true).status)
    }
    @Test fun rawBudgetRejectsOversizeAndOverflow() {
        assertTrue(FusionDiscoveryPolicy.permitsRawBytes(48L * 1024 * 1024, 16L * 1024 * 1024))
        assertFalse(FusionDiscoveryPolicy.permitsRawBytes(FusionDiscoveryPolicy.MAX_RAW_BYTES, 1))
        assertFalse(FusionDiscoveryPolicy.permitsRawBytes(0, Long.MAX_VALUE))
        assertFalse(FusionDiscoveryPolicy.permitsRawBytes(-1, 1))
    }
    @Test fun folderRankingIsBoundedStableAndNotPairing() {
        val names = (1..9).map { "Hearthstone_2026_10_12_13_${it.toString().padStart(2, '0')}_00" }
        val end = Instant.parse("2026-10-12T18:10:00Z").toEpochMilli()
        val plan = FusionDiscoveryPolicy.sessions(names + names, end, ZoneId.of("America/Chicago"))
        assertEquals(4, plan.size)
        assertEquals(names.last(), plan.first())
        assertEquals(plan, FusionDiscoveryPolicy.sessions(names.reversed(), end, ZoneId.of("America/Chicago")))
        assertFailsWith<IllegalArgumentException> { FusionDiscoveryPolicy.sessions(listOf("../Power.log"), end, ZoneId.of("UTC")) }
    }
}
