package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.TransactionDao
import com.example.domain.model.Account
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AccountRepository(
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
        accountDao.deleteAccountById(id)
    }

    suspend fun transfer(fromAccountId: Int, toAccountId: Int, amount: Double) {
        val fromAccount = accountDao.getAccountByIdSync(fromAccountId)
        val toAccount = accountDao.getAccountByIdSync(toAccountId)
        if (fromAccount != null && toAccount != null && fromAccount.saldo >= amount) {
            // Deduct from sender
            val updatedFrom = fromAccount.copy(
                saldo = fromAccount.saldo - amount,
                aggiornatoIl = System.currentTimeMillis()
            )
            accountDao.updateAccount(updatedFrom)

            // Add to receiver
            val updatedTo = toAccount.copy(
                saldo = toAccount.saldo + amount,
                aggiornatoIl = System.currentTimeMillis()
            )
            accountDao.updateAccount(updatedTo)

            // Insert matching history transactions
            transactionDao.insertTransaction(
                Transaction(
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = "Other",
                    dateMillis = System.currentTimeMillis(),
                    note = "Trasferimento a ${toAccount.nome}",
                    paymentMethod = "Transfer",
                    accountId = fromAccountId
                )
            )

            transactionDao.insertTransaction(
                Transaction(
                    amount = amount,
                    type = TransactionType.INCOME,
                    category = "Other",
                    dateMillis = System.currentTimeMillis(),
                    note = "Trasferimento da ${fromAccount.nome}",
                    paymentMethod = "Transfer",
                    accountId = toAccountId
                )
            )
        }
    }
}
