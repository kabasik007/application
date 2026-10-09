package com.kabasik007.wrongulator.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorMachineTest {
    private val machine = CalculatorMachine()

    private fun calculate(
        accurate: Boolean,
        left: CalcKey,
        operator: CalcKey,
        right: CalcKey,
    ): CalculatorState {
        var state = CalculatorState()
        listOf(left, operator, right, CalcKey.EQUALS).forEach { key ->
            state = machine.press(state, key, accurate)
        }
        return state
    }

    @Test fun freeTwoPlusTwoIsTheFamousFive() {
        val result = calculate(false, CalcKey.TWO, CalcKey.ADD, CalcKey.TWO)
        assertEquals("5", result.display)
        assertEquals(ResultKind.WRONG, result.resultKind)
        assertEquals("2 + 2 =", result.expression)
    }

    @Test fun proTwoPlusTwoIsActuallyFour() {
        val result = calculate(true, CalcKey.TWO, CalcKey.ADD, CalcKey.TWO)
        assertEquals("4", result.display)
        assertEquals(ResultKind.CORRECT, result.resultKind)
    }

    @Test fun parodyNeverAccidentallyEqualsCorrectResult() {
        val digits = listOf(-10, -1, 0, 1, 2, 5, 10)
        for (l in digits) for (r in digits) for (op in Operator.entries) {
            if (op == Operator.DIVIDE && r == 0) continue
            val left = BigDecimal.valueOf(l.toLong())
            val right = BigDecimal.valueOf(r.toLong())
            assertNotEquals(
                "Incorrect result matches correct result for $l ${op.symbol} $r",
                0,
                CalculatorMath.parody(left, op, right)
                    .compareTo(CalculatorMath.evaluate(left, op, right)),
            )
        }
    }

    @Test fun divisionByZeroShowsErrorInBothModes() {
        for (accurate in listOf(false, true)) {
            val state = calculate(accurate, CalcKey.SEVEN, CalcKey.DIVIDE, CalcKey.ZERO)
            assertEquals(ResultKind.ERROR, state.resultKind)
            assertEquals("ERR", state.display)
        }
    }

    @Test fun clearResetsExpressionAndResult() {
        val wrong = calculate(false, CalcKey.TWO, CalcKey.ADD, CalcKey.TWO)
        assertEquals(CalculatorState(), machine.press(wrong, CalcKey.CLEAR, false))
    }

    @Test fun decimalAndPercentWork() {
        var state = CalculatorState()
        listOf(CalcKey.FIVE, CalcKey.ZERO, CalcKey.PERCENT).forEach {
            state = machine.press(state, it, true)
        }
        assertEquals("0.5", state.display)
        val changed = machine.press(state, CalcKey.DOT, true)
        assertEquals("0.5", changed.display)
    }

    @Test fun afterEqualsNewDigitStartsNewExpression() {
        val completed = calculate(true, CalcKey.TWO, CalcKey.ADD, CalcKey.TWO)
        val next = machine.press(completed, CalcKey.SEVEN, true)
        assertEquals("7", next.display)
        assertEquals("", next.expression)
        assertEquals(ResultKind.READY, next.resultKind)
    }

    @Test fun multipleOperatorPressesUseMostRecent() {
        var state = CalculatorState()
        listOf(CalcKey.TWO, CalcKey.ADD, CalcKey.MULTIPLY, CalcKey.THREE, CalcKey.EQUALS).forEach {
            state = machine.press(state, it, true)
        }
        assertEquals("6", state.display)
    }

    @Test fun backspaceAndSignNeverProduceUnparseableInput() {
        var state = CalculatorState()
        listOf(CalcKey.TWO, CalcKey.SIGN, CalcKey.BACKSPACE).forEach {
            state = machine.press(state, it, false)
        }
        assertEquals("0", state.display)
        assertTrue(state.display.toBigDecimalOrNull() != null)
    }

    @Test fun inputIsBoundedToTwelveDigits() {
        var state = CalculatorState()
        repeat(40) { state = machine.press(state, CalcKey.NINE, false) }
        assertEquals(12, state.display.length)
    }
}
