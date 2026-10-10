package com.stroexd.hsdecktracker.core.vision

import kotlinx.serialization.Serializable

@Serializable
data class PilotRegion(val id: String, val group: String, val bounds: NormalizedRegion)

/** Diagnostic OCR candidates only. These are not recognized game facts or independent witnesses. */
@Serializable
data class RegionProbe(
    val region: PilotRegion,
    val observedAt: Long,
    val lines: List<OcrLine>,
    val stage: String = "experimental",
)

object VisualRegionPilot {
    const val VERSION = "landscape-pilot-1"
    // Initial broad windows require device calibration; empty OCR does not mean an absent game object.
    val regions = listOf(
        PilotRegion("opponent_hero", "hero_state", NormalizedRegion(.30f, .02f, .70f, .30f)),
        PilotRegion("friendly_hero", "hero_state", NormalizedRegion(.30f, .62f, .70f, .96f)),
        PilotRegion("turn_indicator", "turn_resources", NormalizedRegion(.78f, .32f, 1f, .68f)),
        PilotRegion("friendly_resources", "turn_resources", NormalizedRegion(.45f, .76f, 1f, 1f)),
        PilotRegion("opponent_resources", "turn_resources", NormalizedRegion(.45f, 0f, 1f, .20f)),
        PilotRegion("card_play", "event_recovery", NormalizedRegion(.12f, .20f, .78f, .80f)),
        PilotRegion("history_rail", "event_recovery", NormalizedRegion(.15f, .15f, .25f, .88f)),
        PilotRegion("resume_cues", "event_recovery", NormalizedRegion(.20f, .25f, .80f, .75f)),
    )

    fun probe(frame: OcrFrame, configuration: List<PilotRegion> = regions): List<RegionProbe> =
        configuration.map { region ->
            val b = region.bounds
            RegionProbe(region, frame.timestamp, frame.lines.filter { line ->
                line.centerX >= b.left && line.centerX < b.right &&
                    line.centerY >= b.top && line.centerY < b.bottom
            }.take(32))
        }
}
