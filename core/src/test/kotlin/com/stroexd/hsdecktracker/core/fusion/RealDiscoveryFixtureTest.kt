package com.stroexd.hsdecktracker.core.fusion

import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.ZoneId
import kotlin.test.*

class RealDiscoveryFixtureTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun warriorRawReplayNeverCarriesOpponentAcrossUnresolvedPlayerTurns() {
        val root = File(checkNotNull(System.getProperty("hsDiscoveryFixtureDir")))
        val bytes = File(root, "fusion-test-3957c5b3-875f-4510-a766-3356c4ef47f3-16f675c0-ab94-48e4-a753-dd7a1d1c3e51.zip").readBytes()
        val parsed = EvidenceAdapters.exporterBundle(bytes)
        val (source, document) = parsed.matches.single { it.second.getValue("match").toString().contains("\"index\":3") }
        assertEquals("05525573fe6a2386897d74aefe0b068427d51dfc700d60eace75542cd2169143",
            source.contentSha256["Hearthstone_2026_10_14_18_40_30/Power.log"])
        val reduced = PowerEvidenceReducer.reduce(source, document)
        assertEquals(290, reduced.events.size)
        val signals = reduced.snapshots.filter { it.phase == "TURN_SIGNAL" }
        assertEquals(15, signals.size)
        assertEquals(List(15) { if (it == 0) 1 else if (it % 2 == 1) 2 else null }, signals.map { it.activeController })
        assertEquals(7, reduced.diagnostics.count { it.startsWith("Turn owner unresolved") })
        assertTrue(reduced.events.filter { it.evidence.any { e -> (e.sourceLine ?: 0) >= 57550 } }.all { it.playerTurnIndex == null })
        assertTrue(signals.drop(2).all { it.playerTurnIndices.isEmpty() })
    }
    @Test fun oct14SavedCandidateReviewRecoversNullDescriptorSibling() = runBlocking {
        val root = File(checkNotNull(System.getProperty("hsDiscoveryFixtureDir")))
        val tracker = EvidenceAdapters.tracker(File(root,
            "match_20261014T210056120Z_2ac51b619e2849f3068a0165ddc7078c6baef25da2dcee38773a311c781a4d24.json").readBytes())
        val session = "Hearthstone_2026_10_14_15_50_04"
        val bytes = java.util.zip.ZipFile(File(root,
            "fusion-test-4e5edb8e-035d-4f59-a939-514e4c3c4f58-156d05bc-b19f-4c95-a52d-d629d78b7322.zip")).use { zip ->
            zip.getInputStream(zip.getEntry("$session/Power.log")).use { it.readBytes() }
        }
        assertEquals("75498254000df9a0ca6755ddcdb993bf8432fc925cd89c2213556d3c63820f7a", EvidenceAdapters.sha256(bytes))
        val repo = PostMatchCollectionRepository(temporary.root)
        val engine = PostMatchDiscovery(repo, ZoneId.of("America/Chicago"))
        val result = engine.search(tracker.record, listOf(session, "Hearthstone_2026_10_14_12_38_17"), emptySet()) { name, _ ->
            if (name != session) error("Reader returned no Power.log descriptor")
            PostMatchLogSnapshot.read(name, bytes.size.toLong(), { bytes.inputStream() })
        }
        assertEquals(PairingStatus.PENDING, result.pairing.status)
        val reviewed = engine.reviewSavedCandidate(tracker.record, result, result.candidates.single().sourceId, emptySet())
        assertEquals(PairingStatus.ACCEPTED, reviewed.pairing.status)
        val imports = FusionImportRepository(temporary.root)
        val importer = FusionImporter(imports, ZoneId.of("America/Chicago"))
        val saved = reviewed.bundles.map { it.file.readBytes() }
        val imported = importer.import(tracker.record, saved, emptySet(), reviewed.pairing.acceptedSourceId)
        assertEquals(FusionImportState.FUSED_LOCAL, imported.entry.state)
        val artifact = FusionArtifactCodec.decode(imports.artifactForExport(tracker.record.id).readText())
        assertEquals(414, artifact.events.size)
        assertEquals(562, artifact.snapshots.size)
        assertTrue(FusionProvenanceValidator.validate(artifact).isEmpty())
        assertEquals(imported.entry, importer.import(tracker.record, saved, emptySet(), reviewed.pairing.acceptedSourceId).entry)
    }
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
