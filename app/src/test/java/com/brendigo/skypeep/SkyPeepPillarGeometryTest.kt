package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepPillarGeometryTest {
    @Test fun fullShaftTileStartsAtFirstSourceRow() {
        assertEquals(0, PillarGeometry.trailingSourceY(144f))
    }

    @Test fun partialShaftUsesTrailingRowsToMeetPreviousTile() {
        assertEquals(72, PillarGeometry.trailingSourceY(72f))
        assertEquals(143, PillarGeometry.trailingSourceY(1f))
        assertEquals(0, PillarGeometry.trailingSourceY(143.8f))
        // Clipping at a screen edge cannot produce negative source coordinates.
        assertEquals(143, PillarGeometry.trailingSourceY(0f))
        assertEquals(0, PillarGeometry.trailingSourceY(200f))
    }

    @Test fun stemReachesBoundaryWithoutNegativeLengths() {
        assertEquals(0f, PillarGeometry.bottomTailHeight(100f, 200f), 0.001f)
        assertEquals(224f, PillarGeometry.bottomTailHeight(600f, 400f), 0.001f)
        assertEquals(1456f, PillarGeometry.topTailStart(1000f, 600f), 0.001f)
    }
}
