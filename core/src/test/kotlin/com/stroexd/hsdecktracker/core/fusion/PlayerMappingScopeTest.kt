package com.stroexd.hsdecktracker.core.fusion

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class PlayerMappingScopeTest {
    @Test fun laterGameControllerSwapCannotRewriteEarlierGame() {
        val raw = (1..24).map { line -> when (line) {
            4 -> "PlayerID=1, PlayerName=Local"
            5 -> "PlayerID=2, PlayerName=OpponentA"
            16 -> "PlayerID=2, PlayerName=Local"
            17 -> "PlayerID=1, PlayerName=OpponentB"
            else -> "line $line"
        } }.joinToString("\n")
        fun document(index: Int, offset: Int, first: String, second: String): String {
            fun event(line: Int, body: String, extra: String = "") =
                """{"sequence":$line,"source_line":${line + offset},"raw":"$body"$extra}"""
            val events = listOf(
                event(1, "Player EntityID=2 PlayerID=1"),
                event(2, "Player EntityID=3 PlayerID=2"),
                event(3, "tag", """, "entity":"$first","tag":"CURRENT_PLAYER","value":"1""""),
                event(6, "tag", """, "entity":"$first","tag":"CURRENT_PLAYER","value":"1""""),
                event(8, "tag", """, "entity":"$second","tag":"CURRENT_PLAYER","value":"1""""),
                event(10, "tag", """, "entity":"$first","tag":"CURRENT_PLAYER","value":"1""""),
                event(12, "tag", """, "entity":"$second","tag":"CURRENT_PLAYER","value":"1""""),
            )
            return """{"schema":"hs-power-evidence/0.3","source_session":"session-a","match":{"index":$index,"events":[${events.joinToString()}]}}"""
        }
        val entries = mapOf("session-a/Power.log" to raw,
            "session-a/match-001.json" to document(1, 0, "Local", "OpponentA"),
            "session-a/match-002.json" to document(2, 12, "OpponentB", "Local"))
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip -> entries.forEach { (name, text) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry()
        } }
        val reduced = EvidenceAdapters.exporterBundle(output.toByteArray()).matches.map { PowerEvidenceReducer.reduce(it.first, it.second) }
        for (game in reduced) {
            val turns = game.snapshots.filter { it.phase == "TURN_SIGNAL" }
            assertEquals(listOf(null, 1, 2, 1, 2), turns.map { it.activeController })
            assertTrue(turns.last().playerTurnIndices.isEmpty(), "Unknown turn ownership must not fabricate later indices")
            assertTrue(game.entities.values.flatMap { it.aliasHistory }.all { observation ->
                observation.evidence.sourceLine!! in (if (game.index == 1) 1..12 else 13..24)
            })
        }
    }
}
