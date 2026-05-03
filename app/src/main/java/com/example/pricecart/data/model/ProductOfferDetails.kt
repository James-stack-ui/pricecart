package com.example.pricecart.data.model

data class ProductOfferDetails(
    val productName: String,
    val storeName: String,
    val price: Double,
    val currency: String,
    val distanceKilometers: Double?,
    val updatedAt: Long?,
    val storeDetails: StoreDetails,
    val storeLatitude: Double? = null,
    val storeLongitude: Double? = null,
    val rank: Int,
    val totalOfferCount: Int,
    val bestPrice: Double,
    val savingsComparedToBest: Double,
)
