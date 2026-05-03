package com.example.pricecart

import com.example.pricecart.data.AuthClient
import com.example.pricecart.data.FavouritesStore
import com.example.pricecart.data.RecentSearchStore
import com.example.pricecart.data.RecentlyViewedStore
import com.example.pricecart.data.UserProfileStore
import com.example.pricecart.data.model.AuthUser
import com.example.pricecart.data.model.FavouriteItem
import com.example.pricecart.data.model.RecentSearchItem
import com.example.pricecart.data.model.RecentlyViewedOffer
import com.example.pricecart.data.model.StoreOfferResult
import com.example.pricecart.data.model.UserCoordinates
import com.example.pricecart.favourites.FavouriteItemHelper
import com.example.pricecart.search.SearchTextFormatter

class AndroidTestFakeAuthClient(
    initialUser: AuthUser? = null,
) : AuthClient {
    var currentAuthUser: AuthUser? = initialUser

    override fun currentUser(): AuthUser? = currentAuthUser

    override fun signIn(
        emailAddress: String,
        password: String,
        onSuccess: (AuthUser) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val authUser = AuthUser(uid = "uid-${emailAddress.lowercase()}", email = emailAddress)
        currentAuthUser = authUser
        onSuccess(authUser)
    }

    override fun register(
        emailAddress: String,
        password: String,
        onSuccess: (AuthUser) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val authUser = AuthUser(uid = "uid-${emailAddress.lowercase()}", email = emailAddress)
        currentAuthUser = authUser
        onSuccess(authUser)
    }

    override fun deleteCurrentUser(
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        currentAuthUser = null
        onComplete()
    }

    override fun signOut() {
        currentAuthUser = null
    }
}

class AndroidTestFakeUserProfileStore : UserProfileStore {
    override fun createUserProfile(
        userId: String,
        emailAddress: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        onSuccess()
    }
}

class AndroidTestFakeFavouritesStore : FavouritesStore {
    private val itemsByUser = mutableMapOf<String, MutableMap<String, FavouriteItem>>()
    private var createdAtCounter = 0L

    override fun fetchUserFavourites(
        userId: String,
        onSuccess: (List<FavouriteItem>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        onSuccess(itemsByUser[userId].orEmpty().values.sortedByDescending { it.createdAt ?: 0L })
    }

    override fun isFavouriteItemSaved(
        userId: String,
        productName: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        onSuccess(itemsByUser[userId]?.containsKey(FavouriteItemHelper.createFavouriteKey(productName)) == true)
    }

    override fun saveFavouriteItem(
        userId: String,
        productName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val trimmedName = productName.trim()
        itemsByUser.getOrPut(userId) { linkedMapOf() }[FavouriteItemHelper.createFavouriteKey(trimmedName)] =
            FavouriteItem(
                productName = trimmedName,
                productNameLowercase = FavouriteItemHelper.normalizeProductName(trimmedName),
                createdAt = ++createdAtCounter,
            )
        onSuccess()
    }

    override fun removeFavouriteItem(
        userId: String,
        productName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        itemsByUser[userId]?.remove(FavouriteItemHelper.createFavouriteKey(productName))
        onSuccess()
    }
}

class AndroidTestFakeRecentSearchStore : RecentSearchStore {
    private val itemsByUser = mutableMapOf<String, MutableMap<String, RecentSearchItem>>()
    private var searchedAtCounter = 0L

    override fun fetchRecentSearches(
        userId: String,
        limit: Int,
        onSuccess: (List<RecentSearchItem>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        onSuccess(itemsByUser[userId].orEmpty().values.sortedByDescending { it.searchedAt }.take(limit))
    }

    override fun recordSearch(
        userId: String,
        queryDisplayName: String,
        limit: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val trimmedQuery = queryDisplayName.trim()
        val userItems = itemsByUser.getOrPut(userId) { linkedMapOf() }
        userItems[FavouriteItemHelper.createFavouriteKey(trimmedQuery)] = RecentSearchItem(
            queryDisplayName = trimmedQuery,
            queryNormalized = SearchTextFormatter.normalizeQuery(trimmedQuery),
            searchedAt = ++searchedAtCounter,
        )
        userItems.entries.sortedByDescending { it.value.searchedAt }.drop(limit).map { it.key }.forEach(userItems::remove)
        onSuccess()
    }
}

class AndroidTestFakeRecentlyViewedStore : RecentlyViewedStore {
    private val itemsByUser = mutableMapOf<String, MutableMap<String, RecentlyViewedOffer>>()
    private var viewedAtCounter = 0L

    override fun fetchRecentlyViewed(
        userId: String,
        limit: Int,
        onSuccess: (List<RecentlyViewedOffer>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        onSuccess(itemsByUser[userId].orEmpty().values.sortedByDescending { it.viewedAt }.take(limit))
    }

    override fun recordViewedOffer(
        userId: String,
        storeOfferResult: StoreOfferResult,
        userCoordinates: UserCoordinates?,
        limit: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val userItems = itemsByUser.getOrPut(userId) { linkedMapOf() }
        val offerKey = "${FavouriteItemHelper.createFavouriteKey(storeOfferResult.productName)}_" +
            FavouriteItemHelper.createFavouriteKey(storeOfferResult.storeName)
        userItems[offerKey] = RecentlyViewedOffer(
            productName = storeOfferResult.productName,
            storeName = storeOfferResult.storeName,
            price = storeOfferResult.price,
            currency = storeOfferResult.currency,
            viewedAt = ++viewedAtCounter,
            userLatitude = userCoordinates?.latitude,
            userLongitude = userCoordinates?.longitude,
        )
        userItems.entries.sortedByDescending { it.value.viewedAt }.drop(limit).map { it.key }.forEach(userItems::remove)
        onSuccess()
    }
}
