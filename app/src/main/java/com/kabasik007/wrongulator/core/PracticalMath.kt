package com.kabasik007.wrongulator.core

import java.math.BigDecimal
import java.math.RoundingMode

/** These tools are accurate and available without a paid calculator subscription. */
object PracticalMath {
    private val HUNDRED = BigDecimal("100")

    data class Discount(val original: BigDecimal, val savings: BigDecimal, val final: BigDecimal)
    data class Split(val total: BigDecimal, val perPerson: BigDecimal, val tip: BigDecimal)

    fun parseAmount(text: String): BigDecimal? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty() || normalized.length > 14) return null
        return normalized.toBigDecimalOrNull()
            ?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal("999999999") }
    }

    fun money(value: BigDecimal): String = value.setScale(2, RoundingMode.HALF_UP).toPlainString()

    fun discount(price: BigDecimal, percent: BigDecimal): Discount {
        require(price >= BigDecimal.ZERO && percent >= BigDecimal.ZERO && percent <= HUNDRED)
        val savings = price.multiply(percent).divide(HUNDRED).setScale(2, RoundingMode.HALF_UP)
        return Discount(price, savings, price.subtract(savings).setScale(2, RoundingMode.HALF_UP))
    }

    fun split(bill: BigDecimal, tipPercent: BigDecimal, people: Int): Split {
        require(bill >= BigDecimal.ZERO && tipPercent >= BigDecimal.ZERO)
        require(tipPercent <= HUNDRED && people in 1..100)
        val tip = bill.multiply(tipPercent).divide(HUNDRED).setScale(2, RoundingMode.HALF_UP)
        val total = bill.add(tip).setScale(2, RoundingMode.HALF_UP)
        return Split(total, total.divide(BigDecimal(people), 2, RoundingMode.HALF_UP), tip)
    }
}
