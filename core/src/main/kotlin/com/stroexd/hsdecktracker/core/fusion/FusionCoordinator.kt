package com.stroexd.hsdecktracker.core.fusion

import java.security.MessageDigest

object FusionCoordinator {
    fun assemble(
        tracker: TrackerEvidence,
        powerMatches: List<ReducedPowerMatch>,
        pairing: PairingDecision,
        fusionVersion: String = "offline-r2/2",
    ): FusedMatch {
        val uniquePower = powerMatches.distinctBy { it.source.id }.sortedBy { it.source.id }
        val accepted = uniquePower.singleOrNull { it.source.id == pairing.acceptedSourceId }
        val sources = mergeSources(listOf(tracker.source) + powerMatches.map { it.source })
        val matchId = stableId("logical", tracker.record.id, *sources.map { it.id }.toTypedArray())
        val incorporated = setOfNotNull(accepted?.source?.id) + accepted?.source?.parentSourceIds.orEmpty()
        val unused = uniquePower.filter { it.source.type != SourceType.DERIVED_FUSION && it.source.id !in incorporated }
        val trackerCoverageWarning = if (tracker.source.complete == null) listOf(
            "Tracker match finalization was observed, but screenshot-recognition coverage was not measured. " +
                "Missing tracker events must not be treated as confirmed player inactivity.",
        ) else emptyList()
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
            diagnostics = accepted?.diagnostics.orEmpty() + trackerCoverageWarning + powerCoverageWarnings,
            continuations = listOfNotNull(accepted?.continuation),
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
