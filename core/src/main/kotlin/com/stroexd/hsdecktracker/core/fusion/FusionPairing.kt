package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchResult
import java.time.Duration
import java.time.Instant

data class PowerPairingFacts(
    val match: ReducedPowerMatch,
    val endedAt: Instant?,
    val result: MatchResult? = null,
    val wentFirst: Boolean? = null,
    val anchorCardIds: Set<String> = emptySet(),
)

object FusionPairing {
    fun decide(tracker: TrackerEvidence, powers: List<PowerPairingFacts>, trackerCardIds: Set<String> = emptySet()): PairingDecision {
        val trackerEnd = Instant.ofEpochMilli(tracker.record.timestamp)
        val candidates = powers.map { power ->
            val reasons = mutableListOf<String>()
            val rejected = mutableListOf<String>()
            var score = 0
            power.endedAt?.let {
                val seconds = kotlin.math.abs(Duration.between(it, trackerEnd).seconds)
                if (seconds <= 120) { score += 2; reasons += "endpoints differ by ${seconds}s" }
                else rejected += "endpoint outside 120s window"
            }
            power.result?.let {
                if (it == tracker.record.result) { score += 2; reasons += "result agrees" } else rejected += "result conflicts"
            }
            power.wentFirst?.let {
                if (tracker.record.wentFirst == null) Unit
                else if (it == tracker.record.wentFirst) { score += 1; reasons += "first-player evidence agrees" }
                else rejected += "first-player evidence conflicts"
            }
            val anchors = trackerCardIds intersect power.anchorCardIds
            if (anchors.isNotEmpty()) { score += minOf(3, anchors.size); reasons += "${anchors.size} ordered-card anchor(s) overlap" }
            PairingCandidate(power.match.source.id, score, reasons, rejected)
        }.sortedByDescending { it.score }

        val viable = candidates.filter { it.rejectedReasons.isEmpty() && it.score >= 4 && it.reasons.size >= 2 }
        if (viable.isEmpty()) return PairingDecision(PairingStatus.PENDING, candidates = candidates, reason = "No candidate has two compatible independent characteristics")
        if (viable.size > 1 && viable[0].score - viable[1].score < 2) {
            return PairingDecision(PairingStatus.CONFLICTED, candidates = candidates, reason = "Top candidates are not uniquely distinguishable")
        }
        return PairingDecision(PairingStatus.ACCEPTED, viable[0].powerSourceId, candidates, "Unique candidate supported by multiple characteristics")
    }

    fun rejectComposite(tracker: TrackerEvidence, powerSourceIds: List<String>): PairingDecision = PairingDecision(
        status = PairingStatus.REJECTED_COMPOSITE,
        candidates = powerSourceIds.map { PairingCandidate(it, 0, emptyList(), listOf("tracker record spans multiple CREATE_GAME segments")) },
        reason = "Tracker ${tracker.record.id} is composite; duration, result and turns are not assigned wholesale",
    )
}
