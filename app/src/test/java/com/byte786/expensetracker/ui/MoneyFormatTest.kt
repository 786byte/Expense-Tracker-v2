package com.byte786.expensetracker.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatTest {
    @Test fun international() {
        val s = MoneyStyle()
        assertEquals("৳1,234,567.5", Money.format(123_456_750, "৳", style = s))
        assertEquals("-৳12.05", Money.format(-1205, "৳", style = s))
        assertEquals("৳999", Money.format(99_900, "৳", style = s))
    }

    @Test fun indianGrouping() {
        val s = MoneyStyle(indianGrouping = true)
        assertEquals("৳12,34,567", Money.format(123_456_700, "৳", style = s))
        assertEquals("৳1,00,000", Money.format(10_000_000, "৳", style = s))
    }

    @Test fun noDecimalsAndSymbolAfter() {
        val s = MoneyStyle(symbolAfter = true, decimals = false)
        assertEquals("+1,235 Tk", Money.format(123_450, "Tk ", "+", style = s))
        assertEquals("-13 ৳", Money.format(-1250, "৳", style = s))
    }
}
