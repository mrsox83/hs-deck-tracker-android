package com.stroexd.hsdecktracker.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.stroexd.hsdecktracker.appContainer
import com.stroexd.hsdecktracker.core.data.GameLocales
import com.stroexd.hsdecktracker.core.vision.ArgbImage
import com.stroexd.hsdecktracker.core.vision.OcrFrame
import com.stroexd.hsdecktracker.core.vision.OcrLine
import com.stroexd.hsdecktracker.core.vision.PaddleOcr
import com.stroexd.hsdecktracker.core.vision.CaptureDiagnosticKind
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.min

data class CapturePacing(val intervalMillis: Long, val maxReuseMillis: Long)

/** Connects a frame source to the app: recognition, diagnostics and the recognition status. */
fun Context.startScreenRecognition(
    source: FrameSource,
    maskProvider: () -> Rect?,
    isActive: () -> Boolean = { true },
    onStopped: () -> Unit = {},
): ScreenRecognizer {
    val container = appContainer
    val session = container.onRecognitionStarted(when (source) {
        is MediaProjectionSource -> "media_projection"
        is AccessibilityScreenshotSource -> "accessibility_screenshot"
        else -> "unknown_frame_source"
    })
    var diagnostics: DiagnosticsRecorder? = null
    val diagnosticLock = Any()
    fun recorder(): DiagnosticsRecorder? = synchronized(diagnosticLock) {
        if (!container.settings.value.recordDiagnostics) null
        else diagnostics ?: DiagnosticsRecorder(DiagnosticsRecorder.root(this)).also {
            diagnostics = it
            it.capture(CaptureDiagnosticKind.SESSION, mapOf("sdkInt" to Build.VERSION.SDK_INT.toLong(),
                "sourceCode" to when (source) { is MediaProjectionSource -> 1L; is AccessibilityScreenshotSource -> 2L; else -> 0L },
                "selectionModeUnknown" to 1L))
        }
    }
    val recognizer = ScreenRecognizer(
        context = this,
        source = source,
        isActive = isActive,
        maskProvider = maskProvider,
        cyrillic = { GameLocales.isCyrillic(container.gameLocale.value) },
        pacing = { container.capturePacing() },
        onDiagnostic = { kind, values ->
            if (container.isRecognitionSessionCurrent(session)) recorder()?.capture(kind, values)
        },
        diagnosticsEnabled = { container.settings.value.recordDiagnostics },
        onFrame = { frame, bitmap, contentWidth, contentHeight, reused ->
            if (container.isRecognitionSessionCurrent(session)) {
            // Follows the setting live, so recording can also be switched on in the middle of a game
            val activeRecorder = recorder()
            val notes = activeRecorder?.let { mutableListOf<String>() }
            val matchIdBeforeFrame = container.tracker.state.value?.draftId
            val events = container.onScreenFrame(frame, notes, reused)
            container.onVisualFrame(frame, bitmap, contentWidth, contentHeight, events, matchIdBeforeFrame, reused)
            activeRecorder?.let { runCatching { it.record(frame, events, notes.orEmpty(), bitmap) } }
            }
        },
        onStopped = {
            if (container.onRecognitionStopped(session)) onStopped()
        },
    )
    recognizer.start()
    return recognizer
}

/**
 * Takes screenshots at a pace that depends on the game phase and reads them with on-device text recognition:
 * ML Kit for Latin script, PaddleOCR for [cyrillic] clients. Unchanged screens reuse the last result, and nothing is
 * captured while [isActive] is false, the screen is off or the device is upright (Hearthstone only runs in landscape).
 */
