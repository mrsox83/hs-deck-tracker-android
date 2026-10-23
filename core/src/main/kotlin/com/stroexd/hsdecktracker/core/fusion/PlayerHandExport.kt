package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.fusion.summary.PlayerHandReportRenderer
import java.io.FilterInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/** Read-only, pinned report session. No persistence or observer inference. */
object PlayerHandExport {
    const val MAX_INPUT_BYTES = 32L * 1024 * 1024
    @ConsistentCopyVisibility
    data class Artifact internal constructor(val match: FusedMatch, val sha256: String)
    data class Payload(val filename: String, val text: String)
    data class Checkpoint(val id: String, val label: String, val unique: Boolean)

    fun displayLabel(value: String): String = buildString {
        for(char in value.take(120)) {
            if(char.isISOControl() || Character.getType(char) in setOf(Character.FORMAT.toInt(), Character.SURROGATE.toInt(), Character.LINE_SEPARATOR.toInt(), Character.PARAGRAPH_SEPARATOR.toInt()))
                append("\\u"+char.code.toString(16).padStart(4,'0'))
            else append(char)
        }
    }

    /** Report-only safety cap; larger originals remain available through the existing fusion export. */
    fun load(input: InputStream, maxBytes: Long = MAX_INPUT_BYTES, check: () -> Unit = {}): Artifact {
        require(maxBytes in 1..MAX_INPUT_BYTES)
        val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
        var cancelled: CancellationException? = null
        fun tick() { try { check() } catch (error: CancellationException) { cancelled = error; throw error } }
        val bounded = object : FilterInputStream(input) {
            override fun read(): Int {
                tick()
                val value = `in`.read()
                if (value >= 0) { require(++count <= maxBytes) { "Hand report input exceeds $maxBytes bytes; original fusion is unchanged" }; digest.update(value.toByte()) }
                return value
            }
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                tick()
                val size = `in`.read(bytes, offset, minOf(length.toLong(), maxBytes - count + 1).toInt())
                if (size > 0) { count += size; require(count <= maxBytes) { "Hand report input exceeds $maxBytes bytes; original fusion is unchanged" }; digest.update(bytes, offset, size) }
                return size
            }
        }
        val match = try { FusionArtifactCodec.decodeFromStream(bounded) } catch (error: Exception) {
            cancelled?.let { throw it }; throw error
        }
        val buffer = ByteArray(8192)
        while (bounded.read(buffer) >= 0) tick()
        tick()
        return Artifact(match, digest.digest().joinToString("") { "%02x".format(it) })
    }

    fun checkpoints(artifact: Artifact): List<Checkpoint> {
        val counts = artifact.match.snapshots.groupingBy { it.id }.eachCount()
        return artifact.match.snapshots.map {
            val line = it.evidence.mapNotNull { ref -> ref.sourceLine }.distinct().singleOrNull()
            val phase=if(it.phase=="BEFORE_ACTION") "Before logged action (not verified decision time)" else displayLabel(it.phase)
            Checkpoint(it.id, "Epoch ${it.chronologyEpoch} · turn ${it.rawTurn ?: "unknown"} · $phase · line ${line ?: "unknown"}", counts[it.id] == 1)
        }
    }

    fun prepare(artifact: Artifact, checkpointId: String, playerController: Int, check: () -> Unit = {}): Payload {
        val snapshot = artifact.match.snapshots.singleOrNull { it.id == checkpointId }
            ?: error("Choose one unique checkpoint")
        val header = "Artifact SHA256: ${artifact.sha256}\n"
        val text = header + PlayerHandReportRenderer.render(artifact.match, checkpointId, playerController,
            maxChars = 65_536 - header.length, check = check)
        fun token(value: String) = value.replace(Regex("[^A-Za-z0-9_-]"), "-").take(32).ifEmpty { "unknown" }
        val filename = "player-hand_${token(artifact.match.matchId)}_r${artifact.match.revision}_e${snapshot.chronologyEpoch}_${token(checkpointId)}_c${playerController}_${artifact.sha256.take(12)}.txt"
        return Payload(filename, text)
    }
}
