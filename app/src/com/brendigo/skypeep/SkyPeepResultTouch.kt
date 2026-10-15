package com.brendigo.skypeep

/** Visible action geometry is the single source of truth for draw and input. */
internal object SkyPeepResultTouch {
    enum class Action { HOME, RETRY_OR_NEXT, SHARE }

    data class Button(
        val action:Action,val left:Float,val top:Float,val width:Float,val height:Float
    ) {
        init { require(width>0f && height>0f) }
        fun contains(x:Float,y:Float):Boolean =
            x>=left && x<=left+width && y>=top && y<=top+height
    }

    val victoryButtons=listOf(
        Button(Action.HOME,57f,1308f,256f,157f),
        Button(Action.RETRY_OR_NEXT,334f,1308f,274f,157f),
        Button(Action.SHARE,629f,1308f,256f,157f)
    )

    // Original GAME OVER illustration has a wider row than the victory UI.
    val lossButtons=listOf(
        Button(Action.HOME,80f,1195f,235f,310f),
        Button(Action.RETRY_OR_NEXT,334f,1195f,274f,310f),
        Button(Action.SHARE,629f,1195f,240f,310f)
    )

    fun actionAt(x:Float,yTop:Float,won:Boolean):Action? =
        (if(won) victoryButtons else lossButtons)
            .firstOrNull { it.contains(x,yTop) }?.action
}
