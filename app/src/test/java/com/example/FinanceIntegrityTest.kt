package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.UserSettingsStore
import com.example.data.remote.FirebaseAuthService
import com.example.data.repository.AccountRepository
import com.example.data.repository.FinanceRepository
import com.example.ai.GeminiAiService
import com.example.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FinanceIntegrityTest {
    private suspend fun withLedger(block: suspend (AppDatabase, AccountRepository, FinanceRepository) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val accounts = AccountRepository(db, db.accountDao(), db.transactionDao())
            val finance = FinanceRepository(
                db, db.transactionDao(), db.accountDao(), db.splitBillDao(), db.budgetDao(),
                UserSettingsStore(context), FirebaseAuthService(context), GeminiAiService(db.aiUsageLogDao())
            )
            block(db, accounts, finance)
        } finally { db.close() }
    }
    private fun newAccount(balance: Double = 0.0) = Account(
        nome = "Conto di prova", tipo = "Contanti", saldo = balance,
        colore = "#000000", icona = "€"
    )
    private fun expense(accountId: Int?, amount: Double, id: Int = 0, method: String = "Cash") = Transaction(
        id = id, amount = amount, type = TransactionType.EXPENSE,
        category = "Test", dateMillis = 123456L, note = "", paymentMethod = method, accountId = accountId
    )

    @Test fun requiresRealAccountAndRejectsLegacyOrphans() = runBlocking {
        withLedger { db, accounts, finance ->
            val id = accounts.insertAccount(newAccount(10.0)).toInt()
            assertTrue(runCatching { finance.insertTransaction(expense(null, 2.0)) }.isFailure)
            assertTrue(runCatching { finance.insertTransaction(expense(999, 2.0)) }.isFailure)
            assertEquals(0, db.transactionDao().getAllTransactions().first().size)
            assertEquals(10.0, db.accountDao().getAccountByIdSync(id)!!.saldo, 0.000001)
        }
    }

    @Test fun insertingDuplicateIdNeverDoublesBalance() = runBlocking {
        withLedger { db, accounts, finance ->
            val id = accounts.insertAccount(newAccount(100.0)).toInt()
            finance.insertTransaction(expense(id, 12.35))
            val tx = db.transactionDao().getAllTransactions().first().single()
            assertEquals(87.65, db.accountDao().getAccountByIdSync(id)!!.saldo, 0.000001)
            assertTrue(runCatching { finance.insertTransaction(expense(id, 12.35, tx.id)) }.isFailure)
            assertEquals(1, db.transactionDao().getAllTransactions().first().size)
            assertEquals(87.65, db.accountDao().getAccountByIdSync(id)!!.saldo, 0.000001)
        }
    }

    @Test fun failedAndFractionalWritesDoNotChangeBalance() = runBlocking {
        withLedger { db, accounts, finance ->
            val id = accounts.insertAccount(newAccount(20.0)).toInt()
            assertTrue(runCatching { finance.insertTransaction(expense(id, 0.001)) }.isFailure)
            assertTrue(runCatching { finance.insertTransaction(expense(id, Double.NaN)) }.isFailure)
            assertEquals(20.0, db.accountDao().getAccountByIdSync(id)!!.saldo, 0.000001)
            assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
        }
    }

    @Test fun deleteOrdinaryExpenseReversesExactlyOnce() = runBlocking {
        withLedger { db, accounts, finance ->
            val id = accounts.insertAccount(newAccount(100.0)).toInt()
            finance.insertTransaction(expense(id, 0.10))
            val txId = db.transactionDao().getAllTransactions().first().single().id
            finance.deleteTransaction(txId)
            finance.deleteTransaction(txId) // already deleted must be idempotent
            assertEquals(100.0, db.accountDao().getAccountByIdSync(id)!!.saldo, 0.0000001)
            assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
        }
    }

    @Test fun transferOneHalfCannotBeDeleted() = runBlocking {
        withLedger { db, accounts, finance ->
            val a = accounts.insertAccount(newAccount(15.0)).toInt()
            val b = accounts.insertAccount(newAccount(0.0)).toInt()
            accounts.transfer(a, b, 5.25)
            val entryId = db.transactionDao().getAllTransactions().first().first().id
            assertTrue(runCatching { finance.deleteTransaction(entryId) }.isFailure)
            assertEquals(9.75, db.accountDao().getAccountByIdSync(a)!!.saldo, 0.000001)
            assertEquals(5.25, db.accountDao().getAccountByIdSync(b)!!.saldo, 0.000001)
            assertEquals(2, db.transactionDao().getAllTransactions().first().size)
        }
    }

    @Test fun accountBalanceCannotBeOverwrittenDirectly() = runBlocking {
        withLedger { db, accounts, finance ->
            val id = accounts.insertAccount(newAccount(2.0)).toInt()
            assertTrue(runCatching { accounts.updateAccount(newAccount(9999.0).copy(id=id)) }.isFailure)
            assertEquals(2.0, db.accountDao().getAccountByIdSync(id)!!.saldo, 0.000001)
            assertTrue(runCatching { accounts.insertAccount(newAccount(Double.NaN)) }.isFailure)
        }
    }
    @Test fun v6LedgerSurvivesDatabaseCloseAndReopen() = runBlocking {
        val fileName = "cashmate_reopen_${System.nanoTime()}.db"
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = Room.databaseBuilder(context, AppDatabase::class.java, fileName).build()
        try {
            first.accountDao().insertAccount(newAccount(17.25))
        } finally { first.close() }
        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, fileName).build()
        try {
            assertEquals(17.25, reopened.accountDao().getAllAccounts().first().single().saldo, 0.000001)
            assertEquals(6, reopened.openHelper.writableDatabase.version)
        } finally {
            reopened.close()
            context.deleteDatabase(fileName)
        }
    }

    @Test fun localIdentityStaysStableWithoutCloudAuthentication() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = FirebaseAuthService(context)
        val user = first.loginOffline().getOrThrow()
        assertEquals("local", user.provider)
        assertTrue(user.id.startsWith("local_"))
        val second = FirebaseAuthService(context)
        assertEquals(user.id, second.loginOffline().getOrThrow().id)
        assertTrue(second.login("test@example.invalid", "test").isFailure)
        assertTrue(second.register("test@example.invalid", "Test", "test").isFailure)
    }

}
