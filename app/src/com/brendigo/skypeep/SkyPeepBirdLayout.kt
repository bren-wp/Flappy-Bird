package com.brendigo.skypeep

/**
 * Layout uses the real illustrated bird texture's natural 443:349 aspect.
 * The earlier 245x215 drawing rectangle compressed SkyPeep horizontally.
 * Physics, texture rendering and costume overlays share these coordinates.
 */
internal object SkyPeepBirdLayout {
    const val WIDTH = 295f
    const val HEIGHT = 232f

    // The up and down poses have different native source proportions:
    // idle 443x349, up 413x370, down 394x347.
    // Their height and body centre stay fixed; rendered widths are natural.
    fun widthFor(textureWidth:Int, textureHeight:Int): Float {
        require(textureWidth>0 && textureHeight>0)
        return HEIGHT * textureWidth.toFloat() / textureHeight
    }
    fun leftFor(textureWidth:Int, textureHeight:Int): Float =
        (WIDTH-widthFor(textureWidth,textureHeight))*.5f

    // Keep original idle proportions to within source-pixel rounding.
    fun aspectError(): Float =
        kotlin.math.abs(WIDTH / HEIGHT - 443f / 349f)

    // Original goggles and hats follow the same scalable proportions as bird.
    const val COSTUME_X = 65f
    const val COSTUME_Y_FROM_TOP = 97f
    const val COSTUME_WIDTH = 168f
    const val COSTUME_HEIGHT = 101f
}
