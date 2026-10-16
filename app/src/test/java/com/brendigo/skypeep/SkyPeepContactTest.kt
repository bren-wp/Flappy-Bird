package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepContactTest {
    private val body = SkyPeepContact.body(172f,800f,295f,232f)

    @Test fun bodyIsInsideIllustratedBirdRatherThanScarfOrTransparentWingTips() {
        assertTrue(body.left > 172f + 30f)
        assertTrue(body.right < 172f + 295f - 20f)
        assertTrue(body.bottom > 800f + 20f)
        assertTrue(body.top < 800f + 232f - 30f)
    }

    @Test fun nearMissInsidePassageMustNotKillPlayer() {
        // The visible torso is clear of the top stone by 1 pixel.
        val gapTop = body.top + 1f
        assertFalse(SkyPeepContact.hitsGate(body,body.cx - 120f,275f,500f,gapTop))
        // Exact edge touch is not a penetration either.
        assertFalse(SkyPeepContact.hitsGate(body,body.cx - 120f,275f,500f,body.top))
        assertTrue(SkyPeepContact.hitsGate(body,body.cx - 120f,275f,500f,body.top - 3f))
    }

    @Test fun nearMissAroundPillarCornerDoesNotCauseInvisibleCollision() {
        val left = body.cx + body.rx * .91f
        val lower = body.cy + body.ry * .63f
        assertFalse(SkyPeepContact.intersectsRectangle(
            body,left,left+220f,lower,lower+350f))
        assertTrue(SkyPeepContact.intersectsRectangle(
            body,body.cx+body.rx*.4f,body.cx+220f,
            body.cy+body.ry*.4f,body.cy+350f))
    }

    @Test fun visuallyEmptySideMarginsAreNotSolidStone() {
        val gateX = body.right - 12f
        // Only the first 12px of the textured pillar's transparent side
        // overlaps; its visible cap starts after the 22px safety inset.
        assertFalse(SkyPeepContact.hitsGate(body,gateX,275f,100f,body.cy))
        assertTrue(SkyPeepContact.hitsGate(body,gateX-45f,275f,100f,body.cy))
    }

    @Test fun realHitsBelowAndAboveOpeningStillEndFlight() {
        assertTrue(SkyPeepContact.hitsGate(body,body.cx-100f,275f,
            body.cy+12f,body.cy+250f))
        assertTrue(SkyPeepContact.hitsGate(body,body.cx-100f,275f,
            body.cy-250f,body.cy-12f))
    }

    @Test fun floorAndCeilingUseVisibleCanvasBoundsNotInvisibleOldMargins() {
        assertFalse(SkyPeepContact.hitsWorldEdge(body,0f,1672f))
        assertFalse(SkyPeepContact.hitsWorldEdge(body,
            body.bottom,body.top))
        assertTrue(SkyPeepContact.hitsWorldEdge(body,
            body.bottom+1f,body.top))
        assertTrue(SkyPeepContact.hitsWorldEdge(body,
            body.bottom,body.top-1f))
    }
}
