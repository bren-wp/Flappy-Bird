package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class FlightRulesTest {
    @Test fun levelDifficultyIsBounded() {
        assertEquals(11, FlightRules.targetScore(1))
        assertEquals(26, FlightRules.targetScore(6))
        assertEquals(585f, FlightRules.gap(1), .001f)
        assertEquals(395f, FlightRules.gap(6), .001f)
    }

    @Test fun obstacleCollisionUsesActualPassage() {
        assertFalse(FlightRules.intersectsObstacle(500f, 240f, 600f, 1000f, 220f, 364f, 800f, 950f))
        assertFalse(FlightRules.intersectsObstacle(100f, 240f, 600f, 1000f, 220f, 364f, 800f, 950f))
        assertTrue(FlightRules.intersectsObstacle(100f, 240f, 600f, 1000f, 220f, 364f, 960f, 1040f))
        assertTrue(FlightRules.intersectsObstacle(100f, 240f, 600f, 1000f, 220f, 364f, 560f, 700f))
    }

    @Test fun pickupRequiresIndependentOverlap() {
        assertFalse(FlightRules.overlapsPickup(100f, 100f, 300f, 300f, 50f, 50f))
        assertTrue(FlightRules.overlapsPickup(300f, 345f, 320f, 360f, 50f, 50f))
        assertFalse(FlightRules.overlapsPickup(300f, 345f, 320f, 440f, 50f, 50f))
    }
}
