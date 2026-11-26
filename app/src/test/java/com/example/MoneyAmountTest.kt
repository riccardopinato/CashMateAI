package com.example

import com.example.domain.model.MoneyAmount
import org.junit.Assert.*
import org.junit.Test

class MoneyAmountTest {
    @Test fun exactDecimalCents() {
        assertEquals(125L, MoneyAmount.cents(1.25))
        assertEquals(10L, MoneyAmount.cents(0.10))
        assertEquals(-100L, MoneyAmount.cents(-1.0))
        assertEquals(0.30, MoneyAmount.applyDelta(0.10, 20L), 0.000000001)
    }
    @Test fun rejectsFractionalCentsAndNonFiniteValues() {
        listOf(0.001, 12.349, Double.NaN, Double.POSITIVE_INFINITY).forEach { amount ->
            assertTrue(runCatching { MoneyAmount.cents(amount) }.isFailure)
        }
    }
    @Test fun arithmeticBoundaryRejectsOverflow() {
        assertTrue(runCatching { MoneyAmount.applyDelta(1.0, Long.MAX_VALUE) }.isFailure)
    }
}
