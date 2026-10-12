package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.*
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.ZoneId
import kotlin.test.*

class RealPendingPairingTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun afterMidnightSessionPairsTheEarlierOfTwoCompletedGames() = runBlocking {
        val bundle = File(checkNotNull(System.getProperty("hsPendingZip"))).readBytes()
        val bytes = File(checkNotNull(System.getProperty("hsPendingTracker"))).readBytes()
        assertEquals("bec5952bf14a6d2e430b356ef7def161dec642eaa62e3b43ce87a4164419d32e", EvidenceAdapters.sha256(bundle))
        assertEquals("cb7a5a454066d84a92c1a37cd1c05de7174b88892dc0d1dc46c548c89c208829", EvidenceAdapters.sha256(bytes))
        val parsed = EvidenceAdapters.exporterBundle(bundle)
        assertEquals(2, parsed.matches.size)
        val tracker = EvidenceAdapters.tracker(bytes)
        for ((source, doc) in parsed.matches) {
            val power = PowerEvidenceReducer.reduce(source, doc)
            for (controller in listOf(1, 2)) {
                val end = PowerClock.resolve(source.sessionAlias!!, power.endedLogTime!!, ZoneId.of("America/Chicago"))
                val facts = PowerPairingFacts.fromReduced(power, end, controller)
                println("match=${power.index} controller=$controller result=${facts.result} first=${facts.wentFirst} " +
                    FusionPairing.decide(tracker, listOf(facts)))
            }
        }
        val record = AppJson.decodeFromString(MatchRecord.serializer(), bytes.decodeToString())
        val repo = FusionImportRepository(temporary.root)
        val result = FusionImporter(repo, ZoneId.of("America/Chicago")).import(record, listOf(bundle))
        println("Importer decision: ${result.entry.pairing}")
        assertEquals(FusionImportState.FUSED_LOCAL, result.entry.state)
        val accepted = checkNotNull(result.entry.pairing).acceptedSourceId
        assertEquals(parsed.matches.first().first.id, accepted)
        assertTrue(result.entry.pairing!!.candidates.first { it.powerSourceId == accepted }.reasons.contains("endpoints differ by 6s"))
        val fused = FusionArtifactCodec.decode(repo.artifactForExport(record.id).readText())
        assertTrue(fused.events.isNotEmpty())
        assertTrue(FusionProvenanceValidator.validate(fused).isEmpty())
        assertEquals(1, result.entry.coverageWarnings.size) // Other completed game is not incorporated.
        Unit
    }
}
