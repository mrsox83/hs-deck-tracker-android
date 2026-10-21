package com.stroexd.hsdecktracker.core.fusion.summary

import com.stroexd.hsdecktracker.core.fusion.*

/** Derived position-only report. Selection is caller supplied, never verified observer identity. */
object PlayerHandReportRenderer {
    fun render(match: FusedMatch, checkpointId: String, playerController: Int,
        maxChars: Int = 65_536, check: () -> Unit = {}): String {
        require(playerController > 0) { "Explicit positive player controller required" }
        require(maxChars in 1..1_048_576) { "maxChars must be in 1..1048576" }
        check()
        val view = DecisionHandProjector.project(match, checkpointId, setOf(playerController), check)
        val hand = view.hands.single()
        val output = StringBuilder(minOf(maxChars, 4096))
        fun append(value: String) {
            check()
            require(value.length <= maxChars - output.length) { "Player hand report exceeds maxChars" }
            output.append(value)
        }
        fun data(value: String) {
            for (char in value) {
                check()
                append(when {
                    char.isISOControl() || Character.getType(char) in setOf(Character.FORMAT.toInt(),
                        Character.LINE_SEPARATOR.toInt(), Character.PARAGRAPH_SEPARATOR.toInt(), Character.SURROGATE.toInt()) ->
                        "\\u" + char.code.toString(16).padStart(4, '0')
                    char.isLetterOrDigit() || char == ' ' -> char.toString()
                    else -> "&#${char.code};"
                })
            }
        }
        fun field(label: String, value: String?) {
            append(label); data(value ?: "UNKNOWN"); append("\n")
        }
        append("# Observed player hand positions\nView: player-hand-positions/1\n")
        append("Derived source checkpoint, not certified pre-decision knowledge.\n")
        append("Player controller is CALLER_SELECTED, not verified local observer. Opponent hand not included.\n")
        append("Identities and private costs withheld. Incomplete evidence is not human ignorance or an empty hand.\n")
        field("Match: ", match.matchId)
        field("Revision: ", match.revision.toString())
        field("Fusion version: ", match.fusionVersion)
        field("Schema: ", match.schema)
        field("Checkpoint: ", view.checkpointId)
        field("Phase: ", view.phase)
        field("Epoch: ", view.chronologyEpoch.toString())
        field("Raw turn: ", view.rawTurn?.toString())
        field("Active controller: ", view.activeController?.toString())
        for ((controller, index) in view.playerTurnIndices.toSortedMap()) {
            field("Controller $controller turn index: ", index.toString())
        }
        field("Unresolved block: ", view.unresolvedBlock.toString())
        field("Chronology state: ", view.chronologyState.toString())
        field("Selected player controller: ", hand.controller.toString())
        append("Expected hand size: UNKNOWN\nHand completeness: UNKNOWN\n")
        for (warning in view.diagnostics + hand.diagnostics) field("Warning: ", warning)
        if (hand.slots.isEmpty()) append("No evidenced slots in this view; this does not mean an empty hand.\n")
        for (slot in hand.slots) {
            field("Slot position: ", slot.position.toString())
            field("  Entity reference: ", slot.entityId)
            append("  Card identity: UNKNOWN\n  Effective cost: UNKNOWN\n")
            for (ref in slot.evidence) {
                field("  Source ID: ", ref.sourceId)
                field("  Source line: ", ref.sourceLine?.toString())
                field("  Pointer: ", ref.jsonPointer)
                field("  Rule: ", ref.rule)
            }
        }
        check()
        return output.toString()
    }
}
