package com.byte786.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IconGuessTest {
    private fun g(name: String) = CsvImport.guessIcon(name, "category")

    @Test fun ownerCategories() {
        assertEquals("bike", g("Pulsar N160"))
        assertEquals("powerup", g("Aura Maxing"))
        assertEquals("mosque", g("Deen"))
        assertEquals("perfume", g("Fragrance"))
        assertEquals("tax", g("Moms Tax"))
        assertEquals("charity", g("Mom's Blessing"))
        assertEquals("teacher", g("Tution"))
        assertEquals("translate", g("Alliance Française"))
        assertEquals("ai", g("Claude"))
        assertEquals("computer", g("Pc"))
        assertEquals("theatre", g("Theatre"))
        assertEquals("casino", g("Lottery"))
        assertEquals("sell", g("Sale"))
        assertEquals("redeem", g("Grants"))
        assertEquals("smartphone", g("Phone"))
    }

    @Test fun wordStartsOnly() {
        assertEquals("category", g("Upcycle")) // no "pc" inside a word
        assertEquals("category", g("Scar"))
    }
}
