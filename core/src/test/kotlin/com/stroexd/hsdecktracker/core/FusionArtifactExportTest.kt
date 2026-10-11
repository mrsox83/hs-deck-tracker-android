package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.*
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.stats.MatchResult
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

class FusionArtifactExportTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun exportRequiresFusedStateAndExistingMatchOwnedArtifact() = runBlocking {
        val repo = FusionImportRepository(temporary.root)
        assertFailsWith<IllegalStateException> { repo.artifactForExport("one") }
        repo.begin("one", emptyList())
        assertFailsWith<IllegalStateException> { repo.artifactForExport("one") }
        val record = MatchRecord(id = "one", timestamp = 1, result = MatchResult.WIN)
        val tracker = EvidenceAdapters.tracker(AppJson.encodeToString(MatchRecord.serializer(), record).toByteArray())
        val fused = FusionCoordinator.assemble(tracker, emptyList(),
            PairingDecision(PairingStatus.PENDING, candidates = emptyList(), reason = "Export fixture"))
        val logicalName = "${fused.matchId}.json"
        assertNotEquals("one.json", logicalName)
        repo.transition("one", FusionImportState.FUSED_LOCAL, artifactFile = logicalName)
        assertFailsWith<IllegalArgumentException> { repo.artifactForExport("one") }
        val artifact = File(temporary.root, "fused-matches/$logicalName")
        assertEquals(logicalName, repo.writeArtifact(fused))
        val original = artifact.readBytes()
        assertEquals(artifact.canonicalFile, repo.artifactForExport("one"))
        assertContentEquals(original, artifact.readBytes())
    }

    @Test fun exportRejectsTraversalOrAnotherMatchFilename() = runBlocking {
        val repo = FusionImportRepository(temporary.root)
        repo.begin("one", emptyList())
        for (path in listOf("../secret.json", "two.json", "dir/one.json", "dir\\one.json")) {
            repo.transition("one", FusionImportState.FUSED_LOCAL, artifactFile = path)
            assertFailsWith<IllegalArgumentException> { repo.artifactForExport("one") }
        }
    }
}
