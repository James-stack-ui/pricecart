package com.example.pricecart.data

import com.example.pricecart.data.model.RecentSearchItem

class RecentSearchRepository(
    private val recentSearchStore: RecentSearchStore = ServiceLocator.recentSearchStoreFactory(),
    private val authRepository: AuthRepository = AuthRepository(),
) {
    fun fetchRecentSearches(
        onSuccess: (List<RecentSearchItem>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val userId = currentUserId()
        if (userId == null) {
            onSuccess(emptyList())
            return
        }

        recentSearchStore.fetchRecentSearches(userId, MAX_RECENT_SEARCHES, onSuccess, onError)
    }

    fun recordSearch(
        queryDisplayName: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {},
    ) {
        val userId = currentUserId()
        if (userId == null || queryDisplayName.trim().isBlank()) {
            onSuccess()
            return
        }

        recentSearchStore.recordSearch(
            userId = userId,
            queryDisplayName = queryDisplayName,
            limit = MAX_RECENT_SEARCHES,
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    private fun currentUserId(): String? {
        return authRepository.currentUserId()
            ?.takeIf { userId -> userId.isNotBlank() }
    }

    companion object {
        private const val MAX_RECENT_SEARCHES = 6
    }
}
