package com.riccardopinato.cashmateai.domain

import com.riccardopinato.cashmateai.data.MoneyTransactionEntity
import com.riccardopinato.cashmateai.data.PaymentAccountEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class FinanceMathTest {
    private val account = PaymentAccountEntity("account-a", "Carta", 100_00, 0)
    private val other = PaymentAccountEntity("account-b", "Contanti", 0, 0)

    @Test fun decimalAmountsAreExact() {
        assertEquals(12345L, FinanceMath.parsePositiveCents("123,45"))
        assertEquals(123456L, FinanceMath.parsePositiveCents("1.234,56"))
        assertEquals(123456L, FinanceMath.parsePositiveCents("1,234.56"))
        assertEquals(1L, FinanceMath.parsePositiveCents("0,01"))
        assertNull(FinanceMath.parsePositiveCents("10,999"))
        assertNull(FinanceMath.parsePositiveCents("-2"))
        assertNull(FinanceMath.parsePositiveCents("0"))
    }

    @Test fun allAccountsIsNeverAWriteAccount() {
        val accounts = listOf(account, other)
        assertNull(FinanceMath.validAccountId(accounts, null))
        assertNull(FinanceMath.validAccountId(accounts, "all"))
        assertNull(FinanceMath.validAccountId(accounts, "orphan"))
        assertEquals("account-a", FinanceMath.validAccountId(accounts, "account-a"))
    }

    @Test fun balanceIncludesOpeningOnlyOnSelectedAccounts() {
        val entries = listOf(
            MoneyTransactionEntity("1", "account-a", "INCOME", 250, "Bonus", "", 0),
            MoneyTransactionEntity("2", "account-a", "EXPENSE", 400, "Spesa", "", 0),
            MoneyTransactionEntity("3", "account-b", "INCOME", 300, "Cash", "", 0)
        )
        assertEquals(10_150L, FinanceMath.balanceCents(listOf(account, other), entries))
        assertEquals(9_850L, FinanceMath.balanceCents(listOf(account, other), entries, "account-a"))
    }

    @Test fun splitKeepsEveryCent() {
        assertEquals(listOf(334L, 333L, 333L), FinanceMath.splitEqually(1_000, 3))
        assertEquals(1_001L, FinanceMath.splitEqually(1_001, 3).sum())
    }

    @Test fun monthFilterIsCalendarAware() {
        val zone = ZoneId.of("Europe/Rome")
        val millis = LocalDate.of(2026, 11, 12).atStartOfDay(zone).toInstant().toEpochMilli()
        val entries = listOf(
            MoneyTransactionEntity("x", "account-a", "EXPENSE", 500L, "Test", "", millis)
        )
        assertEquals(1, FinanceMath.monthTransactions(entries, "account-a", LocalDate.of(2026, 11, 26), zone).size)
        assertEquals(0, FinanceMath.monthTransactions(entries, "account-a", LocalDate.of(2026, 12, 1), zone).size)
    }
}
