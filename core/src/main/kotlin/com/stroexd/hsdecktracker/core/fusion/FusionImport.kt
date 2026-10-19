package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.data.JsonFileStore
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.stats.MatchResult
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.ZoneId

@Serializable
enum class FusionImportState { RECEIVED, PROCESSING, PENDING_PAIRING, FUSED_LOCAL, FAILED }

@Serializable
data class FusionImportEntry(
    val matchId: String,
    val bundleSha256: List<String>,
    val state: FusionImportState,
    val pairing: PairingDecision? = null,
    val artifactFile: String? = null,
    val attempts: Int = 0,
    val lastError: String? = null,
    val updatedAt: Long,
    val coverageWarnings: List<String> = emptyList(),
    /** Saved-match identity used when the immutable fused artifact was assembled. Null for legacy entries. */
    val fusedRecordRevision: Int? = null,
    val fusedRecordResult: MatchResult? = null,
)

class FusionImportRepository(
    private val dir: File,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val store = JsonFileStore(
        File(dir, "fusion-outbox.json"),
        ListSerializer(FusionImportEntry.serializer()),
        emptyList(),
    )

    val entries: StateFlow<List<FusionImportEntry>> = store.state

    /** Only the current match-owned published artifact; never accept an arbitrary journal path. */
    fun artifactForExport(matchId: String): File {
        val entry = store.value.firstOrNull { it.matchId == matchId && it.state == FusionImportState.FUSED_LOCAL }
            ?: error("This match has no fused artifact saved locally")
        val name = requireNotNull(entry.artifactFile) { "Fused artifact filename missing" }
        // Coordinator filenames use the derived logical ID, not the tracker UUID.
        require(name.matches(Regex("[0-9a-f]{32}\\.json")) && File(name).name == name) {
            "Invalid fused artifact filename"
        }
        val directory = File(dir, "fused-matches").canonicalFile
        return File(directory, name).canonicalFile.also {
            require(it.parentFile == directory && it.isFile && it.length() > 0) { "Fused artifact missing" }
        }
    }

    suspend fun recoverInterrupted(): Int {
        var recovered = 0
        store.update { entries -> entries.map { entry ->
            if (entry.state == FusionImportState.RECEIVED || entry.state == FusionImportState.PROCESSING) {
                recovered++
                entry.copy(
                    state = FusionImportState.FAILED,
                    lastError = "Import was interrupted; select the bundle again to retry",
                    updatedAt = clock(),
                )
            } else entry
        } }
        return recovered
    }

    suspend fun recordAccessFailure(matchId: String, message: String): FusionImportEntry = update(matchId) { previous ->
        if (previous?.state == FusionImportState.FUSED_LOCAL) previous else FusionImportEntry(
            matchId = matchId,
            bundleSha256 = previous?.bundleSha256.orEmpty(),
            state = FusionImportState.FAILED,
            pairing = previous?.pairing,
            attempts = (previous?.attempts ?: 0) + 1,
            lastError = message,
            updatedAt = clock(),
        )
    }

    suspend fun begin(matchId: String, hashes: List<String>): FusionImportEntry = update(matchId) { previous ->
        FusionImportEntry(
            matchId = matchId,
            bundleSha256 = hashes.sorted(),
            state = FusionImportState.RECEIVED,
            attempts = (previous?.attempts ?: 0) + 1,
            updatedAt = clock(),
        )
    }

    suspend fun transition(
        matchId: String,
        state: FusionImportState,
        pairing: PairingDecision? = null,
        artifactFile: String? = null,
        error: String? = null,
        coverageWarnings: List<String>? = null,
        fusedRecordRevision: Int? = null,
        fusedRecordResult: MatchResult? = null,
    ): FusionImportEntry = update(matchId) { previous ->
        requireNotNull(previous) { "Unknown fusion import $matchId" }.copy(
            state = state,
            pairing = pairing ?: previous.pairing,
            artifactFile = artifactFile ?: previous.artifactFile,
            lastError = error,
            coverageWarnings = coverageWarnings ?: previous.coverageWarnings,
            fusedRecordRevision = fusedRecordRevision ?: previous.fusedRecordRevision,
            fusedRecordResult = fusedRecordResult ?: previous.fusedRecordResult,
            updatedAt = clock(),
        )
    }

    suspend fun writeArtifact(match: FusedMatch): String = withContext(Dispatchers.IO) {
        val artifacts = File(dir, "fused-matches").apply { mkdirs() }
        val target = File(artifacts, "${match.matchId}.json")
        val temporary = File(artifacts, "${match.matchId}.json.tmp")
        try {
            temporary.outputStream().buffered().use { FusionArtifactCodec.encodeToStream(match, it) }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            temporary.delete()
        }
        target.name
    }

    private suspend fun update(matchId: String, transform: (FusionImportEntry?) -> FusionImportEntry): FusionImportEntry {
        lateinit var result: FusionImportEntry
        store.update { entries ->
            val existing = entries.firstOrNull { it.matchId == matchId }
            result = transform(existing)
            entries.filterNot { it.matchId == matchId } + result
        }
        return result
    }
}

