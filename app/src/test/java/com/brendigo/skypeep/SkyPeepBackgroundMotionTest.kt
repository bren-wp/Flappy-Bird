package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepBackgroundMotionTest {
    @Test fun cropsNeverLeaveTheSourceTexture() {
        for (width in listOf(1, 8, 128, 864, 1024, 2048)) {
            for (step in 0..400) {
                val crop = SkyPeepBackgroundMotion.crop(width, step * .35f, true)
                assertTrue(crop.sourceX >= 0)
                assertTrue(crop.sourceWidth > 0)
                assertTrue(crop.sourceX + crop.sourceWidth <= width)
            }
        }
    }

    @Test fun idleSceneIsDeterministicForReferenceScreenshots() {
        val first = SkyPeepBackgroundMotion.crop(864, 0f, false)
        assertEquals(first, SkyPeepBackgroundMotion.crop(864, 90f, false))
        assertEquals(first, SkyPeepBackgroundMotion.crop(864, 900f, false))
    }

    @Test fun motionIsBoundedAndNonzeroDuringFlight() {
        val width = 864
        val first = SkyPeepBackgroundMotion.crop(width, 0f, true)
        val later = SkyPeepBackgroundMotion.crop(width, 13f, true)
        assertEquals(first.sourceWidth, later.sourceWidth)
        assertNotEquals(first.sourceX, later.sourceX)
        assertTrue(first.sourceWidth >= (width * .96).toInt())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidTextures() {
        SkyPeepBackgroundMotion.crop(0, 1f, true)
    }
}
