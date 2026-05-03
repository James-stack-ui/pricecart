package com.example.pricecart

import com.example.pricecart.data.AuthRepository
import com.example.pricecart.data.RecentlyViewedRepository
import com.example.pricecart.data.model.AuthUser
import com.example.pricecart.data.model.StoreOfferResult
import com.example.pricecart.data.model.UserCoordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecentlyViewedRepositoryTest {
    @Test
    fun recordViewedOffer_dedupesAndKeepsMostRecentDetails() {
        val authRepository = AuthRepository(
            authClient = FakeAuthClient(AuthUser(uid = "uid-viewed", email = "viewed@example.com")),
            userProfileStore = FakeUserProfileStore(),
        )
        val recentlyViewedRepository = RecentlyViewedRepository(
            recentlyViewedStore = FakeRecentlyViewedStore(),
            authRepository = authRepository,
        )

        recentlyViewedRepository.recordViewedOffer(
            storeOfferResult = storeOffer(productName = "Rice", storeName = "Store A", price = 400.0),
            userCoordinates = UserCoordinates(latitude = 18.0, longitude = -76.8),
        )
        recentlyViewedRepository.recordViewedOffer(
            storeOfferResult = storeOffer(productName = "Milk", storeName = "Store B", price = 280.0),
            userCoordinates = null,
        )
        recentlyViewedRepository.recordViewedOffer(
            storeOfferResult = storeOffer(productName = "Rice", storeName = "Store A", price = 395.0),
            userCoordinates = UserCoordinates(latitude = 18.1, longitude = -76.7),
        )

        var recentlyViewedOffers = emptyList<com.example.pricecart.data.model.RecentlyViewedOffer>()
        recentlyViewedRepository.fetchRecentlyViewed(
            onSuccess = { recentlyViewedOffers = it },
            onError = { throw it },
        )

        assertEquals(2, recentlyViewedOffers.size)
        assertEquals("Rice", recentlyViewedOffers.first().productName)
        assertEquals(395.0, recentlyViewedOffers.first().price, 0.0)
        assertEquals(18.1, recentlyViewedOffers.first().userLatitude ?: -1.0, 0.0)
    }

    @Test
    fun recordViewedOffer_limitsStoredItemsToConfiguredMaximum() {
        val authRepository = AuthRepository(
            authClient = FakeAuthClient(AuthUser(uid = "uid-viewed-trim", email = "viewed-trim@example.com")),
            userProfileStore = FakeUserProfileStore(),
        )
        val recentlyViewedRepository = RecentlyViewedRepository(
            recentlyViewedStore = FakeRecentlyViewedStore(),
            authRepository = authRepository,
        )

        repeat(9) { index ->
            recentlyViewedRepository.recordViewedOffer(
                storeOfferResult = storeOffer(
                    productName = "Item $index",
                    storeName = "Store $index",
                    price = index.toDouble(),
                ),
                userCoordinates = null,
            )
        }

        var recentlyViewedOffers = emptyList<com.example.pricecart.data.model.RecentlyViewedOffer>()
        recentlyViewedRepository.fetchRecentlyViewed(
            onSuccess = { recentlyViewedOffers = it },
            onError = { throw it },
        )

        assertEquals(8, recentlyViewedOffers.size)
        assertFalse(recentlyViewedOffers.any { recentlyViewedOffer -> recentlyViewedOffer.productName == "Item 0" })
    }

    private fun storeOffer(
        productName: String,
        storeName: String,
        price: Double,
    ): StoreOfferResult {
        return StoreOfferResult(
            productName = productName,
            storeName = storeName,
            price = price,
            currency = "JMD",
            distanceKilometers = null,
            score = price,
        )
    }
}
