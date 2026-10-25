package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchResult
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId

class RealFusionFixtureTest {
    private val fixtureDir = Path.of(checkNotNull(System.getProperty("hsFusionFixtureDir")))

    @Test fun restoredPlayerBlocksHaveExactLateBindingWitnessesWithoutBackdating() {
        val bytes=Files.readAllBytes(fixtureDir.resolve("HS-game-20261018-165407-e3bc4eb7-logs.zip"))
        assertEquals("9392b19815be64f0e053b1edc50933d5bd06749a311369ad0e09da4b21bd8d69",EvidenceAdapters.sha256(bytes))
        val (source,document)=EvidenceAdapters.exporterBundle(bytes).matches.single()
        val r=PowerEvidenceReducer.reduce(source,document)
        val raw=document.getValue("match").jsonObject.getValue("events").jsonArray.map { it.jsonObject }
            .associateBy { it.getValue("source_line").jsonPrimitive.content.toInt() }
        val observations=r.entityEpochs.flatMap { epoch -> epoch.entities.values.flatMap { e ->
            e.tagHistory.filter { it.evidence.isPlayerRestoration }.map { Triple(epoch.chronologyEpoch,e.id,it) }
        } }
        assertEquals(listOf(3575,3614,8248,8289,18972,19021,29123,29175,37620,37671),
            observations.map { it.third.evidence.supportingEvidence.single { p -> p.role=="header" }.sourceLine }.distinct().sorted())
        assertEquals(10,observations.map { it.first to it.second }.distinct().size)
        for ((epoch,id,o) in observations) {
            val proof=o.evidence.supportingEvidence
            val origin=proof.single { it.role=="origin" }.sourceLine
            val identity=proof.single { it.role=="identity" }.sourceLine
            assertEquals("tag=${o.name} value=${o.value}",raw.getValue(origin).getValue("raw").jsonPrimitive.content)
            assertEquals("tag=ENTITY_ID value=$id",raw.getValue(identity).getValue("raw").jsonPrimitive.content)
            assertTrue(o.sourceLine>origin && o.sourceLine>identity)
            assertFalse(raw.getValue(o.sourceLine).getValue("raw").jsonPrimitive.content.startsWith("tag="))
            assertTrue(r.snapshots.filter { it.chronologyEpoch==epoch && it.evidence.single().sourceLine!!<o.sourceLine }
                .all { !it.entityTags.get(id).orEmpty().containsKey(o.name) })
        }
        assertTrue(r.snapshots.filter { it.chronologyEpoch>0 }.all { it.playerTurnIndices.isEmpty() })
        val boundary=r.snapshots.single { it.phase=="BEFORE_ACTION" && it.evidence.single().sourceLine==34937 }
        // Named signal after restoration still lacks an exact matching alias; do not guess.
        val headers=listOf(29123,29175).map { line -> Regex("FULL_ENTITY - Updating (.*) CardID=")
            .matchEntire(raw.getValue(line).getValue("raw").jsonPrimitive.content)!!.groupValues[1] }
        assertFalse(raw.getValue(34561).getValue("entity").jsonPrimitive.content in headers)
        assertNull(boundary.activeController)
        assertNull(boundary.rawTurn) // No fresh explicit GameEntity binding in this epoch.
    }

    @Test fun deliveredSlash6PhoneArtifactRemainsDecodable() {
        val bytes=Files.readAllBytes(fixtureDir.resolve("fusion-test-dd47b4e9-38d7-4cd8-aa8d-b762eecb372c-9df365b54c0f15cd29d1abfdd94da644.json"))
        assertEquals("b036a2207cc6cc34d418a804210d4a25266eace7b1dad1145db8664821e8224d",EvidenceAdapters.sha256(bytes))
        val f=FusionArtifactCodec.decode(bytes.toString(Charsets.UTF_8))
        assertEquals("android-r3/6",f.fusionVersion)
        assertEquals(302,f.events.size)
        assertEquals(494,f.handHistory?.checkpoints?.size)
    }

    @Test fun currentPhoneGlobalTurnIs18WhilePersonalTurnIs9() {
        val bytes = Files.readAllBytes(fixtureDir.resolve("HS-game-20261023-154050-7bad9bbc-logs.zip"))
        assertEquals("611e3d1439ad57e62d9dc2ad85f5480d2078a5f4aafd3c3738eca266b2d1b210", EvidenceAdapters.sha256(bytes))
        val (source, document) = EvidenceAdapters.exporterBundle(bytes).matches.single { it.first.matchAlias == "3" }
        val reduced = PowerEvidenceReducer.reduce(source, document)
        val boundary = reduced.snapshots.single { it.phase == "BEFORE_ACTION" && it.evidence.single().sourceLine == 66081 }
        assertEquals(18, boundary.rawTurn)
        assertEquals(1, boundary.activeController)
        assertEquals("9", reduced.entities.getValue("2").tagHistory.last { it.name == "TURN" && it.sourceLine <= 66081 }.value)
        assertEquals(listOf(48385,49950,51127,52357,54048,56647,59598,63213,67520,71556),
            reduced.diagnostics.filter { it.startsWith("Turn owner unresolved") }.map { Regex("source line (\\d+)").find(it)!!.groupValues[1].toInt() })
        val hands = DecisionHandProjector.all(FusionCoordinator.assemble(
            TrackerEvidence(FusionSource("tracker:test", "test", SourceType.ANDROID_TRACKER_JSON, captureMethod = "test"),
                com.stroexd.hsdecktracker.core.stats.MatchRecord(id = "current-turn-test", timestamp = 1, result = MatchResult.WIN)),
            listOf(reduced), PairingDecision(PairingStatus.ACCEPTED, source.id, emptyList(), "Pinned raw fixture")))
        val hand = hands.single { it.first.phase == "BEFORE_ACTION" && it.first.evidence.single().sourceLine == 66081 }.second
        assertEquals(listOf("32","5","6","33","184","194","9"), hand.hands.single { it.controller == 1 }.slots.map { it.entityId })
    }