data class FusionImportResult(val entry: FusionImportEntry, val diagnostics: List<String> = emptyList())

class FusionBundleAccessException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

object FusionBundleLoader {
    fun load(openers: List<() -> InputStream?>): List<ByteArray> {
        if (openers.isEmpty()) return emptyList()
        return openers.mapIndexed { index, open ->
            try {
                val input = open() ?: throw FusionBundleAccessException("Selected bundle ${index + 1} cannot be opened")
                input.use { it.readBytes() }
            } catch (error: FusionBundleAccessException) {
                throw error
            } catch (error: SecurityException) {
                throw FusionBundleAccessException("Permission was denied for selected bundle ${index + 1}", error)
            } catch (error: Exception) {
                throw FusionBundleAccessException("Selected bundle ${index + 1} could not be read", error)
            }
        }
    }
}

class FusionImporter(
    private val repository: FusionImportRepository,
    private val zoneId: ZoneId,
) {
    private val mutex = Mutex()

    suspend fun import(
        record: MatchRecord,
        bundles: List<ByteArray>,
        trackerCardIds: Set<String> = emptySet(),
        confirmedSourceId: String? = null,
    ): FusionImportResult = mutex.withLock {
        repository.entries.value.firstOrNull { it.matchId == record.id && it.state == FusionImportState.FUSED_LOCAL }
            ?.let { return@withLock FusionImportResult(it, listOf(PostMatchPresentation.EXISTING_ARTIFACT_PRESERVED)) }
        require(bundles.isNotEmpty()) { "Select at least one exporter bundle" }
        val hashes = bundles.map(EvidenceAdapters::sha256).distinct().sorted()
        repository.begin(record.id, hashes)
        repository.transition(record.id, FusionImportState.PROCESSING)
        try {
            val trackerBytes = AppJson.encodeToString(MatchRecord.serializer(), record).toByteArray()
            val tracker = EvidenceAdapters.tracker(trackerBytes)
            val parsed = bundles.map(EvidenceAdapters::exporterBundle)
            val diagnostics = parsed.flatMap { it.diagnostics }
            val originals = parsed.flatMap { bundle -> bundle.matches.map { PowerEvidenceReducer.reduce(it.first, it.second) } }
                .distinctBy { it.source.id }
            require(originals.isNotEmpty()) { "Selected files contain no exporter match evidence" }
            val caller = kotlinx.coroutines.currentCoroutineContext()
            val evaluation = FusionEvaluator.evaluate(tracker, originals, zoneId, trackerCardIds) { caller.ensureActive() }
            val powers = evaluation.powers
            val pairing = if (confirmedSourceId == null) evaluation.manualDecision() else evaluation.discoveryDecision(true)
            if (pairing.status != PairingStatus.ACCEPTED ||
                (confirmedSourceId != null && pairing.acceptedSourceId != confirmedSourceId)) {
                val guarded = if (pairing.status == PairingStatus.ACCEPTED) pairing.copy(
                    status = PairingStatus.PENDING, acceptedSourceId = null, reason = "Confirmed candidate changed; search again") else pairing
                return@withLock FusionImportResult(
                    repository.transition(record.id, FusionImportState.PENDING_PAIRING, pairing = guarded),
                    diagnostics,
                )
            }
            val fused = FusionCoordinator.assemble(tracker, powers, pairing, fusionVersion = "android-r3/3")
            val artifact = repository.writeArtifact(fused)
            FusionImportResult(
                repository.transition(
                    record.id,
                    FusionImportState.FUSED_LOCAL,
                    pairing = pairing,
                    artifactFile = artifact,
                    coverageWarnings = fused.diagnostics.filter { it.startsWith("Partial reconstruction:") } +
                        if (fused.diagnostics.any { it.startsWith("Turn owner unresolved") })
                            listOf("Partial reconstruction: turn ownership or per-player indices remain unknown; see artifact diagnostics.")
                        else emptyList(),
                    fusedRecordRevision = record.revision,
                    fusedRecordResult = record.result,
                ),
                diagnostics,
            )
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            FusionImportResult(
                repository.transition(
                    record.id,
                    FusionImportState.FAILED,
                    error = error.message ?: error::class.simpleName ?: "Unknown fusion failure",
                ),
            )
        }
    }

}
