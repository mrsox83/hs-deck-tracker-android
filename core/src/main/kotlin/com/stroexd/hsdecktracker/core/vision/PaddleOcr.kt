package com.stroexd.hsdecktracker.core.vision

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A screenshot as ARGB pixels; rows are [stride] pixels apart. */
class ArgbImage(val pixels: IntArray, val width: Int, val height: Int, val stride: Int = width)

/** A text line as a rotated rectangle: corners clockwise from the top left, in image pixels. */
class TextBox(val xs: FloatArray, val ys: FloatArray, val score: Float) {
    val left: Float get() = xs.min()
    val top: Float get() = ys.min()
    val right: Float get() = xs.max()
    val bottom: Float get() = ys.max()
    val width: Float get() = max(hypot(xs[1] - xs[0], ys[1] - ys[0]), hypot(xs[2] - xs[3], ys[2] - ys[3]))
    val height: Float get() = max(hypot(xs[3] - xs[0], ys[3] - ys[0]), hypot(xs[2] - xs[1], ys[2] - ys[1]))

    fun scaled(sx: Float, sy: Float) = TextBox(FloatArray(4) { xs[it] * sx }, FloatArray(4) { ys[it] * sy }, score)
}

/**
 * PaddleOCR's PP-OCRv5 text detection and recognition on ONNX Runtime, for alphabets ML Kit can't read.
 * Pre- and post-processing follow PaddleOCR's own pipeline, which the models were trained with.
 */
