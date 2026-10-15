package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepSkinCatalogTest {
    @Test fun everySkinHasUniqueStableIdAndName() {
        val skins = SkyPeepSkinCatalog.all
        assertEquals(listOf(0,1,2,3,4), skins.map { it.id })
        assertEquals(5, skins.map { it.name }.distinct().size)
        assertEquals(listOf("Default","Captain","Forest","Cosmic","Royal"),
            skins.map { it.name })
    }

    @Test fun cosmeticPricesMatchIllustratedShop() {
        assertEquals(listOf(0,500,800,120,1000),
            SkyPeepSkinCatalog.all.map { it.price })
        assertEquals(SkinCurrency.GEMS, SkyPeepSkinCatalog.get(3).currency)
        assertTrue(SkyPeepSkinCatalog.all.filter { it.id != 3 }
            .all { it.currency == SkinCurrency.COINS })
    }

    @Test fun purchasesRespectCurrencyAndExactBalance() {
        assertFalse(SkyPeepSkinCatalog.get(3).affordable(10_000, 119))
        assertTrue(SkyPeepSkinCatalog.get(3).affordable(0, 120))
        assertFalse(SkyPeepSkinCatalog.get(2).affordable(799, 999))
        assertTrue(SkyPeepSkinCatalog.get(2).affordable(800, 0))
        assertTrue(SkyPeepSkinCatalog.get(0).affordable(0, 0))
    }
}
