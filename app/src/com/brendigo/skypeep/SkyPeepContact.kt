package com.brendigo.skypeep

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
    // Contact only the beak/head/torso core. No collision for open wings,\n    // animated feather tips, scarf or transparent texture padding.
    const val BODY_CENTRE_X = .595f
    const val BODY_CENTRE_Y = .465f
    // Only the opaque belly/head is solid. Wings, scarf, goggles and beak
    // overhang are animated decoration. The previous larger ellipse treated
    // a visible few-pixel air gap beside stone as a hit.
    const val BODY_RADIUS_X = 47f
    const val BODY_RADIUS_Y = 44f
    // A small consistent grace margin accounts for filtered texture fringes,
    // invisible antialias edges and 60Hz step quantisation on real devices.
    const val CONTACT_GRACE = 5f
    const val CAP_SIDE_INSET = 36f

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
        // Touching the visual outline or a few antialiased pixels is not a
        // collision. Both axes shrink by the SAME grace in world coordinates.
        val rx = body.rx - CONTACT_GRACE
        val ry = body.ry - CONTACT_GRACE
        if (rx <= 0f || ry <= 0f) return false
        val nearestX = body.cx.coerceIn(left, right)
        val nearestY = body.cy.coerceIn(bottom, top)
        val dx = (body.cx - nearestX) / rx
        val dy = (body.cy - nearestY) / ry
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
        body.bottom < worldBottom - CONTACT_GRACE ||
        body.top > worldTop + CONTACT_GRACE
}
