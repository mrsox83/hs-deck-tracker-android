package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.data.JsonFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Serializable enum class LogCollectionState { COLLECTING, RAW_RETAINED, BUNDLE_READY, INTERRUPTED, FAILED }
@Serializable data class RetainedPowerLog(val session: String, val sha256: String, val bytes: Long)
@Serializable data class LogCollectionEntry(
    val id: String,
    val matchId: String,
    val state: LogCollectionState,
    val logs: List<RetainedPowerLog> = emptyList(),
    val readFailures: Map<String, String> = emptyMap(),
    val error: String? = null,
)

/** App-private raw evidence and journal, independent of tracker persistence and capture lifecycle.
 * Raw retention never implies pairing, successful fusion, or remote upload. No original deletion.
 */
class PostMatchCollectionRepository(private val dataDir: File) {
    private val dir = File(dataDir, "post-match-logs")
    private val mutex = Mutex()
    private val store = JsonFileStore(File(dir, "journal.json"), ListSerializer(LogCollectionEntry.serializer()), emptyList())
    val entries: StateFlow<List<LogCollectionEntry>> = store.state

    suspend fun recoverInterrupted() = store.update { list -> list.map {
        if (it.state == LogCollectionState.COLLECTING) it.copy(state = LogCollectionState.INTERRUPTED,
            error = "Collection interrupted; retained raw evidence is available for retry") else it
    } }

    suspend fun begin(matchId: String): LogCollectionEntry {
        val entry = LogCollectionEntry(UUID.randomUUID().toString(), matchId, LogCollectionState.COLLECTING)
        store.update { it + entry }
        return entry
    }

    suspend fun retain(id: String, snapshot: PostMatchLogSnapshot.Snapshot): LogCollectionEntry = mutex.withLock {
        val log = RetainedPowerLog(snapshot.session, snapshot.sourceSha256, snapshot.sourceBytes)
        // Validate journal ownership before a write. If death occurs after raw publish but before
        // journal update, keep the orphan raw file; never guess a match for it or delete it on recovery.
        require(store.value.any { it.id == id }) { "Unknown collection" }
        withContext(Dispatchers.IO) {
            dir.mkdirs()
            val target = rawFile(log)
            if (!target.exists()) {
                val staging = Files.createTempFile(dir.toPath(), "raw-", ".part").toFile()
                try {
                    FileOutputStream(staging).use { out -> out.write(snapshot.copyBytes()); out.fd.sync() }
                    Files.move(staging.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
                } finally { staging.delete() }
            }
            verify(log)
        }
        update(id) { it.copy(logs = (it.logs + log).distinct(), state = LogCollectionState.RAW_RETAINED, error = null) }
    }

    suspend fun fail(id: String, message: String) = update(id) { it.copy(state = LogCollectionState.FAILED, error = message) }

    suspend fun recordReadFailure(id: String, session: String, message: String): LogCollectionEntry {
        PostMatchLogSnapshot.requireSession(session)
        return update(id) { it.copy(readFailures = it.readFailures + (session to message)) }
    }

    suspend fun buildBundle(id: String, cancelled: () -> Boolean = { false }): File = mutex.withLock {
        val entry = store.value.firstOrNull { it.id == id } ?: error("Unknown collection")
        require(runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)) { "Invalid collection identity" }
        require(entry.logs.isNotEmpty()) { "No raw logs retained" }
        val file = withContext(Dispatchers.IO) {
            val logs = entry.logs.map { it to verify(it) }
            val target = File(dir, "$id.zip")
            // Regeneration is deterministic and must never accept a stale/tampered published ZIP.
            // Build to a unique staging file, then replace only this journal-owned derived bundle.
            val temporary = Files.createTempFile(dir.toPath(), "bundle-", ".part").toFile()
            try {
                PostMatchBundleBuilder.write(logs, temporary, cancelled, entry.readFailures)
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                target
            } finally { temporary.delete() }
        }
        update(id) { it.copy(state = LogCollectionState.BUNDLE_READY, error = null) }
        file
    }

    private suspend fun update(id: String, transform: (LogCollectionEntry) -> LogCollectionEntry): LogCollectionEntry {
        var changed: LogCollectionEntry? = null
        store.update { list -> list.map { if (it.id == id) transform(it).also { changed = it } else it } }
        return checkNotNull(changed) { "Unknown collection" }
    }

    private fun rawFile(log: RetainedPowerLog): File {
        require(log.sha256.matches(Regex("[0-9a-f]{64}"))) { "Invalid raw hash" }
        PostMatchLogSnapshot.requireSession(log.session)
        require(log.bytes in 1..PostMatchLogSnapshot.MAX_BYTES) { "Invalid raw size" }
        return File(dir, "${log.sha256}.Power.log")
    }

