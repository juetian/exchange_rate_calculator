package com.example.fxcalc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculatorTest {

    private fun eval(s: String) = Calculator.evaluate(s)?.stripTrailingZeros()?.toPlainString()

    @Test fun basics() {
        assertEquals("3", eval("1+2"))
        assertEquals("0.3", eval("0.1+0.2"))
        assertEquals("7", eval("1+2×3"))
        assertEquals("-1", eval("2−3"))
        assertEquals("2.5", eval("5÷2"))
        assertEquals("150", eval("100+50"))
    }

    @Test fun percentAndUnary() {
        assertEquals("0.5", eval("50%"))
        assertEquals("15", eval("100×15%"))
        assertEquals("-5", eval("−5"))
        assertEquals("-2", eval("−5+3"))
    }

    @Test fun incompleteInput() {
        assertEquals("12", eval("12+"))
        assertEquals("12", eval("12×"))
        assertEquals("0", eval(""))
        assertEquals("5", eval("5."))
        assertEquals("0.5", eval(".5"))
    }

    @Test fun errors() {
        assertNull(Calculator.evaluate("5÷0"))
        assertNull(Calculator.evaluate("1..2"))
    }

    @Test fun repeatingDivision() {
        val v = Calculator.evaluate("10÷3")!!
        assertEquals(BigDecimal("3.333333333333333"), v)
    }

    @Test fun operatorDetection() {
        assertFalse(Calculator.hasOperator("123.4"))
        assertFalse(Calculator.hasOperator("−5"))
        assertTrue(Calculator.hasOperator("1+2"))
        assertTrue(Calculator.hasOperator("50%"))
    }
}
