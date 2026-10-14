package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.coroutines.*
import java.io.File
import java.time.ZoneId

data class DiscoveryBundle(val collectionId: String, val file: File, val sha256: String)
data class DiscoveryCandidate(val sourceId: String, val label: String, val reasons: List<String>, val rejected: List<String>,
    val sessions: List<String> = emptyList())
data class PostMatchDiscoveryResult(val matchId: String, val bundles: List<DiscoveryBundle>,
    val pairing: PairingDecision, val candidates: List<DiscoveryCandidate>, val notes: List<String>,
    val pendingSessions: List<String> = emptyList(), val searchedSessions: List<String> = emptyList(),
    val stopReasons: List<String> = emptyList(), val complete: Boolean = true,
    val rawBytes: Long = 0, val decodedBytes: Long = 0, val documents: Int = 0,
    internal val reduced: List<ReducedPowerMatch> = emptyList()) {
    fun statusText(): String = (stopReasons + pairing.reason + notes).distinct().joinToString("\n")
}

/** Explicit foreground search. Evidence is retained per session; preview never writes fusion/history. */
class PostMatchDiscovery(private val repository: PostMatchCollectionRepository, private val zone: ZoneId,
    private val elapsedMillis: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private class SearchLimit(message: String) : Exception(message)

    suspend fun search(record: MatchRecord, names: List<String>, cards: Set<String>,
        previous: PostMatchDiscoveryResult? = null,
        snapshot: suspend (String, Long) -> PostMatchLogSnapshot.Snapshot): PostMatchDiscoveryResult {
        require(previous == null || previous.matchId == record.id) { "Search belongs to another game" }
        val caller = currentCoroutineContext()
        val start = elapsedMillis()
        fun check() {
            caller.ensureActive()
            if (elapsedMillis() - start >= FusionDiscoveryPolicy.DEADLINE_MILLIS) throw SearchLimit("60-second search deadline reached")
        }
        val remainingSessions = previous?.pendingSessions ?: FusionDiscoveryPolicy.sessions(names, record.timestamp, zone)
        val plan = remainingSessions.take(FusionDiscoveryPolicy.BATCH_SIZE)
        val bundles = previous?.bundles.orEmpty().toMutableList()
        val powers = previous?.reduced.orEmpty().toMutableList()
        val notes = previous?.notes.orEmpty().filterNot { it.startsWith("Inspected ") || it.startsWith("Search scope:") }.toMutableList()
        val stops = previous?.stopReasons.orEmpty().toMutableList()
        val searched = previous?.searchedSessions.orEmpty().toMutableList()
        var complete = previous?.complete ?: true
        var rawBytes = previous?.rawBytes ?: 0L
        var decodedBytes = previous?.decodedBytes ?: 0L
        var documents = previous?.documents ?: 0
        // Reuse only the same immutable saved bundles; confirmation also rechecks their raw inputs.
        for (bundle in previous?.bundles.orEmpty()) {
            require(repository.entries.value.any { it.id == bundle.collectionId && it.matchId == record.id }) { "Saved search ownership changed" }
            withContext(Dispatchers.IO) {
                require(bundle.file.isFile && EvidenceAdapters.sha256(bundle.file.readBytes()) == bundle.sha256) { "Saved search evidence changed; start a new search" }
            }
        }
        val tracker = EvidenceAdapters.tracker(AppJson.encodeToString(MatchRecord.serializer(), record).toByteArray())
        var lastEvaluation = FusionEvaluation(powers, previous?.pairing?.let(::listOf).orEmpty())
        for (session in plan) {
            var entry: LogCollectionEntry? = null
            try {
                check()
                searched += session
                val remaining = FusionDiscoveryPolicy.MAX_RAW_BYTES - rawBytes
                if (remaining <= 0) throw SearchLimit("64 MiB raw search limit reached")
                entry = repository.begin(record.id)
                val raw = snapshot(session, minOf(remaining, PostMatchLogSnapshot.MAX_BYTES))
                require(raw.session == session) { "Snapshot session changed" }
                require(FusionDiscoveryPolicy.permitsRawBytes(rawBytes, raw.sourceBytes)) { "Raw search byte limit exceeded" }
                repository.retain(entry.id, raw)
                rawBytes += raw.sourceBytes
                check()
                val bundle = repository.buildBundle(entry.id) { check(); false }
                val bytes = withContext(Dispatchers.IO) { bundle.readBytes() }
                bundles += DiscoveryBundle(entry.id, bundle, EvidenceAdapters.sha256(bytes))
                check()
                val parsed = withContext(Dispatchers.Default) { EvidenceAdapters.exporterBundle(bytes) }
                notes += parsed.diagnostics
                require(parsed.diagnostics.none { it.contains("hash mismatch", true) }) { "Bundle hash mismatch" }
                // RAW_ONLY_PARSE_FAILED must not masquerade as an empty unrelated session.
                java.util.zip.ZipFile(bundle).use { zip ->
                    val manifest = zip.getEntry("manifest.json") ?: error("Bundle manifest missing")
                    val text = zip.getInputStream(manifest).use { it.readBytes().decodeToString() }
                    require(!text.contains("RAW_ONLY_PARSE_FAILED")) { "Session parsing failed; raw evidence retained" }
                }
                documents += parsed.matches.size
                if (documents > FusionDiscoveryPolicy.MAX_MATCHES) throw SearchLimit("24 decoded-match search limit reached")
                for ((source, document) in parsed.matches) {
                    check()
                    decodedBytes += document.toString().toByteArray().size
                    if (decodedBytes > 32L * 1024 * 1024) throw SearchLimit("32 MiB decoded search limit reached")
                    powers += withContext(Dispatchers.Default) { PowerEvidenceReducer.reduce(source, document) }
                    check()
                }
                // Keep useful candidate results before attempting another expensive session.
                lastEvaluation = withContext(Dispatchers.Default) { FusionEvaluator.evaluate(tracker, powers, zone, cards, ::check) }
            } catch (error: CancellationException) {
                entry?.let { withContext(NonCancellable) { repository.fail(it.id, "Discovery cancelled; retained raw evidence preserved") } }
                throw error
            } catch (error: SearchLimit) {
                complete = false
                stops += error.message.orEmpty()
                entry?.let { repository.fail(it.id, error.message.orEmpty()) }
                break
            } catch (error: Exception) {
                complete = false
                stops += "Unreadable/unprocessed session: ${PostMatchPresentation.sessionLabel(session)} — ${error.message ?: "read/parse failed"}"
                entry?.let {
                    if (repository.entries.value.first { saved -> saved.id == it.id }.logs.isEmpty())
                        repository.recordReadFailure(it.id, session, error.message ?: "Discovery failed")
                    else repository.fail(it.id, error.message ?: "Discovery parsing failed; raw retained")
                }
            }
        }
        val evaluation = try {
            check()
            withContext(Dispatchers.Default) { FusionEvaluator.evaluate(tracker, powers, zone, cards, ::check) }
        } catch (error: SearchLimit) {
            complete = false; stops += error.message.orEmpty(); lastEvaluation
        }
        val pairing = evaluation.discoveryDecision(complete)
        val candidates = pairing.candidates.map { candidate ->
            val power = evaluation.powers.firstOrNull { it.source.id == candidate.powerSourceId }
            val label = if (power == null) candidate.powerSourceId else
                "${if (power.continuation != null) "Verified reconnect" else power.source.sessionAlias?.let(PostMatchPresentation::sessionLabel) ?: "Unknown session"} • game ${power.index} • end ${power.endedLogTime ?: "unknown"} • ${power.events.size} events"
            val aliases = if (power == null) emptyList() else
                ((if (power.continuation == null) listOfNotNull(power.source.sessionAlias) else emptyList()) + evaluation.powers.filter {
                    it.source.id in power.source.parentSourceIds
                }.mapNotNull { it.source.sessionAlias }).distinct()
            DiscoveryCandidate(candidate.powerSourceId, label, candidate.reasons, candidate.rejectedReasons, aliases)
        }
        val pending = remainingSessions.filterNot { it in searched }
        return PostMatchDiscoveryResult(record.id, bundles, pairing,
            (candidates + previous?.candidates.orEmpty()).distinctBy { it.sourceId },
            (notes + "Search scope: ${searched.size} sessions attempted; ${pending.size} ranked sessions remain. Unsearched sessions are not ruled out."
                + "Inspected ${bundles.size} saved sessions; $rawBytes raw bytes; $documents decoded matches. No fusion or upload performed.").distinct(),
            pending, searched.distinct(), stops.distinct(), complete, rawBytes, decodedBytes, documents, powers.toList())
    }
}
