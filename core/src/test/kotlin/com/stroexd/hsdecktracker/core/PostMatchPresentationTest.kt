package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.*
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.stats.MatchResult
import kotlin.test.*

class PostMatchPresentationTest {
    private fun entry(revision: Int? = 1, result: MatchResult? = MatchResult.WIN) = FusionImportEntry(
        matchId = "match", bundleSha256 = emptyList(), state = FusionImportState.FUSED_LOCAL,
        updatedAt = 1, fusedRecordRevision = revision, fusedRecordResult = result,
    )

    @Test fun fusionConsistencySeparatesCurrentStaleConflictAndLegacyEvidence() {
        val current = MatchRecord(id = "match", timestamp = 1, result = MatchResult.WIN, revision = 2)
        assertEquals(PostMatchPresentation.FusionConsistency.CURRENT,
            PostMatchPresentation.fusionConsistency(current, entry(2)).consistency)

        val stale = PostMatchPresentation.fusionConsistency(current, entry(1))
        assertEquals(PostMatchPresentation.FusionConsistency.REVISION_STALE, stale.consistency)
        assertTrue(stale.message.contains("not silently regenerated"))

        val conflict = PostMatchPresentation.fusionConsistency(
            current.copy(result = MatchResult.LOSS, resultCorrections = listOf(
                com.stroexd.hsdecktracker.core.stats.ResultCorrection(MatchResult.WIN, MatchResult.LOSS, 2),
            )), entry(1, MatchResult.WIN))
        assertEquals(PostMatchPresentation.FusionConsistency.RESULT_CONFLICT, conflict.consistency)
        assertTrue(conflict.message.contains("preserved fusion used WIN"))
        assertTrue(conflict.message.contains("current saved revision 2 is LOSS"))
        assertTrue(conflict.message.contains("Both values"))

        assertEquals(PostMatchPresentation.FusionConsistency.UNKNOWN_LEGACY,
            PostMatchPresentation.fusionConsistency(current, entry(null, null)).consistency)
    }
    @Test fun longCoverageStatusIsCompactWithoutChangingFullEvidence() {
        val warning = "Partial reconstruction: 3 selected Power sources retained as provenance. " + "Unverified reconnect. ".repeat(100)
        val full = PostMatchPresentation.resultText(FusionImportState.FUSED_LOCAL, null, true, listOf(warning))
        val summary = PostMatchPresentation.statusSummary(full)
        assertTrue(summary.length < 140)
        assertTrue(summary.contains("artifact preserved"))
        assertTrue(summary.contains("No cloud upload"))
        assertTrue(summary.contains("Coverage warning"))
        assertTrue(full.endsWith(warning))
    }
    @Test fun pendingAndFailureSummariesNeverClaimFusionSuccess() {
        val pending = PostMatchPresentation.statusSummary(PostMatchPresentation.resultText(FusionImportState.PENDING_PAIRING, "ambiguous".repeat(200)))
        assertEquals("Logs saved locally; pairing pending.", pending)
        for (message in listOf("Export cancelled. Private evidence remains saved locally", "Export not completed: " + "error".repeat(200))) {
            val summary = PostMatchPresentation.statusSummary(message)
            assertTrue(summary.length <= 140)
            assertFalse(summary.contains("Fused locally"))
        }
    }
    @Test fun partialWarningSurvivesPreservedResultPresentation() {
        val warning = "Partial reconstruction: selected source not incorporated"
        for (preserved in listOf(false, true)) {
            assertTrue(PostMatchPresentation.resultText(FusionImportState.FUSED_LOCAL, null, preserved, listOf(warning)).contains(warning))
        }
    }
    @Test fun readableSessionHasNoInferredTimezoneOrPairing() {
        assertEquals("Oct 10, 2026 • 7:54:55 PM", PostMatchPresentation.sessionLabel("Hearthstone_2026_10_10_19_54_55"))
        assertEquals("Oct 11, 2026 • 12:00:00 AM", PostMatchPresentation.sessionLabel("Hearthstone_2026_10_11_00_00_00"))
    }
    @Test fun invalidAndLegacyNamesRemainVisibleWithoutNormalization() {
        for (name in listOf("Hearthstone_2", "Hearthstone_2026_02_30_12_00_00", "Hearthstone_2026_10_10_24_00_00"))
            assertEquals(name, PostMatchPresentation.sessionLabel(name))
    }
    @Test fun preservationIsNotPresentedAsRegeneration() {
        val preserved = PostMatchPresentation.resultText(FusionImportState.FUSED_LOCAL, null, true)
        assertTrue(preserved.contains("Already fused locally"))
        assertTrue(preserved.contains("no regeneration"))
        assertTrue(preserved.contains("No cloud upload"))
        assertFalse(PostMatchPresentation.resultText(FusionImportState.FUSED_LOCAL, null).contains("Already"))
        assertTrue(PostMatchPresentation.resultText(FusionImportState.PENDING_PAIRING, "ambiguous", true).contains("pairing is pending. ambiguous"))
        assertTrue(PostMatchPresentation.resultText(FusionImportState.FAILED, "unreadable").contains("did not complete. unreadable"))
    }
}