    @Test fun existingObserverAssociationsRemainCandidatesAndDoNotCrossReset() {
        val cases = listOf(
            Triple("HS-export-20261007-142822-44f30f8a.zip", "6e89dacbe05f040a10ab5a73ac79e2edc31934d2c3e3fbf18d94a981ff038d4e", 2),
            Triple("HS-export-20261008-161122-04f747f4.zip", "3034236b5d61e1f75d0ace581710952cbc1c969ed9c9fec25ec4a2f6b649e92e", 1),
        )
        for ((name, hash, owner) in cases) {
            val bytes = Files.readAllBytes(fixtureDir.resolve(name))
            assertEquals(hash, EvidenceAdapters.sha256(bytes))
            val (source, document) = EvidenceAdapters.exporterBundle(bytes).matches.single()
            val cutoff = document.getValue("match").jsonObject.getValue("events").jsonArray.last().jsonObject.getValue("source_line").toString().toInt()
            val candidate = ObserverAssociationResolver.resolve(source, document, 0, cutoff)
            assertEquals(ObserverAssociationStatus.CANDIDATE, candidate.status, candidate.diagnostics.toString())
            assertEquals(owner, candidate.candidateController)
            assertTrue(candidate.evidence.all { it.sourceId == source.id && it.sourceLine in 1..cutoff })
            assertTrue(candidate.diagnostics.contains("CANDIDATE_IS_NOT_VERIFIED_OBSERVER_KNOWLEDGE"))
            val beforeSubmit = if (owner == 2) 1070 else 1518
            assertEquals(ObserverAssociationStatus.UNKNOWN, ObserverAssociationResolver.resolve(source, document, 0, beforeSubmit).status)
        }
        val bytes = Files.readAllBytes(fixtureDir.resolve("HS-game-20261018-165407-e3bc4eb7-logs.zip"))
        assertEquals("9392b19815be64f0e053b1edc50933d5bd06749a311369ad0e09da4b21bd8d69", EvidenceAdapters.sha256(bytes))
        val (source, document) = EvidenceAdapters.exporterBundle(bytes).matches.single()
        assertEquals(2, ObserverAssociationResolver.resolve(source, document, 0, 1775).candidateController)
        for ((epoch, line) in listOf(1 to 6320, 4 to 34937, 4 to 36577)) {
            val value = ObserverAssociationResolver.resolve(source, document, epoch, line)
            assertEquals(ObserverAssociationStatus.UNKNOWN, value.status)
            assertEquals(null, value.candidateController)
        }
        println("OBSERVER_ASSOCIATION candidatesOnly=true firstAndSecond=true resetsAndFutureEvidenceUnknown=true")
    }

    @Test fun ratWasteHandOrderProjectionKeepsBothSidesAndWithholdsPrivateIdentity() {
        val started = System.nanoTime()
        val bytes = Files.readAllBytes(fixtureDir.resolve("HS-game-20261018-165407-e3bc4eb7-logs.zip"))
        assertEquals("9392b19815be64f0e053b1edc50933d5bd06749a311369ad0e09da4b21bd8d69", EvidenceAdapters.sha256(bytes))
        val trackerBytes = Files.readAllBytes(fixtureDir.resolve("match_20261018T215407114Z_47cbbf799b54c32d7881fb104804d8e59847cf81da4b1d31115edccead9c809d.json"))
        assertEquals("0540889ea13d924e1be148cc1e4a41029c466c1a31ac4823dc3328e3268e7004", EvidenceAdapters.sha256(trackerBytes))
        val tracker = EvidenceAdapters.tracker(trackerBytes)
        val bundle = EvidenceAdapters.exporterBundle(bytes)
        val powers = bundle.matches.map { PowerEvidenceReducer.reduce(it.first, it.second) }
        assertTrue(powers.single().source.contentSha256.values.contains("7729f57c8893047fcdb360892323b28031523d52bdf259f714e28730cfef6f0a"))
        val evaluation = FusionEvaluator.evaluate(tracker, powers, ZoneId.of("America/Chicago"), tracker.record.timeline.mapNotNull { it.cardId }.toSet())
        val pairing = evaluation.manualDecision()
        assertEquals(PairingStatus.ACCEPTED, pairing.status)
        val fused = FusionCoordinator.assemble(tracker, evaluation.powers, pairing)
        val checkpoint = fused.snapshots.single { it.phase == "BEFORE_ACTION" && it.evidence.any { ref -> ref.sourceLine == 34937 } }
        val view = DecisionHandProjector.project(fused, checkpoint.id)
        assertEquals(listOf(1, 2), view.hands.map { it.controller })
        assertEquals(listOf("38", "53", "44", "126", "48", "46", "234", "40"), view.hands.single { it.controller == 2 }.slots.map { it.entityId })
        val rawByLine = bundle.matches.single().second.getValue("match").jsonObject.getValue("events").jsonArray
            .associateBy { it.jsonObject.getValue("source_line").toString().toInt() }
        val archive = fused.entityEpochs.single { it.chronologyEpoch == checkpoint.chronologyEpoch }
        for (slot in view.hands.single { it.controller == 2 }.slots.take(7)) {
            val observations = archive.entities.getValue(slot.entityId).tagHistory.filter {
                it.evidence in slot.evidence && it.name in setOf("ZONE", "ZONE_POSITION", "CONTROLLER")
            }
            assertEquals(setOf("ZONE", "ZONE_POSITION", "CONTROLLER"), observations.map { it.name }.toSet())
            for (observation in observations) {
                assertTrue(observation.sourceLine <= 34937)
                val raw = rawByLine.getValue(observation.sourceLine).jsonObject.getValue("raw").jsonPrimitive.content
                if (observation.evidence.rule == "power-descriptor-position/1") {
                    assertTrue(raw.contains(" id=${slot.entityId} zone=HAND zonePos=${slot.position} "))
                    assertTrue(raw.contains(" player=2]"))
                } else if (raw.startsWith("TAG_CHANGE")) {
                    assertTrue(raw.contains(" id=${slot.entityId} "))
                    assertTrue(raw.trimEnd().endsWith("tag=${observation.name} value=${observation.value}"))
                } else {
                    assertEquals("tag=${observation.name} value=${observation.value}", raw)
                    val header = rawByLine.values.map { it.jsonObject }.last { event ->
                        event.getValue("source_line").toString().toInt() < observation.sourceLine &&
                            event.getValue("raw").jsonPrimitive.content.let { text ->
                                text.startsWith("FULL_ENTITY") || text.startsWith("SHOW_ENTITY") || text.startsWith("CHANGE_ENTITY") ||
                                    text.startsWith("Player EntityID=") || text.startsWith("GameEntity EntityID=") ||
                                    text.startsWith("BLOCK_START BlockType=GAME_RESET") || text == "RESET_GAME"
                            }
                    }.getValue("raw").jsonPrimitive.content
                    assertTrue(header.contains(" id=${slot.entityId} ") || header.contains("Creating ID=${slot.entityId} "))
                }
                assertEquals(mapOf("ZONE" to "HAND", "ZONE_POSITION" to "${slot.position}", "CONTROLLER" to "2")[observation.name], observation.value)
            }
            assertEquals(observations.any { it.evidence.rule == "power-descriptor-position/1" },
                view.hands.single { it.controller == 2 }.diagnostics.contains("HAND_SLOT_SUPPLEMENTAL_DESCRIPTOR:${slot.entityId}"))
        }
        // The old ghost-warning expectation was never reached while the eight-slot assertion failed.
        // Raw line34576 directly places entity67 in PLAY, not in either hand.
        assertFalse(view.hands.flatMap { it.slots }.any { it.entityId == "67" })
        val materialized = FusionSnapshotMaterializer.materialize(fused.snapshots.takeWhile { it.id != checkpoint.id } + checkpoint).last()
        assertEquals("PLAY", materialized.entityTags.getValue("67")["ZONE"])
        assertTrue(rawByLine.getValue(34576).jsonObject.getValue("raw").jsonPrimitive.content.contains(" id=67 zone=PLAY zonePos=0 "))
        assertTrue(view.hands.flatMap { it.slots }.all { it.cardId == null && it.effectiveCost == null })
        println("HAND_VIEW exactOrder=true bothControllers=true observerKnowledgeUnknown=true elapsedMillis=${(System.nanoTime() - started) / 1_000_000}")
    }

