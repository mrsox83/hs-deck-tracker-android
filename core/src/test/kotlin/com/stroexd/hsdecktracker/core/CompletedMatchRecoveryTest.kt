package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.data.CompletedMatchCommitter
import com.stroexd.hsdecktracker.core.data.MatchOutboxRepository
import com.stroexd.hsdecktracker.core.data.MatchRepository
import com.stroexd.hsdecktracker.core.data.MatchTransferResult
import com.stroexd.hsdecktracker.core.data.MatchOutboxState
import com.stroexd.hsdecktracker.core.cards.HsClass
import com.stroexd.hsdecktracker.core.stats.MatchJsonExport
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.stats.MatchResult
import com.stroexd.hsdecktracker.core.stats.ResultCorrection
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class CompletedMatchRecoveryTest {
    @Test fun recoveryPreservesUiSavedRevisionWithoutRestagingOutbox() = runTest {
        val dir = Files.createTempDirectory("hs-saved-edit-recovery-").toFile()
        try {
            val original = MatchRecord(id = "edited", timestamp = 1, result = MatchResult.WIN)
            val edited = original.copy(notes = "Keep my saved note", result = MatchResult.LOSS,
                revision = 2, editedAt = 2)
            val matches = MatchRepository(dir)
            CompletedMatchCommitter(matches, MatchOutboxRepository(dir)) { MatchTransferResult.WRITTEN }
                .commit(original, transferEnabled = false)
            // MatchDetailScreen's Save path updates history directly, not the outbox.
            matches.update(edited)
            assertEquals(original, MatchOutboxRepository(dir).entries.value.single().match)

            CompletedMatchCommitter(MatchRepository(dir), MatchOutboxRepository(dir)) { MatchTransferResult.WRITTEN }
                .recover(transferEnabled = false)

            assertEquals(edited, MatchRepository(dir).get(original.id))
        } finally {
            dir.deleteRecursively()
        }
    }

    private suspend fun withStore(block: suspend (File) -> Unit) {
        val dir = Files.createTempDirectory("hs-saved-edit-cases-").toFile()
        try { block(dir) } finally { dir.deleteRecursively() }
    }

    private fun original(id: String = "edited") = MatchRecord(id = id, timestamp = 1, result = MatchResult.WIN)

    private fun edited(record: MatchRecord, revision: Int = 2) = record.copy(
        revision = revision, editedAt = revision.toLong(), notes = "Saved note\nUnicode: español",
        result = MatchResult.LOSS, opponentClass = HsClass.PRIEST, wentFirst = false,
        resultCorrections = listOf(ResultCorrection(MatchResult.WIN, MatchResult.LOSS, 2)),
    )

    @Test fun failedTransferRecoveryAndRetryUseLatestSavedFields() = runTest { withStore { dir ->
        val first = original()
        val latest = edited(first)
        val matches = MatchRepository(dir)
        CompletedMatchCommitter(matches, MatchOutboxRepository(dir)) { error("offline") }
            .commit(first, transferEnabled = true)
        matches.update(latest)
        val attempted = mutableListOf<MatchRecord>()
        val failed = CompletedMatchCommitter(MatchRepository(dir), MatchOutboxRepository(dir)) {
            attempted += it
            error("still offline")
        }.recover(transferEnabled = true).single()
        assertEquals(MatchOutboxState.TRANSFER_FAILED, failed.entry.state)
        assertEquals(listOf(latest), attempted)
        assertEquals(latest, MatchRepository(dir).get(first.id))
        val retried = CompletedMatchCommitter(MatchRepository(dir), MatchOutboxRepository(dir)) {
            attempted += it
            MatchTransferResult.WRITTEN
        }.recover(transferEnabled = true).single()
        assertEquals(listOf(latest, latest), attempted)
        assertEquals(latest, retried.entry.match)
        assertEquals(MatchOutboxState.LOCALLY_EXPORTED, retried.entry.state)
        assertEquals(2, retried.entry.attempts)
        assertEquals(1, MatchRepository(dir).matches.value.size)
    } }

    @Test fun recoveryReconcilesHistoryNewerEqualAndOlderThanOutbox() = runTest {
        for ((savedRevision, stagedRevision) in listOf(3 to 2, 2 to 2, 1 to 2)) withStore { dir ->
            val saved = original().copy(revision = savedRevision, notes = "revision $savedRevision")
            val staged = original().copy(revision = stagedRevision, notes = "revision $stagedRevision")
            MatchRepository(dir).add(saved)
            MatchOutboxRepository(dir).stage(staged)
            val expected = if (savedRevision >= stagedRevision) saved else staged
            val recovered = CompletedMatchCommitter(MatchRepository(dir), MatchOutboxRepository(dir)) {
                error("Transfer disabled")
            }.recover(transferEnabled = false).single()
            assertEquals(expected, recovered.entry.match)
            assertEquals(expected, MatchRepository(dir).get(saved.id))
            assertEquals(expected, MatchOutboxRepository(dir).entries.value.single().match)
        }
    }

    @Test fun equalRevisionConflictPreservesBothStoresAndDoesNotExport() = runTest { withStore { dir ->
        val saved = edited(original()).copy(notes = "saved")
        val staged = saved.copy(notes = "conflicting pending content")
        MatchRepository(dir).add(saved)
        val before = MatchOutboxRepository(dir).stage(staged)
        var transfers = 0
        val committer = CompletedMatchCommitter(MatchRepository(dir), MatchOutboxRepository(dir)) {
            transfers++
            MatchTransferResult.WRITTEN
        }
        assertFailsWith<IllegalArgumentException> { committer.recover(transferEnabled = true) }
        assertEquals(saved, MatchRepository(dir).get(saved.id))
        assertEquals(before, MatchOutboxRepository(dir).entries.value.single())
        assertEquals(0, transfers)
    } }

    @Test fun staleCommitArgumentUsesNewerSavedRecordBeforeExport() = runTest { withStore { dir ->
        val stale = original()
        val latest = edited(stale, revision = 3)
        MatchRepository(dir).add(latest)
        MatchOutboxRepository(dir).stage(stale)
        val exported = mutableListOf<MatchRecord>()
        val result = CompletedMatchCommitter(MatchRepository(dir), MatchOutboxRepository(dir)) {
            exported += it
            MatchTransferResult.WRITTEN
        }.commit(stale, transferEnabled = true)
        assertEquals(listOf(latest), exported)
        assertEquals(latest, result.entry.match)
        assertEquals(latest, MatchRepository(dir).get(stale.id))
    } }

    @Test fun conflictsInOlderRevisionsDoNotOverrideUnambiguousNewestSavedRecord() = runTest { withStore { dir ->
        val latest = edited(original())
        MatchRepository(dir).add(latest)
        MatchOutboxRepository(dir).stage(original().copy(notes = "old pending"))
        val result = CompletedMatchCommitter(MatchRepository(dir), MatchOutboxRepository(dir)) {
            error("Transfer disabled")
        }.commit(original().copy(notes = "different old caller"), transferEnabled = false)
        assertEquals(latest, result.entry.match)
        assertEquals(latest, MatchRepository(dir).get(latest.id))
    } }

    @Test fun newestExplicitEmptyNoteIsPreservedRatherThanBackfilled() = runTest { withStore { dir ->
        val first = original().copy(notes = "delete this note deliberately")
        val matches = MatchRepository(dir)
        CompletedMatchCommitter(matches, MatchOutboxRepository(dir)) { MatchTransferResult.WRITTEN }
            .commit(first, transferEnabled = false)
        val cleared = first.copy(notes = "", revision = 2, editedAt = 2)
        matches.update(cleared)
        CompletedMatchCommitter(MatchRepository(dir), MatchOutboxRepository(dir)) { error("Transfer disabled") }
            .recover(transferEnabled = false)
        assertEquals(cleared, MatchRepository(dir).get(first.id))
    } }

    @Test fun retryIsIdempotentAndAdjacentGameIsUnchanged() = runTest { withStore { dir ->
        val first = original("first")
        val adjacent = original("adjacent").copy(notes = "other game")
        val matches = MatchRepository(dir)
        val outbox = MatchOutboxRepository(dir)
        val exported = mutableListOf<MatchRecord>()
        val committer = CompletedMatchCommitter(matches, outbox) { exported += it; MatchTransferResult.WRITTEN }
        committer.commit(first, transferEnabled = false)
        committer.commit(adjacent, transferEnabled = false)
        val latest = edited(first)
        matches.update(latest)
        committer.recover(transferEnabled = false)
        committer.recover(transferEnabled = true)
        assertEquals(emptyList(), committer.recover(transferEnabled = true))
        assertEquals(listOf(latest, adjacent), exported)
        assertEquals(listOf(latest, adjacent), MatchRepository(dir).matches.value)
        assertEquals(2, outbox.entries.value.size)
    } }

    @Test fun previouslyExportedRecordDoesNotUndoEditAndManualExportUsesNewFilename() = runTest { withStore { dir ->
        val first = original()
        val matches = MatchRepository(dir)
        val outbox = MatchOutboxRepository(dir)
        val exported = mutableListOf<MatchRecord>()
        val committer = CompletedMatchCommitter(matches, outbox) { exported += it; MatchTransferResult.WRITTEN }
        committer.commit(first, transferEnabled = true)
        val latest = edited(first)
        matches.update(latest)
        assertEquals(emptyList(), committer.recover(transferEnabled = true))
        assertEquals(latest, MatchRepository(dir).get(first.id))
        committer.commit(checkNotNull(matches.get(first.id)), transferEnabled = true)
        assertEquals(listOf(first, latest), exported)
        assertNotEquals(MatchJsonExport.filename(first), MatchJsonExport.filename(latest))
    } }

    @Test fun staleHistoryWritesAreIgnoredAndSameRevisionConflictsAreRefused() = runTest { withStore { dir ->
        val latest = edited(original(), revision = 3)
        val matches = MatchRepository(dir)
        matches.add(latest)
        matches.update(original())
        matches.upsert(edited(original()))
        assertEquals(latest, matches.get(latest.id))
        assertFailsWith<IllegalArgumentException> { matches.update(latest.copy(notes = "conflict")) }
        assertFailsWith<IllegalArgumentException> { matches.upsert(latest.copy(result = MatchResult.WIN)) }
        assertEquals(latest, MatchRepository(dir).get(latest.id))
    } }

    @Test fun staleOutboxStagePreservesNewerTerminalState() = runTest { withStore { dir ->
        val latest = edited(original())
        val outbox = MatchOutboxRepository(dir)
        outbox.stage(latest)
        val terminal = outbox.transition(latest.id, MatchOutboxState.LOCALLY_EXPORTED)
        assertEquals(terminal, outbox.stage(original()))
        assertFailsWith<IllegalArgumentException> { outbox.stage(latest.copy(notes = "conflict")) }
        assertEquals(terminal, MatchOutboxRepository(dir).entries.value.single())
    } }

    @Test fun uiEditDuringRecoveryTransferSurvivesReopen() = runTest { withStore { dir ->
        val matches = MatchRepository(dir)
        val outbox = MatchOutboxRepository(dir)
        CompletedMatchCommitter(matches, outbox) { MatchTransferResult.WRITTEN }
            .commit(original(), transferEnabled = false)
        val transferStarted = CompletableDeferred<Unit>()
        val allowTransfer = CompletableDeferred<Unit>()
        val recovery = async {
            CompletedMatchCommitter(matches, outbox) {
                transferStarted.complete(Unit)
                allowTransfer.await()
                MatchTransferResult.WRITTEN
            }.recover(transferEnabled = true)
        }
        transferStarted.await()
        val latest = edited(original())
        matches.update(latest)
        allowTransfer.complete(Unit)
        recovery.await()
        assertEquals(latest, MatchRepository(dir).get(latest.id))
        CompletedMatchCommitter(matches, outbox) { error("No pending transfer") }.recover(transferEnabled = true)
        assertEquals(latest, matches.get(latest.id))
    } }
}
