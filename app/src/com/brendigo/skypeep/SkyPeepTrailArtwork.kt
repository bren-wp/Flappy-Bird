package com.brendigo.skypeep

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture

/** Small reusable alpha-only 2D speed-line texture tinted by equipped trail. */
internal object SkyPeepTrailArtwork {
    fun create():Texture {
        val p=Pixmap(200,120,Pixmap.Format.RGBA8888)
        p.setColor(0f,0f,0f,0f)
        p.fill()
        val segments=arrayOf(
            intArrayOf(8,72,182,58,184,52),
            intArrayOf(1,43,146,51,165,47),
            intArrayOf(38,100,167,72,183,66)
        )
        for((i,v) in segments.withIndex()) {
            p.setColor(Color(1f,1f,1f,if(i==0).85f else .54f))
            p.fillTriangle(v[0],v[1],v[2],v[3],v[4],v[5])
        }
        val texture=Texture(p)
        texture.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear)
        p.dispose()
        return texture
    }
}
