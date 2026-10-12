package com.stroexd.hsdecktracker.core.fusion

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/** Display only: session names have no timezone and do not establish match membership. */
object PostMatchPresentation {
    const val EXISTING_ARTIFACT_PRESERVED = "Already fused locally; preserved existing artifact"
    private val input = DateTimeFormatter.ofPattern("uuuu_MM_dd_HH_mm_ss", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)
    private val output = DateTimeFormatter.ofPattern("MMM d, uuuu • h:mm:ss a", Locale.US)

    fun sessionLabel(session: String): String {
        val value = session.removePrefix("Hearthstone_")
        return runCatching { LocalDateTime.parse(value, input).format(output) }
            .getOrDefault(session)
    }

    fun resultText(state: FusionImportState, errors: String?, alreadyFused: Boolean = false,
        coverageWarnings: List<String> = emptyList()): String = (when (state) {
        FusionImportState.FUSED_LOCAL -> if (alreadyFused)
            "Already fused locally. Existing artifact preserved; no regeneration or new Shizuku read. No cloud upload was performed."
        else "Fused locally. No cloud upload was performed."
        FusionImportState.PENDING_PAIRING -> "Raw logs saved locally; pairing is pending. ${errors.orEmpty()}"
        else -> "Raw logs saved locally; fusion did not complete. ${errors.orEmpty()}"
    }) + if (coverageWarnings.isEmpty()) "" else "\n" + coverageWarnings.joinToString("\n")
}
