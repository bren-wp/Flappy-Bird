package com.brendigo.skypeep

/**
 * Layout uses the real illustrated bird texture's natural 443:349 aspect.
 * The earlier 245x215 drawing rectangle compressed SkyPeep horizontally.
 * Physics, texture rendering and costume overlays share these coordinates.
 */
internal object SkyPeepBirdLayout {
    const val WIDTH = 295f
    const val HEIGHT = 232f

    // Keep original texture proportions to within source-pixel rounding.
    fun aspectError(): Float =
        kotlin.math.abs(WIDTH / HEIGHT - 443f / 349f)

    // Original goggles and hats follow the same scalable proportions as bird.
    const val COSTUME_X = 65f
    const val COSTUME_Y_FROM_TOP = 97f
    const val COSTUME_WIDTH = 168f
    const val COSTUME_HEIGHT = 101f
}
