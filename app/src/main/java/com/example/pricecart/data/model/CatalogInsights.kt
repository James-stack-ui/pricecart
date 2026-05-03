package com.example.pricecart.data.model

data class CatalogInsights(
    val totalProducts: Int,
    val totalStores: Int,
    val totalPriceChecks: Int,
    val featuredProducts: List<String>,
    val bestBasketStoreName: String,
    val bestBasketTotal: Double,
    val averageSavingsPerItem: Double,
)
