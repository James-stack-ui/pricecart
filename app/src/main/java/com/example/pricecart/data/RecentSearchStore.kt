package com.example.pricecart.data

import com.example.pricecart.data.model.RecentSearchItem

interface RecentSearchStore {
    fun fetchRecentSearches(
        userId: String,
        limit: Int,
        onSuccess: (List<RecentSearchItem>) -> Unit,
        onError: (Exception) -> Unit,
    )

    fun recordSearch(
        userId: String,
        queryDisplayName: String,
        limit: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    )
}
