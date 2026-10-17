package com.brendigo.skypeep

/**
 * The score remains centred on the original 941px illustrated play area.
 * Wallets and pause stay against the ACTUAL visible display edges, including
 * wide Android tablets and foldables. Coordinates are in LibGDX world units.
 *
 * The pause touch area is intentionally slightly larger than the 125px drawn
 * control, exactly as on the original 941x1672 reference screen.
 */
internal object SkyPeepHudLayout {
    fun coinX(frame: SkyPeepScreenFrame.Frame): Float = frame.left + 14f
    fun gemX(frame: SkyPeepScreenFrame.Frame): Float = frame.right - 306f
    fun pauseX(frame: SkyPeepScreenFrame.Frame): Float = frame.right - 130f

    /** topY uses the reference image's top-down coordinates (H-worldY). */
    fun pauseContains(frame: SkyPeepScreenFrame.Frame,
                      x: Float, topY: Float): Boolean =
        x >= frame.right - 135f && x <= frame.right &&
        topY >= -frame.extraY && topY <= 155f - frame.extraY
}
