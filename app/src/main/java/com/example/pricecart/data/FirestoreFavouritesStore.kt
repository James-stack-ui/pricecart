package com.example.pricecart.data

import com.example.pricecart.data.model.FavouriteItem
import com.example.pricecart.favourites.FavouriteItemHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirestoreFavouritesStore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : FavouritesStore {
    override fun fetchUserFavourites(
        userId: String,
        onSuccess: (List<FavouriteItem>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        userCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                onSuccess(
                    querySnapshot.documents.mapNotNull { document ->
                        val productName = document.getString("productName") ?: return@mapNotNull null
                        FavouriteItem(
                            productName = productName,
                            productNameLowercase = document.getString("productNameLowercase")
                                ?: FavouriteItemHelper.normalizeProductName(productName),
                            createdAt = document.getTimestamp("createdAt")?.toDate()?.time,
                        )
                    },
                )
            }
            .addOnFailureListener(onError)
    }

    override fun isFavouriteItemSaved(
        userId: String,
        productName: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        userCollection(userId)
            .document(FavouriteItemHelper.createFavouriteKey(productName))
            .get()
            .addOnSuccessListener { snapshot -> onSuccess(snapshot.exists()) }
            .addOnFailureListener(onError)
    }

    override fun saveFavouriteItem(
        userId: String,
        productName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val trimmedName = productName.trim()
        val favouriteKey = FavouriteItemHelper.createFavouriteKey(trimmedName)
        userCollection(userId)
            .document(favouriteKey)
            .set(
                mapOf(
                    "productName" to trimmedName,
                    "productNameLowercase" to FavouriteItemHelper.normalizeProductName(trimmedName),
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    override fun removeFavouriteItem(
        userId: String,
        productName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        userCollection(userId)
            .document(FavouriteItemHelper.createFavouriteKey(productName))
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    private fun userCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(FAVOURITES_COLLECTION)

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FAVOURITES_COLLECTION = "favourites"
    }
}
