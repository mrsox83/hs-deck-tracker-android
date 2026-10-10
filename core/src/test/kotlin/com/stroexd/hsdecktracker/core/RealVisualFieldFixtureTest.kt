package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.data.VisualEvidenceEntry
import com.stroexd.hsdecktracker.core.vision.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Requires the labeled private calibration archive; never silently skips a missing fixture. */
class RealVisualFieldFixtureTest {
    @Test fun `focused hand hides health and incomplete vertical resources remain unknown`() {
        ZipFile(requireNotNull(System.getProperty("hsVisualExpandedFixtureZip"))).use { zip ->
            val metadata = requireNotNull(zip.getEntry("visual-evidence/visual-evidence.json"))
            val records = Json { ignoreUnknownKeys = true }.decodeFromString(
                ListSerializer(VisualEvidenceEntry.serializer()),
                zip.getInputStream(metadata).bufferedReader().use { it.readText() },
            )
            val bookmarks = records.filter { it.reason == "manual_bookmark" && it.keyframe != null }.sortedBy { it.observedAt }
            assertEquals(11, bookmarks.size, "Use the labeled seven-new-bookmark archive")
            bookmarks.takeLast(7).forEachIndexed { index, entry ->
                val k = requireNotNull(entry.keyframe)
                val frame = OcrFrame(entry.observedAt, k.regionProbes.flatMap { it.lines }.distinct(), k.width.toFloat() / k.height)
                val result = VisualFieldPilot.read(frame, k.regionOcrReused).associateBy { it.field }
                if (index == 0 || index == 6) {
                    assertEquals("30", result.getValue("friendly_health").value)
                } else {
                    assertEquals("expanded_hand_candidate", result.getValue("hand_layout").value)
                    assertNull(result.getValue("friendly_health").value)
                }
                assertNull(result.getValue("friendly_mana").value)
                assertNull(result.getValue("opponent_health").value)
            }
        }
    }
    @Test fun `saved gameplay health labels replay and merged counters remain unknown`() {
        val path = requireNotNull(System.getProperty("hsVisualFixtureZip"))
        ZipFile(path).use { zip ->
            val metadata = requireNotNull(zip.getEntry("visual-evidence/visual-evidence.json"))
            val records = Json { ignoreUnknownKeys = true }.decodeFromString(
                ListSerializer(VisualEvidenceEntry.serializer()),
                zip.getInputStream(metadata).bufferedReader().use { it.readText() },
            )
            val bookmarks = records.filter { it.reason == "manual_bookmark" && it.keyframe != null }
                .sortedBy { it.observedAt }
            assertEquals(4, bookmarks.size, "Use the labeled opening-hand plus three-gameplay archive")
            fun replay(entry: VisualEvidenceEntry): Map<String, DiagnosticFieldCandidate> {
                val keyframe = requireNotNull(entry.keyframe)
                assertNotNull(zip.getEntry("visual-evidence/${keyframe.relativePath}"))
                val lines = keyframe.regionProbes.flatMap { it.lines }.distinct()
                val frame = OcrFrame(entry.observedAt, lines, keyframe.width.toFloat() / keyframe.height)
                return VisualFieldPilot.read(frame, keyframe.regionOcrReused).associateBy { it.field }
            }
            assertNull(replay(bookmarks.first()).getValue("friendly_health").value)
            bookmarks.takeLast(3).zip(listOf("26", "26", "15")).forEach { (entry, health) ->
                val result = replay(entry)
                assertEquals(health, result.getValue("friendly_health").value)
                assertEquals("enemy_turn_label", result.getValue("turn_affordance").value)
                assertNull(result.getValue("friendly_mana").value)
                assertNull(result.getValue("opponent_health").value)
                assertEquals("experimental", result.getValue("friendly_health").stage)
            }
        }
    }
}
