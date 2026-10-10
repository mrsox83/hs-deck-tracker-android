package com.stroexd.hsdecktracker.vision

import android.graphics.Bitmap
import com.stroexd.hsdecktracker.core.data.VisualKeyframe
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.ArrayDeque

/**
 * Writes only explicitly triggered frames. There is no pre-event or rolling frame buffer: a manual request pins the
 * next frame delivered by the existing recognition loop, while automatic callers must name each trigger.
 */
class VisualEvidenceRecorder(private val root: File) {
    private data class Pending(val id: String, val onSaved: (Result<VisualKeyframe>) -> Unit)

    private val keyframes = File(root, "keyframes")
    private val pending = ArrayDeque<Pending>()
    private var lastAutomaticReason: String? = null
    private var lastAutomaticAt = 0L

    @Synchronized
    fun requestManual(id: String, onSaved: (Result<VisualKeyframe>) -> Unit) {
        if (pending.size >= MAX_PENDING) {
            onSaved(Result.failure(IllegalStateException("Too many pending bookmark frames")))
            return
        }
        pending += Pending(id, onSaved)
    }

    @Synchronized
    fun failPending(message: String) {
        while (pending.isNotEmpty()) pending.removeFirst().onSaved(Result.failure(IllegalStateException(message)))
    }

    /** Called on the recognizer worker. Manual requests and at most one named automatic trigger use this frame. */
    fun onFrame(
        bitmap: Bitmap,
        contentWidth: Int,
        contentHeight: Int,
        observedAt: Long,
        automaticReason: String?,
        onAutomatic: (Result<VisualKeyframe>) -> Unit,
    ) {
        val manual = synchronized(this) { buildList { while (pending.isNotEmpty()) add(pending.removeFirst()) } }
        manual.forEach { request -> request.onSaved(save(bitmap, contentWidth, contentHeight, observedAt, request.id)) }

        if (automaticReason != null && acceptAutomatic(automaticReason, observedAt)) {
            onAutomatic(save(bitmap, contentWidth, contentHeight, observedAt, "auto-$automaticReason"))
        }
    }

    @Synchronized
    private fun acceptAutomatic(reason: String, observedAt: Long): Boolean {
        if (reason == lastAutomaticReason && observedAt - lastAutomaticAt < AUTOMATIC_DEBOUNCE_MS) return false
        lastAutomaticReason = reason
        lastAutomaticAt = observedAt
        return true
    }

    private fun save(bitmap: Bitmap, contentWidth: Int, contentHeight: Int, observedAt: Long, label: String): Result<VisualKeyframe> = runCatching {
        require(contentWidth in 1..bitmap.width && contentHeight in 1..bitmap.height) { "Invalid captured content bounds" }
        keyframes.mkdirs()
        enforceBudget()
        val safeLabel = label.replace(Regex("[^A-Za-z0-9_-]"), "-").take(48)
        val file = File(keyframes, "$observedAt-$safeLabel.jpg")
        val content = if (contentWidth == bitmap.width && contentHeight == bitmap.height) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, contentWidth, contentHeight)
        }
        val scale = MAX_WIDTH.toFloat() / content.width
        val output = if (scale < 1f) {
            Bitmap.createScaledBitmap(content, MAX_WIDTH, (content.height * scale).toInt().coerceAtLeast(1), true)
        } else {
            content
        }
        val outputWidth = output.width
        val outputHeight = output.height
        try {
            FileOutputStream(file).use { stream ->
                check(output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) { "JPEG encoder rejected keyframe" }
            }
            if (file.length() > MAX_BYTES || currentBytes() > MAX_BYTES) {
                file.delete()
                error("Visual evidence byte budget reached")
            }
        } finally {
            if (output !== content) output.recycle()
            if (content !== bitmap) content.recycle()
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        VisualKeyframe(
            relativePath = "keyframes/${file.name}",
            sha256 = hash,
            width = outputWidth,
            height = outputHeight,
            bytes = file.length(),
        )
    }

    private fun enforceBudget() {
        val files = keyframes.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() }.orEmpty()
        val bytes = files.sumOf { it.length() }
        check(files.size < MAX_FILES && bytes < MAX_BYTES) { "Visual evidence budget reached" }
    }

    private fun currentBytes(): Long = keyframes.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

    private companion object {
        const val MAX_PENDING = 8
        const val MAX_FILES = 200
        const val MAX_BYTES = 100L * 1024 * 1024
        const val MAX_WIDTH = 1280
        const val JPEG_QUALITY = 72
        const val AUTOMATIC_DEBOUNCE_MS = 2_000L
    }
}
