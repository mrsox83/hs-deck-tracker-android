package com.stroexd.hsdecktracker.core.fusion

object FusionProvenanceValidator {
    fun validate(match: FusedMatch): List<String> {
        val errors = mutableListOf<String>()
        val sourceIds = match.sources.mapTo(mutableSetOf()) { it.id }
        fun evidence(path: String, refs: List<EvidenceRef>) {
            if (refs.isEmpty()) errors += "$path has no evidence"
            refs.forEachIndexed { index, ref ->
                if (ref.sourceId.isBlank()) errors += "$path evidence[$index] has no sourceId"
                else if (ref.sourceId !in sourceIds) errors += "$path evidence[$index] references unknown source ${ref.sourceId}"
                if (ref.sourceLine == null && ref.jsonPointer == null) errors += "$path evidence[$index] has no exact location"
                if (ref.isPlayerRestoration || ref.supportingEvidence.isNotEmpty()) {
                    val parts = ref.rule?.removePrefix(PlayerRestoration.RULE)?.split(':').orEmpty()
                    val roles = setOf("header", "origin", "identity", "player", "controller", "type", "boundary")
                    val proof = ref.supportingEvidence
                    val at = ref.sourceLine ?: 0
                    val source = match.sources.singleOrNull { it.id == ref.sourceId }
                    val header = proof.singleOrNull { it.role == "header" }?.sourceLine ?: 0
                    val resets = match.events.filter { it.kind == "GAME_RESET" }.flatMap { it.evidence }
                        .filter { it.sourceId == ref.sourceId }.mapNotNull { it.sourceLine }
                    if (!ref.isPlayerRestoration || parts.size != 2 || parts[0].toIntOrNull()?.let { it > 0 } != true ||
                        (parts.getOrNull(1) !in PlayerRestoration.fields && parts.getOrNull(1) != "ALIAS") ||
                        source?.type != SourceType.ANDROID_POWER_EVIDENCE || at <= 0 ||
                        proof.size != roles.size || proof.map { it.role }.toSet() != roles ||
                        header <= 0 || proof.any { it.sourceId != ref.sourceId ||
                            if (it.role == "boundary") it.sourceLine != at else it.sourceLine !in header until at } ||
                        proof.filter { it.role !in setOf("header", "origin", "boundary") }.any { it.sourceLine <= header } ||
                        proof.any { it.role == "origin" && it.sourceLine == header && parts.getOrNull(1) != "ALIAS" } ||
                        proof.any { leaf -> resets.count { it <= leaf.sourceLine } != resets.count { it <= at } } ||
                        (resets.filter { it <= at }.maxOrNull()?.let { header <= it } == true) ||
                        (source?.sourceLineStart?.let { header < it } == true) ||
                        (source?.sourceLineEnd?.let { at > it } == true))
                        errors += "$path evidence[$index] invalid player restoration proof"
                }
            }
        }
        fun <T> claim(path: String, value: Claim<T>, entity: String? = null, field: String? = null) {
            val restored = value.evidence.filter { it.isPlayerRestoration }
            if (value.status != ClaimStatus.UNKNOWN || restored.isNotEmpty()) evidence(path, value.evidence)
            if (restored.isNotEmpty() && value.status != ClaimStatus.DERIVED)
                errors += "$path delayed player restoration must be DERIVED"
            if (restored.any { entity == null || field == null || it.rule != "${PlayerRestoration.RULE}$entity:$field" })
                errors += "$path restoration claim entity/field mismatch"
        }
        fun context(path: String, ref: EvidenceRef, id: String, field: String) {
            if (ref.isPlayerRestoration && ref.rule != "${PlayerRestoration.RULE}$id:$field")
                errors += "$path restoration entity/field mismatch"
        }

        match.handHistory?.let { history ->
            // Derived fields are claims about canonical evidence, not trusted imported annotations.
            try {
                if (history != CoachingHandHistoryBuilder.build(match))
                    errors += "handHistory differs from canonical source derivation"
            } catch (error: IllegalArgumentException) {
                errors += "handHistory canonical source derivation is unavailable"
            }
        }
        match.trackerTimeline?.let { summary ->
            if (match.sources.singleOrNull { it.id == summary.sourceId }?.type != SourceType.ANDROID_TRACKER_JSON)
                errors += "trackerTimeline requires one tracker source"
            if (summary.evidence.sourceId != summary.sourceId || summary.evidence.jsonPointer != "/timeline")
                errors += "trackerTimeline must reference its tracker timeline"
            evidence("trackerTimeline", listOf(summary.evidence))
        }
        match.userNote?.let { note ->
            val source = match.sources.singleOrNull { it.id == note.evidence.sourceId }
            if (source?.type != SourceType.ANDROID_TRACKER_JSON ||
                source.id != "tracker:${note.trackerSha256}" || source.artifactSha256 != note.trackerSha256 ||
                !note.trackerSha256.matches(Regex("[0-9a-f]{64}")) ||
                source.matchAlias != note.trackerMatchId || note.trackerMatchId.isBlank() || note.trackerRevision <= 0 ||
                match.trackerTimeline?.sourceId != source.id)
                errors += "userNote tracker source/hash/identity/revision mismatch"
            if (note.evidence != EvidenceRef(note.evidence.sourceId, jsonPointer = "/notes", rule = "tracker-user-note/1"))
                errors += "userNote must reference its exact tracker notes location"
            if ((note.state == UserNoteState.TEXT) != (note.text != null))
                errors += "userNote state/text mismatch"
            evidence("userNote", listOf(note.evidence))
        }
        match.sources.forEachIndexed { index, source ->
            if (source.type == SourceType.DERIVED_FUSION && source.parentSourceIds.isEmpty()) {
                errors += "sources[$index] derived source has no parents"
            }
            source.parentSourceIds.forEach { parent ->
                if (parent !in sourceIds) errors += "sources[$index] references unknown parent source $parent"
            }
        }

        if (match.pairing.status == PairingStatus.ACCEPTED) {
            val accepted = match.pairing.candidates.singleOrNull { it.powerSourceId == match.pairing.acceptedSourceId }
            if (accepted == null) errors += "pairing acceptedSourceId has no candidate"
            else evidence("pairing.accepted", accepted.evidence)
        }
        match.continuations.forEachIndexed { index, continuation ->
            if (continuation.status == ContinuationStatus.ACCEPTED) {
                if (continuation.earlierSourceId !in sourceIds) errors += "continuations[$index] references unknown earlier source ${continuation.earlierSourceId}"
                if (continuation.laterSourceId !in sourceIds) errors += "continuations[$index] references unknown later source ${continuation.laterSourceId}"
                evidence("continuations[$index]", continuation.evidence)
            }
        }
        val maxEventEpoch = match.events.maxOfOrNull { it.chronologyEpoch } ?: 0
        val eventById = match.events.associateBy { it.id }
        var priorEventEpoch = 0
        match.events.forEachIndexed { index, event ->
            evidence("events[$index]", event.evidence)
            if (event.chronologyEpoch < 0) errors += "events[$index] has negative chronology epoch"
            if (event.chronologyEpoch < priorEventEpoch) errors += "events[$index] chronology epoch moves backward"
            if (event.chronologyEpoch < maxEventEpoch && event.chronologyState != ChronologyState.HISTORICAL)
                errors += "events[$index] earlier chronology epoch must be HISTORICAL"
            if (event.chronologyEpoch == maxEventEpoch && event.chronologyState != ChronologyState.CURRENT)
                errors += "events[$index] final chronology epoch must be CURRENT"
            event.parentEventId?.let { parentId ->
                val parent = eventById[parentId]
                if (parent != null && parent.chronologyState != event.chronologyState)
                    errors += "events[$index] parent crosses chronology state"
            }
            priorEventEpoch = event.chronologyEpoch
        }
        match.entities.forEach { (id, entity) ->
            claim("entities[$id].cardId", entity.cardId)
            claim("entities[$id].controller", entity.controller, id, "CONTROLLER")
            entity.tagHistory.forEachIndexed { index, observation ->
                evidence("entities[$id].tagHistory[$index]", listOf(observation.evidence))
                context("entities[$id].tagHistory[$index]", observation.evidence, id, observation.name)
                if (observation.evidence.isPlayerRestoration && observation.sourceLine != observation.evidence.sourceLine)
                    errors += "entities[$id].tagHistory[$index] restoration availability mismatch"
            }
            entity.identityHistory.forEachIndexed { index, observation ->
                evidence("entities[$id].identityHistory[$index]", listOf(observation.evidence))
            }
            entity.zoneHistory.forEachIndexed { index, observation ->
                evidence("entities[$id].zoneHistory[$index]", listOf(observation.evidence))
            }
            entity.positionHistory.forEachIndexed { index, observation ->
                evidence("entities[$id].positionHistory[$index]", listOf(observation.evidence))
            }
            entity.controllerHistory.forEachIndexed { index, observation ->
                evidence("entities[$id].controllerHistory[$index]", listOf(observation.evidence))
                context("entities[$id].controllerHistory[$index]", observation.evidence, id, "CONTROLLER")
            }
            entity.visibilityHistory.forEachIndexed { index, observation ->
                evidence("entities[$id].visibilityHistory[$index]", listOf(observation.evidence))
            }
            entity.aliasHistory.forEachIndexed { index, observation ->
                evidence("entities[$id].aliasHistory[$index]", listOf(observation.evidence))
                context("entities[$id].aliasHistory[$index]", observation.evidence, id, "ALIAS")
            }
            entity.entityLinks.forEach { (name, link) -> claim("entities[$id].links[$name]", link, id, name) }
        }
        val archiveEpochs = match.entityEpochs.map { it.chronologyEpoch }
        if (archiveEpochs.any { it < 0 } || archiveEpochs != archiveEpochs.distinct().sorted())
            errors += "entityEpochs must have distinct nonnegative increasing epochs"
        val chronologyMax = (match.events.map { it.chronologyEpoch } + match.snapshots.map { it.chronologyEpoch }).maxOrNull()
        if (archiveEpochs.isNotEmpty() && archiveEpochs != (0..(chronologyMax ?: archiveEpochs.last())).toList())
            errors += "entityEpochs must cover every chronology epoch from zero through the final epoch"
        if ((match.fusionVersion.substringAfterLast('/').toIntOrNull() ?: 0) >= 3 && match.pairing.status == PairingStatus.ACCEPTED &&
            match.events.isNotEmpty() && archiveEpochs.isEmpty())
            errors += "/3+ accepted chronology requires entity epoch archives"
        match.entityEpochs.forEachIndexed { index, archive ->
            // Check archived delayed proofs against the root's complete reset boundaries too.
            archive.entities.forEach { (id, entity) ->
                val refs = (entity.tagHistory.map { it.evidence } + entity.aliasHistory.map { it.evidence } +
                    entity.controllerHistory.map { it.evidence } + entity.identityHistory.map { it.evidence } +
                    entity.zoneHistory.map { it.evidence } + entity.positionHistory.map { it.evidence } +
                    entity.visibilityHistory.map { it.evidence } + entity.controller.evidence + entity.cardId.evidence +
                    entity.entityLinks.values.flatMap { it.evidence }).filter { it.isPlayerRestoration || it.supportingEvidence.isNotEmpty() }
                if (refs.isNotEmpty()) evidence("entityEpochs[$index].entities[$id].restoration", refs)
            }
            // Reuse the complete entity evidence checks without recursively visiting archives.
            val archiveErrors = validate(match.copy(
                entities = archive.entities, entityEpochs = emptyList(), events = emptyList(),
                snapshots = emptyList(), choices = emptyList(), continuations = emptyList(),
                handHistory = null, trackerTimeline = null,
                pairing = PairingDecision(PairingStatus.PENDING, candidates = emptyList(), reason = "archive validation"),
            ))
            errors += archiveErrors.filter { it.startsWith("entities[") }.map { "entityEpochs[$index].$it" }
        }
        if (match.entityEpochs.isNotEmpty() && match.entities != match.entityEpochs.last().entities)
            errors += "entities must equal the final entity epoch"
        match.choices.forEachIndexed { index, choice ->
            if (choice.offered.entityRefs.isNotEmpty()) evidence("choices[$index].offered", choice.offered.evidence)
            if (choice.submitted.entityRefs.isNotEmpty()) evidence("choices[$index].submitted", choice.submitted.evidence)
            if (choice.confirmed.entityRefs.isNotEmpty()) evidence("choices[$index].confirmed", choice.confirmed.evidence)
        }
        if (match.snapshots.firstOrNull()?.stateMode == SnapshotStateMode.DELTA) {
            errors += "snapshots[0] must contain FULL state"
        }
        val maxSnapshotEpoch = match.snapshots.maxOfOrNull { it.chronologyEpoch } ?: 0
        var priorSnapshotEpoch = 0
        val seenSnapshotEpochs = mutableSetOf<Int>()
        match.snapshots.forEachIndexed { index, snapshot ->
            evidence("snapshots[$index]", snapshot.evidence)
            if (snapshot.chronologyEpoch < 0) errors += "snapshots[$index] has negative chronology epoch"
            if (snapshot.chronologyEpoch < priorSnapshotEpoch) errors += "snapshots[$index] chronology epoch moves backward"
            if (snapshot.chronologyEpoch < maxSnapshotEpoch && snapshot.chronologyState != ChronologyState.HISTORICAL)
                errors += "snapshots[$index] earlier chronology epoch must be HISTORICAL"
            if (snapshot.chronologyEpoch == maxSnapshotEpoch && snapshot.chronologyState != ChronologyState.CURRENT)
                errors += "snapshots[$index] final chronology epoch must be CURRENT"
            if (seenSnapshotEpochs.add(snapshot.chronologyEpoch) && snapshot.stateMode != SnapshotStateMode.FULL)
                errors += "snapshots[$index] must begin chronology epoch with FULL state"
            priorSnapshotEpoch = snapshot.chronologyEpoch
            snapshot.counters.forEach { (entity, counters) ->
                counters.forEach { (name, value) -> claim("snapshots[$index].counters[$entity][$name]", value, entity, name) }
            }
            snapshot.players.forEach { (controller, player) ->
                claim("snapshots[$index].players[$controller].hero", player.heroEntityId, player.playerEntityId, "HERO_ENTITY")
                claim("snapshots[$index].players[$controller].health", player.health)
                claim("snapshots[$index].players[$controller].damage", player.damage)
                claim("snapshots[$index].players[$controller].remainingHealth", player.remainingHealth)
                claim("snapshots[$index].players[$controller].armor", player.armor)
                claim("snapshots[$index].players[$controller].resources", player.resources, player.playerEntityId, "RESOURCES")
                claim("snapshots[$index].players[$controller].resourcesUsed", player.resourcesUsed, player.playerEntityId, "RESOURCES_USED")
                claim("snapshots[$index].players[$controller].temporaryResources", player.temporaryResources, player.playerEntityId, "TEMP_RESOURCES")
                claim("snapshots[$index].players[$controller].overloadOwed", player.overloadOwed, player.playerEntityId, "OVERLOAD_OWED")
                claim("snapshots[$index].players[$controller].overloadLocked", player.overloadLocked, player.playerEntityId, "OVERLOAD_LOCKED")
                claim("snapshots[$index].players[$controller].heraldAmount", player.heraldAmount, player.playerEntityId, "HERALD_COLOSSAL_AMOUNT")
                claim("snapshots[$index].players[$controller].heraldClass", player.heraldClass, player.playerEntityId, "HERALD_COLOSSAL_CLASS")
                claim("snapshots[$index].players[$controller].heraldThresholdReached", player.heraldThresholdReached)
            }
            snapshot.quests.forEach { (id, quest) ->
                claim("snapshots[$index].quests[$id].controller", quest.controller)
                claim("snapshots[$index].quests[$id].progress", quest.progress)
                claim("snapshots[$index].quests[$id].total", quest.total)
                claim("snapshots[$index].quests[$id].completed", quest.completed)
                claim("snapshots[$index].quests[$id].reward", quest.rewardEntityId)
            }
        }
        return errors
    }
}
