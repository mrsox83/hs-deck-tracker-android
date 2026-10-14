package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.stats.MatchResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.ZoneId
import kotlin.test.*

class PostMatchDiscoveryTest {
    @get:Rule val temporary = TemporaryFolder()
    private val session = "Hearthstone_2026_10_13_12_00_00"
    private val record = MatchRecord(id = "discovery-game", timestamp = Instant.parse("2026-10-13T17:00:10Z").toEpochMilli(),
        result = MatchResult.WIN, wentFirst = true)
    private val zone = ZoneId.of("America/Chicago")
    private val raw = """
        D 12:00:00.0000000 GameState.DebugPrintPower() - CREATE_GAME
        D 12:00:00.0010000 GameState.DebugPrintPower() - Player EntityID=2 PlayerID=1 GameAccountId=[hi=1 lo=2]
        D 12:00:00.0020000 GameState.DebugPrintPower() - TAG_CHANGE Entity=[entityName=Player id=2 zone=PLAY] tag=CONTROLLER value=1
        D 12:00:00.0030000 GameState.DebugPrintPower() - TAG_CHANGE Entity=[entityName=Player id=2 zone=PLAY] tag=FIRST_PLAYER value=1
        D 12:00:01.0000000 GameState.DebugPrintPower() - TAG_CHANGE Entity=[entityName=Player id=2 zone=PLAY] tag=PLAYSTATE value=WON
        D 12:00:02.0000000 GameState.DebugPrintPower() - TAG_CHANGE Entity=GameEntity tag=STEP value=FINAL_GAMEOVER
    """.trimIndent()
    private fun snapshot(name: String, text: String = raw): PostMatchLogSnapshot.Snapshot {
        val bytes = text.toByteArray()
        return PostMatchLogSnapshot.read(name, bytes.size.toLong(), { ByteArrayInputStream(bytes) })
    }

