package com.riccardopinato.cashmateai.domain

import com.riccardopinato.cashmateai.data.MoneyTransactionEntity
import com.riccardopinato.cashmateai.data.PaymentAccountEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class TransactionKind { INCOME, EXPENSE }

object FinanceMath {
    private val amountPattern = Regex("""^\d+(\.\d{1,2})?$""")

    /** Parses a strictly positive euro amount; supports comma decimals. Avoids Double rounding. */
    fun parsePositiveCents(text: String): Long? {
        val raw = text.trim().replace("€", "").replace(" ", "")
        val normalized = when {
            ',' in raw && '.' in raw && raw.lastIndexOf(',') > raw.lastIndexOf('.') ->
                raw.replace(".", "").replace(',', '.')
            ',' in raw && '.' in raw ->
                raw.replace(",", "")
            else -> raw.replace(',', '.')
        }
        if (!amountPattern.matches(normalized)) return null
        return try {
            BigDecimal(normalized).movePointRight(2).longValueExact().takeIf { it > 0L }
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun euro(cents: Long): String =
        NumberFormat.getCurrencyInstance(Locale.ITALY).format(BigDecimal.valueOf(cents, 2))

    /** A financial entry can be saved only on a real, existing payment account. */
    fun validAccountId(accounts: List<PaymentAccountEntity>, id: String?): String? =
        id?.takeIf { selected -> accounts.any { it.id == selected } }

    fun balanceCents(
        accounts: List<PaymentAccountEntity>,
        transactions: List<MoneyTransactionEntity>,
        filterAccountId: String? = null
    ): Long {
        val visibleAccounts = accounts.filter { filterAccountId == null || it.id == filterAccountId }
        val allowedIds = visibleAccounts.map { it.id }.toSet()
        val opening = visibleAccounts.sumOf { it.openingBalanceCents }
        val net = transactions.filter { it.accountId in allowedIds }.sumOf {
            if (it.kind == TransactionKind.INCOME.name) it.amountCents else -it.amountCents
        }
        return opening + net
    }

    fun monthTransactions(
        transactions: List<MoneyTransactionEntity>,
        accountId: String? = null,
        now: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<MoneyTransactionEntity> = transactions.filter {
        (accountId == null || it.accountId == accountId) &&
            Instant.ofEpochMilli(it.occurredAtMillis).atZone(zone).toLocalDate()
                .let { date -> date.year == now.year && date.month == now.month }
    }

    /** Deterministically distributes every cent, including the remainder. */
    fun splitEqually(totalCents: Long, people: Int): List<Long> {
        require(totalCents > 0L) { "Importo positivo obbligatorio" }
        require(people in 2..100) { "Numero partecipanti da 2 a 100" }
        val base = totalCents / people
        val remainder = (totalCents % people).toInt()
        return List(people) { position -> base + if (position < remainder) 1L else 0L }
    }
}
