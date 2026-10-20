package com.stroexd.hsdecktracker.core.fusion

import kotlinx.serialization.json.*

/** An in-memory source association, never a claim of human visibility or knowledge. */
enum class ObserverAssociationStatus { CANDIDATE, UNKNOWN, CONFLICTED }

data class ObserverAssociation(
    val sourceId: String,
    val chronologyEpoch: Int,
    val sourceLine: Int,
    val status: ObserverAssociationStatus,
    val candidateController: Int? = null,
    val evidence: List<EvidenceRef> = emptyList(),
    val diagnostics: List<String>,
)

/**
 * Uses existing adapter-derived choice evidence and alias mappings; no I/O or model mutation.
 * Prefix invariance applies to valid, located adapter records. Unlocated/malformed records
 * cannot safely be assigned to the future and may invalidate an earlier candidate.
 */
object ObserverAssociationResolver {
    private val offerHeader = Regex("id=([0-9]+) Player=(.+?) TaskList=.*")
    private val sendHeader = Regex("id=([0-9]+)(?: .*)?")
    private val reset = Regex("BLOCK_START BlockType=GAME_RESET(?: .*)?")

    fun resolve(source: FusionSource, document: JsonObject, epoch: Int, cutoff: Int,
        check: () -> Unit = {}): ObserverAssociation {
        fun result(status: ObserverAssociationStatus, reason: String, evidence: List<EvidenceRef> = emptyList(),
            controller: Int? = null) = ObserverAssociation(source.id, epoch, cutoff, status, controller,
            evidence.distinct(), listOf(reason, "CANDIDATE_IS_NOT_VERIFIED_OBSERVER_KNOWLEDGE"))
        fun unknown(reason: String) = result(ObserverAssociationStatus.UNKNOWN, reason)
        check()
        if (source.type != SourceType.ANDROID_POWER_EVIDENCE || source.id.isBlank() || epoch < 0 || cutoff <= 0)
            return unknown("UNSUPPORTED_SOURCE_OR_LOCATION")
        if (source.sourceLineStart?.let { cutoff < it } == true || source.sourceLineEnd?.let { cutoff > it } == true)
            return unknown("CUTOFF_OUTSIDE_SOURCE")
        val match = document["match"] as? JsonObject ?: return unknown("MATCH_UNAVAILABLE")
        if (source.sessionAlias != null && source.sessionAlias != document.text("source_session") ||
            source.matchAlias != null && source.matchAlias != match.text("index"))
            return unknown("SOURCE_DOCUMENT_SCOPE_MISMATCH")
        val events = match["events"] as? JsonArray ?: return unknown("EVENT_LOCATIONS_UNAVAILABLE")
        val start = (events.firstOrNull() as? JsonObject)?.number("source_line") ?: return unknown("MATCH_START_UNAVAILABLE")
        val end = (events.lastOrNull() as? JsonObject)?.number("source_line") ?: return unknown("MATCH_END_UNAVAILABLE")
        if (start <= 0 || cutoff !in start..end) return unknown("CUTOFF_OUTSIDE_MATCH")
        if (source.sourceLineStart?.let { start < it } == true || source.sourceLineEnd?.let { end > it } == true)
            return unknown("MATCH_OUTSIDE_SOURCE")
        var last = 0
        var locatedEpoch = 0
        var epochStart = start - 1
        for (item in events) {
            check()
            val event = item as? JsonObject ?: return unknown("MALFORMED_EVENT_LOCATION")
            val line = event.number("source_line") ?: return unknown("MALFORMED_EVENT_LOCATION")
            if (line > cutoff) break // Later evidence cannot change an earlier valid view.
            if (line <= last || line < start) return unknown("AMBIGUOUS_EVENT_ORDER")
            last = line
            val raw = event.text("raw") ?: return unknown("RAW_EVENT_UNAVAILABLE")
            val structuredReset = raw.startsWith("BLOCK_START") && event.text("block_type") == "GAME_RESET"
            if (reset.matches(raw) && !structuredReset) return unknown("UNCLASSIFIED_RESET_BOUNDARY")
            if (structuredReset) { locatedEpoch++; epochStart = line }
        }
        if (epoch != locatedEpoch) return unknown("EPOCH_CUTOFF_MISMATCH")

        val choices = match["choices"] as? JsonArray ?: return unknown("CHOICE_EVIDENCE_UNAVAILABLE")
        val headers = mutableListOf<Header>()
        for ((choiceIndex, item) in choices.withIndex()) {
            check()
            val choice = item as? JsonObject ?: return unknown("MALFORMED_CHOICE")
            val records = choice["evidence"] as? JsonArray ?: return unknown("CHOICE_EVIDENCE_UNAVAILABLE")
            for ((recordIndex, record) in records.withIndex()) {
                check()
                val observed = record as? JsonObject ?: return unknown("MALFORMED_CHOICE_EVIDENCE")
                val method = observed.text("method")
                if (method !in setOf("DebugPrintEntityChoices", "SendChoices")) continue
                val line = observed.number("source_line") ?: return unknown("UNLOCATED_CHOICE_EVIDENCE")
                if (line <= epochStart || line > cutoff || line < start) continue
                val raw = observed.text("raw") ?: return unknown("RAW_CHOICE_UNAVAILABLE")
                if (!raw.startsWith("id=")) continue // Options/submitted-entity continuation, not a header.
                val parsed = (if (method == "SendChoices") sendHeader else offerHeader).matchEntire(raw)
                    ?: return unknown("UNSUPPORTED_CHOICE_HEADER")
                val id = parsed.groupValues[1]
                if (choice.text("choice_id") != id) return unknown("CHOICE_ID_DISAGREEMENT")
                headers += Header(id, line, if (method == "SendChoices") null else parsed.groupValues[2],
                    EvidenceRef(source.id, line, "/match/choices/$choiceIndex/evidence/$recordIndex",
                        observed.text("log_time"), "observer-association/1"))
            }
        }
        // One raw line cannot be two different offers/submissions. Exact duplicates are harmless.
        if (headers.groupBy { it.line }.values.any { group -> group.map { Triple(it.id, it.alias, it.line) }.distinct().size != 1 })
            return result(ObserverAssociationStatus.CONFLICTED, "CONFLICTING_HEADER_LOCATION")
        val unique = headers.distinctBy { Triple(it.id, it.alias, it.line) }
        val submissions = unique.filter { it.alias == null }
        if (submissions.isEmpty()) return unknown("NO_SUBMISSION_BY_CUTOFF_IN_EPOCH")
        val aliases = document["player_name_mappings"] as? JsonArray ?: return unknown("ALIAS_MAPPINGS_UNAVAILABLE")
        val controllers = mutableSetOf<Int>()
        val refs = mutableListOf<EvidenceRef>()
        var unresolved = false
        for (send in submissions) {
            check()
            val offers = unique.filter { it.id == send.id && it.alias != null && it.line < send.line }
            if (offers.size > 1) return result(ObserverAssociationStatus.CONFLICTED, "AMBIGUOUS_CHOICE_OFFER", listOf(send.ref) + offers.map { it.ref })
            val offer = offers.singleOrNull()
            if (offer == null) { unresolved = true; continue }
            val mappings = mutableListOf<Pair<Int, EvidenceRef>>()
            for ((index, entry) in aliases.withIndex()) {
                check()
                val alias = entry as? JsonObject ?: return unknown("MALFORMED_ALIAS_MAPPING")
                if (alias.text("name") != offer.alias) continue
                val line = alias.number("source_line") ?: return unknown("UNLOCATED_ALIAS_MAPPING")
                if (line <= epochStart || line < start || line >= offer.line) continue
                val owner = alias.number("controller")?.takeIf { it > 0 } ?: return unknown("INVALID_ALIAS_CONTROLLER")
                mappings += owner to EvidenceRef(source.id, line, "/player_name_mappings/$index", rule = "observer-association/1")
            }
            val owners = mappings.map { it.first }.distinct()
            if (owners.size > 1) return result(ObserverAssociationStatus.CONFLICTED, "CONFLICTING_ALIAS_MAPPING", listOf(offer.ref, send.ref) + mappings.map { it.second })
            if (owners.isEmpty()) { unresolved = true; continue }
            controllers += owners.single()
            refs += offer.ref; refs += send.ref; refs += mappings.map { it.second }
        }
        if (controllers.size > 1) return result(ObserverAssociationStatus.CONFLICTED, "SUBMISSIONS_ASSOCIATE_MULTIPLE_CONTROLLERS", refs)
        if (unresolved || controllers.isEmpty()) return result(ObserverAssociationStatus.UNKNOWN, "INCOMPLETE_SUBMISSION_ASSOCIATION", refs)
        return result(ObserverAssociationStatus.CANDIDATE, "UNIQUE_SUBMISSION_ASSOCIATION_NOT_VISIBILITY_PROOF", refs, controllers.single())
    }

    private data class Header(val id: String, val line: Int, val alias: String?, val ref: EvidenceRef)
    private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.number(key: String) = (this[key] as? JsonPrimitive)?.intOrNull
}
