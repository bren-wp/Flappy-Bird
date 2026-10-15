package com.brendigo.skypeep

/** Pure world-map geometry and status, shared by rendering, taps and tests. */
internal data class MapPosition(val x:Float,val y:Float)
internal data class MapRect(val left:Float,val top:Float,val right:Float,val bottom:Float) {
    fun contains(x:Float,y:Float):Boolean =
        x>=left && x<=right && y>=top && y<=bottom
}
internal enum class MapWorldStatus { LOCKED, OPEN, COMPLETED }
internal object SkyPeepMapProgress {
    fun status(world:Int,unlocked:Int,stars:Int):MapWorldStatus {
        require(world in 1..6)
        return when {
            world>unlocked.coerceIn(1,6) -> MapWorldStatus.LOCKED
            stars.coerceIn(0,3)>0 -> MapWorldStatus.COMPLETED
            else -> MapWorldStatus.OPEN
        }
    }
}
internal object SkyPeepMapLayout {
    // Coordinates refer to the untouched 941 x 1672 illustrated map.
    val back=MapRect(5f,1475f,205f,1672f)
    val play=MapRect(580f,1495f,940f,1672f)
    private val islands=listOf(
        MapRect(260f,1260f,650f,1490f),
        MapRect(540f,1000f,910f,1270f),
        MapRect(120f,795f,500f,1080f),
        MapRect(490f,585f,880f,864f),
        MapRect(135f,315f,530f,632f),
        MapRect(545f,195f,915f,498f)
    )
    private val markers=listOf(
        MapPosition(465f,1327f),
        MapPosition(705f,1112f),
        MapPosition(345f,903f),
        MapPosition(670f,695f),
        MapPosition(360f,450f),
        MapPosition(733f,278f)
    )
    private val starRows=listOf(
        MapPosition(462f,1441f),
        MapPosition(678f,1237f),
        MapPosition(350f,1030f),
        MapPosition(634f,813f),
        MapPosition(341f,600f),
        MapPosition(724f,429f)
    )
    fun marker(world:Int)=markers[world-1]
    fun starRow(world:Int)=starRows[world-1]
    fun worldAt(x:Float,y:Float):Int? =
        islands.indexOfFirst { it.contains(x,y) }
            .takeIf { it>=0 }?.plus(1)
}
