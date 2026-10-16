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

    @Test fun allThreeAnimationPosesKeepTheirIndividualPixelAspect() {
        for((w,h) in listOf(443 to 349,413 to 370,394 to 347)){
            val rendered=SkyPeepBirdLayout.widthFor(w,h)
            assertEquals(w.toFloat()/h,
                rendered/SkyPeepBirdLayout.HEIGHT,.0001f)
            assertEquals(SkyPeepBirdLayout.WIDTH*.5f,
                SkyPeepBirdLayout.leftFor(w,h)+rendered*.5f,.001f)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidSpriteSizesAreRejected() {
        SkyPeepBirdLayout.widthFor(413,0)
    }

    @Test fun cosmeticLayerStaysInsideTheVisibleBird() {
        assertTrue(SkyPeepBirdLayout.COSTUME_X >= 0f)
        assertTrue(SkyPeepBirdLayout.COSTUME_X +
            SkyPeepBirdLayout.COSTUME_WIDTH <= SkyPeepBirdLayout.WIDTH)
        assertTrue(SkyPeepBirdLayout.COSTUME_Y_FROM_TOP >
            SkyPeepBirdLayout.COSTUME_HEIGHT * .6f)
    }
}
