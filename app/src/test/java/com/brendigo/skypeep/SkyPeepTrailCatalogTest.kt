package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepTrailCatalogTest {
    @Test fun fourDistinctTrailsAndCurrencies() {
        assertEquals(listOf(0,1,2,3),SkyPeepTrailCatalog.all.map { it.id })
        assertEquals(4,SkyPeepTrailCatalog.all.map { it.name }.toSet().size)
        assertEquals(listOf(0,200,450,12),SkyPeepTrailCatalog.all.map { it.price })
        assertEquals(TrailCurrency.GEMS,SkyPeepTrailCatalog.all[3].currency)
    }
    @Test fun shopLabelsShowActualPricesNotTemplateText() {
        assertEquals("INCLUDED",SkyPeepTrailCatalog.priceLabel(SkyPeepTrailCatalog.all[0]))
        assertEquals("200 COINS",SkyPeepTrailCatalog.priceLabel(SkyPeepTrailCatalog.all[1]))
        assertEquals("450 COINS",SkyPeepTrailCatalog.priceLabel(SkyPeepTrailCatalog.all[2]))
        assertEquals("12 GEMS",SkyPeepTrailCatalog.priceLabel(SkyPeepTrailCatalog.all[3]))
    }

    @Test fun exactPriceAndInsufficientFunds() {
        val bought=SkyPeepTrailCatalog.purchase(1,false,200,4)
        assertTrue(bought.success)
        assertEquals(0,bought.coins)
        assertEquals(4,bought.gems)
        val denied=SkyPeepTrailCatalog.purchase(3,false,10000,11)
        assertFalse(denied.success)
        assertEquals(10000,denied.coins)
        assertEquals(11,denied.gems)
    }
    @Test fun equippedTrailsAreNeverChargedTwice() {
        val equip=SkyPeepTrailCatalog.purchase(2,true,0,0)
        assertTrue(equip.success)
        assertTrue(equip.alreadyOwned)
        assertEquals(0,equip.coins)
        assertEquals(0,equip.gems)
    }
    @Test fun invalidIdAndNegativeBalancesCannotPurchase() {
        assertFalse(SkyPeepTrailCatalog.purchase(-1,false,100,100).success)
        assertFalse(SkyPeepTrailCatalog.purchase(77,false,100,100).success)
        assertFalse(SkyPeepTrailCatalog.purchase(1,false,-1,200).success)
        val free=SkyPeepTrailCatalog.purchase(0,false,0,0)
        assertTrue(free.success)
    }
}
