package com.stroexd.hsdecktracker.core.vision

import kotlinx.serialization.Serializable

/** Experimental candidate with the exact OCR support retained. Never a tracker event. */
@Serializable
data class DiagnosticFieldCandidate(
    val field: String,
    val value: String? = null,
    val reason: String,
    val region: NormalizedRegion,
    val support: List<OcrLine> = emptyList(),
    val ruleVersion: String = "landscape-fields-2",
    val stage: String = "experimental",
)

object VisualFieldPilot {
    private val turnWindow = NormalizedRegion(.74f, .42f, .84f, .57f)
    private val friendlyHealth = NormalizedRegion(.50f, .87f, .565f, .99f)
    private val opponentHealth = NormalizedRegion(.50f, .15f, .565f, .26f)
    private val friendlyMana = NormalizedRegion(.735f, .75f, .825f, .85f)
    private val handWindow = NormalizedRegion(.15f, .85f, .80f, .95f)
    private val verticalMana = NormalizedRegion(.84f, .76f, .91f, .90f)
    private val verticalCorpses = NormalizedRegion(.84f, .90f, .91f, 1f)
    private val normalCorpses = NormalizedRegion(.80f, .75f, .84f, .86f)

    fun read(frame: OcrFrame, reused: Boolean): List<DiagnosticFieldCandidate> {
        fun lines(region: NormalizedRegion) = frame.lines.filter {
            it.centerX >= region.left && it.centerX < region.right &&
                it.centerY >= region.top && it.centerY < region.bottom
        }.take(32)
        val unavailable = when {
            reused -> "reused_ocr"
            frame.aspect !in 2.2f..2.5f -> "uncalibrated_aspect"
            else -> null
        }
        val turnLines = lines(turnWindow)
        val turnValues = turnLines.mapNotNull {
            when (it.text.trim().uppercase().replace(Regex("\\s+"), " ")) {
                "ENEMY TURN" -> "enemy_turn_label"
                "END TURN" -> "end_turn_button"
                else -> null
            }
        }.distinct()
        val turn = DiagnosticFieldCandidate("turn_affordance",
            value = if (unavailable == null && turnValues.size == 1) turnValues.single() else null,
            reason = unavailable ?: if (turnValues.size == 1) "exact_label" else "missing_or_ambiguous_label",
            region = turnWindow, support = turnLines)
        val gate = unavailable ?: if (turn.value == null) "no_gameplay_anchor" else null
        // Large lower-screen text is a layout cue only, not recognition of a card identity or action.
        val largeHandText = lines(handWindow).filter { it.height >= .025f && it.text.count(Char::isLetter) >= 5 }
        val expanded = largeHandText.size >= 2
        val layout = DiagnosticFieldCandidate("hand_layout",
            value = if (gate == null && expanded) "expanded_hand_candidate" else null,
            reason = gate ?: if (expanded) "large_lower_card_text" else "no_expansion_cue",
            region = handWindow, support = largeHandText)
        fun health(field: String, region: NormalizedRegion): DiagnosticFieldCandidate {
            val support = lines(region)
            val numbers = support.filter { it.text.matches(Regex("[0-9]{1,3}")) }
            val value = numbers.singleOrNull()?.text?.toIntOrNull()?.takeIf { it in 1..999 }
            return DiagnosticFieldCandidate(field,
                value = if (gate == null && !(expanded && field == "friendly_health") && support.size == 1 && value != null) value.toString() else null,
                reason = gate ?: if (expanded && field == "friendly_health") "expanded_hand_occlusion" else if (support.size == 1 && value != null) "single_numeric_candidate" else "missing_or_ambiguous_numeric",
                region = region, support = support)
        }
        val manaRegion = if (expanded) verticalMana else friendlyMana
        val manaLines = lines(manaRegion)
        val manaMatch = manaLines.singleOrNull()?.text?.trim()?.let { Regex("([0-9]{1,2})/([0-9]{1,2})").matchEntire(it) }
        val manaValue = manaMatch?.let {
            val current = it.groupValues[1].toInt()
            val total = it.groupValues[2].toInt()
            if (current <= total && total <= 20) "$current/$total" else null
        }
        val corpseRegion = if (expanded) verticalCorpses else normalCorpses
        val corpseLines = lines(corpseRegion).filter { it.left >= corpseRegion.left && it.right <= corpseRegion.right }
        val corpseValue = corpseLines.singleOrNull()?.text?.takeIf { it.matches(Regex("[0-9]{1,3}")) }
        return listOf(turn, layout, health("friendly_health", friendlyHealth), health("opponent_health", opponentHealth),
            DiagnosticFieldCandidate("friendly_mana",
                value = if (gate == null) manaValue else null,
                reason = gate ?: if (manaValue != null) "exact_resource_pair" else "missing_or_merged_resource_text",
                region = manaRegion, support = manaLines),
            DiagnosticFieldCandidate("friendly_adjacent_counter",
                value = if (gate == null) corpseValue else null,
                reason = gate ?: if (corpseValue != null) "untyped_counter_candidate" else "missing_or_merged_counter",
                region = corpseRegion, support = corpseLines))
    }
}
