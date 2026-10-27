package com.stroexd.hsdecktracker.core.vision

data class CaptureContentGeometry(val width: Int, val height: Int)

/** Source-local, monotonic freshness; never bootstraps from a display-sized reader alone. */
class ProjectionGeometry(private val maxAgeMillis: Long = 10_000L) {
    private var running = false
    private var geometry: CaptureContentGeometry? = null
    private var readerWidth = 0
    private var readerHeight = 0
    private var confirmedAt = 0L
    private var lastImageTimestamp = 0L

    fun start() { running = true; lastImageTimestamp = 0L; invalidate() }
    fun stop() { running = false; lastImageTimestamp = 0L; invalidate() }
    fun resizing() { invalidate() }
    private fun invalidate() { geometry = null; readerWidth = 0; readerHeight = 0 }

    /** Called only after a valid captured-content resize was applied (including an unchanged reader size). */
    fun resized(width: Int, height: Int, readerWidth: Int, readerHeight: Int, now: Long) {
        invalidate()
        if (!running || width <= 0 || height <= 0 || width == height || readerWidth <= 0 || readerHeight <= 0 ||
            (width > height) != (readerWidth > readerHeight)) return
        geometry = CaptureContentGeometry(width, height)
        this.readerWidth = readerWidth
        this.readerHeight = readerHeight
        confirmedAt = now
    }

    /** A newly delivered matching image renews known source geometry, not proof of unobscured pixels. */
    fun delivered(width: Int, height: Int, now: Long, imageTimestampNanos: Long) {
        if (!running || geometry == null) return
        if (width != readerWidth || height != readerHeight || now < confirmedAt) { invalidate(); return }
        if (imageTimestampNanos <= lastImageTimestamp) return
        lastImageTimestamp = imageTimestampNanos
        confirmedAt = now
    }

    fun current(now: Long): CaptureContentGeometry? = geometry?.takeIf {
        running && now >= confirmedAt && now - confirmedAt <= maxAgeMillis
    }
}
