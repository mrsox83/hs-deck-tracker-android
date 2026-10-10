package com.stroexd.hsdecktracker.core.vision

import com.stroexd.hsdecktracker.core.fusion.ClaimStatus
import com.stroexd.hsdecktracker.core.fusion.Confidence
import kotlinx.serialization.Serializable

@Serializable
enum class VisualReviewKind { HUMAN_IMAGE_LABEL, INDEPENDENT_NON_VISUAL }

/** Explicit external annotation, already aligned to this frame/field, not another OCR score.
 * Callers must verify the referenced source and alignment before supplying a non-visual review.
 * This API does not collect logs, pair sources or turn annotations into tracker events.
 */
@Serializable
data class VisualFieldReview(
    val frameSha256: String,
    val frameObservedAt: Long,
    val field: String,
    val value: String,
    val kind: VisualReviewKind,
    val sourceSha256: String,
    val sourcePointer: String,
    val reviewer: String,
    val parentFrameSha256: String? = null,
)

@Serializable
data class VisualFieldAssessment(
    val frameSha256: String,
    val frameObservedAt: Long,
    val candidate: DiagnosticFieldCandidate,
    val value: String? = null,
    val stage: String = "experimental",
    val status: ClaimStatus = ClaimStatus.UNKNOWN,
    val confidence: Confidence = Confidence.LOW,
    val reason: String,
    val reviews: List<VisualFieldReview> = emptyList(),
    val rejectedReviewCount: Int = 0,
    val validationVersion: String = "visual-field-validation-1",
)

/** Pure per-frame assessment. Never upgrades an entire reader, match or legacy tracker record. */
object VisualFieldValidation {
    private val hash = Regex("[a-f0-9]{64}")
    private val fields = setOf("turn_affordance", "friendly_health", "opponent_health", "friendly_mana")

    fun assess(frameSha256: String, observedAt: Long, candidate: DiagnosticFieldCandidate,
        reused: Boolean, reviews: List<VisualFieldReview>): VisualFieldAssessment {
        require(hash.matches(frameSha256)) { "Assessment requires the exact frame SHA256" }
        require(observedAt >= 0) { "Assessment requires the frame capture timestamp" }
        val valid = reviews.filter {
            it.frameSha256 == frameSha256 && it.frameObservedAt == observedAt && it.field == candidate.field &&
                hash.matches(it.sourceSha256) && it.sourcePointer.startsWith("/") &&
                it.reviewer.isNotBlank() && it.value.isNotBlank() &&
                (it.kind == VisualReviewKind.HUMAN_IMAGE_LABEL ||
                    (it.sourceSha256 != frameSha256 && it.parentFrameSha256 == null))
        }.distinct()
        fun result(value: String? = null, stage: String = "experimental",
            status: ClaimStatus = ClaimStatus.UNKNOWN, confidence: Confidence = Confidence.LOW,
            reason: String) = VisualFieldAssessment(frameSha256, observedAt, candidate, value, stage,
                status, confidence, reason, valid, reviews.size - valid.size)
        // Stored/incoming candidate.stage is deliberately not trusted as a promotion decision.
        if (reused) return result(reason = "reused_ocr")
        if (candidate.value == null || candidate.support.isEmpty()) return result(reason = "unknown_or_unsupported_candidate")
        if (candidate.field !in fields) return result(candidate.value, status = ClaimStatus.INFERRED,
            reason = "untyped_or_inferred_field_not_promotable")
        val supported = candidate.ruleVersion == "landscape-fields-3" && when (candidate.field) {
            "turn_affordance" -> candidate.reason == "exact_label" &&
                candidate.value in setOf("enemy_turn_label", "end_turn_button")
            "friendly_health", "opponent_health" -> candidate.reason == "single_numeric_candidate" &&
                candidate.value.toIntOrNull()?.let { it in 1..999 } == true
            "friendly_mana" -> candidate.reason in setOf("exact_resource_pair", "positioned_resource_pair") &&
                Regex("([0-9]{1,2})/([0-9]{1,2})").matchEntire(candidate.value)?.let {
                    val current = it.groupValues[1].toInt()
                    val total = it.groupValues[2].toInt()
                    current <= total && total <= 20
                } == true
            else -> false
        }
        if (!supported) return result(reason = "unvalidated_rule_or_value")
        if (valid.any { it.value != candidate.value }) return result(status = ClaimStatus.CONFLICTED,
            reason = "review_disagrees_with_candidate")
        if (valid.isEmpty()) return result(candidate.value, status = ClaimStatus.INFERRED,
            reason = "no_independent_review")
        val human = valid.filter { it.kind == VisualReviewKind.HUMAN_IMAGE_LABEL }
        val independent = valid.filter { it.kind == VisualReviewKind.INDEPENDENT_NON_VISUAL }
        val twoSources = human.any { h -> independent.any { it.sourceSha256 != h.sourceSha256 } }
        return if (twoSources) result(candidate.value, "accepted", ClaimStatus.DERIVED,
            Confidence.MEDIUM, "human_and_independent_source_agree_at_frame")
        else result(candidate.value, "corroborating", ClaimStatus.DERIVED, Confidence.MEDIUM,
            "single_review_family_only")
    }
}