    @Test fun growingLogPrefixIdentityCharacterization() {
        fun bytes(name: String, hash: String): ByteArray = Files.readAllBytes(fixtureDir.resolve(name)).also {
            assertEquals(hash, EvidenceAdapters.sha256(it))
        }
        val earlyBytes = bytes("HS-game-20261018-225042-27dda0c7-logs.zip", "68a83dd54506a0bffeb5a0da995fec262d09944ce9ad8069b3b56cad2e03b451")
        val lateBytes = bytes("HS-game-20261018-230613-90920e7a-logs.zip", "f634d5ff447b73a3b71bc14065ed91663ddd801d032132a65257b94580ba1df1")
        val dk = EvidenceAdapters.tracker(bytes("match_20261019T035042027Z_c8a04179877cd4067982473cce92da34072a987446d6bb1aa5e4bd150cb43e3c.json", "46d22ade4924283bc0f45b612d2183e0556997ceb817506a2d391efe96448a31"))
        val priest = EvidenceAdapters.tracker(bytes("match_20261019T040613874Z_dfcbcbd8a6861693d9cd7072fe5dedb77c13caa5ed213bc0f3c77f60fbe1d406.json", "a320a53c4951fd0f5a243142e88e4a3a2968af26fc3f0ff0f58f2b8e3d13694d"))
        val early = EvidenceAdapters.exporterBundle(earlyBytes)
        val late = EvidenceAdapters.exporterBundle(lateBytes)
        assertEquals(1, early.matches.size)
        assertEquals(2, late.matches.size)
        assertEquals(early.matches.single().second.getValue("match"), late.matches.first().second.getValue("match"))
        assertFalse(early.matches.single().first.id == late.matches.first().first.id)
        val earlyPower = early.matches.map { PowerEvidenceReducer.reduce(it.first, it.second) }
        val latePower = late.matches.map { PowerEvidenceReducer.reduce(it.first, it.second) }
        fun decision(tracker: TrackerEvidence, powers: List<ReducedPowerMatch>) = FusionEvaluator.evaluate(
            tracker, powers, ZoneId.of("America/Chicago"), tracker.record.timeline.mapNotNull { it.cardId }.toSet(),
        ).manualDecision()
        val dkEarly = decision(dk, earlyPower)
        val dkLate = decision(dk, latePower)
        val dkTogether = decision(dk, earlyPower + latePower)
        val dkReversed = decision(dk, latePower + earlyPower)
        val priestTogether = decision(priest, earlyPower + latePower)
        println("GROWING_PREFIX dkEarly=${dkEarly.status} dkLate=${dkLate.status} dkTogether=${dkTogether.status} dkReversed=${dkReversed.status} priestTogether=${priestTogether.status}")
        println("GROWING_PREFIX priestCandidates=" + priestTogether.candidates.map { "${it.powerSourceId} score=${it.score} reasons=${it.reasons} rejected=${it.rejectedReasons}" })
        assertEquals(PairingStatus.ACCEPTED, dkEarly.status)
        assertEquals(PairingStatus.ACCEPTED, dkLate.status)
        assertEquals(dkTogether.status, dkReversed.status)
        assertEquals(PairingStatus.ACCEPTED, dkTogether.status)
        assertEquals(dkTogether.acceptedSourceId, dkReversed.acceptedSourceId)
        val combined = FusionEvaluator.evaluate(dk, earlyPower + latePower, ZoneId.of("America/Chicago"),
            dk.record.timeline.mapNotNull { it.cardId }.toSet())
        assertEquals(PairingStatus.ACCEPTED, combined.discoveryDecision(true).status)
        assertEquals(3, combined.powers.size, "All raw sources, including the separate Priest game, remain retained")
        val combinedArtifact = FusionCoordinator.assemble(dk, combined.powers, combined.manualDecision())
        assertEquals(4, combinedArtifact.sources.size, "Tracker plus all three original Power sources")
        assertTrue(combinedArtifact.sources.all { it.verifiedCompletedMatchKey == null }, "Ingestion-only proof does not leak into artifact model")
        assertTrue(FusionProvenanceValidator.validate(combinedArtifact).isEmpty())
        assertTrue(combinedArtifact.diagnostics.any { it.startsWith("Partial reconstruction: 1 selected Power") })
        assertEquals(1, combinedArtifact.diagnostics.count { it.startsWith("Equivalent completed Power evidence:") })
        assertEquals(PairingStatus.ACCEPTED, priestTogether.status, "Restored source identities now have scoped local PLAY evidence")
        val priestPower = latePower.single { it.source.id == priestTogether.acceptedSourceId }
        assertEquals(latePower.last().source.id, priestPower.source.id)
        val priestDocument = late.matches.single { it.first.id == priestPower.source.id }.second
        val priestRaw = priestDocument.getValue("match").jsonObject.getValue("events").jsonArray
            .map { it.jsonObject }.associateBy { it.getValue("source_line").toString().toInt() }
        for ((line, cardId) in listOf(32894 to "JAIL_720", 38812 to "CATA_301", 42241 to "CATA_215")) {
            val raw = priestRaw.getValue(line)
            assertEquals("PLAY", raw.getValue("block_type").jsonPrimitive.content)
            val actor = raw.getValue("actor").jsonPrimitive.content
            assertTrue(actor.contains("cardId=$cardId ") && actor.contains("player=2]"))
            assertTrue(priest.record.timeline.any { it.cardId == cardId })
            val play = priestPower.events.single { it.kind == "PLAY" && it.evidence.any { ref -> ref.sourceLine == line } }
            val entity = priestPower.entityEpochs.single { it.chronologyEpoch == play.chronologyEpoch }.entities.getValue(play.actorEntityId!!)
            val identity = entity.identityHistory.filter { it.evidence.sourceId == priestPower.source.id &&
                (it.evidence.sourceLine ?: 0) in 1..line }.maxByOrNull { it.evidence.sourceLine!! }!!
            val owner = entity.tagHistory.filter { it.name == "CONTROLLER" && it.evidence.sourceId == priestPower.source.id &&
                it.sourceLine in 1..line }.maxByOrNull { it.sourceLine }!!
            assertEquals(cardId, identity.cardId)
            assertEquals("2", owner.value)
            val proofs = PowerPairingFacts.fromReduced(priestPower, null, 2).anchorEvidence.getValue(cardId)
            assertTrue(identity.evidence in proofs && owner.evidence in proofs && play.evidence.all { it in proofs })
        }
        for ((line, controller, hero) in listOf(Triple(30408, 1, "64"), Triple(30421, 2, "66"))) {
            assertEquals("tag=HERO_ENTITY value=$hero", priestRaw.getValue(line).getValue("raw").jsonPrimitive.content)
            val playerHeader = priestRaw.values.last { event -> event.getValue("source_line").toString().toInt() < line &&
                event.getValue("raw").jsonPrimitive.content.startsWith("Player EntityID=") }.getValue("raw").jsonPrimitive.content
            assertTrue(Regex("PlayerID=$controller(?: |$)").containsMatchIn(playerHeader))
        }
        assertTrue(priestRaw.getValue(30671).getValue("raw").jsonPrimitive.content.endsWith("CardID=HERO_09a"))
        assertTrue(priestRaw.getValue(30703).getValue("raw").jsonPrimitive.content.endsWith("CardID=HERO_03bt"))
        assertEquals("LOST", priestRaw.getValue(66658).getValue("value").jsonPrimitive.content)
        val ownMapping = priestDocument.getValue("player_name_mappings").jsonArray.single {
            it.jsonObject.getValue("controller").toString() == "2"
        }.jsonObject
        assertTrue(ownMapping.getValue("source_line").toString().toInt() < 66658)
        assertEquals(ownMapping.getValue("name").jsonPrimitive.content, priestRaw.getValue(66658).getValue("entity").jsonPrimitive.content)
        assertEquals(MatchResult.LOSS, priest.record.result)
        val firstArtifact = FusionCoordinator.assemble(dk, earlyPower, dkEarly)
        val laterArtifact = FusionCoordinator.assemble(dk, latePower, dkLate)
        println("GROWING_PREFIX dkLogicalIdEarly=${firstArtifact.matchId} dkLogicalIdLate=${laterArtifact.matchId}")
        assertEquals(firstArtifact.matchId, laterArtifact.matchId)
        assertEquals(firstArtifact.matchId, combinedArtifact.matchId)
        kotlinx.coroutines.runBlocking {
            val dir = Files.createTempDirectory("hs-growing-prefix-").toFile()
            try {
                val repository = FusionImportRepository(dir)
                val importer = FusionImporter(repository, ZoneId.of("America/Chicago"))
                val original = importer.import(dk.record, listOf(earlyBytes))
                val savedBytes = repository.artifactForExport(dk.record.id).readBytes()
                val repeated = importer.import(dk.record, listOf(lateBytes))
                assertEquals(FusionImportState.FUSED_LOCAL, original.entry.state)
                assertEquals(original.entry, repeated.entry)
                assertEquals(1, repository.entries.value.size)
                assertTrue(savedBytes.contentEquals(repository.artifactForExport(dk.record.id).readBytes()))
                val legacyRepository = FusionImportRepository(java.io.File(dir, "legacy"))
                val legacy = firstArtifact.copy(matchId = "973af1e15211bbf43047a9719ce6a50c", fusionVersion = "android-r3/3")
                val legacyName = legacyRepository.writeArtifact(legacy)
                legacyRepository.begin(dk.record.id, listOf(early.artifactSha256))
                val legacyEntry = legacyRepository.transition(dk.record.id, FusionImportState.FUSED_LOCAL,
                    artifactFile = legacyName, fusedRecordRevision = dk.record.revision, fusedRecordResult = dk.record.result)
                val legacyBytes = legacyRepository.artifactForExport(dk.record.id).readBytes()
                val legacyRetry = FusionImporter(legacyRepository, ZoneId.of("America/Chicago"))
                    .import(dk.record.copy(result = MatchResult.LOSS, revision = dk.record.revision + 1), listOf(lateBytes))
                assertEquals(legacyEntry, legacyRetry.entry)
                assertEquals(legacyName, legacyRepository.artifactForExport(dk.record.id).name)
                assertTrue(legacyBytes.contentEquals(legacyRepository.artifactForExport(dk.record.id).readBytes()))
                assertEquals(1, java.io.File(dir, "legacy/fused-matches").listFiles()!!.size)
                assertTrue(legacy.matchId != firstArtifact.matchId)
                println("STABLE_GAME_ID legacyPathAndBytesPreserved=true newPolicy=${firstArtifact.fusionVersion}")
                println("GROWING_PREFIX existingHistoryEntries=${repository.entries.value.size} originalPreserved=true")
            } finally { dir.deleteRecursively() }
        }
    }

