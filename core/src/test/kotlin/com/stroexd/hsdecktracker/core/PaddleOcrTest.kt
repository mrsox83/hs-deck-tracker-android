package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.ArgbImage
import com.stroexd.hsdecktracker.core.vision.CardNameIndex
import com.stroexd.hsdecktracker.core.vision.CtcDecoder
import com.stroexd.hsdecktracker.core.vision.PaddleOcr
import com.stroexd.hsdecktracker.core.vision.TextDetection
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaddleOcrTest {
    @Test
    fun ctcDropsBlanksAndRepeats() {
        val characters = listOf("а", "б")
        // Classes: blank, а, б, space
        val steps = listOf(1, 1, 0, 1, 2, 3, 2, 2)
        val probabilities = FloatArray(steps.size * 4)
        steps.forEachIndexed { t, c -> probabilities[t * 4 + c] = 0.9f }
        val (text, confidence) = CtcDecoder(characters).decode(probabilities, 0, steps.size, 4)
        assertEquals("ааб б", text)
        assertEquals(0.9f, confidence, 1e-4f)
    }

    @Test
    fun detectionFindsRotatedRegions() {
        val width = 200
        val height = 100
        val map = FloatArray(width * height)
        // A bar 120 × 12 pixels, tilted by 5°
        val angle = Math.toRadians(5.0)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - 100.0
                val dy = y - 50.0
                val u = dx * Math.cos(angle) + dy * Math.sin(angle)
                val v = -dx * Math.sin(angle) + dy * Math.cos(angle)
                if (abs(u) <= 60 && abs(v) <= 6) map[y * width + x] = 0.9f
            }
        }
        map[5 * width + 5] = 0.9f
        val boxes = TextDetection.boxes(map, width, height)
        assertEquals(1, boxes.size)
        val box = boxes.single()
        val slope = (box.ys[1] - box.ys[0]) / (box.xs[1] - box.xs[0])
        assertEquals(Math.tan(angle), slope.toDouble(), 0.02)
        // Grown by the unclip distance: 120 · 12 · 1.5 / (2 · 132) ≈ 8 on each side
        assertEquals(120f + 16f, box.width, 3f)
        assertEquals(12f + 16f, box.height, 3f)
        assertTrue(box.xs[0] < box.xs[1] && box.ys[0] < box.ys[3])
    }

    @Test
    fun readsRussianTexts() {
        val dir = File("../app/src/main/assets/ocr")
        val ocr = PaddleOcr(
            File(dir, "det.onnx").readBytes(),
            File(dir, "rec_eslav.onnx").readBytes(),
            File(dir, "eslav_dict.txt").readLines(),
        )
        val texts = listOf(
            Placed("Огненный шар", 0.30f, 0.62f, 26f, 0f),
            Placed("Ледяная стрела", 0.50f, 0.62f, 26f, -4f),
            Placed("Чародейский интеллект", 0.70f, 0.62f, 22f, 3f),
            Placed("Стартовая рука", 0.50f, 0.12f, 40f, 0f),
            Placed("ЧУЖОЙ ХОД", 0.85f, 0.46f, 22f, 0f),
            Placed("Ваш ход", 0.50f, 0.85f, 48f, 0f),
            Placed("Охотник на демонов", 0.30f, 0.30f, 20f, 0f),
            Placed("Монетка", 0.15f, 0.85f, 24f, 0f),
        )
        val screen = render(1600, 720, texts)
        val lines = ocr.use { it.read(screen) }
        for (placed in texts) {
            val expected = CardNameIndex.normalize(placed.text)
            val read = lines.minByOrNull { abs(it.centerX - placed.x) + abs(it.centerY - placed.y) }
            val normalized = read?.let { CardNameIndex.normalize(it.text) }.orEmpty()
            assertTrue(CardNameIndex.similarity(expected, normalized) >= 0.85, "${placed.text}: read '${read?.text}'")
        }
    }

    private class Placed(val text: String, val x: Float, val y: Float, val size: Float, val degrees: Float)

    /** Light text with a dark outline on a textured background, like Hearthstone's card banners. */
    private fun render(width: Int, height: Int, texts: List<Placed>): ArgbImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        for (y in 0 until height step 8) {
            for (x in 0 until width step 8) {
                g.color = Color(60 + (x * 7 + y * 3) % 40, 45 + (x + y * 5) % 30, 30 + (x * 3 + y) % 25)
                g.fillRect(x, y, 8, 8)
            }
        }
        for (placed in texts) {
            val font = Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(placed.size)
            val layout = TextLayout(placed.text, font, g.fontRenderContext)
            val bounds = layout.bounds
            val transform = AffineTransform().apply {
                translate(placed.x * width.toDouble(), placed.y * height.toDouble())
                rotate(Math.toRadians(placed.degrees.toDouble()))
                translate(-bounds.centerX, -bounds.centerY)
            }
            val outline = layout.getOutline(transform)
            g.color = Color(20, 15, 10)
            g.stroke = BasicStroke(placed.size / 6)
            g.draw(outline)
            g.color = Color(250, 245, 235)
            g.fill(outline)
        }
        g.dispose()
        return ArgbImage(image.getRGB(0, 0, width, height, null, 0, width), width, height)
    }
}
