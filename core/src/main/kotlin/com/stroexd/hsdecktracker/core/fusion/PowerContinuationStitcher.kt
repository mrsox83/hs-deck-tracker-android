package com.stroexd.hsdecktracker.core.fusion

import java.security.MessageDigest
import java.time.Duration
import java.time.ZoneId
import kotlinx.serialization.Serializable

@Serializable
enum class ContinuationStatus { ACCEPTED, PENDING, REJECTED }

@Serializable
data class PowerContinuationDecision(
    val status: ContinuationStatus,
    val earlierSourceId: String,
    val laterSourceId: String,
    val reasons: List<String>,
    val evidence: List<EvidenceRef> = emptyList(),
)

object PowerContinuationStitcher {
    private const val MAX_SESSION_GAP_SECONDS = 180L
    private const val MAX_TRACKER_END_DELTA_SECONDS = 120L
    private const val MIN_SHARED_IDENTITIES = 3

    fun evaluate(
        earlier: ReducedPowerMatch,
        later: ReducedPowerMatch,
        tracker: TrackerEvidence,
        zoneId: ZoneId,
    ): PowerContinuationDecision {
        val rejected = mutableListOf<String>()
        if (earlier.completed) rejected += "earlier segment is already complete"
        if (earlier.sourceTruncated) rejected += "earlier segment is source-truncated"
        if (!later.completed || later.endedLogTime == null || later.endedEvidence == null) {
            rejected += "later segment has no evidenced completion"
        }

        val earlierLast = earlier.snapshots.lastOrNull()?.evidence?.lastOrNull()
        val earlierEnd = resolve(earlier, earlierLast?.observedTime, zoneId)
        val laterStart = resolve(later, later.startedLogTime, zoneId)
        val laterEnd = resolve(later, later.endedLogTime, zoneId)
        if (earlierEnd == null || laterStart == null || laterEnd == null) {
            rejected += "session clocks cannot be resolved"
        } else {
            val sessionGap = Duration.between(earlierEnd, laterStart).seconds
            if (sessionGap !in 0..MAX_SESSION_GAP_SECONDS) rejected += "session gap is outside 0..$MAX_SESSION_GAP_SECONDS seconds"
            val trackerDelta = kotlin.math.abs(Duration.between(laterEnd, java.time.Instant.ofEpochMilli(tracker.record.timestamp)).seconds)
            if (trackerDelta > MAX_TRACKER_END_DELTA_SECONDS) rejected += "tracker endpoint is outside $MAX_TRACKER_END_DELTA_SECONDS seconds"
        }

        val sharedChoice = earlier.choices.asSequence().mapNotNull { first ->
            later.choices.firstOrNull { second ->
                choiceKey(first) == choiceKey(second) && first.type == second.type &&
                    descriptorKey(first.sourceEntity) == descriptorKey(second.sourceEntity) &&
                    first.offered.entityRefs.isNotEmpty() &&
                    first.offered.entityRefs.map(::descriptorKey) == second.offered.entityRefs.map(::descriptorKey)
            }?.let { first to it }
        }.singleOrNull()
        if (sharedChoice == null) rejected += "no unique open-choice continuation anchor"
        else if (sharedChoice.first.confirmed.entityRefs.isNotEmpty()) rejected += "earlier choice was already confirmed"
        else if (sharedChoice.second.confirmed.entityRefs.isEmpty()) rejected += "later choice has no confirmation"

        val sharedIdentities = earlier.entities.mapNotNull { (id, state) ->
            val cardId = state.cardId.value ?: return@mapNotNull null
            if (later.entities[id]?.cardId?.value == cardId) id to cardId else null
        }
        if (sharedIdentities.size < MIN_SHARED_IDENTITIES) rejected += "fewer than $MIN_SHARED_IDENTITIES stable entity identities overlap"

        val sharedPlayers = earlier.entities.values.flatMap { state ->
            state.aliasHistory.mapNotNull { alias -> state.controller.value?.let { alias.value to it } }
        }.toSet().intersect(later.entities.values.flatMap { state ->
            state.aliasHistory.mapNotNull { alias -> state.controller.value?.let { alias.value to it } }
        }.toSet())
        if (sharedPlayers.size < 2) rejected += "both player/controller identities do not overlap"

        val earlierTurn = FusionSnapshotMaterializer.materialize(earlier.snapshots).lastOrNull()?.rawTurn
        val laterTurn = FusionSnapshotMaterializer.materialize(later.snapshots).firstOrNull { it.rawTurn != null }?.rawTurn
        if (earlierTurn == null || laterTurn == null || laterTurn !in earlierTurn..(earlierTurn + 1)) {
            rejected += "raw turn continuity is absent"
        }

        if (rejected.isNotEmpty()) return PowerContinuationDecision(
            ContinuationStatus.REJECTED, earlier.source.id, later.source.id, rejected,
        )

        val reasons = listOf(
            "incomplete non-truncated segment precedes completed segment within bounded session gap",
            "tracker endpoint agrees with completed segment",
            "choice ${choiceKey(sharedChoice!!.first)} resumes with identical source and offered entities",
            "${sharedIdentities.size} stable entity identities overlap",
            "both player/controller identities overlap",
            "raw turn continues from $earlierTurn to $laterTurn",
        )
        val evidence = (sharedChoice.first.offered.evidence + sharedChoice.second.offered.evidence +
            sharedChoice.second.confirmed.evidence + listOf(
                EvidenceRef(tracker.source.id, jsonPointer = "/timestamp", rule = "power-continuation/1"),
            ) + sharedIdentities.flatMap { (id, _) ->
                listOfNotNull(earlier.entities[id]?.cardId?.evidence?.lastOrNull(), later.entities[id]?.cardId?.evidence?.lastOrNull())
            }).distinct()
        return PowerContinuationDecision(
            ContinuationStatus.ACCEPTED, earlier.source.id, later.source.id, reasons, evidence,
        )
    }

