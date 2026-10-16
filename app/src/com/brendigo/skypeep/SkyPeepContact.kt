package com.brendigo.skypeep

import kotlin.math.max
import kotlin.math.min

/**
 * Fair contact-only collisions for the VISIBLE body of the illustrated bird.
 *
 * The wing tips, scarf, motion trail and transparent sprite margins are visual
 * effects, not solid obstacles. Ellipse-to-rectangle distance prevents the
 * diagonal-corner false positives caused by the former broad AABB test.
 *
 * All dimensions are virtual design coordinates (941 × 1672); render and
 * collision centres track the same bird sprite regardless of screen pixels.
 */
internal object SkyPeepContact {
    // The winged artwork is ~295 × 232 but the solid torso/head is smaller.
    // Allow narrow passes alongside stone caps without an invisible wing hit.
    const val BODY_CENTRE_X = .595f
    const val BODY_CENTRE_Y = .465f
    const val BODY_RADIUS_X = 68f
    const val BODY_RADIUS_Y = 65f
    const val CAP_SIDE_INSET = 22f

    internal data class Body(val cx: Float, val cy: Float, val rx: Float, val ry: Float) {
        val left: Float get() = cx - rx
        val right: Float get() = cx + rx
        val bottom: Float get() = cy - ry
        val top: Float get() = cy + ry
    }

    fun body(birdX: Float, birdY: Float, width: Float, height: Float): Body =
        Body(birdX + width * BODY_CENTRE_X, birdY + height * BODY_CENTRE_Y,
            BODY_RADIUS_X, BODY_RADIUS_Y)

    /** Strict inequality: at exact edge tangency there is no penetration. */
    fun intersectsRectangle(
        body: Body, left: Float, right: Float, bottom: Float, top: Float
    ): Boolean {
        if (right <= left || top <= bottom || body.rx <= 0f || body.ry <= 0f)
            return false
        val nearestX = body.cx.coerceIn(left, right)
        val nearestY = body.cy.coerceIn(bottom, top)
        val dx = (body.cx - nearestX) / body.rx
        val dy = (body.cy - nearestY) / body.ry
        return dx * dx + dy * dy < 1f
    }

    fun hitsGate(
        body: Body,
        gateX: Float,
        gateWidth: Float,
        gapBottom: Float,
        gapTop: Float
    ): Boolean {
        if (gapBottom >= gapTop || gateWidth <= 2f * CAP_SIDE_INSET) return false
        val left = gateX + CAP_SIDE_INSET
        val right = gateX + gateWidth - CAP_SIDE_INSET
        // Top and bottom stone shafts extend to the world's outer edges.
        // Huge extents avoid artificial hitboxes inside the open gap.
        return intersectsRectangle(body, left, right, gapTop, Float.MAX_VALUE) ||
            intersectsRectangle(body, left, right, -Float.MAX_VALUE, gapBottom)
    }

    fun hitsWorldEdge(body: Body, worldBottom: Float, worldTop: Float): Boolean =
        body.bottom < worldBottom || body.top > worldTop
}
