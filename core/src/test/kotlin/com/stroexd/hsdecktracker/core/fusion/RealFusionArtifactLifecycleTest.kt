package com.stroexd.hsdecktracker.core.fusion

import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RealFusionArtifactEncodeTest {
    private val fixtureDir = Path.of(checkNotNull(System.getProperty("hsFusionFixtureDir")))
    private val artifactPath = Path.of(checkNotNull(System.getProperty("hsFusionArtifactPath")))

    @Test
    fun `real artifact streams in an isolated producer lifecycle`() {
        val trackerBytes = Files.readAllBytes(fixtureDir.resolve(TRACKER_FIXTURE))
        val bundleBytes = Files.readAllBytes(fixtureDir.resolve(BUNDLE_FIXTURE))
        assertEquals(TRACKER_SHA256, EvidenceAdapters.sha256(trackerBytes))
        assertEquals(BUNDLE_SHA256, EvidenceAdapters.sha256(bundleBytes))

        val tracker = EvidenceAdapters.tracker(trackerBytes)
        val bundle = EvidenceAdapters.exporterBundle(bundleBytes)
        val reduced = PowerEvidenceReducer.reduce(bundle.matches.single().first, bundle.matches.single().second)
        val end = PowerClock.resolve(
            checkNotNull(reduced.source.sessionAlias),
            checkNotNull(reduced.endedLogTime),
            ZoneId.of("America/Chicago"),
        )
        val pairing = FusionPairing.decide(
            tracker,
            listOf(PowerPairingFacts.fromReduced(reduced, end, localController = 2)),
            trackerCardIds = setOf("CATA_190h"),
        )
        assertEquals(PairingStatus.ACCEPTED, pairing.status)
        val fused = FusionCoordinator.assemble(tracker, listOf(reduced), pairing)

        Files.createDirectories(artifactPath.parent)
        Files.newOutputStream(artifactPath).use { FusionArtifactCodec.encodeToStream(fused, it) }
        assertTrue(Files.size(artifactPath) > 0)
    }
}

class RealFusionArtifactDecodeTest {
    private val artifactPath = Path.of(checkNotNull(System.getProperty("hsFusionArtifactPath")))
    private val measurementPath = Path.of(checkNotNull(System.getProperty("hsFusionMeasurementPath")))

    @Test
    fun `real artifact decodes in an isolated consumer lifecycle`() {
        val heapPools = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }
        heapPools.forEach { it.resetPeakUsage() }
        val started = System.nanoTime()
        val decoded = Files.newInputStream(artifactPath).use(FusionArtifactCodec::decodeFromStream)
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000
        val heapUsedAfterDecode = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
        val materializeStarted = System.nanoTime()
        val materializedLast = FusionSnapshotMaterializer.materialize(decoded.snapshots).last()
        val materializeMillis = (System.nanoTime() - materializeStarted) / 1_000_000
        val heapUsedAfterMaterialize = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
        val peakHeapPoolBytes = heapPools.sumOf { it.peakUsage.used }
        val maxHeapBytes = Runtime.getRuntime().maxMemory()
        val artifactBytes = Files.size(artifactPath)

        assertEquals(FUSION_SCHEMA, decoded.schema)
        assertEquals(PairingStatus.ACCEPTED, decoded.pairing.status)
        assertEquals(2, decoded.sources.size)
        assertTrue(decoded.events.isNotEmpty())
        assertTrue(decoded.snapshots.isNotEmpty())
        assertTrue(decoded.snapshots.any { it.stateMode == SnapshotStateMode.DELTA })
        assertEquals(SnapshotStateMode.FULL, decoded.snapshots.last().stateMode)
        assertEquals(decoded.snapshots.last(), materializedLast)
        assertTrue(heapUsedAfterDecode in 1 until maxHeapBytes)
        assertTrue(heapUsedAfterMaterialize in 1 until maxHeapBytes)

        val report = Properties().apply {
            setProperty("artifactBytes", artifactBytes.toString())
            setProperty("decodeMillis", elapsedMillis.toString())
            setProperty("materializeMillis", materializeMillis.toString())
            setProperty("maxHeapBytes", maxHeapBytes.toString())
            setProperty("heapUsedAfterDecode", heapUsedAfterDecode.toString())
            setProperty("heapUsedAfterMaterialize", heapUsedAfterMaterialize.toString())
            setProperty("peakHeapPoolBytes", peakHeapPoolBytes.toString())
            setProperty("matchId", decoded.matchId)
            setProperty("events", decoded.events.size.toString())
            setProperty("snapshots", decoded.snapshots.size.toString())
            setProperty("entities", decoded.entities.size.toString())
        }
        Files.createDirectories(measurementPath.parent)
        Files.newOutputStream(measurementPath).use { report.store(it, "isolated real fused artifact decode") }
        println(
            "ISOLATED_FUSION_DECODE artifactBytes=$artifactBytes decodeMillis=$elapsedMillis " +
                "materializeMillis=$materializeMillis " +
                "heapUsedAfterDecode=$heapUsedAfterDecode heapUsedAfterMaterialize=$heapUsedAfterMaterialize " +
                "peakHeapPoolBytes=$peakHeapPoolBytes " +
                "maxHeapBytes=$maxHeapBytes",
        )
    }
}

private const val TRACKER_FIXTURE =
    "match_20261007T192626774Z_961f10263d57920408090f2fbccc7518a75e84983274cb123f0b5f41a4bf7cb0.json"
private const val BUNDLE_FIXTURE = "HS-export-20261007-142822-44f30f8a.zip"
private const val TRACKER_SHA256 = "d9b037917de5007d456b305850d3dd9e6ddaf919d59b8e2928d08de31057d574"
private const val BUNDLE_SHA256 = "6e89dacbe05f040a10ab5a73ac79e2edc31934d2c3e3fbf18d94a981ff038d4e"
