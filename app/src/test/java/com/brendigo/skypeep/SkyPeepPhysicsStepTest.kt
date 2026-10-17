package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

/** Regression coverage for low-FPS frames and TEMPEST contact sampling. */
class SkyPeepPhysicsStepTest {
    @Test fun usualSixtyHertzFramesUseShortSubsteps() {
        val dt = 1f / 60f
        val steps = SkyPeepPhysicsStep.substepCount(dt)
        assertEquals(2, steps)
        assertTrue(dt / steps <= SkyPeepPhysicsStep.MAX_STEP_SECONDS + 0.000001f)
    }

    @Test fun longAndroidFrameCannotSkipFortyMillisecondsOfContactChecks() {
        for (delta in listOf(.025f, .04f, .05f)) {
            val bounded = SkyPeepPhysicsStep.boundedDelta(delta)
            val count = SkyPeepPhysicsStep.substepCount(delta)
            assertTrue(count in 3..6)
            assertTrue(bounded / count <= SkyPeepPhysicsStep.MAX_STEP_SECONDS + 0.000001f)
            assertEquals(bounded, (bounded / count) * count, .000001f)
        }
    }

    @Test fun stalledOrInvalidRenderDeltaDoesNotRunUnboundedPhysics() {
        assertEquals(.05f, SkyPeepPhysicsStep.boundedDelta(2f), 0f)
        assertEquals(6, SkyPeepPhysicsStep.substepCount(2f))
        for (delta in listOf(0f, -1f, Float.NaN,
                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(0f, SkyPeepPhysicsStep.boundedDelta(delta), 0f)
            assertEquals(0, SkyPeepPhysicsStep.substepCount(delta))
        }
    }

    @Test fun closePassChecksRemainShorterThanSmallestBodyRadius() {
        val stormSpeed = SkyRouteRules.speed(6, SkyDifficulty.TEMPEST)
        val maxStepTravel = stormSpeed * SkyPeepPhysicsStep.MAX_STEP_SECONDS
        assertTrue(maxStepTravel < SkyPeepContact.BODY_RADIUS_X / 10f)
        val terminalFall = 900f * SkyDifficulty.TEMPEST.gravityFactor
        assertTrue(terminalFall * SkyPeepPhysicsStep.MAX_STEP_SECONDS <
            SkyPeepContact.BODY_RADIUS_Y / 4f)
    }
}
