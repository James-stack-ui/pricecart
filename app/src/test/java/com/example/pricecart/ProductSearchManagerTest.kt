package com.example.pricecart

import com.example.pricecart.data.model.StorePrice
import com.example.pricecart.search.ProductSearchDataSource
import com.example.pricecart.search.ProductSearchManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSearchManagerTest {
    @Test
    fun searchProductsByName_normalizesQueryBeforeFetching() {
        val fakeDataSource = FakeProductSearchDataSource()
        val productSearchManager = ProductSearchManager(fakeDataSource)

        productSearchManager.searchProductsByName(
            rawQuery = "  Milk ",
            onBlankQuery = { },
            onSuccess = { normalizedQuery, _ ->
                assertEquals("milk", normalizedQuery)
            },
            onError = { throw it },
        )

        assertEquals("milk", fakeDataSource.lastQuery)
    }

    @Test
    fun searchProductsByName_doesNotFetchWhenQueryIsBlank() {
        val fakeDataSource = FakeProductSearchDataSource()
        val productSearchManager = ProductSearchManager(fakeDataSource)
        var blankQueryCalled = false

        val searchStarted = productSearchManager.searchProductsByName(
            rawQuery = "   ",
            onBlankQuery = { blankQueryCalled = true },
            onSuccess = { _, _ -> },
            onError = { throw it },
        )

        assertFalse(searchStarted)
        assertTrue(blankQueryCalled)
        assertEquals(0, fakeDataSource.fetchCallCount)
    }

    private class FakeProductSearchDataSource : ProductSearchDataSource {
        var fetchCallCount: Int = 0
        var lastQuery: String = ""

        override fun fetchProductsByName(
            normalizedQuery: String,
            onSuccess: (List<StorePrice>) -> Unit,
            onError: (Exception) -> Unit,
        ) {
            fetchCallCount += 1
            lastQuery = normalizedQuery
            onSuccess(emptyList())
        }
    }
}
