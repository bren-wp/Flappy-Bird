package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepEconomyTest {
    @Test fun normalCoinsAndGemsAreCreditedExactly() {
        assertEquals(1255,SkyPeepEconomy.credit(1250,5))
        assertEquals(76,SkyPeepEconomy.credit(75,1))
        assertEquals(190,SkyPeepEconomy.credit(0,FlightRules.winBonus(6)))
    }

    @Test fun limitsNeverOverflowToNegativeBalance() {
        assertEquals(Int.MAX_VALUE,SkyPeepEconomy.credit(Int.MAX_VALUE,1))
        assertEquals(Int.MAX_VALUE,SkyPeepEconomy.credit(Int.MAX_VALUE-3,100))
        assertEquals(Int.MAX_VALUE,SkyPeepEconomy.credit(Int.MAX_VALUE,0))
        assertEquals(5,SkyPeepEconomy.credit(-999,5))
    }

    @Test fun rejectingNegativeRewardsPreventsAccidentalCreditsOrDebits() {
        try {
            SkyPeepEconomy.credit(100,-1)
            fail("Expected negative credit to be rejected")
        } catch (_:IllegalArgumentException) {
            // Expected.
        }
    }
}
