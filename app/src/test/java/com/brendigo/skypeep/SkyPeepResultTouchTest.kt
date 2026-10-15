package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepResultTouchTest {
    @Test fun victoryButtonsMatchExactlyDrawnRects() {
        for(button in SkyPeepResultTouch.victoryButtons) {
            assertEquals(button.action,SkyPeepResultTouch.actionAt(
                button.left+button.width/2,button.top+button.height/2,true))
            assertNull(SkyPeepResultTouch.actionAt(
                button.left-1f,button.top+button.height/2,true).takeIf {
                    // Only this button may be selected, not a neighbour.
                    it == button.action
                })
        }
    }
    @Test fun guttersAndOuterMarginsNeverActivateControls() {
        for(won in listOf(true,false)) {
            assertNull(SkyPeepResultTouch.actionAt(320f,1380f,won))
            assertNull(SkyPeepResultTouch.actionAt(618f,1380f,won))
            assertNull(SkyPeepResultTouch.actionAt(10f,1380f,won))
            assertNull(SkyPeepResultTouch.actionAt(900f,1380f,won))
            assertNull(SkyPeepResultTouch.actionAt(470f,1100f,won))
            assertNull(SkyPeepResultTouch.actionAt(470f,1530f,won))
        }
    }
    @Test fun originalLossButtonsStillSupportRetryHomeAndShare() {
        assertEquals(SkyPeepResultTouch.Action.HOME,SkyPeepResultTouch.actionAt(185f,1380f,false))
        assertEquals(SkyPeepResultTouch.Action.RETRY_OR_NEXT,SkyPeepResultTouch.actionAt(470f,1380f,false))
        assertEquals(SkyPeepResultTouch.Action.SHARE,SkyPeepResultTouch.actionAt(755f,1380f,false))
        assertEquals(SkyPeepResultTouch.Action.RETRY_OR_NEXT,SkyPeepResultTouch.actionAt(470f,1380f,true))
    }
}
