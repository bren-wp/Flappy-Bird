package com.brendigo.skypeep

/**
 * In-game wallet math. Corrupt persisted balances and rewards must never wrap
 * Int.MAX_VALUE into negative currency or inadvertently enable a purchase.
 * The wallet is local-only and is never transmitted by the app.
 */
internal object SkyPeepEconomy {
    fun credit(balance: Int, reward: Int): Int {
        require(reward >= 0) { "Negative credit is not permitted" }
        val safeBalance = balance.coerceAtLeast(0)
        return if (safeBalance > Int.MAX_VALUE - reward) Int.MAX_VALUE
               else safeBalance + reward
    }
}
