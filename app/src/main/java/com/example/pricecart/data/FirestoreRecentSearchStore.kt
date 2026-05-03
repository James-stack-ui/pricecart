package com.example.pricecart.data

import com.example.pricecart.data.model.RecentSearchItem
import com.example.pricecart.favourites.FavouriteItemHelper
import com.example.pricecart.search.SearchTextFormatter
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirestoreRecentSearchStore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : RecentSearchStore {
    override fun fetchRecentSearches(
        userId: String,
        limit: Int,
        onSuccess: (List<RecentSearchItem>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        userCollection(userId)
            .orderBy("searchedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->
                onSuccess(
                    querySnapshot.documents.mapNotNull { document ->
                        val queryDisplayName = document.getString("queryDisplayName") ?: return@mapNotNull null
                        RecentSearchItem(
                            queryDisplayName = queryDisplayName,
                            queryNormalized = document.getString("queryNormalized")
                                ?: SearchTextFormatter.normalizeQuery(queryDisplayName),
                            searchedAt = document.getTimestamp("searchedAt")?.toDate()?.time ?: 0L,
                        )
                    },
                )
            }
            .addOnFailureListener(onError)
    }

    override fun recordSearch(
        userId: String,
        queryDisplayName: String,
        limit: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val trimmedQuery = queryDisplayName.trim()
        val searchKey = FavouriteItemHelper.createFavouriteKey(trimmedQuery)
        userCollection(userId)
            .document(searchKey)
            .set(
                mapOf(
                    "queryDisplayName" to trimmedQuery,
                    "queryNormalized" to SearchTextFormatter.normalizeQuery(trimmedQuery),
                    "searchedAt" to FieldValue.serverTimestamp(),
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
            .orderBy("searchedAt", Query.Direction.DESCENDING)
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

    private fun userCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(RECENT_SEARCHES_COLLECTION)

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val RECENT_SEARCHES_COLLECTION = "recent_searches"
    }
}
