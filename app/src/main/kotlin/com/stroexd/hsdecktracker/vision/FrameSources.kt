package com.stroexd.hsdecktracker.vision

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.stroexd.hsdecktracker.core.vision.CaptureDiagnosticKind
import com.stroexd.hsdecktracker.core.vision.CaptureContentGeometry
import com.stroexd.hsdecktracker.core.vision.ProjectionGeometry
import kotlin.math.max
import kotlin.math.min

/** A screenshot; [width] can be smaller than the bitmap when rows are padded. */
class Capture(val bitmap: Bitmap, val width: Int, val height: Int, val imageTimestampNanos: Long? = null)

/** Where screenshots come from. All calls and callbacks happen on the recognizer's thread. */
interface FrameSource {
    fun currentContentGeometry(nowElapsedMillis: Long): CaptureContentGeometry? = null
    fun setDiagnosticListener(listener: ((CaptureDiagnosticKind, Map<String, Long>) -> Unit)?) {}
    /** [onStopped] is called when the source ends by itself, e.g. screen sharing was stopped. */
    fun start(handler: Handler, onStopped: () -> Unit)

    fun capture(onCapture: (Capture?) -> Unit)

    fun stop()
}

fun realDisplaySize(context: Context): Pair<Int, Int> {
    val windowManager = context.getSystemService(WindowManager::class.java)
    return if (Build.VERSION.SDK_INT >= 30) {
        val bounds = windowManager.maximumWindowMetrics.bounds
        bounds.width() to bounds.height()
    } else {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        metrics.widthPixels to metrics.heightPixels
    }
}

/**
 * Screen sharing. The virtual display is only attached while a frame is taken instead of rendering 60+ frames a
 * second; devices where re-attaching doesn't deliver frames fall back to staying attached.
 */
class MediaProjectionSource(private val context: Context, private val projection: MediaProjection) : FrameSource {
    private val contentGeometry = ProjectionGeometry()
    override fun currentContentGeometry(nowElapsedMillis: Long) = contentGeometry.current(nowElapsedMillis)
    private var diagnosticListener: ((CaptureDiagnosticKind, Map<String, Long>) -> Unit)? = null
    private var unrequestedImages = 0L
    private var emptyImages = 0L
    private var acquireFailures = 0L
    override fun setDiagnosticListener(listener: ((CaptureDiagnosticKind, Map<String, Long>) -> Unit)?) {
        diagnosticListener = listener
    }
    private fun report(kind: CaptureDiagnosticKind, values: Map<String, Long> = emptyMap()) {
        runCatching { diagnosticListener?.invoke(kind, values) }
    }
    /** Attached surfaces may deliver at display rate: summarize without per-image I/O. */
    private fun reportImageCounters() {
        if (unrequestedImages > 0) report(CaptureDiagnosticKind.UNREQUESTED_IMAGE, mapOf("count" to unrequestedImages))
        if (emptyImages > 0) report(CaptureDiagnosticKind.NO_IMAGE, mapOf("count" to emptyImages))
        if (acquireFailures > 0) report(CaptureDiagnosticKind.ACQUIRE_FAILED, mapOf("count" to acquireFailures))
        unrequestedImages = 0; emptyImages = 0; acquireFailures = 0
    }
    private lateinit var handler: Handler
    private var reader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var pending: ((Capture?) -> Unit)? = null
    private var detachBetweenFrames = true
    private var reattachWorks = false
    private var reattached = false
    private var timeouts = 0
    private var bitmap: Bitmap? = null
    private var densityDpi = 0
    private val timeout = Runnable { onTimeout() }

    override fun start(handler: Handler, onStopped: () -> Unit) {
        this.handler = handler
        contentGeometry.start()
        projection.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    contentGeometry.stop()
                    report(CaptureDiagnosticKind.SOURCE_STOP)
                    onStopped()
                }

