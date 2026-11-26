package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.RecurringTransactionRepository
import com.example.domain.model.RecurringTransaction
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecurringTransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RecurringTransactionRepository
    val recurringTransactions: StateFlow<List<RecurringTransaction>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RecurringTransactionRepository(database.recurringTransactionDao())

        recurringTransactions = repository.allRecurring.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addRecurring(
        note: String,
        amount: Double,
        category: String,
        type: TransactionType,
        paymentMethod: String,
        accountId: Int? = null,
        frequency: String
    ) {
        viewModelScope.launch {
            val recurring = RecurringTransaction(
                note = note,
                amount = amount,
                category = category,
                type = type,
                paymentMethod = paymentMethod,
                accountId = accountId,
                frequency = frequency,
                enabled = true,
                lastExecutedMillis = 0,
                createdAtMillis = System.currentTimeMillis()
            )
            repository.insertRecurring(recurring)
        }
    }

    fun updateRecurring(recurring: RecurringTransaction) {
        viewModelScope.launch {
            repository.updateRecurring(recurring)
        }
    }

    fun toggleRecurring(recurring: RecurringTransaction, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateRecurring(recurring.copy(enabled = isEnabled))
        }
    }

    fun deleteRecurring(id: Int) {
        viewModelScope.launch {
            repository.deleteRecurring(id)
        }
    }
}
