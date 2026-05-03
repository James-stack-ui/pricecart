package com.example.pricecart.data

import com.example.pricecart.data.model.FavouriteItem

class FavouritesRepository(
    private val favouritesStore: FavouritesStore = ServiceLocator.favouritesStoreFactory(),
    private val authRepository: AuthRepository = AuthRepository(),
) {
    fun fetchUserFavourites(
        onSuccess: (List<FavouriteItem>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val userId = currentUserId()
        if (userId == null) {
            onSuccess(emptyList())
            return
        }

        favouritesStore.fetchUserFavourites(userId, onSuccess, onError)
    }

    fun isFavouriteItemSaved(
        productName: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val userId = currentUserId()
        if (userId == null) {
            onSuccess(false)
            return
        }

        favouritesStore.isFavouriteItemSaved(userId, productName, onSuccess, onError)
    }

    fun saveFavouriteItem(
        productName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val userId = currentUserId()
        if (userId == null) {
            onError(IllegalStateException("A logged-in user is required to save favourites."))
            return
        }

        favouritesStore.saveFavouriteItem(userId, productName, onSuccess, onError)
    }

    fun removeFavouriteItem(
        productName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val userId = currentUserId()
        if (userId == null) {
            onSuccess()
            return
        }

        favouritesStore.removeFavouriteItem(userId, productName, onSuccess, onError)
    }

    private fun currentUserId(): String? {
        return authRepository.currentUserId()
            ?.takeIf { userId -> userId.isNotBlank() }
    }
}
