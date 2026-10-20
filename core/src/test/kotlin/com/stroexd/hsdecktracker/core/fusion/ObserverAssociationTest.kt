package com.stroexd.hsdecktracker.core.fusion

import kotlinx.serialization.json.*
import kotlin.test.*

class ObserverAssociationTest {
    private val source = FusionSource("power", "hash", SourceType.ANDROID_POWER_EVIDENCE, captureMethod = "test")
    private fun mapping(name: String = "Alias", owner: Int = 2, line: Int = 2) = buildJsonObject {
        put("name", name); put("controller", owner); put("source_line", line)
    }
    private fun header(id: String = "1", line: Int, offer: Boolean, alias: String = "Alias") = buildJsonObject {
        put("source_line", line); put("method", if (offer) "DebugPrintEntityChoices" else "SendChoices")
        put("raw", if (offer) "id=$id Player=$alias TaskList= ChoiceType=GENERAL" else "id=$id ChoiceType=GENERAL")
    }
    private fun document(headers: List<JsonObject> = listOf(header(line=5, offer=true), header(line=8, offer=false)),
        mappings: List<JsonObject> = listOf(mapping()), resets: List<Int> = emptyList()): JsonObject = buildJsonObject {
        put("player_name_mappings", JsonArray(mappings))
        putJsonObject("match") {
            put("events", JsonArray((listOf(1) + resets + 50).sorted().map { line -> buildJsonObject {
                put("source_line", line); put("raw", if (line in resets) "BLOCK_START BlockType=GAME_RESET Entity=0 EffectCardId= Target=0 SubOption=-1" else "BLOCK_END")
                if (line in resets) put("block_type", "GAME_RESET")
            } }))
            put("choices", JsonArray(headers.groupBy { Regex("id=(\\d+)").find(it.getValue("raw").jsonPrimitive.content)!!.groupValues[1] }
                .map { (id, records) -> buildJsonObject { put("choice_id", id); put("evidence", JsonArray(records)) } }))
        }
    }
    private fun resolve(doc: JsonObject = document(), epoch: Int = 0, cutoff: Int = 10) = ObserverAssociationResolver.resolve(source, doc, epoch, cutoff)

    @Test fun bothControllerCasesAreCandidatesNotVisibilityClaims() {
        for (owner in listOf(1, 2)) {
            val doc = document(mappings=listOf(mapping(owner=owner)))
            val before = doc.toString()
            val value = resolve(doc)
            assertEquals(ObserverAssociationStatus.CANDIDATE, value.status)
            assertEquals(owner, value.candidateController)
            assertTrue(value.evidence.all { it.sourceId == source.id && it.sourceLine in 1..10 })
            assertTrue(value.diagnostics.contains("CANDIDATE_IS_NOT_VERIFIED_OBSERVER_KNOWLEDGE"))
            assertFalse(value.toString().contains("Alias"))
            assertEquals(before, doc.toString())
        }
    }
    @Test fun laterEvidenceDoesNotChangeAnEarlierAssociation() {
        val later = document(listOf(header(line=5, offer=true), header(line=8, offer=false),
            header(id="2", line=30, offer=true, alias="Other"), header(id="2", line=31, offer=false)),
            listOf(mapping(), mapping("Other", 1, 29)))
        assertEquals(resolve(), resolve(later))
        assertEquals(resolve(), resolve(document(mappings=listOf(mapping(), mapping(owner=1, line=40)))))
        assertNull(resolve(cutoff=7).candidateController)
        assertEquals(ObserverAssociationStatus.CONFLICTED, resolve(later, cutoff=40).status)
    }
    @Test fun resetsNeedFreshAliasAndChoiceProof() {
        val headers = listOf(header(line=5, offer=true), header(line=8, offer=false), header(line=23, offer=true), header(line=25, offer=false))
        val old = document(headers, resets=listOf(20))
        assertEquals(ObserverAssociationStatus.UNKNOWN, resolve(old, epoch=1, cutoff=22).status)
        assertEquals(ObserverAssociationStatus.UNKNOWN, resolve(old, epoch=1, cutoff=30).status)
        val fresh = document(headers, listOf(mapping(), mapping(owner=1, line=21)), listOf(20))
        assertEquals(1, resolve(fresh, epoch=1, cutoff=30).candidateController)
        assertEquals(ObserverAssociationStatus.UNKNOWN, resolve(fresh, epoch=0, cutoff=30).status)
    }
    @Test fun missingOfferAliasOrAdditionalUnresolvedSubmissionNeverCertifies() {
        assertEquals(ObserverAssociationStatus.UNKNOWN, resolve(document(listOf(header(line=8, offer=false)))).status)
        assertEquals(ObserverAssociationStatus.UNKNOWN, resolve(document(mappings=emptyList())).status)
        val mixed = document(listOf(header(line=5, offer=true), header(line=8, offer=false), header(id="2", line=9, offer=false)))
        assertEquals(ObserverAssociationStatus.UNKNOWN, resolve(mixed).status)
        assertNull(resolve(mixed).candidateController)
    }
    @Test fun ambiguityIsExplicitNotResolvedByLatestOrFirst() {
        val alias = resolve(document(mappings=listOf(mapping(), mapping(owner=1, line=3))))
        assertEquals(ObserverAssociationStatus.CONFLICTED, alias.status)
        assertNull(alias.candidateController)
        val offers = document(listOf(header(line=4, offer=true), header(line=5, offer=true), header(line=8, offer=false)))
        assertEquals(ObserverAssociationStatus.CONFLICTED, resolve(offers).status)
        val collision = document(listOf(header(line=5, offer=true), header(line=5, offer=false)))
        assertEquals(ObserverAssociationStatus.CONFLICTED, resolve(collision).status)
    }
    @Test fun aliasesMustPrecedeOfferAndCannotComeFromOtherEpoch() {
        for (line in listOf(5, 6, 11, 40)) assertNull(resolve(document(mappings=listOf(mapping(line=line)))).candidateController)
    }
    @Test fun unsupportedSourcesLocationsAndCancellationFailClosed() {
        assertEquals(ObserverAssociationStatus.UNKNOWN, resolve(cutoff=51).status)
        assertEquals(ObserverAssociationStatus.UNKNOWN, ObserverAssociationResolver.resolve(source.copy(type=SourceType.DERIVED_FUSION), document(), 0, 10).status)
        for (wrongSource in listOf(source.copy(sourceLineStart=4), source.copy(sourceLineEnd=30),
            source.copy(sessionAlias="other-session"), source.copy(matchAlias="other-match"))) {
            assertEquals(ObserverAssociationStatus.UNKNOWN, ObserverAssociationResolver.resolve(wrongSource, document(), 0, 10).status)
        }
        assertFailsWith<java.util.concurrent.CancellationException> {
            ObserverAssociationResolver.resolve(source, document(), 0, 10) { throw java.util.concurrent.CancellationException() }
        }
    }

