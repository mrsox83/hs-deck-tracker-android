package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchRecord
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

    enum class FusionConsistency { CURRENT, REVISION_STALE, RESULT_CONFLICT, UNKNOWN_LEGACY }

    data class FusionConsistencyNotice(val consistency: FusionConsistency, val message: String)

    /** Compares saved truth with the immutable tracker record incorporated at fusion time. */
    fun fusionConsistency(record: MatchRecord, entry: FusionImportEntry): FusionConsistencyNotice {
        val fusedRevision = entry.fusedRecordRevision
        val fusedResult = entry.fusedRecordResult
        if (fusedRevision == null || fusedResult == null) return FusionConsistencyNotice(
            FusionConsistency.UNKNOWN_LEGACY,
            "Fusion provenance predates saved-result revision tracking. The artifact is preserved, but current-result agreement is unverified.",
        )
        if (fusedResult != record.result) return FusionConsistencyNotice(
            FusionConsistency.RESULT_CONFLICT,
            "Result conflict: preserved fusion used $fusedResult from saved revision $fusedRevision; current saved revision ${record.revision} is ${record.result}. Both values and the user-edit history remain preserved. A separately approved re-fusion path would be required before treating the artifact as current.",
        )
        if (fusedRevision != record.revision) return FusionConsistencyNotice(
            FusionConsistency.REVISION_STALE,
            "Saved match changed after fusion (artifact revision $fusedRevision; current revision ${record.revision}). Result remains ${record.result}; the preserved artifact is not silently regenerated.",
        )
        return FusionConsistencyNotice(
            FusionConsistency.CURRENT,
            "Fusion matches saved revision ${record.revision} and result ${record.result}.",
        )
    }

    /** Compact UI label only; the full original message remains available in status details. */
    fun statusSummary(message: String): String {
        val summary = when {
            message.startsWith("Already fused locally.") -> "Already fused locally; artifact preserved. No cloud upload."
            message.startsWith("Fused locally.") -> "Fused locally. No cloud upload."
            message.startsWith("Raw logs saved locally; pairing is pending.") -> "Logs saved locally; pairing pending."
            else -> message.substringBefore('\n').substringBefore(". ").take(140)
        }
        return summary + if (message.contains("Partial reconstruction:")) " Coverage warning: see details." else ""
    }

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
