package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.*
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipFile

class PostMatchCollectionTest {
    @get:Rule val temporary = TemporaryFolder()
    private val session = "Hearthstone_2026_10_10_12_00_00"
    private val normal = """
        D 12:00:00.000 GameState.DebugPrintPower() - CREATE_GAME
        D 12:00:00.001 PowerTaskList.DebugPrintPower() - CREATE_GAME
        D 12:00:01.000 GameState.DebugPrintPower() - TAG_CHANGE Entity=GameEntity tag=TURN value=1
        D 12:00:02.000 GameState.DebugPrintPower() - TAG_CHANGE Entity=GameEntity tag=STEP value=FINAL_GAMEOVER
    """.trimIndent()
    private fun snapshot(text: String = normal, name: String = session): PostMatchLogSnapshot.Snapshot {
        val bytes = text.toByteArray()
        return PostMatchLogSnapshot.read(name, bytes.size.toLong(), { ByteArrayInputStream(bytes) })
    }

    @Test fun retainedBundleRoundTripsExistingAdapterAndIsStableOnRetry() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val job = repo.begin("tracker-match")
        repo.retain(job.id, snapshot())
        val bundle = repo.buildBundle(job.id)
        val original = bundle.readBytes()
        val evidence = EvidenceAdapters.exporterBundle(original)
        assertEquals("hs-export-bundle/0.4", evidence.schema)
        assertEquals(1, evidence.matches.size)
        assertTrue(evidence.matches.single().first.complete == true)
        assertEquals(session, evidence.matches.single().first.sessionAlias)
        assertEquals(snapshot().sourceSha256, evidence.matches.single().first.contentSha256["$session/Power.log"])
        assertEquals(LogCollectionState.BUNDLE_READY, repo.entries.value.single().state)
        assertContentEquals(original, repo.buildBundle(job.id).readBytes())
        ZipFile(bundle).use { zip -> assertEquals(normal, zip.getInputStream(zip.getEntry("$session/Power.log")).readBytes().decodeToString()) }
    }

    @Test fun repeatedAndRotatedSourcesRetainOneRawFileButBothAliases() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val job = repo.begin("tracker")
        repo.retain(job.id, snapshot())
        repo.retain(job.id, snapshot())
        repo.retain(job.id, snapshot(name = "Hearthstone_2"))
        assertEquals(2, repo.entries.value.single().logs.size)
        assertEquals(1, File(temporary.root, "post-match-logs").listFiles()!!.count { it.name.endsWith(".Power.log") })
        val evidence = EvidenceAdapters.exporterBundle(repo.buildBundle(job.id).readBytes())
        assertEquals(1, evidence.matches.map { it.first.id }.distinct().size)
    }

    @Test fun recoveryRetainsEvidenceAndDoesNotGuessOrDuplicateTrackerRecords() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val empty = repo.begin("one")
        val retained = repo.begin("two")
        repo.retain(retained.id, snapshot())
        val reopened = PostMatchCollectionRepository(temporary.root)
        reopened.recoverInterrupted()
        assertEquals(LogCollectionState.INTERRUPTED, reopened.entries.value.first { it.id == empty.id }.state)
        assertEquals(LogCollectionState.RAW_RETAINED, reopened.entries.value.first { it.id == retained.id }.state)
        assertEquals(1, EvidenceAdapters.exporterBundle(reopened.buildBundle(retained.id).readBytes()).matches.size)
        assertEquals(2, reopened.entries.value.size)
    }

    @Test fun tamperedRawIsRejectedWithoutOverwritingOrDeletingIt() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val job = repo.begin("one")
        val source = snapshot()
        repo.retain(job.id, source)
        val raw = File(temporary.root, "post-match-logs/${source.sourceSha256}.Power.log")
        raw.writeText("x".repeat(source.sourceBytes.toInt()))
        assertFailsWith<IllegalArgumentException> { repo.buildBundle(job.id) }
        assertTrue(raw.exists())
        assertFalse(File(raw.parentFile, "${job.id}.zip").exists())
    }

    @Test fun truncatedAndNoMatchRemainHonestRawEvidence() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val job = repo.begin("one")
        repo.retain(job.id, snapshot(normal + "\nTruncating log, which has reached the size limit"))
        val parsed = EvidenceAdapters.exporterBundle(repo.buildBundle(job.id).readBytes())
        assertTrue(parsed.diagnostics.any { it.contains("truncation") })
        val empty = repo.begin("two")
        repo.retain(empty.id, snapshot("no CREATE_GAME"))
        val bundle = repo.buildBundle(empty.id)
        assertTrue(EvidenceAdapters.exporterBundle(bundle.readBytes()).matches.isEmpty())
        ZipFile(bundle).use { zip ->
            assertTrue(zip.getInputStream(zip.getEntry("manifest.json")).reader().readText().contains("RAW_ONLY_NO_MATCHES"))
        }
    }

    @Test fun cancelledBuildRetainsRawAndCleansOnlyItsStaging() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val job = repo.begin("one")
        repo.retain(job.id, snapshot())
        assertFailsWith<java.util.concurrent.CancellationException> { repo.buildBundle(job.id) { true } }
        val files = File(temporary.root, "post-match-logs").listFiles()!!
        assertTrue(files.any { it.name.endsWith(".Power.log") })
        assertFalse(files.any { it.name.endsWith(".zip") || it.name.endsWith(".part") })
    }

    @Test fun partialReadFailureIsPreservedInJournalAndManifest() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val job = repo.begin("one")
        repo.retain(job.id, snapshot())
        repo.recordReadFailure(job.id, "Hearthstone_2", "No Power.log descriptor")
        val bundle = repo.buildBundle(job.id)
        assertEquals(1, repo.entries.value.single().readFailures.size)
        ZipFile(bundle).use { zip ->
            val manifest = zip.getInputStream(zip.getEntry("manifest.json")).reader().readText()
            assertTrue(manifest.contains("READ_FAILED"))
            assertTrue(manifest.contains("No Power.log descriptor"))
        }
        assertEquals(1, EvidenceAdapters.exporterBundle(bundle.readBytes()).matches.size)
    }

    @Test fun interruptedRawOnlyAttemptRegeneratesAfterReopenWithoutReader() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val attempt = repo.begin("tracker-match")
        val source = snapshot()
        repo.retain(attempt.id, source)
        assertFailsWith<java.util.concurrent.CancellationException> { repo.buildBundle(attempt.id) { true } }
        val reopened = PostMatchCollectionRepository(temporary.root)
        reopened.recoverInterrupted()
        val bundle = reopened.buildBundle(attempt.id)
        val bytes = bundle.readBytes()
        assertEquals(1, EvidenceAdapters.exporterBundle(bytes).matches.size)
        assertContentEquals(bytes, reopened.buildBundle(attempt.id).readBytes())
        assertEquals(1, reopened.entries.value.size)
        assertEquals("tracker-match", reopened.entries.value.single().matchId)
        assertContentEquals(source.copyBytes(), File(temporary.root, "post-match-logs/${source.sourceSha256}.Power.log").readBytes())
    }

    @Test fun permissionOrBinderReadFailuresNeverBecomeBundleSuccess() = runBlocking {
        for (reason in listOf("Permission denied", "Reader disconnected; reconnect manually")) {
            val directory = temporary.newFolder()
            val repo = PostMatchCollectionRepository(directory)
            val attempt = repo.begin("tracker-match")
            repo.recordReadFailure(attempt.id, session, reason)
            repo.fail(attempt.id, reason)
            assertFailsWith<IllegalArgumentException> { repo.buildBundle(attempt.id) }
            val reopened = PostMatchCollectionRepository(directory)
            reopened.recoverInterrupted()
            assertEquals(LogCollectionState.FAILED, reopened.entries.value.single().state)
            assertEquals(reason, reopened.entries.value.single().readFailures[session])
            assertTrue(reopened.entries.value.single().logs.isEmpty())
            assertFalse(File(directory, "post-match-logs").listFiles()!!.any { it.name.endsWith(".zip") })
        }
    }
}
