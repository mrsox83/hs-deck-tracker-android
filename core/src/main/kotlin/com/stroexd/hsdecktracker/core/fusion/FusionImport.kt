package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.data.JsonFileStore
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.coroutines.Dispatchers
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
    ): FusionImportEntry = update(matchId) { previous ->
        requireNotNull(previous) { "Unknown fusion import $matchId" }.copy(
            state = state,
            pairing = pairing ?: previous.pairing,
            artifactFile = artifactFile ?: previous.artifactFile,
            lastError = error,
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
    ): FusionImportResult = mutex.withLock {
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
            val powers = originals + acceptedContinuations(originals, tracker)
            val decisions = candidateControllers(powers).map { controller ->
                FusionPairing.decide(
                    tracker,
                    powers.map { power ->
                        val end = power.endedLogTime?.let { time ->
                            power.source.sessionAlias?.let { PowerClock.resolve(it, time, zoneId) }
                        }
                        PowerPairingFacts.fromReduced(power, end, controller)
                    },
                    trackerCardIds,
                )
            }
            val accepted = decisions.filter { it.status == PairingStatus.ACCEPTED }
                .distinctBy { it.acceptedSourceId }
            if (accepted.size != 1) {
                val pending = decisions.maxByOrNull { decision -> decision.candidates.maxOfOrNull { it.score } ?: 0 }
                    ?: PairingDecision(PairingStatus.PENDING, candidates = emptyList(), reason = "No controller evidence was available")
                val guarded = if (accepted.size > 1) pending.copy(
                    status = PairingStatus.CONFLICTED,
                    acceptedSourceId = null,
                    reason = "Multiple selected Power matches satisfy the conservative pairing gate",
                ) else pending
                return@withLock FusionImportResult(
                    repository.transition(record.id, FusionImportState.PENDING_PAIRING, pairing = guarded),
                    diagnostics,
                )
            }
            val pairing = accepted.single()
            val fused = FusionCoordinator.assemble(tracker, powers, pairing, fusionVersion = "android-r3/1")
            val artifact = repository.writeArtifact(fused)
            FusionImportResult(
                repository.transition(
                    record.id,
                    FusionImportState.FUSED_LOCAL,
                    pairing = pairing,
                    artifactFile = artifact,
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

    private fun acceptedContinuations(matches: List<ReducedPowerMatch>, tracker: TrackerEvidence): List<ReducedPowerMatch> {
        val stitched = mutableListOf<ReducedPowerMatch>()
        for (earlier in matches) for (later in matches) {
            if (earlier === later) continue
            val decision = PowerContinuationStitcher.evaluate(earlier, later, tracker, zoneId)
            if (decision.status == ContinuationStatus.ACCEPTED) {
                stitched += PowerContinuationStitcher.stitch(earlier, later, decision)
            }
        }
        return stitched.distinctBy { it.source.id }
    }

    private fun candidateControllers(matches: List<ReducedPowerMatch>): Set<Int> = matches
        .flatMap { match -> match.entities.values.mapNotNull { it.controller.value } }
        .filter { it > 0 }
        .toSet()
        .ifEmpty { setOf(1, 2) }
}
