package com.stroexd.hsdecktracker.core.tracker

import com.stroexd.hsdecktracker.core.cards.GameFormat
import com.stroexd.hsdecktracker.core.cards.HsClass
import com.stroexd.hsdecktracker.core.deck.Deck
import com.stroexd.hsdecktracker.core.stats.DrawOdds
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.stats.MatchResult
import com.stroexd.hsdecktracker.core.stats.MatchSource
import com.stroexd.hsdecktracker.core.stats.RecognitionCoverageInterval
import com.stroexd.hsdecktracker.core.stats.RecognitionCoverageState
import com.stroexd.hsdecktracker.core.stats.TimelineEvent
import com.stroexd.hsdecktracker.core.stats.TimelineType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TrackerState(
    val draftId: String = UUID.randomUUID().toString(),
    val draftActive: Boolean = true,
    val deckId: String?,
    val deckName: String,
    val playerClass: HsClass,
    val format: GameFormat,
    val deckCards: Map<Int, Int>,
    val remaining: Map<Int, Int>,
    val drawHistory: List<Int> = emptyList(),
    val extraDraws: List<String> = emptyList(),
    val opponentClass: HsClass = HsClass.UNKNOWN,
    val opponentCards: List<Int> = emptyList(),
    val turn: Int = 1,
    val wentFirst: Boolean? = null,
    val startedAt: Long,
    val autoTracked: Boolean = false,
    val timeline: List<TimelineEvent> = emptyList(),
    val recognitionCoverage: List<RecognitionCoverageInterval> = emptyList(),
    val recognitionCoverageTruncated: Boolean = false,
) {
    val initialCount: Int get() = deckCards.values.sum()
    val remainingCount: Int get() = remaining.values.sum()
    val drawnCount: Int get() = initialCount - remainingCount

    fun remainingOf(dbfId: Int): Int = remaining[dbfId] ?: 0

    fun nextDrawChance(dbfId: Int): Double = DrawOdds.nextDraw(remainingCount, remainingOf(dbfId))

    private fun event(type: TimelineType, dbfId: Int? = null, cardId: String? = null) =
        TimelineEvent(turn, type, dbfId, cardId)

    fun draw(dbfId: Int): TrackerState {
        val left = remainingOf(dbfId)
        if (left <= 0) return this
        return copy(
            remaining = remaining + (dbfId to left - 1),
            drawHistory = drawHistory + dbfId,
            timeline = timeline + event(TimelineType.DRAW, dbfId),
        )
    }

    fun undoLastDraw(): TrackerState {
        val last = drawHistory.lastOrNull() ?: return this
        val index = timeline.indexOfLast { it.type == TimelineType.DRAW && it.dbfId == last }
        return copy(
            remaining = remaining + (last to remainingOf(last) + 1),
            drawHistory = drawHistory.dropLast(1),
            timeline = if (index >= 0) timeline.toMutableList().apply { removeAt(index) } else timeline,
        )
    }

    fun returnToDeck(dbfId: Int): TrackerState {
        val original = deckCards[dbfId] ?: return this
        val left = remainingOf(dbfId)
        if (left >= original) return this
        val index = drawHistory.lastIndexOf(dbfId)
        val history = if (index >= 0) drawHistory.toMutableList().apply { removeAt(index) } else drawHistory
        return copy(
            remaining = remaining + (dbfId to left + 1),
            drawHistory = history,
            timeline = timeline + event(TimelineType.RETURN, dbfId),
        )
    }

    fun addExtraDraw(cardId: String): TrackerState =
        copy(extraDraws = extraDraws + cardId, timeline = timeline + event(TimelineType.EXTRA_DRAW, cardId = cardId))

    fun addFriendlyCardPlayed(dbfId: Int): TrackerState =
        copy(timeline = timeline + event(TimelineType.PLAYER_PLAY, dbfId))

    fun removeLastExtraDraw(cardIds: Collection<String>): TrackerState {
        val index = extraDraws.indexOfLast { it in cardIds }
        if (index < 0) return this
        val timelineIndex = timeline.indexOfLast { it.type == TimelineType.EXTRA_DRAW && it.cardId == extraDraws[index] }
        return copy(
            extraDraws = extraDraws.toMutableList().apply { removeAt(index) },
            timeline = if (timelineIndex >= 0) timeline.toMutableList().apply { removeAt(timelineIndex) } else timeline,
        )
    }

    fun addOpponentCard(dbfId: Int): TrackerState =
        copy(opponentCards = opponentCards + dbfId, timeline = timeline + event(TimelineType.OPPONENT_PLAY, dbfId))

    fun removeOpponentCardAt(index: Int): TrackerState {
        if (index !in opponentCards.indices) return this
        var seen = -1
        val eventIndex = timeline.indexOfFirst { it.type == TimelineType.OPPONENT_PLAY && ++seen == index }
        return copy(
            opponentCards = opponentCards.toMutableList().apply { removeAt(index) },
            timeline = if (eventIndex >= 0) timeline.toMutableList().apply { removeAt(eventIndex) } else timeline,
        )
    }

    fun withOpponentClass(cls: HsClass): TrackerState = copy(opponentClass = cls)

    fun nextTurn(): TrackerState = copy(turn = turn + 1)

    fun previousTurn(): TrackerState = copy(turn = (turn - 1).coerceAtLeast(1))

    fun withWentFirst(first: Boolean?): TrackerState = copy(wentFirst = first)

    fun observeRecognitionCoverage(at: Long, state: RecognitionCoverageState): TrackerState {
        if (at < startedAt) return this
        val next = recognitionCoverage.toMutableList()
        val last = next.lastOrNull()
        if (last != null && at < last.endedAt) return this
        if (last != null && last.state == RecognitionCoverageState.CAPTURE_GAP) {
            next[next.lastIndex] = last.copy(endedAt = at)
        } else if (last != null && at - last.endedAt > COVERAGE_GAP_MILLIS) {
            next += RecognitionCoverageInterval(last.endedAt, at, RecognitionCoverageState.CAPTURE_GAP)
        }
        val current = next.lastOrNull()
        if (current != null && current.state == state && at >= current.endedAt) {
            next[next.lastIndex] = current.copy(endedAt = at)
        } else {
            next += RecognitionCoverageInterval(at, at, state)
        }
        return boundedCoverage(next)
    }

    fun markRecognitionStopped(at: Long): TrackerState {
        if (at < startedAt) return this
        val next = recognitionCoverage.toMutableList()
        val last = next.lastOrNull()
        if (last != null && at < last.endedAt) return this
        if (last?.state == RecognitionCoverageState.CAPTURE_GAP) return this
        next += RecognitionCoverageInterval(at, at, RecognitionCoverageState.CAPTURE_GAP)
        return boundedCoverage(next)
    }

    private fun boundedCoverage(intervals: List<RecognitionCoverageInterval>): TrackerState {
        if (intervals.size <= MAX_COVERAGE_INTERVALS) return copy(recognitionCoverage = intervals)
        val retained = intervals.take(KEEP_FIRST_COVERAGE_INTERVALS) +
            intervals.takeLast(MAX_COVERAGE_INTERVALS - KEEP_FIRST_COVERAGE_INTERVALS)
        return copy(recognitionCoverage = retained, recognitionCoverageTruncated = true)
    }

    private fun finalizedCoverage(now: Long): List<RecognitionCoverageInterval> {
        val last = recognitionCoverage.lastOrNull() ?: return emptyList()
        return if (last.state == RecognitionCoverageState.CAPTURE_GAP && now > last.endedAt) {
            recognitionCoverage.dropLast(1) + last.copy(endedAt = now)
        } else recognitionCoverage
    }

    fun resetForNewGame(now: Long, active: Boolean = true): TrackerState = copy(
        draftId = UUID.randomUUID().toString(),
        draftActive = active,
        remaining = deckCards,
        drawHistory = emptyList(),
        extraDraws = emptyList(),
        opponentClass = HsClass.UNKNOWN,
        opponentCards = emptyList(),
        turn = 1,
        wentFirst = null,
        startedAt = now,
        timeline = emptyList(),
        recognitionCoverage = emptyList(),
        recognitionCoverageTruncated = false,
    )

    fun toMatchRecord(
        result: MatchResult,
        now: Long,
        source: MatchSource = if (autoTracked) MatchSource.AUTO else MatchSource.TRACKER,
        opponentArchetype: String? = null,
    ): MatchRecord = MatchRecord(
        id = draftId,
        timestamp = now,
        deckId = deckId,
        deckName = deckName,
        playerClass = playerClass,
        opponentClass = opponentClass,
        result = result,
        format = format,
        wentFirst = wentFirst,
        turns = turn,
        durationSeconds = ((now - startedAt) / 1000).toInt().takeIf { it > 0 },
        opponentCards = opponentCards,
        opponentArchetype = opponentArchetype,
        source = source,
        timeline = timeline,
        recognitionCoverage = finalizedCoverage(now),
        recognitionCoverageTruncated = recognitionCoverageTruncated,
    )

    companion object {
        private const val COVERAGE_GAP_MILLIS = 15_000L
        private const val MAX_COVERAGE_INTERVALS = 512
        private const val KEEP_FIRST_COVERAGE_INTERVALS = 64

        fun start(deck: Deck, now: Long): TrackerState = TrackerState(
            deckId = deck.id,
            deckName = deck.name,
            playerClass = deck.heroClass,
            format = deck.format,
            deckCards = deck.cards,
            remaining = deck.cards,
            startedAt = now,
        )

        fun empty(now: Long): TrackerState = TrackerState(
            deckId = null,
            deckName = "",
            playerClass = HsClass.UNKNOWN,
            format = GameFormat.STANDARD,
            deckCards = emptyMap(),
            remaining = emptyMap(),
            startedAt = now,
        )
    }
}
