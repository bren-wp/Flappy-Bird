package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyRouteRulesTest {
    @Test fun eighteenChallengesIncreasePredictably() {
        for (world in 1..6) {
            assertTrue(SkyRouteRules.target(world, SkyDifficulty.BREEZE) < SkyRouteRules.target(world, SkyDifficulty.EXPLORER))
            assertTrue(SkyRouteRules.target(world, SkyDifficulty.EXPLORER) < SkyRouteRules.target(world, SkyDifficulty.TEMPEST))
            assertTrue(SkyRouteRules.gap(world, SkyDifficulty.BREEZE) > SkyRouteRules.gap(world, SkyDifficulty.TEMPEST))
        }
    }

    @Test fun explorerKeepsExistingWorldGoals() {
        for (world in 1..6) {
            assertEquals(FlightRules.targetScore(world), SkyRouteRules.target(world, SkyDifficulty.EXPLORER))
            assertEquals(FlightRules.gap(world), SkyRouteRules.gap(world, SkyDifficulty.EXPLORER), .001f)
            assertEquals(FlightRules.winBonus(world), SkyRouteRules.completionBonus(world, SkyDifficulty.EXPLORER))
        }
    }
}