class PaddleOcr(
    detectionModel: ByteArray,
    recognitionModel: ByteArray,
    characters: List<String>,
    threads: Int = 2,
) : AutoCloseable {
    private val env = OrtEnvironment.getEnvironment()
    private val options = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(threads)
        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
    }
    private val detection = env.createSession(detectionModel, options)
    private val recognition = env.createSession(recognitionModel, options)
    private val decoder = CtcDecoder(characters)

    /** Text lines in normalized coordinates; [skip] leaves out boxes (in image pixels) before they are read. */
    fun read(image: ArgbImage, skip: (TextBox) -> Boolean = { false }): List<OcrLine> {
        val boxes = detect(image).filterNot(skip)
        val texts = recognize(image, boxes)
        val w = image.width.toFloat()
        val h = image.height.toFloat()
        return boxes.indices.mapNotNull { i ->
            val (text, confidence) = texts[i] ?: return@mapNotNull null
            if (text.isBlank() || confidence < MIN_CONFIDENCE) return@mapNotNull null
            val box = boxes[i]
            OcrLine(
                text.trim(),
                (box.left / w).coerceIn(0f, 1f),
                (box.top / h).coerceIn(0f, 1f),
                (box.right / w).coerceIn(0f, 1f),
                (box.bottom / h).coerceIn(0f, 1f),
            )
        }
    }

    fun detect(image: ArgbImage, maxSide: Int = DETECTION_MAX_SIDE): List<TextBox> {
        val scale = min(1f, maxSide.toFloat() / max(image.width, image.height))
        val w = max(32, (image.width * scale / 32).roundToInt() * 32)
        val h = max(32, (image.height * scale / 32).roundToInt() * 32)
        val input = FloatArray(3 * w * h)
        val plane = w * h
        val sx = image.width.toFloat() / w
        val sy = image.height.toFloat() / h
        for (y in 0 until h) {
            val srcY = (y + 0.5f) * sy - 0.5f
            for (x in 0 until w) {
                val pixel = bilinear(image, (x + 0.5f) * sx - 0.5f, srcY)
                val i = y * w + x
                // BGR order and ImageNet statistics, like PaddleOCR's NormalizeImage
                input[i] = ((pixel and 0xFF) / 255f - 0.485f) / 0.229f
                input[plane + i] = (((pixel shr 8) and 0xFF) / 255f - 0.456f) / 0.224f
                input[2 * plane + i] = (((pixel shr 16) and 0xFF) / 255f - 0.406f) / 0.225f
            }
        }
        val map = run(detection, input, longArrayOf(1, 3, h.toLong(), w.toLong())).first
        return TextDetection.boxes(map, w, h).map { it.scaled(sx, sy) }
    }

    internal fun recognize(image: ArgbImage, boxes: List<TextBox>): Array<Pair<String, Float>?> {
        val result = arrayOfNulls<Pair<String, Float>>(boxes.size)
        val ratios = FloatArray(boxes.size) { i ->
            val box = boxes[i]
            (box.width / max(box.height, 1f)).coerceIn(0.1f, MAX_RATIO)
        }
        val order = boxes.indices.sortedBy { ratios[it] }
        for (batch in order.chunked(BATCH_SIZE)) {
            val batchRatio = max(MIN_WIDTH.toFloat() / HEIGHT, batch.maxOf { ratios[it] })
            val width = (HEIGHT * batchRatio).toInt()
            val input = FloatArray(batch.size * 3 * HEIGHT * width)
            batch.forEachIndexed { n, index ->
                val resized = min(width, ceil(HEIGHT * ratios[index]).toInt())
                sampleLine(image, boxes[index], resized, width, input, n * 3 * HEIGHT * width)
            }
            val (output, shape) = run(recognition, input, longArrayOf(batch.size.toLong(), 3, HEIGHT.toLong(), width.toLong()))
            val steps = shape[1].toInt()
            val classes = shape[2].toInt()
            batch.forEachIndexed { n, index ->
                result[index] = decoder.decode(output, n * steps * classes, steps, classes)
            }
        }
        return result
    }

    /** Straightens the box into a line of [HEIGHT] pixels, normalized to -1..1 and padded with zeros. */
    private fun sampleLine(image: ArgbImage, box: TextBox, resized: Int, width: Int, into: FloatArray, offset: Int) {
        val plane = HEIGHT * width
        val cropWidth = box.width.toInt().coerceAtLeast(1)
        val cropHeight = box.height.toInt().coerceAtLeast(1)
        val ux = (box.xs[1] - box.xs[0]) / cropWidth
        val uy = (box.ys[1] - box.ys[0]) / cropWidth
        val vx = (box.xs[3] - box.xs[0]) / cropHeight
        val vy = (box.ys[3] - box.ys[0]) / cropHeight
        for (ty in 0 until HEIGHT) {
            val cy = (ty + 0.5f) * cropHeight / HEIGHT - 0.5f
            for (tx in 0 until resized) {
                val cx = (tx + 0.5f) * cropWidth / resized - 0.5f
                val pixel = bilinear(image, box.xs[0] + cx * ux + cy * vx, box.ys[0] + cx * uy + cy * vy)
                val i = offset + ty * width + tx
                into[i] = (pixel and 0xFF) / 127.5f - 1f
                into[plane + i] = ((pixel shr 8) and 0xFF) / 127.5f - 1f
                into[2 * plane + i] = ((pixel shr 16) and 0xFF) / 127.5f - 1f
            }
        }
    }

    private fun run(session: OrtSession, input: FloatArray, shape: LongArray): Pair<FloatArray, LongArray> =
        OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape).use { tensor ->
            session.run(mapOf(session.inputNames.first() to tensor)).use { result ->
                val output = result.get(0) as OnnxTensor
                val buffer = output.floatBuffer
                FloatArray(buffer.remaining()).also { buffer.get(it) } to output.info.shape
            }
        }

    override fun close() {
        detection.close()
        recognition.close()
        options.close()
    }

    companion object {
        const val DETECTION_MAX_SIDE = 1280
        private const val HEIGHT = 48
        private const val MIN_WIDTH = 320
        private const val MAX_RATIO = 25f
        private const val BATCH_SIZE = 6
        private const val MIN_CONFIDENCE = 0.5f

        /** Clamped bilinear sample of an ARGB image. */
        internal fun bilinear(image: ArgbImage, x: Float, y: Float): Int {
            val fx = x.coerceIn(0f, (image.width - 1).toFloat())
            val fy = y.coerceIn(0f, (image.height - 1).toFloat())
            val x0 = fx.toInt()
            val y0 = fy.toInt()
            val x1 = min(x0 + 1, image.width - 1)
            val y1 = min(y0 + 1, image.height - 1)
            val ax = fx - x0
            val ay = fy - y0
            val p00 = image.pixels[y0 * image.stride + x0]
            val p01 = image.pixels[y0 * image.stride + x1]
            val p10 = image.pixels[y1 * image.stride + x0]
            val p11 = image.pixels[y1 * image.stride + x1]
            return channel(p00, p01, p10, p11, ax, ay, 0) or
                channel(p00, p01, p10, p11, ax, ay, 8) or
                channel(p00, p01, p10, p11, ax, ay, 16)
        }

        private fun channel(p00: Int, p01: Int, p10: Int, p11: Int, ax: Float, ay: Float, shift: Int): Int {
            val top = ((p00 shr shift) and 0xFF) * (1 - ax) + ((p01 shr shift) and 0xFF) * ax
            val bottom = ((p10 shr shift) and 0xFF) * (1 - ax) + ((p11 shr shift) and 0xFF) * ax
            return (top * (1 - ay) + bottom * ay + 0.5f).toInt().coerceIn(0, 255) shl shift
        }
    }
}

