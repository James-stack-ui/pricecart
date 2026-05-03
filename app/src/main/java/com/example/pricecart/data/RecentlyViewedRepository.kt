package com.example.pricecart.data

import com.example.pricecart.data.model.RecentlyViewedOffer
import com.example.pricecart.data.model.StoreOfferResult
import com.example.pricecart.data.model.UserCoordinates

class RecentlyViewedRepository(
    private val recentlyViewedStore: RecentlyViewedStore = ServiceLocator.recentlyViewedStoreFactory(),
    private val authRepository: AuthRepository = AuthRepository(),
) {
    fun fetchRecentlyViewed(
        onSuccess: (List<RecentlyViewedOffer>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val userId = currentUserId()
        if (userId == null) {
            onSuccess(emptyList())
            return
        }

        recentlyViewedStore.fetchRecentlyViewed(userId, MAX_RECENTLY_VIEWED_ITEMS, onSuccess, onError)
    }

    fun recordViewedOffer(
        storeOfferResult: StoreOfferResult,
        userCoordinates: UserCoordinates?,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {},
    ) {
        val userId = currentUserId()
        if (userId == null) {
            onSuccess()
            return
        }

        recentlyViewedStore.recordViewedOffer(
            userId = userId,
            storeOfferResult = storeOfferResult,
            userCoordinates = userCoordinates,
            limit = MAX_RECENTLY_VIEWED_ITEMS,
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    private fun currentUserId(): String? {
        return authRepository.currentUserId()
            ?.takeIf { userId -> userId.isNotBlank() }
    }

    companion object {
        private const val MAX_RECENTLY_VIEWED_ITEMS = 8
    }
}
