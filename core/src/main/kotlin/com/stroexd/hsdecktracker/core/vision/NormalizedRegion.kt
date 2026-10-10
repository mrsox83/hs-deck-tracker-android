package com.stroexd.hsdecktracker.core.vision

import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A screen region stored independently of device resolution. Coordinates are fractions of the captured content,
 * not absolute display coordinates, so app-only and whole-display captures can share the same region definitions.
 */
@Serializable
data class NormalizedRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f)
        require(left < right && top < bottom)
    }

    fun pixels(width: Int, height: Int): ScreenBox {
        require(width > 0 && height > 0)
        return ScreenBox(
            left = floor(left * width).toInt().coerceIn(0, width - 1),
            top = floor(top * height).toInt().coerceIn(0, height - 1),
            right = ceil(right * width).toInt().coerceIn(1, width),
            bottom = ceil(bottom * height).toInt().coerceIn(1, height),
        )
    }
}
