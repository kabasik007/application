package com.kabasik007.wrongulator.core

import java.math.BigDecimal
import java.math.MathContext

/** Logic is pure Kotlin: no Activity, Compose, network, or payment SDK dependencies. */
enum class Operator(val symbol: String) {
    ADD("+"), SUBTRACT("−"), MULTIPLY("×"), DIVIDE("÷")
}

enum class CalcKey(val label: String) {
    CLEAR("AC"), SIGN("±"), PERCENT("%"), DIVIDE("÷"),
    SEVEN("7"), EIGHT("8"), NINE("9"), MULTIPLY("×"),
    FOUR("4"), FIVE("5"), SIX("6"), SUBTRACT("−"),
    ONE("1"), TWO("2"), THREE("3"), ADD("+"),
    BACKSPACE("⌫"), ZERO("0"), DOT("."), EQUALS("=")
}

enum class ResultKind { READY, WRONG, CORRECT, ERROR }

data class CalculatorState(
    val display: String = "0",
    val expression: String = "",
    val leftOperand: String? = null,
    val operator: Operator? = null,
    val nextDigitReplaces: Boolean = false,
    val resultKind: ResultKind = ResultKind.READY,
)

object CalculatorMath {
    private val precision = MathContext.DECIMAL64

    fun evaluate(left: BigDecimal, operator: Operator, right: BigDecimal): BigDecimal =
        when (operator) {
            Operator.ADD -> left.add(right)
            Operator.SUBTRACT -> left.subtract(right)
            Operator.MULTIPLY -> left.multiply(right)
            Operator.DIVIDE -> {
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw ArithmeticException("Division by zero")
                }
                left.divide(right, precision)
            }
        }

    /**
     * Parody mode deliberately returns a wrong answer. It is explicitly labeled as such
     * by the UI, and its value is never stored/advertised as a real financial calculation.
     */
    fun parody(left: BigDecimal, operator: Operator, right: BigDecimal): BigDecimal {
        val correct = evaluate(left, operator, right)
        if (operator == Operator.ADD &&
            left.compareTo(BigDecimal.valueOf(2)) == 0 &&
            right.compareTo(BigDecimal.valueOf(2)) == 0
        ) return BigDecimal.valueOf(5)

        val mix = (left.hashCode().toLong() * 31L) +
            (right.hashCode().toLong() * 17L) + operator.ordinal.toLong()
        val magnitude = ((mix and 0x7fffffffL) % 9L) + 1L
        val delta = if ((mix and 1L) == 0L) magnitude else -magnitude
        return correct.add(BigDecimal.valueOf(delta))
    }

    fun format(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        // Input is bounded to 12 digits; keep all output deterministic and printable.
        val plain = stripped.toPlainString()
        return if (plain.length <= 28) plain else stripped.round(MathContext(12)).toString()
    }
}

/**
 * Immutable transition function for the calculator keypad.
 * The premium entitlement is supplied from outside; the machine cannot grant it.
 */
class CalculatorMachine {
    fun press(state: CalculatorState, key: CalcKey, accurate: Boolean): CalculatorState =
        when (key) {
            CalcKey.CLEAR -> CalculatorState()
            CalcKey.BACKSPACE -> backspace(state)
            CalcKey.SIGN -> sign(state)
            CalcKey.PERCENT -> percent(state)
            CalcKey.DOT -> appendDot(state)
            CalcKey.ADD -> chooseOperation(state, Operator.ADD)
            CalcKey.SUBTRACT -> chooseOperation(state, Operator.SUBTRACT)
            CalcKey.MULTIPLY -> chooseOperation(state, Operator.MULTIPLY)
            CalcKey.DIVIDE -> chooseOperation(state, Operator.DIVIDE)
            CalcKey.EQUALS -> equals(state, accurate)
            else -> digit(state, key.label)
        }

    private fun digit(state: CalculatorState, value: String): CalculatorState {
        val reset = state.nextDigitReplaces || state.resultKind != ResultKind.READY
        val current = if (reset) "0" else state.display
        if (current.count(Char::isDigit) >= 12 && !(current == "0" && value != "0")) {
            return state
        }
        val next = if (current == "0") value else if (current == "-0") "-$value" else current + value
        val afterResult = state.resultKind != ResultKind.READY
        return state.copy(
            display = next,
            expression = if (afterResult) "" else state.expression,
            leftOperand = if (afterResult) null else state.leftOperand,
            operator = if (afterResult) null else state.operator,
            nextDigitReplaces = false,
            resultKind = ResultKind.READY,
        )
    }

    private fun appendDot(state: CalculatorState): CalculatorState {
        val reset = state.nextDigitReplaces || state.resultKind != ResultKind.READY
        if (!reset && '.' in state.display) return state
        val afterResult = state.resultKind != ResultKind.READY
        return state.copy(
            display = if (reset) "0." else state.display + ".",
            expression = if (afterResult) "" else state.expression,
            leftOperand = if (afterResult) null else state.leftOperand,
            operator = if (afterResult) null else state.operator,
            nextDigitReplaces = false,
            resultKind = ResultKind.READY,
        )
    }

    private fun backspace(state: CalculatorState): CalculatorState {
        if (state.resultKind != ResultKind.READY) return CalculatorState()
        if (state.nextDigitReplaces) return state.copy(display = "0", nextDigitReplaces = false)
        val shortened = state.display.dropLast(1)
        val next = if (shortened.isEmpty() || shortened == "-") "0" else shortened
        return state.copy(display = next)
    }

    private fun sign(state: CalculatorState): CalculatorState {
        if (state.resultKind == ResultKind.ERROR) return state
        if (state.display == "0" || state.display == "0.") return state
        val changed = if (state.display.startsWith("-")) state.display.drop(1) else "-${state.display}"
        return state.copy(display = changed, nextDigitReplaces = false)
    }

    private fun percent(state: CalculatorState): CalculatorState {
        if (state.resultKind == ResultKind.ERROR) return state
        val value = state.display.toBigDecimalOrNull() ?: return state
        return state.copy(
            display = CalculatorMath.format(value.movePointLeft(2)),
            nextDigitReplaces = false,
            resultKind = ResultKind.READY,
        )
    }

    private fun chooseOperation(state: CalculatorState, operator: Operator): CalculatorState {
        if (state.resultKind == ResultKind.ERROR) return state
        val first = state.display
        return state.copy(
            display = first,
            expression = "$first ${operator.symbol}",
            leftOperand = first,
            operator = operator,
            nextDigitReplaces = true,
            resultKind = ResultKind.READY,
        )
    }

    private fun equals(state: CalculatorState, accurate: Boolean): CalculatorState {
        val left = state.leftOperand?.toBigDecimalOrNull() ?: return state
        val right = state.display.toBigDecimalOrNull() ?: return state
        val operator = state.operator ?: return state
        if (state.nextDigitReplaces) return state
        val expression = "${state.leftOperand} ${operator.symbol} ${state.display} ="
        return try {
            val answer = if (accurate) {
                CalculatorMath.evaluate(left, operator, right)
            } else {
                CalculatorMath.parody(left, operator, right)
            }
            state.copy(
                display = CalculatorMath.format(answer),
                expression = expression,
                leftOperand = null,
                operator = null,
                nextDigitReplaces = true,
                resultKind = if (accurate) ResultKind.CORRECT else ResultKind.WRONG,
            )
        } catch (_: ArithmeticException) {
            state.copy(
                display = "ERR",
                expression = expression,
                leftOperand = null,
                operator = null,
                nextDigitReplaces = true,
                resultKind = ResultKind.ERROR,
            )
        }
    }
}
