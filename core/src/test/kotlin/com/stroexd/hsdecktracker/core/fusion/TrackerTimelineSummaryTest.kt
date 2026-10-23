package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.*
import kotlin.test.*

class TrackerTimelineSummaryTest {
    private fun tracker(events: List<TimelineEvent>) = TrackerEvidence(
        FusionSource("tracker", "hash", SourceType.ANDROID_TRACKER_JSON, captureMethod = "screenshot-recognition"),
        MatchRecord(id = "saved-game", timestamp = 1L, result = MatchResult.WIN, timeline = events),
    )
    private val pending = PairingDecision(PairingStatus.PENDING, candidates = emptyList(), reason = "No raw source")

    @Test fun keepsOriginalOrderDuplicatesAndAllObservationKindsSeparateFromPowerEvents() {
        val events = listOf(TimelineEvent(2, TimelineType.DRAW, dbfId = 7),
            TimelineEvent(1, TimelineType.RETURN, dbfId = 7),
            TimelineEvent(2, TimelineType.DRAW, dbfId = 7),
            TimelineEvent(2, TimelineType.PLAYER_PLAY, cardId = "CARD"))
        val fused = FusionCoordinator.assemble(tracker(events), emptyList(), pending)
        val decoded = FusionArtifactCodec.decode(FusionArtifactCodec.encode(fused))
        assertEquals(events, decoded.trackerTimeline!!.events)
        assertEquals("/timeline", decoded.trackerTimeline!!.evidence.jsonPointer)
        assertTrue(decoded.trackerTimeline!!.warning.contains("incomplete or inaccurate"))
        assertTrue(decoded.events.isEmpty())
        assertTrue(decoded.snapshots.isEmpty())
    }

    @Test fun emptyTimelineIsNotClaimedCompleteAndOldArtifactWithoutFieldStillDecodes() {
        val fused = FusionCoordinator.assemble(tracker(emptyList()), emptyList(), pending)
        assertTrue(fused.trackerTimeline!!.events.isEmpty())
        val old = fused.copy(trackerTimeline = null)
        assertNull(FusionArtifactCodec.decode(FusionArtifactCodec.encode(old)).trackerTimeline)
    }

    @Test fun rejectsSummaryPointingAtAnotherSource() {
        val fused = FusionCoordinator.assemble(tracker(emptyList()), emptyList(), pending)
        assertFailsWith<FusionArtifactException> {
            FusionArtifactCodec.encode(fused.copy(trackerTimeline = fused.trackerTimeline!!.copy(sourceId = "other")))
        }
    }
}