    private fun withChoices(extra: JsonElement): JsonObject {
        val doc = document()
        val match = doc.getValue("match").jsonObject
        return JsonObject(doc + ("match" to JsonObject(match + ("choices" to
            JsonArray(match.getValue("choices").jsonArray + extra)))))
    }

    @Test fun missingOrWronglyTypedChoiceEvidenceInvalidatesCandidate() {
        for (evidence in listOf<JsonElement?>(null, JsonNull, JsonPrimitive("bad"))) {
            val extra = buildJsonObject {
                put("choice_id", "2"); put("submitted_log_time", "t")
                put("submitted_entities", JsonArray(listOf(JsonPrimitive("9"))))
                if (evidence != null) put("evidence", evidence)
            }
            val result = resolve(withChoices(extra))
            assertEquals(ObserverAssociationStatus.UNKNOWN, result.status)
            assertNull(result.candidateController)
            assertTrue("CHOICE_EVIDENCE_UNAVAILABLE" in result.diagnostics)
        }
    }

    @Test fun unlocatedOrMalformedAppendedRecordsSafelyInvalidateEarlierCandidate() {
        val unlocated = buildJsonObject {
            put("choice_id", "2")
            putJsonArray("evidence") { add(buildJsonObject {
                put("method", "SendChoices"); put("raw", "id=2")
            }) }
        }
        val base = document()
        val badMapping = JsonObject(base + ("player_name_mappings" to
            JsonArray(base.getValue("player_name_mappings").jsonArray + JsonNull)))
        for (doc in listOf(withChoices(unlocated), withChoices(JsonNull), badMapping)) {
            val result = resolve(doc)
            assertEquals(ObserverAssociationStatus.UNKNOWN, result.status)
            assertNull(result.candidateController)
        }
    }

    @Test fun resetWithoutReducerClassificationFailsClosed() {
        val doc = document(resets=listOf(20))
        val match = doc.getValue("match").jsonObject
        val events = match.getValue("events").jsonArray.map { item ->
            val event = item.jsonObject
            if (event.getValue("source_line").jsonPrimitive.int == 20)
                JsonObject((event - "block_type") + ("raw" to JsonPrimitive("BLOCK_START BlockType=GAME_RESET Entity=0")))
            else event
        }
        val malformed = JsonObject(doc + ("match" to JsonObject(match + ("events" to JsonArray(events)))))
        assertEquals(resolve(doc), resolve(malformed)) // Future malformed reset cannot affect cutoff10.
        for (epoch in listOf(0, 1)) {
            val result = resolve(malformed, epoch=epoch, cutoff=30)
            assertEquals(ObserverAssociationStatus.UNKNOWN, result.status)
            assertTrue("UNCLASSIFIED_RESET_BOUNDARY" in result.diagnostics)
        }
    }
}