/** Greedy CTC decoding: index 0 is the blank, then the characters, then a space. */
internal class CtcDecoder(private val characters: List<String>) {
    fun decode(probabilities: FloatArray, offset: Int, steps: Int, classes: Int): Pair<String, Float> {
        val text = StringBuilder()
        var sum = 0f
        var count = 0
        var previous = -1
        for (t in 0 until steps) {
            val row = offset + t * classes
            var best = 0
            for (c in 1 until classes) if (probabilities[row + c] > probabilities[row + best]) best = c
            if (best != 0 && best != previous) {
                text.append(characters.getOrNull(best - 1) ?: " ")
                sum += probabilities[row + best]
                count++
            }
            previous = best
        }
        return text.toString() to if (count == 0) 0f else sum / count
    }
}

/** DB post-processing: text regions of the probability map as rotated rectangles, grown by the unclip ratio. */
internal object TextDetection {
    private const val THRESHOLD = 0.3f
    private const val BOX_THRESHOLD = 0.6f
    private const val UNCLIP_RATIO = 1.5f
    private const val MIN_SIZE = 3f
    private const val MAX_CANDIDATES = 1000

    fun boxes(map: FloatArray, width: Int, height: Int): List<TextBox> {
        val labels = IntArray(width * height)
        val rowMin = IntArray(height)
        val rowMax = IntArray(height)
        val stack = IntArray(width * height)
        val result = ArrayList<TextBox>()
        var label = 0
        for (start in map.indices) {
            if (map[start] <= THRESHOLD || labels[start] != 0) continue
            if (++label > MAX_CANDIDATES) break
            // Row-major scanning reaches every region at its top row first
            val top = start / width
            var bottom = top - 1
            var size = 0
            stack[size++] = start
            labels[start] = label
            var pixels = 0
            var sum = 0f
            while (size > 0) {
                val p = stack[--size]
                val x = p % width
                val y = p / width
                while (bottom < y) {
                    bottom++
                    rowMin[bottom] = Int.MAX_VALUE
                    rowMax[bottom] = -1
                }
                rowMin[y] = min(rowMin[y], x)
                rowMax[y] = max(rowMax[y], x)
                pixels++
                sum += map[p]
                for (dy in -1..1) {
                    val ny = y + dy
                    if (ny < 0 || ny >= height) continue
                    for (dx in -1..1) {
                        val nx = x + dx
                        if (nx < 0 || nx >= width) continue
                        val q = ny * width + nx
                        if (labels[q] == 0 && map[q] > THRESHOLD) {
                            labels[q] = label
                            stack[size++] = q
                        }
                    }
                }
            }
            if (pixels < 4) continue
            val points = ArrayList<Point>()
            for (r in top..bottom) {
                if (rowMax[r] < 0) continue
                points += Point(rowMin[r].toFloat(), r.toFloat())
                if (rowMax[r] != rowMin[r]) points += Point(rowMax[r].toFloat(), r.toFloat())
            }
            // Scored over the region itself rather than its rectangle (PaddleOCR's "slow" mode): card names are curved
            val score = sum / pixels
            if (score < BOX_THRESHOLD) continue
            val rect = minAreaRect(convexHull(points)) ?: continue
            if (min(rect.halfAlong, rect.halfAcross) * 2 < MIN_SIZE) continue
            val along = rect.halfAlong * 2
            val across = rect.halfAcross * 2
            val distance = along * across * UNCLIP_RATIO / (2 * (along + across))
            val grown = rect.copy(halfAlong = rect.halfAlong + distance, halfAcross = rect.halfAcross + distance)
            if (min(grown.halfAlong, grown.halfAcross) * 2 < MIN_SIZE + 2) continue
            result += grown.toBox(score)
        }
        return result
    }

