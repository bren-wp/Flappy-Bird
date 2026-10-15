package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepMapProgressTest {
    @Test fun openIsNotClearedUntilStarsAreEarned() {
        assertEquals(MapWorldStatus.COMPLETED,SkyPeepMapProgress.status(1,3,3))
        assertEquals(MapWorldStatus.COMPLETED,SkyPeepMapProgress.status(2,3,2))
        assertEquals(MapWorldStatus.OPEN,SkyPeepMapProgress.status(3,3,0))
        assertEquals(MapWorldStatus.LOCKED,SkyPeepMapProgress.status(4,3,0))
    }
    @Test fun malformedStarsAreBounded() {
        assertEquals(MapWorldStatus.OPEN,SkyPeepMapProgress.status(3,3,-1))
        assertEquals(MapWorldStatus.COMPLETED,SkyPeepMapProgress.status(3,3,500))
    }
    @Test fun playHitAreaDoesNotInterceptFirstIslandOrItsStars() {
        assertEquals(1,SkyPeepMapLayout.worldAt(600f,1475f))
        assertFalse(SkyPeepMapLayout.play.contains(600f,1475f))
        assertTrue(SkyPeepMapLayout.play.contains(760f,1580f))
        assertTrue(SkyPeepMapLayout.back.contains(100f,1540f))
    }
    @Test fun individualWorldHitsMatchPaintedIslands() {
        assertEquals(2,SkyPeepMapLayout.worldAt(730f,1140f))
        assertEquals(3,SkyPeepMapLayout.worldAt(350f,900f))
        assertEquals(4,SkyPeepMapLayout.worldAt(660f,710f))
        assertEquals(5,SkyPeepMapLayout.worldAt(340f,450f))
        assertEquals(6,SkyPeepMapLayout.worldAt(730f,300f))
        assertNull(SkyPeepMapLayout.worldAt(20f,30f))
    }
}
