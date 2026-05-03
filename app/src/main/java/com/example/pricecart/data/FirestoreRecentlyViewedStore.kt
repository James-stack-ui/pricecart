package com.example.pricecart.data

import com.example.pricecart.data.model.RecentlyViewedOffer
import com.example.pricecart.data.model.StoreOfferResult
import com.example.pricecart.data.model.UserCoordinates
import com.example.pricecart.favourites.FavouriteItemHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirestoreRecentlyViewedStore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : RecentlyViewedStore {
    override fun fetchRecentlyViewed(
        userId: String,
        limit: Int,
        onSuccess: (List<RecentlyViewedOffer>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        userCollection(userId)
            .orderBy("viewedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->
                onSuccess(
                    querySnapshot.documents.mapNotNull { document ->
                        val productName = document.getString("productName") ?: return@mapNotNull null
                        val storeName = document.getString("storeName") ?: return@mapNotNull null
                        val price = document.getDouble("price") ?: return@mapNotNull null
                        RecentlyViewedOffer(
                            productName = productName,
                            storeName = storeName,
                            price = price,
                            currency = document.getString("currency") ?: "JMD",
                            viewedAt = document.getTimestamp("viewedAt")?.toDate()?.time ?: 0L,
                            userLatitude = document.getDouble("userLatitude"),
                            userLongitude = document.getDouble("userLongitude"),
                        )
                    },
                )
            }
            .addOnFailureListener(onError)
    }

    override fun recordViewedOffer(
        userId: String,
        storeOfferResult: StoreOfferResult,
        userCoordinates: UserCoordinates?,
        limit: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        userCollection(userId)
            .document(createOfferKey(storeOfferResult.productName, storeOfferResult.storeName))
            .set(
                mapOf(
                    "productName" to storeOfferResult.productName,
                    "storeName" to storeOfferResult.storeName,
                    "price" to storeOfferResult.price,
                    "currency" to storeOfferResult.currency,
                    "viewedAt" to FieldValue.serverTimestamp(),
                    "userLatitude" to userCoordinates?.latitude,
                    "userLongitude" to userCoordinates?.longitude,
                ),
            )
            .addOnSuccessListener {
                trimExcessEntries(userId, limit, onSuccess, onError)
            }
            .addOnFailureListener(onError)
    }

    private fun trimExcessEntries(
        userId: String,
        limit: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        userCollection(userId)
            .orderBy("viewedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val documentsToDelete = querySnapshot.documents.drop(limit)
                if (documentsToDelete.isEmpty()) {
                    onSuccess()
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()
                documentsToDelete.forEach { document -> batch.delete(document.reference) }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    private fun createOfferKey(
        productName: String,
        storeName: String,
    ): String {
        val productKey = FavouriteItemHelper.createFavouriteKey(productName)
        val storeKey = FavouriteItemHelper.createFavouriteKey(storeName)
        return "${productKey}_$storeKey"
    }

    private fun userCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(RECENTLY_VIEWED_COLLECTION)

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val RECENTLY_VIEWED_COLLECTION = "recently_viewed"
    }
}
