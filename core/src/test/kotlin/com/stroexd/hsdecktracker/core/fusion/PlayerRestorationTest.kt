package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.serialization.json.*
import kotlin.test.*

class PlayerRestorationTest {
    private val source = FusionSource("p", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod = "test")
    private fun reduce(lines: List<String>): ReducedPowerMatch {
        val events = lines.mapIndexed { i, raw -> buildJsonObject {
            put("sequence", i + 1); put("source_line", i + 1); put("log_time", "t$i"); put("raw", raw)
            Regex("TAG_CHANGE Entity=(.*?) tag=(\\S+) value=(.*)").matchEntire(raw)?.let {
                put("entity", it.groupValues[1]); put("tag", it.groupValues[2]); put("value", it.groupValues[3])
            }
            if (raw.startsWith("BLOCK_START")) put("block_type", if (raw.contains("GAME_RESET")) "GAME_RESET" else "PLAY")
        } }
        return PowerEvidenceReducer.reduce(source, buildJsonObject { putJsonObject("match") { put("events", JsonArray(events)) } })
    }
    private val reset = listOf("BLOCK_START BlockType=GAME_RESET Entity=0", "BLOCK_END")
    private val action = listOf("BLOCK_START BlockType=PLAY Entity=0", "BLOCK_END")
    private fun block(name: String = "PlayerOne", id: Int = 2, side: Int = 1) = listOf(
        "FULL_ENTITY - Updating $name CardID=", "tag=CONTROLLER value=$side", "tag=CARDTYPE value=PLAYER",
        "tag=PLAYER_ID value=$side", "tag=TURN value=9", "tag=CURRENT_PLAYER value=1",
        "tag=HERO_ENTITY value=64", "tag=ENTITY_ID value=$id", "tag=RESOURCES value=9")
    private fun artifact(r: ReducedPowerMatch) = FusedMatch(FUSION_SCHEMA, "test", fusionVersion = "test/7",
        sources = listOf(source), pairing = PairingDecision(PairingStatus.PENDING, candidates = emptyList(), reason = "test"),
        events = r.events, snapshots = r.snapshots, entities = r.entities, entityEpochs = r.entityEpochs, choices = emptyList())

