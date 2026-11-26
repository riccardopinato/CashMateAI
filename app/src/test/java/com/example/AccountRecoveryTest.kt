package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.AccountRepository
import com.example.domain.model.Account
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountRecoveryTest {
    @Test
    fun transferIsAtomicAndAccountWithMovementsIsProtected() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val sourceId = db.accountDao().insertAccount(Account(
                nome = "Conto di prova A", tipo = "Conto Corrente", saldo = 50.0,
                colore = "#000000", icona = "A"
            )).toInt()
            val destinationId = db.accountDao().insertAccount(Account(
                nome = "Conto di prova B", tipo = "Conto Corrente", saldo = 10.0,
                colore = "#000000", icona = "B"
            )).toInt()
            val repository = AccountRepository(db, db.accountDao(), db.transactionDao())
            repository.transfer(sourceId, destinationId, 5.25)
            assertEquals(44.75, db.accountDao().getAccountByIdSync(sourceId)!!.saldo, 0.00001)
            assertEquals(15.25, db.accountDao().getAccountByIdSync(destinationId)!!.saldo, 0.00001)
            assertEquals(2, db.transactionDao().getAllTransactions().first().size)
            assertTrue(runCatching { repository.deleteAccount(sourceId) }.isFailure)
            assertNotNull(db.accountDao().getAccountByIdSync(sourceId))
            assertTrue(runCatching { repository.transfer(sourceId, sourceId, 1.0) }.isFailure)
            assertEquals(2, db.transactionDao().getAllTransactions().first().size)
        } finally {
            db.close()
        }
    }
}
