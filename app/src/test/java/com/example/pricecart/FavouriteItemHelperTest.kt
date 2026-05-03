package com.example.pricecart

import com.example.pricecart.favourites.FavouriteItemHelper
import com.example.pricecart.favourites.FavouriteToggleAction
import org.junit.Assert.assertEquals
import org.junit.Test

class FavouriteItemHelperTest {
    @Test
    fun createFavouriteKey_returnsConsistentNormalizedKey() {
        val favouriteKey = FavouriteItemHelper.createFavouriteKey("  Brown Sugar 2LB ")

        assertEquals("brown_sugar_2lb", favouriteKey)
    }

    @Test
    fun resolveToggleAction_returnsSaveWhenItemIsNotSaved() {
        val action = FavouriteItemHelper.resolveToggleAction(isCurrentlySaved = false)

        assertEquals(FavouriteToggleAction.SAVE, action)
    }

    @Test
    fun resolveToggleAction_returnsRemoveWhenItemIsSaved() {
        val action = FavouriteItemHelper.resolveToggleAction(isCurrentlySaved = true)

        assertEquals(FavouriteToggleAction.REMOVE, action)
    }
}
