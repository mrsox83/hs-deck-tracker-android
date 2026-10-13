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
    val pairing: PairingDecision, val candidates: List<DiscoveryCandidate>, val notes: List<String>)

/** Explicit foreground search. Evidence is retained per session; preview never writes fusion/history. */
class PostMatchDiscovery(private val repository: PostMatchCollectionRepository, private val zone: ZoneId,
    private val elapsedMillis: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private class SearchLimit(message: String) : Exception(message)

    suspend fun search(record: MatchRecord, names: List<String>, cards: Set<String>,
        snapshot: suspend (String, Long) -> PostMatchLogSnapshot.Snapshot): PostMatchDiscoveryResult {
        val caller = currentCoroutineContext()
        val start = elapsedMillis()
        fun check() {
            caller.ensureActive()
            if (elapsedMillis() - start >= FusionDiscoveryPolicy.DEADLINE_MILLIS) throw SearchLimit("60-second search deadline reached")
        }
        val plan = FusionDiscoveryPolicy.sessions(names, record.timestamp, zone)
        val bundles = mutableListOf<DiscoveryBundle>()
        val powers = mutableListOf<ReducedPowerMatch>()
        val notes = mutableListOf<String>()
        var complete = names.distinct().size <= FusionDiscoveryPolicy.MAX_SESSIONS
        if (!complete) notes += "Only ${plan.size} of ${names.distinct().size} sessions inspected: session limit. Use manual selection if inconclusive."
        var rawBytes = 0L
        var decodedBytes = 0L
        var documents = 0
        for (session in plan) {
            var entry: LogCollectionEntry? = null
            try {
                check()
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
            } catch (error: CancellationException) {
                entry?.let { withContext(NonCancellable) { repository.fail(it.id, "Discovery cancelled; retained raw evidence preserved") } }
                throw error
            } catch (error: SearchLimit) {
                complete = false
                notes += error.message.orEmpty()
                entry?.let { repository.fail(it.id, error.message.orEmpty()) }
                break
            } catch (error: Exception) {
                complete = false
                notes += "${PostMatchPresentation.sessionLabel(session)}: ${error.message ?: "read/parse failed"}"
                entry?.let {
                    if (repository.entries.value.first { saved -> saved.id == it.id }.logs.isEmpty())
                        repository.recordReadFailure(it.id, session, error.message ?: "Discovery failed")
                    else repository.fail(it.id, error.message ?: "Discovery parsing failed; raw retained")
                }
            }
        }
        val tracker = EvidenceAdapters.tracker(AppJson.encodeToString(MatchRecord.serializer(), record).toByteArray())
        val evaluation = try {
            check()
            withContext(Dispatchers.Default) { FusionEvaluator.evaluate(tracker, powers, zone, cards, ::check) }
        } catch (error: SearchLimit) {
            complete = false; notes += error.message.orEmpty(); FusionEvaluation(powers, emptyList())
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
        return PostMatchDiscoveryResult(record.id, bundles, pairing, candidates,
            (notes + "Inspected ${bundles.size}/${plan.size} sessions; $rawBytes raw bytes; $documents decoded matches. No fusion or upload performed.").distinct())
    }
}
