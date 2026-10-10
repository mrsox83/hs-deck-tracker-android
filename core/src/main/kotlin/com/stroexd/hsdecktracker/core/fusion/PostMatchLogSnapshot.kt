package com.stroexd.hsdecktracker.core.fusion

import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/** R5's bounded, read-only snapshot primitive. No polling, parser, permissions or destination writes. */
object PostMatchLogSnapshot {
    const val MAX_BYTES: Long = 16L * 1024 * 1024
    private val sessionPattern = Regex("Hearthstone_[0-9_]+")

    fun requireSession(session: String) {
        require(sessionPattern.matches(session)) { "Invalid Hearthstone session name" }
    }

    /**
     * The caller must supply size and stream from the SAME opened descriptor, not a separate path stat.
     * Copies only the observed prefix: subsequent appends are excluded. A short read fails instead of
     * publishing partial evidence. This cannot detect an equal-length in-place rewrite or certify
     * match completeness; source identity is the hash of the actual copied bytes.
     */
    fun read(
        session: String,
        observedBytes: Long,
        openStream: () -> InputStream,
        cancelled: () -> Boolean = { false },
    ): Snapshot {
        requireSession(session)
        require(observedBytes in 1..MAX_BYTES) { "Log size must be known, non-empty and at most $MAX_BYTES bytes" }
        checkCancellation(cancelled)
        val bytes = ByteArray(observedBytes.toInt())
        openStream().use { input ->
            var offset = 0
            while (offset < bytes.size) {
                checkCancellation(cancelled)
                val count = input.read(bytes, offset, minOf(64 * 1024, bytes.size - offset))
                if (count < 0) throw java.io.EOFException("Log shortened during snapshot: $offset of $observedBytes bytes")
                if (count == 0) throw java.io.IOException("Log read made no progress")
                offset += count
            }
            checkCancellation(cancelled)
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return Snapshot(session, observedBytes, hash, bytes)
    }

    private fun checkCancellation(cancelled: () -> Boolean) {
        if (Thread.currentThread().isInterrupted || cancelled()) throw CancellationException("Log snapshot cancelled")
    }

    class Snapshot internal constructor(
        val session: String,
        val sourceBytes: Long,
        val sourceSha256: String,
        private val content: ByteArray,
    ) {
        // Session is provenance, not a deduplication key: a rotated name can contain identical bytes.
        val contentIdentity: String get() = sourceSha256
        fun copyBytes(): ByteArray = content.copyOf()
    }
}
