package com.stroexd.hsdecktracker.core.fusion

import java.time.Instant
import java.time.ZoneId

/** Search bounds only. Folder times order reads, never establish pairing or global uniqueness. */
object FusionDiscoveryPolicy {
    const val MAX_SESSIONS = 6
    const val MAX_RAW_BYTES = 64L * 1024 * 1024
    const val MAX_MATCHES = 24
    const val DEADLINE_MILLIS = 60_000L

    fun sessions(names: List<String>, trackerTimestamp: Long, zone: ZoneId): List<String> {
        names.forEach(PostMatchLogSnapshot::requireSession)
        val end = Instant.ofEpochMilli(trackerTimestamp)
        return names.distinct().sortedWith(compareBy<String> { name ->
            val time = name.takeLast(8).replace('_', ':')
            PowerClock.resolve(name, time, zone)?.let {
                kotlin.math.abs(java.time.Duration.between(it, end).seconds)
            } ?: Long.MAX_VALUE
        }.thenByDescending { it }).take(MAX_SESSIONS)
    }

    fun permitsRawBytes(retainedBytes: Long, nextBytes: Long): Boolean =
        retainedBytes in 0..MAX_RAW_BYTES && nextBytes in 1..PostMatchLogSnapshot.MAX_BYTES &&
            nextBytes <= MAX_RAW_BYTES - retainedBytes
}

/** Non-writing preview over the existing pairing gate's per-controller decisions.
 * Never prefer a high score over another viable game. Caller retains raw evidence separately.
 */
object FusionDiscoveryPreview {
    fun decide(decisions: List<PairingDecision>, searchComplete: Boolean): PairingDecision {
        val candidates = decisions.flatMap { it.candidates }
            .groupBy { it.powerSourceId }.values.map { variants ->
                variants.sortedWith(compareBy<PairingCandidate> { it.rejectedReasons.isNotEmpty() }
                    .thenByDescending { it.score }).first()
            }.sortedWith(compareByDescending<PairingCandidate> { it.score }.thenBy { it.powerSourceId })
        if (!searchComplete) return PairingDecision(PairingStatus.PENDING, candidates = candidates,
            reason = "Search incomplete; limits or read failures prevent a unique-match recommendation")
        val viable = candidates.filter { it.rejectedReasons.isEmpty() && it.score >= 4 && it.reasons.size >= 2 && it.evidence.isNotEmpty() }
        val acceptedIds = decisions.filter { it.status == PairingStatus.ACCEPTED }.mapNotNull { it.acceptedSourceId }.toSet()
        return when {
            viable.size > 1 -> PairingDecision(PairingStatus.CONFLICTED, candidates = candidates,
                reason = "Multiple plausible games; review candidates or select sessions manually")
            viable.size == 1 && viable.single().powerSourceId in acceptedIds -> PairingDecision(
                PairingStatus.ACCEPTED, viable.single().powerSourceId, candidates,
                "One supported candidate in the inspected sessions; confirm before fusion")
            else -> PairingDecision(PairingStatus.PENDING, candidates = candidates,
                reason = "No uniquely supported game; missing or conflicting evidence remains unresolved")
        }
    }
}
