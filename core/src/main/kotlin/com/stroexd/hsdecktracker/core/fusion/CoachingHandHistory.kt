package com.stroexd.hsdecktracker.core.fusion

import kotlinx.serialization.Serializable

@Serializable
data class CoachingHandHistory(
    val version: String = "source-hand-history/1",
    val checkpoints: List<CoachingHandCheckpoint>,
    val warnings: List<String> = listOf(
        "Controllers are source sides, not automatically verified as the local player.",
        "Missing identity, cost or position is missing tracker evidence, not proof the player did not know it.",
        "Recorded costs are not proof of mana paid. Action targets are not independently verified user selections.",
        "Only source-observed positions are included; missing draw/mulligan order is not guessed.",
    ),
)

@Serializable
data class CoachingHandCard(
    val entityId: String,
    val position: Int,
    val cardId: String? = null,
    val recordedCost: Int? = null,
    val positionEvidence: List<EvidenceRef>,
    val identityEvidence: List<EvidenceRef> = emptyList(),
    val costEvidence: List<EvidenceRef> = emptyList(),
)

@Serializable
data class CoachingHandSide(
    val controller: Int,
    val cards: List<CoachingHandCard>,
    val warnings: List<String>,
)

@Serializable
data class CoachingHandCheckpoint(
    val checkpointId: String,
    val chronologyEpoch: Int,
    val chronologyState: ChronologyState,
    val phase: String,
    val rawTurn: Int?,
    val activeController: Int?,
    val hands: List<CoachingHandSide>,
    val evidence: List<EvidenceRef>,
    /** Exact canonical outer action at this boundary, not an inferred selected target. */
    val actionEventId: String? = null,
    val warnings: List<String> = emptyList(),
    val actorCardBeforeAction: CoachingHandCard? = null,
    val targetCardBeforeAction: CoachingHandCard? = null,
    /** Observed across an outer action; not an assertion that every change was caused by its actor. */
    val costChanges: List<CoachingHandCostChange> = emptyList(),
)

@Serializable
data class CoachingHandCostChange(
    val entityId: String,
    val controllerBefore: Int,
    val controllerAfter: Int,
    val costBefore: Int?,
    val costAfter: Int,
    val evidence: List<EvidenceRef>,
    val changeKind: String = "RECORDED_COST_CHANGED",
)

/** Derives export data from the existing position projector and canonical snapshot replay. */
object CoachingHandHistoryBuilder {
    fun build(match: FusedMatch, check: () -> Unit = {}): CoachingHandHistory {
        require(match.entityEpochs.map { it.chronologyEpoch }.distinct().size == match.entityEpochs.size) {
            "Ambiguous entity epoch archive"
        }
        val epochs = match.entityEpochs.associate { it.chronologyEpoch to it.entities }
        val actions = match.events.filter { it.parentEventId == null }
            .groupBy { it.chronologyEpoch to it.sourceSequence }
        var pending: CanonicalEvent? = null
        var pendingSource: String? = null
        var beforeHands = emptyList<CoachingHandSide>()
        val checkpoints = DecisionHandProjector.all(match, check).map { (snapshot, view) ->
            check()
            val location = snapshot.evidence.map { it.sourceId to it.sourceLine!! }.distinct().single()
            val (source, cutoff) = location
            if (pending?.chronologyEpoch != snapshot.chronologyEpoch || pendingSource != source) {
                pending = null
                beforeHands = emptyList()
            }
            val action = when (snapshot.phase) {
                "BEFORE_ACTION" -> actions[snapshot.chronologyEpoch to snapshot.sequence]?.singleOrNull()
                    ?.takeIf { event -> event.evidence.any { it.sourceId == source && it.sourceLine == cutoff } }
                    .also { pending = it; pendingSource = source }
                "AFTER_OUTER_ACTION" -> pending.also { pending = null }
                else -> null
            }
            val entities = epochs[snapshot.chronologyEpoch].orEmpty()
            val hands = view.hands.map { hand ->
                val cards = hand.slots.map { slot ->
                    check()
                    val entity = entities[slot.entityId]
                    val identities = entity?.identityHistory.orEmpty().filter {
                        it.evidence.sourceId == source && (it.evidence.sourceLine ?: 0) in 1..cutoff
                    }
                    val latestIdentity = identities.maxOfOrNull { it.evidence.sourceLine!! }
                    val identity = identities.filter { it.evidence.sourceLine == latestIdentity }
                    val cardId = identity.map { it.cardId }.distinct().singleOrNull()?.takeIf { it.isNotBlank() }
                    val costs = entity?.tagHistory.orEmpty().filter {
                        it.name == "COST" && it.evidence.sourceId == source && it.sourceLine == it.evidence.sourceLine &&
                            it.sourceLine in 1..cutoff
                    }
                    val latestCost = costs.maxOfOrNull { it.sourceLine }
                    val cost = costs.filter { it.sourceLine == latestCost }
                    val recordedCost = cost.map { it.value }.distinct().singleOrNull()
                        ?.toIntOrNull()?.takeIf { it >= 0 }
                        ?.takeIf { snapshot.entityTags[slot.entityId]?.get("COST") == it.toString() }
                    CoachingHandCard(slot.entityId, slot.position, cardId, recordedCost, slot.evidence,
                        if (cardId == null) emptyList() else identity.map { it.evidence }.distinct(),
                        if (recordedCost == null) emptyList() else cost.map { it.evidence }.distinct())
                }
                CoachingHandSide(hand.controller, cards, hand.diagnostics)
            }
            if (snapshot.phase == "BEFORE_ACTION") beforeHands = hands
            val priorCards = beforeHands.flatMap { hand -> hand.cards.map { hand.controller to it } }
            fun beforeCard(id: String?) = priorCards.filter { it.second.entityId == id }.singleOrNull()?.second
            val changes = if (snapshot.phase == "AFTER_OUTER_ACTION" && action != null) hands.flatMap { hand ->
                hand.cards.mapNotNull { card ->
                    val old = priorCards.filter { it.second.entityId == card.entityId }.singleOrNull() ?: return@mapNotNull null
                    val oldCost = old.second.recordedCost
                    val newCost = card.recordedCost ?: return@mapNotNull null
                    if (oldCost == newCost) null else CoachingHandCostChange(card.entityId, old.first, hand.controller,
                        oldCost, newCost, (old.second.costEvidence + card.costEvidence).distinct(),
                        if (oldCost == null) "RECORDED_COST_BECAME_AVAILABLE" else "RECORDED_COST_CHANGED")
                }
            } else emptyList()
            val result = CoachingHandCheckpoint(snapshot.id, snapshot.chronologyEpoch, snapshot.chronologyState,
                snapshot.phase, snapshot.rawTurn, snapshot.activeController, hands, snapshot.evidence,
                action?.id, buildList {
                    if (view.unresolvedBlock) add("UNRESOLVED_ACTION_BOUNDARY")
                    if (snapshot.chronologyEpoch !in epochs) add("ENTITY_EPOCH_ARCHIVE_UNAVAILABLE")
                }, if (action == null) null else beforeCard(action.actorEntityId),
                if (action == null) null else beforeCard(action.targetEntityId), changes)
            if (snapshot.phase == "AFTER_OUTER_ACTION") beforeHands = emptyList()
            result
        }.toList()
        return CoachingHandHistory(checkpoints = checkpoints)
    }
}
