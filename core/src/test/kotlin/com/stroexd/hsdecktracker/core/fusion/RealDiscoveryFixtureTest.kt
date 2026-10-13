package com.stroexd.hsdecktracker.core.fusion

import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.ZoneId
import kotlin.test.*

class RealDiscoveryFixtureTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun multiGamePrivatePreviewAndConfirmedImportPreserveSelectedIdentity() = runBlocking {
        val root = File(checkNotNull(System.getProperty("hsDiscoveryFixtureDir")))
        val bundle = File(root, "fusion-test-5ac9586b-19b0-4a06-98f6-91dd23c4e87a-5c56b471-f751-459f-9ca3-f94480a0d48e.zip").readBytes()
        assertEquals("12570727d01d7a510e769a28ccb0230d5c6e753ecd6a7a889ff40fe435d16bd4", EvidenceAdapters.sha256(bundle))
        val tracker = EvidenceAdapters.tracker(File(root, "match_20261012T181907620Z_2f7db814c79afbcc98a3ab0a5efa6067220e19ee5fa5c474fb451d64203160e8.json").readBytes())
        val originals = EvidenceAdapters.exporterBundle(bundle).matches.map { PowerEvidenceReducer.reduce(it.first, it.second) }
        val cards = tracker.record.timeline.mapNotNull { it.cardId }.toSet()
        val evaluation = FusionEvaluator.evaluate(tracker, originals, ZoneId.of("America/Chicago"), cards)
        val preview = evaluation.discoveryDecision(true)
        assertEquals(PairingStatus.ACCEPTED, preview.status, preview.toString())
        assertTrue(preview.acceptedSourceId!!.endsWith(":1"))
        assertEquals(PairingStatus.PENDING, evaluation.discoveryDecision(false).status)
        val repository = FusionImportRepository(temporary.root)
        val importer = FusionImporter(repository, ZoneId.of("America/Chicago"))
        val changed = importer.import(tracker.record, listOf(bundle), cards, "not-the-previewed-source")
        assertEquals(FusionImportState.PENDING_PAIRING, changed.entry.state)
        assertNull(changed.entry.artifactFile)
        val imported = importer.import(tracker.record, listOf(bundle), cards, preview.acceptedSourceId)
        assertEquals(FusionImportState.FUSED_LOCAL, imported.entry.state)
        val artifact = repository.artifactForExport(tracker.record.id).readBytes()
        val fused = FusionArtifactCodec.decode(artifact.decodeToString())
        assertEquals("d4d4dd3dbb19ee9c02d6e592c93c8678", fused.matchId)
        assertEquals(402, fused.events.size)
        assertTrue(FusionProvenanceValidator.validate(fused).isEmpty())
        assertEquals(imported.entry, importer.import(tracker.record, listOf(bundle), cards, preview.acceptedSourceId).entry)
        assertContentEquals(artifact, repository.artifactForExport(tracker.record.id).readBytes())
    }

    @Test fun verifiedReconnectDoesNotCompeteWithItsOwnParents() {
        val root = File(checkNotNull(System.getProperty("hsDiscoveryFixtureDir")))
        val tracker = EvidenceAdapters.tracker(File(root,
            "match_20261008T041335265Z_bb7a20f1d4ac2bb0787aceb75997fd0643cdece6d795dd3bfcfa661e75018739.json").readBytes())
        val bundle = File(root, "HS-export-20261007-231433-ae674730.zip").readBytes()
        val originals = EvidenceAdapters.exporterBundle(bundle).matches.map { PowerEvidenceReducer.reduce(it.first, it.second) }
        val evaluation = FusionEvaluator.evaluate(tracker, originals, ZoneId.of("America/Chicago"), setOf("CATA_190h"))
        val preview = evaluation.discoveryDecision(true)
        assertEquals(PairingStatus.ACCEPTED, preview.status, preview.toString())
        val accepted = evaluation.powers.single { it.source.id == preview.acceptedSourceId }
        assertNotNull(accepted.continuation)
        assertEquals(2, accepted.source.parentSourceIds.size)
        assertTrue(preview.candidates.none { it.powerSourceId in accepted.source.parentSourceIds })
    }
}
