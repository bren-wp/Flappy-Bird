package com.brendigo.skypeep

/** Stable locale-independent concise labels, sized for illustrated HUD in portrait mode. */
internal object SkyPeepHudNumbers {
    fun compact(amount:Int):String {
        val n=amount.coerceAtLeast(0)
        return when {
            n < 10_000 -> n.toString()
            n < 1_000_000 -> {
                val base=n/1000
                val decimal=(n%1000)/100
                if(decimal==0) "${base}K" else "${base}.${decimal}K"
            }
            else -> {
                val base=n/1_000_000
                val decimal=(n%1_000_000)/100_000
                if(decimal==0) "${base}M" else "${base}.${decimal}M"
            }
        }
    }
}