                override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                    report(CaptureDiagnosticKind.VISIBILITY, mapOf("visible" to if (isVisible) 1L else 0L))
                }

                override fun onCapturedContentResize(width: Int, height: Int) {
                    report(CaptureDiagnosticKind.RESIZE, mapOf("contentWidth" to width.toLong(), "contentHeight" to height.toLong()))
                    if (Build.VERSION.SDK_INT >= 34) {
                        contentGeometry.resizing()
                        resize(width, height)
                        contentGeometry.resized(width, height, reader?.width ?: 0, reader?.height ?: 0, SystemClock.elapsedRealtime())
                    }
                }
            },
            handler,
        )
        val (realWidth, realHeight) = realDisplaySize(context)
        val (width, height) = scaledSize(realWidth, realHeight)
        report(CaptureDiagnosticKind.SOURCE_START, mapOf("realWidth" to realWidth.toLong(), "realHeight" to realHeight.toLong(),
            "readerWidth" to width.toLong(), "readerHeight" to height.toLong()))
        densityDpi = context.resources.displayMetrics.densityDpi
        val imageReader = newReader(width, height)
        virtualDisplay = projection.createVirtualDisplay(
            "hs-deck-tracker",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            handler,
        )
    }

    private fun newReader(width: Int, height: Int): ImageReader =
        ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).also { imageReader ->
            imageReader.setOnImageAvailableListener({ onImageAvailable(it) }, handler)
            reader = imageReader
        }

    private fun resize(contentWidth: Int, contentHeight: Int) {
        if (contentWidth <= 0 || contentHeight <= 0) return
        val (width, height) = scaledSize(contentWidth, contentHeight)
        val old = reader ?: return
        if (old.width == width && old.height == height) return
        handler.removeCallbacks(timeout)
        pending?.also { callback ->
            pending = null
            callback(null)
        }
        val next = newReader(width, height)
        report(CaptureDiagnosticKind.VIEWPORT, mapOf("oldWidth" to old.width.toLong(), "oldHeight" to old.height.toLong(),
            "readerWidth" to width.toLong(), "readerHeight" to height.toLong(), "detachBetweenFrames" to if (detachBetweenFrames) 1L else 0L))
        virtualDisplay?.apply {
            surface = null
            resize(width, height, densityDpi)
            if (!detachBetweenFrames) surface = next.surface
        }
        runCatching { old.close() }
        bitmap = null
    }

    private fun scaledSize(contentWidth: Int, contentHeight: Int): Pair<Int, Int> {
        val scale = min(1f, MAX_WIDTH.toFloat() / max(contentWidth, contentHeight))
        val width = max(2, ((contentWidth * scale).toInt() / 2) * 2)
        val height = max(2, ((contentHeight * scale).toInt() / 2) * 2)
        return width to height
    }

    override fun capture(onCapture: (Capture?) -> Unit) {
        reportImageCounters()
        val imageReader = reader ?: return onCapture(null)
        pending = onCapture
        if (detachBetweenFrames) {
            runCatching { imageReader.acquireLatestImage()?.close() }
            virtualDisplay?.surface = imageReader.surface
            reattached = true
        }
        handler.postDelayed(timeout, FRAME_TIMEOUT_MS)
    }

    override fun stop() {
        contentGeometry.stop()
        reportImageCounters()
        handler.removeCallbacks(timeout)
        pending = null
        runCatching { virtualDisplay?.release() }
        runCatching { reader?.close() }
        virtualDisplay = null
        reader = null
        bitmap = null
        runCatching { projection.stop() }
    }

    private fun onTimeout() {
        reportImageCounters()
        val callback = pending ?: return
        timeouts++
        report(CaptureDiagnosticKind.TIMEOUT, mapOf("count" to timeouts.toLong(), "reattachWorks" to if (reattachWorks) 1L else 0L))
        if (!reattachWorks && timeouts >= MAX_TIMEOUTS) {
            report(CaptureDiagnosticKind.ATTACH_FALLBACK)
            detachBetweenFrames = false
            virtualDisplay?.surface = reader?.surface
            return
        }
        pending = null
        virtualDisplay?.surface = null
        callback(null)
    }

    private fun onImageAvailable(imageReader: ImageReader) {
        val image = try {
            imageReader.acquireLatestImage()
        } catch (e: IllegalStateException) {
            acquireFailures++
            null
        } ?: run { emptyImages++; return }
        val callback = pending
        if (callback == null) {
            unrequestedImages++
            image.close()
            return
        }
        pending = null
        handler.removeCallbacks(timeout)
        timeouts = 0
        if (reattached) reattachWorks = true
        if (detachBetweenFrames) virtualDisplay?.surface = null
        val capture = try {
            Capture(copyToBitmap(image), image.width, image.height, image.timestamp)
        } catch (e: Exception) {
            report(CaptureDiagnosticKind.COPY_FAILED)
            null
        } finally {
            image.close()
        }
        if (capture != null) contentGeometry.delivered(capture.width, capture.height, SystemClock.elapsedRealtime(), capture.imageTimestampNanos ?: -1L)
        callback(capture)
    }

    private fun copyToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val bitmapWidth = plane.rowStride / plane.pixelStride
        val current = bitmap?.takeIf { it.width == bitmapWidth && it.height == image.height && !it.isRecycled }
        val target = current ?: Bitmap.createBitmap(bitmapWidth, image.height, Bitmap.Config.ARGB_8888).also { bitmap = it }
        plane.buffer.rewind()
        target.copyPixelsFromBuffer(plane.buffer)
        return target
    }

    private companion object {
        const val MAX_WIDTH = 2000
        const val FRAME_TIMEOUT_MS = 700L
        const val MAX_TIMEOUTS = 3
    }
}

/** Screenshots through the app's accessibility service: no screen-sharing prompt per session (Android 11+). */
@RequiresApi(30)
class AccessibilityScreenshotSource(private val service: AccessibilityService) : FrameSource {
    private var diagnosticListener: ((CaptureDiagnosticKind, Map<String, Long>) -> Unit)? = null
    override fun setDiagnosticListener(listener: ((CaptureDiagnosticKind, Map<String, Long>) -> Unit)?) {
        diagnosticListener = listener
    }
    private fun report(kind: CaptureDiagnosticKind, values: Map<String, Long> = emptyMap()) {
        runCatching { diagnosticListener?.invoke(kind, values) }
    }
    private lateinit var handler: Handler
    private var last: Bitmap? = null

    override fun start(handler: Handler, onStopped: () -> Unit) {
        this.handler = handler
    }

    override fun capture(onCapture: (Capture?) -> Unit) {
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            { command -> handler.post(command) },
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val buffer = result.hardwareBuffer
                    val copy = try {
                        Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)?.let { hardware ->
                            hardware.copy(Bitmap.Config.ARGB_8888, false).also { hardware.recycle() }
                        }
                    } catch (e: Exception) {
                        report(CaptureDiagnosticKind.COPY_FAILED)
                        null
                    } finally {
                        buffer.close()
                    }
                    // The previous screenshot is done with: captures never overlap
                    last?.recycle()
                    last = copy
                    onCapture(copy?.let { Capture(it, it.width, it.height) })
                }

                override fun onFailure(errorCode: Int) {
                    report(CaptureDiagnosticKind.SOURCE_FAILED, mapOf("errorCode" to errorCode.toLong()))
                    onCapture(null)
                }
            },
        )
    }

    override fun stop() {
        last = null
    }
}
