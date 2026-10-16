package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepPrecisionRoutesTest {
    @Test fun precisionPassesRequireCentreOfGap() {
        assertTrue(SkyRouteRules.isPrecisionPass(750f, 750f, 400f))
        assertTrue(SkyRouteRules.isPrecisionPass(802f, 750f, 400f))
        assertFalse(SkyRouteRules.isPrecisionPass(900f, 750f, 400f))
        assertFalse(SkyRouteRules.isPrecisionPass(750f, 750f, 0f))
    }

    @Test fun streakBonusesCannotPayEveryGate() {
        for (streak in 0..12) {
            val expected = if (streak > 0 && streak % 3 == 0) 12 else 0
            assertEquals(expected, SkyRouteRules.precisionReward(streak))
        }
    }

    @Test fun starsDependOnRouteAccuracy() {
        assertEquals(1, SkyRouteRules.starsOnCompletion(0, 20))
        assertEquals(1, SkyRouteRules.starsOnCompletion(4, 20))
        assertEquals(2, SkyRouteRules.starsOnCompletion(5, 20))
        assertEquals(3, SkyRouteRules.starsOnCompletion(10, 20))
        assertEquals(1, SkyRouteRules.starsOnCompletion(0, 0))
    }
}
