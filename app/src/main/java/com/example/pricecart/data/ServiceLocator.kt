package com.example.pricecart.data

object ServiceLocator {
    var authClientFactory: () -> AuthClient = { FirebaseAuthClient() }
    var userProfileStoreFactory: () -> UserProfileStore = { FirestoreUserProfileStore() }
    var favouritesStoreFactory: () -> FavouritesStore = { FirestoreFavouritesStore() }
    var recentSearchStoreFactory: () -> RecentSearchStore = { FirestoreRecentSearchStore() }
    var recentlyViewedStoreFactory: () -> RecentlyViewedStore = { FirestoreRecentlyViewedStore() }

    fun reset() {
        authClientFactory = { FirebaseAuthClient() }
        userProfileStoreFactory = { FirestoreUserProfileStore() }
        favouritesStoreFactory = { FirestoreFavouritesStore() }
        recentSearchStoreFactory = { FirestoreRecentSearchStore() }
        recentlyViewedStoreFactory = { FirestoreRecentlyViewedStore() }
    }
}
