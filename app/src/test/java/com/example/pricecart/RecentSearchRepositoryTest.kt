package com.example.pricecart

import com.example.pricecart.data.AuthRepository
import com.example.pricecart.data.RecentSearchRepository
import com.example.pricecart.data.model.AuthUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecentSearchRepositoryTest {
    @Test
    fun recordSearch_dedupesAndSortsNewestFirst() {
        val authRepository = AuthRepository(
            authClient = FakeAuthClient(AuthUser(uid = "uid-recent", email = "recent@example.com")),
            userProfileStore = FakeUserProfileStore(),
        )
        val recentSearchRepository = RecentSearchRepository(
            recentSearchStore = FakeRecentSearchStore(),
            authRepository = authRepository,
        )

        recentSearchRepository.recordSearch("Rice")
        recentSearchRepository.recordSearch("Milk")
        recentSearchRepository.recordSearch("Rice")

        var recentSearches = emptyList<com.example.pricecart.data.model.RecentSearchItem>()
        recentSearchRepository.fetchRecentSearches(
            onSuccess = { recentSearches = it },
            onError = { throw it },
        )

        assertEquals(listOf("Rice", "Milk"), recentSearches.map { it.queryDisplayName })
    }

    @Test
    fun recordSearch_limitsStoredItemsToConfiguredMaximum() {
        val authRepository = AuthRepository(
            authClient = FakeAuthClient(AuthUser(uid = "uid-trim", email = "trim@example.com")),
            userProfileStore = FakeUserProfileStore(),
        )
        val recentSearchRepository = RecentSearchRepository(
            recentSearchStore = FakeRecentSearchStore(),
            authRepository = authRepository,
        )

        repeat(7) { index ->
            recentSearchRepository.recordSearch("Item $index")
        }

        var recentSearches = emptyList<com.example.pricecart.data.model.RecentSearchItem>()
        recentSearchRepository.fetchRecentSearches(
            onSuccess = { recentSearches = it },
            onError = { throw it },
        )

        assertEquals(6, recentSearches.size)
        assertFalse(recentSearches.any { recentSearch -> recentSearch.queryDisplayName == "Item 0" })
    }
}
