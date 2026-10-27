package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.data.*
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class BookmarkFeedbackTest {
    private fun entry(id: String = "bookmark-1", status: VisualEvidenceStatus = VisualEvidenceStatus.KEYFRAME_PENDING, error: String? = null) =
        VisualEvidenceEntry(id, 123L, reason = "manual_bookmark", requestedCaptureMode = VisualCaptureMode.AUTOMATIC_ROUTE, status = status, error = error)

    @Test fun `timestamp pending attached and failed are distinct messages`() {
        val expected = listOf(BookmarkFeedback.TIMESTAMP_SAVED, BookmarkFeedback.IMAGE_PENDING, BookmarkFeedback.IMAGE_ATTACHED, BookmarkFeedback.IMAGE_FAILED)
        assertEquals(expected, VisualEvidenceStatus.entries.map { entry(status = it).bookmarkFeedback() })
    }
    @Test fun `count and byte budget errors surface store full without raw exception text`() {
        for (error in listOf("Visual evidence budget reached", "Visual evidence byte budget reached")) {
            assertEquals(BookmarkFeedback.STORE_FULL, entry(status = VisualEvidenceStatus.KEYFRAME_FAILED, error = error).bookmarkFeedback())
        }
        assertEquals(BookmarkFeedback.IMAGE_FAILED, entry(status = VisualEvidenceStatus.KEYFRAME_FAILED, error = "Synthetic storage error").bookmarkFeedback())
    }
    @Test fun `pending to attached feedback follows durable attachment and survives reopen`() = runBlocking {
        val dir = createTempDirectory("bookmark-feedback").toFile()
        val repo = VisualEvidenceRepository(dir)
        repo.add(entry())
        assertEquals(BookmarkFeedback.IMAGE_PENDING, repo.entries.value.single().bookmarkFeedback())
        repo.attachKeyframe("bookmark-1", VisualKeyframe("keyframes/synthetic.jpg", "abc", 10, 10, 4))
        assertEquals(BookmarkFeedback.IMAGE_ATTACHED, VisualEvidenceRepository(dir).entries.value.single().bookmarkFeedback())
    }
    @Test fun `full store preserves timestamp and prior evidence with durable failed outcome`() = runBlocking {
        val dir = createTempDirectory("bookmark-feedback").toFile()
        val repo = VisualEvidenceRepository(dir)
        val old = entry("prior", VisualEvidenceStatus.KEYFRAME_SAVED).copy(keyframe = VisualKeyframe("keyframes/prior.jpg", "old", 10, 10, 4))
        repo.add(old)
        repo.add(entry())
        repo.markFailed("bookmark-1", "Visual evidence budget reached")
        val restored = VisualEvidenceRepository(dir).entries.value
        assertEquals(old, restored.first())
        assertEquals(123L, restored.last().observedAt)
        assertNull(restored.last().keyframe)
        assertEquals(BookmarkFeedback.STORE_FULL, restored.last().bookmarkFeedback())
    }
    @Test fun `stopped capture and queue rejection remain image failures not full store`() {
        for (reason in listOf("Visual capture stopped before the bookmark frame arrived", "Too many pending bookmark frames")) {
            assertEquals(BookmarkFeedback.IMAGE_FAILED, entry(status = VisualEvidenceStatus.KEYFRAME_FAILED, error = reason).bookmarkFeedback())
        }
    }
    @Test fun `unrelated failed entry does not relabel pending timestamp or successful image`() {
        for (status in listOf(VisualEvidenceStatus.TIMESTAMP_ONLY, VisualEvidenceStatus.KEYFRAME_PENDING, VisualEvidenceStatus.KEYFRAME_SAVED)) {
            assertNotEquals(BookmarkFeedback.STORE_FULL, entry(status = status, error = "Visual evidence budget reached").bookmarkFeedback())
        }
    }
}
