package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.stats.MatchResult
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.time.ZoneId

class UserNoteProjectionTest {
    private fun tracker(id: String = "synthetic-game-a", note: String = "", revision: Int = 1): TrackerEvidence =
        EvidenceAdapters.tracker(AppJson.encodeToString(MatchRecord.serializer(),
            MatchRecord(id = id, timestamp = 1000, result = MatchResult.WIN, notes = note, revision = revision)).toByteArray())

    private fun assemble(tracker: TrackerEvidence): FusedMatch = FusionCoordinator.assemble(
        tracker, emptyList(), PairingDecision(PairingStatus.PENDING, candidates = emptyList(), reason = "synthetic"))

    @Test fun exactTextAndProvenanceRoundTrip() {
        val tracker = tracker(note = "  Journal: café 🐈\nsecond line\t ", revision = 3)
        val fused = assemble(tracker)
        val note = assertNotNull(fused.userNote)
        assertEquals(tracker.record.notes, note.text)
        assertEquals(UserNoteState.TEXT, note.state)
        assertEquals(3, note.trackerRevision)
        assertEquals(tracker.record.id, note.trackerMatchId)
        assertEquals(tracker.source.artifactSha256, note.trackerSha256)
        assertEquals(EvidenceRef(tracker.source.id, jsonPointer = "/notes", rule = "tracker-user-note/1"), note.evidence)
        assertEquals(fused, FusionArtifactCodec.decode(FusionArtifactCodec.encode(fused)))
    }

    @Test fun absentNullEmptyAndWhitespaceRemainDistinct() {
        val source = tracker()
        val document = AppJson.parseToJsonElement(AppJson.encodeToString(MatchRecord.serializer(), source.record)).jsonObject
        val inputs = listOf(null to UserNoteState.ABSENT, JsonNull to UserNoteState.NULL,
            JsonPrimitive("") to UserNoteState.TEXT, JsonPrimitive(" \n ") to UserNoteState.TEXT)
        for ((value, state) in inputs) {
            val fields = document.toMutableMap().apply { if (value == null) remove("notes") else put("notes", value) }
            val fused = assemble(EvidenceAdapters.tracker(JsonObject(fields).toString().toByteArray()))
            val note = assertNotNull(FusionArtifactCodec.decode(FusionArtifactCodec.encode(fused)).userNote)
            assertEquals(state, note.state)
            assertEquals((value as? JsonPrimitive)?.takeIf { it.isString }?.content, note.text)
        }
    }

    @Test fun wrongGameChangedSourceAndStaleRevisionCannotReuseSnapshot() {
        val source = tracker(note = "annotation", revision = 2)
        for (record in listOf(source.record.copy(id = "synthetic-game-b"), source.record.copy(revision = 1),
            source.record.copy(revision = 3), source.record.copy(notes = "changed"))) {
            assertFailsWith<IllegalArgumentException> { assemble(source.copy(record = record)) }
        }
        assertFailsWith<IllegalArgumentException> { assemble(source.copy(source = source.source.copy(artifactSha256 = "0".repeat(64)))) }
        assertFailsWith<IllegalArgumentException> { assemble(source.copy(source = source.source.copy(matchAlias = "synthetic-game-b"))) }
        assertFailsWith<IllegalArgumentException> { assemble(tracker(revision = 0)) }
        val fresh = assemble(tracker(note = "new revision", revision = 3))
        assertEquals("new revision", fresh.userNote?.text)
    }

    @Test fun latestExplicitClearAndUnverifiedManualEvidenceDoNotInventText() {
        val first = tracker(note = "earlier annotation", revision = 1)
        val cleared = tracker(note = "", revision = 2)
        assertEquals("", assemble(cleared).userNote?.text)
        assertNotEquals(first.source.id, cleared.source.id)
        // No raw note-state witness: do not infer that a default empty string was explicitly saved.
        assertNull(assemble(TrackerEvidence(first.source, first.record)).userNote)
    }

    @Test fun invalidRawNoteTypesAreRejectedInsteadOfCoercedToEmpty() {
        val document = AppJson.parseToJsonElement(AppJson.encodeToString(MatchRecord.serializer(), tracker().record)).jsonObject
        for (value in listOf(JsonPrimitive(42), JsonPrimitive(false), JsonObject(emptyMap()))) {
            val fields = document.toMutableMap().apply { put("notes", value) }
            assertFailsWith<IllegalArgumentException> { EvidenceAdapters.tracker(JsonObject(fields).toString().toByteArray()) }
        }
    }

    @Test fun serializedStructuralProvenanceMismatchesAreRejected() {
        val fused = assemble(tracker(note = "annotation"))
        val note = assertNotNull(fused.userNote)
        for (bad in listOf(note.copy(trackerSha256 = "0".repeat(64)), note.copy(trackerMatchId = "synthetic-game-b"),
            note.copy(trackerRevision = 0), note.copy(evidence = note.evidence.copy(jsonPointer = "/result")),
            note.copy(state = UserNoteState.ABSENT), note.copy(text = null))) {
            assertFailsWith<FusionArtifactException> { FusionArtifactCodec.encode(fused.copy(userNote = bad)) }
        }
    }

    @Test fun oldJsonAndAdjacentGamesDoNotAcquireOrMixNotes() {
        val a = assemble(tracker(note = "annotation A"))
        val b = assemble(tracker(id = "synthetic-game-b", note = ""))
        assertEquals("annotation A", a.userNote?.text)
        assertEquals("", b.userNote?.text)
        assertNotEquals(a.matchId, b.matchId)
        val oldFields = AppJson.parseToJsonElement(FusionArtifactCodec.encode(a)).jsonObject.toMutableMap().apply { remove("userNote") }
        val original = JsonObject(oldFields).toString()
        assertNull(FusionArtifactCodec.decode(original).userNote)
        assemble(tracker(note = "later edit", revision = 2))
        assertEquals(original, JsonObject(oldFields).toString())
        assertEquals("annotation A", a.userNote?.text)
    }

    @Test fun existingArtifactSurvivesLaterNoteEditImportAndReexport() = runTest {
        val dir = Files.createTempDirectory("synthetic-note-lifecycle").toFile()
        try {
            val original = tracker(note = "original note")
            val repository = FusionImportRepository(dir)
            val old = assemble(original).copy(userNote = null)
            val filename = repository.writeArtifact(old)
            repository.begin(original.record.id, emptyList())
            repository.transition(original.record.id, FusionImportState.FUSED_LOCAL, artifactFile = filename)
            val bytes = repository.artifactForExport(original.record.id).readBytes()
            val restarted = FusionImportRepository(dir)
            val result = FusionImporter(restarted, ZoneId.of("UTC")).import(
                original.record.copy(notes = "later saved note", revision = 2), emptyList())
            assertEquals(FusionImportState.FUSED_LOCAL, result.entry.state)
            assertContentEquals(bytes, restarted.artifactForExport(original.record.id).readBytes())
            assertNull(FusionArtifactCodec.decode(bytes.toString(Charsets.UTF_8)).userNote)
            assertFailsWith<FusionArtifactException> { restarted.writeArtifact(assemble(tracker(note = "later saved note", revision = 2))) }
            assertContentEquals(bytes, restarted.artifactForExport(original.record.id).readBytes())
        } finally { dir.deleteRecursively() }
    }
}
