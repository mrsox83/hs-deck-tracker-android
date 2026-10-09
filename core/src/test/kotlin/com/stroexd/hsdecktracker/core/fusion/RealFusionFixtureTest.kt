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
    fun `all inventoried tracker records parse with pinned identities`() {
        val matchIds = linkedSetOf<String>()
        trackerFixtureHashes.forEach { (name, expectedHash) ->
            val bytes = Files.readAllBytes(fixtureDir.resolve(name))
            assertEquals(expectedHash, EvidenceAdapters.sha256(bytes), name)
            val tracker = EvidenceAdapters.tracker(bytes)
            assertEquals("tracker:$expectedHash", tracker.source.id, name)
            assertEquals(expectedHash, tracker.source.artifactSha256, name)
            assertEquals(tracker.record.id, tracker.source.matchAlias, name)
            assertTrue(matchIds.add(tracker.record.id), "Duplicate tracker match id ${tracker.record.id}")
        }
        assertEquals(32, matchIds.size)
    }

    @Test
    fun `all inventoried exporter bundles reduce with valid compact snapshot streams`() {
        var matchCount = 0
        var eventCount = 0
        var snapshotCount = 0
        var completedCount = 0
        var truncatedCount = 0
        val schemas = linkedMapOf<String, Int>()
        val diagnosticCounts = linkedMapOf<String, Int>()
        val diagnosticContexts = mutableListOf<String>()
        exporterFixtureHashes.forEach { (name, expectedHash) ->
            val bytes = Files.readAllBytes(fixtureDir.resolve(name))
            assertEquals(expectedHash, EvidenceAdapters.sha256(bytes), name)
            val bundle = EvidenceAdapters.exporterBundle(bytes)
            assertEquals(expectedHash, bundle.artifactSha256, name)
            assertTrue(bundle.matches.isNotEmpty(), "$name has no match evidence")
            schemas[bundle.schema ?: "missing"] = schemas.getOrDefault(bundle.schema ?: "missing", 0) + 1
            bundle.matches.forEach { (source, document) ->
                val reduced = PowerEvidenceReducer.reduce(source, document)
                matchCount++
                eventCount += reduced.events.size
                snapshotCount += reduced.snapshots.size
                if (reduced.completed) completedCount++
                if (reduced.sourceTruncated) truncatedCount++
                reduced.diagnostics.forEach { diagnostic ->
                    val category = diagnostic
                        .replace(Regex("at source line [0-9]+"), "at source line <n>")
                        .replace(Regex("^[0-9]+ unterminated"), "<n> unterminated")
                    diagnosticCounts[category] = diagnosticCounts.getOrDefault(category, 0) + 1
                    diagnosticContexts += "$name#${source.matchAlias} completed=${reduced.completed} " +
                        "truncated=${reduced.sourceTruncated}: $diagnostic"
                }
                assertTrue(reduced.events.all { event ->
                    event.evidence.isNotEmpty() && event.evidence.all { it.sourceId == source.id && it.sourceLine != null }
                }, "$name match ${source.matchAlias} has an event without exact source evidence")
                if (reduced.snapshots.isNotEmpty()) {
                    assertEquals(SnapshotStateMode.FULL, reduced.snapshots.first().stateMode, "$name first snapshot")
                    assertEquals(SnapshotStateMode.FULL, reduced.snapshots.last().stateMode, "$name last snapshot")
                    assertEquals(
                        reduced.snapshots.last(),
                        FusionSnapshotMaterializer.materialize(reduced.snapshots).last(),
                        "$name final materialized snapshot",
                    )
                }
            }
        }
        assertEquals(48, matchCount)
        assertEquals(43, completedCount)
        assertEquals(4, truncatedCount)
        assertEquals(21_018, eventCount)
        assertEquals(29_674, snapshotCount)
        assertEquals(
            linkedMapOf("hs-export-bundle/0.2" to 4, "hs-export-bundle/0.3" to 2, "hs-export-bundle/0.4" to 15),
            schemas,
        )
        assertEquals(
            linkedMapOf(
                "Unmatched BLOCK_END at source line <n>" to 6,
                "<n> unterminated block(s); final snapshot is unresolved" to 1,
            ),
            diagnosticCounts,
        )
        println(
            "REAL_FUSION_CORPUS bundles=${exporterFixtureHashes.size} matches=$matchCount " +
                "completed=$completedCount truncated=$truncatedCount events=$eventCount snapshots=$snapshotCount " +
                "schemas=$schemas diagnostics=$diagnosticCounts diagnosticContexts=$diagnosticContexts",
        )
    }

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
        val observedPlayer = FusionSnapshotMaterializer.materialize(reduced.snapshots)
            .flatMap { it.players.values.asSequence() }
            .firstOrNull { it.remainingHealth.value != null }
        assertNotNull(observedPlayer)
        assertEquals(ClaimStatus.DERIVED, observedPlayer.remainingHealth.status)
        assertEquals(2, observedPlayer.remainingHealth.evidence.size)
        val heraldPlayer = FusionSnapshotMaterializer.materialize(reduced.snapshots)
            .flatMap { it.players.values.asSequence() }
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

    @Test
    fun `reconnect sessions stitch one match with both source provenances`() {
        val trackerBytes = Files.readAllBytes(fixtureDir.resolve(
            "match_20261008T041335265Z_bb7a20f1d4ac2bb0787aceb75997fd0643cdece6d795dd3bfcfa661e75018739.json",
        ))
        val tracker = EvidenceAdapters.tracker(trackerBytes)
        assertEquals(26, tracker.record.turns)
        assertEquals(MatchResult.LOSS, tracker.record.result)

        val bundle = EvidenceAdapters.exporterBundle(
            Files.readAllBytes(fixtureDir.resolve("HS-export-20261007-231433-ae674730.zip")),
        )
        val segments = bundle.matches.associate { (source, document) ->
            source.sessionAlias to PowerEvidenceReducer.reduce(source, document)
        }
        val earlier = segments.getValue("Hearthstone_2026_10_07_22_44_20")
        val later = segments.getValue("Hearthstone_2026_10_07_23_04_47")
        val decision = PowerContinuationStitcher.evaluate(
            earlier, later, tracker, ZoneId.of("America/Chicago"),
        )

        assertEquals(ContinuationStatus.ACCEPTED, decision.status, decision.reasons.joinToString())
        assertTrue(decision.evidence.all { it.sourceLine != null || it.jsonPointer != null })
        assertTrue(decision.evidence.any { it.sourceId == tracker.source.id && it.jsonPointer == "/timestamp" })
        val stitched = PowerContinuationStitcher.stitch(earlier, later, decision)
        assertEquals(listOf(earlier.source.id, later.source.id), stitched.source.parentSourceIds)
        assertTrue(stitched.completed)
        assertFalse(stitched.sourceTruncated)
        assertEquals(earlier.events.size + later.events.size, stitched.events.size)
        assertEquals(1, stitched.choices.count { it.id.endsWith(":choice:16") })
        val deathwingChoice = stitched.choices.single { it.id.endsWith(":choice:16") }
        assertTrue(deathwingChoice.offered.entityRefs.any { it.contains("CATA_190t12") })
        assertTrue(deathwingChoice.confirmed.entityRefs.single().contains("CATA_190t13"))
        assertTrue(deathwingChoice.offered.evidence.any { it.sourceId == earlier.source.id })
        assertTrue(deathwingChoice.confirmed.evidence.any { it.sourceId == later.source.id })
        assertTrue(stitched.diagnostics.last().contains("source-session gap retained"))

        val end = PowerClock.resolve(checkNotNull(later.source.sessionAlias), checkNotNull(later.endedLogTime), ZoneId.of("America/Chicago"))
        val pairing = FusionPairing.decide(
            tracker,
            listOf(PowerPairingFacts.fromReduced(stitched, end, localController = 1)),
            trackerCardIds = setOf("CATA_190h"),
        )
        assertEquals(PairingStatus.ACCEPTED, pairing.status)
        val fused = FusionCoordinator.assemble(tracker, listOf(earlier, later, stitched), pairing)
        assertTrue(FusionProvenanceValidator.validate(fused).isEmpty(), FusionProvenanceValidator.validate(fused).joinToString())
        assertEquals(4, fused.sources.size)
        assertEquals(stitched.events.size, fused.events.size)
    }
}

