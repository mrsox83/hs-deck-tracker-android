package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VisualFieldPilotTest {
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
}
