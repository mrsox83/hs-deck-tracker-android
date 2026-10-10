package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VisualFieldPilotTest {
    private val expandedText = arrayOf(OcrLine("Chillfallen Baron", .50f, .88f, .58f, .92f),
        OcrLine("The Coin", .65f, .90f, .72f, .94f))
    private val turn = OcrLine("ENEMY TURN", .755f, .48f, .812f, .505f)
    private val health = OcrLine("26", .513f, .89f, .553f, .958f)
    private fun read(vararg lines: OcrLine, reused: Boolean = false, aspect: Float = 2.34f) =
        VisualFieldPilot.read(OcrFrame(123, lines.toList(), aspect), reused).associateBy { it.field }

    @Test fun `isolated health excludes adjacent minion numbers and merged mana`() {
        val result = read(turn, health, OcrLine("22", .509f, .647f, .567f, .703f),
            OcrLine("1/7 12", .755f, .772f, .828f, .823f))
        assertEquals("26", result.getValue("friendly_health").value)
        assertEquals("enemy_turn_label", result.getValue("turn_affordance").value)
        assertNull(result.getValue("friendly_mana").value)
        assertNull(result.getValue("opponent_health").value)
    }
    @Test fun `missing gameplay anchor cached OCR and other layouts remain unknown`() {
        assertNull(read(health).getValue("friendly_health").value)
        assertNull(read(turn, health, reused = true).getValue("friendly_health").value)
        assertNull(read(turn, health, aspect = 1.8f).getValue("friendly_health").value)
    }
    @Test fun `conflicting candidates are unknown and exact mana retains support`() {
        val mana = OcrLine("1/7", .755f, .772f, .80f, .823f)
        val result = read(turn, health, health.copy(text = "25"), mana)
        assertNull(result.getValue("friendly_health").value)
        assertEquals("1/7", result.getValue("friendly_mana").value)
        assertEquals(listOf(mana), result.getValue("friendly_mana").support)
        assertEquals("experimental", result.getValue("friendly_mana").stage)
    }
    @Test fun `expanded hand routes resources separately and never invents missing mana numerator`() {
        val result = read(turn, health,
            OcrLine("Chillfallen Baron", .50f, .88f, .58f, .92f),
            OcrLine("The Coin", .65f, .90f, .72f, .94f),
            OcrLine("2", .87f, .84f, .88f, .88f),
            OcrLine("0", .87f, .94f, .88f, .98f))
        assertEquals("expanded_hand_candidate", result.getValue("hand_layout").value)
        assertNull(result.getValue("friendly_health").value)
        assertNull(result.getValue("friendly_mana").value)
        assertEquals("0", result.getValue("friendly_adjacent_counter").value)
    }
    @Test fun `stacked mana supports empty partial full and excludes corpse counter`() {
        for ((current, total) in listOf(0 to 6, 1 to 2, 10 to 10, 3 to 10)) {
            val upper = OcrLine(current.toString(), .85f, .78f, .88f, .82f)
            val lower = OcrLine(total.toString(), .86f, .84f, .89f, .88f)
            val result = read(turn, *expandedText, upper, lower, OcrLine("16", .86f, .92f, .88f, .95f))
            assertEquals("$current/$total", result.getValue("friendly_mana").value)
            assertEquals(listOf(upper, lower), result.getValue("friendly_mana").support)
            assertEquals("16", result.getValue("friendly_adjacent_counter").value)
        }
    }
    @Test fun `stacked mana rejects missing conflicting crossing merged invalid and reused digits`() {
        val upper = OcrLine("3", .85f, .78f, .88f, .82f)
        val lower = OcrLine("10", .86f, .84f, .89f, .88f)
        for (digits in listOf(listOf(upper), listOf(lower), listOf(upper, lower, upper.copy(text = "2")),
            listOf(upper.copy(bottom = .85f), lower), listOf(upper.copy(text = "3 16"), lower),
            listOf(upper.copy(text = "11"), lower), listOf(upper, lower.copy(text = "21")))) {
            assertNull(read(turn, *expandedText, *digits.toTypedArray()).getValue("friendly_mana").value)
        }
        assertNull(read(turn, *expandedText, upper, lower, reused = true).getValue("friendly_mana").value)
        assertNull(read(*expandedText, upper, lower).getValue("friendly_mana").value)
    }
}
