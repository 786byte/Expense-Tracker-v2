package com.byte786.expensetracker.data

/**
 * The status cat's moods, from worst to best. ANGRY = this month's spending is over income.
 * PLAIN is only used for the add-menu cats.
 */
enum class CatMood { VERY_DEBT, DEBT, ANGRY, NEUTRAL, PLUS, RICH, PLAIN }

object MoodRules {
    /**
     * Decides the status cat's mood. Thresholds are percentages of [refIncome]
     * (the usual monthly income): within ±[neutralPct]% is "even", beyond [veryPct]% is "very".
     */
    fun mood(
        balance: Long,
        monthIncome: Long,
        monthExpense: Long,
        refIncome: Long,
        neutralPct: Int,
        veryPct: Int,
    ): CatMood {
        val ref = if (refIncome > 0) refIncome else maxOf(monthIncome, monthExpense)
        val band = ref * neutralPct / 100
        val very = maxOf(ref * veryPct / 100, band + 1)
        return when {
            ref == 0L -> when {
                balance < 0 -> CatMood.DEBT
                balance > 0 -> CatMood.PLUS
                else -> CatMood.NEUTRAL
            }
            balance < -very -> CatMood.VERY_DEBT
            balance < -band -> CatMood.DEBT
            monthExpense > monthIncome && monthExpense - monthIncome > band -> CatMood.ANGRY
            balance > very -> CatMood.RICH
            balance > band -> CatMood.PLUS
            else -> CatMood.NEUTRAL
        }
    }

    /** Average income per month over the [months] full months before [monthStart] (epoch millis ranges given). */
    fun averageIncome(txns: List<Txn>, ranges: List<Pair<Long, Long>>): Long {
        if (ranges.isEmpty()) return 0
        val sums = ranges.map { (f, t) -> txns.filter { it.type == TxnType.INCOME && it.date >= f && it.date < t }.sumOf { it.amount } }
        val nonEmpty = sums.filter { it > 0 }
        return if (nonEmpty.isEmpty()) 0 else nonEmpty.sum() / nonEmpty.size
    }
}
