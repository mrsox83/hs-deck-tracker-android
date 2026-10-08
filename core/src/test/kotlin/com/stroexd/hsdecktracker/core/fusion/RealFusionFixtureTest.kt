package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchResult
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId

class RealFusionFixtureTest {
    private val fixtureDir = Path.of(checkNotNull(System.getProperty("hsFusionFixtureDir")))

    @Test
    fun `rafaam tracker and exporter pair reduce with exact evidence`() {
        val trackerPath = fixtureDir.resolve("match_20261007T192626774Z_961f10263d57920408090f2fbccc7518a75e84983274cb123f0b5f41a4bf7cb0.json")
        val bundlePath = fixtureDir.resolve("HS-export-20261007-142822-44f30f8a.zip")
        assertTrue(Files.isRegularFile(trackerPath), "Missing private tracker fixture")
        assertTrue(Files.isRegularFile(bundlePath), "Missing private exporter fixture")

        val trackerBytes = Files.readAllBytes(trackerPath)
        val bundleBytes = Files.readAllBytes(bundlePath)
        assertEquals("d9b037917de5007d456b305850d3dd9e6ddaf919d59b8e2928d08de31057d574", EvidenceAdapters.sha256(trackerBytes))
        assertEquals("6e89dacbe05f040a10ab5a73ac79e2edc31934d2c3e3fbf18d94a981ff038d4e", EvidenceAdapters.sha256(bundleBytes))

        val tracker = EvidenceAdapters.tracker(trackerBytes)
        val bundle = EvidenceAdapters.exporterBundle(bundleBytes)
        assertEquals("hs-export-bundle/0.4", bundle.schema)
        assertTrue(bundle.diagnostics.isEmpty(), bundle.diagnostics.joinToString())
        assertEquals(1, bundle.matches.size)
        val reduced = PowerEvidenceReducer.reduce(bundle.matches.single().first, bundle.matches.single().second)

        assertEquals(MatchResult.WIN, tracker.record.result)
        assertEquals(18, tracker.record.turns)
        assertEquals(73, tracker.record.timeline.size)
        assertTrue(reduced.completed)
        assertFalse(reduced.sourceTruncated)
        assertEquals(9, reduced.choices.size)
        assertEquals(22_601, bundle.matches.single().second.getValue("match").jsonObject.getValue("events").jsonArray.size)
        assertEquals("2c05f4bf27aca34e440ef4fc3a554759ffe40605f01beaa9bdf3193b52b38744", reduced.source.contentSha256.values.single { it != reduced.source.contentSha256.getValue("match-document") })

        val choice8 = reduced.choices.single { it.id.endsWith(":choice:8") }
        val choice9 = reduced.choices.single { it.id.endsWith(":choice:9") }
        assertTrue(choice8.submitted.entityRefs.single().contains("cardId=CATA_190t12"))
        assertTrue(choice8.confirmed.entityRefs.single().contains("cardId=CATA_190t12"))
        assertTrue(choice9.submitted.entityRefs.single().contains("cardId=CATA_190t13"))
        assertTrue(choice9.confirmed.entityRefs.single().contains("cardId=CATA_190t13"))
        assertTrue(choice8.offered.evidence.all { it.sourceLine != null })

        val masterDusk = reduced.events.single { event -> event.evidence.any { it.sourceLine == 38_189 } }
        val deathwing = reduced.events.single { event -> event.evidence.any { it.sourceLine == 45_191 } }
        assertEquals("PLAY", masterDusk.kind)
        assertEquals("TLC_513t", reduced.entities.getValue(checkNotNull(masterDusk.actorEntityId)).cardId.value)
        assertEquals(null, masterDusk.targetEntityId)
        assertEquals("PLAY", deathwing.kind)
        assertEquals("CATA_190h", reduced.entities.getValue(checkNotNull(deathwing.actorEntityId)).cardId.value)
        assertEquals(null, deathwing.targetEntityId)
        assertTrue(masterDusk.sourceSequence < deathwing.sourceSequence)

        val end = PowerClock.resolve(checkNotNull(reduced.source.sessionAlias), checkNotNull(reduced.endedLogTime), ZoneId.of("America/Chicago"))
        assertNotNull(end)
        val pairing = FusionPairing.decide(
            tracker,
            listOf(PowerPairingFacts.fromReduced(reduced, end, localController = 2)),
            trackerCardIds = setOf("CATA_190h"),
        )
        assertEquals(PairingStatus.ACCEPTED, pairing.status)
        assertTrue(pairing.candidates.single().evidence.all { it.sourceLine != null || it.jsonPointer != null })

        val fused = FusionCoordinator.assemble(tracker, listOf(reduced, reduced), pairing)
        assertTrue(FusionProvenanceValidator.validate(fused).isEmpty(), FusionProvenanceValidator.validate(fused).joinToString())
        assertEquals(2, fused.sources.size)
        assertTrue(fused.events.isNotEmpty())
        assertTrue(fused.events.all { it.evidence.isNotEmpty() })
        assertTrue(fused.entities.values.flatMap { it.tagHistory }.all { (it.evidence.sourceLine ?: 0) > 0 })
        assertTrue(reduced.snapshots.any { snapshot -> snapshot.counters.values.any { "RESOURCES_USED" in it } })
        assertTrue(reduced.snapshots.any { snapshot -> snapshot.counters.values.any { "HERALD_COLOSSAL_AMOUNT" in it } })
        assertTrue(reduced.entities.values.any { it.identityHistory.size > 1 })
        assertTrue(reduced.entities.values.any { it.zoneHistory.size > 1 })
        assertTrue(reduced.entities.values.any { it.controllerHistory.isNotEmpty() })
        assertTrue(reduced.entities.values.any { it.visibilityHistory.isNotEmpty() })
        assertTrue(reduced.entities.values.any { "HERO_ENTITY" in it.entityLinks })
        assertTrue(reduced.events.any { it.activeController != null && it.playerTurnIndex != null })
        val observedPlayer = reduced.snapshots.asSequence().flatMap { it.players.values.asSequence() }
            .firstOrNull { it.remainingHealth.value != null }
        assertNotNull(observedPlayer)
        assertEquals(ClaimStatus.DERIVED, observedPlayer.remainingHealth.status)
        assertEquals(2, observedPlayer.remainingHealth.evidence.size)
        val heraldPlayer = reduced.snapshots.asSequence().flatMap { it.players.values.asSequence() }
            .firstOrNull { it.heraldAmount.value != null }
        assertNotNull(heraldPlayer)
        assertEquals(ClaimStatus.OBSERVED, heraldPlayer.heraldAmount.status)
        assertEquals(null, heraldPlayer.heraldThresholdReached.value)

        val encoded = Files.createTempFile("hs-fused-rafaam-", ".json")
        try {
            Files.newOutputStream(encoded).use { FusionArtifactCodec.encodeToStream(fused, it) }
            assertTrue(Files.size(encoded) > 0)
        } finally {
            Files.deleteIfExists(encoded)
        }
    }

