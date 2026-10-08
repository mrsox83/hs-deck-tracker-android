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
        var pendingEntity: String? = null
        var activeController: Int? = null
        var rawTurn: Int? = null
        var lastEvidence: EvidenceRef? = null

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

            val tagName = event.string("tag")
            val tagValue = event.string("value")
            val tagEntity = event.string("entity")?.let(::entityReference) ?: pendingEntity
            if (tagName != null && tagValue != null && tagEntity != null) {
                observeTag(mutableEntities, tagEntity, tagName, tagValue, sequence, line, time, evidence)
                if (tagName == "TURN") rawTurn = tagValue.toIntOrNull()
                if (tagName == "CURRENT_PLAYER" && tagValue == "1") activeController = mutableEntities[tagEntity]?.tags?.get("CONTROLLER")?.toIntOrNull()
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
                    val actor = actorText?.let(::entityReference)
                    val target = targetText?.let(::entityReference)
                    observeDescriptor(mutableEntities, actorText, evidence)
                    observeDescriptor(mutableEntities, targetText, evidence)
                    if (blockStack.isEmpty()) snapshots += snapshot(
                        source, sequence, "BEFORE_ACTION", activeController, rawTurn, false, mutableEntities, evidence,
                    )
                    outputEvents += CanonicalEvent(
                        id, outputEvents.size + 1, sequence, kind, actor, target,
                        blockStack.lastOrNull(), rawTurn, activeController, listOf(evidence),
                    )
                    blockStack.addLast(id)
                }
                body.startsWith("BLOCK_END") -> {
                    if (blockStack.isEmpty()) diagnostics += "Unmatched BLOCK_END at source line $line"
                    else {
                        blockStack.removeLast()
                        if (blockStack.isEmpty()) snapshots += snapshot(
                            source, sequence, "AFTER_OUTER_ACTION", activeController, rawTurn, false, mutableEntities, evidence,
                        )
                    }
                }
                tagName == "CURRENT_PLAYER" && tagValue == "1" -> snapshots +=
                    snapshot(source, sequence, "TURN_SIGNAL", activeController, rawTurn, blockStack.isNotEmpty(), mutableEntities, evidence)
            }
        }
        if (blockStack.isNotEmpty()) diagnostics += "${blockStack.size} unterminated block(s); final snapshot is unresolved"
        if (lastEvidence != null) snapshots += snapshot(
            source, Int.MAX_VALUE, "LAST_VALID", activeController, rawTurn, blockStack.isNotEmpty(), mutableEntities, lastEvidence,
        )

        val choices = match["choices"]?.jsonArray.orEmpty().map { choice(source, it.jsonObject) }
        return ReducedPowerMatch(
            source = source,
            index = match.int("index") ?: 0,
            startedLogTime = match.string("started_log_time") ?: "",
            endedLogTime = match.string("ended_log_time"),
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
    }

    private fun snapshot(
        source: FusionSource, sequence: Int, phase: String, controller: Int?, turn: Int?, unresolved: Boolean,
        entities: Map<String, MutableEntity>, evidence: EvidenceRef,
    ) = FusionSnapshot(
        id = "${source.id}:snapshot:${phase.lowercase()}:$sequence",
        sequence = sequence,
        phase = phase,
        activeController = controller,
        rawTurn = turn,
        unresolvedBlock = unresolved,
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
        evidence = listOf(evidence),
    )

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

    private fun entityReference(value: String): String? = entityId.find(value)?.groupValues?.get(1)
        ?: value.trim().takeIf { it != "0" && it.all(Char::isDigit) }

    private fun observeDescriptor(entities: MutableMap<String, MutableEntity>, value: String?, evidence: EvidenceRef) {
        if (value == null) return
        val id = entityReference(value) ?: return
        val observedCardId = cardId.find(value)?.groupValues?.get(1).orEmpty()
        if (observedCardId.isNotEmpty()) entities.getOrPut(id) { MutableEntity(id) }.observeCard(observedCardId, evidence)
    }

    private class MutableEntity(val id: String) {
        var cardId: Claim<String> = Claim(reason = "Identity not observed")
        var controller: Claim<Int> = Claim(reason = "Controller not observed")
        val tags = linkedMapOf<String, String>()
        val history = mutableListOf<TagObservation>()
        val identities = mutableListOf<IdentityObservation>()

        fun observeCard(value: String, evidence: EvidenceRef) {
            if (value.isNotEmpty()) {
                if (identities.lastOrNull()?.cardId != value) identities += IdentityObservation(value, evidence)
                cardId = Claim(value, ClaimStatus.OBSERVED, Confidence.HIGH, "CardID observed in power record", listOf(evidence))
            }
        }
        fun observeController(value: Int?, evidence: EvidenceRef) {
            if (value != null) controller = Claim(value, ClaimStatus.OBSERVED, Confidence.HIGH, "CONTROLLER tag observed", listOf(evidence))
        }
        fun freeze() = EntityState(id, cardId, controller, tags.toMap(), history.toList(), identities.toList())
    }
}

private fun JsonObject.string(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull
private fun JsonObject.int(name: String): Int? = this[name]?.jsonPrimitive?.intOrNull
private fun JsonObject.boolean(name: String): Boolean? = this[name]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
