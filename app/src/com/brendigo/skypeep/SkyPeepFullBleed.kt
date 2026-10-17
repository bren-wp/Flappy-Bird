package com.brendigo.skypeep

/**
 * Responsive portrait overscan. Never stretch a 4px solid colour into the
 * extra display region: sample up to 12% of the actual source illustration.
 * The 941x1672 authored content and touch geometry remain untouched.
 */
internal object SkyPeepFullBleed {
    /** Source UI graphics start farther in than this 12px outermost border. */
    fun safeUiStrip(sourcePixels: Int): Int {
        require(sourcePixels > 0)
        return sourcePixels.coerceAtMost(12)
    }

    fun sample(sourcePixels: Int): Int {
        require(sourcePixels > 0)
        return (sourcePixels / 8).coerceIn(4, 128).coerceAtMost(sourcePixels)
    }
}