    @Test fun delayedBlockCommitsOnlyAtBoundaryWithOriginProofAndNoTurnSignal() {
        val lines = reset + block() + action
        for (prefix in reset.size..(reset.size + block().size))
            assertFalse(reduce(lines.take(prefix)).entities.containsKey("2"), "No complete block boundary at $prefix")
        val r = reduce(lines)
        val e = r.entities.getValue("2")
        assertEquals(ClaimStatus.DERIVED, e.controller.status)
        assertTrue(e.tagHistory.all { it.sourceLine == 12 && it.evidence.sourceLine == 12 })
        assertEquals(4, e.tagHistory.single { it.name == "CONTROLLER" }.evidence.supportingEvidence.single { it.role == "origin" }.sourceLine)
        assertEquals(10, e.tagHistory.single { it.name == "CONTROLLER" }.evidence.supportingEvidence.single { it.role == "identity" }.sourceLine)
        assertEquals(1, r.events.last().activeController)
        assertNull(r.events.last().rawTurn)
        assertNull(r.events.last().playerTurnIndex)
        assertFalse(r.snapshots.any { it.phase == "TURN_SIGNAL" })
        assertFalse(r.snapshots.first().entityTags.containsKey("2"))
        val f = artifact(r)
        assertEquals(emptyList(), FusionProvenanceValidator.validate(f))
        assertEquals(f, FusionArtifactCodec.decode(FusionArtifactCodec.encode(f)))
    }
    @Test fun globalTurnAndBothSidesAndExactAliasAreNotGuessed() {
        for (side in 1..2) {
            val r = reduce(reset + listOf("GameEntity EntityID=7", "tag=TURN value=18") + block(side=side) +
                listOf("TAG_CHANGE Entity=PlayerOne tag=CURRENT_PLAYER value=1") + action)
            assertEquals(18, r.events.last().rawTurn)
            assertEquals(side, r.events.last().activeController)
            assertNull(r.events.last().playerTurnIndex)
        }
        val unknown = reduce(reset + block(name="UNKNOWN HUMAN PLAYER") +
            listOf("TAG_CHANGE Entity=SomeoneElse tag=CURRENT_PLAYER value=1") + action)
        assertNull(unknown.events.last().activeController)
        assertTrue(unknown.entities.getValue("2").aliasHistory.isEmpty())
    }
    @Test fun conflictsMissingProofOverflowAndResetInterruptionRefuseRestoration() {
        val bad = listOf(block().filterNot { it.startsWith("tag=ENTITY_ID") },
            block() + "tag=ENTITY_ID value=3", block() + "tag=CONTROLLER value=2",
            block() + List(260) { "tag=EXTRA_$it value=0" },
            block().map { if (it=="tag=CARDTYPE value=PLAYER") "tag=CARDTYPE value=MINION" else it })
        for (b in bad) assertFalse(reduce(reset+b+action).entities.containsKey("2"))
        val interrupted = reduce(reset + block() + reset + action)
        assertFalse(interrupted.entities.containsKey("2"))
    }
    @Test fun reusedIdNeedsFreshBlockAndEarlierEpochRemainsImmutable() {
        val r = reduce(reset+block()+action+reset+action)
        assertTrue(r.entityEpochs.single { it.chronologyEpoch==1 }.entities.containsKey("2"))
        assertFalse(r.entities.containsKey("2"))
        val restored = reduce(reset+block()+action+reset+block()+action)
        assertTrue(restored.entities.containsKey("2"))
        assertTrue(restored.entities.getValue("2").tagHistory.all { it.sourceLine>20 })
    }
    @Test fun gameOrExistingCardCollisionAndAliasCollisionRefused() {
        val game = reduce(reset+listOf("GameEntity EntityID=2")+block()+action)
        assertFalse(game.entities["2"]?.tags.orEmpty().containsKey("PLAYER_ID"))
        val card = reduce(reset+listOf("FULL_ENTITY - Creating ID=2 CardID=CARD")+block()+action)
        assertEquals("CARD", card.entities.getValue("2").cardId.value)
        assertFalse(card.entities.getValue("2").tags.containsKey("PLAYER_ID"))
        val alias = reduce(reset+block()+block(id=3,side=2)+action)
        assertFalse(alias.entities.containsKey("3"))
    }
    @Test fun contradictoryActivePlayersStayUnknownAndCannotAuthorizeContinuation() {
        val r = reduce(reset+block()+block(name="Other",id=3,side=2)+action)
        assertNull(r.events.last().activeController)
        assertEquals(2,r.entities.size)
        assertTrue(r.entities.values.all { it.aliasHistory.single().evidence.isPlayerRestoration })
    }
    @Test fun laterCardOrGameCollisionQuarantinesCurrentStateWithoutEditingEarlierSnapshots() {
        for (collision in listOf("GameEntity EntityID=2", "FULL_ENTITY - Creating ID=2 CardID=CARD")) {
            val r=reduce(reset+block()+action+listOf(collision,"TAG_CHANGE Entity=2 tag=TURN value=18")+action)
            assertEquals(1,r.events[1].activeController)
            assertNull(r.events.last().activeController)
            assertNull(r.events.last().rawTurn)
            assertNull(r.entities.getValue("2").controller.value)
            assertFalse(r.entities.getValue("2").tags.containsKey("PLAYER_ID"))
            assertTrue(r.snapshots.any { it.entityTags["2"]?.get("PLAYER_ID")=="1" })
            val last=FusionSnapshotMaterializer.materialize(r.snapshots).last()
            assertFalse(last.entityTags["2"].orEmpty().containsKey("PLAYER_ID"))
            assertTrue(last.players.isEmpty())
            assertTrue(r.entities.getValue("2").aliasHistory.isNotEmpty()) // Historical proof retained.
        }
    }
    @Test fun claimStatusAndControllerAndLinkContextsAreValidated() {
        val f=artifact(reduce(reset+block()+action));val e=f.entities.getValue("2")
        fun withEntity(entity: EntityState)=f.copy(entities=mapOf("2" to entity),entityEpochs=f.entityEpochs.map {
            if(it.chronologyEpoch==1) it.copy(entities=mapOf("2" to entity)) else it })
        for(status in ClaimStatus.entries.filter { it!=ClaimStatus.DERIVED }) {
            assertTrue(FusionProvenanceValidator.validate(withEntity(e.copy(controller=e.controller.copy(status=status)))).isNotEmpty())
        }
        val wrong=e.controller.evidence.single().copy(rule="${PlayerRestoration.RULE}3:CONTROLLER")
        assertTrue(FusionProvenanceValidator.validate(withEntity(e.copy(controller=e.controller.copy(evidence=listOf(wrong))))).isNotEmpty())
        val link=e.entityLinks.getValue("HERO_ENTITY")
        assertTrue(FusionProvenanceValidator.validate(withEntity(e.copy(entityLinks=mapOf("HERO_ENTITY" to link.copy(
            evidence=listOf(link.evidence.single().copy(rule="${PlayerRestoration.RULE}2:TURN"))))))).isNotEmpty())
    }
    @Test fun codecRejectsCrossSourceFutureWrongContextAndCrossEpochProof() {
        val f=artifact(reduce(reset+block()+action))
        val e=f.entities.getValue("2"); val o=e.tagHistory.first()
        fun changed(ref: EvidenceRef): FusedMatch {
            val replacement=e.copy(tagHistory=e.tagHistory.map { if(it==o) it.copy(evidence=ref) else it })
            return f.copy(entities=mapOf("2" to replacement),entityEpochs=f.entityEpochs.map { if(it.chronologyEpoch==1) it.copy(entities=mapOf("2" to replacement)) else it })
        }
        val bad=listOf(o.evidence.copy(supportingEvidence=emptyList()),
            o.evidence.copy(rule="${PlayerRestoration.RULE}3:CONTROLLER"),
            o.evidence.copy(supportingEvidence=o.evidence.supportingEvidence.map { if(it.role=="origin") it.copy(sourceId="other") else it }),
            o.evidence.copy(supportingEvidence=o.evidence.supportingEvidence.map { if(it.role=="identity") it.copy(sourceLine=99) else it }),
            o.evidence.copy(supportingEvidence=o.evidence.supportingEvidence.map { if(it.role=="header") it.copy(sourceLine=1) else it }))
        for(ref in bad) {
            val invalid=changed(ref)
            assertTrue(FusionProvenanceValidator.validate(invalid).isNotEmpty())
            assertFailsWith<FusionArtifactException> { FusionArtifactCodec.encode(invalid) }
            assertFailsWith<FusionArtifactException> { FusionArtifactCodec.decode(AppJson.encodeToString(FusedMatch.serializer(),invalid)) }
        }
    }
    @Test fun oldEvidenceEncodingKeepsDefaultSupportingProofAbsent() {
        val ref=EvidenceRef("p",1,rule="ordinary")
        val encoded=AppJson.encodeToString(EvidenceRef.serializer(),ref)
        assertFalse(encoded.contains("supportingEvidence"))
        assertEquals(ref,AppJson.decodeFromString(EvidenceRef.serializer(),encoded))
    }
    @Test fun archivedClaimsCannotHideCrossEpochProofBehindClearedRecursiveEvents() {
        val f=artifact(reduce(reset+block()+action+reset+action))
        assertTrue(FusionProvenanceValidator.validate(f).isEmpty())
        val archive=f.entityEpochs.single { it.chronologyEpoch==1 };val e=archive.entities.getValue("2")
        fun corrupt(ref: EvidenceRef)=ref.copy(supportingEvidence=ref.supportingEvidence.map {
            if(it.role=="header") it.copy(sourceLine=1) else it })
        val controller=e.copy(controller=e.controller.copy(evidence=e.controller.evidence.map(::corrupt)))
        val links=e.copy(entityLinks=e.entityLinks.mapValues { (_,claim)->claim.copy(evidence=claim.evidence.map(::corrupt)) })
        for(replacement in listOf(controller,links)) {
            val invalid=f.copy(entityEpochs=f.entityEpochs.map { if(it.chronologyEpoch==1) it.copy(entities=mapOf("2" to replacement)) else it })
            assertTrue(FusionProvenanceValidator.validate(invalid).any { it.contains("invalid player restoration proof") })
        }
    }
}
