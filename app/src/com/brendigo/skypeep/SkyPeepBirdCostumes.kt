package com.brendigo.skypeep

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture

/**
 * Lightweight 2D costume accessories composited over the original animated bird.
 * All shapes are created once; no extra downloads, photos or generated images.
 * Keeps the bird body and red scarf intact, including its 3 wing animations.
 */
internal object SkyPeepBirdCostumes {
    private const val WIDTH = 184
    private const val HEIGHT = 110

    fun prepare(): Map<Int, Texture> = (1..4).associateWith { id ->
        val canvas = Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888)
        canvas.setColor(0f,0f,0f,0f)
        canvas.fill()
        when(id) {
            1 -> captain(canvas)
            2 -> forest(canvas)
            3 -> cosmic(canvas)
            4 -> royal(canvas)
        }
        val texture = Texture(canvas)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        canvas.dispose()
        texture
    }

    private fun colour(p: Pixmap, hex: String) { p.setColor(Color.valueOf(hex)) }

    private fun captain(p: Pixmap) {
        // Rounded cream captain's cap with navy brim and gold insignia.
        colour(p,"152A49")
        p.fillRectangle(12,53,160,25)
        p.fillCircle(26,65,13)
        p.fillCircle(159,65,13)
        colour(p,"F5FCFF")
        p.fillRectangle(23,20,138,40)
        p.fillCircle(34,38,18)
        p.fillCircle(151,38,17)
        p.fillRectangle(19,53,146,13)
        colour(p,"174371")
        p.fillRectangle(18,69,149,12)
        p.fillCircle(25,74,7)
        p.fillCircle(162,74,7)
        colour(p,"FFD04E")
        p.fillCircle(92,47,12)
        colour(p,"5B3A23")
        p.drawCircle(92,47,12)
        p.drawLine(92,38,92,58)
        p.drawLine(81,53,92,61)
        p.drawLine(103,53,92,61)
        colour(p,"FFFFFF")
        p.fillRectangle(40,24,91,4)
    }

    private fun forest(p: Pixmap) {
        // Leafy forest wreath over an ochre leather aviator strap.
        colour(p,"583923")
        p.fillRectangle(19,68,143,15)
        colour(p,"8B5B2F")
        p.fillRectangle(21,69,139,8)
        for(i in 0..6) {
            val x=20+i*22
            val y=if(i==0||i==6) 44 else if(i==1||i==5) 27 else 17
            colour(p,"214F27")
            p.fillCircle(x+11,y+16,19)
            colour(p,if(i%2==0)"63C938" else "3FAF49")
            p.fillCircle(x+13,y+12,14)
            colour(p,"A5DD4A")
            p.drawLine(x+4,y+7,x+17,y+21)
        }
        colour(p,"E9D36D")
        p.fillCircle(91,67,8)
    }

    private fun cosmic(p: Pixmap) {
        // Cosmic crest and glowing constellation: not merely a colour filter.
        colour(p,"2C205D")
        p.fillCircle(92,48,40)
        colour(p,"6841C8")
        p.fillCircle(92,46,34)
        colour(p,"4939A2")
        p.fillCircle(92,46,27)
        star(p,92,22,19,"C4B0FF")
        star(p,41,40,12,"6CF3FF")
        star(p,143,35,13,"E69AFF")
        star(p,61,64,7,"FFFFFF")
        star(p,121,65,8,"A8D6FF")
    }

    private fun royal(p: Pixmap) {
        // Royal crown, individual pointed tips and inset jewel.
        colour(p,"442B37")
        p.fillRectangle(25,55,134,28)
        p.fillTriangle(28,62,25,15,72,51)
        p.fillTriangle(64,60,92,4,121,60)
        p.fillTriangle(116,51,160,15,157,65)
        colour(p,"FFC83D")
        p.fillRectangle(30,57,124,20)
        p.fillTriangle(34,59,32,24,76,55)
        p.fillTriangle(69,57,92,12,114,57)
        p.fillTriangle(108,54,152,24,149,59)
        colour(p,"FFE67B")
        p.fillRectangle(38,58,107,6)
        colour(p,"E8416B")
        p.fillCircle(92,57,11)
        colour(p,"9EF2FF")
        p.fillCircle(92,56,5)
        colour(p,"FFFFFF")
        p.fillCircle(92,52,2)
    }

    private fun star(p: Pixmap, x:Int,y:Int,r:Int,hex:String) {
        colour(p,hex)
        p.fillTriangle(x,y-r,x-r/3,y-r/4,x+r/3,y-r/4)
        p.fillTriangle(x,y+r,x-r/3,y+r/4,x+r/3,y+r/4)
        p.fillTriangle(x-r,y,x-r/4,y-r/3,x-r/4,y+r/3)
        p.fillTriangle(x+r,y,x+r/4,y-r/3,x+r/4,y+r/3)
        p.fillCircle(x,y,Math.max(3,r/3))
    }
}
