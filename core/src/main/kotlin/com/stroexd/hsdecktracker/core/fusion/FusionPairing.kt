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
    val endEvidence: List<EvidenceRef> = emptyList(),
    val resultEvidence: List<EvidenceRef> = emptyList(),
    val wentFirstEvidence: List<EvidenceRef> = emptyList(),
    val anchorEvidence: Map<String, List<EvidenceRef>> = emptyMap(),
) {
    companion object {
        fun fromReduced(match: ReducedPowerMatch, endedAt: Instant?, localController: Int): PowerPairingFacts {
            val player = match.entities.values.firstOrNull { it.controller.value == localController && "PLAYSTATE" in it.tags }
            val result = when (player?.tags?.get("PLAYSTATE")) {
                "WON" -> MatchResult.WIN
                "LOST" -> MatchResult.LOSS
                "TIED" -> MatchResult.DRAW
                else -> null
            }
            val resultEvidence = player?.tagHistory?.lastOrNull { it.name == "PLAYSTATE" }?.evidence?.let(::listOf).orEmpty()
            val firstRaw = player?.tags?.get("FIRST_PLAYER")
            val wentFirst = firstRaw?.let { it == "1" }
            val firstEvidence = player?.tagHistory?.lastOrNull { it.name == "FIRST_PLAYER" }?.evidence?.let(::listOf).orEmpty()
            val anchors = linkedMapOf<String, MutableList<EvidenceRef>>()
            for (event in match.events) {
                val cardId = event.actorEntityId?.let { match.entities[it]?.cardId?.value } ?: continue
                anchors.getOrPut(cardId) { mutableListOf() }.addAll(event.evidence)
            }
            return PowerPairingFacts(
                match = match,
                endedAt = endedAt,
                result = result,
                wentFirst = wentFirst,
                anchorCardIds = anchors.keys,
                endEvidence = match.endedEvidence?.let(::listOf).orEmpty(),
                resultEvidence = resultEvidence,
                wentFirstEvidence = firstEvidence,
                anchorEvidence = anchors,
            )
        }
    }
}

object FusionPairing {
    fun decide(tracker: TrackerEvidence, powers: List<PowerPairingFacts>, trackerCardIds: Set<String> = emptySet()): PairingDecision {
        val trackerEnd = Instant.ofEpochMilli(tracker.record.timestamp)
        val candidates = powers.map { power ->
            val reasons = mutableListOf<String>()
            val rejected = mutableListOf<String>()
            val evidence = mutableListOf<EvidenceRef>()
            var score = 0
            power.endedAt?.let {
                val seconds = kotlin.math.abs(Duration.between(it, trackerEnd).seconds)
                if (seconds <= 120 && power.endEvidence.isNotEmpty()) {
                    score += 2; reasons += "endpoints differ by ${seconds}s"
                    evidence += power.endEvidence
                    evidence += EvidenceRef(tracker.source.id, jsonPointer = "/timestamp", rule = "pairing-endpoint/1")
                }
                else if (seconds <= 120) rejected += "endpoint agreement lacks source evidence"
                else rejected += "endpoint outside 120s window"
            }
            power.result?.let {
                if (it == tracker.record.result && power.resultEvidence.isNotEmpty()) {
                    score += 2; reasons += "result agrees"; evidence += power.resultEvidence
                    evidence += EvidenceRef(tracker.source.id, jsonPointer = "/result", rule = "pairing-result/1")
                } else if (it == tracker.record.result) rejected += "result agreement lacks source evidence"
                else rejected += "result conflicts"
            }
            power.wentFirst?.let {
                if (tracker.record.wentFirst == null) Unit
                else if (it == tracker.record.wentFirst && power.wentFirstEvidence.isNotEmpty()) {
                    score += 1; reasons += "first-player evidence agrees"; evidence += power.wentFirstEvidence
                    evidence += EvidenceRef(tracker.source.id, jsonPointer = "/wentFirst", rule = "pairing-first-player/1")
                }
                else if (it == tracker.record.wentFirst) rejected += "first-player agreement lacks source evidence"
                else rejected += "first-player evidence conflicts"
            }
            val anchors = trackerCardIds intersect power.anchorCardIds
            val evidencedAnchors = anchors.filter { power.anchorEvidence[it].orEmpty().isNotEmpty() }
            if (evidencedAnchors.isNotEmpty()) {
                score += minOf(3, evidencedAnchors.size); reasons += "${evidencedAnchors.size} ordered-card anchor(s) overlap"
                evidencedAnchors.forEach { evidence += power.anchorEvidence.getValue(it) }
            }
            PairingCandidate(power.match.source.id, score, reasons, rejected, evidence.distinct())
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
