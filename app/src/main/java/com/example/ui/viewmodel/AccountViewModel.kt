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
            database = database,
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

        // User-owned accounts must never be seeded, deleted or reassigned on startup.
        // Historical orphans require a separately reviewed migration and user confirmation.
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
            runCatching { repository.deleteAccount(id) }
                .onFailure {
                    android.widget.Toast.makeText(getApplication(), it.message ?: "Conto non eliminato", android.widget.Toast.LENGTH_LONG).show()
                }
        }
    }

    fun transfer(fromAccountId: Int, toAccountId: Int, amount: Double) {
        viewModelScope.launch {
            runCatching { repository.transfer(fromAccountId, toAccountId, amount) }
                .onFailure {
                    android.widget.Toast.makeText(getApplication(), it.message ?: "Trasferimento non eseguito", android.widget.Toast.LENGTH_LONG).show()
                }
        }
    }
}
