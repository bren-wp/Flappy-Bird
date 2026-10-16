package com.brendigo.skypeep

import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Conservative camera drift within the existing illustrated gameplay scene.
 * Cropping, rather than repeating, avoids a hard seam at the horizon.
 * This class is platform independent for deterministic JVM tests.
 */
internal object SkyPeepBackgroundMotion {
    internal data class Crop(val sourceX: Int, val sourceWidth: Int)

    fun crop(textureWidth: Int, elapsedSeconds: Float, playing: Boolean): Crop {
        require(textureWidth > 0)
        val margin = (textureWidth * 0.018f).roundToInt().coerceAtMost((textureWidth - 1) / 2)
        val visible = textureWidth - margin * 2
        if (margin == 0 || !playing) return Crop(margin, visible)
        val phase = sin(elapsedSeconds.toDouble() * 0.13).toFloat()
        val x = (margin + margin * phase).roundToInt().coerceIn(0, margin * 2)
        return Crop(x, visible)
    }
}
