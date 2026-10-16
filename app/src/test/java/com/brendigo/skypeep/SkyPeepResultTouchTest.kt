package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepResultTouchTest {
    @Test fun allVisibleButtonCentersWork() {
        for(won in listOf(false,true)) {
            val actions=if(won) SkyPeepResultTouch.victoryButtons
                else SkyPeepResultTouch.lossButtons
            for(button in actions) assertEquals(
                button.action,
                SkyPeepResultTouch.actionAt(
                    button.left+button.width/2f,
                    button.top+button.height/2f,won)
            )
        }
    }

    @Test fun emptyMarginsAndGuttersNeverActivate() {
        for(won in listOf(false,true)) {
            for((x,y) in listOf(
                10f to 1380f, 930f to 1380f,
                320f to 1380f, 618f to 1380f,
                470f to 1100f, 470f to 1550f
            )) assertNull("Spurious hit at $x,$y win=$won",
                SkyPeepResultTouch.actionAt(x,y,won))
        }
    }

    @Test fun outerControlsExcludeNarrowBlankSideMargins() {
        for (x in listOf(80f,85f,93f,851f,860f,869f)) {
            assertNull("Blank edge x=$x must be inert",
                SkyPeepResultTouch.actionAt(x,1335f,false))
        }
        assertEquals(SkyPeepResultTouch.Action.HOME,
            SkyPeepResultTouch.actionAt(94f,1335f,false))
        assertEquals(SkyPeepResultTouch.Action.SHARE,
            SkyPeepResultTouch.actionAt(850f,1335f,false))
    }

    @Test fun lossBackgroundAboveAndBelowDrawnButtonsIsNotClickable() {
        for(y in listOf(1195f,1210f,1239f,1436f,1490f,1505f)) {
            assertNull("Result card background must be inert at y=$y",
                SkyPeepResultTouch.actionAt(470f,y,false))
        }
        assertEquals(SkyPeepResultTouch.Action.RETRY_OR_NEXT,
            SkyPeepResultTouch.actionAt(470f,1240f,false))
        assertEquals(SkyPeepResultTouch.Action.RETRY_OR_NEXT,
            SkyPeepResultTouch.actionAt(470f,1435f,false))
    }

    @Test fun victoryHitboxesMatchTheirDrawnRectangles() {
        for(b in SkyPeepResultTouch.victoryButtons) {
            assertEquals(b.action,SkyPeepResultTouch.actionAt(b.left,b.top,true))
            assertEquals(b.action,SkyPeepResultTouch.actionAt(
                b.left+b.width,b.top+b.height,true))
            assertNull(SkyPeepResultTouch.actionAt(
                b.left+b.width/2f,b.top-1f,true))
            assertNull(SkyPeepResultTouch.actionAt(
                b.left+b.width/2f,b.top+b.height+1f,true))
        }
    }
}
