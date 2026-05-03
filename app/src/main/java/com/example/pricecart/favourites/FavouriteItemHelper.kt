package com.example.pricecart.favourites

import com.example.pricecart.search.SearchTextFormatter

enum class FavouriteToggleAction {
    SAVE,
    REMOVE,
}

object FavouriteItemHelper {
    fun normalizeProductName(productName: String): String {
        return SearchTextFormatter.normalizeQuery(productName)
    }

    fun createFavouriteKey(productName: String): String {
        val normalizedName = normalizeProductName(productName)
        return normalizedName
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "favourite_item" }
    }

    fun resolveToggleAction(isCurrentlySaved: Boolean): FavouriteToggleAction {
        return if (isCurrentlySaved) {
            FavouriteToggleAction.REMOVE
        } else {
            FavouriteToggleAction.SAVE
        }
    }
}
