package com.example.pricecart.search

import com.example.pricecart.data.model.StorePrice

interface ProductSearchDataSource {
    fun fetchProductsByName(
        normalizedQuery: String,
        onSuccess: (List<StorePrice>) -> Unit,
        onError: (Exception) -> Unit,
    )
}

class ProductSearchManager(
    private val productSearchDataSource: ProductSearchDataSource,
) {
    fun searchProductsByName(
        rawQuery: String,
        onBlankQuery: () -> Unit,
        onSuccess: (normalizedQuery: String, storePrices: List<StorePrice>) -> Unit,
        onError: (Exception) -> Unit,
    ): Boolean {
        val normalizedQuery = SearchTextFormatter.normalizeQuery(rawQuery)
        if (normalizedQuery.isBlank()) {
            onBlankQuery()
            return false
        }

        productSearchDataSource.fetchProductsByName(
            normalizedQuery = normalizedQuery,
            onSuccess = { storePrices ->
                onSuccess(normalizedQuery, storePrices)
            },
            onError = onError,
        )
        return true
    }
}