    fun stitch(
        earlier: ReducedPowerMatch,
        later: ReducedPowerMatch,
        decision: PowerContinuationDecision,
    ): ReducedPowerMatch {
        require(decision.status == ContinuationStatus.ACCEPTED) { "Only an accepted continuation can be stitched" }
        require(decision.earlierSourceId == earlier.source.id && decision.laterSourceId == later.source.id) {
            "Continuation sources do not match the supplied segments"
        }
        val source = FusionSource(
            id = "power-continuation:" + hash("power-continuation", earlier.source.id, later.source.id).take(32),
            artifactSha256 = hash("artifacts", earlier.source.artifactSha256, later.source.artifactSha256),
            type = SourceType.DERIVED_FUSION,
            schema = earlier.source.schema,
            captureMethod = "stitched-power-session-continuation",
            sessionAlias = "${earlier.source.sessionAlias}->${later.source.sessionAlias}",
            matchAlias = "${earlier.source.matchAlias}->${later.source.matchAlias}",
            complete = true,
            parentSourceIds = listOf(earlier.source.id, later.source.id),
            contentSha256 = earlier.source.contentSha256 + later.source.contentSha256,
            artifactAliases = (earlier.source.artifactAliases + earlier.source.artifactSha256 +
                later.source.artifactAliases + later.source.artifactSha256).distinct().sorted(),
        )
        val events = (earlier.events + later.events).mapIndexed { index, event -> event.copy(sequence = index + 1) }
        val entities = (earlier.entities.keys + later.entities.keys).associateWith { id ->
            mergeEntity(earlier.entities[id], later.entities[id])
        }
        val choices = (earlier.choices + later.choices).groupBy(::choiceKey).values.map { revisions ->
            revisions.reduce(::mergeChoice)
        }.sortedBy { it.id.toIntOrNull() ?: Int.MAX_VALUE }
        return ReducedPowerMatch(
            source = source,
            index = earlier.index,
            startedLogTime = earlier.startedLogTime,
            endedLogTime = later.endedLogTime,
            endedEvidence = later.endedEvidence,
            completed = true,
            sourceTruncated = false,
            events = events,
            entities = entities,
            choices = choices,
            snapshots = earlier.snapshots + later.snapshots,
            diagnostics = earlier.diagnostics + later.diagnostics + listOf(
                "Observed source-session gap retained between ${earlier.source.id} and ${later.source.id}",
            ),
            continuation = decision,
        )
    }

    private fun mergeEntity(earlier: EntityState?, later: EntityState?): EntityState {
        if (earlier == null) return requireNotNull(later)
        if (later == null) return earlier
        return later.copy(
            cardId = if (later.cardId.value != null) later.cardId else earlier.cardId,
            controller = if (later.controller.value != null) later.controller else earlier.controller,
            tags = earlier.tags + later.tags,
            tagHistory = (earlier.tagHistory + later.tagHistory).distinctBy { it.evidence },
            identityHistory = (earlier.identityHistory + later.identityHistory).distinctBy { it.evidence },
            zoneHistory = (earlier.zoneHistory + later.zoneHistory).distinctBy { it.evidence },
            positionHistory = (earlier.positionHistory + later.positionHistory).distinctBy { it.evidence },
            controllerHistory = (earlier.controllerHistory + later.controllerHistory).distinctBy { it.evidence },
            visibilityHistory = (earlier.visibilityHistory + later.visibilityHistory).distinctBy { it.evidence },
            aliasHistory = (earlier.aliasHistory + later.aliasHistory).distinctBy { it.evidence },
            entityLinks = earlier.entityLinks + later.entityLinks,
        )
    }

    private fun mergeChoice(earlier: FusedChoice, later: FusedChoice) = later.copy(
        type = later.type ?: earlier.type,
        sourceEntity = later.sourceEntity ?: earlier.sourceEntity,
        offered = mergeStage(earlier.offered, later.offered),
        submitted = mergeStage(earlier.submitted, later.submitted),
        confirmed = mergeStage(earlier.confirmed, later.confirmed),
    )

    private fun mergeStage(earlier: ChoiceStage, later: ChoiceStage): ChoiceStage {
        val selected = if (later.entityRefs.isNotEmpty()) later else earlier
        return selected.copy(evidence = (earlier.evidence + later.evidence).distinct())
    }

    private fun choiceKey(choice: FusedChoice): String = choice.id.substringAfterLast(":choice:")

    private fun descriptorKey(value: String?): String? {
        if (value == null) return null
        val id = Regex("(?:id|entity)=([0-9]+)", RegexOption.IGNORE_CASE).find(value)?.groupValues?.get(1)
        val card = Regex("cardId=([^ ]*?)(?:\\]| |$)", RegexOption.IGNORE_CASE).find(value)?.groupValues?.get(1)
        return if (id != null) "$id:${card.orEmpty()}" else value.trim()
    }

    private fun resolve(match: ReducedPowerMatch, time: String?, zoneId: ZoneId): java.time.Instant? =
        if (time == null || match.source.sessionAlias == null) null else PowerClock.resolve(match.source.sessionAlias, time, zoneId)

    private fun hash(vararg parts: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(parts.joinToString("\u0000").toByteArray())
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
