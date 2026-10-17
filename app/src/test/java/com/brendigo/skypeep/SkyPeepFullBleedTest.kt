package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepFullBleedTest {
    @Test fun edgeSamplingDoesNotProduceTinyFlatBands() {
        assertEquals(128, SkyPeepFullBleed.sample(1254))
        assertEquals(117, SkyPeepFullBleed.sample(941))
        assertEquals(4, SkyPeepFullBleed.sample(16))
        assertEquals(2, SkyPeepFullBleed.sample(2))
    }

    @Test fun menusNeverCopyWideSourceBandsContainingWalletsOrLogos() {
        for (dimension in listOf(941,1254,1672,2048,16)) {
            val pixels=SkyPeepFullBleed.safeUiStrip(dimension)
            assertTrue(pixels <= 12)
            assertTrue(pixels <= dimension)
            assertTrue(pixels > 0)
        }
        // Gameplay backgrounds have no baked wallet controls and may safely
        // use a broad naturally illustrated source region.
        assertTrue(SkyPeepFullBleed.sample(1254) >
            SkyPeepFullBleed.safeUiStrip(1254))
    }

    @Test fun overscanUsesSourceArtworkOnTallAndWideDisplays() {
        for ((w,h) in listOf(360 to 800, 1080 to 2400, 1200 to 1920,
                               1440 to 3200, 2048 to 1536, 2560 to 1600)) {
            val frame=SkyPeepScreenFrame.forPixels(w,h)
            assertTrue(frame.left <= 0f && frame.right >= 941f)
            assertTrue(frame.bottom <= 0f && frame.top >= 1672f)
            assertTrue(frame.extraX > 0f || frame.extraY > 0f)
        }
    }

    @Test(expected=IllegalArgumentException::class)
    fun invalidTextureDimensionCannotBeSampled() {
        SkyPeepFullBleed.sample(0)
    }
}
