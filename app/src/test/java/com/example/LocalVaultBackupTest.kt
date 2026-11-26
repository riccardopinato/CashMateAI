package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.LocalVaultBackup
import com.example.domain.model.Account
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalVaultBackupTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun db() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val password = "UnPassphraseLungo!2026".toCharArray()

    @Test fun encryptedRoundtripRestoresAllRoomTablesOnEmptyLedger() = runBlocking {
        val first = db()
        val second = db()
        try {
            val id = first.accountDao().insertAccount(Account(
                nome="Conto vero", tipo="Contanti", saldo=100.25,
                colore="#123456", icona="€"
            )).toInt()
            first.transactionDao().insertTransaction(Transaction(
                amount=2.35, type=TransactionType.EXPENSE, category="Cibo",
                dateMillis=10L, note="Solo prova", paymentMethod="Contanti", accountId=id
            ))
            val payload = LocalVaultBackup(first).exportEncrypted(password)
            assertFalse(String(payload, Charsets.ISO_8859_1).contains("Conto vero"))
            assertTrue(payload.size > 60)
            val count = LocalVaultBackup(second).restoreIfEmpty(password, payload)
            assertEquals(1, count.accounts)
            assertEquals(1, count.transactions)
            assertEquals("Conto vero", second.accountDao().getAllAccounts().first().single().nome)
            assertEquals(2.35, second.transactionDao().getAllTransactions().first().single().amount, 0.0000001)
        } finally { first.close(); second.close() }
    }

    @Test fun incorrectPassphraseNeverChangesExistingDatabase() = runBlocking {
        val original = db()
        val target = db()
        try {
            original.accountDao().insertAccount(Account(
                nome="Da recuperare", tipo="Contanti", saldo=10.0,
                colore="#123456", icona="€"
            ))
            val vault = LocalVaultBackup(original).exportEncrypted(password)
            assertTrue(runCatching { LocalVaultBackup(target).restoreIfEmpty("wrong-passphrase-2026".toCharArray(), vault) }.isFailure)
            assertTrue(target.accountDao().getAllAccounts().first().isEmpty())
        } finally { original.close(); target.close() }
    }

    @Test fun existingUserDataCannotBeOverwrittenByRestore() = runBlocking {
        val original = db()
        val target = db()
        try {
            original.accountDao().insertAccount(Account(
                nome="Da recuperare", tipo="Contanti", saldo=10.0,
                colore="#123456", icona="€"
            ))
            val encrypted = LocalVaultBackup(original).exportEncrypted(password)
            target.accountDao().insertAccount(Account(
                nome="Non sovrascrivere", tipo="Carta", saldo=77.0,
                colore="#000000", icona="C"
            ))
            assertTrue(runCatching { LocalVaultBackup(target).restoreIfEmpty(password, encrypted) }.isFailure)
            assertEquals("Non sovrascrivere", target.accountDao().getAllAccounts().first().single().nome)
            assertEquals(77.0, target.accountDao().getAllAccounts().first().single().saldo, 0.0000001)
        } finally { original.close(); target.close() }
    }

    @Test fun rejectsOversizeInputBeforeDecryption() {
        assertTrue(runCatching { LocalVaultBackup.readLimited(ByteArrayInputStream(ByteArray(20)), 12) }.isFailure)
    }
}
