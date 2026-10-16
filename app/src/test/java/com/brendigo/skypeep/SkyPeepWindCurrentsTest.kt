package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class SkyPeepWindCurrentsTest {
    @Test fun firstWorldIsCalmAndLaterWorldsHaveCurrents() {
        for (difficulty in SkyDifficulty.entries) {
            for (world in 1..6) {
                assertEquals(0f, SkyRouteRules.windAcceleration(world, difficulty, 0f), .001f)
                var active = false
                for (i in 1..100) {
                    val wind = SkyRouteRules.windAcceleration(world, difficulty, i * .17f)
                    assertTrue(abs(wind) <= 240f)
                    if (abs(wind) > 1f) active = true
                }
                assertEquals(world != 1, active)
            }
        }
    }

    @Test fun savedDifficultyDefaultsToExplorerAndCyclesPredictably() {
        assertEquals(SkyDifficulty.EXPLORER, SkyDifficulty.fromStored(-1))
        assertEquals(SkyDifficulty.EXPLORER, SkyDifficulty.fromStored(99))
        assertEquals(SkyDifficulty.TEMPEST, SkyDifficulty.EXPLORER.next())
        assertEquals(SkyDifficulty.BREEZE, SkyDifficulty.TEMPEST.next())
    }
}