    @Test fun previewRetainsRawButDoesNotCreateFusionOrHistory() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val result = PostMatchDiscovery(repo, zone).search(record, listOf(session), emptySet()) { name, remaining ->
            assertTrue(remaining <= PostMatchLogSnapshot.MAX_BYTES); snapshot(name)
        }
        assertEquals(PairingStatus.ACCEPTED, result.pairing.status, result.pairing.toString())
        assertEquals(1, result.bundles.size)
        assertEquals(1, result.candidates.size)
        assertEquals(listOf(session), result.candidates.single().sessions)
        assertTrue(FusionImportRepository(temporary.root).entries.value.isEmpty())
    }

    @Test fun multiGameSessionSelectsSupportedGameNotLatestGame() = runBlocking {
        val result = PostMatchDiscovery(PostMatchCollectionRepository(temporary.root), zone)
            .search(record, listOf(session), emptySet()) { name, _ -> snapshot(name, raw + "\n" + raw.replace("12:00:", "12:10:")) }
        assertEquals(PairingStatus.ACCEPTED, result.pairing.status, result.pairing.toString())
        assertTrue(result.pairing.acceptedSourceId!!.endsWith(":1"))
        assertEquals(2, result.candidates.size)
    }

    @Test fun crossSessionDuplicatesDeduplicateButPlausibleDistinctSourcesConflict() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val other = "Hearthstone_2026_10_13_11_59_00"
        val duplicate = PostMatchDiscovery(repo, zone).search(record, listOf(session, other), emptySet()) { name, _ -> snapshot(name) }
        assertEquals(PairingStatus.ACCEPTED, duplicate.pairing.status)
        assertEquals(1, duplicate.candidates.size)
        val ambiguous = PostMatchDiscovery(repo, zone).search(record, listOf(session, other), emptySet()) { name, _ ->
            snapshot(name, if (name == other) raw + "\nignored additional line" else raw)
        }
        assertEquals(PairingStatus.CONFLICTED, ambiguous.pairing.status)
        assertNull(ambiguous.pairing.acceptedSourceId)
    }

    @Test fun unreadableOtherSessionPreventsUniqueRecommendation() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val other = "Hearthstone_2026_10_13_11_00_00"
        val result = PostMatchDiscovery(repo, zone).search(record, listOf(session, other), emptySet()) { name, _ ->
            if (name == other) error("Reader lost") else snapshot(name)
        }
        assertEquals(PairingStatus.PENDING, result.pairing.status)
        assertNull(result.pairing.acceptedSourceId)
        assertTrue(result.statusText().contains("Reader lost"))
        assertTrue(repo.entries.value.any { it.readFailures.isNotEmpty() })
    }

    @Test fun savedReviewRecoversUnreadableSiblingWithoutReadsOrNewCollections() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val engine = PostMatchDiscovery(repo, zone)
        var reads = 0
        val result = engine.search(record, listOf(session, "Hearthstone_2026_10_13_11_00_00"), emptySet()) { name, _ ->
            reads++
            if (name != session) error("Reader returned no Power.log descriptor") else snapshot(name)
        }
        assertFalse(result.complete)
        assertEquals(PairingStatus.PENDING, result.pairing.status)
        val count = repo.entries.value.size
        val reviewed = engine.reviewSavedCandidate(record, result, result.candidates.single().sourceId, emptySet())
        assertEquals(PairingStatus.ACCEPTED, reviewed.pairing.status)
        assertTrue(reviewed.complete)
        assertEquals(2, reads)
        assertEquals(count, repo.entries.value.size)
        assertTrue(reviewed.statusText().contains("Earlier broad-search warning"))
        assertTrue(reviewed.statusText().contains("excluded, not ruled out"))
        assertEquals(PairingStatus.PENDING, result.pairing.status)
        assertTrue(FusionImportRepository(temporary.root).entries.value.isEmpty())
        assertFailsWith<IllegalArgumentException> {
            engine.reviewSavedCandidate(record.copy(id = "other"), result, result.candidates.single().sourceId, emptySet())
        }
        result.bundles.single().file.appendText("changed")
        assertFailsWith<IllegalArgumentException> {
            engine.reviewSavedCandidate(record, result, result.candidates.single().sourceId, emptySet())
        }
        Unit
    }

    @Test fun savedReviewDoesNotCherryPickOneGameFromAmbiguousSession() = runBlocking {
        val engine = PostMatchDiscovery(PostMatchCollectionRepository(temporary.root), zone)
        val result = engine.search(record, listOf(session), emptySet()) { name, _ -> snapshot(name, raw + "\n" + raw) }
        val reviewed = engine.reviewSavedCandidate(record, result, result.candidates.first().sourceId, emptySet())
        assertEquals(PairingStatus.CONFLICTED, reviewed.pairing.status)
        assertNull(reviewed.pairing.acceptedSourceId)
        assertEquals(2, reviewed.documents)
    }

    @Test fun deadlinePreservesRawWithoutContinuingReads() = runBlocking {
        var now = 0L
        var reads = 0
        val repo = PostMatchCollectionRepository(temporary.root)
        val result = PostMatchDiscovery(repo, zone) { now }.search(record, listOf(session), emptySet()) { name, _ ->
            reads++; now = 60_000; snapshot(name)
        }
        assertEquals(1, reads)
        assertEquals(PairingStatus.PENDING, result.pairing.status)
        assertEquals(1, repo.entries.value.single().logs.size)
        assertTrue(result.statusText().contains("deadline"))
    }

    @Test fun cancellationPropagatesAndRecordsRecovery() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        assertFailsWith<CancellationException> {
            PostMatchDiscovery(repo, zone).search(record, listOf(session), emptySet()) { _, _ -> throw CancellationException("closed") }
        }
        assertEquals(LogCollectionState.FAILED, repo.entries.value.single().state)
        assertTrue(repo.entries.value.single().error!!.contains("cancelled"))
    }

    @Test fun matchLimitAndSessionLimitStayIncomplete() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val large = PostMatchDiscovery(repo, zone).search(record, listOf(session), emptySet()) { name, _ ->
            snapshot(name, (1..25).joinToString("\n") { raw })
        }
        assertEquals(PairingStatus.PENDING, large.pairing.status)
        assertTrue(large.statusText().contains("24 decoded-match"))
        val names = (1..7).map { "Hearthstone_2026_10_13_12_0${it}_00" }
        var reads = 0
        val limited = PostMatchDiscovery(repo, zone).search(record, names, emptySet()) { name, _ -> reads++; snapshot(name, "no matches") }
        assertEquals(3, reads)
        assertEquals(PairingStatus.PENDING, limited.pairing.status)
        assertEquals(3, limited.pendingSessions.size)
        val expanded = PostMatchDiscovery(repo, zone).search(record, names, emptySet(), limited) { name, _ -> reads++; snapshot(name, "no matches") }
        assertEquals(6, reads)
        assertTrue(expanded.pendingSessions.isEmpty())
        assertEquals(6, expanded.bundles.size)
    }

    @Test fun timeoutKeepsEarlierCandidatesButNeverClaimsAcceptance() = runBlocking {
        var now = 0L
        var reads = 0
        val repo = PostMatchCollectionRepository(temporary.root)
        val other = "Hearthstone_2026_10_13_11_59_00"
        val result = PostMatchDiscovery(repo, zone) { now }.search(record, listOf(session, other), emptySet()) { name, _ ->
            reads++
            if (name == other) now = 60_000
            snapshot(name)
        }
        assertEquals(2, reads)
        assertEquals(PairingStatus.PENDING, result.pairing.status)
        assertNull(result.pairing.acceptedSourceId)
        assertTrue(result.candidates.isNotEmpty())
        assertTrue(result.statusText().startsWith("60-second search deadline reached"))
    }

    @Test fun nextBatchFindsGameWithoutRereadingEarlierSessions() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val names = (1..6).map { "Hearthstone_2026_10_13_11_0${it}_00" }
        val order = FusionDiscoveryPolicy.sessions(names, record.timestamp, zone)
        val reads = mutableListOf<String>()
        val initial = PostMatchDiscovery(repo, zone).search(record, names, emptySet()) { name, _ ->
            reads += name; snapshot(name, "no matches")
        }
        assertEquals(order.take(3), reads)
        assertEquals(PairingStatus.PENDING, initial.pairing.status)
        val expanded = PostMatchDiscovery(repo, zone).search(record, names, emptySet(), initial) { name, _ ->
            reads += name; snapshot(name, if (name == order[3]) raw else "no matches")
        }
        assertEquals(order, reads)
        assertEquals(reads.size, reads.distinct().size)
        assertEquals(PairingStatus.ACCEPTED, expanded.pairing.status, expanded.pairing.toString())
        assertEquals(6, expanded.bundles.size)
        assertTrue(expanded.pendingSessions.isEmpty())
    }

    @Test fun firstBatchCanRecommendOnlyItsInspectedScopeAndErrorsLeadStatus() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val names = (1..6).map { "Hearthstone_2026_10_13_11_0${it}_00" }
        val order = FusionDiscoveryPolicy.sessions(names, record.timestamp, zone)
        val result = PostMatchDiscovery(repo, zone).search(record, names, emptySet()) { name, _ ->
            snapshot(name, if (name == order.first()) raw else "no matches")
        }
        assertEquals(PairingStatus.ACCEPTED, result.pairing.status)
        assertEquals(3, result.pendingSessions.size)
        assertTrue(result.notes.any { it.contains("Unsearched sessions") })
        val failed = PostMatchDiscovery(repo, zone).search(record, listOf(names.first()), emptySet()) { _, _ -> error("No descriptor") }
        assertTrue(failed.statusText().startsWith("Unreadable/unprocessed session:"))
        assertTrue(failed.statusText().contains("No descriptor"))
    }

    @Test fun alreadyFusedImportIsPreservedWithoutParsingNewInputs() = runBlocking {
        val repo = FusionImportRepository(temporary.root)
        repo.begin(record.id, listOf("original"))
        val entry = repo.transition(record.id, FusionImportState.FUSED_LOCAL, artifactFile = "unchanged.json")
        val result = FusionImporter(repo, zone).import(record, listOf("invalid new bundle".toByteArray()), confirmedSourceId = "other")
        assertEquals(entry, result.entry)
        assertEquals(listOf(PostMatchPresentation.EXISTING_ARTIFACT_PRESERVED), result.diagnostics)
        assertEquals(1, repo.entries.value.single().attempts)
    }
}
