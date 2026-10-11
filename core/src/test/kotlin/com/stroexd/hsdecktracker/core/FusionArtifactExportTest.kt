package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.*
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
        repo.transition("one", FusionImportState.FUSED_LOCAL, artifactFile = "one.json")
        assertFailsWith<IllegalArgumentException> { repo.artifactForExport("one") }
        val artifact = File(temporary.root, "fused-matches/one.json")
        artifact.parentFile.mkdirs()
        artifact.writeText("{}")
        assertEquals(artifact.canonicalFile, repo.artifactForExport("one"))
        assertEquals("{}", artifact.readText())
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
