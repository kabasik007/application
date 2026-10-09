package com.kabasik007.wrongulator.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PracticalMathTest {
    @Test fun twentyPercentDiscountIsAccurate() {
        val result = PracticalMath.discount(BigDecimal("250.00"), BigDecimal("20"))
        assertEquals("50.00", PracticalMath.money(result.savings))
        assertEquals("200.00", PracticalMath.money(result.final))
    }

    @Test fun parsesCommaAndRejectsInvalidAmounts() {
        assertEquals(BigDecimal("10.5"), PracticalMath.parseAmount("10,5"))
        assertNull(PracticalMath.parseAmount("-1"))
        assertNull(PracticalMath.parseAmount("hello"))
        assertNull(PracticalMath.parseAmount("99999999999999999"))
    }

    @Test fun percentCannotExceedOneHundred() {
        assertThrows(IllegalArgumentException::class.java) {
            PracticalMath.discount(BigDecimal.TEN, BigDecimal("101"))
        }
    }

    @Test fun splitWithTipWorks() {
        val result = PracticalMath.split(BigDecimal("120"), BigDecimal("10"), 3)
        assertEquals("12.00", PracticalMath.money(result.tip))
        assertEquals("132.00", PracticalMath.money(result.total))
        assertEquals("44.00", PracticalMath.money(result.perPerson))
    }

    @Test fun splitRejectsInvalidPeopleCount() {
        assertThrows(IllegalArgumentException::class.java) {
            PracticalMath.split(BigDecimal.TEN, BigDecimal.ZERO, 0)
        }
    }

    @Test fun roundsMoneyDeterministically() {
        val result = PracticalMath.discount(BigDecimal("9.99"), BigDecimal("15"))
        assertEquals("1.50", PracticalMath.money(result.savings))
        assertEquals("8.49", PracticalMath.money(result.final))
    }
}
