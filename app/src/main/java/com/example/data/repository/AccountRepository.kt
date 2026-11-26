package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.TransactionDao
import com.example.domain.model.Account
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.domain.model.MoneyAmount
import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction
import com.example.data.local.AppDatabase

class AccountRepository(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) {
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    fun getAccountById(id: Int): Flow<Account?> {
        return accountDao.getAccountById(id)
    }

    suspend fun insertAccount(account: Account): Long {
        require(account.id == 0) { "Non sovrascrivere un conto esistente" }
        require(account.nome.isNotBlank()) { "Il conto deve avere un nome" }
        val startingCents = MoneyAmount.cents(account.saldo)
        return accountDao.insertAccount(account.copy(saldo = MoneyAmount.euros(startingCents)))
    }

    suspend fun updateAccount(account: Account) {
        database.withTransaction {
            val existing = requireNotNull(accountDao.getAccountByIdSync(account.id)) { "Conto inesistente" }
            require(MoneyAmount.cents(existing.saldo) == MoneyAmount.cents(account.saldo)) {
                "Il saldo non può essere modificato direttamente senza una registrazione contabile"
            }
            accountDao.updateAccount(account.copy(saldo = existing.saldo))
        }
    }

    suspend fun deleteAccount(id: Int) {
        database.withTransaction {
            require(transactionDao.countTransactionsForAccount(id) == 0) {
                "Impossibile eliminare un conto con movimenti. Spostali solo con una procedura verificata."
            }
            accountDao.deleteAccountById(id)
        }
    }

    suspend fun transfer(fromAccountId: Int, toAccountId: Int, amount: Double) {
        require(fromAccountId > 0 && toAccountId > 0 && fromAccountId != toAccountId) {
            "Seleziona due conti reali distinti"
        }
        val amountCents = MoneyAmount.cents(amount)
        require(amountCents > 0L) { "Importo trasferimento non valido" }
        database.withTransaction {
            val from = requireNotNull(accountDao.getAccountByIdSync(fromAccountId)) { "Conto origine inesistente" }
            val to = requireNotNull(accountDao.getAccountByIdSync(toAccountId)) { "Conto destinazione inesistente" }
            val fromCents = MoneyAmount.cents(from.saldo)
            require(fromCents >= amountCents) { "Saldo insufficiente per il trasferimento" }
            val newFromBalance = MoneyAmount.euros(Math.subtractExact(fromCents, amountCents))
            val newToBalance = MoneyAmount.applyDelta(to.saldo, amountCents)
            val now = System.currentTimeMillis()
            accountDao.updateAccount(from.copy(saldo = newFromBalance, aggiornatoIl = now))
            accountDao.updateAccount(to.copy(saldo = newToBalance, aggiornatoIl = now))
            val canonicalAmount = MoneyAmount.euros(amountCents)
            transactionDao.insertTransaction(
                Transaction(amount = canonicalAmount, type = TransactionType.EXPENSE,
                    category = "Other", dateMillis = now, note = "Trasferimento a ${to.nome}",
                    paymentMethod = "Transfer", accountId = fromAccountId)
            )
            transactionDao.insertTransaction(
                Transaction(amount = canonicalAmount, type = TransactionType.INCOME,
                    category = "Other", dateMillis = now, note = "Trasferimento da ${from.nome}",
                    paymentMethod = "Transfer", accountId = toAccountId)
            )
        }
    }
}
