package com.stroexd.hsdecktracker.core.fusion

/** In-memory evidence view, deliberately not serializable or attached to FusedMatch. */
data class DecisionHandSlot(
    val entityId: String,
    val position: Int,
    val evidence: List<EvidenceRef>,
    val cardId: String? = null,
    val effectiveCost: Int? = null,
    val knowledge: String = "NOT_ESTABLISHED_IN_DECISION_TIME_EVIDENCE",
)

data class DecisionHand(
    val controller: Int,
    val slots: List<DecisionHandSlot>,
    val diagnostics: List<String>,
    /** No independent complete inventory is established by contiguous positions. */
    val expectedHandSize: Int? = null,
    val complete: Boolean? = null,
)

data class DecisionHandView(
    val checkpointId: String,
    val chronologyEpoch: Int,
    val phase: String,
    val rawTurn: Int?,
    val activeController: Int?,
    val playerTurnIndices: Map<Int, Int>,
    val unresolvedBlock: Boolean,
    val chronologyState: ChronologyState,
    val hands: List<DecisionHand>,
    val diagnostics: List<String>,
)

/** Position-first projection. Source identity alone cannot certify human observer knowledge. */
object DecisionHandProjector {
    fun project(match: FusedMatch, checkpointId: String, controllers: Set<Int> = setOf(1, 2),
        check: () -> Unit = {}): DecisionHandView {
        require(match.schema == FUSION_SCHEMA && match.pairing.status == PairingStatus.ACCEPTED) {
            "Hand projection requires a supported accepted fusion"
        }
        require(controllers.isNotEmpty() && controllers.all { it > 0 }) { "Explicit positive controllers required" }
        val selected = match.snapshots.withIndex().filter { it.value.id == checkpointId }.singleOrNull()
            ?: error("Checkpoint must identify exactly one snapshot")
        val prefix = match.snapshots.take(selected.index + 1)
        val sources = match.sources.associateBy { it.id }
        require(sources.size == match.sources.size && match.pairing.acceptedSourceId in sources) { "Ambiguous or missing accepted source" }
        val incorporated = setOfNotNull(match.pairing.acceptedSourceId) + match.sources
            .singleOrNull { it.id == match.pairing.acceptedSourceId }?.parentSourceIds.orEmpty()
        fun location(snapshot: FusionSnapshot): Pair<String, Int> {
            val refs = snapshot.evidence.map { ref ->
                require(ref.sourceId in sources && ref.sourceId in incorporated && (ref.sourceLine ?: 0) > 0) {
                    "Checkpoint lacks an exact incorporated source location"
                }
                ref.sourceId to ref.sourceLine!!
            }.distinct()
            return refs.singleOrNull() ?: error("Checkpoint source/cutoff is ambiguous")
        }
        var previous: FusionSnapshot? = null
        var previousLocation: Pair<String, Int>? = null
        for (snapshot in prefix) {
            check()
            val at = location(snapshot)
            require(snapshot.chronologyEpoch >= 0) { "Invalid chronology epoch" }
            val prior = previous
            require(prior != null || snapshot.stateMode == SnapshotStateMode.FULL) { "DELTA before FULL" }
            if (prior != null) {
                require(snapshot.chronologyEpoch >= prior.chronologyEpoch) { "Chronology moved backwards" }
                if (snapshot.chronologyEpoch != prior.chronologyEpoch || at.first != previousLocation!!.first)
                    require(snapshot.stateMode == SnapshotStateMode.FULL) { "Epoch/source transition needs a FULL baseline" }
                if (snapshot.chronologyEpoch == prior.chronologyEpoch && at.first == previousLocation!!.first)
                    require(at.second >= previousLocation!!.second) { "Source cutoff moved backwards" }
            }
            previous = snapshot
            previousLocation = at
        }
        val snapshot = FusionSnapshotMaterializer.materialize(prefix).map { check(); it }.last()
        val (source, cutoff) = location(snapshot)
        val archives = match.entityEpochs.filter { it.chronologyEpoch == snapshot.chronologyEpoch }
        require(archives.size <= 1) { "Ambiguous entity epoch archive" }
        val entities = archives.singleOrNull()?.entities.orEmpty()
        fun tag(entityId: String, name: String, expected: String): EvidenceRef? {
            val observations = entities[entityId]?.tagHistory.orEmpty().filter {
                it.name == name && it.evidence.sourceId == source && it.sourceLine == it.evidence.sourceLine &&
                    it.sourceLine in 1..cutoff
            }
            val latest = observations.maxOfOrNull { it.sourceLine } ?: return null
            val values = observations.filter { it.sourceLine == latest }
            if (values.map { it.value }.distinct() != listOf(expected)) return null
            return values.first().evidence
        }
        val hands = controllers.sorted().map { controller ->
            val slots = mutableListOf<DecisionHandSlot>()
            val warnings = mutableListOf<String>()
            for ((entityId, tags) in snapshot.entityTags) {
                check()
                if (tags["ZONE"] != "HAND") continue
                val owner = tags["CONTROLLER"]?.toIntOrNull()
                if (owner == null) { warnings += "HAND_CONTROLLER_UNKNOWN:$entityId"; continue }
                if (owner != controller) continue
                val position = tags["ZONE_POSITION"]?.toIntOrNull()
                if (position == null || position <= 0) { warnings += "HAND_POSITION_INVALID:$entityId"; continue }
                val refs = listOfNotNull(tag(entityId, "ZONE", "HAND"),
                    tag(entityId, "CONTROLLER", tags.getValue("CONTROLLER")),
                    tag(entityId, "ZONE_POSITION", tags.getValue("ZONE_POSITION")))
                if (refs.size != 3) { warnings += "HAND_SLOT_PROVENANCE_UNESTABLISHED:$entityId"; continue }
                if (refs.any { it.rule == "power-descriptor-position/1" })
                    warnings += "HAND_SLOT_SUPPLEMENTAL_DESCRIPTOR:$entityId"
                // Deliberately do not inspect identityHistory, final cardId, COST, or opponent lists.
                slots += DecisionHandSlot(entityId, position, refs.distinct())
            }
            val ordered = slots.sortedWith(compareBy({ it.position }, { it.entityId }))
            if (ordered.map { it.position }.distinct().size != ordered.size) warnings += "DUPLICATE_HAND_POSITIONS"
            if (ordered.map { it.position }.distinct().withIndex().any { (index, position) -> position != index + 1 })
                warnings += "HAND_POSITION_GAPS"
            warnings += "HAND_COMPLETENESS_UNKNOWN"
            DecisionHand(controller, ordered, warnings.distinct())
        }
        return DecisionHandView(snapshot.id, snapshot.chronologyEpoch, snapshot.phase, snapshot.rawTurn,
            snapshot.activeController, snapshot.playerTurnIndices, snapshot.unresolvedBlock, snapshot.chronologyState,
            hands, listOf("OBSERVER_KNOWLEDGE_UNESTABLISHED: identities and private costs withheld; missing capture is not human ignorance") +
                if (archives.isEmpty()) listOf("ENTITY_EPOCH_ARCHIVE_UNAVAILABLE") else emptyList())
    }
}
