package com.example.pricecart

import com.example.pricecart.search.SearchTextFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTextFormatterTest {
    @Test
    fun normalizeQuery_trimsWhitespaceAndLowercasesText() {
        val normalizedQuery = SearchTextFormatter.normalizeQuery("  BANANAS ")

        assertEquals("bananas", normalizedQuery)
    }
}
