package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

/** All coordinates are top-left oriented, matching the 941x1672 design. */
class SkyPeepUiTouchTest {
    @Test fun pauseButtonsDoNotOverlapAndGuttersAreInert() {
        assertEquals(SkyPeepUiTouch.PauseAction.RESUME, SkyPeepUiTouch.pauseActionAt(470f, 720f))
        assertEquals(SkyPeepUiTouch.PauseAction.RESTART, SkyPeepUiTouch.pauseActionAt(470f, 875f))
        assertEquals(SkyPeepUiTouch.PauseAction.HOME, SkyPeepUiTouch.pauseActionAt(470f, 1040f))
        assertEquals(SkyPeepUiTouch.PauseAction.MUSIC, SkyPeepUiTouch.pauseActionAt(580f, 1180f))
        assertEquals(SkyPeepUiTouch.PauseAction.SFX, SkyPeepUiTouch.pauseActionAt(580f, 1300f))
        for (y in listOf(630f, 800f, 962f, 1120f, 1249f, 1390f)) {
            assertNull("Unexpected pause action at y=$y", SkyPeepUiTouch.pauseActionAt(470f, y))
        }
        assertNull(SkyPeepUiTouch.pauseActionAt(60f, 720f))
        assertNull(SkyPeepUiTouch.pauseActionAt(900f, 875f))
    }

    @Test fun skinsRequireTheActualBuyButtonRatherThanAnyCardTap() {
        for (skin in 1..4) {
            val rect = SkyPeepUiTouch.skinPurchaseRect(skin)
            assertEquals(skin, SkyPeepUiTouch.skinPurchaseAt(rect.left + 80f, 1400f))
            assertNull(SkyPeepUiTouch.skinPurchaseAt(rect.left + 80f, 1100f))
            assertNull(SkyPeepUiTouch.skinPurchaseAt(rect.left + 80f, 1460f))
        }
        assertNull(SkyPeepUiTouch.skinPurchaseAt(230f, 1400f))
        assertNull(SkyPeepUiTouch.skinPurchaseAt(940f, 1400f))
        assertEquals(1, SkyPeepUiTouch.skinPurchaseAt(100f, 1450f))
        assertFalse("Captain BUY must not go back", SkyPeepUiTouch.skinsBack(100f, 1450f))
        assertTrue(SkyPeepUiTouch.skinsBack(100f, 1560f))
        assertFalse(SkyPeepUiTouch.skinsBack(100f, 1490f))
    }

    @Test fun shopOnlyAcceptsVisibleOfferControlsAndBack() {
        for (id in 0..3) {
            val r = SkyPeepUiTouch.shopPurchaseRect(id)
            assertEquals(id, SkyPeepUiTouch.shopPurchaseAt(r.left + r.width/2, r.top + r.height/2))
            assertNull(SkyPeepUiTouch.shopPurchaseAt(400f, r.top + r.height/2))
            assertNull(SkyPeepUiTouch.shopPurchaseAt(r.left + 40f, r.top - 10f))
        }
        assertEquals(1, SkyPeepUiTouch.shopPurchaseAt(700f, 687f))
        assertNull(SkyPeepUiTouch.shopPurchaseAt(700f, 800f))
        assertTrue(SkyPeepUiTouch.shopBack(470f, 1360f))
        assertFalse(SkyPeepUiTouch.shopBack(470f, 1230f))
        assertFalse(SkyPeepUiTouch.shopBack(470f, 1450f))
    }

    @Test fun settingsRowsHaveAccurateHitboxesAndInertGutters() {
        for (row in 0..3) {
            val top = 543f + row * 143f
            assertEquals(row, SkyPeepUiTouch.settingsRowAt(470f, top+60f))
            assertEquals(row, SkyPeepUiTouch.settingsRowAt(137f, top))
            assertNull(SkyPeepUiTouch.settingsRowAt(470f, top+129f))
            assertNull(SkyPeepUiTouch.settingsRowAt(110f, top+60f))
            assertNull(SkyPeepUiTouch.settingsRowAt(830f, top+60f))
        }
        assertEquals(3, SkyPeepUiTouch.settingsRowAt(695f, 1040f))
    }

    @Test fun settingsBackHitboxMatchesTheVisibleBadge() {
        assertTrue(SkyPeepUiTouch.settingsBack(465f, 1380f))
        assertTrue(SkyPeepUiTouch.settingsBack(185f, 1300f))
        assertFalse(SkyPeepUiTouch.settingsBack(465f, 1280f))
        assertFalse(SkyPeepUiTouch.settingsBack(465f, 1450f))
        assertFalse(SkyPeepUiTouch.settingsBack(900f, 1380f))
    }
}
