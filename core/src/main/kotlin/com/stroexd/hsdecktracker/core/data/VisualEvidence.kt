package com.stroexd.hsdecktracker.core.data

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

@Serializable
enum class VisualEvidenceStatus { TIMESTAMP_ONLY, KEYFRAME_PENDING, KEYFRAME_SAVED, KEYFRAME_FAILED }

/** UI outcome categories only; no serialized evidence/schema change or raw exception text displayed. */
enum class BookmarkFeedback { TIMESTAMP_SAVED, IMAGE_PENDING, IMAGE_ATTACHED, IMAGE_FAILED, STORE_FULL, SAVE_FAILED }

fun VisualEvidenceEntry.bookmarkFeedback(): BookmarkFeedback = when (status) {
    VisualEvidenceStatus.TIMESTAMP_ONLY -> BookmarkFeedback.TIMESTAMP_SAVED
    VisualEvidenceStatus.KEYFRAME_PENDING -> BookmarkFeedback.IMAGE_PENDING
    VisualEvidenceStatus.KEYFRAME_SAVED -> BookmarkFeedback.IMAGE_ATTACHED
    VisualEvidenceStatus.KEYFRAME_FAILED -> if (error == "Visual evidence budget reached" || error == "Visual evidence byte budget reached") {
        BookmarkFeedback.STORE_FULL
    } else BookmarkFeedback.IMAGE_FAILED
}

@Serializable
data class VisualKeyframe(
    val relativePath: String,
    val sha256: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val regionPilotVersion: String? = null,
    val regionOcrReused: Boolean = false,
    val regionProbes: List<com.stroexd.hsdecktracker.core.vision.RegionProbe> = emptyList(),
    val fieldCandidates: List<com.stroexd.hsdecktracker.core.vision.DiagnosticFieldCandidate> = emptyList(),
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
    /** Backend that delivered pixels, not proof of the Android-selected app/display scope. */
    val captureMethod: String? = null,
)

class VisualEvidenceRepository(dir: File) : BookmarkStore {
    private val store = JsonFileStore(
        File(dir, "visual-evidence.json"),
        ListSerializer(VisualEvidenceEntry.serializer()),
        emptyList(),
    )

    val entries: StateFlow<List<VisualEvidenceEntry>> = store.state

    override suspend fun add(entry: VisualEvidenceEntry) {
        store.update { current ->
            require(current.none { it.id == entry.id }) { "Visual evidence id ${entry.id} already exists" }
            current + entry
        }
    }

    override suspend fun attachKeyframe(id: String, keyframe: VisualKeyframe) {
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

    override suspend fun markFailed(id: String, error: String) {
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
