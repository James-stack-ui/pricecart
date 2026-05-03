package com.example.pricecart

import com.example.pricecart.data.IncompleteCatalogDataException
import com.example.pricecart.data.PriceRepository
import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceRepositoryTest {
    @Test
    fun documentDataToStoreRecord_readsDocumentedStoreFields() {
        val storeRecord = PriceRepository.documentDataToStoreRecord(
            storeId = "store-1",
            data = mapOf(
                "storeName" to "Budget Mart",
                "latitude" to 18.01234,
                "longitude" to -76.81234,
                "neighborhood" to "Downtown",
                "address" to "12 Market Road",
                "hours" to "8 AM - 8 PM",
                "phoneNumber" to "876-555-0101",
                "note" to "Fresh produce near checkout",
            ),
        )

        requireNotNull(storeRecord)
        assertEquals("store-1", storeRecord.storeId)
        assertEquals("Budget Mart", storeRecord.storeDetails.storeName)
        assertEquals(18.01234, storeRecord.latitude ?: 0.0, 0.00001)
        assertEquals(-76.81234, storeRecord.longitude ?: 0.0, 0.00001)
        assertEquals("Downtown", storeRecord.storeDetails.neighborhood)
        assertEquals("12 Market Road", storeRecord.storeDetails.address)
        assertEquals("8 AM - 8 PM", storeRecord.storeDetails.hours)
        assertEquals("876-555-0101", storeRecord.storeDetails.phoneNumber)
        assertEquals("Fresh produce near checkout", storeRecord.storeDetails.note)
    }

    @Test
    fun documentDataToStoreRecord_usesContactAliasWhenPhoneNumberIsMissing() {
        val storeRecord = PriceRepository.documentDataToStoreRecord(
            storeId = "store-1",
            data = mapOf(
                "storeName" to "Budget Mart",
                "latitude" to 18.0,
                "longitude" to -76.8,
                "contact" to "876-555-0202",
            ),
        )

        requireNotNull(storeRecord)
        assertEquals("876-555-0202", storeRecord.storeDetails.phoneNumber)
    }

    @Test
    fun documentDataToStoreRecord_usesPhoneAliasWhenPhoneNumberAndContactAreMissing() {
        val storeRecord = PriceRepository.documentDataToStoreRecord(
            storeId = "store-1",
            data = mapOf(
                "storeName" to "Budget Mart",
                "latitude" to 18.0,
                "longitude" to -76.8,
                "phone" to "876-555-0303",
            ),
        )

        requireNotNull(storeRecord)
        assertEquals("876-555-0303", storeRecord.storeDetails.phoneNumber)
    }

    @Test
    fun documentDataToStoreRecord_prefersPhoneNumberOverContactAlias() {
        val storeRecord = PriceRepository.documentDataToStoreRecord(
            storeId = "store-1",
            data = mapOf(
                "storeName" to "Budget Mart",
                "latitude" to 18.0,
                "longitude" to -76.8,
                "phoneNumber" to "876-555-0101",
                "contact" to "876-555-0202",
            ),
        )

        requireNotNull(storeRecord)
        assertEquals("876-555-0101", storeRecord.storeDetails.phoneNumber)
    }

    @Test
    fun documentDataToStoreRecord_readsGeoPointCoordinatesAlias() {
        val storeRecord = PriceRepository.documentDataToStoreRecord(
            storeId = "store-1",
            data = mapOf(
                "storeName" to "Budget Mart",
                "coordinates" to GeoPoint(18.04567, -76.84567),
            ),
        )

        requireNotNull(storeRecord)
        assertEquals(18.04567, storeRecord.latitude ?: 0.0, 0.00001)
        assertEquals(-76.84567, storeRecord.longitude ?: 0.0, 0.00001)
    }

    @Test
    fun documentDataToStoreRecord_readsMapCoordinatesAlias() {
        val storeRecord = PriceRepository.documentDataToStoreRecord(
            storeId = "store-1",
            data = mapOf(
                "storeName" to "Budget Mart",
                "coordinates" to mapOf(
                    "latitude" to 18.0789,
                    "longitude" to -76.8789,
                ),
            ),
        )

        requireNotNull(storeRecord)
        assertEquals(18.0789, storeRecord.latitude ?: 0.0, 0.00001)
        assertEquals(-76.8789, storeRecord.longitude ?: 0.0, 0.00001)
    }

    @Test
    fun documentDataToStoreRecord_trimsTextAndTreatsBlankOptionalFieldsAsMissing() {
        val storeRecord = PriceRepository.documentDataToStoreRecord(
            storeId = "store-1",
            data = mapOf(
                "storeName" to "  Budget Mart  ",
                "latitude" to 18.0,
                "longitude" to -76.8,
                "address" to "   ",
                "hours" to "\t",
                "note" to "",
                "neighborhood" to "  Downtown  ",
                "phoneNumber" to "  876-555-0101  ",
            ),
        )

        requireNotNull(storeRecord)
        assertEquals("Budget Mart", storeRecord.storeDetails.storeName)
        assertEquals("Downtown", storeRecord.storeDetails.neighborhood)
        assertEquals("", storeRecord.storeDetails.address)
        assertEquals("", storeRecord.storeDetails.hours)
        assertEquals("", storeRecord.storeDetails.note)
        assertEquals("876-555-0101", storeRecord.storeDetails.phoneNumber)
    }

    @Test
    fun evaluateJoinResultForTest_allowsHealthyJoinedResults() {
        val exception = PriceRepository.evaluateJoinResultForTest(
            joinedResultCount = 2,
            expectedPriceDocumentCount = 2,
        )

        assertNull(exception)
    }

    @Test
    fun evaluateJoinResultForTest_treatsEmptyJoinedResultsAsIncompleteCatalog() {
        val exception = PriceRepository.evaluateJoinResultForTest(
            joinedResultCount = 0,
            expectedPriceDocumentCount = 1,
        )

        assertTrue(exception is IncompleteCatalogDataException)
    }

    @Test
    fun evaluateJoinResultForTest_treatsMissingStoreRecordsAsIncompleteCatalog() {
        val exception = PriceRepository.evaluateJoinResultForTest(
            joinedResultCount = 0,
            expectedPriceDocumentCount = 1,
            missingStoreIds = listOf("store-1"),
        )

        assertTrue(exception is IncompleteCatalogDataException)
    }

    @Test
    fun evaluateJoinResultForTest_treatsMalformedPriceRecordsAsIncompleteCatalog() {
        val exception = PriceRepository.evaluateJoinResultForTest(
            joinedResultCount = 0,
            expectedPriceDocumentCount = 1,
            malformedPriceRecordCount = 1,
        )

        assertTrue(exception is IncompleteCatalogDataException)
    }
}
