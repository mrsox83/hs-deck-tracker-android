package com.stroexd.hsdecktracker.core.vision

import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

/** Reasons are observations, never diagnoses of redaction, occlusion or missing gameplay. */
@Serializable
enum class CaptureDiagnosticKind {
    SESSION, SOURCE_START, SOURCE_STOP, VISIBILITY, RESIZE, VIEWPORT,
    REQUEST, SKIPPED, TIMEOUT, ATTACH_FALLBACK, ACQUIRE_FAILED, NO_IMAGE,
    UNREQUESTED_IMAGE, COPY_FAILED, SOURCE_FAILED, DELIVERED, EMPTY_CAPTURE, FINGERPRINT_FAILED,
    OCR_START, OCR_SUCCESS, OCR_FAILED, OCR_REUSED, FRAME_QUALITY, STOPPED, LIMIT,
}

@Serializable
data class CaptureDiagnostic(
    val schema: String = "capture-diagnostics/1",
    val sessionId: String,
    val sequence: Int,
    val wallTimeMillis: Long,
    val elapsedMillis: Long,
    val kind: CaptureDiagnosticKind,
    val values: Map<String, Long>,
)

/** Fixed metadata-only budget. Never stores pixels, OCR text, exception messages or personal identifiers. */
class CaptureDiagnosticLog(
    private val file: File,
    private val wallClock: () -> Long,
    private val elapsedClock: () -> Long,
    private val maxEvents: Int = 10_000,
    private val maxBytes: Int = 2 * 1024 * 1024,
    private val sessionId: String = UUID.randomUUID().toString(),
) {
    private var sequence = 0
    private var bytesWritten = 0
    private var exhausted = false

    init { require(maxEvents > 0 && maxBytes > 0) }

    private fun append(bytes: ByteArray) {
        try {
            file.parentFile?.mkdirs()
            file.appendBytes(bytes)
        } catch (error: Exception) {
            exhausted = true
            throw error
        }
    }

    @Synchronized
    fun record(kind: CaptureDiagnosticKind, values: Map<String, Long> = emptyMap()) {
        if (exhausted) return
        require(values.size <= 24 && values.keys.all { it.matches(Regex("[a-zA-Z][a-zA-Z0-9_]{0,47}")) })
        val line = AppJson.encodeToString(CaptureDiagnostic.serializer(), CaptureDiagnostic(
            sessionId = sessionId, sequence = sequence + 1, wallTimeMillis = wallClock(),
            elapsedMillis = elapsedClock(), kind = kind, values = values.toMap(),
        )).toByteArray(Charsets.UTF_8) + byteArrayOf(10)
        // Account for sequential reopen; each live recorder must own a distinct file.
        val existing = maxOf(bytesWritten.toLong(), file.takeIf { it.exists() }?.length() ?: 0L)
        val limitLine = AppJson.encodeToString(CaptureDiagnostic.serializer(), CaptureDiagnostic(
            sessionId = sessionId, sequence = sequence + 1, wallTimeMillis = wallClock(),
            elapsedMillis = elapsedClock(), kind = CaptureDiagnosticKind.LIMIT, values = emptyMap(),
        )).toByteArray(Charsets.UTF_8) + byteArrayOf(10)
        if (sequence >= maxEvents - 1 || line.size + limitLine.size > maxBytes - existing) {
            exhausted = true
            if (sequence < maxEvents && limitLine.size <= maxBytes - existing) {
                append(limitLine)
            }
            return
        }
        append(line)
        sequence++
        bytesWritten = (existing + line.size).toInt()
    }
}
