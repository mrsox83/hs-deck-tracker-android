package com.stroexd.hsdecktracker.core.fusion

import java.time.ZoneId

data class FusionEvaluation(val powers: List<ReducedPowerMatch>, val decisions: List<PairingDecision>,
    val discoveryDecisions: List<PairingDecision> = decisions) {
    /** Preserve the established manual import gate. Discovery adds a stricter ambiguity check. */
    fun manualDecision(): PairingDecision {
        val accepted = decisions.filter { it.status == PairingStatus.ACCEPTED }.distinctBy { it.acceptedSourceId }
        if (accepted.size == 1) return accepted.single()
        val pending = decisions.maxByOrNull { it.candidates.maxOfOrNull { candidate -> candidate.score } ?: 0 }
            ?: PairingDecision(PairingStatus.PENDING, candidates = emptyList(), reason = "No controller evidence was available")
        return if (accepted.size > 1) pending.copy(status = PairingStatus.CONFLICTED, acceptedSourceId = null,
            reason = "Multiple selected Power matches satisfy the conservative pairing gate") else pending
    }

    fun discoveryDecision(complete: Boolean): PairingDecision {
        return FusionDiscoveryPreview.decide(discoveryDecisions, complete)
    }
}

/** Shared non-writing evaluator: same controller, clock and continuation semantics as manual import. */
object FusionEvaluator {
    fun evaluate(tracker: TrackerEvidence, originals: List<ReducedPowerMatch>, zone: ZoneId,
        trackerCardIds: Set<String> = emptySet(), check: () -> Unit = {}): FusionEvaluation {
        val unique = originals.distinctBy { it.source.id }
        val representatives = CompletedMatchEquivalence.representatives(unique)
        val stitched = mutableListOf<ReducedPowerMatch>()
        for (earlier in representatives) for (later in representatives) {
            check()
            if (earlier === later) continue
            val decision = PowerContinuationStitcher.evaluate(earlier, later, tracker, zone)
            if (decision.status == ContinuationStatus.ACCEPTED)
                stitched += PowerContinuationStitcher.stitch(earlier, later, decision)
        }
        val powers = unique + stitched.distinctBy { it.source.id }
        val candidates = representatives + stitched.distinctBy { it.source.id }
        val controllers = powers.flatMap { it.entities.values.mapNotNull { entity -> entity.controller.value } }
            .filter { it > 0 }.toSet().ifEmpty { setOf(1, 2) }
        fun decisionsFor(matches: List<ReducedPowerMatch>) = controllers.map { controller ->
            check()
            FusionPairing.decide(tracker, matches.map { power ->
                check()
                val end = power.endedLogTime?.let { time -> power.source.sessionAlias?.let { PowerClock.resolve(it, time, zone) } }
                PowerPairingFacts.fromReduced(power, end, controller)
            }, trackerCardIds)
        }
        val decisions = decisionsFor(candidates)
        // Re-evaluate after collapsing a verified, viable stitch's parents. Merely filtering
        // candidates from a CONFLICTED decision would leave a false ambiguity status behind.
        val viableIds = decisions.flatMap { it.candidates }.filter {
            it.rejectedReasons.isEmpty() && it.score >= 4 && it.reasons.size >= 2 && it.evidence.isNotEmpty()
        }.map { it.powerSourceId }.toSet()
        val superseded = powers.filter { it.continuation != null && it.source.id in viableIds }
            .flatMap { it.source.parentSourceIds }.toSet()
        val discovery = if (superseded.isEmpty()) decisions else decisionsFor(candidates.filterNot { it.source.id in superseded })
        return FusionEvaluation(powers, decisions, discovery)
    }
}
