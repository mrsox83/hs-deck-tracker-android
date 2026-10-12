package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.*
import kotlin.test.*

class PostMatchPresentationTest {
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