    private fun verify(log: RetainedPowerLog): File = rawFile(log).also { raw ->
        require(raw.isFile && raw.length() == log.bytes) { "Retained raw log missing or size changed" }
        require(EvidenceAdapters.sha256(raw.readBytes()) == log.sha256) { "Retained raw log hash changed" }
    }
}

/** Produces the existing companion contract; no source edits or new gameplay semantics. */
object PostMatchBundleBuilder {
    const val MAX_SESSIONS = 2 // Manual pilot: one normal session plus one reconnect fragment.
    private const val MAX_DOCUMENT_BYTES = 16 * 1024 * 1024

    fun write(logs: List<Pair<RetainedPowerLog, File>>, destination: File, cancelled: () -> Boolean = { false }, readFailures: Map<String, String> = emptyMap()) {
        require(logs.size in 1..MAX_SESSIONS) { "Select one or two sessions for the manual pilot" }
        require(logs.size + readFailures.size <= MAX_SESSIONS) { "Too many selected sessions" }
        require(logs.map { it.first.session }.distinct().size == logs.size) { "Duplicate session selection" }
        val manifest = mutableListOf<Map<String, Any?>>()
        ZipOutputStream(destination.outputStream().buffered()).use { zip ->
            fun put(name: String, bytes: ByteArray) {
                checkCancellation(cancelled)
                val entry = ZipEntry(name).apply { time = 0 } // Stable bundle hash for identical retry.
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
            for ((log, file) in logs.sortedBy { it.first.session }) {
                checkCancellation(cancelled)
                PostMatchLogSnapshot.requireSession(log.session)
                require(log.bytes in 1..PostMatchLogSnapshot.MAX_BYTES && file.length() == log.bytes) { "Invalid source size" }
                val raw = file.readBytes()
                require(EvidenceAdapters.sha256(raw) == log.sha256) { "Raw source hash mismatch" }
                put("${log.session}/Power.log", raw)
                val item = linkedMapOf<String, Any?>("session" to log.session, "source_bytes" to log.bytes,
                    "source_sha256" to log.sha256, "snapshot_note" to "Fixed opened-descriptor prefix; later growth excluded")
                var documents = emptyList<ByteArray>()
                try {
                    val parsed = PowerParser.parse(file)
                    checkCancellation(cancelled)
                    // Serialize/validate all documents before publishing any. Parse/size failure
                    // yields raw-only evidence, never a partially decoded match list.
                    documents = parsed.matches.map { match ->
                        PowerParser.json(linkedMapOf("schema" to "hs-power-evidence/0.3", "source_session" to log.session,
                            "source_sha256" to log.sha256, "selected_stream" to parsed.stream,
                            "limitations" to parsed.warnings, "match" to match)).toByteArray(Charsets.UTF_8).also {
                            require(it.size <= MAX_DOCUMENT_BYTES) { "Decoded match document exceeds manual-pilot limit" }
                        }
                    }
                    require(documents.sumOf { it.size.toLong() } <= 32L * 1024 * 1024) { "Decoded session exceeds manual-pilot limit" }
                    // Source files are app-private and immutable by protocol. Recheck after parser
                    // reads so a changed retained file is not paired with earlier raw ZIP bytes.
                    require(file.length() == log.bytes && EvidenceAdapters.sha256(file.readBytes()) == log.sha256) { "Raw source changed during parsing" }
                    item["status"] = if (documents.isEmpty()) "RAW_ONLY_NO_MATCHES" else "DECODED"
                    item["matches"] = documents.size
                    item["source_truncated"] = parsed.sourceTruncated
                    item["warnings"] = parsed.warnings
                } catch (error: java.util.concurrent.CancellationException) { throw error
                } catch (error: Exception) {
                    documents = emptyList()
                    item["status"] = "RAW_ONLY_PARSE_FAILED"
                    item["error"] = error.message ?: error.javaClass.simpleName
                }
                // Destination IO failures must abort the bundle, not masquerade as parser warnings.
                documents.forEachIndexed { index, bytes -> put("${log.session}/match-${(index + 1).toString().padStart(3, '0')}.json", bytes) }
                manifest += item
            }
            for ((session, message) in readFailures.toSortedMap()) {
                PostMatchLogSnapshot.requireSession(session)
                require(logs.none { it.first.session == session }) { "Session cannot be readable and failed simultaneously" }
                manifest += linkedMapOf("session" to session, "status" to "READ_FAILED", "error" to message)
            }
            put("manifest.json", PowerParser.json(linkedMapOf("schema" to "hs-export-bundle/0.4", "sessions" to manifest)).toByteArray(Charsets.UTF_8))
        }
        checkCancellation(cancelled)
    }

    private fun checkCancellation(cancelled: () -> Boolean) {
        if (Thread.currentThread().isInterrupted || cancelled()) throw java.util.concurrent.CancellationException("Bundle build cancelled")
    }
}
