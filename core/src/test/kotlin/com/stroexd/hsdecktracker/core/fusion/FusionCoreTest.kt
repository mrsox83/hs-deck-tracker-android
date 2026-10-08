package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchResult
import com.stroexd.hsdecktracker.core.stats.MatchSource
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.time.Instant
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FusionCoreTest {
    @Test
    fun `power reducer retains order tags blocks choices and provenance`() {
        val bytes = powerEvidence().toByteArray()
        val (source, document) = EvidenceAdapters.powerDocument(bytes)
        val reduced = PowerEvidenceReducer.reduce(source, document)

        assertEquals(listOf("PLAY", "ATTACK"), reduced.events.map { it.kind })
        assertEquals(reduced.events[0].id, reduced.events[1].parentEventId)
        assertEquals(null, reduced.events[0].targetEntityId)
        assertEquals(2, reduced.events[0].activeController)
        assertEquals(1, reduced.events[0].playerTurnIndex)
        assertEquals("CARD_A", reduced.entities.getValue("7").cardId.value)
        assertEquals("2", reduced.entities.getValue("7").tags["CONTROLLER"])
        assertEquals("4", reduced.entities.getValue("7").tags["HERALD_COLOSSAL_AMOUNT"])
        assertEquals(listOf("HAND"), reduced.entities.getValue("7").zoneHistory.map { it.value })
        assertEquals(listOf(2), reduced.entities.getValue("7").positionHistory.map { it.value })
        assertEquals(listOf(false, true), reduced.entities.getValue("7").visibilityHistory.map { it.value })
        assertEquals("9", reduced.entities.getValue("7").entityLinks.getValue("CREATOR").value)
        assertTrue(reduced.entities.getValue("7").tagHistory.all { it.evidence.sourceLine != null })
        assertEquals(listOf("[id=8 cardId=OPTION_A]"), reduced.choices.single().offered.entityRefs)
        assertEquals(listOf("[id=8 cardId=OPTION_A]"), reduced.choices.single().submitted.entityRefs)
        assertEquals(listOf("[id=8 cardId=OPTION_A]"), reduced.choices.single().confirmed.entityRefs)
        assertEquals(1, reduced.snapshots.count { it.phase == "BEFORE_ACTION" })
        assertEquals(1, reduced.snapshots.count { it.phase == "AFTER_OUTER_ACTION" })
        assertEquals(4, reduced.snapshots.first { it.phase == "AFTER_OUTER_ACTION" }.counters.getValue("7").getValue("HERALD_COLOSSAL_AMOUNT").value)
        assertEquals(6, reduced.snapshots.first { it.phase == "AFTER_OUTER_ACTION" }.counters.getValue("7").getValue("HERALD_COLOSSAL_AMOUNT").evidence.single().sourceLine)
        assertEquals(mapOf(2 to 1), reduced.snapshots.first { it.phase == "AFTER_OUTER_ACTION" }.playerTurnIndices)
        val player = reduced.snapshots.first { it.phase == "AFTER_OUTER_ACTION" }.players.getValue(2)
        assertEquals("9", player.heroEntityId.value)
        assertEquals(30, player.health.value)
        assertEquals(7, player.damage.value)
        assertEquals(23, player.remainingHealth.value)
        assertEquals(ClaimStatus.DERIVED, player.remainingHealth.status)
        assertEquals(listOf(43, 44), player.remainingHealth.evidence.map { it.sourceLine })
        assertEquals(2, player.armor.value)
        assertEquals(4, player.heraldAmount.value)
        assertEquals(null, player.heraldThresholdReached.value)
        val quest = reduced.snapshots.first { it.phase == "AFTER_OUTER_ACTION" }.quests.getValue("10")
        assertEquals(3, quest.progress.value)
        assertEquals(3, quest.total.value)
        assertEquals(null, quest.completed.value)
        assertEquals("11", quest.rewardEntityId.value)
        assertFalse(reduced.snapshots.last().unresolvedBlock)
        assertTrue(reduced.diagnostics.isEmpty())
    }

    @Test
    fun `unterminated blocks and unknown tags remain visible`() {
        val doc = powerEvidence().replace(
            "{\"sequence\":9,\"source_line\":19,\"log_time\":\"12:00:09.0\",\"raw\":\"BLOCK_END\",\"kind\":\"BLOCK_END\"},",
            "",
        )
        val (source, parsed) = EvidenceAdapters.powerDocument(doc.toByteArray())
        val reduced = PowerEvidenceReducer.reduce(source, parsed)
        assertEquals("opaque", reduced.entities.getValue("7").tags["FUTURE_TAG"])
        assertTrue(reduced.snapshots.last().unresolvedBlock)
        assertTrue(reduced.diagnostics.single().contains("unterminated"))
    }

    @Test
    fun `entity identity revisions retain when each identity became known`() {
        val changed = powerEvidence().replace(
            "{\"sequence\":4,\"source_line\":4,\"log_time\":\"12:00:03.0\",\"raw\":\"tag=FUTURE_TAG value=opaque\",\"kind\":\"tag=FUTURE_TAG\"}",
            "{\"sequence\":4,\"source_line\":4,\"log_time\":\"12:00:03.0\",\"raw\":\"CHANGE_ENTITY - Updating Entity=[entityName=A id=7 zone=PLAY] CardID=CARD_B\",\"kind\":\"CHANGE_ENTITY\",\"entity_id\":\"7\",\"card_id\":\"CARD_B\"}",
        )
        val (source, document) = EvidenceAdapters.powerDocument(changed.toByteArray())
        val entity = PowerEvidenceReducer.reduce(source, document).entities.getValue("7")
        assertEquals(listOf("CARD_A", "CARD_B"), entity.identityHistory.map { it.cardId })
        assertEquals(listOf(2, 4), entity.identityHistory.map { it.evidence.sourceLine })
        assertEquals("CARD_B", entity.cardId.value)
    }

    @Test
    fun `pairing requires unique multi characteristic evidence`() {
        val tracker = tracker()
        val reduced = reducedPower()
        fun facts(match: ReducedPowerMatch, offsetMillis: Long = 7_000) = PowerPairingFacts(
            match = match,
            endedAt = Instant.ofEpochMilli(tracker.record.timestamp - offsetMillis),
            result = MatchResult.WIN,
            wentFirst = true,
            anchorCardIds = setOf("CARD_A"),
            endEvidence = listOf(EvidenceRef(match.source.id, sourceLine = 20)),
            resultEvidence = listOf(EvidenceRef(match.source.id, sourceLine = 21)),
            wentFirstEvidence = listOf(EvidenceRef(match.source.id, sourceLine = 22)),
            anchorEvidence = mapOf("CARD_A" to listOf(EvidenceRef(match.source.id, sourceLine = 7))),
        )
        val accepted = FusionPairing.decide(
            tracker,
            listOf(facts(reduced)),
            setOf("CARD_A"),
        )
        assertEquals(PairingStatus.ACCEPTED, accepted.status)
        assertTrue(accepted.candidates.single().evidence.all { it.sourceLine != null || it.jsonPointer != null })

        val ambiguous = FusionPairing.decide(
            tracker,
            listOf(
                facts(reduced),
                facts(reduced.copy(source = reduced.source.copy(id = "power:other")), 8_000),
            ),
        )
        assertEquals(PairingStatus.CONFLICTED, ambiguous.status)
    }

    @Test
    fun `pairing does not accept matching values without exact power evidence`() {
        val tracker = tracker()
        val reduced = reducedPower()
        val decision = FusionPairing.decide(
            tracker,
            listOf(PowerPairingFacts(reduced, Instant.ofEpochMilli(tracker.record.timestamp), MatchResult.WIN, true, setOf("CARD_A"))),
            setOf("CARD_A"),
        )

        assertEquals(PairingStatus.PENDING, decision.status)
        assertTrue(decision.candidates.single().rejectedReasons.all { it.contains("lacks source evidence") })
        assertTrue(decision.candidates.single().evidence.isEmpty())
    }

    @Test
    fun `replayed and shuffled imports produce one deterministic fused artifact`() {
        val tracker = tracker()
        val power = reducedPower()
        val decision = evidencedDecision(power)
        val first = FusionCoordinator.assemble(tracker, listOf(power, power), decision)
        val second = FusionCoordinator.assemble(tracker, listOf(power), decision)

        assertEquals(second, first)
        assertEquals(2, first.sources.size)
        assertEquals(FUSION_SCHEMA, first.schema)
        assertTrue(first.events.all { it.evidence.isNotEmpty() })
        assertNotNull(first.matchId)
    }

    @Test
    fun `versioned fused artifact survives json round trip`() {
        val tracker = tracker()
        val power = reducedPower()
        val fused = FusionCoordinator.assemble(tracker, listOf(power), evidencedDecision(power))

        val encoded = FusionArtifactCodec.encode(fused)
        val decoded = FusionArtifactCodec.decode(encoded)
        val streamed = ByteArrayOutputStream().also { FusionArtifactCodec.encodeToStream(fused, it) }
        val streamDecoded = ByteArrayInputStream(streamed.toByteArray()).use(FusionArtifactCodec::decodeFromStream)

        assertTrue(encoded.contains("\"schema\":\"$FUSION_SCHEMA\""))
        assertEquals(fused, decoded)
        assertEquals(fused, streamDecoded)
    }

    @Test
    fun `fused artifact codec requires an explicit supported schema`() {
        val power = reducedPower()
        val encoded = FusionArtifactCodec.encode(FusionCoordinator.assemble(tracker(), listOf(power), evidencedDecision(power)))
        val withoutSchema = encoded.replace("\"schema\":\"$FUSION_SCHEMA\",", "")
        val wrongSchema = encoded.replace("\"schema\":\"$FUSION_SCHEMA\"", "\"schema\":\"hs-fused-match/2\"")
        val numericSchema = encoded.replace("\"schema\":\"$FUSION_SCHEMA\"", "\"schema\":2")

        val missing = assertFailsWith<FusionArtifactException> { FusionArtifactCodec.decode(withoutSchema) }
        val unsupported = assertFailsWith<FusionArtifactException> { FusionArtifactCodec.decode(wrongSchema) }
        val nonString = assertFailsWith<FusionArtifactException> { FusionArtifactCodec.decode(numericSchema) }

        assertEquals("Fused artifact schema is required", missing.message)
        assertEquals("Unsupported fused artifact schema: hs-fused-match/2", unsupported.message)
        assertEquals("Fused artifact schema must be a string", nonString.message)
    }

    @Test
    fun `fused artifact codec rejects malformed json and invalid provenance`() {
        val malformed = assertFailsWith<FusionArtifactException> { FusionArtifactCodec.decode("{not-json") }
        assertEquals("Invalid fused artifact JSON", malformed.message)

        val power = reducedPower()
        val fused = FusionCoordinator.assemble(tracker(), listOf(power), evidencedDecision(power))
        val invalid = fused.copy(events = fused.events.mapIndexed { index, event ->
            if (index == 0) event.copy(evidence = emptyList()) else event
        })
        val invalidJson = AppJson.encodeToString(FusedMatch.serializer(), invalid)

        val encodeError = assertFailsWith<FusionArtifactException> { FusionArtifactCodec.encode(invalid) }
        val decodeError = assertFailsWith<FusionArtifactException> { FusionArtifactCodec.decode(invalidJson) }
        assertTrue(encodeError.message.orEmpty().contains("events[0] has no evidence"))
        assertTrue(decodeError.message.orEmpty().contains("events[0] has no evidence"))
    }

    @Test
    fun `provenance validator rejects an accepted fact with no exact evidence`() {
        val tracker = tracker()
        val power = reducedPower()
        val fused = FusionCoordinator.assemble(tracker, listOf(power), evidencedDecision(power))
        assertTrue(FusionProvenanceValidator.validate(fused).isEmpty())

        val invalid = fused.copy(events = fused.events.mapIndexed { index, event ->
            when (index) {
                0 -> event.copy(evidence = emptyList())
                1 -> event.copy(evidence = listOf(EvidenceRef("missing-source", sourceLine = 8)))
                else -> event
            }
        })
        val errors = FusionProvenanceValidator.validate(invalid)
        assertTrue(errors.any { it == "events[0] has no evidence" })
        assertTrue(errors.any { it == "events[1] evidence[0] references unknown source missing-source" })
    }

    @Test
    fun `composite tracker is rejected without assigning match facts`() {
        val rejected = FusionPairing.rejectComposite(tracker(), listOf("power:a", "power:b"))
        assertEquals(PairingStatus.REJECTED_COMPOSITE, rejected.status)
        assertEquals(null, rejected.acceptedSourceId)
        assertTrue(rejected.candidates.all { it.rejectedReasons.isNotEmpty() })
    }

    @Test
    fun `bundle adapter verifies inner power hash and finds matches independent of zip order`() {
        val raw = "power-log".toByteArray()
        val expected = EvidenceAdapters.sha256(raw)
        val entries = linkedMapOf(
            "session-a/match-001.json" to powerEvidence().toByteArray(),
            "session-a/Power.log" to raw,
            "manifest.json" to """{"schema":"hs-export-bundle/0.4","sessions":[{"session":"session-a","source_sha256":"$expected"}]}""".toByteArray(),
        )
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip -> entries.entries.reversed().forEach { (name, content) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(content); zip.closeEntry()
        } }
        val bundle = EvidenceAdapters.exporterBundle(output.toByteArray())
        assertEquals("hs-export-bundle/0.4", bundle.schema)
        assertTrue(bundle.diagnostics.isEmpty())
        assertEquals(expected, bundle.matches.single().first.contentSha256["session-a/Power.log"])
        assertEquals(bundle.artifactSha256, bundle.matches.single().first.artifactSha256)
    }

    @Test
    fun `legacy bundle truncation marker patches only final source match`() {
        val first = powerEvidence().replace("\"index\":1", "\"index\":1")
        val last = powerEvidence().replace("\"index\":1", "\"index\":2")
            .replace("\"completed\":true", "\"completed\":false")
        val raw = "line one\nD marker - Truncating log, which has reached the size limit\n".toByteArray()
        val entries = linkedMapOf(
            "session-a/Power.log" to raw,
            "session-a/match-001.json" to first.toByteArray(),
            "session-a/match-002.json" to last.toByteArray(),
            "manifest.json" to """{"schema":"hs-export-bundle/0.3","sessions":[{"session":"session-a","source_sha256":"${EvidenceAdapters.sha256(raw)}"}]}""".toByteArray(),
        )
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip -> entries.forEach { (name, content) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(content); zip.closeEntry()
        } }
        val bundle = EvidenceAdapters.exporterBundle(output.toByteArray())
        val reduced = bundle.matches.map { PowerEvidenceReducer.reduce(it.first, it.second) }
        assertEquals(listOf(false, true), reduced.map { it.sourceTruncated })
        assertTrue(bundle.diagnostics.single().contains("line 2"))
    }

    @Test
    fun `different wrappers around identical power content share source identity and retain aliases`() {
        fun bundle(readme: String): ByteArray {
            val raw = "same-power-log".toByteArray()
            val entries = linkedMapOf(
                "session-a/Power.log" to raw,
                "session-a/match-001.json" to powerEvidence().toByteArray(),
                "manifest.json" to """{"schema":"hs-export-bundle/0.4","sessions":[{"session":"session-a","source_sha256":"${EvidenceAdapters.sha256(raw)}"}]}""".toByteArray(),
                "README.txt" to readme.toByteArray(),
            )
            val output = ByteArrayOutputStream()
            ZipOutputStream(output).use { zip -> entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(content); zip.closeEntry()
            } }
            return output.toByteArray()
        }
        val left = EvidenceAdapters.exporterBundle(bundle("left")).matches.single()
        val right = EvidenceAdapters.exporterBundle(bundle("right")).matches.single()
        assertEquals(left.first.id, right.first.id)
        assertFalse(left.first.artifactSha256 == right.first.artifactSha256)

        val reduced = listOf(left, right).map { PowerEvidenceReducer.reduce(it.first, it.second) }
        val tracker = tracker()
        val decision = evidencedDecision(reduced.first())
        val fused = FusionCoordinator.assemble(tracker, reduced, decision)
        val source = fused.sources.single { it.id == left.first.id }
        assertEquals(2, source.artifactAliases.size)
        assertEquals(reduced.first().events, fused.events)
    }

    private fun tracker(): TrackerEvidence {
        val record = MatchRecord(
            id = "tracker-match", timestamp = 1_791_401_186_774, result = MatchResult.WIN,
            wentFirst = true, source = MatchSource.AUTO,
        )
        return EvidenceAdapters.tracker(AppJson.encodeToString(MatchRecord.serializer(), record).toByteArray())
    }

    private fun reducedPower(): ReducedPowerMatch {
        val (source, document) = EvidenceAdapters.powerDocument(powerEvidence().toByteArray())
        return PowerEvidenceReducer.reduce(source, document)
    }

    private fun evidencedDecision(power: ReducedPowerMatch) = PairingDecision(
        status = PairingStatus.ACCEPTED,
        acceptedSourceId = power.source.id,
        candidates = listOf(
            PairingCandidate(
                powerSourceId = power.source.id,
                score = 4,
                reasons = listOf("test evidence"),
                rejectedReasons = emptyList(),
                evidence = listOf(EvidenceRef(power.source.id, sourceLine = 1, rule = "test/1")),
            ),
        ),
        reason = "test",
    )

    private fun powerEvidence() = """
        {
          "schema":"hs-power-evidence/0.3",
          "source_session":"session-a",
          "match":{
            "index":1,"started_log_time":"12:00:00.0","ended_log_time":"12:00:10.0","completed":true,"source_truncated":false,
            "events":[
              {"sequence":1,"source_line":1,"log_time":"12:00:00.0","raw":"CREATE_GAME","kind":"CREATE_GAME"},
              {"sequence":40,"source_line":40,"log_time":"12:00:00.1","raw":"Player EntityID=2 PlayerID=2 GameAccountId=[hi=1 lo=2]","kind":"Player"},
              {"sequence":41,"source_line":41,"log_time":"12:00:00.2","raw":"tag=CONTROLLER value=2","kind":"tag=CONTROLLER"},
              {"sequence":42,"source_line":42,"log_time":"12:00:00.3","raw":"tag=HERO_ENTITY value=9","kind":"tag=HERO_ENTITY"},
              {"sequence":47,"source_line":47,"log_time":"12:00:00.31","raw":"tag=HERALD_COLOSSAL_AMOUNT value=4","kind":"tag=HERALD_COLOSSAL_AMOUNT"},
              {"sequence":48,"source_line":48,"log_time":"12:00:00.32","raw":"tag=HERALD_COLOSSAL_CLASS value=7","kind":"tag=HERALD_COLOSSAL_CLASS"},
              {"sequence":43,"source_line":43,"log_time":"12:00:00.4","raw":"FULL_ENTITY - Creating ID=9 CardID=HERO_A","kind":"FULL_ENTITY","entity_id":"9","card_id":"HERO_A"},
              {"sequence":44,"source_line":43,"log_time":"12:00:00.5","raw":"tag=HEALTH value=30","kind":"tag=HEALTH"},
              {"sequence":45,"source_line":44,"log_time":"12:00:00.6","raw":"tag=DAMAGE value=7","kind":"tag=DAMAGE"},
              {"sequence":46,"source_line":45,"log_time":"12:00:00.7","raw":"tag=ARMOR value=2","kind":"tag=ARMOR"},
              {"sequence":50,"source_line":50,"log_time":"12:00:00.8","raw":"FULL_ENTITY - Creating ID=10 CardID=QUEST_A","kind":"FULL_ENTITY","entity_id":"10","card_id":"QUEST_A"},
              {"sequence":51,"source_line":51,"log_time":"12:00:00.81","raw":"tag=CONTROLLER value=2","kind":"tag=CONTROLLER"},
              {"sequence":52,"source_line":52,"log_time":"12:00:00.82","raw":"tag=QUEST_PROGRESS value=3","kind":"tag=QUEST_PROGRESS"},
              {"sequence":53,"source_line":53,"log_time":"12:00:00.83","raw":"tag=QUEST_PROGRESS_TOTAL value=3","kind":"tag=QUEST_PROGRESS_TOTAL"},
              {"sequence":54,"source_line":54,"log_time":"12:00:00.84","raw":"tag=REWARD_ENTITY value=11","kind":"tag=REWARD_ENTITY"},
              {"sequence":2,"source_line":2,"log_time":"12:00:01.0","raw":"FULL_ENTITY - Creating ID=7 CardID=CARD_A","kind":"FULL_ENTITY","entity_id":"7","card_id":"CARD_A"},
              {"sequence":3,"source_line":3,"log_time":"12:00:02.0","raw":"tag=CONTROLLER value=2","kind":"tag=CONTROLLER"},
              {"sequence":30,"source_line":30,"log_time":"12:00:02.1","raw":"tag=ZONE value=HAND","kind":"tag=ZONE"},
              {"sequence":31,"source_line":31,"log_time":"12:00:02.2","raw":"tag=ZONE_POSITION value=2","kind":"tag=ZONE_POSITION"},
              {"sequence":32,"source_line":32,"log_time":"12:00:02.3","raw":"TAG_CHANGE Entity=[entityName=A id=7 zone=HAND] tag=CREATOR value=9","kind":"TAG_CHANGE","entity":"[entityName=A id=7 zone=HAND]","tag":"CREATOR","value":"9"},
              {"sequence":33,"source_line":33,"log_time":"12:00:02.4","raw":"HIDE_ENTITY - Entity=[entityName=A id=7 zone=HAND]","kind":"HIDE_ENTITY"},
              {"sequence":34,"source_line":34,"log_time":"12:00:02.5","raw":"SHOW_ENTITY - Updating Entity=[entityName=A id=7 zone=HAND] CardID=CARD_A","kind":"SHOW_ENTITY","entity_id":"7","card_id":"CARD_A"},
              {"sequence":35,"source_line":35,"log_time":"12:00:02.6","raw":"TAG_CHANGE Entity=[entityName=Player id=2 zone=PLAY] tag=CURRENT_PLAYER value=1","kind":"TAG_CHANGE","entity":"[entityName=Player id=2 zone=PLAY]","tag":"CURRENT_PLAYER","value":"1"},
              {"sequence":4,"source_line":4,"log_time":"12:00:03.0","raw":"tag=FUTURE_TAG value=opaque","kind":"tag=FUTURE_TAG"},
              {"sequence":5,"source_line":5,"log_time":"12:00:04.0","raw":"TAG_CHANGE Entity=[entityName=A id=7 zone=PLAY] tag=TURN value=3","kind":"TAG_CHANGE","entity":"[entityName=A id=7 zone=PLAY]","tag":"TURN","value":"3"},
              {"sequence":6,"source_line":6,"log_time":"12:00:05.0","raw":"TAG_CHANGE Entity=[entityName=A id=7 zone=PLAY] tag=HERALD_COLOSSAL_AMOUNT value=4","kind":"TAG_CHANGE","entity":"[entityName=A id=7 zone=PLAY]","tag":"HERALD_COLOSSAL_AMOUNT","value":"4"},
              {"sequence":7,"source_line":7,"log_time":"12:00:06.0","raw":"BLOCK_START BlockType=PLAY Entity=[entityName=A id=7] EffectCardId= Target=0 SubOption=-1","kind":"BLOCK_START","block_type":"PLAY","actor":"[entityName=A id=7]","target":"0"},
              {"sequence":8,"source_line":8,"log_time":"12:00:07.0","raw":"BLOCK_START BlockType=ATTACK Entity=[entityName=A id=7] EffectCardId= Target=[entityName=B id=8] SubOption=-1","kind":"BLOCK_START","block_type":"ATTACK","actor":"[entityName=A id=7]","target":"[entityName=B id=8]"},
              {"sequence":9,"source_line":19,"log_time":"12:00:09.0","raw":"BLOCK_END","kind":"BLOCK_END"},
              {"sequence":10,"source_line":20,"log_time":"12:00:10.0","raw":"BLOCK_END","kind":"BLOCK_END"}
            ],
            "choices":[{
              "choice_id":"4","choice_type":"GENERAL","source":"[id=7 cardId=CARD_A]",
              "options":["[id=8 cardId=OPTION_A]"],"submitted_entities":["[id=8 cardId=OPTION_A]"],"confirmed_entities":["[id=8 cardId=OPTION_A]"],
              "log_time":"12:00:01.0","submitted_log_time":"12:00:02.0","confirmed_log_time":"12:00:03.0",
              "evidence":[
                {"source_line":11,"log_time":"12:00:01.0","method":"DebugPrintEntityChoices"},
                {"source_line":12,"log_time":"12:00:02.0","method":"SendChoices"},
                {"source_line":13,"log_time":"12:00:03.0","method":"DebugPrintEntitiesChosen"}
              ]
            }]
          }
        }
    """.trimIndent()
}
