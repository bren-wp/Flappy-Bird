package com.brendigo.skypeep

/**
 * Pure, testable touch geometry for the *visible* illustrated controls.
 * Coordinates use the 941 x 1672 reference canvas with the origin at top-left.
 * LibGDX's FitViewport.unproject() is performed by SkyPeepGame before lookup.
 */
internal object SkyPeepUiTouch {
    internal data class Rect(
        val left: Float, val top: Float, val width: Float, val height: Float
    ) {
        fun contains(x: Float, y: Float): Boolean =
            x >= left && x <= left + width && y >= top && y <= top + height
    }

    internal enum class PauseAction { RESUME, RESTART, HOME, MUSIC, SFX }

    // Deliberate non-interactive gutters between these five controls.
    private val pauseTargets = listOf(
        PauseAction.RESUME to Rect(220f, 650f, 500f, 145f),
        PauseAction.RESTART to Rect(220f, 810f, 500f, 145f),
        PauseAction.HOME to Rect(220f, 970f, 500f, 140f),
        PauseAction.MUSIC to Rect(200f, 1140f, 545f, 103f),
        PauseAction.SFX to Rect(200f, 1255f, 545f, 101f)
    )

    fun pauseActionAt(x: Float, yTop: Float): PauseAction? =
        pauseTargets.firstOrNull { (_, bounds) -> bounds.contains(x, yTop) }?.first

    // The original SKINS screenshot contains four BUY buttons. Only their
    // visible bottom-row area is actionable; tapping the bird/card must not buy.
    private val skinPurchaseTargets = (0..3).map { index ->
        Rect(25f + 235f * index, 1362f, 188f, 94f)
    }

    /** 1..4 = Captain, Forest, Cosmic, Royal; null = non-button background. */
    fun skinPurchaseAt(x: Float, yTop: Float): Int? {
        val i = skinPurchaseTargets.indexOfFirst { it.contains(x, yTop) }
        return if (i >= 0) i + 1 else null
    }

    // Exact same rectangles that drawShop uses for its four BUY/EQUIP badges.
    // A tap on the illustration, price or gap must never deduct currency.
    fun shopPurchaseAt(x: Float, yTop: Float): Int? {
        for (id in 0..3) {
            val cardBottom = 1060f - id * 195f
            val visibleButtonTop = 1672f - (cardBottom + 27f + 118f)
            if (Rect(594f, visibleButtonTop, 224f, 118f).contains(x, yTop)) return id
        }
        return null
    }

    fun shopBack(x: Float, yTop: Float): Boolean =
        Rect(279f, 1290f, 384f, 137f).contains(x, yTop)

    fun settingsBack(x: Float, yTop: Float): Boolean =
        Rect(185f, 1300f, 575f, 129f).contains(x, yTop)
}
