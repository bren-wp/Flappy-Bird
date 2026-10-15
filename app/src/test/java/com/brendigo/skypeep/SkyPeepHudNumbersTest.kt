package com.brendigo.skypeep

import org.junit.Assert.*
import org.junit.Test

class SkyPeepHudNumbersTest {
    @Test fun ordinaryBalancesRemainExactLikeUserArtwork() {
        assertEquals("63",SkyPeepHudNumbers.compact(63))
        assertEquals("4",SkyPeepHudNumbers.compact(4))
        assertEquals("1250",SkyPeepHudNumbers.compact(1250))
        assertEquals("9999",SkyPeepHudNumbers.compact(9999))
    }
    @Test fun largeBalancesStayInsideTheOriginalWallet() {
        assertEquals("10K",SkyPeepHudNumbers.compact(10000))
        assertEquals("12.5K",SkyPeepHudNumbers.compact(12500))
        assertEquals("999.9K",SkyPeepHudNumbers.compact(999999))
        assertEquals("1M",SkyPeepHudNumbers.compact(1000000))
        assertEquals("2.3M",SkyPeepHudNumbers.compact(2345678))
    }
    @Test fun malformedNegativeBalanceDoesNotDisplayMinus() {
        assertEquals("0",SkyPeepHudNumbers.compact(-1))
    }
}
