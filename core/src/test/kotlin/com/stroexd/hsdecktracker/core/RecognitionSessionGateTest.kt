package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.RecognitionSessionGate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecognitionSessionGateTest {
    @Test fun `replacement survives old stop and rejects late frames`() {
        val gate = RecognitionSessionGate()
        val old = gate.start()
        val replacement = gate.start()
        assertFalse(gate.stop(old))
        assertFalse(gate.isCurrent(old))
        assertTrue(gate.isCurrent(replacement))
        assertTrue(gate.stop(replacement))
        assertFalse(gate.isCurrent(replacement))
        assertFalse(gate.stop(replacement))
    }
}