    data class Point(val x: Float, val y: Float)

    /** [ax], [ay] point along the text (rightwards), the across axis points down. */
    data class Rect(val cx: Float, val cy: Float, val ax: Float, val ay: Float, val halfAlong: Float, val halfAcross: Float) {
        fun toBox(score: Float): TextBox {
            val bx = -ay
            val by = ax
            val xs = floatArrayOf(-1f, 1f, 1f, -1f)
            val ys = floatArrayOf(-1f, -1f, 1f, 1f)
            return TextBox(
                FloatArray(4) { cx + xs[it] * halfAlong * ax + ys[it] * halfAcross * bx },
                FloatArray(4) { cy + xs[it] * halfAlong * ay + ys[it] * halfAcross * by },
                score,
            )
        }
    }

    fun convexHull(points: List<Point>): List<Point> {
        val sorted = points.distinct().sortedWith(compareBy({ it.x }, { it.y }))
        if (sorted.size < 3) return sorted
        fun cross(o: Point, a: Point, b: Point) = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
        val hull = ArrayList<Point>()
        for (pass in 0..1) {
            val start = hull.size
            for (p in if (pass == 0) sorted else sorted.asReversed()) {
                while (hull.size >= start + 2 && cross(hull[hull.size - 2], hull[hull.size - 1], p) <= 0) hull.removeAt(hull.size - 1)
                hull += p
            }
            hull.removeAt(hull.size - 1)
        }
        return hull
    }

    /** Smallest rectangle around a convex hull; its long side is taken as the text direction only if it isn't steep. */
    fun minAreaRect(hull: List<Point>): Rect? {
        if (hull.isEmpty()) return null
        if (hull.size == 1) return Rect(hull[0].x, hull[0].y, 1f, 0f, 0f, 0f)
        var best: Rect? = null
        var bestArea = Float.MAX_VALUE
        for (i in hull.indices) {
            val a = hull[i]
            val b = hull[(i + 1) % hull.size]
            val length = hypot(b.x - a.x, b.y - a.y)
            if (length == 0f) continue
            val ex = (b.x - a.x) / length
            val ey = (b.y - a.y) / length
            var minU = Float.MAX_VALUE
            var maxU = -Float.MAX_VALUE
            var minV = Float.MAX_VALUE
            var maxV = -Float.MAX_VALUE
            for (p in hull) {
                val u = p.x * ex + p.y * ey
                val v = -p.x * ey + p.y * ex
                minU = min(minU, u)
                maxU = max(maxU, u)
                minV = min(minV, v)
                maxV = max(maxV, v)
            }
            val area = (maxU - minU) * (maxV - minV)
            if (area < bestArea - 1e-3f) {
                bestArea = area
                val mu = (minU + maxU) / 2
                val mv = (minV + maxV) / 2
                best = Rect(mu * ex - mv * ey, mu * ey + mv * ex, ex, ey, (maxU - minU) / 2, (maxV - minV) / 2)
            }
        }
        return best?.let(::upright)
    }

    /** Hearthstone's text runs horizontally: the axis closer to horizontal becomes the text direction. */
    private fun upright(rect: Rect): Rect {
        var r = rect
        if (abs(r.ay) > abs(r.ax)) r = Rect(r.cx, r.cy, -r.ay, r.ax, r.halfAcross, r.halfAlong)
        if (r.ax < 0) r = r.copy(ax = -r.ax, ay = -r.ay)
        val norm = sqrt(r.ax * r.ax + r.ay * r.ay)
        return r.copy(ax = r.ax / norm, ay = r.ay / norm)
    }
}
