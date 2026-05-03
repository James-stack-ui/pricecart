package com.example.pricecart.ranking

import com.example.pricecart.data.model.StoreOfferResult
import com.example.pricecart.data.model.StorePrice
import com.example.pricecart.data.model.UserCoordinates
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object StoreRankingCalculator {
    fun calculateStoreScore(normalizedPrice: Double, normalizedDistance: Double): Double {
        return (normalizedPrice * 0.6) + (normalizedDistance * 0.4)
    }

    fun rankStoreOffers(
        storePrices: List<StorePrice>,
        userCoordinates: UserCoordinates?,
    ): List<StoreOfferResult> {
        if (storePrices.isEmpty()) {
            return emptyList()
        }

        if (userCoordinates == null) {
            return storePrices
                .sortedWith(compareBy<StorePrice> { it.price }.thenBy { it.storeName })
                .map { storePrice ->
                    StoreOfferResult(
                        productName = storePrice.productName,
                        storeName = storePrice.storeName,
                        price = storePrice.price,
                        currency = storePrice.currency,
                        distanceKilometers = null,
                        score = storePrice.price,
                    )
                }
        }

        val distanceValues = storePrices.map { storePrice ->
            distanceBetweenKilometers(
                latitudeOne = userCoordinates.latitude,
                longitudeOne = userCoordinates.longitude,
                latitudeTwo = storePrice.storeLatitude,
                longitudeTwo = storePrice.storeLongitude,
            )
        }
        val minPrice = storePrices.minOf { it.price }
        val maxPrice = storePrices.maxOf { it.price }
        val minDistance = distanceValues.minOrNull() ?: 0.0
        val maxDistance = distanceValues.maxOrNull() ?: 0.0

        return storePrices
            .mapIndexed { index, storePrice ->
                val distance = distanceValues[index]
                val normalizedPrice = normalizeValue(storePrice.price, minPrice, maxPrice)
                val normalizedDistance = normalizeValue(distance, minDistance, maxDistance)
                StoreOfferResult(
                    productName = storePrice.productName,
                    storeName = storePrice.storeName,
                    price = storePrice.price,
                    currency = storePrice.currency,
                    distanceKilometers = distance,
                    score = calculateStoreScore(normalizedPrice, normalizedDistance),
                )
            }
            .sortedWith(
                compareBy<StoreOfferResult> { it.score }
                    .thenBy { it.price }
                    .thenBy { it.storeName },
            )
    }

    private fun normalizeValue(value: Double, minimum: Double, maximum: Double): Double {
        if (maximum <= minimum) {
            return 0.0
        }
        return (value - minimum) / (maximum - minimum)
    }

    private fun distanceBetweenKilometers(
        latitudeOne: Double,
        longitudeOne: Double,
        latitudeTwo: Double,
        longitudeTwo: Double,
    ): Double {
        val earthRadius = 6371.0
        val latitudeDistance = Math.toRadians(latitudeTwo - latitudeOne)
        val longitudeDistance = Math.toRadians(longitudeTwo - longitudeOne)
        val firstLatitude = Math.toRadians(latitudeOne)
        val secondLatitude = Math.toRadians(latitudeTwo)

        val haversineValue = sin(latitudeDistance / 2).pow(2.0) +
            cos(firstLatitude) * cos(secondLatitude) * sin(longitudeDistance / 2).pow(2.0)
        val angularDistance = 2 * atan2(sqrt(haversineValue), sqrt(1 - haversineValue))

        return earthRadius * angularDistance
    }
}
