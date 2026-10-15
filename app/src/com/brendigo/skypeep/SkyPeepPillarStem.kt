package com.brendigo.skypeep

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import kotlin.math.max

/** The illustrated pillars must visually connect to the screen boundaries. */
internal object PillarGeometry {
    fun bottomTailHeight(bottomY: Float, spriteHeight: Float): Float =
        max(0f,bottomY-spriteHeight+24f)

    fun topTailStart(topY: Float, spriteHeight: Float): Float =
        topY+spriteHeight*.76f
}

/** Compact repeatable 2D shaft; original foliage, banners and caps stay in PNG artwork. */
internal object SkyPeepPillarStem {
    private fun theme(world:Int):Color=when(world) {
        2 -> Color.valueOf("E7DCBA")
        3 -> Color.valueOf("344A79")
        4 -> Color.valueOf("B8B6A7")
        5 -> Color.valueOf("45317D")
        6 -> Color.valueOf("F5D78A")
        else -> Color.valueOf("E6D7B4")
    }
    fun create(world:Int):Texture {
        require(world in 1..6)
        val pix=Pixmap(128,144,Pixmap.Format.RGBA8888)
        val base=theme(world)
        val dark=Color(base).mul(.72f)
        val shadow=Color(base).mul(.84f)
        val light=Color(base).lerp(Color.WHITE,.18f)
        pix.setColor(dark)
        pix.fill()
        pix.setColor(base)
        pix.fillRectangle(6,0,116,144)
        pix.setColor(light)
        pix.fillRectangle(12,0,14,144)
        pix.setColor(shadow)
        pix.fillRectangle(102,0,15,144)
        pix.setColor(dark)
        pix.fillRectangle(6,69,115,5)
        pix.fillRectangle(6,140,115,4)
        pix.setColor(light)
        pix.fillRectangle(8,74,108,4)
        pix.fillRectangle(8,1,108,3)
        pix.setColor(shadow)
        pix.drawLine(79,6,79,65)
        pix.drawLine(42,80,42,139)
        pix.drawLine(80,94,97,109)
        pix.drawLine(97,109,91,120)
        pix.drawLine(40,25,33,33)
        pix.drawLine(33,33,44,45)
        val tex=Texture(pix)
        tex.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear)
        pix.dispose()
        return tex
    }
}
