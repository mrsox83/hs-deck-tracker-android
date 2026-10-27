package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.data.*
import com.stroexd.hsdecktracker.core.vision.RecognitionSessionGate
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkWorkflowTest {
    private val entry = VisualEvidenceEntry("synthetic-bookmark", 123L, reason = "manual_bookmark",
        requestedCaptureMode = VisualCaptureMode.AUTOMATIC_ROUTE, status = VisualEvidenceStatus.KEYFRAME_PENDING)
    private val image = VisualKeyframe("keyframes/synthetic.jpg", "abc", 100, 50, 12)
    private class Store : BookmarkStore {
        val entries = mutableMapOf<String, VisualEvidenceEntry>()
        var beforeAdd: suspend () -> Unit = {}
        var beforeAttach: suspend () -> Unit = {}
        override suspend fun add(entry: VisualEvidenceEntry) { beforeAdd(); entries[entry.id] = entry }
        override suspend fun attachKeyframe(id: String, keyframe: VisualKeyframe) {
            beforeAttach(); entries[id] = entries.getValue(id).copy(status = VisualEvidenceStatus.KEYFRAME_SAVED, keyframe = keyframe)
        }
        override suspend fun markFailed(id: String, error: String) {
            entries[id] = entries.getValue(id).copy(status = VisualEvidenceStatus.KEYFRAME_FAILED, error = error)
        }
    }
    @Test fun `timestamp write failure announces failure without pending or queuing`() = runTest {
        val store = Store().apply { beforeAdd = { error("Synthetic write failure") } }
        val messages = mutableListOf<BookmarkFeedback>()
        var queued = false
        BookmarkWorkflow(store, this, messages::add).submit(entry) { queued = true; true }
        assertEquals(listOf(BookmarkFeedback.SAVE_FAILED), messages)
        assertFalse(queued)
        assertTrue(store.entries.isEmpty())
    }
    @Test fun `pending follows timestamp write and attachment follows successful image write`() = runTest {
        val store = Store()
        val releaseTimestamp = CompletableDeferred<Unit>()
        val releaseImage = CompletableDeferred<Unit>()
        store.beforeAdd = { releaseTimestamp.await() }
        store.beforeAttach = { releaseImage.await() }
        val messages = mutableListOf<BookmarkFeedback>()
        var callback: ((Result<VisualKeyframe>) -> Unit)? = null
        launch { BookmarkWorkflow(store, this@runTest, messages::add).submit(entry) { callback = it; true } }
        runCurrent(); assertTrue(messages.isEmpty()); assertNull(callback)
        releaseTimestamp.complete(Unit); runCurrent()
        assertEquals(listOf(BookmarkFeedback.IMAGE_PENDING), messages)
        callback!!(Result.success(image)); runCurrent()
        assertEquals(listOf(BookmarkFeedback.IMAGE_PENDING), messages)
        releaseImage.complete(Unit); runCurrent()
        assertEquals(listOf(BookmarkFeedback.IMAGE_PENDING, BookmarkFeedback.IMAGE_ATTACHED), messages)
        assertEquals(image, store.entries.getValue(entry.id).keyframe)
    }
    @Test fun `full store failure keeps timestamp and emits terminal capacity message`() = runTest {
        val store = Store(); val messages = mutableListOf<BookmarkFeedback>()
        BookmarkWorkflow(store, this, messages::add).submit(entry) { callback ->
            callback(Result.failure(IllegalStateException("Visual evidence budget reached"))); true
        }
        runCurrent()
        assertEquals(listOf(BookmarkFeedback.IMAGE_PENDING, BookmarkFeedback.STORE_FULL), messages)
        assertEquals(123L, store.entries.getValue(entry.id).observedAt)
        assertNull(store.entries.getValue(entry.id).keyframe)
    }
    @Test fun `stop during timestamp persistence cannot enqueue after pending drain`() = runTest {
        val gate = RecognitionSessionGate(); val session = gate.start()
        val release = CompletableDeferred<Unit>(); val store = Store().apply { beforeAdd = { release.await() } }
        val messages = mutableListOf<BookmarkFeedback>(); var queued = false
        launch { BookmarkWorkflow(store, this@runTest, messages::add).submit(entry) {
            if (!gate.isCurrent(session)) false else { queued = true; true }
        } }
        runCurrent(); gate.stop(session); release.complete(Unit); runCurrent()
        assertFalse(queued)
        assertEquals(listOf(BookmarkFeedback.IMAGE_PENDING, BookmarkFeedback.IMAGE_FAILED), messages)
        assertEquals(VisualEvidenceStatus.KEYFRAME_FAILED, store.entries.getValue(entry.id).status)
    }
    @Test fun `replacement session cannot supply image for a delayed old bookmark`() = runTest {
        val gate = RecognitionSessionGate(); val old = gate.start()
        val release = CompletableDeferred<Unit>(); val store = Store().apply { beforeAdd = { release.await() } }
        val messages = mutableListOf<BookmarkFeedback>(); var queues = 0
        launch { BookmarkWorkflow(store, this@runTest, messages::add).submit(entry) {
            if (!gate.isCurrent(old)) false else { queues++; true }
        } }
        runCurrent(); val replacement = gate.start(); release.complete(Unit); runCurrent()
        assertTrue(gate.isCurrent(replacement)); assertEquals(0, queues)
        assertEquals(BookmarkFeedback.IMAGE_FAILED, messages.last())
    }
    @Test fun `attachment persistence failure never announces attached`() = runTest {
        val store = Store().apply { beforeAttach = { error("Synthetic attachment failure") } }
        val messages = mutableListOf<BookmarkFeedback>()
        BookmarkWorkflow(store, this, messages::add).submit(entry) { it(Result.success(image)); true }
        runCurrent()
        assertEquals(listOf(BookmarkFeedback.IMAGE_PENDING, BookmarkFeedback.IMAGE_FAILED), messages)
        assertNull(store.entries.getValue(entry.id).keyframe)
    }
    @Test fun `inactive capture saves timestamp without requesting an image`() = runTest {
        val store = Store(); val messages = mutableListOf<BookmarkFeedback>()
        BookmarkWorkflow(store, this, messages::add).submit(entry.copy(status = VisualEvidenceStatus.TIMESTAMP_ONLY))
        assertEquals(listOf(BookmarkFeedback.TIMESTAMP_SAVED), messages)
    }
    @Test fun `timestamp persistence cancellation propagates instead of announcing failure`() = runTest {
        val store = Store().apply { beforeAdd = { throw CancellationException("Synthetic cancellation") } }
        val messages = mutableListOf<BookmarkFeedback>()
        assertFailsWith<CancellationException> { BookmarkWorkflow(store, this, messages::add).submit(entry) }
        assertTrue(messages.isEmpty())
    }
}