    @Test fun explicitLocalWentSecondPairRetainsRawOwnedTurnCounts() {
        val zone = ZoneId.of("America/Chicago")
        val trackerNames = listOf("match_20261008T210742979Z_2d002b13fdc7b8819a62d26100dc81bf7e53e3b83d9d9066915564dca160dcc0.json")
        val trackers = trackerNames.map { name ->
            val bytes = Files.readAllBytes(fixtureDir.resolve(name))
            assertEquals(trackerFixtureHashes.getValue(name), EvidenceAdapters.sha256(bytes))
            name to EvidenceAdapters.tracker(bytes)
        }.filter { it.second.record.wentFirst == false }
        assertTrue(trackers.isNotEmpty(), "Existing inventory must include explicit went-second records")
        data class Candidate(val bundle: String, val tracker: String, val power: ReducedPowerMatch, val controller: Int, val facts: PowerPairingFacts)
        val candidates = mutableListOf<Candidate>()
        val bundleNames = listOf("HS-export-20261008-161122-04f747f4.zip")
        for (bundleName in bundleNames) {
            val bytes = Files.readAllBytes(fixtureDir.resolve(bundleName))
            assertEquals(exporterFixtureHashes.getValue(bundleName), EvidenceAdapters.sha256(bytes))
            for ((source, document) in EvidenceAdapters.exporterBundle(bytes).matches) {
                val match = document.getValue("match").jsonObject
                val endText = match["ended_log_time"]?.toString()?.trim('"') ?: continue
                val end = PowerClock.resolve(source.sessionAlias ?: continue, endText, zone) ?: continue
                val nearby = trackers.filter { kotlin.math.abs(end.toEpochMilli() - it.second.record.timestamp) <= 120_000 }
                if (nearby.isEmpty()) continue
                val power = PowerEvidenceReducer.reduce(source, document)
                if (power.entityEpochs.size != 1) continue
                for ((trackerName, tracker) in nearby) for (controller in 1..2) {
                    val observedFacts = PowerPairingFacts.fromReduced(power, end, controller)
                    val openingOwners = power.snapshots.filter { it.phase == "TURN_SIGNAL" }.take(2).map { it.activeController }
                    val firstPlayer = power.entities.values.singleOrNull { it.tags["FIRST_PLAYER"] == "1" }
                    val explicitFirst = firstPlayer?.tagHistory?.lastOrNull { it.name == "FIRST_PLAYER" && it.value == "1" }?.evidence
                    val observedSecond = openingOwners == listOf(3 - controller, controller) &&
                        firstPlayer?.tags?.get("PLAYER_ID") == (3 - controller).toString() && explicitFirst != null
                    if (!observedSecond || observedFacts.result != tracker.record.result) continue
                    // Both opening CURRENT_PLAYER signals are observed; no default-zero or parity inference.
                    val facts = observedFacts.copy(wentFirst = false, wentFirstEvidence = listOfNotNull(explicitFirst) +
                        power.snapshots.filter { it.phase == "TURN_SIGNAL" }.take(2).flatMap { it.evidence })
                    val decision = FusionPairing.decide(tracker, listOf(facts))
                    if (decision.status == PairingStatus.ACCEPTED && facts.wentFirstEvidence.isNotEmpty())
                        candidates += Candidate(bundleName, trackerName, power, controller, facts)
                }
            }
        }
        val selected = candidates.groupBy { it.tracker }.values.firstOrNull { matches ->
            matches.distinctBy { it.power.source.id to it.controller }.size == 1 &&
                matches.first().power.snapshots.count { it.phase == "TURN_SIGNAL" } >= 4
        }?.first() ?: error("No uniquely paired explicit local went-second capture found")
        val power = selected.power
        assertEquals("power:b3f2f5c912db77f821b47cfa2611d32ba51366c686813eb4646c831c06ea0ed4:1", power.source.id)
        assertEquals(1, selected.controller)
        assertEquals(listOf(466, 465, 2192), selected.facts.wentFirstEvidence.map { it.sourceLine })
        assertEquals(listOf(54759), selected.facts.resultEvidence.map { it.sourceLine })
        val signals = power.snapshots.filter { it.phase == "TURN_SIGNAL" }
        assertEquals(30, signals.size)
        val rawEvents = EvidenceAdapters.exporterBundle(Files.readAllBytes(fixtureDir.resolve(selected.bundle))).matches
            .single { it.first.id == power.source.id }.second.getValue("match").jsonObject.getValue("events").jsonArray
            .map { it.jsonObject }.associateBy { it.getValue("source_line").toString().toInt() }
        assertEquals(3 - selected.controller, signals.first().activeController)
        assertEquals(1, signals.first { it.activeController == selected.controller }.playerTurnIndices[selected.controller])
        val counts = mutableMapOf<Int, Int>()
        var last: Int? = null
        var complete = true
        for (signal in signals) {
            val owner = signal.activeController
            if (owner == null) { complete = false; counts.clear(); last = null; continue }
            if (complete && owner != last) counts[owner] = (counts[owner] ?: 0) + 1
            assertEquals(counts, signal.playerTurnIndices)
            val player = power.entities.values.single { it.tags["PLAYER_ID"] == owner.toString() }
            val line = signal.evidence.single().sourceLine!!
            assertTrue(player.tagHistory.any { it.name == "PLAYER_ID" && it.sourceLine <= line })
            val rawSignal = rawEvents.getValue(line)
            assertEquals("\"CURRENT_PLAYER\"", rawSignal["tag"].toString())
            assertEquals("\"1\"", rawSignal["value"].toString())
            val namedEntity = rawSignal.getValue("entity").toString().trim('"')
            val directId = Regex("(?:id|entity)=([0-9]+)").find(namedEntity)?.groupValues?.get(1)
                ?: namedEntity.takeIf { it.all(Char::isDigit) }
            assertTrue(directId == player.id || player.aliasHistory.any {
                it.value == namedEntity && (it.evidence.sourceLine ?: Int.MAX_VALUE) <= line
            }, "Raw owner must resolve through direct ID or an already-observed alias at $line")
            last = owner
        }
        println("HSC004_LOCAL_SECOND tracker=${selected.tracker} trackerHash=${EvidenceAdapters.sha256(Files.readAllBytes(fixtureDir.resolve(selected.tracker)))} bundle=${selected.bundle} bundleHash=${EvidenceAdapters.sha256(Files.readAllBytes(fixtureDir.resolve(selected.bundle)))} source=${power.source.id} controller=${selected.controller} turnSignals=${signals.size} firstEvidence=${selected.facts.wentFirstEvidence} resultEvidence=${selected.facts.resultEvidence}")
    }

