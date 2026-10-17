package com.brendigo.skypeep

/**
 * Responsive portrait overscan. Never stretch a 4px solid colour into the
 * extra display region: sample up to 12% of the actual source illustration.
 * The 941x1672 authored content and touch geometry remain untouched.
 */
internal object SkyPeepFullBleed {
    /** Use a genuine 32px-wide illustrated edge strip, never a stretched 12px line. */
    fun safeUiStrip(sourcePixels: Int): Int {
        require(sourcePixels > 0)
        return sourcePixels.coerceAtMost(32)
    }
    /** The bottom middle of menu art is scenery, while the left edge may be
     * occupied by large illustrated islands. Matching the real core's centre
     * prevents a coloured horizontal join on extra-tall phones.
     */
    fun safeBottomSceneryX(sourcePixels: Int): Int =
        (sourcePixels-safeUiStrip(sourcePixels)) / 2


    fun sample(sourcePixels: Int): Int {
        require(sourcePixels > 0)
        return (sourcePixels / 8).coerceIn(4, 128).coerceAtMost(sourcePixels)
    }
}
