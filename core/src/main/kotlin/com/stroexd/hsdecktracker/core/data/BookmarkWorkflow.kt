package com.stroexd.hsdecktracker.core.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface BookmarkStore {
    suspend fun add(entry: VisualEvidenceEntry)
    suspend fun attachKeyframe(id: String, keyframe: VisualKeyframe)
    suspend fun markFailed(id: String, error: String)
}

/** Persists before announcing success. Enqueue must atomically validate the originating capture session. */
class BookmarkWorkflow(
    private val store: BookmarkStore,
    private val scope: CoroutineScope,
    private val notify: suspend (BookmarkFeedback) -> Unit,
) {
    suspend fun submit(entry: VisualEvidenceEntry, enqueue: ((((Result<VisualKeyframe>) -> Unit)) -> Boolean)? = null) {
        try {
            store.add(entry)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            notify(BookmarkFeedback.SAVE_FAILED)
            return
        }
        notify(entry.bookmarkFeedback())
        if (enqueue != null) {
            val queued = enqueue { result -> scope.launch { finish(entry, result) } }
            if (!queued) finish(entry, Result.failure(IllegalStateException("Visual capture stopped before the bookmark frame arrived")))
        }
    }

    private suspend fun finish(entry: VisualEvidenceEntry, result: Result<VisualKeyframe>) {
        val outcome = result.fold(
            onSuccess = { entry.copy(status = VisualEvidenceStatus.KEYFRAME_SAVED, keyframe = it) },
            onFailure = { entry.copy(status = VisualEvidenceStatus.KEYFRAME_FAILED, error = it.message ?: "Keyframe failed") },
        )
        try {
            if (outcome.status == VisualEvidenceStatus.KEYFRAME_SAVED) store.attachKeyframe(entry.id, requireNotNull(outcome.keyframe))
            else store.markFailed(entry.id, requireNotNull(outcome.error))
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            // Timestamp is durable; no successful image/failed-status persistence is claimed.
            notify(BookmarkFeedback.IMAGE_FAILED)
            return
        }
        notify(outcome.bookmarkFeedback())
    }
}
