package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepBackgroundMotionTest {
    @Test fun cropsNeverLeaveTheSourceTexture() {
        for (width in listOf(1, 8, 128, 706, 864, 1024, 2048)) {
            for (step in 0..400) {
                val crop = SkyPeepBackgroundMotion.crop(width, step * .35f, true)
                assertTrue(crop.sourceX >= 0)
                assertTrue(crop.sourceWidth > 0)
                assertTrue(crop.sourceX + crop.sourceWidth <= width)
            }
        }
    }

    @Test fun idleScenePreservesOriginalReferencePixels() {
        val first = SkyPeepBackgroundMotion.crop(706, 0f, false)
        assertEquals(SkyPeepBackgroundMotion.Crop(0, 706), first)
        assertEquals(first, SkyPeepBackgroundMotion.crop(706, 900f, false))
        assertEquals(first, SkyPeepBackgroundMotion.crop(706, 0f, true))
    }

    @Test fun firstFlapTransitionsSmoothlyToMovingCrop() {
        val initial = SkyPeepBackgroundMotion.crop(864, 0f, true)
        val middle = SkyPeepBackgroundMotion.crop(864, .7f, true)
        val final = SkyPeepBackgroundMotion.crop(864, 1.4f, true)
        assertEquals(864, initial.sourceWidth)
        assertTrue(middle.sourceWidth < initial.sourceWidth)
        assertTrue(middle.sourceWidth > final.sourceWidth)
    }

    @Test fun motionIsBoundedAndNonzeroDuringFlight() {
        val width = 864
        val first = SkyPeepBackgroundMotion.crop(width, 2f, true)
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
