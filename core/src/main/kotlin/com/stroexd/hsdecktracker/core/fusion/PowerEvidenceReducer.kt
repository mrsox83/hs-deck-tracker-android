package com.stroexd.hsdecktracker.core.fusion

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object PowerEvidenceReducer {
    private val entityId = Regex("(?:id|entity)=([0-9]+)", RegexOption.IGNORE_CASE)
    private val cardId = Regex("cardId=([^ ]*)", RegexOption.IGNORE_CASE)
    private val inlineTag = Regex("tag=([^ ]+) value=(.*)")
    private val fullEntity = Regex("FULL_ENTITY - Creating ID=([0-9]+) CardID=(.*)")
    private val shownEntity = Regex("(?:SHOW_ENTITY|CHANGE_ENTITY) - Updating Entity=.*? id=([0-9]+).*? CardID=([^ ]*)")
    private val playerEntity = Regex("Player EntityID=([0-9]+) PlayerID=([0-9]+).*")
    private val gameEntity = Regex("GameEntity EntityID=([0-9]+).*")
    private val entityName = Regex("entityName=(.*?) id=([0-9]+)")
    private val linkedEntityTags = setOf(
        "CREATOR", "COPIED_FROM_ENTITY_ID", "COPY_OF_ENTITY_ID", "HERO_ENTITY", "HERO_POWER", "ATTACHED", "REWARD_ENTITY",
    )
    private val snapshotCounters = setOf(
        "RESOURCES", "RESOURCES_USED", "TEMP_RESOURCES", "OVERLOAD_OWED", "OVERLOAD_LOCKED",
        "HEALTH", "DAMAGE", "ARMOR", "HERALD_COLOSSAL_AMOUNT", "QUEST_PROGRESS",
        "QUEST_PROGRESS_TOTAL", "NUM_TURNS_IN_PLAY",
    )

    fun reduce(source: FusionSource, document: JsonObject): ReducedPowerMatch {
        val match = document.getValue("match").jsonObject
        val mutableEntities = linkedMapOf<String, MutableEntity>()
        val outputEvents = mutableListOf<CanonicalEvent>()
        val snapshots = mutableListOf<FusionSnapshot>()
        val diagnostics = mutableListOf<String>()
        val blockStack = ArrayDeque<String>()
        val aliases = linkedMapOf<String, String>()
        val playerTurnIndices = linkedMapOf<Int, Int>()
        val playerNamesByController = document["player_name_mappings"]?.jsonArray.orEmpty().mapNotNull { item ->
            val mapping = item.jsonObject
            val name = mapping.string("name") ?: return@mapNotNull null
            val controller = mapping.int("controller") ?: return@mapNotNull null
            val sourceLine = mapping.int("source_line") ?: return@mapNotNull null
            controller to (name to EvidenceRef(source.id, sourceLine, rule = "power-player-name/1"))
        }.groupBy({ it.first }, { it.second })
        var pendingEntity: String? = null
        var activeController: Int? = null
        var rawTurn: Int? = null
        var lastEvidence: EvidenceRef? = null
        var previousSnapshotState: SnapshotState? = null

        fun captureSnapshot(sequence: Int, phase: String, unresolved: Boolean, evidence: EvidenceRef) {
            val (snapshot, state) = snapshot(
                source, sequence, phase, activeController, rawTurn, playerTurnIndices,
                unresolved, mutableEntities, evidence, previousSnapshotState,
            )
            snapshots += snapshot
            previousSnapshotState = state
        }

        for (raw in match["events"]?.jsonArray.orEmpty()) {
            val event = raw.jsonObject
            val sequence = event.int("sequence") ?: continue
            val line = event.int("source_line") ?: continue
            val time = event.string("log_time") ?: ""
            val body = event.string("raw") ?: ""
            val evidence = EvidenceRef(source.id, line, observedTime = time, rule = "power-reducer/1")
            lastEvidence = evidence

            val created = fullEntity.matchEntire(body) ?: shownEntity.matchEntire(body)
            if (created != null) {
                val id = created.groupValues[1]
                pendingEntity = id
                val entity = mutableEntities.getOrPut(id) { MutableEntity(id) }
                entity.observeCard(created.groupValues[2], evidence)
            }
            playerEntity.matchEntire(body)?.let { player ->
                pendingEntity = player.groupValues[1]
                val controller = player.groupValues[2].toInt()
                observeTag(
                    mutableEntities, pendingEntity!!, "PLAYER_ID", controller.toString(), sequence, line, time, evidence,
                )
                playerNamesByController[controller].orEmpty().forEach { (name, aliasEvidence) ->
                    aliases[name] = pendingEntity!!
                    mutableEntities.getValue(pendingEntity!!).observeAlias(name, aliasEvidence)
                }
            }
            gameEntity.matchEntire(body)?.let { game -> pendingEntity = game.groupValues[1] }
            observeAlias(body, aliases)

            val tagName = event.string("tag")
            val tagValue = event.string("value")
            val tagEntityText = event.string("entity")
            observeAlias(tagEntityText, aliases)
            val tagEntity = if (tagEntityText != null) entityReference(tagEntityText, aliases) else pendingEntity
            if (tagName != null && tagValue != null && tagEntity != null) {
                observeTag(mutableEntities, tagEntity, tagName, tagValue, sequence, line, time, evidence)
                if (tagName == "TURN") rawTurn = tagValue.toIntOrNull()
                if (tagName == "CURRENT_PLAYER" && tagValue == "1") {
                    val nextController = mutableEntities[tagEntity]?.controller?.value
                        ?: mutableEntities[tagEntity]?.tags?.get("PLAYER_ID")?.toIntOrNull()
                    if (nextController != null && nextController != activeController) {
                        playerTurnIndices[nextController] = (playerTurnIndices[nextController] ?: 0) + 1
                        activeController = nextController
                    }
                }
            } else {
                val inline = inlineTag.matchEntire(body)
                if (inline != null && pendingEntity != null) {
                    observeTag(mutableEntities, pendingEntity!!, inline.groupValues[1], inline.groupValues[2], sequence, line, time, evidence)
                }
            }

            when {
                body.startsWith("BLOCK_START") -> {
                    val id = "${source.id}:event:$sequence"
                    val kind = event.string("block_type") ?: "BLOCK"
                    val actorText = event.string("actor")
                    val targetText = event.string("target")
                    observeAlias(actorText, aliases)
                    observeAlias(targetText, aliases)
                    val actor = actorText?.let { entityReference(it, aliases) }
                    val target = targetText?.let { entityReference(it, aliases) }
                    observeDescriptor(mutableEntities, actorText, evidence, aliases)
                    observeDescriptor(mutableEntities, targetText, evidence, aliases)
                    if (blockStack.isEmpty()) captureSnapshot(sequence, "BEFORE_ACTION", false, evidence)
                    outputEvents += CanonicalEvent(
                        id, outputEvents.size + 1, sequence, kind, actor, target,
                        blockStack.lastOrNull(), rawTurn, activeController,
                        activeController?.let { playerTurnIndices[it] }, listOf(evidence),
                    )
                    blockStack.addLast(id)
                }
                body.startsWith("BLOCK_END") -> {
                    if (blockStack.isEmpty()) diagnostics += "Unmatched BLOCK_END at source line $line"
                    else {
                        blockStack.removeLast()
                        if (blockStack.isEmpty()) captureSnapshot(sequence, "AFTER_OUTER_ACTION", false, evidence)
                    }
                }
                tagName == "CURRENT_PLAYER" && tagValue == "1" ->
                    captureSnapshot(sequence, "TURN_SIGNAL", blockStack.isNotEmpty(), evidence)
            }
            if (body.startsWith("SHOW_ENTITY")) {
                val id = event.string("entity_id") ?: entityReference(body, aliases)
                if (id != null) mutableEntities.getOrPut(id) { MutableEntity(id) }.observeVisibility(true, evidence)
            } else if (body.startsWith("HIDE_ENTITY")) {
                val id = entityReference(body, aliases)
                if (id != null) mutableEntities.getOrPut(id) { MutableEntity(id) }.observeVisibility(false, evidence)
            }
        }
        if (blockStack.isNotEmpty()) diagnostics += "${blockStack.size} unterminated block(s); final snapshot is unresolved"
        if (lastEvidence != null) captureSnapshot(Int.MAX_VALUE, "LAST_VALID", blockStack.isNotEmpty(), lastEvidence)

        val choices = match["choices"]?.jsonArray.orEmpty().map { choice(source, it.jsonObject) }
        val endedEvidence = match["events"]?.jsonArray.orEmpty().asSequence().map { it.jsonObject }.lastOrNull { event ->
            event.string("tag") == "STEP" && event.string("value") == "FINAL_GAMEOVER"
        }?.let { event ->
            EvidenceRef(source.id, event.int("source_line"), observedTime = event.string("log_time"), rule = "power-final-gameover/1")
        }
        return ReducedPowerMatch(
            source = source,
            index = match.int("index") ?: 0,
            startedLogTime = match.string("started_log_time") ?: "",
            endedLogTime = match.string("ended_log_time"),
            endedEvidence = endedEvidence,
            completed = match.boolean("completed") ?: false,
            sourceTruncated = match.boolean("source_truncated") ?: false,
            events = outputEvents,
            entities = mutableEntities.mapValues { it.value.freeze() },
            choices = choices,
            snapshots = snapshots,
            diagnostics = diagnostics,
        )
    }

    private fun observeTag(
        entities: MutableMap<String, MutableEntity>, id: String, name: String, value: String,
        sequence: Int, line: Int, time: String, evidence: EvidenceRef,
    ) {
        val entity = entities.getOrPut(id) { MutableEntity(id) }
        entity.tags[name] = value
        entity.history += TagObservation(sequence, line, time, name, value, evidence)
        if (name == "CONTROLLER") entity.observeController(value.toIntOrNull(), evidence)
        if (name == "ZONE") entity.observeZone(value, evidence)
        if (name == "ZONE_POSITION") entity.observePosition(value.toIntOrNull(), evidence)
        if (name in linkedEntityTags && value.toIntOrNull() != null) entity.observeLink(name, value, evidence)
    }

    private fun snapshot(
        source: FusionSource, sequence: Int, phase: String, controller: Int?, turn: Int?, turnIndices: Map<Int, Int>, unresolved: Boolean,
        entities: Map<String, MutableEntity>, evidence: EvidenceRef, previous: SnapshotState?,
    ): Pair<FusionSnapshot, SnapshotState> {
        val current = SnapshotState(
            entityTags = entities.mapValues { it.value.tags.toMap() },
            counters = entities.mapValues { (_, entity) ->
            entity.tags.mapNotNull { (name, rawValue) ->
                if (name !in snapshotCounters) return@mapNotNull null
                val value = rawValue.toIntOrNull() ?: return@mapNotNull null
                val observation = entity.history.lastOrNull { it.name == name } ?: return@mapNotNull null
                name to Claim(
                    value = value,
                    status = ClaimStatus.OBSERVED,
                    confidence = Confidence.HIGH,
                    reason = "$name tag observed at snapshot boundary",
                    evidence = listOf(observation.evidence),
                )
            }.toMap()
        }.filterValues { it.isNotEmpty() },
            players = playerSnapshots(entities),
            quests = questSnapshots(entities),
        )
        val isFull = previous == null || phase == "TURN_SIGNAL" || phase == "LAST_VALID"
        val snapshot = FusionSnapshot(
            id = "${source.id}:snapshot:${phase.lowercase()}:$sequence",
            sequence = sequence,
            phase = phase,
            stateMode = if (isFull) SnapshotStateMode.FULL else SnapshotStateMode.DELTA,
            activeController = controller,
            rawTurn = turn,
            playerTurnIndices = turnIndices.toMap(),
            unresolvedBlock = unresolved,
            entityTags = if (isFull) current.entityTags else nestedDelta(current.entityTags, previous!!.entityTags),
            counters = if (isFull) current.counters else nestedDelta(current.counters, previous!!.counters),
            players = if (isFull) current.players else mapDelta(current.players, previous!!.players),
            quests = if (isFull) current.quests else mapDelta(current.quests, previous!!.quests),
            evidence = listOf(evidence),
        )
        return snapshot to current
    }

    private fun <K, V> mapDelta(current: Map<K, V>, previous: Map<K, V>): Map<K, V> =
        current.filter { (key, value) -> previous[key] != value }

    private fun <K1, K2, V> nestedDelta(
        current: Map<K1, Map<K2, V>>,
        previous: Map<K1, Map<K2, V>>,
    ): Map<K1, Map<K2, V>> = current.mapNotNull { (outerKey, currentValues) ->
        val changes = currentValues.filter { (key, value) -> previous[outerKey]?.get(key) != value }
        if (changes.isEmpty()) null else outerKey to changes
    }.toMap()

    private fun playerSnapshots(entities: Map<String, MutableEntity>): Map<Int, PlayerSnapshot> = entities.values.mapNotNull { player ->
        val controller = player.controller.value ?: player.tags["PLAYER_ID"]?.toIntOrNull() ?: return@mapNotNull null
        val heroLink = player.links["HERO_ENTITY"] ?: return@mapNotNull null
        val heroId = heroLink.value ?: return@mapNotNull null
        val hero = entities[heroId]
        fun observed(entity: MutableEntity?, name: String): Claim<Int> {
            val value = entity?.tags?.get(name)?.toIntOrNull()
                ?: return Claim(reason = "$name not observed for current hero/player")
            val observation = entity.history.lastOrNull { it.name == name }
                ?: return Claim(reason = "$name has no retained observation")
            return Claim(value, ClaimStatus.OBSERVED, Confidence.HIGH, "$name tag observed", listOf(observation.evidence))
        }
        val health = observed(hero, "HEALTH")
        val damage = observed(hero, "DAMAGE")
        val remaining = if (health.value != null && damage.value != null) Claim(
            value = health.value - damage.value,
            status = ClaimStatus.DERIVED,
            confidence = Confidence.HIGH,
            reason = "Current hero HEALTH minus DAMAGE",
            evidence = (health.evidence + damage.evidence).distinct(),
        ) else Claim(reason = "Current hero HEALTH and DAMAGE are both required")
        controller to PlayerSnapshot(
            controller = controller,
            playerEntityId = player.id,
            heroEntityId = heroLink,
            health = health,
            damage = damage,
            remainingHealth = remaining,
            armor = observed(hero, "ARMOR"),
            resources = observed(player, "RESOURCES"),
            resourcesUsed = observed(player, "RESOURCES_USED"),
            temporaryResources = observed(player, "TEMP_RESOURCES"),
            overloadOwed = observed(player, "OVERLOAD_OWED"),
            overloadLocked = observed(player, "OVERLOAD_LOCKED"),
            heraldAmount = observed(player, "HERALD_COLOSSAL_AMOUNT"),
            heraldClass = observed(player, "HERALD_COLOSSAL_CLASS"),
        )
    }.toMap()

    private fun questSnapshots(entities: Map<String, MutableEntity>): Map<String, QuestSnapshot> = entities.values
        .filter { entity -> entity.tags.keys.any { it in setOf("QUEST_PROGRESS", "QUEST_PROGRESS_TOTAL", "QUEST_COMPLETED") } }
        .associate { entity ->
            fun observedInt(name: String): Claim<Int> {
                val value = entity.tags[name]?.toIntOrNull() ?: return Claim(reason = "$name not observed")
                val observation = entity.history.lastOrNull { it.name == name }
                    ?: return Claim(reason = "$name has no retained observation")
                return Claim(value, ClaimStatus.OBSERVED, Confidence.HIGH, "$name tag observed", listOf(observation.evidence))
            }
            val completedValue = entity.tags["QUEST_COMPLETED"]?.toIntOrNull()
            val completedObservation = entity.history.lastOrNull { it.name == "QUEST_COMPLETED" }
            val completed = if (completedValue != null && completedObservation != null) Claim(
                completedValue != 0, ClaimStatus.OBSERVED, Confidence.HIGH,
                "QUEST_COMPLETED tag observed", listOf(completedObservation.evidence),
            ) else Claim(reason = "QUEST_COMPLETED not observed; progress equality is not treated as completion")
            entity.id to QuestSnapshot(
                entityId = entity.id,
                controller = entity.controller,
                progress = observedInt("QUEST_PROGRESS"),
                total = observedInt("QUEST_PROGRESS_TOTAL"),
                completed = completed,
                rewardEntityId = entity.links["REWARD_ENTITY"] ?: Claim(reason = "REWARD_ENTITY not observed"),
            )
        }

    private fun choice(source: FusionSource, value: JsonObject): FusedChoice {
        fun stage(field: String, timeField: String, methods: Set<String>): ChoiceStage {
            val refs = value[field]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val evidence = value["evidence"]?.jsonArray.orEmpty().mapNotNull { item ->
                val obj = item.jsonObject
                if (obj.string("method") !in methods) null else EvidenceRef(
                    source.id, obj.int("source_line"), observedTime = obj.string("log_time"), rule = "power-choice/1",
                )
            }
            return ChoiceStage(refs, value.string(timeField), evidence)
        }
        val id = value.string("choice_id") ?: "unknown"
        return FusedChoice(
            id = "${source.id}:choice:$id",
            type = value.string("choice_type"),
            sourceEntity = value.string("source"),
            offered = stage("options", "log_time", setOf("DebugPrintEntityChoices")),
            submitted = stage("submitted_entities", "submitted_log_time", setOf("SendChoices")),
            confirmed = stage("confirmed_entities", "confirmed_log_time", setOf("DebugPrintEntitiesChosen")),
        )
    }

    private fun entityReference(value: String, aliases: Map<String, String>): String? = entityId.find(value)?.groupValues?.get(1)
        ?: value.trim().takeIf { it != "0" && it.all(Char::isDigit) }
        ?: aliases[value.trim()]

    private fun observeDescriptor(entities: MutableMap<String, MutableEntity>, value: String?, evidence: EvidenceRef, aliases: Map<String, String>) {
        if (value == null) return
        val id = entityReference(value, aliases) ?: return
        val observedCardId = cardId.find(value)?.groupValues?.get(1).orEmpty()
        if (observedCardId.isNotEmpty()) entities.getOrPut(id) { MutableEntity(id) }.observeCard(observedCardId, evidence)
    }

    private fun observeAlias(value: String?, aliases: MutableMap<String, String>) {
        if (value == null) return
        val match = entityName.find(value) ?: return
        aliases[match.groupValues[1]] = match.groupValues[2]
    }

    private class MutableEntity(val id: String) {
        var cardId: Claim<String> = Claim(reason = "Identity not observed")
        var controller: Claim<Int> = Claim(reason = "Controller not observed")
        val tags = linkedMapOf<String, String>()
        val history = mutableListOf<TagObservation>()
        val identities = mutableListOf<IdentityObservation>()
        val zones = mutableListOf<ValueObservation<String>>()
        val positions = mutableListOf<ValueObservation<Int>>()
        val controllers = mutableListOf<ValueObservation<Int>>()
        val visibility = mutableListOf<ValueObservation<Boolean>>()
        val entityAliases = mutableListOf<ValueObservation<String>>()
        val links = linkedMapOf<String, Claim<String>>()

        fun observeCard(value: String, evidence: EvidenceRef) {
            if (value.isNotEmpty()) {
                if (identities.lastOrNull()?.cardId != value) identities += IdentityObservation(value, evidence)
                cardId = Claim(value, ClaimStatus.OBSERVED, Confidence.HIGH, "CardID observed in power record", listOf(evidence))
            }
        }
        fun observeController(value: Int?, evidence: EvidenceRef) {
            if (value != null) {
                if (controllers.lastOrNull()?.value != value) controllers += ValueObservation(value, evidence)
                controller = Claim(value, ClaimStatus.OBSERVED, Confidence.HIGH, "CONTROLLER tag observed", listOf(evidence))
            }
        }
        fun observeZone(value: String, evidence: EvidenceRef) {
            if (zones.lastOrNull()?.value != value) zones += ValueObservation(value, evidence)
        }
        fun observePosition(value: Int?, evidence: EvidenceRef) {
            if (value != null && positions.lastOrNull()?.value != value) positions += ValueObservation(value, evidence)
        }
        fun observeVisibility(value: Boolean, evidence: EvidenceRef) {
            if (visibility.lastOrNull()?.value != value) visibility += ValueObservation(value, evidence)
        }
        fun observeAlias(value: String, evidence: EvidenceRef) {
            if (entityAliases.none { it.value == value }) entityAliases += ValueObservation(value, evidence)
        }
        fun observeLink(name: String, value: String, evidence: EvidenceRef) {
            links[name] = Claim(value, ClaimStatus.OBSERVED, Confidence.HIGH, "$name entity link observed", listOf(evidence))
        }
        fun freeze() = EntityState(
            id, cardId, controller, tags.toMap(), history.toList(), identities.toList(), zones.toList(),
            positions.toList(), controllers.toList(), visibility.toList(), entityAliases.toList(), links.toMap(),
        )
    }

    private data class SnapshotState(
        val entityTags: Map<String, Map<String, String>>,
        val counters: Map<String, Map<String, Claim<Int>>>,
        val players: Map<Int, PlayerSnapshot>,
        val quests: Map<String, QuestSnapshot>,
    )
}

private fun JsonObject.string(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull
private fun JsonObject.int(name: String): Int? = this[name]?.jsonPrimitive?.intOrNull
private fun JsonObject.boolean(name: String): Boolean? = this[name]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
