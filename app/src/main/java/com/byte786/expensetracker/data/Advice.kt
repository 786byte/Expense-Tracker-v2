package com.byte786.expensetracker.data

import java.time.LocalDate
import java.time.ZoneId

/** Rule-based tips shown when you tap the status cat. No AI, just arithmetic on your records. */
object Advice {
    data class Tip(val emoji: String, val title: String, val text: String)

    data class AccountInfo(val name: String, val balance: Long)

    /**
     * [money] formats an amount in paisa. [today] and [zone] are parameters so this can be tested.
     */
    fun tips(
        txns: List<Txn>,
        accounts: List<AccountInfo>,
        categoryName: (Long) -> String?,
        refIncome: Long,
        money: (Long) -> String,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Tip> {
        fun ms(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
        fun sum(list: List<Txn>, type: Int) = list.filter { it.type == type }.sumOf { it.amount }
        fun between(from: LocalDate, toExcl: LocalDate): List<Txn> {
            val f = ms(from)
            val t = ms(toExcl)
            return txns.filter { it.date >= f && it.date < t }
        }

        val tips = mutableListOf<Tip>()
        val monthStart = today.withDayOfMonth(1)
        val tomorrow = today.plusDays(1)
        val thisMonth = between(monthStart, monthStart.plusMonths(1))
        val mtd = between(monthStart, tomorrow)
        val lastStart = monthStart.minusMonths(1)
        // Same number of days into last month (capped at its length).
        val lastSameDay = lastStart.plusDays(minOf(today.dayOfMonth.toLong(), lastStart.lengthOfMonth().toLong()))
        val lastMtd = between(lastStart, lastSameDay)
        val lastMonth = between(lastStart, monthStart)

        // 1. Balance
        val total = accounts.sumOf { it.balance }
        tips += Tip(
            if (total < 0) "😿" else "🐱", "Where you stand",
            "Total balance is ${money(total)} across ${accounts.size} account${if (accounts.size == 1) "" else "s"}." +
                if (total < 0) " You owe more than you have, so pay the debts down first." else "",
        )

        // 2. This month vs last month, up to the same day
        val spent = sum(mtd, TxnType.EXPENSE)
        val spentBefore = sum(lastMtd, TxnType.EXPENSE)
        if (spent > 0 || spentBefore > 0) {
            val diff = spent - spentBefore
            val text = when {
                spentBefore == 0L -> "So far this month you spent ${money(spent)}. Nothing was spent by this day last month."
                diff > 0 -> "So far this month you spent ${money(spent)}: ${money(diff)} more than by this day last month (${money(spentBefore)})."
                diff < 0 -> "So far this month you spent ${money(spent)}: ${money(-diff)} less than by this day last month (${money(spentBefore)}). Nice!"
                else -> "So far this month you spent ${money(spent)}, the same as by this day last month."
            }
            tips += Tip(if (diff > 0) "📈" else "📉", "This month vs last month", text)
        }

        // 3. The category that grew most
        val now = mtd.filter { it.type == TxnType.EXPENSE }.groupBy { it.categoryId }.mapValues { e -> e.value.sumOf { it.amount } }
        val before = lastMtd.filter { it.type == TxnType.EXPENSE }.groupBy { it.categoryId }.mapValues { e -> e.value.sumOf { it.amount } }
        val grew = now.map { (id, v) -> id to v - (before[id] ?: 0L) }.filter { it.second > 0 }.maxByOrNull { it.second }
        if (grew != null) {
            val name = grew.first?.let(categoryName) ?: "Uncategorized"
            tips += Tip(
                "🔍", "Growing fastest: $name",
                "$name is up ${money(grew.second)} compared with the same days last month " +
                    "(${money(now[grew.first] ?: 0)} vs ${money(before[grew.first] ?: 0)}).",
            )
        }

        // 4. Saving rate
        val inc = sum(thisMonth, TxnType.INCOME)
        val exp = sum(thisMonth, TxnType.EXPENSE)
        val (rateInc, rateExp, which) = if (inc > 0) Triple(inc, exp, "this month") else
            Triple(sum(lastMonth, TxnType.INCOME), sum(lastMonth, TxnType.EXPENSE), "last month")
        if (rateInc > 0) {
            val rate = ((rateInc - rateExp) * 100 / rateInc).toInt()
            val verdict = when {
                rate >= 30 -> "Excellent, the cat is proud."
                rate >= 10 -> "Good. Aim for 20% or more."
                rate >= 0 -> "Tight. Try to keep at least 10% aside."
                else -> "You spent more than you earned."
            }
            tips += Tip("🐷", "Saving rate", "You saved $rate% of your income $which. $verdict")
        }

        // 5. Days left and a daily budget
        val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
        val budgetIncome = if (inc > 0) inc else refIncome
        if (budgetIncome > 0) {
            val left = budgetIncome - exp
            val src = if (inc > 0) "this month's income" else "your usual monthly income"
            tips += if (left > 0) Tip(
                "📅", "$daysLeft day${if (daysLeft == 1) "" else "s"} left",
                "To stay within $src you can spend about ${money(left / daysLeft)} a day for the rest of the month.",
            ) else Tip(
                "📅", "$daysLeft day${if (daysLeft == 1) "" else "s"} left",
                "You are already ${money(-left)} over $src. Try to spend only on what you must until the month ends.",
            )
        }

        // 6. Loans and negative accounts
        accounts.filter { it.balance < 0 || it.name.contains("loan", true) || it.name.contains("debt", true) }.forEach { a ->
            if (a.balance < 0) tips += Tip(
                "⚠️", "${a.name} is negative",
                "${a.name} is at ${money(a.balance)}. Plan a transfer to pay it back before it grows.",
            ) else if (a.balance > 0) tips += Tip(
                "💳", a.name, "${a.name} holds ${money(a.balance)}. If this is money you owe, remember to return it.",
            )
        }
        return tips
    }
}
