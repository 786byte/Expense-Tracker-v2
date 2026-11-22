package com.byte786.expensetracker.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AdviceTest {
    private val zone = ZoneOffset.UTC
    private fun at(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000
    private val today = LocalDate.of(2026, 11, 10)

    @Test fun coversAllTopics() {
        val txns = listOf(
            Txn(type = TxnType.INCOME, amount = 3_000_000, date = at(today.withDayOfMonth(1)), accountId = 1),
            Txn(type = TxnType.EXPENSE, amount = 500_000, date = at(today.withDayOfMonth(5)), accountId = 1, categoryId = 7),
            Txn(type = TxnType.EXPENSE, amount = 100_000, date = at(today.minusMonths(1).withDayOfMonth(5)), accountId = 1, categoryId = 7),
            Txn(type = TxnType.EXPENSE, amount = 900_000, date = at(today.minusMonths(1).withDayOfMonth(20)), accountId = 1, categoryId = 7),
        )
        val tips = Advice.tips(
            txns, listOf(Advice.AccountInfo("Cash", 2_500_000), Advice.AccountInfo("Loan", -100_000)),
            categoryName = { if (it == 7L) "Bike" else null }, refIncome = 0,
            money = { "৳" + it / 100 }, today = today, zone = zone,
        )
        val titles = tips.joinToString("\n") { it.title + ": " + it.text }
        println(titles)
        assertTrue(titles.contains("Where you stand"))
        assertTrue(titles.contains("৳4000 more than by this day last month"))
        assertTrue(titles.contains("Growing fastest: Bike"))
        assertTrue(titles.contains("You saved 83%"))
        assertTrue(titles.contains("21 days left"))
        assertTrue(titles.contains("Loan is negative"))
    }

    @Test fun emptyData() {
        val tips = Advice.tips(emptyList(), emptyList(), { null }, 0, { it.toString() }, today, zone)
        assertTrue(tips.size == 1)
    }
}
