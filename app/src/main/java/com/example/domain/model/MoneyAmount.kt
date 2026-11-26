package com.example.domain.model

import java.math.BigDecimal

/**
 * A v6-compatible precision boundary: the historic Room schema stores REAL/Double.
 * All new financial operations must use integral cents and convert only at persistence.
 * We deliberately do not change the Room schema during historical APK recovery.
 */
object MoneyAmount {
    fun cents(value: Double): Long {
        require(value.isFinite()) { "Importo non finito" }
        return try {
            BigDecimal.valueOf(value).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("L'importo deve avere al massimo due decimali ed essere rappresentabile in centesimi")
        }
    }

    fun euros(cents: Long): Double = BigDecimal.valueOf(cents, 2).toDouble()

    fun applyDelta(balanceEuros: Double, deltaCents: Long): Double =
        euros(Math.addExact(cents(balanceEuros), deltaCents))
}
