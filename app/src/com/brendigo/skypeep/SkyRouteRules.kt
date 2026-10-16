package com.brendigo.skypeep

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sin

/**
 * Three SkyPeep flight licenses across the six illustrated worlds: 18 route
 * challenges, with independent bests and stars. EXPLORER reproduces the 0.3.10
 * speed, gravity, gaps and target so existing progress remains familiar.
 */
internal enum class SkyDifficulty(
    val title: String,
    val gapFactor: Float,
    val speedFactor: Float,
    val gravityFactor: Float,
    val spawnFactor: Float,
    val targetFactor: Float,
    val rewardFactor: Float,
    val windFactor: Float
) {
    BREEZE("BREEZE", 1.15f, .86f, .85f, 1.14f, .80f, .85f, .65f),
    EXPLORER("EXPLORER", 1f, 1f, 1f, 1f, 1f, 1f, 1f),
    TEMPEST("TEMPEST", .87f, 1.17f, 1.13f, .86f, 1.22f, 1.45f, 1.35f);

    fun next(): SkyDifficulty = entries[(ordinal + 1) % entries.size]

    companion object {
        // Old installations default to EXPLORER, preserving their gameplay.
        fun fromStored(raw: Int): SkyDifficulty =
            entries.getOrNull(raw) ?: EXPLORER
    }
}

/**
 * Distinctive route mechanics: varying flight licenses, optional precision
 * chains and theme-specific air currents. Every function is deterministic and
 * independent of Android/LibGDX for fast JVM regression tests.
 */
internal object SkyRouteRules {
    fun gap(world: Int, difficulty: SkyDifficulty): Float =
        (FlightRules.gap(world) * difficulty.gapFactor).coerceIn(315f, 760f)

    fun speed(world: Int, difficulty: SkyDifficulty): Float =
        (235f + (world.coerceIn(1, 6) - 1) * 22f) * difficulty.speedFactor

    fun gravity(difficulty: SkyDifficulty): Float = 1720f * difficulty.gravityFactor

    fun spawnSeconds(world: Int, difficulty: SkyDifficulty): Float =
        (2.25f - (world.coerceIn(1, 6) - 1) * .11f)
            .coerceAtLeast(1.38f) * difficulty.spawnFactor

    fun target(world: Int, difficulty: SkyDifficulty): Int =
        ceil(FlightRules.targetScore(world) * difficulty.targetFactor.toDouble())
            .toInt().coerceIn(5, 50)

    fun completionBonus(world: Int, difficulty: SkyDifficulty): Int =
        (FlightRules.winBonus(world) * difficulty.rewardFactor).toInt()
            .coerceAtLeast(0)

    /** Gentle updrafts start in CLOUD PASS; WIND GATE has the strongest gusts. */
    fun windAcceleration(world: Int, difficulty: SkyDifficulty, seconds: Float): Float {
        val strength = when (world.coerceIn(1, 6)) {
            1 -> 0f
            2 -> 65f
            3 -> 95f
            4 -> 175f
            5 -> 125f
            else -> 140f
        }
        if (strength == 0f) return 0f
        val frequency = if (world == 5) .65f else if (world == 4) 2.0f else 1.25f
        val fadeIn = (seconds.coerceAtLeast(0f) / 1.8f).coerceIn(0f, 1f)
        return (sin((seconds.toDouble() * frequency) + world) * strength *
            difficulty.windFactor * fadeIn).toFloat()
    }

    fun isPrecisionPass(birdCenterY: Float, gateCenterY: Float, gap: Float): Boolean =
        gap > 0f && abs(birdCenterY - gateCenterY) <= gap * .17f

    fun precisionReward(streak: Int): Int =
        if (streak > 0 && streak % 3 == 0) 12 else 0

    /** Earned stars reward flying the centre line, not only finishing. */
    fun starsOnCompletion(precisePasses: Int, totalPasses: Int): Int {
        if (totalPasses <= 0) return 1
        val proportion = precisePasses.coerceIn(0, totalPasses).toFloat() / totalPasses
        return when {
            proportion >= .5f -> 3
            proportion >= .25f -> 2
            else -> 1
        }
    }
}
