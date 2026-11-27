package com.byte786.expensetracker.data

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Evaluates simple sums typed on the calculator keypad, e.g. "600+400-50" or "3×120". */
object Calc {
    private val ops = setOf('+', '-', '×', '÷', '*', '/')

    /** Result in minor units (paisa/cents), or null if the expression is incomplete or invalid. */
    fun evalMinor(expr: String): Long? = eval(expr)
        ?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.toLong()

    fun eval(expr: String): BigDecimal? {
        val s = expr.replace(",", "").replace(" ", "")
        if (s.isEmpty()) return null
        // Tokenize into numbers and operators. A leading '-' belongs to the first number.
        val nums = mutableListOf<BigDecimal>()
        val opsList = mutableListOf<Char>()
        var i = 0
        while (i < s.length) {
            val start = i
            if (s[i] == '-' && nums.size == opsList.size) i++ // unary minus
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            val n = s.substring(start, i).toBigDecimalOrNull() ?: return null
            nums.add(n)
            if (i < s.length) {
                if (s[i] !in ops) return null
                opsList.add(s[i]); i++
                if (i == s.length) opsList.removeAt(opsList.lastIndex) // ignore a trailing operator
            }
        }
        if (nums.isEmpty() || opsList.size != nums.size - 1) return null
        // First × and ÷, then + and −.
        val n2 = mutableListOf(nums[0])
        val o2 = mutableListOf<Char>()
        opsList.forEachIndexed { k, op ->
            val b = nums[k + 1]
            when (op) {
                '×', '*' -> n2[n2.lastIndex] = n2.last().multiply(b)
                '÷', '/' -> {
                    if (b.signum() == 0) return null
                    n2[n2.lastIndex] = n2.last().divide(b, MathContext.DECIMAL64)
                }
                else -> { o2.add(op); n2.add(b) }
            }
        }
        var acc = n2[0]
        o2.forEachIndexed { k, op -> acc = if (op == '+') acc.add(n2[k + 1]) else acc.subtract(n2[k + 1]) }
        return acc
    }

    fun hasOperator(expr: String): Boolean = expr.drop(1).any { it in ops }
}
