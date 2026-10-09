package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.TestCards.FIREBALL
import com.stroexd.hsdecktracker.core.TestCards.LEEROY
import com.stroexd.hsdecktracker.core.TestCards.RARE_NEUTRAL
import com.stroexd.hsdecktracker.core.cards.GameFormat
import com.stroexd.hsdecktracker.core.cards.HsClass
import com.stroexd.hsdecktracker.core.deck.Deck
import com.stroexd.hsdecktracker.core.stats.MatchResult
import com.stroexd.hsdecktracker.core.stats.MatchSource
import com.stroexd.hsdecktracker.core.stats.TimelineType
import com.stroexd.hsdecktracker.core.tracker.GameEvent
import com.stroexd.hsdecktracker.core.tracker.TrackerController
import com.stroexd.hsdecktracker.core.tracker.TrackerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals

class TrackerTest {
    private val deck = Deck(
        id = "deck-1",
        name = "Test Mage",
        heroClass = HsClass.MAGE,
        format = GameFormat.STANDARD,
        cards = mapOf(FIREBALL to 2, LEEROY to 1, RARE_NEUTRAL to 2),
    )

    @Test
    fun controllerAppliesEventsToSelectedDeck() {
        val controller = TrackerController(clock = { 1_000L })
        controller.start(deck)
        listOf(
            GameEvent.GameStarted,
            GameEvent.ClassDetected(friendly = false, hsClass = HsClass.WARRIOR),
            GameEvent.FriendlyCardSeen(listOf(FIREBALL)),
            GameEvent.FriendlyCardSeen(listOf(LEEROY)),
            GameEvent.FriendlyCardMulliganed(listOf(LEEROY)),
            GameEvent.FriendlyCardPlayed(listOf(FIREBALL)),
            GameEvent.TurnOrderDetected(friendlyWentFirst = true),
            GameEvent.TurnChanged(1),
            GameEvent.TurnChanged(2),
            GameEvent.OpponentCardSeen(listOf(RARE_NEUTRAL)),
            GameEvent.TurnChanged(3),
        ).forEach { controller.onGameEvent(it, TestCards.db) }

        val state = assertNotNull(controller.state.value)
        assertEquals(1, state.remainingOf(FIREBALL))
        assertEquals(1, state.remainingOf(LEEROY))
        assertEquals(listOf(RARE_NEUTRAL), state.opponentCards)
        assertEquals(HsClass.WARRIOR, state.opponentClass)
        assertEquals(true, state.wentFirst)
        assertEquals(2, state.turn)

        val record = assertNotNull(controller.onGameEvent(GameEvent.GameEnded(MatchResult.WIN), TestCards.db))
        assertEquals(MatchResult.WIN, record.result)
        assertEquals(HsClass.WARRIOR, record.opponentClass)
        assertEquals("deck-1", record.deckId)
        assertEquals(MatchSource.AUTO, record.source)
        assertEquals(
            listOf(FIREBALL),
            record.timeline.filter { it.type == TimelineType.PLAYER_PLAY }.mapNotNull { it.dbfId },
        )
        assertEquals(5, controller.state.value?.remainingCount)
    }

    @Test
    fun manualTrackingDrawUndoAndReturn() {
        var state = TrackerState.start(deck, now = 0)
        assertEquals(5, state.remainingCount)
        assertEquals(0.4, state.nextDrawChance(FIREBALL))
        state = state.draw(FIREBALL).draw(FIREBALL).draw(FIREBALL)
        assertEquals(0, state.remainingOf(FIREBALL))
        assertEquals(2, state.drawHistory.size)
        state = state.undoLastDraw()
        assertEquals(1, state.remainingOf(FIREBALL))
        state = state.returnToDeck(FIREBALL).returnToDeck(FIREBALL)
        assertEquals(2, state.remainingOf(FIREBALL))
        state = state.addOpponentCard(LEEROY).addOpponentCard(FIREBALL).removeOpponentCardAt(0)
        assertEquals(listOf(FIREBALL), state.opponentCards)
        val record = state.withOpponentClass(HsClass.ROGUE).nextTurn().toMatchRecord(MatchResult.LOSS, now = 60_000)
        assertEquals(60, record.durationSeconds)
        assertEquals(2, record.turns)
        assertEquals(MatchSource.TRACKER, record.source)
    }

    @Test
    fun restoredDraftKeepsItsCompletionIdentityAndTransitionsInOrder() {
        var now = 1_000L
        val original = TrackerState.start(deck, now).copy(draftId = "draft-stable").draw(FIREBALL)
        val controller = TrackerController(clock = { now })
        val transitions = mutableListOf<Pair<TrackerState?, TrackerState?>>()
        controller.onStateChanged = { previous, current -> transitions += previous to current }

        controller.restore(original)
        controller.update { it.nextTurn() }
        now = 61_000L
        val record = assertNotNull(controller.finishGame(MatchResult.WIN))

        assertEquals("draft-stable", record.id)
        assertEquals(2, record.turns)
        assertEquals(false, controller.state.value?.draftActive)
        assertEquals(listOf(null, original, original.nextTurn()), transitions.map { it.first })
        assertNotEquals("draft-stable", controller.state.value?.draftId)
    }
}
