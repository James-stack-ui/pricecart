package com.example.pricecart.data.model

data class StoreOfferResult(
    val productName: String,
    val storeName: String,
    val price: Double,
    val currency: String,
    val distanceKilometers: Double?,
    val score: Double,
)
