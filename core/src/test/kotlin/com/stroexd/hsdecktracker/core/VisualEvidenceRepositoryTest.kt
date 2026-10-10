package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.data.VisualCaptureMode
import com.stroexd.hsdecktracker.core.data.VisualEvidenceEntry
import com.stroexd.hsdecktracker.core.data.VisualEvidenceRepository
import com.stroexd.hsdecktracker.core.data.VisualEvidenceStatus
import com.stroexd.hsdecktracker.core.data.VisualKeyframe
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class VisualEvidenceRepositoryTest {
    @Test
    fun `timestamp bookmark is durable and can receive one keyframe`() = runBlocking {
        val dir = createTempDirectory("visual-evidence").toFile()
        val repository = VisualEvidenceRepository(dir)
        repository.add(
            VisualEvidenceEntry(
                id = "bookmark-1",
                observedAt = 123L,
                matchId = "match-1",
                reason = "manual_bookmark",
                requestedCaptureMode = VisualCaptureMode.PRIVACY_FIRST,
                status = VisualEvidenceStatus.KEYFRAME_PENDING,
            ),
        )
        repository.attachKeyframe("bookmark-1", VisualKeyframe("keyframes/a.jpg", "abc", 100, 50, 12))

        val restored = VisualEvidenceRepository(dir).entries.value.single()
        assertEquals(VisualEvidenceStatus.KEYFRAME_SAVED, restored.status)
        assertEquals("abc", restored.keyframe?.sha256)
        assertEquals("match-1", restored.matchId)
    }
}
