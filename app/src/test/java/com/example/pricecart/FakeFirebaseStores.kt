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

class FakeAuthClient(
    initialUser: AuthUser? = null,
) : AuthClient {
    var currentAuthUser: AuthUser? = initialUser
    var nextSignInError: Exception? = null
    var nextRegisterError: Exception? = null
    var nextDeleteError: Exception? = null
    var deleteCurrentUserCalls: Int = 0
    var signOutCalls: Int = 0

    override fun currentUser(): AuthUser? = currentAuthUser

    override fun signIn(
        emailAddress: String,
        password: String,
        onSuccess: (AuthUser) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val error = nextSignInError
        if (error != null) {
            nextSignInError = null
            onError(error)
            return
        }

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
        val error = nextRegisterError
        if (error != null) {
            nextRegisterError = null
            onError(error)
            return
        }

        val authUser = AuthUser(uid = "uid-${emailAddress.lowercase()}", email = emailAddress)
        currentAuthUser = authUser
        onSuccess(authUser)
    }

    override fun deleteCurrentUser(
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        deleteCurrentUserCalls += 1
        val error = nextDeleteError
        currentAuthUser = null
        if (error != null) {
            nextDeleteError = null
            onError(error)
            return
        }

        onComplete()
    }

    override fun signOut() {
        signOutCalls += 1
        currentAuthUser = null
    }
}

class FakeUserProfileStore : UserProfileStore {
    var nextCreateError: Exception? = null
    val createdProfiles = mutableListOf<Pair<String, String>>()

    override fun createUserProfile(
        userId: String,
        emailAddress: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val error = nextCreateError
        if (error != null) {
            nextCreateError = null
            onError(error)
            return
        }

        createdProfiles += userId to emailAddress
        onSuccess()
    }
}

class FakeFavouritesStore : FavouritesStore {
    private val itemsByUser = mutableMapOf<String, MutableMap<String, FavouriteItem>>()
    private var createdAtCounter = 0L

    override fun fetchUserFavourites(
        userId: String,
        onSuccess: (List<FavouriteItem>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        onSuccess(
            itemsByUser[userId]
                .orEmpty()
                .values
                .sortedByDescending { it.createdAt ?: 0L },
        )
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
        val item = FavouriteItem(
            productName = trimmedName,
            productNameLowercase = FavouriteItemHelper.normalizeProductName(trimmedName),
            createdAt = ++createdAtCounter,
        )
        itemsByUser.getOrPut(userId) { linkedMapOf() }[FavouriteItemHelper.createFavouriteKey(trimmedName)] = item
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

class FakeRecentSearchStore : RecentSearchStore {
    private val itemsByUser = mutableMapOf<String, MutableMap<String, RecentSearchItem>>()
    private var searchedAtCounter = 0L

    override fun fetchRecentSearches(
        userId: String,
        limit: Int,
        onSuccess: (List<RecentSearchItem>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        onSuccess(
            itemsByUser[userId]
                .orEmpty()
                .values
                .sortedByDescending { it.searchedAt }
                .take(limit),
        )
    }

    override fun recordSearch(
        userId: String,
        queryDisplayName: String,
        limit: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val trimmedQuery = queryDisplayName.trim()
        val item = RecentSearchItem(
            queryDisplayName = trimmedQuery,
            queryNormalized = SearchTextFormatter.normalizeQuery(trimmedQuery),
            searchedAt = ++searchedAtCounter,
        )
        val userItems = itemsByUser.getOrPut(userId) { linkedMapOf() }
        userItems[FavouriteItemHelper.createFavouriteKey(trimmedQuery)] = item
        userItems.entries
            .sortedByDescending { it.value.searchedAt }
            .drop(limit)
            .map { it.key }
            .forEach(userItems::remove)
        onSuccess()
    }
}

class FakeRecentlyViewedStore : RecentlyViewedStore {
    private val itemsByUser = mutableMapOf<String, MutableMap<String, RecentlyViewedOffer>>()
    private var viewedAtCounter = 0L

    override fun fetchRecentlyViewed(
        userId: String,
        limit: Int,
        onSuccess: (List<RecentlyViewedOffer>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        onSuccess(
            itemsByUser[userId]
                .orEmpty()
                .values
                .sortedByDescending { it.viewedAt }
                .take(limit),
        )
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
        val offerKey = createOfferKey(storeOfferResult.productName, storeOfferResult.storeName)
        userItems[offerKey] = RecentlyViewedOffer(
            productName = storeOfferResult.productName,
            storeName = storeOfferResult.storeName,
            price = storeOfferResult.price,
            currency = storeOfferResult.currency,
            viewedAt = ++viewedAtCounter,
            userLatitude = userCoordinates?.latitude,
            userLongitude = userCoordinates?.longitude,
        )
        userItems.entries
            .sortedByDescending { it.value.viewedAt }
            .drop(limit)
            .map { it.key }
            .forEach(userItems::remove)
        onSuccess()
    }

    private fun createOfferKey(
        productName: String,
        storeName: String,
    ): String {
        val productKey = FavouriteItemHelper.createFavouriteKey(productName)
        val storeKey = FavouriteItemHelper.createFavouriteKey(storeName)
        return "${productKey}_$storeKey"
    }
}
