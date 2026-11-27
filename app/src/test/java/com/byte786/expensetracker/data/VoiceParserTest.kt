package com.byte786.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceParserTest {
    private val cats = listOf(
        VoiceParser.Cat(1, "Bike", TxnType.EXPENSE, null),
        VoiceParser.Cat(2, "Mobil", TxnType.EXPENSE, 1),
        VoiceParser.Cat(3, "Fuel", TxnType.EXPENSE, 1),
        VoiceParser.Cat(4, "Bills", TxnType.EXPENSE, null),
        VoiceParser.Cat(5, "Food", TxnType.EXPENSE, null),
        VoiceParser.Cat(6, "Restaurant", TxnType.EXPENSE, 5),
        VoiceParser.Cat(7, "Salary", TxnType.INCOME, null),
        VoiceParser.Cat(8, "Tuition", TxnType.INCOME, 7),
    )
    private val accs = listOf(
        VoiceParser.Acc(10, "Cash"), VoiceParser.Acc(11, "bKash"),
        VoiceParser.Acc(12, "Bank"), VoiceParser.Acc(13, "Tuition"),
    )
    private fun p(s: String) = VoiceParser.parse(s, cats, accs)

    @Test fun simpleExpenseWithSubcategory() {
        val d = p("500 taka bike mobil from cash")
        assertEquals(TxnType.EXPENSE, d.type)
        assertEquals(50000L, d.total)
        assertEquals(1L, d.categoryId)
        assertEquals(2L, d.subcategoryId)
        assertEquals(listOf(10L to null), d.sources)
    }

    @Test fun subcategoryAloneFindsItsParent() {
        val d = p("fuel 1650 cash")
        assertEquals(1L, d.categoryId)
        assertEquals(3L, d.subcategoryId)
        assertEquals(165000L, d.total)
    }

    @Test fun twoSourcesWithTheirParts() {
        val d = p("1,000 bills 600 cash 400 b kash")
        assertEquals(100000L, d.total)
        assertEquals(4L, d.categoryId)
        assertEquals(listOf(10L to 60000L, 11L to 40000L), d.sources)
    }

    @Test fun incomeIntoAccountNamedLikeASubcategory() {
        val d = p("got 5000 salary in tuition")
        assertEquals(TxnType.INCOME, d.type)
        assertEquals(7L, d.categoryId)
        assertEquals(null, d.subcategoryId)
        assertEquals(listOf(13L to null), d.sources)
    }

    @Test fun transfer() {
        val d = p("transfer 2k from cash to bkash")
        assertEquals(TxnType.TRANSFER, d.type)
        assertEquals(200000L, d.total)
        assertEquals(listOf(10L to null), d.sources)
        assertEquals(11L, d.toAccountId)
    }

    @Test fun titleAndYesterday() {
        val d = p("yesterday 230 for lunch with friends food restaurant from cash")
        assertEquals(1, d.daysAgo)
        assertEquals("Lunch", d.title)
        assertEquals(6L, d.subcategoryId)
    }
}