private val exporterFixtureHashes = linkedMapOf(
    "HS-export-20261007-110747-2272cd33.zip" to "6f0f7e8d65f242cb7b57b45d1dec0e9423c89a09fc28dc44dae0c686a2749e10",
    "HS-export-20261007-110749-cf440038.zip" to "3f5818836410ff3a63a7284962dd81425f668629bac095691461e594d68b2503",
    "HS-export-20261007-111036-b7a67ce3.zip" to "7dd993a47d623f9654b25b331c5ece2815654003dcc47c9ea0e32878bf96363b",
    "HS-export-20261007-113129-d6fcdf3a.zip" to "a14a4ce49d54cabb9f93ceb4a9abb9acca867916c45b93faef4d8a77621b8552",
    "HS-export-20261007-114457-2d4a92bd.zip" to "5b68bded1a2ae1ca5bc045dc5ccc0a43095378dc3e990e813cd1041dfee9e6db",
    "HS-export-20261007-120905-b91f0ce1.zip" to "9c731e4ce8c09a710d971cb0fa5b13dccee8cf12dc7c1fc6bfaec5e4e14fd71f",
    "HS-export-20261007-135643-ba507a42.zip" to "525adb245802a072e0da5bd88ae59cd85bc9b7a0fd278c21e565c13f8257921a",
    "HS-export-20261007-142822-44f30f8a.zip" to "6e89dacbe05f040a10ab5a73ac79e2edc31934d2c3e3fbf18d94a981ff038d4e",
    "HS-export-20261007-174816-10dc47e2.zip" to "49c306cd1f68fc6cba60a7d52e62116d52081d340e1606c8c71ee07655748aac",
    "HS-export-20261007-174833-2fd24e42.zip" to "4f947182a79542a662709ba484819957a065dc4b65d57c2f20e8c09496354da8",
    "HS-export-20261007-183243-2da66cfb.zip" to "ba003ad570ad9f672a9b9984db7581ade4da76846df26a12299ccb80b49954f4",
    "HS-export-20261007-204322-0bdd9eb6.zip" to "d11825e39427121bd2eb3a651ece6ea211621004bd7c31bd3962cde7ac6ecbd4",
    "HS-export-20261007-222956-57ee2873.zip" to "291fec781cd4a22261af96e4711bd9c5314a9d6404c705b132d24ab19534f5aa",
    "HS-export-20261007-224120-4adcf77b.zip" to "ce963db2de453506d120a42ca095783ede4c93d51ff35211e5a3838bfe18da8a",
    "HS-export-20261007-231422-ce33574b.zip" to "508d495b5f7a81261ec5354cb4e990cf475ef5ea41dd035fc969c343dfb3b233",
    "HS-export-20261007-231433-ae674730.zip" to "8aeea469a983594d281a1a740728413d87e1db198941a8ca315dcaf6e08447c6",
    "HS-export-20261007-231517-45ff02a6.zip" to "815030d4bf0a355532d26a6d0588e881059caa410ee7fd35ca7776086548d964",
    "HS-export-20261008-161122-04f747f4.zip" to "3034236b5d61e1f75d0ace581710952cbc1c969ed9c9fec25ec4a2f6b649e92e",
    "HS-export-20261008-180502-9e83a13b.zip" to "97085f456df4716f36df221a8a03ba541650c1b82cde52e39443ecf6edc8b019",
    "HS-export-20261008-180748-5b19e208.zip" to "9d483acda28f2a03c1cf9d83d1e066a98223e9e156b59a6b340ef49734bcf1a5",
    "HS-export-20261008-214308-8e4ba0e9.zip" to "dfdedc88b5f2dfabe83305509b909de17d75a0f7dba6d68b5cb37517e32a82a4",
)

