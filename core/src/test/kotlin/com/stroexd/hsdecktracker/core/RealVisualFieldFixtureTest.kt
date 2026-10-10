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
    @Test fun `v5 stacked resources replay only complete fresh pairs`() {
        ZipFile(requireNotNull(System.getProperty("hsVisualResourceFixtureZip"))).use { zip ->
            val records = Json { ignoreUnknownKeys = true }.decodeFromString(
                ListSerializer(VisualEvidenceEntry.serializer()),
                zip.getInputStream(requireNotNull(zip.getEntry("visual-evidence/visual-evidence.json")))
                    .bufferedReader().use { it.readText() },
            )
            val bookmarks = records.filter { it.reason == "manual_bookmark" &&
                it.requestedCaptureMode == com.stroexd.hsdecktracker.core.data.VisualCaptureMode.AUTOMATIC_ROUTE }
                .sortedBy { it.observedAt }
            assertEquals(9, bookmarks.size)
            val expected = listOf(null, null, null, null, "1/2", null, "10/10", "3/10", null)
            bookmarks.forEachIndexed { index, entry ->
                val k = requireNotNull(entry.keyframe)
                assertNotNull(zip.getEntry("visual-evidence/${k.relativePath}"))
                assertEquals("media_projection", entry.captureMethod)
                val result = VisualFieldPilot.read(OcrFrame(entry.observedAt,
                    k.regionProbes.flatMap { it.lines }.distinct(), k.width.toFloat() / k.height),
                    k.regionOcrReused).associateBy { it.field }
                assertEquals(expected[index], result.getValue("friendly_mana").value, "Bookmark ${index + 1}")
                val candidate = result.getValue("friendly_mana")
                val captureTime = k.regionProbes.first().observedAt
                val humanLabel = listOf("0/2", "1/1", "1/1", "2/2", "1/2", "0/6", "10/10", "3/10", null)[index]
                val annotation = humanLabel?.let {
                    VisualFieldReview(k.sha256, captureTime, "friendly_mana", it,
                        VisualReviewKind.HUMAN_IMAGE_LABEL, k.sha256, "/human-image-labels/bookmark-${index + 1}",
                        "HS-FUSION labeled image review")
                }
                val assessment = VisualFieldValidation.assess(k.sha256, captureTime, candidate,
                    k.regionOcrReused, listOfNotNull(annotation))
                assertEquals(if (expected[index] == null) "experimental" else "corroborating", assessment.stage)
                assertEquals(expected[index], assessment.value)
                if (index in 5..7) assertEquals(listOf("8", "11", "16")[index - 5],
                    result.getValue("friendly_adjacent_counter").value)
                assertEquals("experimental", result.getValue("friendly_mana").stage)
            }
        }
    }
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
