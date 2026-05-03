package com.example.pricecart.data

import com.example.pricecart.data.model.FavouriteItem

interface FavouritesStore {
    fun fetchUserFavourites(
        userId: String,
        onSuccess: (List<FavouriteItem>) -> Unit,
        onError: (Exception) -> Unit,
    )

    fun isFavouriteItemSaved(
        userId: String,
        productName: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit,
    )

    fun saveFavouriteItem(
        userId: String,
        productName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    )

    fun removeFavouriteItem(
        userId: String,
        productName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    )
}
