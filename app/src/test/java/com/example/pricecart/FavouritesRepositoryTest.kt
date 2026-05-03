package com.example.pricecart

import com.example.pricecart.data.AuthRepository
import com.example.pricecart.data.FavouritesRepository
import com.example.pricecart.data.model.AuthUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavouritesRepositoryTest {
    @Test
    fun saveFavouriteItem_persistsAcrossRepositoryInstances() {
        val authRepository = AuthRepository(
            authClient = FakeAuthClient(AuthUser(uid = "uid-favourites", email = "favourites@example.com")),
            userProfileStore = FakeUserProfileStore(),
        )
        val favouritesStore = FakeFavouritesStore()

        val firstRepository = FavouritesRepository(favouritesStore, authRepository)
        firstRepository.saveFavouriteItem(
            productName = "Rice",
            onSuccess = { },
            onError = { throw it },
        )

        val secondRepository = FavouritesRepository(favouritesStore, authRepository)
        var favouriteItems = emptyList<com.example.pricecart.data.model.FavouriteItem>()
        secondRepository.fetchUserFavourites(
            onSuccess = { favouriteItems = it },
            onError = { throw it },
        )

        assertEquals(1, favouriteItems.size)
        assertEquals("Rice", favouriteItems.first().productName)
    }

    @Test
    fun removeFavouriteItem_updatesSavedState() {
        val authRepository = AuthRepository(
            authClient = FakeAuthClient(AuthUser(uid = "uid-toggle", email = "toggle@example.com")),
            userProfileStore = FakeUserProfileStore(),
        )
        val favouritesRepository = FavouritesRepository(FakeFavouritesStore(), authRepository)

        favouritesRepository.saveFavouriteItem(
            productName = "Milk",
            onSuccess = { },
            onError = { throw it },
        )

        var isSaved = false
        favouritesRepository.isFavouriteItemSaved(
            productName = "Milk",
            onSuccess = { isSaved = it },
            onError = { throw it },
        )
        assertTrue(isSaved)

        favouritesRepository.removeFavouriteItem(
            productName = "Milk",
            onSuccess = { },
            onError = { throw it },
        )
        favouritesRepository.isFavouriteItemSaved(
            productName = "Milk",
            onSuccess = { isSaved = it },
            onError = { throw it },
        )
        assertFalse(isSaved)
    }
}
