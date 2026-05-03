package com.example.pricecart.data.model

data class RecentlyViewedOffer(
    val productName: String,
    val storeName: String,
    val price: Double,
    val currency: String,
    val viewedAt: Long,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
) {
    fun userCoordinates(): UserCoordinates? {
        return if (userLatitude != null && userLongitude != null) {
            UserCoordinates(
                latitude = userLatitude,
                longitude = userLongitude,
            )
        } else {
            null
        }
    }
}
