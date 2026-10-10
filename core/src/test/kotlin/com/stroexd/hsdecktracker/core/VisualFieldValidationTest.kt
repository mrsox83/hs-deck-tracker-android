package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.ClaimStatus
import com.stroexd.hsdecktracker.core.vision.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.*

class VisualFieldValidationTest {
    private val frame = "a".repeat(64)
    private val annotation = "b".repeat(64)
    private val log = "c".repeat(64)
    private val candidate = DiagnosticFieldCandidate("friendly_mana", "1/2", "positioned_resource_pair",
        NormalizedRegion(.84f, .76f, .91f, .90f), listOf(OcrLine("1", .85f, .78f, .88f, .82f)))
    private fun review(kind: VisualReviewKind = VisualReviewKind.HUMAN_IMAGE_LABEL) =
        VisualFieldReview(frame, 123, candidate.field, "1/2", kind,
            if (kind == VisualReviewKind.HUMAN_IMAGE_LABEL) annotation else log, "/labels/0", "reviewer")
    private fun assess(c: DiagnosticFieldCandidate = candidate, reused: Boolean = false,
        vararg reviews: VisualFieldReview) = VisualFieldValidation.assess(frame, 123, c, reused, reviews.toList())

    @Test fun `no review is experimental and human review is frame scoped corroboration`() {
        assertEquals("experimental", assess().stage)
        val result = assess(reviews = arrayOf(review()))
        assertEquals("corroborating", result.stage)
        assertEquals("1/2", result.value)
        assertEquals(candidate, result.candidate)
        assertEquals(result, Json.decodeFromString<VisualFieldAssessment>(Json.encodeToString(result)))
    }
    @Test fun `acceptance requires human plus independent non visual source`() {
        assertEquals("accepted", assess(reviews = arrayOf(review(), review(VisualReviewKind.INDEPENDENT_NON_VISUAL))).stage)
        assertEquals("corroborating", assess(reviews = arrayOf(review(VisualReviewKind.INDEPENDENT_NON_VISUAL))).stage)
        assertEquals("corroborating", assess(reviews = arrayOf(review(), review().copy(sourcePointer = "/labels/1"))).stage)
    }
    @Test fun `duplicates same source and visual descendants cannot manufacture independence`() {
        val repeated = assess(reviews = arrayOf(review(), review()))
        assertEquals(1, repeated.reviews.size)
        assertEquals("corroborating", repeated.stage)
        for (fake in listOf(review(VisualReviewKind.INDEPENDENT_NON_VISUAL).copy(sourceSha256 = frame),
            review(VisualReviewKind.INDEPENDENT_NON_VISUAL).copy(parentFrameSha256 = frame))) {
            assertEquals("corroborating", assess(reviews = arrayOf(review(), fake)).stage)
        }
        assertEquals("corroborating", assess(reviews = arrayOf(review(),
            review(VisualReviewKind.INDEPENDENT_NON_VISUAL).copy(sourceSha256 = annotation))).stage)
    }
    @Test fun `mismatch conflicts instead of silently overwriting candidate`() {
        val result = assess(reviews = arrayOf(review(), review().copy(value = "0/2")))
        assertEquals(ClaimStatus.CONFLICTED, result.status)
        assertNull(result.value)
        assertEquals("1/2", result.candidate.value)
    }
    @Test fun `unknown rule and invalid domain cannot be accepted by agreeing labels`() {
        for (bad in listOf(candidate.copy(ruleVersion = "future-rule"), candidate.copy(reason = "guessed"),
            candidate.copy(value = "3/2"), candidate.copy(value = "1/99"))) {
            assertNull(assess(bad, reviews = arrayOf(review().copy(value = requireNotNull(bad.value)),
                review(VisualReviewKind.INDEPENDENT_NON_VISUAL).copy(value = bad.value))).value)
        }
    }
    @Test fun `wrong binding malformed references cached OCR and unknowns cannot promote`() {
        for (bad in listOf(review().copy(frameSha256 = log), review().copy(frameObservedAt = 124),
            review().copy(field = "opponent_health"), review().copy(sourceSha256 = "bad"),
            review().copy(sourcePointer = ""), review().copy(reviewer = ""))) {
            assertEquals("experimental", assess(reviews = arrayOf(bad)).stage)
        }
        assertNull(assess(reused = true, reviews = arrayOf(review())).value)
        assertNull(assess(candidate.copy(value = null), reviews = arrayOf(review())).value)
        assertNull(assess(candidate.copy(support = emptyList()), reviews = arrayOf(review())).value)
        assertEquals("experimental", assess(candidate.copy(stage = "accepted")).stage)
        assertEquals("experimental", assess(candidate.copy(field = "friendly_adjacent_counter"),
            reviews = arrayOf(review().copy(field = "friendly_adjacent_counter"))).stage)
    }
}
