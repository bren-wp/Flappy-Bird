package com.brendigo.skypeep

/**
 * Button geometry in reference-artwork coordinates (941 × 1672, top-left origin).
 * Win buttons are drawn from these exact rectangles. The failure view uses
 * buttons baked into screen_over.jpg, measured from the real illustration.
 */
internal object SkyPeepResultTouch {
    enum class Action { HOME, RETRY_OR_NEXT, SHARE }

    data class Button(
        val action: Action, val left: Float, val top: Float,
        val width: Float, val height: Float
    ) {
        init { require(width > 0f && height > 0f) }
        fun contains(x: Float, y: Float): Boolean =
            x >= left && x <= left + width && y >= top && y <= top + height
    }

    val victoryButtons = listOf(
        Button(Action.HOME, 57f, 1308f, 256f, 157f),
        Button(Action.RETRY_OR_NEXT, 334f, 1308f, 274f, 157f),
        Button(Action.SHARE, 629f, 1308f, 256f, 157f)
    )

    // Restrict loss targets to the actual visible button row, not the
    // oversized y=1195..1505 that was flagged in PR #16's code review.
    val lossButtons = listOf(
        Button(Action.HOME, 80f, 1240f, 235f, 195f),
        Button(Action.RETRY_OR_NEXT, 334f, 1240f, 274f, 195f),
        Button(Action.SHARE, 629f, 1240f, 240f, 195f)
    )

    fun actionAt(x: Float, yTop: Float, won: Boolean): Action? =
        (if (won) victoryButtons else lossButtons)
            .firstOrNull { it.contains(x, yTop) }?.action
}
