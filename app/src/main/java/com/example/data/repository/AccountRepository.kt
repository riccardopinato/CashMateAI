package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.TransactionDao
import com.example.domain.model.Account
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
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
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
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
        require(amount.isFinite() && amount > 0.0) { "Importo trasferimento non valido" }
        database.withTransaction {
            val from = requireNotNull(accountDao.getAccountByIdSync(fromAccountId)) { "Conto origine inesistente" }
            val to = requireNotNull(accountDao.getAccountByIdSync(toAccountId)) { "Conto destinazione inesistente" }
            require(from.saldo >= amount) { "Saldo insufficiente per il trasferimento" }
            val now = System.currentTimeMillis()
            accountDao.updateAccount(from.copy(saldo = from.saldo - amount, aggiornatoIl = now))
            accountDao.updateAccount(to.copy(saldo = to.saldo + amount, aggiornatoIl = now))
            transactionDao.insertTransaction(
                Transaction(amount = amount, type = TransactionType.EXPENSE,
                    category = "Other", dateMillis = now, note = "Trasferimento a ${to.nome}",
                    paymentMethod = "Transfer", accountId = fromAccountId)
            )
            transactionDao.insertTransaction(
                Transaction(amount = amount, type = TransactionType.INCOME,
                    category = "Other", dateMillis = now, note = "Trasferimento da ${from.nome}",
                    paymentMethod = "Transfer", accountId = toAccountId)
            )
        }
    }
}