    @Test fun rawFirstPlayerAndCurrentOwnerAgreeForBothStartingControllers() {
        val starts = mutableSetOf<Int>()
        for ((name, hash) in exporterFixtureHashes) {
            val bytes = Files.readAllBytes(fixtureDir.resolve(name))
            assertEquals(hash, EvidenceAdapters.sha256(bytes))
            for ((source, document) in EvidenceAdapters.exporterBundle(bytes).matches) {
                val reduced = PowerEvidenceReducer.reduce(source, document)
                if (reduced.entityEpochs.size != 1) continue
                val firstPlayer = reduced.entities.values.singleOrNull { it.tags["FIRST_PLAYER"] == "1" }
                    ?: continue
                val controller = firstPlayer.tags["PLAYER_ID"]?.toIntOrNull() ?: continue
                val firstSignal = reduced.snapshots.firstOrNull { it.phase == "TURN_SIGNAL" } ?: continue
                if (firstSignal.activeController == null) continue
                assertEquals(controller, firstSignal.activeController, "$name first-player tag versus first current-player signal")
                assertEquals(1, firstSignal.playerTurnIndices[controller])
                starts += controller
                println("HSC004_START controller=$controller bundle=$name source=${source.id} line=${firstSignal.evidence.first().sourceLine}")
            }
            if (starts.containsAll(listOf(1, 2))) break
        }
        assertEquals(setOf(1, 2), starts, "Raw corpus must exercise both controller starting positions")
    }