private val trackerFixtureHashes = linkedMapOf(
    "match_20261006T010602189Z_170c29ebf6a1ac9810decb7d06d5889b5bd7826566b9f6b337ad222ef900123d.json" to "7133609191cfbd2eee5b585af8385b3767d64359f0b1eb9126cdf18dadcf3193",
    "match_20261006T033605754Z_8a02e0efe1b31a6c7490fea3132a0273f0001d74f6544c3d273b365c5453d9c0.json" to "c820373c1f820e721b0014210606a2c02df36f4a1c049807c8042e9234e40114",
    "match_20261006T041437900Z_b5ed9842ae75e91280dcb61783f13d03cc9aec1301983f29db0e4ea6b755a852.json" to "f48217845237d7f248f07a5a853394d78519359d13c043e0fd4fa498009a5898",
    "match_20261006T043702883Z_5cca0df957f1a390b1961d9def574c4053daf1cb6d133ab08cbd084123aa896d.json" to "84224a5208671cc62792e48692367a2555cef39a7a8ba3d81718ebf1595d5328",
    "match_20261006T181137976Z_1a6f055fc130c3a1059b3fd2fc95a1daa8da56e907a98a20389874d759473a70.json" to "efc9a16bf5e2c87dab884ac09af835ffc75db088a34dff1a5fc0fbb7686073b0",
    "match_20261006T182817758Z_68796488b30b5a4b0978a2e60e9ee27740b4bced5589e2abb24625f32566ad89.json" to "fdcefff38debf28afa21bbf53962870d1b8d8e12a32d5cbe2f939743ef6068b6",
    "match_20261006T200436174Z_fcddd38bfcdab1b7eefe7e23a3202029abd743542924dda619ce6aa8fca4619a.json" to "3de76453ed7172006fd43a436d159e6f57334cb1ab5c090b48eab4c2c1fd51da",
    "match_20261007T000354423Z_19209fe55c0163c39c5446ec9b183c40f5959b13aaf71c98e42e1d291267cbe9.json" to "715e944b18f57cf005e3565a5454be24ab19ce08f1759515a2afcd67e87bf1e4",
    "match_20261007T002453746Z_ad8a7bc6510136ac4aab8737d3d9cbc54b518bcc19a9cf83ab9fcd40b8cfa1d3.json" to "9d79f7cc5cab632c41daf59d13f9f556791e0c1d1109ff5f10900e37ea706574",
    "match_20261007T044712279Z_d4f7c07b22287489a2e259104cdaf778b6686ef0e2e31ccd51d1408d7900b3a9.json" to "d0e2c4eb52b2305ccfe11c5de555e2a871e067e4e99ff62860d32d296d84ed86",
    "match_20261007T051338743Z_08fc47e156e2d41663cc4d76b732b85d5260971599c7c2e8255f0dafd48fc788.json" to "457153853a80b4c14efed2dc4a301aca8fd86bcb55ad241db9653f598599d0c0",
    "match_20261007T054630690Z_54f494a9957616abe1f6ab1b6807bc2d553fab95204ca1743dbe50470770021b.json" to "785b455dedeeb2bed273d5d73c575b71434b2306926bf2a61a858778f8e972ee",
    "match_20261007T063917393Z_fb638c67673fe9f04755c64d2fe9493c3d5ae27cdb118960b416caca68440b72.json" to "fd86a704518d1bbcac2a5564b7760c84f34cfd968e6a047a97ef4db72823fbe6",
    "match_20261007T065558653Z_979539251cb23479b3bf3c06b9ad540ad6dcbc9d263f0950e591b5a18fdd22c7.json" to "eab6447658c64a17867ead81963b13a74cb3893524ae3aac057fb0d1056fc718",
    "match_20261007T073619776Z_7a2bf748fe700d2a3e2278ae4e9c6ed423b1f0f05af792e8190fd45eb60c9462.json" to "f106c1a03dbd81cc3ef955ee3e60c8a47900ee7ade12fae64c479c9d130778d0",
    "match_20261007T154348064Z_944cf25b5fb9cb700e2122b6e1eca82f4b7923e6dae1fbe37b25b4e49b934b15.json" to "9e314581ee4fccc40cc79be4817ee9854ba9c12b6ba6c363c4b75854b362075e",
    "match_20261007T170706547Z_e5732e521d4bd2445cd204542fc4783459659087cba9da5ab8f453ccee6f2514.json" to "063274ea05271dc85c710cfef146bd3b226acc97b89ce4112018f420847848bd",
    "match_20261007T192626774Z_961f10263d57920408090f2fbccc7518a75e84983274cb123f0b5f41a4bf7cb0.json" to "d9b037917de5007d456b305850d3dd9e6ddaf919d59b8e2928d08de31057d574",
    "match_20261007T224522751Z_29f123dad3c94ddce12192af0bc09665e0238c0a42de12dc3a76fe0749a67367.json" to "dc9e19397bce99fbf19beb8793ef70d6ec25ddb55e31fba6e243004306ecbdca",
    "match_20261008T013925553Z_97cb53de15b7eb9fb730c14306f8e8a251bdd0a87843bebf5388d4decfd0809e.json" to "9e2cd97b60be9be4ff10f260ba75b1efe9ac9da5a263191a7fa2ea344292234d",
    "match_20261008T041335265Z_bb7a20f1d4ac2bb0787aceb75997fd0643cdece6d795dd3bfcfa661e75018739.json" to "ecc6ed12c6fa02adaf76ec49904d4139f35825f9341e0eae0480eaae60f279a2",
    "match_20261008T210742979Z_2d002b13fdc7b8819a62d26100dc81bf7e53e3b83d9d9066915564dca160dcc0.json" to "57c9e135926c2811cae9cbd83bc5a18f71b1bc13eb9c8ca4979f5c1c7b4a771f",
    "match_20261008T235129438Z_5f9105e3c1b934205fa1c2788fe65fcd2f9ba622cd7ea1bc5aa8dd5c9a9db285.json" to "70020e5f37124b4fec15ffe6ee519d815a6be3f730eaf2ce5d722f0d3596aa9c",
    "match_20261009T000129951Z_922d6a827a070ddf781137d2067181f11437edadfc0d7d68b48683e4a79838b5.json" to "d749d060e888c5ea026c6da40d4f5b5d4cdff5714feec16e4c905c8e76d36e0c",
    "match_20261009T002447267Z_151ef5ec65c9e2539027e87acc244611eb281ec3134857a61de34879c74889f9.json" to "a2c1514ecfdf8d321f4de06074809a0332ec281f6fd85aab4f5f8eb62476600c",
    "match_20261009T004244131Z_7c07a9488f388e3ff9ea435328fcf448f98612bb214683385aad49c03726cadf.json" to "11fe972fca12004226e5218a09a5cd9ff961f045955f3a3a0d7d077fc6fccb9d",
    "match_20261009T005449590Z_31a8e7c5a64c2558e7988a674de1a9d932206c2accfe4d28425b73db39729188.json" to "4ea291f0ee609324141dafe9b1235c23cc73a978b74349944d4e6f165d4ba225",
    "match_20261009T010301103Z_58b614b7e3b4ffaefc09787ed947cf8d4c461c214cf89c937d2c4e62cbee8f6c.json" to "35b6ffc1f5a7cc58b65275a41909802575351e0b1708fae6037b68b769d0f2b2",
    "match_20261009T030231557Z_f141004466b38284d926fc8ba06e4c65f45c7c3d0f7ed332e9df0668e836ab44.json" to "624ea15ac21c1f2eb4533123fe988079c4c8b2c9f37b254b2600badeb2928e92",
    "match_20261009T040708691Z_a8d928b9332ad7e73e3fe1d62655775e3c8bb61ae9aef0115250d930ea76e9b8.json" to "7ed151bc18c3d6c6ce022c5eb2d285e012b14f4d5234d5a373dd45d268ee4c84",
    "match_20261009T041703315Z_9856cbe1c4718007ed449efa1b95678b01b5dbb4759baa9a5392627b25ba5fcc.json" to "8f65acf25a31309a76577925c39c150aaca4bf0f1c44bd21c2015eee52faeb17",
    "match_20261009T065742999Z_bdeb272bbede79d84308d0c897e9b7b6a1cae8d2c1fbe2312448e2b0e1532639.json" to "e9e1de8a03ca6ae29f63de45a5908a6a1cab02c05c176e4f140b6b7d45914423",
)
