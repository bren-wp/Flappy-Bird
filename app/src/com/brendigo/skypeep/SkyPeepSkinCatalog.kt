package com.brendigo.skypeep

/** Prices and currency are defined once for preview, purchase and regression tests. */
internal enum class SkinCurrency { COINS, GEMS }

internal data class BirdSkin(
    val id: Int,
    val name: String,
    val price: Int,
    val currency: SkinCurrency
) {
    fun affordable(coins: Int, gems: Int): Boolean = when(currency) {
        SkinCurrency.COINS -> coins >= price
        SkinCurrency.GEMS -> gems >= price
    }
}

internal object SkyPeepSkinCatalog {
    val all = listOf(
        BirdSkin(0, "Default", 0, SkinCurrency.COINS),
        BirdSkin(1, "Captain", 500, SkinCurrency.COINS),
        BirdSkin(2, "Forest", 800, SkinCurrency.COINS),
        BirdSkin(3, "Cosmic", 120, SkinCurrency.GEMS),
        BirdSkin(4, "Royal", 1000, SkinCurrency.COINS)
    )
    fun get(id: Int): BirdSkin = all[id.coerceIn(0, all.lastIndex)]
}
