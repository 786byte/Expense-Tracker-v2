package com.byte786.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MoodTest {
    private fun m(balance: Long, inc: Long = 0, exp: Long = 0, ref: Long = 100_000) =
        MoodRules.mood(balance, inc, exp, ref, neutralPct = 10, veryPct = 50)

    @Test fun veryInDebt() = assertEquals(CatMood.VERY_DEBT, m(-60_000))
    @Test fun slightlyInDebt() = assertEquals(CatMood.DEBT, m(-20_000))
    @Test fun evenWithinBand() = assertEquals(CatMood.NEUTRAL, m(5_000))
    @Test fun slightlyUp() = assertEquals(CatMood.PLUS, m(20_000))
    @Test fun wellUp() = assertEquals(CatMood.RICH, m(80_000))
    @Test fun angryWhenOverspending() = assertEquals(CatMood.ANGRY, m(80_000, inc = 50_000, exp = 90_000))
    @Test fun debtBeatsAngry() = assertEquals(CatMood.DEBT, m(-20_000, inc = 0, exp = 90_000))
    @Test fun noIncomeHistory() {
        assertEquals(CatMood.DEBT, MoodRules.mood(-5, 0, 0, 0, 10, 50))
        assertEquals(CatMood.PLUS, MoodRules.mood(5, 0, 0, 0, 10, 50))
        assertEquals(CatMood.NEUTRAL, MoodRules.mood(0, 0, 0, 0, 10, 50))
    }
}
