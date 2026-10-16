package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.stats.*
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MatchJsonExportTest {
    @Test fun fullRecordRoundTrips() {
        val record = MatchRecord(timestamp = 123, result = MatchResult.WIN,
            notes = "Unicode: español 日本語", opponentCards = listOf(42),
            timeline = TimelineType.entries.mapIndexed { index, type ->
                TimelineEvent(index + 1, type, 42, "TOKEN_01")
            })
        assertEquals(record, AppJson.decodeFromString(MatchRecord.serializer(), MatchJsonExport.encode(record)))
    }

    @Test fun filenamesAreStableSafeAndDistinct() {
        val record = MatchRecord(id = "../unsafe/id", timestamp = 0, result = MatchResult.DRAW)
        val name = MatchJsonExport.filename(record)
        assertEquals(name, MatchJsonExport.filename(record))
        assertTrue(name.matches(Regex("match_19700101T000000000Z_[a-f0-9]{64}\\.json")))
        assertNotEquals(name, MatchJsonExport.filename(record.copy(id = "other")))
    }

    @Test fun correctedResultHasProvenanceAndARevisionedFilename() {
        val original = MatchRecord(id = "edited-match", timestamp = 0, result = MatchResult.WIN)
        val corrected = original.copy(
            result = MatchResult.LOSS,
            revision = 2,
            editedAt = 456,
            resultCorrections = listOf(ResultCorrection(MatchResult.WIN, MatchResult.LOSS, 456)),
        )

        assertNotEquals(MatchJsonExport.filename(original), MatchJsonExport.filename(corrected))
        assertTrue(MatchJsonExport.filename(corrected).matches(
            Regex("match_19700101T000000000Z_[a-f0-9]{64}_r2_[a-f0-9]{16}\\.json"),
        ))
        val decoded = AppJson.decodeFromString(MatchRecord.serializer(), MatchJsonExport.encode(corrected))
        assertEquals(corrected, decoded)
        assertEquals("USER_EDIT", decoded.resultCorrections.single().method)
    }
}
