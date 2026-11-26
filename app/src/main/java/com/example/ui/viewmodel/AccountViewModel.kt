package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserSettingsStore
import com.example.data.repository.AccountRepository
import com.example.domain.model.Account
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AccountRepository
    private val userSettingsStore = UserSettingsStore(application)

    val accounts: StateFlow<List<Account>>
    val currency: StateFlow<String>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AccountRepository(
            accountDao = database.accountDao(),
            transactionDao = database.transactionDao()
        )

        accounts = repository.allAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        currency = userSettingsStore.currency.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "€"
        )

        // Seed default accounts if database is empty, and migrate transactions without account
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(application)
            val accDao = db.accountDao()
            val txDao = db.transactionDao()

            // 1. Clean up invalid accounts (e.g., virtual or fake accounts like "Tutti i conti" or "Tutto")
            val allAccList = accDao.getAllAccounts().first()
            val invalidNames = listOf("tutti i conti", "tutto", "tutti", "all", "all_accounts")
            val invalidAccounts = allAccList.filter { it.nome.trim().lowercase() in invalidNames }
            val invalidIds = invalidAccounts.map { it.id }.toSet()

            invalidAccounts.forEach { acc ->
                accDao.deleteAccountById(acc.id)
            }

            // Check if database is empty
            var current = accDao.getAllAccounts().first()
            if (current.isEmpty()) {
                accDao.insertAccount(
                    Account(
                        nome = "Conto Corrente",
                        tipo = "Conto Corrente",
                        saldo = 2500.0,
                        colore = "#00E676",
                        icona = "🏦",
                        ordine = 1
                    )
                )
                accDao.insertAccount(
                    Account(
                        nome = "Revolut",
                        tipo = "Revolut",
                        saldo = 450.0,
                        colore = "#3B82F6",
                        icona = "💳",
                        ordine = 2
                    )
                )
                accDao.insertAccount(
                    Account(
                        nome = "Contanti",
                        tipo = "Contanti",
                        saldo = 80.0,
                        colore = "#FFC857",
                        icona = "💵",
                        ordine = 3
                    )
                )
                current = accDao.getAllAccounts().first()
            }

            // Perform transaction migration: automatically assign transactions with null/invalid accountId to "Conto Corrente"
            val defaultAccount = current.find { it.nome == "Conto Corrente" } ?: current.firstOrNull()
            if (defaultAccount != null) {
                val allTx = txDao.getAllTransactions().first()
                val validAccountIds = current.map { it.id }.toSet()

                allTx.forEach { tx ->
                    val isOrphan = tx.accountId == null ||
                                   tx.accountId in invalidIds ||
                                   tx.accountId !in validAccountIds

                    if (isOrphan) {
                        txDao.insertTransaction(tx.copy(accountId = defaultAccount.id))
                    }
                }
            }
        }
    }

    fun addAccount(nome: String, tipo: String, saldoIniziale: Double, colore: String, icona: String) {
        viewModelScope.launch {
            val account = Account(
                nome = nome,
                tipo = tipo,
                saldo = saldoIniziale,
                colore = colore,
                icona = icona,
                ordine = accounts.value.size + 1
            )
            repository.insertAccount(account)
        }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch {
            repository.deleteAccount(id)
        }
    }

    fun transfer(fromAccountId: Int, toAccountId: Int, amount: Double) {
        viewModelScope.launch {
            repository.transfer(fromAccountId, toAccountId, amount)
        }
    }
}
