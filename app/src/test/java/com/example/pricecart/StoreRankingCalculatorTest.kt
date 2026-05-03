package com.example.pricecart

import com.example.pricecart.data.model.StorePrice
import com.example.pricecart.data.model.UserCoordinates
import com.example.pricecart.ranking.StoreRankingCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRankingCalculatorTest {
    @Test
    fun rankStoreOffers_prefersCheaperAndCloserStore() {
        val rankedStores = StoreRankingCalculator.rankStoreOffers(
            storePrices = listOf(
                storePrice(storeName = "Budget Mart", price = 8.50, latitude = 18.0100, longitude = -76.7900),
                storePrice(storeName = "Far Town Market", price = 10.00, latitude = 18.0800, longitude = -76.8600),
            ),
            userCoordinates = UserCoordinates(latitude = 18.0000, longitude = -76.8000),
        )

        assertEquals("Budget Mart", rankedStores.first().storeName)
    }

    @Test
    fun rankStoreOffers_canPreferSlightlyPricierNearbyStore() {
        val rankedStores = StoreRankingCalculator.rankStoreOffers(
            storePrices = listOf(
                storePrice(storeName = "Corner Shop", price = 11.50, latitude = 18.0020, longitude = -76.8020),
                storePrice(storeName = "Cheap But Far", price = 10.00, latitude = 18.2200, longitude = -77.0300),
                storePrice(storeName = "Premium Market", price = 16.00, latitude = 18.0100, longitude = -76.8090),
            ),
            userCoordinates = UserCoordinates(latitude = 18.0000, longitude = -76.8000),
        )

        assertEquals("Corner Shop", rankedStores.first().storeName)
    }

    @Test
    fun rankStoreOffers_withoutLocationSortsByPriceOnly() {
        val rankedStores = StoreRankingCalculator.rankStoreOffers(
            storePrices = listOf(
                storePrice(storeName = "Store B", price = 13.50, latitude = 18.0000, longitude = -76.8100),
                storePrice(storeName = "Store A", price = 12.25, latitude = 18.3000, longitude = -77.0000),
            ),
            userCoordinates = null,
        )

        assertEquals("Store A", rankedStores.first().storeName)
        assertTrue(rankedStores.all { it.distanceKilometers == null })
    }

    @Test
    fun calculateStoreScore_usesConfiguredPriceAndDistanceWeights() {
        val score = StoreRankingCalculator.calculateStoreScore(
            normalizedPrice = 0.5,
            normalizedDistance = 0.25,
        )

        assertEquals(0.4, score, 0.0001)
    }

    private fun storePrice(
        storeName: String,
        price: Double,
        latitude: Double,
        longitude: Double,
    ): StorePrice {
        return StorePrice(
            productName = "Rice",
            productNameLowercase = "rice",
            storeName = storeName,
            storeLatitude = latitude,
            storeLongitude = longitude,
            price = price,
            currency = "JMD",
        )
    }
}
