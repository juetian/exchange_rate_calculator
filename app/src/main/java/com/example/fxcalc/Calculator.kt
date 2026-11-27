package com.example.fxcalc

import java.math.BigDecimal
import java.math.MathContext

/**
 * 简单四则运算：支持 + − × ÷ 和后缀 %（除以 100），遵循先乘除后加减。
 * 使用 BigDecimal 计算，避免 0.1 + 0.2 这类浮点误差。
 */
object Calculator {

    const val PLUS = '+'
    const val MINUS = '−'
    const val TIMES = '×'
    const val DIVIDE = '÷'
    val OPERATORS = setOf(PLUS, MINUS, TIMES, DIVIDE)

    private val MC = MathContext.DECIMAL64

    fun hasOperator(expr: String): Boolean =
        expr.drop(1).any { it in OPERATORS } || expr.contains('%')

    /**
     * 计算表达式。末尾未完成的运算符会被忽略（如 "12+" 按 12 计算）。
     * 空表达式返回 0；无法计算（如除以 0）返回 null。
     */
    fun evaluate(expr: String): BigDecimal? {
        val s = expr.trimEnd { it in OPERATORS }
        if (s.isEmpty()) return BigDecimal.ZERO
        return try {
            Parser(s).parse()
        } catch (e: ArithmeticException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private class Parser(val s: String) {
        var pos = 0

        fun parse(): BigDecimal {
            val v = expr()
            require(pos == s.length) { "unexpected '${s[pos]}'" }
            return v
        }

        private fun expr(): BigDecimal {
            var v = term()
            while (pos < s.length && (s[pos] == PLUS || s[pos] == MINUS)) {
                val op = s[pos++]
                val r = term()
                v = if (op == PLUS) v.add(r, MC) else v.subtract(r, MC)
            }
            return v
        }

        private fun term(): BigDecimal {
            var v = factor()
            while (pos < s.length && (s[pos] == TIMES || s[pos] == DIVIDE)) {
                val op = s[pos++]
                val r = factor()
                v = if (op == TIMES) v.multiply(r, MC) else v.divide(r, MC)
            }
            return v
        }

        private fun factor(): BigDecimal {
            var negative = false
            while (pos < s.length && (s[pos] == MINUS || s[pos] == '-')) {
                negative = !negative
                pos++
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            require(pos > start) { "number expected at $start" }
            var text = s.substring(start, pos)
            if (text.startsWith(".")) text = "0$text"
            var v = BigDecimal(text)
            while (pos < s.length && s[pos] == '%') {
                v = v.divide(BigDecimal(100), MC)
                pos++
            }
            return if (negative) v.negate() else v
        }
    }
}
