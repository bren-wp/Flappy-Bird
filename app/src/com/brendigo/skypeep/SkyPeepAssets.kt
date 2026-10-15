package com.brendigo.skypeep

/**
 * Defines the screen image actually needed by a visible UI state.
 * Playing the game needs no full-screen menu illustration in GPU memory.
 */
internal object SkyPeepAssets {
    fun screenFor(page: String): String? = when (page) {
        "SPLASH" -> "screen_splash"
        "MENU", "SETTINGS" -> "screen_menu"
        "MAP" -> "screen_map"
        "PAUSE" -> "screen_pause"
        "RESULT" -> "screen_over"
        "SKINS", "SHOP" -> "screen_skins"
        "PLAY" -> null
        else -> null
    }
}