    @Test fun sixRewindsPreserveSevenEntityEpochsAndLocatedChoiceStages() {
        val bundle = EvidenceAdapters.exporterBundle(Files.readAllBytes(fixtureDir.resolve(
            "fusion-test-589424a4-7b2c-48e1-a879-80ed5adf1fba-73cfa31b-853f-4567-bb99-14bd35b52ee4.zip")))
        val (source, document) = bundle.matches.single { (source, _) ->
            source.contentSha256.values.contains("ddc8357c0cd8f87ed37e9d092d758402dd1467a41968ff257c61e1358866b71e")
        }
        val reduced = PowerEvidenceReducer.reduce(source, document)
        assertEquals(6, reduced.events.count { it.kind == "GAME_RESET" })
        assertEquals((0..6).toList(), reduced.entityEpochs.map { it.chronologyEpoch })
        assertEquals(reduced.entities, reduced.entityEpochs.last().entities)
        assertTrue(reduced.entityEpochs.dropLast(1).all { it.entities.isNotEmpty() })
        assertEquals(6, reduced.diagnostics.count { it.contains("observed gameplay rewind") })
        val resets = reduced.events.filter { it.kind == "GAME_RESET" }.map { it.evidence.single().sourceLine!! }
        reduced.choices.flatMap { listOf(it.offered, it.submitted, it.confirmed) }.forEach { stage ->
            stage.chronologyEpoch?.let { epoch ->
                assertTrue(stage.evidence.isNotEmpty())
                assertTrue(stage.evidence.all { ref -> ref.sourceLine?.let { line -> resets.count { it <= line } == epoch } == true })
            }
        }
    }

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
        val positionalDiagnosticObservations = linkedMapOf<String, Int>()
        var unassignedHeaderCount = 0
        val positionalDisagreement = Regex("Positional descriptors disagree with explicit (ZONE|ZONE_POSITION|CONTROLLER) tags: ([0-9]+) observation\\(s\\); explicit tags retained; first source line ([0-9]+)")
        val unsupportedDescriptor = Regex("Unsupported positional descriptors: ([0-9]+) observation\\(s\\); no positional fields imported; first source line ([0-9]+)")
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
                // Cover every inventoried match, including rewinds, with equivalence disabled.
                assertEquals(reduced.diagnostics, PowerEvidenceReducer.reduce(
                    source.copy(verifiedCompletedMatchKey = null), document,
                ).diagnostics, "Ingestion-only equivalence proof must not change reducer diagnostics")
                matchCount++
                eventCount += reduced.events.size
                snapshotCount += reduced.snapshots.size
                if (reduced.completed) completedCount++
                if (reduced.sourceTruncated) truncatedCount++
                val perMatchDescriptorCounts = linkedMapOf<String, Int>()
                val unassignedHeaderLines = mutableListOf<Int>()
                val restoredHeaderLines = mutableListOf<Int>()
                reduced.diagnostics.forEach { diagnostic ->
                    val disagreement = positionalDisagreement.matchEntire(diagnostic)
                    val unsupported = unsupportedDescriptor.matchEntire(diagnostic)
                    if (disagreement != null || unsupported != null) {
                        val key = disagreement?.groupValues?.get(1) ?: "UNSUPPORTED"
                        val count = (disagreement?.groupValues?.get(2) ?: unsupported!!.groupValues[1]).toInt()
                        val firstLine = (disagreement?.groupValues?.get(3) ?: unsupported!!.groupValues[2]).toInt()
                        assertTrue(count > 0 && firstLine > 0)
                        assertTrue(document.getValue("match").jsonObject.getValue("events").jsonArray.any {
                            it.jsonObject.getValue("source_line").toString().toInt() == firstLine
                        }, "Diagnostic must retain a real raw source location")
                        positionalDiagnosticObservations[key] = positionalDiagnosticObservations.getOrDefault(key, 0) + count
                        perMatchDescriptorCounts[key] = count
                        return@forEach
                    }
                    val restored = Regex("Player restoration committed at source line ([0-9]+) from header ([0-9]+); earlier checkpoints unchanged").matchEntire(diagnostic)
                    if (restored != null) {
                        val commit=restored.groupValues[1].toInt(); val header=restored.groupValues[2].toInt()
                        val proofs=reduced.entityEpochs.flatMap { it.entities.values }.flatMap { it.tagHistory }
                            .filter { it.evidence.isPlayerRestoration && it.evidence.sourceLine==commit &&
                                it.evidence.supportingEvidence.any { p->p.role=="header" && p.sourceLine==header } }
                        assertTrue(proofs.isNotEmpty())
                        val rawEvents=document.getValue("match").jsonObject.getValue("events").jsonArray.map { it.jsonObject }
                            .associateBy { it.getValue("source_line").jsonPrimitive.content.toInt() }
                        for(proof in proofs) {
                            val origin=proof.evidence.supportingEvidence.single { it.role=="origin" }.sourceLine
                            assertEquals("tag=${proof.name} value=${proof.value}",rawEvents.getValue(origin).getValue("raw").jsonPrimitive.content)
                            val identity=proof.evidence.supportingEvidence.single { it.role=="identity" }.sourceLine
                            assertTrue(origin<commit && identity<commit && header<origin)
                        }
                        restoredHeaderLines += header
                        return@forEach
                    }
                    val unassigned = Regex("Unassigned FULL_ENTITY header at source line ([0-9]+); .*").matchEntire(diagnostic)
                    if (unassigned != null) {
                        val line = unassigned.groupValues[1].toInt()
                        val raw = document.getValue("match").jsonObject.getValue("events").jsonArray.single {
                            it.jsonObject.getValue("source_line").toString().toInt() == line
                        }.jsonObject.getValue("raw").jsonPrimitive.content
                        assertTrue(raw.startsWith("FULL_ENTITY - Updating "))
                        assertFalse(Regex("\\bid=[0-9]+").containsMatchIn(raw))
                        unassignedHeaderCount++
                        unassignedHeaderLines += line
                        return@forEach
                    }
                    val category = diagnostic
                        .replace(Regex("at source line [0-9]+"), "at source line <n>")
                        .replace(Regex("^[0-9]+ unterminated"), "<n> unterminated")
                    diagnosticCounts[category] = diagnosticCounts.getOrDefault(category, 0) + 1
                    diagnosticContexts += "$name#${source.matchAlias} completed=${reduced.completed} " +
                        "truncated=${reduced.sourceTruncated}: $diagnostic"
                }
                assertEquals(rawDescriptorAudit(reduced, document), perMatchDescriptorCounts,
                    "Every descriptor disagreement must match a prior explicit same-epoch raw observation")
                assertInlineRecipients(reduced, document)
                val expectedUnassignedLines = document.getValue("match").jsonObject.getValue("events").jsonArray.map { it.jsonObject }.filter { event ->
                    val raw = event.getValue("raw").jsonPrimitive.content
                    if (!raw.startsWith("FULL_ENTITY") || Regex("FULL_ENTITY - Creating ID=[0-9]+ CardID=.*").matches(raw)) false
                    else {
                        val update = Regex("FULL_ENTITY - Updating (.+) CardID=[^ ]*").matchEntire(raw)
                        val ids = update?.groupValues?.get(1)?.let { text -> Regex("(?:id|entity)=([0-9]+)", RegexOption.IGNORE_CASE)
                            .findAll(text).map { it.groupValues[1] }.distinct().toList() }.orEmpty()
                        ids.singleOrNull()?.toIntOrNull()?.let { it > 0 } != true
                    }
                }.map { it.getValue("source_line").toString().toInt() }
                assertEquals(expectedUnassignedLines, (unassignedHeaderLines+restoredHeaderLines).sorted(),
                    "Every formerly unassigned header needs a raw-verified restoration proof or an explicit refusal")
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
        // Preserve the original chronology golden below; independently pin the new raw-descriptor audit.
        // Numeric reveal blocks now update their actual cards instead of the prior card.
        // Raw inline recipient audit above independently checks attribution before this count pin.
        assertEquals(mapOf("ZONE" to 19_544, "ZONE_POSITION" to 20_665, "CONTROLLER" to 75, "UNSUPPORTED" to 288), positionalDiagnosticObservations)
        assertEquals(
            linkedMapOf("hs-export-bundle/0.2" to 4, "hs-export-bundle/0.3" to 2, "hs-export-bundle/0.4" to 15),
            schemas,
        )
        assertEquals(
            linkedMapOf(
                // Old golden predates the published HSC-004 reset/rewind chronology implementation.
                "Turn owner unresolved at source line <n>; per-player turn indices remain unknown" to 437,
                "Chronology epoch 1 begins at source line <n> after an observed gameplay rewind" to 3,
                "Chronology epoch 2 begins at source line <n> after an observed gameplay rewind" to 2,
                "Chronology epoch 3 begins at source line <n> after an observed gameplay rewind" to 2,
                "Chronology epoch 4 begins at source line <n> after an observed gameplay rewind" to 2,
                "Chronology epoch 5 begins at source line <n> after an observed gameplay rewind" to 2,
                "Chronology epoch 1 begins at source line <n> after GAME_RESET; reset cause is unclassified" to 4,
                "Unmatched BLOCK_END at source line <n>" to 6,
                "<n> unterminated block(s); final snapshot is unresolved" to 1,
            ),
            diagnosticCounts,
        )
        println(
            "REAL_FUSION_CORPUS bundles=${exporterFixtureHashes.size} matches=$matchCount " +
                "completed=$completedCount truncated=$truncatedCount events=$eventCount snapshots=$snapshotCount " +
                "schemas=$schemas unassignedFullHeaders=$unassignedHeaderCount diagnostics=$diagnosticCounts diagnosticContexts=$diagnosticContexts",
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

/** Independent raw block attribution check; never use the reducer's recipient resolver. */
private fun assertInlineRecipients(reduced: ReducedPowerMatch, document: kotlinx.serialization.json.JsonObject) {
    val owners = mutableMapOf<Int, String?>()
    var owner: String? = null
    for (item in document.getValue("match").jsonObject.getValue("events").jsonArray) {
        val e = item.jsonObject
        val raw = e.getValue("raw").jsonPrimitive.content
        if (e["block_type"]?.jsonPrimitive?.content == "GAME_RESET") owner = null
        if (listOf("FULL_ENTITY", "SHOW_ENTITY", "CHANGE_ENTITY", "Player ", "GameEntity ").any { raw.startsWith(it) }) {
            owner = when {
                raw.startsWith("FULL_ENTITY - Creating ID=") -> Regex("FULL_ENTITY - Creating ID=([0-9]+) CardID=.*").matchEntire(raw)?.groupValues?.get(1)
                raw.startsWith("Player ") || raw.startsWith("GameEntity ") -> Regex("EntityID=([0-9]+)").find(raw)?.groupValues?.get(1)
                else -> {
                    val target = raw.substringAfter("Updating ", "").substringBeforeLast(" CardID=", "").removePrefix("Entity=")
                    if (target.matches(Regex("[1-9][0-9]*"))) target
                    else Regex("(?:^| )id=([0-9]+)(?: |\\])").findAll(target).toList().singleOrNull()?.groupValues?.get(1)
                }
            }
        }
        if (raw.startsWith("tag=")) owners[e.getValue("source_line").jsonPrimitive.content.toInt()] = owner
    }
    for (epoch in reduced.entityEpochs) for ((id, entity) in epoch.entities) for (tag in entity.tagHistory) {
        if (tag.evidence.isPlayerRestoration || tag.sourceLine !in owners) continue
        assertEquals(owners[tag.sourceLine], id, "Inline raw recipient at source line ${tag.sourceLine}")
    }
}

/** Test-only audit: compare raw descriptors against earlier explicit tag proofs, not final state. */
private fun rawDescriptorAudit(reduced: ReducedPowerMatch, document: kotlinx.serialization.json.JsonObject): Map<String, Int> {
    val counts = linkedMapOf<String, Int>()
    val pattern = Regex("\\[entityName=.* id=([0-9]+) zone=([A-Z_]+) zonePos=([0-9]+) cardId=[^ ]* player=([0-9]+)\\]")
    val zones = setOf("INVALID", "PLAY", "DECK", "HAND", "GRAVEYARD", "REMOVEDFROMGAME", "SETASIDE", "SECRET")
    var epoch = 0
    for (element in document.getValue("match").jsonObject.getValue("events").jsonArray) {
        val event = element.jsonObject
        val raw = event.getValue("raw").jsonPrimitive.content
        if (raw.startsWith("BLOCK_START") && event["block_type"]?.jsonPrimitive?.content == "GAME_RESET") epoch++
        val text = event["entity"]?.jsonPrimitive?.content ?: continue
        val tag = event["tag"]?.jsonPrimitive?.content ?: continue
        val value = event["value"]?.jsonPrimitive?.content ?: continue
        val prefix = "TAG_CHANGE Entity=$text tag=$tag value="
        if (!raw.startsWith(prefix) || raw.removePrefix(prefix).trim() != value) continue
        val id = Regex("(?:id|entity)=([0-9]+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1) ?: continue
        val fields = pattern.matchEntire(text)?.groupValues
        val valid = fields != null && fields[1] == id && (id.toIntOrNull() ?: 0) > 0 && fields[2] in zones &&
            fields[3].toIntOrNull() != null && (fields[4].toIntOrNull() ?: 0) > 0
        if (!valid) {
            if (text.contains("zonePos=")) counts["UNSUPPORTED"] = counts.getOrDefault("UNSUPPORTED", 0) + 1
            continue
        }
        val line = event.getValue("source_line").toString().toInt()
        val entity = reduced.entityEpochs.single { it.chronologyEpoch == epoch }.entities[id] ?: continue
        for ((name, observed) in listOf("ZONE" to fields!![2], "ZONE_POSITION" to fields[3], "CONTROLLER" to fields[4])) {
            if (name == tag) continue
            val prior = entity.tagHistory.filter { it.name == name && it.evidence.sourceId == reduced.source.id &&
                it.sourceLine < line && it.evidence.rule != "power-descriptor-position/1" }.maxByOrNull { it.sourceLine }
            if (prior != null && prior.value != observed) counts[name] = counts.getOrDefault(name, 0) + 1
        }
    }
    return counts
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
