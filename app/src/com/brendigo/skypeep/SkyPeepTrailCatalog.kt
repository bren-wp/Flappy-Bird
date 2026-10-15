package com.brendigo.skypeep

/** In-game cosmetics only. No real-money purchases or external payment API. */
internal enum class TrailCurrency { COINS, GEMS }

internal data class FlightTrailOffer(
    val id:Int,val name:String,val price:Int,val currency:TrailCurrency
)
internal data class TrailPurchase(
    val success:Boolean,val alreadyOwned:Boolean,val coins:Int,val gems:Int
)
internal object SkyPeepTrailCatalog {
    val all=listOf(
        FlightTrailOffer(0,"Classic",0,TrailCurrency.COINS),
        FlightTrailOffer(1,"Cyan Dash",200,TrailCurrency.COINS),
        FlightTrailOffer(2,"Golden Spark",450,TrailCurrency.COINS),
        FlightTrailOffer(3,"Moonlight",12,TrailCurrency.GEMS)
    )
    fun purchase(id:Int,owned:Boolean,coins:Int,gems:Int):TrailPurchase {
        val offer=all.getOrNull(id) ?: return TrailPurchase(false,false,coins,gems)
        if(owned || offer.price==0) return TrailPurchase(true,true,coins,gems)
        if(coins<0 || gems<0) return TrailPurchase(false,false,coins,gems)
        return when(offer.currency) {
            TrailCurrency.COINS -> if(coins>=offer.price)
                TrailPurchase(true,false,coins-offer.price,gems)
                else TrailPurchase(false,false,coins,gems)
            TrailCurrency.GEMS -> if(gems>=offer.price)
                TrailPurchase(true,false,coins,gems-offer.price)
                else TrailPurchase(false,false,coins,gems)
        }
    }
}
