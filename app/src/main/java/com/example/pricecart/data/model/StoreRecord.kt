package com.example.pricecart.data.model

data class StoreRecord(
    val storeId: String,
    val storeDetails: StoreDetails,
    val latitude: Double?,
    val longitude: Double?,
)
