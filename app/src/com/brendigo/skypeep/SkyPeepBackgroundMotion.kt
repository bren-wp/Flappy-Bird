package com.brendigo.skypeep

import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Subtle, seam-free horizontal movement within the existing illustrated world.
 * The full reference image remains visible before the first flap. A gentle
 * camera crop is introduced over 1.4 seconds to avoid a sudden visual jump.
 */
internal object SkyPeepBackgroundMotion {
    internal data class Crop(val sourceX: Int, val sourceWidth: Int)

    fun crop(textureWidth: Int, elapsedSeconds: Float, playing: Boolean): Crop {
        require(textureWidth > 0)
        val maximumMargin = (textureWidth * 0.018f).roundToInt()
            .coerceAtMost((textureWidth - 1) / 2)
        if (!playing || maximumMargin == 0) return Crop(0, textureWidth)

        // Startup and pause/resume must not jump or stretch the illustrated sky.
        val elapsed = elapsedSeconds.coerceAtLeast(0f)
        val eased = (elapsed / 1.4f).coerceIn(0f, 1f)
        val margin = (maximumMargin * eased).roundToInt()
        if (margin == 0) return Crop(0, textureWidth)

        val drift = (margin * sin(elapsed.toDouble() * 0.13).toFloat()).roundToInt()
        val sourceX = (margin + drift).coerceIn(0, margin * 2)
        return Crop(sourceX, textureWidth - margin * 2)
    }
}
