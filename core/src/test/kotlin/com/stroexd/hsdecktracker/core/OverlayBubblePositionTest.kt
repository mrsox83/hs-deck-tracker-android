package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.OverlayLayout
import kotlin.test.*

class OverlayBubblePositionTest {
    @Test fun interiorBubbleDoesNotSnapToEitherSide() {
        assertEquals(400 to 200, OverlayLayout.bubblePosition(400, 200, 1000, 600, 48))
    }
    @Test fun movementAndRotationKeepBubbleReachable() {
        assertEquals(952 to 552, OverlayLayout.bubblePosition(1100, 700, 1000, 600, 48))
        assertEquals(0 to 0, OverlayLayout.bubblePosition(-20, -20, 1000, 600, 48))
        assertEquals(352 to 552, OverlayLayout.bubblePosition(952, 552, 400, 800, 48))
        assertEquals(0 to 0, OverlayLayout.bubblePosition(20, 20, 30, 30, 48))
    }
}
