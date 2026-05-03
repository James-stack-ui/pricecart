package com.example.pricecart.data

import com.example.pricecart.data.model.ProductOfferDetails
import com.example.pricecart.data.model.StoreDetails
import com.example.pricecart.data.model.StorePrice
import com.example.pricecart.data.model.StoreRecord
import com.example.pricecart.data.model.UserCoordinates
import com.example.pricecart.ranking.StoreRankingCalculator
import com.example.pricecart.search.ProductSearchDataSource
import com.example.pricecart.search.SearchTextFormatter
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class PriceRepository(
    private val firestore: FirebaseFirestore,
) : ProductSearchDataSource {
    fun fetchCatalogProductNames(
        limit: Int,
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        firestorePriceCollection()
            .orderBy(FIELD_PRODUCT_NAME_LOWERCASE)
            .limit((limit * QUICK_PICK_FETCH_MULTIPLIER).toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->
                onSuccess(
                    querySnapshot.documents
                        .mapNotNull { document -> document.getString(FIELD_PRODUCT_NAME)?.trim() }
                        .filter { productName -> productName.isNotBlank() }
                        .distinct()
                        .take(limit),
                )
            }
            .addOnFailureListener(onError)
    }

    override fun fetchProductsByName(
        normalizedQuery: String,
        onSuccess: (List<StorePrice>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        firestorePriceCollection()
            .orderBy(FIELD_PRODUCT_NAME_LOWERCASE)
            .startAt(normalizedQuery)
            .endAt(normalizedQuery + "\uf8ff")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }
                joinStorePricesWithStores(
                    priceDocuments = querySnapshot.documents,
                    onSuccess = { joinedStorePrices ->
                        onSuccess(
                            joinedStorePrices.sortedWith(
                                compareBy<StorePrice> { it.productNameLowercase }.thenBy { it.storeName },
                            ),
                        )
                    },
                    onError = onError,
                )
            }
            .addOnFailureListener(onError)
    }

    fun getProductOfferDetails(
        productName: String,
        storeName: String,
        userCoordinates: UserCoordinates? = null,
    ): ProductOfferDetails? {
        throw UnsupportedOperationException("Synchronous catalog access is not supported in Firestore-only mode.")
    }

    fun getProductOfferDetails(
        productName: String,
        storeName: String,
        userCoordinates: UserCoordinates? = null,
        onSuccess: (ProductOfferDetails?) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val normalizedProductName = SearchTextFormatter.normalizeQuery(productName)
        firestorePriceCollection()
            .whereEqualTo(FIELD_PRODUCT_NAME_LOWERCASE, normalizedProductName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }
                joinStorePriceContext(
                    priceDocuments = querySnapshot.documents,
                    onSuccess = { joinResult ->
                        val joinedContext = joinResult.joinedContext
                        val matchingStorePrices = joinedContext.map { (storePrice, _) -> storePrice }
                            .sortedWith(compareBy<StorePrice> { it.productNameLowercase }.thenBy { it.storeName })
                        val storesByStoreName = joinedContext.associate { (storePrice, storeRecord) ->
                            storePrice.storeName to storeRecord
                        }
                        onSuccess(
                            buildProductOfferDetails(
                                storePrices = matchingStorePrices,
                                storeRecordResolver = { lookupStoreName -> storesByStoreName[lookupStoreName] },
                                productName = productName,
                                storeName = storeName,
                                userCoordinates = userCoordinates,
                            ),
                        )
                    },
                    onError = onError,
                )
            }
            .addOnFailureListener(onError)
    }

    private fun buildProductOfferDetails(
        storePrices: List<StorePrice>,
        storeRecordResolver: (String) -> StoreRecord?,
        productName: String,
        storeName: String,
        userCoordinates: UserCoordinates?,
    ): ProductOfferDetails? {
        val normalizedProductName = SearchTextFormatter.normalizeQuery(productName)
        val matchingStorePrices = storePrices
            .filter { storePrice ->
                storePrice.productNameLowercase == normalizedProductName
            }
            .sortedWith(compareBy<StorePrice> { it.productNameLowercase }.thenBy { it.storeName })

        val selectedStorePrice = matchingStorePrices.firstOrNull { storePrice ->
            storePrice.storeName == storeName
        } ?: return null
        val selectedStoreRecord = storeRecordResolver(storeName) ?: return null
        val rankedOffers = StoreRankingCalculator.rankStoreOffers(
            storePrices = matchingStorePrices,
            userCoordinates = userCoordinates,
        )
        val selectedRank = rankedOffers.indexOfFirst { offer -> offer.storeName == storeName } + 1
        val selectedOffer = rankedOffers.firstOrNull { offer -> offer.storeName == storeName } ?: return null
        val bestPrice = matchingStorePrices.minOf { storePrice -> storePrice.price }

        return ProductOfferDetails(
            productName = selectedStorePrice.productName,
            storeName = selectedStorePrice.storeName,
            price = selectedStorePrice.price,
            currency = selectedStorePrice.currency,
            distanceKilometers = selectedOffer.distanceKilometers,
            updatedAt = selectedStorePrice.updatedAt,
            storeDetails = selectedStoreRecord.storeDetails,
            storeLatitude = selectedStoreRecord.latitude,
            storeLongitude = selectedStoreRecord.longitude,
            rank = selectedRank,
            totalOfferCount = rankedOffers.size,
            bestPrice = bestPrice,
            savingsComparedToBest = (selectedStorePrice.price - bestPrice).coerceAtLeast(0.0),
        )
    }

    private fun joinStorePricesWithStores(
        priceDocuments: List<DocumentSnapshot>,
        onSuccess: (List<StorePrice>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        joinStorePriceContext(
            priceDocuments = priceDocuments,
            onSuccess = { joinResult ->
                onSuccess(joinResult.joinedContext.map { (storePrice, _) -> storePrice })
            },
            onError = onError,
        )
    }

    private fun joinStorePriceContext(
        priceDocuments: List<DocumentSnapshot>,
        onSuccess: (JoinStorePriceContextResult) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val storeIds = priceDocuments.mapNotNull { document -> document.getString(FIELD_STORE_ID) }
            .distinct()
        if (storeIds.isEmpty()) {
            onError(IncompleteCatalogDataException("Store price records are missing store references."))
            return
        }

        val storeTasks = storeIds.map { storeId ->
            firestoreStoresCollection().document(storeId).get()
        }
        Tasks.whenAllSuccess<DocumentSnapshot>(storeTasks)
            .addOnSuccessListener { storeDocuments ->
                val storesById = storeDocuments.mapNotNull(::documentToStoreRecord).associateBy { it.storeId }
                val missingStoreIds = storeIds.filterNot(storesById::containsKey)
                val malformedStoreRecordCount = storeDocuments.size - storesById.size
                var malformedPriceRecordCount = 0
                val joinedResults = priceDocuments.mapNotNull { priceDocument ->
                    val storeId = priceDocument.getString(FIELD_STORE_ID) ?: return@mapNotNull null
                    val storeRecord = storesById[storeId] ?: return@mapNotNull null
                    val storePrice = documentToStorePrice(
                        document = priceDocument,
                        storeRecord = storeRecord,
                    ) ?: run {
                        malformedPriceRecordCount += 1
                        return@mapNotNull null
                    }
                    storePrice to storeRecord
                }
                val joinResult = JoinStorePriceContextResult(
                    joinedContext = joinedResults,
                    missingStoreIds = missingStoreIds,
                    malformedStoreRecordCount = malformedStoreRecordCount,
                    malformedPriceRecordCount = malformedPriceRecordCount,
                )
                validateJoinResult(
                    joinResult = joinResult,
                    expectedPriceDocumentCount = priceDocuments.size,
                    onValid = { onSuccess(joinResult) },
                    onInvalid = onError,
                )
            }
            .addOnFailureListener { exception ->
                onError(exception as? Exception ?: IllegalStateException("Failed to load store records.", exception))
            }
    }

    private fun validateJoinResult(
        joinResult: JoinStorePriceContextResult,
        expectedPriceDocumentCount: Int,
        onValid: () -> Unit,
        onInvalid: (Exception) -> Unit,
    ) {
        val droppedPriceRecordCount = expectedPriceDocumentCount - joinResult.joinedContext.size -
            joinResult.malformedPriceRecordCount
        when {
            joinResult.missingStoreIds.isNotEmpty() -> {
                onInvalid(
                    IncompleteCatalogDataException(
                        "Store catalog is incomplete. Missing stores for ids: ${joinResult.missingStoreIds.joinToString()}",
                    ),
                )
            }

            joinResult.malformedStoreRecordCount > 0 -> {
                onInvalid(
                    IncompleteCatalogDataException(
                        "Store catalog is incomplete. Some store records are missing required fields.",
                    ),
                )
            }

            joinResult.malformedPriceRecordCount > 0 || droppedPriceRecordCount > 0 -> {
                onInvalid(
                    IncompleteCatalogDataException(
                        "Store catalog is incomplete. Some price records are missing required fields.",
                    ),
                )
            }

            joinResult.joinedContext.isEmpty() -> {
                onInvalid(
                    IncompleteCatalogDataException(
                        "Store catalog is incomplete. No usable store prices were found for that product.",
                    ),
                )
            }

            else -> onValid()
        }
    }

    private fun documentToStorePrice(
        document: DocumentSnapshot,
        storeRecord: StoreRecord,
    ): StorePrice? {
        val productName = document.getString(FIELD_PRODUCT_NAME) ?: return null
        val productNameLowercase = document.getString(FIELD_PRODUCT_NAME_LOWERCASE)
            ?: SearchTextFormatter.normalizeQuery(productName)
        val price = document.getDouble(FIELD_PRICE) ?: return null
        val latitude = storeRecord.latitude ?: return null
        val longitude = storeRecord.longitude ?: return null

        return StorePrice(
            storeId = storeRecord.storeId,
            productName = productName,
            productNameLowercase = productNameLowercase,
            storeName = storeRecord.storeDetails.storeName,
            storeLatitude = latitude,
            storeLongitude = longitude,
            price = price,
            currency = document.getString(FIELD_CURRENCY) ?: DEFAULT_CURRENCY,
            updatedAt = document.getTimestamp(FIELD_UPDATED_AT)?.toDate()?.time
                ?: document.getLong(FIELD_UPDATED_AT),
        )
    }

    private fun documentToStoreRecord(document: DocumentSnapshot): StoreRecord? {
        val storeId = document.id
        return documentDataToStoreRecord(storeId = storeId, data = document.data.orEmpty())
    }

    private fun firestorePriceCollection() = firestore.collection(STORE_PRICES_COLLECTION)

    private fun firestoreStoresCollection() = firestore.collection(STORES_COLLECTION)

    companion object {
        private const val STORE_PRICES_COLLECTION = "store_prices"
        private const val STORES_COLLECTION = "stores"
        private const val FIELD_STORE_ID = "storeId"
        private const val FIELD_PRODUCT_NAME = "productName"
        private const val FIELD_PRODUCT_NAME_LOWERCASE = "productNameLowercase"
        private const val FIELD_STORE_NAME = "storeName"
        private const val FIELD_PRICE = "price"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_NEIGHBORHOOD = "neighborhood"
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_HOURS = "hours"
        private const val FIELD_PHONE_NUMBER = "phoneNumber"
        private const val FIELD_CONTACT = "contact"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_NOTE = "note"
        private const val FIELD_LATITUDE = "latitude"
        private const val FIELD_LONGITUDE = "longitude"
        private const val FIELD_COORDINATES = "coordinates"
        private const val DEFAULT_CURRENCY = "JMD"
        private const val QUICK_PICK_FETCH_MULTIPLIER = 4

        internal fun documentDataToStoreRecord(
            storeId: String,
            data: Map<String, Any?>,
        ): StoreRecord? {
            val storeName = data.trimmedString(FIELD_STORE_NAME) ?: return null
            val coordinates = data.resolveCoordinates()
            return StoreRecord(
                storeId = storeId,
                storeDetails = StoreDetails(
                    storeName = storeName,
                    neighborhood = data.trimmedString(FIELD_NEIGHBORHOOD).orEmpty(),
                    address = data.trimmedString(FIELD_ADDRESS).orEmpty(),
                    hours = data.trimmedString(FIELD_HOURS).orEmpty(),
                    phoneNumber = data.firstTrimmedString(FIELD_PHONE_NUMBER, FIELD_CONTACT, FIELD_PHONE).orEmpty(),
                    note = data.trimmedString(FIELD_NOTE).orEmpty(),
                ),
                latitude = coordinates?.latitude,
                longitude = coordinates?.longitude,
            )
        }

        private fun Map<String, Any?>.firstTrimmedString(vararg fieldNames: String): String? {
            return fieldNames.firstNotNullOfOrNull { fieldName -> trimmedString(fieldName) }
        }

        private fun Map<String, Any?>.trimmedString(fieldName: String): String? {
            return (this[fieldName] as? String)
                ?.trim()
                ?.takeIf { value -> value.isNotBlank() }
        }

        private fun Map<String, Any?>.resolveCoordinates(): StoreCoordinates? {
            val latitude = numberValue(FIELD_LATITUDE)
            val longitude = numberValue(FIELD_LONGITUDE)
            if (latitude != null && longitude != null) {
                return StoreCoordinates(latitude = latitude, longitude = longitude)
            }

            return when (val coordinates = this[FIELD_COORDINATES]) {
                is GeoPoint -> StoreCoordinates(
                    latitude = coordinates.latitude,
                    longitude = coordinates.longitude,
                )

                is Map<*, *> -> {
                    val coordinateMap = coordinates.entries.associate { (key, value) -> key.toString() to value }
                    val nestedLatitude = coordinateMap.numberValue(FIELD_LATITUDE)
                    val nestedLongitude = coordinateMap.numberValue(FIELD_LONGITUDE)
                    if (nestedLatitude != null && nestedLongitude != null) {
                        StoreCoordinates(latitude = nestedLatitude, longitude = nestedLongitude)
                    } else {
                        null
                    }
                }

                else -> null
            }
        }

        private fun Map<String, Any?>.numberValue(fieldName: String): Double? {
            return when (val value = this[fieldName]) {
                is Number -> value.toDouble()
                else -> null
            }
        }

        private data class StoreCoordinates(
            val latitude: Double,
            val longitude: Double,
        )

        internal fun evaluateJoinResultForTest(
            joinedResultCount: Int,
            expectedPriceDocumentCount: Int,
            missingStoreIds: List<String> = emptyList(),
            malformedStoreRecordCount: Int = 0,
            malformedPriceRecordCount: Int = 0,
        ): Exception? {
            val droppedPriceRecordCount = expectedPriceDocumentCount - joinedResultCount - malformedPriceRecordCount
            return when {
                missingStoreIds.isNotEmpty() -> {
                    IncompleteCatalogDataException(
                        "Store catalog is incomplete. Missing stores for ids: ${missingStoreIds.joinToString()}",
                    )
                }

                malformedStoreRecordCount > 0 -> {
                    IncompleteCatalogDataException(
                        "Store catalog is incomplete. Some store records are missing required fields.",
                    )
                }

                malformedPriceRecordCount > 0 || droppedPriceRecordCount > 0 -> {
                    IncompleteCatalogDataException(
                        "Store catalog is incomplete. Some price records are missing required fields.",
                    )
                }

                joinedResultCount == 0 -> {
                    IncompleteCatalogDataException(
                        "Store catalog is incomplete. No usable store prices were found for that product.",
                    )
                }

                else -> null
            }
        }
    }

    private data class JoinStorePriceContextResult(
        val joinedContext: List<Pair<StorePrice, StoreRecord>>,
        val missingStoreIds: List<String>,
        val malformedStoreRecordCount: Int,
        val malformedPriceRecordCount: Int,
    )
}

class IncompleteCatalogDataException(
    message: String,
) : IllegalStateException(message)
