package com.example.pricecart.search

import java.util.Locale

object SearchTextFormatter {
    fun normalizeQuery(rawQuery: String): String {
        return rawQuery.trim().lowercase(Locale.ROOT)
    }
}
