package com.brendigo.skypeep

import kotlin.math.abs
import kotlin.math.max

/** Pure gameplay geometry shared by rendering and deterministic unit tests. */
internal object FlightRules {
    fun gap(level: Int): Float = max(355f, 585f - (level.coerceIn(1, 6) - 1) * 38f)

    fun targetScore(level: Int): Int = 8 + level.coerceIn(1, 6) * 3

    fun intersectsObstacle(
        gateX: Float, gateWidth: Float, gapBottom: Float, gapTop: Float,
        birdLeft: Float, birdRight: Float, birdBottom: Float, birdTop: Float
    ): Boolean = gateX < birdRight && gateX + gateWidth > birdLeft &&
        (birdBottom < gapBottom || birdTop > gapTop)

    fun overlapsPickup(
        playerX: Float, playerY: Float, pickupX: Float, pickupY: Float,
        radiusX: Float, radiusY: Float
    ): Boolean = abs(playerX - pickupX) <= radiusX &&
        abs(playerY - pickupY) <= radiusY
}
