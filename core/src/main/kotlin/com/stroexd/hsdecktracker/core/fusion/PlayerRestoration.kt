package com.stroexd.hsdecktracker.core.fusion

internal object PlayerRestoration {
    const val RULE = "power-player-restoration/1:"
    val fields = setOf("ENTITY_ID", "PLAYER_ID", "CONTROLLER", "TURN", "CURRENT_PLAYER", "HERO_ENTITY",
        "RESOURCES", "RESOURCES_USED", "TEMP_RESOURCES", "OVERLOAD_OWED", "OVERLOAD_LOCKED",
        "HERALD_COLOSSAL_AMOUNT", "HERALD_COLOSSAL_CLASS")
    data class Tag(val name: String, val value: String, val evidence: EvidenceRef)
    class Pending(val name: String, val header: EvidenceRef) {
        private val tags = mutableListOf<Tag>()
        private var chars = name.length
        private var invalid = false
        fun add(tag: Tag) {
            chars += tag.name.length + tag.value.length
            if (tags.size >= 256 || chars > 16384) invalid = true
            if (!invalid) tags += tag
        }
        fun finish(commit: EvidenceRef): Bound? {
            if (invalid || name.isBlank() || name.length > 256 || tags.isEmpty()) return null
            fun value(key: String) = tags.filter { it.name == key }.map { it.value }.distinct().singleOrNull()
            fun positive(key: String) = value(key)?.toIntOrNull()?.takeIf { it > 0 }
            val id = positive("ENTITY_ID") ?: return null
            val player = positive("PLAYER_ID") ?: return null
            if (player !in 1..2) return null
            if (positive("CONTROLLER") != player || value("CARDTYPE") != "PLAYER") return null
            if (tags.groupBy { it.name }.values.any { group -> group.map { it.value }.distinct().size != 1 }) return null
            if (tags.any { it.evidence.sourceId != commit.sourceId ||
                    (it.evidence.sourceLine ?: 0) <= (header.sourceLine ?: 0) ||
                    (it.evidence.sourceLine ?: Int.MAX_VALUE) >= (commit.sourceLine ?: 0) }) return null
            val selected = tags.filter { it.name in fields }.distinctBy { it.name }
            if (selected.any { it.value.toIntOrNull()?.let { n -> n < 0 } != false }) return null
            if (value("CURRENT_PLAYER")?.let { it !in setOf("0", "1") } == true) return null
            val binding = tags.first { it.name == "ENTITY_ID" }.evidence
            val playerProof = tags.first { it.name == "PLAYER_ID" }.evidence
            val controllerProof = tags.first { it.name == "CONTROLLER" }.evidence
            val typeProof = tags.first { it.name == "CARDTYPE" }.evidence
            fun proof(origin: EvidenceRef, field: String) = commit.copy(rule = "$RULE$id:$field", supportingEvidence = listOf(
                SupportingEvidenceRef(commit.sourceId, header.sourceLine!!, "header"),
                SupportingEvidenceRef(commit.sourceId, origin.sourceLine!!, "origin"),
                SupportingEvidenceRef(commit.sourceId, binding.sourceLine!!, "identity"),
                SupportingEvidenceRef(commit.sourceId, playerProof.sourceLine!!, "player"),
                SupportingEvidenceRef(commit.sourceId, controllerProof.sourceLine!!, "controller"),
                SupportingEvidenceRef(commit.sourceId, typeProof.sourceLine!!, "type"),
                SupportingEvidenceRef(commit.sourceId, commit.sourceLine!!, "boundary"),
            ))
            return Bound(id.toString(), player, selected.map { it.copy(evidence = proof(it.evidence, it.name)) },
                name.takeUnless { it == "UNKNOWN HUMAN PLAYER" || it == "GameEntity" || it.startsWith("[") },
                proof(header, "ALIAS"))
        }
    }
    data class Bound(val id: String, val controller: Int, val tags: List<Tag>, val alias: String?, val aliasEvidence: EvidenceRef)
}

internal val EvidenceRef.isPlayerRestoration: Boolean get() = rule?.startsWith(PlayerRestoration.RULE) == true
internal fun EvidenceRef.observationStatus() = if (isPlayerRestoration) ClaimStatus.DERIVED else ClaimStatus.OBSERVED
