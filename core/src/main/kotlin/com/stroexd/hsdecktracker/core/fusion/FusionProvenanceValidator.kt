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
            }
        }
        fun <T> claim(path: String, value: Claim<T>) {
            if (value.status != ClaimStatus.UNKNOWN) evidence(path, value.evidence)
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
            claim("entities[$id].controller", entity.controller)
            entity.tagHistory.forEachIndexed { index, observation ->
                evidence("entities[$id].tagHistory[$index]", listOf(observation.evidence))
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
            }
            entity.visibilityHistory.forEachIndexed { index, observation ->
                evidence("entities[$id].visibilityHistory[$index]", listOf(observation.evidence))
            }
            entity.aliasHistory.forEachIndexed { index, observation ->
                evidence("entities[$id].aliasHistory[$index]", listOf(observation.evidence))
            }
            entity.entityLinks.forEach { (name, link) -> claim("entities[$id].links[$name]", link) }
        }
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
                counters.forEach { (name, value) -> claim("snapshots[$index].counters[$entity][$name]", value) }
            }
            snapshot.players.forEach { (controller, player) ->
                claim("snapshots[$index].players[$controller].hero", player.heroEntityId)
                claim("snapshots[$index].players[$controller].health", player.health)
                claim("snapshots[$index].players[$controller].damage", player.damage)
                claim("snapshots[$index].players[$controller].remainingHealth", player.remainingHealth)
                claim("snapshots[$index].players[$controller].armor", player.armor)
                claim("snapshots[$index].players[$controller].resources", player.resources)
                claim("snapshots[$index].players[$controller].resourcesUsed", player.resourcesUsed)
                claim("snapshots[$index].players[$controller].temporaryResources", player.temporaryResources)
                claim("snapshots[$index].players[$controller].overloadOwed", player.overloadOwed)
                claim("snapshots[$index].players[$controller].overloadLocked", player.overloadLocked)
                claim("snapshots[$index].players[$controller].heraldAmount", player.heraldAmount)
                claim("snapshots[$index].players[$controller].heraldClass", player.heraldClass)
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
