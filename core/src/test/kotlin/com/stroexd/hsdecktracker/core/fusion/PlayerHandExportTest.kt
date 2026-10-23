package com.stroexd.hsdecktracker.core.fusion

import java.util.concurrent.CancellationException
import kotlin.test.*

class PlayerHandExportTest {
    private fun fixture(): FusedMatch = FusedMatch(FUSION_SCHEMA, "unsafe/match", fusionVersion="offline-r2/4", sources=listOf(
        FusionSource("power", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod="test")),
        pairing=PairingDecision(PairingStatus.ACCEPTED, "power", listOf(PairingCandidate("power", 100, listOf("test"), evidence=listOf(EvidenceRef("power",1)))), "test"),
        events=emptyList(), entities=emptyMap(), choices=emptyList(), snapshots=listOf(
            FusionSnapshot("point/one", 1, "BEFORE_ACTION", entityTags=emptyMap(), evidence=listOf(EvidenceRef("power",10)))))
    @Test fun hashPinSafeFilenameAndOriginalBytesUnchanged() {
        val bytes=FusionArtifactCodec.encode(fixture()).toByteArray()
        val loaded=PlayerHandExport.load(bytes.inputStream())
        assertEquals(loaded,PlayerHandExport.load(bytes.inputStream(),maxBytes=bytes.size.toLong()))
        assertEquals(EvidenceAdapters.sha256(bytes), loaded.sha256)
        val report=PlayerHandExport.prepare(loaded, "point/one",2)
        assertTrue(report.filename.matches(Regex("[A-Za-z0-9_.-]+")))
        assertTrue(report.text.startsWith("Artifact SHA256: ${loaded.sha256}"))
        assertTrue(report.text.contains("Selected player controller: 2"))
        assertTrue(report.text.length<=65536)
        assertContentEquals(bytes,FusionArtifactCodec.encode(loaded.match).toByteArray())
    }
    @Test fun capCancellationAndInvalidSelectionsNeverProducePayload() {
        val bytes=FusionArtifactCodec.encode(fixture()).toByteArray()
        assertFails { PlayerHandExport.load(bytes.inputStream(), maxBytes=10) }
        var calls=0
        assertFailsWith<CancellationException> { PlayerHandExport.load(bytes.inputStream()) { if (++calls>1) throw CancellationException() } }
        val loaded=PlayerHandExport.load(bytes.inputStream())
        assertFails { PlayerHandExport.prepare(loaded,"absent",1) }
        assertFails { PlayerHandExport.prepare(loaded,"point/one",0) }
        assertFailsWith<CancellationException> { PlayerHandExport.prepare(loaded,"point/one",1) { throw CancellationException() } }
    }
    @Test fun duplicateCheckpointsCannotBeSelectedAndLabelsRetainEpoch() {
        val model=fixture()
        val loaded=PlayerHandExport.load(FusionArtifactCodec.encode(model).byteInputStream())
        assertTrue(PlayerHandExport.checkpoints(loaded).single().label.contains("Epoch 0"))
        val duplicate=loaded.copy(match=model.copy(snapshots=model.snapshots+model.snapshots))
        assertTrue(PlayerHandExport.checkpoints(duplicate).all { !it.unique })
        assertFails { PlayerHandExport.prepare(duplicate,"point/one",1) }
        assertEquals("x\\u000a\\u202e",PlayerHandExport.displayLabel("x\n\u202e"))
        assertTrue(PlayerHandExport.displayLabel("a".repeat(1000)).length<=120)
    }
}
