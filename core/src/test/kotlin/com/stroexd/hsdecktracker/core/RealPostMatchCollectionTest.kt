package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.*

class RealPostMatchCollectionTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun pinnedReconnectRawLogsReproduceEveryOriginalMatch() = runBlocking {
        val file = File(checkNotNull(System.getProperty("hsPostMatchFixtureZip")))
        val originalBytes = file.readBytes()
        assertEquals("6854c9f0259e95b944da6efe98de9bb4d4b40c969e412fd9c2c0cf9a3eab3738", EvidenceAdapters.sha256(originalBytes))
        val original = EvidenceAdapters.exporterBundle(originalBytes)
        assertEquals(2, original.matches.size)
        val repository = PostMatchCollectionRepository(temporary.root)
        val entry = repository.begin("private-reconnect-fixture")
        ZipFile(file).use { zip ->
            val logs = zip.entries().asSequence().filter { it.name.endsWith("/Power.log") }.toList()
            assertEquals(2, logs.size)
            for (log in logs) {
                val bytes = zip.getInputStream(log).use { it.readBytes() }
                val snapshot = PostMatchLogSnapshot.read(log.name.substringBefore('/'), bytes.size.toLong(), { ByteArrayInputStream(bytes) })
                repository.retain(entry.id, snapshot)
            }
        }
        val bundle = repository.buildBundle(entry.id)
        val rebuilt = EvidenceAdapters.exporterBundle(bundle.readBytes())
        assertEquals(original.matches.map { it.first.id }.toSet(), rebuilt.matches.map { it.first.id }.toSet())
        for ((source, document) in original.matches) {
            val other = rebuilt.matches.single { it.first.id == source.id }
            assertEquals(document["match"], other.second["match"], "Parser must not reinterpret original evidence")
            val before = PowerEvidenceReducer.reduce(source, document)
            val after = PowerEvidenceReducer.reduce(other.first, other.second)
            assertEquals(before.completed, after.completed)
            assertEquals(before.sourceTruncated, after.sourceTruncated)
            assertEquals(before.events.size, after.events.size)
            assertEquals(before.snapshots.size, after.snapshots.size)
        }
        assertEquals(1, rebuilt.matches.count { it.first.complete == true })
        assertContentEquals(bundle.readBytes(), repository.buildBundle(entry.id).readBytes())
    }
}
