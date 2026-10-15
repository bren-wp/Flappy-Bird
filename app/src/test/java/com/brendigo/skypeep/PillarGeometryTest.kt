package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class PillarGeometryTest {
    @Test fun shortPillarsExtendToTheFloor() {
        assertEquals(153f,PillarGeometry.bottomTailHeight(890f,761f),0.001f)
        assertEquals(0f,PillarGeometry.bottomTailHeight(400f,761f),0.001f)
    }

    @Test fun topTileOverlapsIllustratedCap() {
        assertEquals(1140f,PillarGeometry.topTailStart(760f,500f),0.001f)
        assertTrue(PillarGeometry.topTailStart(900f,500f)>900f)
    }
}
