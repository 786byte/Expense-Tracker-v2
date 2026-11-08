package com.byte786.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcTest {
    @Test fun addsAndSubtracts() = assertEquals(95000L, Calc.evalMinor("600+400-50"))
    @Test fun multipliesFirst() = assertEquals(37000L, Calc.evalMinor("10+3×120"))
    @Test fun divides() = assertEquals(3333L, Calc.evalMinor("100÷3"))
    @Test fun decimals() = assertEquals(12550L, Calc.evalMinor("100.5+25"))
    @Test fun ignoresTrailingOperator() = assertEquals(60000L, Calc.evalMinor("600+"))
    @Test fun leadingMinus() = assertEquals(-5000L, Calc.evalMinor("-50"))
    @Test fun divideByZeroIsNull() = assertNull(Calc.evalMinor("5÷0"))
    @Test fun garbageIsNull() = assertNull(Calc.evalMinor("5++"))
}
