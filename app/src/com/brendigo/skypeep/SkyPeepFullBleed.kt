package com.brendigo.skypeep

/**
 * Responsive portrait overscan. Never stretch a 4px solid colour into the
 * extra display region: sample up to 12% of the actual source illustration.
 * The 941x1672 authored content and touch geometry remain untouched.
 */
internal object SkyPeepFullBleed {
    /** The user-facing menu artwork has controls near its outer edges.\n     * Never duplicate those controls into wide Android overscan.\n     */
    fun safeUiStrip(sourcePixels: Int): Int {
        require(sourcePixels > 0)
        return sourcePixels.coerceAtMost(12)
    }
    /** Sample the complete original image width at the actual top/bottom
     * edge. A former implementation mixed a centred 12px column origin with
     * a full-width draw, producing out-of-bounds UVs on tall UI screens.
     */
    internal data class SourceBand(val x: Int, val y: Int,
                                   val width: Int, val height: Int)

    fun horizontalBand(sourceWidth: Int, sourceHeight: Int,
                       scenery: Boolean, bottom: Boolean): SourceBand {
        require(sourceWidth > 0 && sourceHeight > 0)
        val rows = if (scenery) sample(sourceHeight) else safeUiStrip(sourceHeight)
        return SourceBand(0, if (bottom) sourceHeight - rows else 0,
                          sourceWidth, rows)
    }

    fun sample(sourcePixels: Int): Int {
        require(sourcePixels > 0)
        return (sourcePixels / 8).coerceIn(4, 128).coerceAtMost(sourcePixels)
    }
}