    @Test
    fun `legacy truncated session keeps two completed matches and partial suffix`() {
        val bundlePath = fixtureDir.resolve("HS-export-20261007-120905-b91f0ce1.zip")
        val bytes = Files.readAllBytes(bundlePath)
        assertEquals("9c731e4ce8c09a710d971cb0fa5b13dccee8cf12dc7c1fc6bfaec5e4e14fd71f", EvidenceAdapters.sha256(bytes))
        val bundle = EvidenceAdapters.exporterBundle(bytes)
        assertEquals("hs-export-bundle/0.3", bundle.schema)
        assertEquals(3, bundle.matches.size)
        assertTrue(bundle.diagnostics.single().contains("Source truncation marker observed"))
        val reduced = bundle.matches.map { PowerEvidenceReducer.reduce(it.first, it.second) }
        assertEquals(listOf(true, true, false), reduced.map { it.completed })
        assertEquals(listOf(false, false, true), reduced.map { it.sourceTruncated })
        assertTrue(reduced.take(2).all { it.events.isNotEmpty() })
        assertTrue(reduced.last().events.isNotEmpty())
        assertTrue(reduced.last().snapshots.isNotEmpty())
    }

    @Test
    fun `duplicate wrappers share inner source identity without becoming independent witnesses`() {
        val leftBytes = Files.readAllBytes(fixtureDir.resolve("HS-export-20261007-110747-2272cd33.zip"))
        val rightBytes = Files.readAllBytes(fixtureDir.resolve("HS-export-20261007-110749-cf440038.zip"))
        assertEquals("6f0f7e8d65f242cb7b57b45d1dec0e9423c89a09fc28dc44dae0c686a2749e10", EvidenceAdapters.sha256(leftBytes))
        assertEquals("3f5818836410ff3a63a7284962dd81425f668629bac095691461e594d68b2503", EvidenceAdapters.sha256(rightBytes))
        val left = EvidenceAdapters.exporterBundle(leftBytes)
        val right = EvidenceAdapters.exporterBundle(rightBytes)
        assertFalse(left.artifactSha256 == right.artifactSha256)
        assertEquals(left.matches.map { it.first.id }, right.matches.map { it.first.id })
        assertEquals(
            left.matches.flatMap { it.first.contentSha256.filterKeys { key -> key.endsWith("/Power.log") }.values },
            right.matches.flatMap { it.first.contentSha256.filterKeys { key -> key.endsWith("/Power.log") }.values },
        )
    }
}
