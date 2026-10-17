package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepHudLayoutTest {
    @Test fun referenceGeometryMatchesExistingHudAndTouchExactly() {
        val f = SkyPeepScreenFrame.forPixels(941,1672)
        assertEquals(14f,SkyPeepHudLayout.coinX(f),.0001f)
        assertEquals(635f,SkyPeepHudLayout.gemX(f),.0001f)
        assertEquals(811f,SkyPeepHudLayout.pauseX(f),.0001f)
        assertTrue(SkyPeepHudLayout.pauseContains(f,868f,65f))
        assertFalse(SkyPeepHudLayout.pauseContains(f,805f,65f))
        assertFalse(SkyPeepHudLayout.pauseContains(f,868f,156f))
    }

    @Test fun physicalEdgesAndPaintedPauseMatchAcrossDisplayRatios() {
        for ((w,h) in listOf(360 to 640,720 to 1280,
                1080 to 2400,1440 to 3200,1200 to 1920,
                2048 to 1536,2560 to 1600)) {
            val frame=SkyPeepScreenFrame.forPixels(w,h)
            val pauseX=SkyPeepHudLayout.pauseX(frame)
            val gemX=SkyPeepHudLayout.gemX(frame)
            val coinX=SkyPeepHudLayout.coinX(frame)
            // Both currency controls remain physically visible and the
            // score is never covered by a border or overlapping wallet.
            assertEquals(14f,coinX-frame.left,.01f)
            assertEquals(130f,frame.right-pauseX,.01f)
            assertEquals(176f,pauseX-gemX,.01f)
            assertTrue(coinX+269f < gemX)
            assertTrue(gemX+191f < frame.right)
            // Drawn control is [pauseX,pauseX+125] and located top-129..top-7.
            val paintedMiddleX=pauseX+62f
            val paintedMiddleY=68f-frame.extraY
            assertTrue(SkyPeepHudLayout.pauseContains(
                frame,paintedMiddleX,paintedMiddleY))
            assertFalse(SkyPeepHudLayout.pauseContains(
                frame,pauseX-6f,paintedMiddleY))
            assertFalse(SkyPeepHudLayout.pauseContains(
                frame,paintedMiddleX,156f-frame.extraY))
        }
    }
}
