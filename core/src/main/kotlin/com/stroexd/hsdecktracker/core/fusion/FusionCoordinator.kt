package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.RecognitionCoverageState
import java.security.MessageDigest

const val OFFLINE_FUSION_VERSION = "offline-r2/6"
const val ANDROID_FUSION_VERSION = "android-r3/6"

object FusionCoordinator {
    fun assemble(
        tracker: TrackerEvidence,
        powerMatches: List<ReducedPowerMatch>,
        pairing: PairingDecision,
        fusionVersion: String = OFFLINE_FUSION_VERSION,
    ): FusedMatch {
        val uniquePower = powerMatches.distinctBy { it.source.id }.sortedBy { it.source.id }
        val accepted = uniquePower.singleOrNull { it.source.id == pairing.acceptedSourceId }
        val sources = mergeSources(listOf(tracker.source) + powerMatches.map { it.source })
        val trackerId = tracker.record.id
        require(trackerId.isNotBlank() && trackerId == trackerId.trim() && trackerId.none { it.isISOControl() }) {
            "A nonblank, unpadded saved tracker identity without control characters is required"
        }
        require(Charsets.UTF_8.newEncoder().canEncode(trackerId)) { "Saved tracker identity must contain valid Unicode" }
        // Saved IDs are opaque (legacy imports need not be UUID-shaped). Never normalize or guess.
        // Keep the established 128-bit filename shape; the repository refuses content collisions.
        val matchId = stableId("logical:tracker-record/1", trackerId)
        val incorporated = setOfNotNull(accepted?.source?.id) + accepted?.source?.parentSourceIds.orEmpty()
        val incorporatedKeys = uniquePower.filter { it.source.id in incorporated }
            .mapNotNull { it.source.verifiedCompletedMatchKey }.toSet()
        val unused = uniquePower.filter { it.source.type != SourceType.DERIVED_FUSION && it.source.id !in incorporated &&
            (it.source.verifiedCompletedMatchKey == null || it.source.verifiedCompletedMatchKey !in incorporatedKeys) }
        val equivalentSources = uniquePower.filter { it.source.id !in incorporated &&
            it.source.verifiedCompletedMatchKey != null && it.source.verifiedCompletedMatchKey in incorporatedKeys }
        val equivalenceDiagnostics = equivalentSources.map {
            "Equivalent completed Power evidence: ${it.source.id} retained as original provenance, not a separate game candidate " +
                "(completed-prefix/1 exact parsed content and raw-prefix proof ${it.source.verifiedCompletedMatchKey})."
        }
        val trackerCoverageDiagnostics = trackerCoverageDiagnostics(tracker)
        val powerCoverageWarnings = if (accepted != null && unused.isNotEmpty()) listOf(
            "Partial reconstruction: ${unused.size} selected Power source(s) retained as provenance but not incorporated into the timeline. " +
                "They may be unrelated games or unverified reconnect fragments; full reconnect recovery is not established.",
        ) else emptyList()
        val fused = FusedMatch(
            schema = FUSION_SCHEMA,
            matchId = matchId,
            fusionVersion = fusionVersion,
            sources = sources,
            pairing = pairing,
            events = accepted?.events.orEmpty(),
            entities = accepted?.entities.orEmpty(),
            choices = accepted?.choices.orEmpty(),
            snapshots = accepted?.snapshots.orEmpty(),
            entityEpochs = accepted?.entityEpochs.orEmpty(),
            trackerTimeline = TrackerTimelineSummary(
                sourceId = tracker.source.id,
                events = tracker.record.timeline.toList(),
                evidence = EvidenceRef(tracker.source.id, jsonPointer = "/timeline", rule = "tracker-timeline-summary/1"),
            ),
            diagnostics = accepted?.diagnostics.orEmpty() + trackerCoverageDiagnostics + powerCoverageWarnings + equivalenceDiagnostics +
                "Logical match identity: tracker-record/1; selected evidence and saved-result revisions do not create another game identity.",
            continuations = listOfNotNull(accepted?.continuation),
        )
        if (accepted == null) return fused
        // Supplemental hand derivation must not block preservation of the existing canonical evidence.
        return try { fused.copy(handHistory = CoachingHandHistoryBuilder.build(fused)) }
        catch (error: IllegalArgumentException) {
            fused.copy(diagnostics = fused.diagnostics + "Hand history unavailable: source checkpoint chronology/provenance did not pass validation.")
        }
    }

    private fun trackerCoverageDiagnostics(tracker: TrackerEvidence): List<String> {
        val intervals = tracker.record.recognitionCoverage
        if (intervals.isEmpty()) return listOf(
            "Tracker match finalization was observed, but screenshot-recognition coverage was not measured. " +
                "Missing tracker events must not be treated as confirmed player inactivity.",
        )
        val counts = RecognitionCoverageState.entries.associateWith { state ->
            intervals.count { it.state == state }
        }
        val truncation = if (tracker.record.recognitionCoverageTruncated) {
            " The interval list was truncated; omitted coverage must remain unknown."
        } else ""
        return listOf(
            "Tracker screenshot-recognition coverage was measured across ${intervals.size} interval(s): " +
                "${counts.getValue(RecognitionCoverageState.FRAME_OBSERVED)} frame-observed, " +
                "${counts.getValue(RecognitionCoverageState.PLAY_DETECTION_ELIGIBLE)} play-detection-eligible, and " +
                "${counts.getValue(RecognitionCoverageState.CAPTURE_GAP)} capture-gap. " +
                "Coverage describes observation eligibility, not proof that every gameplay action was recognized." + truncation,
        )
    }

    private fun mergeSources(sources: List<FusionSource>): List<FusionSource> = sources.groupBy { it.id }
        .map { (_, revisions) ->
            val first = revisions.first()
            val aliases = revisions.flatMap { it.artifactAliases + it.artifactSha256 }.distinct().sorted()
            first.copy(artifactAliases = aliases, verifiedCompletedMatchKey = null)
        }
        .sortedBy { it.id }

    private fun stableId(vararg parts: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(parts.joinToString("\u0000").toByteArray())
        return bytes.take(16).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