class ScreenRecognizer(
    private val context: Context,
    private val source: FrameSource,
    private val isActive: () -> Boolean,
    private val maskProvider: () -> Rect?,
    private val cyrillic: () -> Boolean,
    private val pacing: () -> CapturePacing,
    private val onFrame: (frame: OcrFrame, bitmap: Bitmap, contentWidth: Int, contentHeight: Int, reused: Boolean) -> Unit,
    private val onStopped: () -> Unit,
    private val onDiagnostic: (CaptureDiagnosticKind, Map<String, Long>) -> Unit = { _, _ -> },
    private val diagnosticsEnabled: () -> Boolean = { false },
) {
    private var captureId = 0L
    private val sourceCode = when (source) { is MediaProjectionSource -> 1L; is AccessibilityScreenshotSource -> 2L; else -> 0L }
    private fun report(kind: CaptureDiagnosticKind, values: Map<String, Long> = emptyMap()) {
        if (!diagnosticsEnabled()) return
        runCatching { onDiagnostic(kind, values + mapOf("captureId" to captureId, "sourceCode" to sourceCode)) }
    }
    private val thread = HandlerThread("hs-screen-capture").apply { start() }
    private val handler = Handler(thread.looper)
    private val worker = Executors.newSingleThreadExecutor()

    private val listenerExecutor = Executor { command -> runCatching { worker.execute(command) } }
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var paddle: PaddleOcr? = null
    @Volatile private var paddleFailed = false
    private var pixels = IntArray(0)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val stopped = AtomicBoolean(false)

    private var captureStartedAt = 0L
    private var lastPrint: IntArray? = null
    private var lastLines: List<OcrLine>? = null
    private var lastOcrAt = 0L

    private val captureTask = Runnable { capture() }

    fun start() {
        handler.post {
            source.setDiagnosticListener { kind, values -> report(kind, values) }
            val started = runCatching { source.start(handler) { stop() } }.isSuccess
            if (!started) report(CaptureDiagnosticKind.SOURCE_STOP, mapOf("startFailed" to 1L))
            if (started) capture() else stop()
        }
    }

    fun stop() {
        if (!stopped.compareAndSet(false, true)) return
        handler.post {
            report(CaptureDiagnosticKind.STOPPED)
            handler.removeCallbacks(captureTask)
            runCatching { source.stop() }
            worker.execute {
                runCatching { recognizer.close() }
                runCatching { paddle?.close() }
            }
            worker.shutdown()
            thread.quitSafely()
            onStopped()
        }
    }

    private fun scheduleNext() {
        if (stopped.get()) return
        val interval = effectiveInterval(pacing().intervalMillis)
        val wait = (captureStartedAt + interval - SystemClock.elapsedRealtime()).coerceAtLeast(MIN_GAP_MS)
        handler.removeCallbacks(captureTask)
        handler.postDelayed(captureTask, wait)
    }

    private fun effectiveInterval(base: Long): Long {
        var factor = 1.0
        if (powerManager?.isPowerSaveMode == true) factor *= 1.5
        if (Build.VERSION.SDK_INT >= 29 && (powerManager?.currentThermalStatus ?: 0) >= PowerManager.THERMAL_STATUS_SEVERE) factor *= 2
        return (base * factor).toLong()
    }

    private fun capture() {
        if (stopped.get()) return
        captureStartedAt = SystemClock.elapsedRealtime()
        val (realWidth, realHeight) = realDisplaySize(context)
        if (powerManager?.isInteractive == false || realWidth < realHeight || !isActive()) {
            report(CaptureDiagnosticKind.SKIPPED, mapOf("screenInteractive" to if (powerManager?.isInteractive == false) 0L else 1L,
                "portrait" to if (realWidth < realHeight) 1L else 0L,
                "reasonCode" to when { powerManager?.isInteractive == false -> 1L; realWidth < realHeight -> 2L; else -> 3L }))
            handler.postDelayed(captureTask, INACTIVE_CHECK_MS)
            return
        }
        captureId++
        report(CaptureDiagnosticKind.REQUEST)
        source.capture { capture ->
            report(if (capture == null) CaptureDiagnosticKind.EMPTY_CAPTURE else CaptureDiagnosticKind.DELIVERED,
                mapOf("deliveryMillis" to (SystemClock.elapsedRealtime() - captureStartedAt),
                    "imageTimestampNanos" to (capture?.imageTimestampNanos ?: -1L)))
            if (capture == null || stopped.get()) scheduleNext() else process(capture)
        }
    }

    private fun process(capture: Capture) {
        val (realWidth, realHeight) = realDisplaySize(context)
        val bitmap = capture.bitmap
        val width = capture.width
        val height = capture.height
        val rawMask = maskProvider()
        val mask = rawMask?.let { scaleMask(it, width, height, realWidth, realHeight) }
        report(CaptureDiagnosticKind.VIEWPORT, mapOf("realWidth" to realWidth.toLong(), "realHeight" to realHeight.toLong(),
            "contentWidth" to width.toLong(), "contentHeight" to height.toLong(), "bitmapWidth" to bitmap.width.toLong(),
            "bitmapHeight" to bitmap.height.toLong(), "maskLeft" to (mask?.left?.toLong() ?: -1L),
            "maskTop" to (mask?.top?.toLong() ?: -1L), "maskRight" to (mask?.right?.toLong() ?: -1L),
            "maskBottom" to (mask?.bottom?.toLong() ?: -1L), "rawMaskLeft" to (rawMask?.left?.toLong() ?: -1L),
            "rawMaskTop" to (rawMask?.top?.toLong() ?: -1L), "rawMaskRight" to (rawMask?.right?.toLong() ?: -1L),
            "rawMaskBottom" to (rawMask?.bottom?.toLong() ?: -1L)))
        val print = try {
            fingerprint(capture, mask)
        } catch (e: Exception) {
            report(CaptureDiagnosticKind.FINGERPRINT_FAILED)
            scheduleNext()
            return
        }
        val now = SystemClock.elapsedRealtime()
        val previousLines = lastLines
        val previousPrint = lastPrint
        val aspect = width.toFloat() / height
        if (diagnosticsEnabled()) {
            val sampled = print.filter { it >= 0 }
            report(CaptureDiagnosticKind.FRAME_QUALITY, mapOf("sampledCells" to sampled.size.toLong(),
                "darkCells" to sampled.count { it <= 3 }.toLong(),
                "brightnessRange" to ((sampled.maxOrNull() ?: 0) - (sampled.minOrNull() ?: 0)).toLong()))
        }
        val ocrStarted = SystemClock.elapsedRealtime()
        if (previousLines != null && previousPrint != null && now - lastOcrAt < pacing().maxReuseMillis && isSimilar(print, previousPrint)) {
            val frame = OcrFrame(System.currentTimeMillis(), previousLines, aspect)
            report(CaptureDiagnosticKind.OCR_REUSED, mapOf("ocrAgeMillis" to now - lastOcrAt,
                "lineCount" to previousLines.size.toLong(), "frameTimestampMillis" to frame.timestamp))
            listenerExecutor.execute {
                runCatching { onFrame(frame, bitmap, width, height, true) }
                handler.post { scheduleNext() }
            }
            return
        }
        report(CaptureDiagnosticKind.OCR_START, mapOf("engine" to if (cyrillic() && !paddleFailed) 2L else 1L))
        if (cyrillic() && !paddleFailed) {
            listenerExecutor.execute {
                val frame = paddle()?.let { reader -> runCatching { readCyrillic(reader, capture, aspect, mask) }.getOrNull() }
                report(if (frame == null) CaptureDiagnosticKind.OCR_FAILED else CaptureDiagnosticKind.OCR_SUCCESS,
                    mapOf("durationMillis" to SystemClock.elapsedRealtime() - ocrStarted, "lineCount" to (frame?.lines?.size?.toLong() ?: -1L),
                        "frameTimestampMillis" to (frame?.timestamp ?: -1L)))
                if (frame != null) runCatching { onFrame(frame, bitmap, width, height, false) }
                handler.post {
                    if (frame != null) {
                        lastLines = frame.lines
                        lastPrint = print
                        lastOcrAt = now
                    }
                    scheduleNext()
                }
            }
            return
        }
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener(listenerExecutor) { text ->
                val frame = toFrame(text, width, height, aspect, mask)
                report(CaptureDiagnosticKind.OCR_SUCCESS, mapOf("durationMillis" to SystemClock.elapsedRealtime() - ocrStarted,
                    "lineCount" to frame.lines.size.toLong(), "frameTimestampMillis" to frame.timestamp))
                runCatching { onFrame(frame, bitmap, width, height, false) }
                handler.post {
                    lastLines = frame.lines
                    lastPrint = print
                    lastOcrAt = now
                    scheduleNext()
                }
            }
            .addOnFailureListener(listenerExecutor) {
                report(CaptureDiagnosticKind.OCR_FAILED, mapOf("durationMillis" to SystemClock.elapsedRealtime() - ocrStarted))
                handler.post { scheduleNext() }
            }
    }

    /** Loaded on first use on the worker thread; ML Kit takes over if the models can't be loaded. */
    private fun paddle(): PaddleOcr? {
        if (paddle == null && !paddleFailed) {
            paddle = runCatching {
                val assets = context.assets
                fun bytes(name: String) = assets.open("ocr/$name").use { it.readBytes() }
                val characters = assets.open("ocr/eslav_dict.txt").bufferedReader().use { it.readLines() }
                PaddleOcr(bytes("det.onnx"), bytes("rec_eslav.onnx"), characters, threads = min(4, Runtime.getRuntime().availableProcessors()))
            }.onFailure { paddleFailed = true }.getOrNull()
        }
        return paddle
    }

    private fun readCyrillic(reader: PaddleOcr, capture: Capture, aspect: Float, mask: Rect?): OcrFrame {
        val width = capture.width
        val height = capture.height
        if (pixels.size < width * height) pixels = IntArray(width * height)
        capture.bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val lines = reader.read(ArgbImage(pixels, width, height)) { box ->
            mask != null && mask.intersects(box.left.toInt(), box.top.toInt(), box.right.toInt(), box.bottom.toInt())
        }
        return OcrFrame(System.currentTimeMillis(), lines, aspect = aspect)
    }

    /** Brightness of a coarse grid, the overlay left out: enough to notice whether anything changed. */
    private fun fingerprint(capture: Capture, mask: Rect?): IntArray {
        val cellWidth = capture.width / GRID_X
        val cellHeight = capture.height / GRID_Y
        val result = IntArray(GRID_X * GRID_Y)
        for (gy in 0 until GRID_Y) {
            for (gx in 0 until GRID_X) {
                val index = gy * GRID_X + gx
                val left = gx * cellWidth
                val top = gy * cellHeight
                if (mask != null && mask.intersects(left, top, left + cellWidth, top + cellHeight)) {
                    result[index] = -1
                    continue
                }
                var sum = 0
                for (sy in 1..2) {
                    for (sx in 1..2) {
                        val pixel = capture.bitmap.getPixel(left + cellWidth * sx / 3, top + cellHeight * sy / 3)
                        sum += (((pixel shr 16) and 0xFF) * 54 + ((pixel shr 8) and 0xFF) * 183 + (pixel and 0xFF) * 19) shr 8
                    }
                }
                result[index] = sum / 4
            }
        }
        return result
    }

    private fun isSimilar(a: IntArray, b: IntArray): Boolean {
        if (a.size != b.size) return false
        var changed = 0
        for (i in a.indices) {
            if (a[i] < 0 || b[i] < 0) continue
            if (abs(a[i] - b[i]) > CELL_THRESHOLD && ++changed > MAX_CHANGED_CELLS) return false
        }
        return true
    }

    private fun scaleMask(mask: Rect, frameWidth: Int, frameHeight: Int, realWidth: Int, realHeight: Int): Rect {
        val sx = frameWidth.toFloat() / realWidth
        val sy = frameHeight.toFloat() / realHeight
        return Rect(
            (mask.left * sx).toInt() - MASK_MARGIN,
            (mask.top * sy).toInt() - MASK_MARGIN,
            (mask.right * sx).toInt() + MASK_MARGIN,
            (mask.bottom * sy).toInt() + MASK_MARGIN,
        )
    }

    private fun toFrame(text: Text, width: Int, height: Int, aspect: Float, mask: Rect?): OcrFrame {
        val w = width.toFloat()
        val h = height.toFloat()
        val lines = ArrayList<OcrLine>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                if (box.left >= width) continue
                if (mask != null && Rect.intersects(mask, box)) continue
                lines += OcrLine(line.text, box.left / w, box.top / h, min(box.right.toFloat(), w) / w, box.bottom / h)
            }
        }
        return OcrFrame(System.currentTimeMillis(), lines, aspect = aspect)
    }

    private companion object {
        const val MASK_MARGIN = 8
        const val MIN_GAP_MS = 50L
        const val INACTIVE_CHECK_MS = 3_000L
        const val GRID_X = 48
        const val GRID_Y = 27
        const val CELL_THRESHOLD = 20
        const val MAX_CHANGED_CELLS = 6
    }
}
