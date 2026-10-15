package com.stroexd.hsdecktracker.core.fusion

import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipFile
import kotlin.test.*

class GameLogExportTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun exportUsesOwnedSessionAndHashNotNewestCollection() = runBlocking {
        val repo = PostMatchCollectionRepository(temporary.root)
        val wanted = "Hearthstone_2026_10_14_18_40_30"
        val newer = "Hearthstone_2026_10_14_22_12_39"
        fun snapshot(session: String, text: String) = PostMatchLogSnapshot.read(session, text.toByteArray().size.toLong(), { text.byteInputStream() })
        val saved = snapshot(wanted, "raw wanted")
        repo.retain(repo.begin("game").id, saved)
        repo.retain(repo.begin("game").id, snapshot(newer, "raw unrelated"))
        val count = repo.entries.value.size
        val zip = repo.buildGameBundle("game", mapOf(wanted to saved.sourceSha256))
        ZipFile(zip).use { archive ->
            assertNotNull(archive.getEntry("$wanted/Power.log"))
            assertNull(archive.getEntry("$newer/Power.log"))
            assertEquals("raw wanted", archive.getInputStream(archive.getEntry("$wanted/Power.log")).reader().readText())
        }
        assertEquals(count, repo.entries.value.size)
        assertFailsWith<IllegalArgumentException> { repo.buildGameBundle("other-game", mapOf(wanted to saved.sourceSha256)) }
        assertFailsWith<IllegalArgumentException> { repo.buildGameBundle("game", mapOf(wanted to "0".repeat(64))) }
        Unit
    }
}
