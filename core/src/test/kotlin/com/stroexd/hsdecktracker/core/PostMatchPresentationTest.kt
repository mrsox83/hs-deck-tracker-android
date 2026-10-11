package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.*
import kotlin.test.*

class PostMatchPresentationTest {
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
