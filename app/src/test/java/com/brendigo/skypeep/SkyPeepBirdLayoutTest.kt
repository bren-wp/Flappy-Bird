package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepBirdLayoutTest {
    @Test fun illustratedBirdKeepsSourceTextureAspect() {
        assertTrue("Bird must not be squeezed into the old narrow rect",
            SkyPeepBirdLayout.aspectError() < .004f)
        assertTrue(SkyPeepBirdLayout.WIDTH > 245f)
        assertTrue(SkyPeepBirdLayout.HEIGHT > 215f)
    }

    @Test fun cosmeticLayerStaysInsideTheVisibleBird() {
        assertTrue(SkyPeepBirdLayout.COSTUME_X >= 0f)
        assertTrue(SkyPeepBirdLayout.COSTUME_X +
            SkyPeepBirdLayout.COSTUME_WIDTH <= SkyPeepBirdLayout.WIDTH)
        assertTrue(SkyPeepBirdLayout.COSTUME_Y_FROM_TOP >
            SkyPeepBirdLayout.COSTUME_HEIGHT * .6f)
    }
}
