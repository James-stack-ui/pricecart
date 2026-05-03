package com.example.pricecart.data.model

data class StorePrice(
    var storeId: String = "",
    var productName: String = "",
    var productNameLowercase: String = "",
    var storeName: String = "",
    var storeLatitude: Double = 0.0,
    var storeLongitude: Double = 0.0,
    var price: Double = 0.0,
    var currency: String = "JMD",
    var updatedAt: Long? = null,
)
