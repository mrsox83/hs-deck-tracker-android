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

/** Private files are supplied explicitly and hash-pinned, never checked into Git. */
class RealManualReconnectTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun manualRecordDoesNotSilentlyClaimBothReconnectFragmentsWereReconstructed() = runBlocking {
        val bundle = File(checkNotNull(System.getProperty("hsManualReconnectZip"))).readBytes()
        val recordBytes = File(checkNotNull(System.getProperty("hsManualReconnectTracker"))).readBytes()
        assertEquals("b688245ea65034ff967762ecbc44b0c047ed996437f663a77f4e7aa5f1909419", EvidenceAdapters.sha256(bundle))
        assertEquals("5b9101309a2cf21622387adadc885d37513c30b71fcdb625e64694a4af398676", EvidenceAdapters.sha256(recordBytes))
        val tracker = EvidenceAdapters.tracker(recordBytes)
        val originals = EvidenceAdapters.exporterBundle(bundle).matches.map { PowerEvidenceReducer.reduce(it.first, it.second) }
        assertEquals(2, originals.size)
        val earlier = originals.single { !it.completed }
        val later = originals.single { it.completed }
        val decision = PowerContinuationStitcher.evaluate(earlier, later, tracker, ZoneId.of("America/Chicago"))
        println("Manual reconnect rejection: ${decision.reasons.joinToString("; ")}")
        assertEquals(ContinuationStatus.REJECTED, decision.status)
        assertTrue(decision.reasons.contains("no unique open-choice continuation anchor"))
        val repository = FusionImportRepository(temporary.root)
        val record = AppJson.decodeFromString(MatchRecord.serializer(), recordBytes.decodeToString())
        val result = FusionImporter(repository, ZoneId.of("America/Chicago")).import(record, listOf(bundle))
        assertEquals(FusionImportState.FUSED_LOCAL, result.entry.state)
        assertTrue(result.entry.coverageWarnings.single().startsWith("Partial reconstruction:"))
        val fused = FusionArtifactCodec.decode(repository.artifactForExport(record.id).readText())
        assertEquals("0102e83964067daec883a187d0fa8eb9", fused.matchId)
        assertEquals(172, fused.events.size)
        assertEquals(later.source.id, fused.pairing.acceptedSourceId)
        assertTrue(fused.continuations.isEmpty())
        assertTrue(FusionProvenanceValidator.validate(fused).isEmpty())
        val reopened = FusionImportRepository(temporary.root).entries.value.single()
        assertEquals(result.entry.coverageWarnings, reopened.coverageWarnings)
    }
}
