package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.RecognitionCoverageState
import java.security.MessageDigest

object FusionCoordinator {
    fun assemble(
        tracker: TrackerEvidence,
        powerMatches: List<ReducedPowerMatch>,
        pairing: PairingDecision,
        fusionVersion: String = "offline-r2/3",
    ): FusedMatch {
        val uniquePower = powerMatches.distinctBy { it.source.id }.sortedBy { it.source.id }
        val accepted = uniquePower.singleOrNull { it.source.id == pairing.acceptedSourceId }
        val sources = mergeSources(listOf(tracker.source) + powerMatches.map { it.source })
        val matchId = stableId("logical", tracker.record.id, *sources.map { it.id }.toTypedArray())
        val incorporated = setOfNotNull(accepted?.source?.id) + accepted?.source?.parentSourceIds.orEmpty()
        val unused = uniquePower.filter { it.source.type != SourceType.DERIVED_FUSION && it.source.id !in incorporated }
        val trackerCoverageDiagnostics = trackerCoverageDiagnostics(tracker)
        val powerCoverageWarnings = if (accepted != null && unused.isNotEmpty()) listOf(
            "Partial reconstruction: ${unused.size} selected Power source(s) retained as provenance but not incorporated into the timeline. " +
                "They may be unrelated games or unverified reconnect fragments; full reconnect recovery is not established.",
        ) else emptyList()
        return FusedMatch(
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
            diagnostics = accepted?.diagnostics.orEmpty() + trackerCoverageDiagnostics + powerCoverageWarnings,
            continuations = listOfNotNull(accepted?.continuation),
        )
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
            first.copy(artifactAliases = aliases)
        }
        .sortedBy { it.id }

    private fun stableId(vararg parts: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(parts.joinToString("\u0000").toByteArray())
        return bytes.take(16).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
