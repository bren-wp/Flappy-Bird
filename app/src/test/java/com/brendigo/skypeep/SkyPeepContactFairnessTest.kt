package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepContactFairnessTest {
    private val body=SkyPeepContact.body(172f,800f,295f,232f)

    @Test fun nearMissesAtOnePixelAndTangencyDoNotEndFlight() {
        for (distance in listOf(0f,1f,2f,6f,10f)) {
            assertFalse(SkyPeepContact.hitsGate(body,body.cx-110f,275f,
                body.cy-300f,body.top+distance))
            assertFalse(SkyPeepContact.hitsGate(body,body.cx-110f,275f,
                body.bottom-distance,body.cy+300f))
        }
    }

    @Test fun realHeadAndTorsoContactStillEndsFlight() {
        assertTrue(SkyPeepContact.hitsGate(body,body.cx-110f,275f,
            body.cy-300f,body.top-8f))
        assertTrue(SkyPeepContact.hitsGate(body,body.cx-110f,275f,
            body.bottom+8f,body.cy+300f))
    }

    @Test fun capGraphicMarginsAreNotSolidStone() {
        assertFalse(SkyPeepContact.hitsGate(body,body.right-20f,275f,
            body.cy-80f,body.cy+10f))
        assertTrue(SkyPeepContact.hitsGate(body,body.right-70f,275f,
            body.cy-80f,body.cy+10f))
    }

    @Test fun bodiesOnReferenceAndWideDisplaysUseSameFairContactShape() {
        for((width,height) in listOf(360 to 640,1080 to 2400,2048 to 1536)) {
            val frame=SkyPeepScreenFrame.forPixels(width,height)
            assertFalse(SkyPeepContact.hitsWorldEdge(body,frame.bottom,frame.top))
            assertEquals(56f,body.rx,.001f)
            assertEquals(52f,body.ry,.001f)
        }
    }
}
