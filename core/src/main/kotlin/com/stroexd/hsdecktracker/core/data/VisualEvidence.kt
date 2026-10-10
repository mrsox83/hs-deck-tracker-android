package com.stroexd.hsdecktracker.core.data

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

@Serializable
enum class VisualEvidenceStatus { TIMESTAMP_ONLY, KEYFRAME_PENDING, KEYFRAME_SAVED, KEYFRAME_FAILED }

@Serializable
data class VisualKeyframe(
    val relativePath: String,
    val sha256: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
)

@Serializable
data class VisualEvidenceEntry(
    val id: String,
    val observedAt: Long,
    val matchId: String? = null,
    val reason: String,
    /** Requested policy, not proof of which source the Android consent dialog ultimately granted. */
    val requestedCaptureMode: VisualCaptureMode,
    val status: VisualEvidenceStatus,
    val keyframe: VisualKeyframe? = null,
    val error: String? = null,
)

class VisualEvidenceRepository(dir: File) {
    private val store = JsonFileStore(
        File(dir, "visual-evidence.json"),
        ListSerializer(VisualEvidenceEntry.serializer()),
        emptyList(),
    )

    val entries: StateFlow<List<VisualEvidenceEntry>> = store.state

    suspend fun add(entry: VisualEvidenceEntry) {
        store.update { current ->
            require(current.none { it.id == entry.id }) { "Visual evidence id ${entry.id} already exists" }
            current + entry
        }
    }

    suspend fun attachKeyframe(id: String, keyframe: VisualKeyframe) {
        var found = false
        store.update { current -> current.map { entry ->
            if (entry.id != id) entry else entry.copy(
                status = VisualEvidenceStatus.KEYFRAME_SAVED,
                keyframe = keyframe,
                error = null,
            ).also { found = true }
        } }
        require(found) { "Unknown visual evidence id $id" }
    }

    suspend fun markFailed(id: String, error: String) {
        var found = false
        store.update { current -> current.map { entry ->
            if (entry.id != id) entry else entry.copy(status = VisualEvidenceStatus.KEYFRAME_FAILED, error = error)
                .also { found = true }
        } }
        require(found) { "Unknown visual evidence id $id" }
    }

    suspend fun clear() {
        store.set(emptyList())
    }
}
