package com.brendigo.skypeep

import kotlin.math.ceil

/**
 * Bounded Android render delta and short gameplay physics substeps.
 *
 * This is not a pixel hitbox approximation: it prevents a slow GL frame
 * from jumping between widely separated bird/gate contact positions.
 * The render delta cap is the same as the previous SkyPeepGame limit.
 */
internal object SkyPeepPhysicsStep {
    const val MAX_FRAME_SECONDS = .05f
    const val MAX_STEP_SECONDS = 1f / 120f

    fun boundedDelta(seconds: Float): Float =
        if (seconds.isFinite() && seconds > 0f)
            seconds.coerceAtMost(MAX_FRAME_SECONDS) else 0f

    fun substepCount(seconds: Float): Int {
        val bounded = boundedDelta(seconds)
        if (bounded == 0f) return 0
        return ceil((bounded / MAX_STEP_SECONDS).toDouble()).toInt()
            .coerceIn(1, 6)
    }
}
