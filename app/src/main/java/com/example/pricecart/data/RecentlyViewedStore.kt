package com.example.pricecart.data

import com.example.pricecart.data.model.RecentlyViewedOffer
import com.example.pricecart.data.model.StoreOfferResult
import com.example.pricecart.data.model.UserCoordinates

interface RecentlyViewedStore {
    fun fetchRecentlyViewed(
        userId: String,
        limit: Int,
        onSuccess: (List<RecentlyViewedOffer>) -> Unit,
        onError: (Exception) -> Unit,
    )

    fun recordViewedOffer(
        userId: String,
        storeOfferResult: StoreOfferResult,
        userCoordinates: UserCoordinates?,
        limit: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    )
}
