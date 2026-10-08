package com.stroexd.hsdecktracker.core.fusion

import java.security.MessageDigest

object FusionCoordinator {
    fun assemble(
        tracker: TrackerEvidence,
        powerMatches: List<ReducedPowerMatch>,
        pairing: PairingDecision,
        fusionVersion: String = "offline-r2/1",
    ): FusedMatch {
        val uniquePower = powerMatches.distinctBy { it.source.id }.sortedBy { it.source.id }
        val accepted = uniquePower.singleOrNull { it.source.id == pairing.acceptedSourceId }
        val sources = mergeSources(listOf(tracker.source) + powerMatches.map { it.source })
        val matchId = stableId("logical", tracker.record.id, *sources.map { it.id }.toTypedArray())
        return FusedMatch(
            matchId = matchId,
            fusionVersion = fusionVersion,
            sources = sources,
            pairing = pairing,
            events = accepted?.events.orEmpty(),
            entities = accepted?.entities.orEmpty(),
            choices = accepted?.choices.orEmpty(),
            snapshots = accepted?.snapshots.orEmpty(),
            diagnostics = accepted?.diagnostics.orEmpty(),
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
