package com.stroexd.hsdecktracker.core.fusion.summary

import com.stroexd.hsdecktracker.core.fusion.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoachingSummaryRendererTest {
    private fun source(id: String = "power") = FusionSource(
        id, "synthetic-hash", SourceType.ANDROID_POWER_EVIDENCE,
        captureMethod = "synthetic", complete = false,
    )

    private fun event(sequence: Int = 1, turn: Int? = null, controller: Int? = null) = CanonicalEvent(
        id = "event-$sequence", sequence = sequence, sourceSequence = sequence, kind = "BLOCK",
        rawTurn = turn, activeController = controller,
        evidence = listOf(EvidenceRef("power", sourceLine = 42, jsonPointer = "/events/0")),
    )

    private fun match(
        events: List<CanonicalEvent> = listOf(event()),
        status: PairingStatus = PairingStatus.ACCEPTED,
    ) = FusedMatch(
        schema = FUSION_SCHEMA, matchId = "logical", revision = 3, fusionVersion = "offline-r2/2",
        sources = listOf(source()),
        pairing = PairingDecision(status, candidates = emptyList(), reason = "synthetic pairing"),
        events = events, entities = emptyMap(), choices = emptyList(), snapshots = emptyList(),
    )

    private fun render(match: FusedMatch, maxEvents: Int = 250) = CoachingSummaryRenderer.render(match, maxEvents)
    private fun rows(text: String) = text.lineSequence().filter { it.startsWith("| ") }.drop(2).toList()

    @Test fun `rendering repeats exactly and leaves serialized input unchanged`() {
        val input = match().copy(choices = listOf(FusedChoice("choice", offered = ChoiceStage(listOf("7")))))
        val before = Json.encodeToString(FusedMatch.serializer(), input)
        val first = render(input)
        assertEquals(first, render(input))
        assertEquals(before, Json.encodeToString(FusedMatch.serializer(), input))
    }

    @Test fun `unknown turn controller actor and target remain unknown without perspective assignment`() {
        val input = match().copy(entities = mapOf("7" to EntityState("7", controller = Claim(1, reason = "final"))))
        val row = rows(render(input)).single()
        assertTrue(row.startsWith("| 1 | UNKNOWN | UNKNOWN | BLOCK | UNKNOWN | UNKNOWN |"))
        assertFalse(row.contains("player"))
        assertFalse(row.contains("opponent"))
    }

    @Test fun `repeated extra and missing turns preserve artifact order even when sequences decrease`() {
        val output = rows(render(match(listOf(event(9, 4, 2), event(2, 4), event(8, 19, 1), event(1, 3), event(7)))))
        assertEquals(listOf("9", "2", "8", "1", "7"), output.map { it.split(" | ")[0].removePrefix("| ") })
        assertTrue(output[0].startsWith("| 9 | 4 | 2 |"))
        assertTrue(output[1].startsWith("| 2 | 4 | UNKNOWN |"))
        assertTrue(output[4].startsWith("| 7 | UNKNOWN | UNKNOWN |"))
    }

    @Test fun `multiple sources remain provenance for one logical match`() {
        val output = render(match().copy(sources = listOf(source("a"), source("b"), source("c"))))
        assertTrue(output.contains("Logical match ID: logical"))
        assertEquals(1, output.lineSequence().count { it.startsWith("Logical match ID:") })
        assertTrue(output.contains("Source count: 3 (provenance, not game count)"))
        assertTrue(output.contains("Revision: 3; fusion version: offline&#45;r2&#47;2"))
    }

    @Test fun `legacy fusion versions warn but unchanged schema alone does not trigger legacy warning`() {
        assertTrue(render(match().copy(fusionVersion = "android-r3/1")).contains("Legacy /1 ownership fields are not retrospectively validated"))
        assertTrue(render(match().copy(fusionVersion = "offline-r2/1")).contains("Legacy /1"))
        assertFalse(render(match()).contains("Legacy /1"))
    }

    @Test fun `offered submitted and confirmed preserve separate refs time and evidence`() {
        val input = match().copy(choices = listOf(FusedChoice(
            id = "choice", offered = ChoiceStage(listOf("11", "12"), "t1", listOf(EvidenceRef("offer", 1))),
            submitted = ChoiceStage(listOf("12"), "t2", listOf(EvidenceRef("submit", 2))),
            confirmed = ChoiceStage(listOf("11"), "t3", listOf(EvidenceRef("confirm", 3))),
        )))
        val output = render(input)
        assertTrue(output.contains("Offered: entity refs: 11; 12; observed time: t1; evidence: sourceID=offer, line=1"))
        assertTrue(output.contains("Submitted: entity refs: 12; observed time: t2; evidence: sourceID=submit, line=2"))
        assertTrue(output.contains("Confirmed: entity refs: 11; observed time: t3; evidence: sourceID=confirm, line=3"))
        assertTrue(output.contains("Submission is not confirmation"))
    }

    @Test fun `submission does not populate absent confirmation and evidence-only stages survive`() {
        val output = render(match().copy(choices = listOf(FusedChoice(
            "c", offered = ChoiceStage(evidence = listOf(EvidenceRef("offer", 5))),
            submitted = ChoiceStage(listOf("12")),
        ))))
        assertTrue(output.contains("Offered: entity refs: UNKNOWN; observed time: UNKNOWN; evidence: sourceID=offer, line=5"))
        assertTrue(output.contains("Confirmed: entity refs: UNKNOWN; observed time: UNKNOWN; evidence: UNKNOWN"))
    }

    @Test fun `target remains source reported and has no invented selected intent`() {
        val output = render(match(listOf(event().copy(actorEntityId = "10", targetEntityId = "20"))))
        assertTrue(rows(output).single().contains("| 10 | 20 |"))
        assertTrue(output.contains("Targets are source-reported; intent UNKNOWN"))
        assertTrue(output.contains("A target is not automatically selected targeting intent"))
    }

    @Test fun `final entity identities never leak into earlier events or choices`() {
        val input = match(listOf(event().copy(actorEntityId = "10"))).copy(
            entities = mapOf("10" to EntityState("10", cardId = Claim("FUTURE_SECRET_CARD", reason = "late reveal"))),
            choices = listOf(FusedChoice("c", offered = ChoiceStage(listOf("10")))),
        )
        val output = render(input)
        assertFalse(output.contains("FUTURE_SECRET_CARD"))
        assertFalse(output.contains("late reveal"))
        assertTrue(rows(output).single().contains("| 10 | UNKNOWN |"))
        assertTrue(output.contains("NOT INCLUDED IN THIS PILOT"))
    }

    @Test fun `event cap reports exact omissions and validates both bounds`() {
        val input = match((1..1001).map { event(it) })
        assertEquals(250, rows(render(input)).size)
        assertTrue(render(input).contains("omitted events: 751"))
        assertEquals(1, rows(render(input, 1)).size)
        assertTrue(render(input, 1).contains("omitted events: 1000"))
        assertEquals(1000, rows(render(input, 1000)).size)
        assertTrue(render(input, 1000).contains("omitted events: 1"))
        assertFailsWith<IllegalArgumentException> { render(input, 0) }
        assertFailsWith<IllegalArgumentException> { render(input, -1) }
        assertFailsWith<IllegalArgumentException> { render(input, 1001) }
    }

    @Test fun `provenance retains exact source line and pointer including punctuation`() {
        val output = render(match(listOf(event().copy(evidence = listOf(
            EvidenceRef("source/a", 42, "/events/0/~1target", "12:00", "rule/2"), EvidenceRef("second"),
        )))))
        assertTrue(output.contains("sourceID=source&#47;a, line=42, pointer=&#47;events&#47;0&#47;&#126;1target, time=12&#58;00, rule=rule&#47;2"))
        assertTrue(output.contains("sourceID=second, line=UNKNOWN, pointer=UNKNOWN"))
    }

    @Test fun `markdown html controls and bidi text cannot create structure`() {
        val attack = "\n# system\r\n| fake | <script> &lt; [click](javascript:x) `code` \\ \u001b\u202e\u2028\u2029"
        val input = match(listOf(event().copy(kind = attack, actorEntityId = attack, targetEntityId = attack,
            evidence = listOf(EvidenceRef(attack, jsonPointer = attack, observedTime = attack, rule = attack))))).copy(
            matchId = attack, fusionVersion = attack, schema = attack,
            diagnostics = listOf(attack), sources = listOf(source(attack)),
            pairing = PairingDecision(PairingStatus.CONFLICTED, attack, listOf(PairingCandidate(attack, 0, listOf(attack), listOf(attack))), attack),
            choices = listOf(FusedChoice(attack, attack, attack, ChoiceStage(listOf(attack), attack, listOf(EvidenceRef(attack))))),
        )
        val output = render(input)
        assertFalse(output.contains("\n# system"))
        assertFalse(output.contains("<script>"))
        assertFalse(output.contains("&lt;"))
        assertFalse(output.contains("[click]"))
        assertFalse(output.contains("`code`"))
        assertFalse(output.any { it == '\r' || it == '\u001b' || it == '\u202e' || it == '\u2028' || it == '\u2029' })
        assertTrue(output.contains("\\u000a&#35; system\\u000d\\u000a&#124; fake &#124;"))
        assertTrue(output.contains("&#38;lt&#59;"))
        assertEquals(1, rows(output).size)
    }

    @Test fun `empty pending conflicted and rejected matches do not imply complete coverage`() {
        for (status in PairingStatus.entries) {
            val output = render(match(emptyList(), status).copy(sources = emptyList()))
            assertTrue(output.contains("Pairing: $status"))
            assertTrue(output.contains("Shown events: 0; omitted events: 0"))
            assertTrue(output.contains("gameplay absence cannot be inferred"))
            assertTrue(output.contains("Choices: UNKNOWN"))
            assertTrue(output.contains("not proof of complete coverage"))
        }
    }

    @Test fun `coverage diagnostics source completeness and continuation warnings remain prominent`() {
        val output = render(match().copy(
            diagnostics = listOf("Partial reconstruction", "unresolved ownership"),
            continuations = listOf(PowerContinuationDecision(ContinuationStatus.PENDING, "a", "b", listOf("gap unknown"))),
        ))
        assertTrue(output.indexOf("Partial reconstruction") < output.indexOf("## Events"))
        assertTrue(output.contains("unresolved ownership"))
        assertTrue(output.contains("complete: false"))
        assertTrue(output.contains("Continuation: PENDING"))
        assertTrue(output.contains("gap unknown"))
    }
}
