package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepAssetsTest {
    @Test fun usesOnlyOneScreenTextureAtATime() {
        assertEquals("screen_splash", SkyPeepAssets.screenFor("SPLASH"))
        assertEquals("screen_menu", SkyPeepAssets.screenFor("MENU"))
        assertEquals("screen_menu", SkyPeepAssets.screenFor("SETTINGS"))
        assertEquals("screen_map", SkyPeepAssets.screenFor("MAP"))
        assertEquals("screen_pause", SkyPeepAssets.screenFor("PAUSE"))
        assertEquals("screen_over", SkyPeepAssets.screenFor("RESULT"))
        assertEquals("screen_skins", SkyPeepAssets.screenFor("SHOP"))
        assertEquals("screen_skins", SkyPeepAssets.screenFor("SKINS"))
        assertNull(SkyPeepAssets.screenFor("PLAY"))
        assertNull(SkyPeepAssets.screenFor("UNKNOWN"))
    }

    @Test fun allMenuScreensHaveKnownIllustrations() {
        val available = setOf("screen_splash","screen_menu","screen_map",
            "screen_pause","screen_over","screen_skins")
        val screens = listOf("SPLASH","MENU","SETTINGS","MAP","PAUSE",
            "RESULT","SKINS","SHOP")
        assertTrue(screens.mapNotNull { SkyPeepAssets.screenFor(it) }.all { it in available })
    }
}
