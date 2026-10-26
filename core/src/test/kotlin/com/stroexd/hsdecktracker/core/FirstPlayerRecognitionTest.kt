package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.tracker.GameEvent
import com.stroexd.hsdecktracker.core.vision.CardNameIndex
import com.stroexd.hsdecktracker.core.vision.OcrFrame
import com.stroexd.hsdecktracker.core.vision.OcrLine
import com.stroexd.hsdecktracker.core.vision.VisionGameTracker
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class FirstPlayerRecognitionTest {
    private fun line(text: String, x: Float, y: Float) = OcrLine(text, x - .03f, y - .01f, x + .03f, y + .01f)
    private val intro = listOf(line("MAGE", .31f, .71f), line("ROGUE", .69f, .71f))
    private fun tracker() = VisionGameTracker(CardNameIndex(emptyList()))
    private val opening = listOf(line("Starting Hand", .49f, .125f), line("You get an extra card", .6f, .2f))
    private fun finish(tracker: VisionGameTracker, at: Long = 2000) =
        tracker.onFrame(OcrFrame(at, listOf(line("Your Turn", .49f, .5f))))

    @Test fun ambiguousCoinDoesNotEstablishSecondPlayer() {
        val tracker = tracker()
        tracker.onFrame(OcrFrame(0, intro))
        tracker.onFrame(OcrFrame(500, listOf(line("The Coin", .8f, .65f))))
        val events = tracker.onFrame(OcrFrame(1000, listOf(line("Your Turn", .49f, .5f))))
        assertTrue(events.none { it is GameEvent.TurnOrderDetected }, "A card popup does not prove starting order")
    }

    @Test fun missedOpeningTurnDoesNotEstablishStartingOrder() {
        val tracker = tracker()
        tracker.onFrame(OcrFrame(0, intro))
        val events = (0..2).flatMap { tracker.onFrame(OcrFrame(60_000L + it * 500, listOf(line("ENEMY TURN", .8f, .49f)))) }
        assertTrue(events.none { it is GameEvent.TurnOrderDetected }, "First observed turn may follow missed actual turns")
    }

    @Test fun explicitRepeatedOpeningMessageEstablishesSecondPlayer() {
        val tracker = tracker()
        tracker.onFrame(OcrFrame(0, intro))
        tracker.onFrame(OcrFrame(500, opening))
        tracker.onFrame(OcrFrame(1000, opening))
        assertEquals(listOf(GameEvent.TurnOrderDetected(false)), finish(tracker).filterIsInstance<GameEvent.TurnOrderDetected>())
        val later = tracker.onFrame(OcrFrame(2500, listOf(line("END TURN", .8f, .49f))))
        assertTrue(later.none { it is GameEvent.TurnOrderDetected })
    }

    @Test fun transientInterruptedOrGappedMessagesStayUnknown() {
        for (middle in listOf(OcrFrame(750, emptyList()), OcrFrame(500, opening))) {
            val tracker = tracker()
            tracker.onFrame(OcrFrame(0, intro))
            tracker.onFrame(OcrFrame(500, opening))
            tracker.onFrame(middle)
            assertTrue(finish(tracker).none { it is GameEvent.TurnOrderDetected })
        }
        val tracker = tracker()
        tracker.onFrame(OcrFrame(0, intro))
        tracker.onFrame(OcrFrame(500, opening))
        tracker.onFrame(OcrFrame(4000, opening))
        assertTrue(finish(tracker, 4500).none { it is GameEvent.TurnOrderDetected })
    }

    @Test fun ambiguousRegionSubstringOrMissingOpeningContextStaysUnknown() {
        for (candidate in listOf(
            listOf(line("Starting Hand", .49f, .125f), line("The Coin", .7f, .9f)),
            listOf(line("Starting Hand", .49f, .125f), line("You get an extra card", .2f, .2f)),
            listOf(line("Starting Hand", .49f, .125f), line("You get an extra card later", .6f, .2f)),
            listOf(line("You get an extra card", .6f, .2f)),
        )) {
            val tracker = tracker()
            tracker.onFrame(OcrFrame(0, intro))
            tracker.onFrame(OcrFrame(500, candidate))
            tracker.onFrame(OcrFrame(1000, candidate))
            assertTrue(finish(tracker).none { it is GameEvent.TurnOrderDetected })
        }
    }

    @Test fun lateOpeningAndGameplayCoinStayUnknown() {
        val stale = tracker()
        stale.onFrame(OcrFrame(0, intro))
        stale.onFrame(OcrFrame(61_000, opening))
        stale.onFrame(OcrFrame(61_500, opening))
        assertTrue(finish(stale, 62_000).none { it is GameEvent.TurnOrderDetected })
        val playing = tracker()
        playing.onFrame(OcrFrame(0, intro))
        finish(playing)
        val later = (0..2).flatMap { playing.onFrame(OcrFrame(3000L + it * 500, opening + line("The Coin", .7f, .9f))) }
        assertTrue(later.none { it is GameEvent.TurnOrderDetected })
    }

    @Test fun nextGameDoesNotInheritOpeningEvidence() {
        val tracker = tracker()
        tracker.onFrame(OcrFrame(0, intro))
        tracker.onFrame(OcrFrame(500, opening))
        tracker.onFrame(OcrFrame(1000, opening))
        finish(tracker)
        repeat(2) { tracker.onFrame(OcrFrame(3000L + it * 500, listOf(line("Victory!", .5f, .65f).copy(top=.62f, bottom=.68f)))) }
        tracker.onFrame(OcrFrame(20_000, intro))
        assertTrue(finish(tracker, 21_000).none { it is GameEvent.TurnOrderDetected })
    }
}
