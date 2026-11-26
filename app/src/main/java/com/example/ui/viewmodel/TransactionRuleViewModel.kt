package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.TransactionRuleRepository
import com.example.domain.model.TransactionRule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionRuleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRuleRepository
    val rules: StateFlow<List<TransactionRule>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRuleRepository(database.transactionRuleDao())

        rules = repository.allRules.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addRule(
        merchantName: String,
        containsText: String,
        categoryId: String,
        accountId: Int? = null,
        priority: Int = 0,
        enabled: Boolean = true
    ) {
        viewModelScope.launch {
            val rule = TransactionRule(
                merchantName = merchantName,
                containsText = containsText,
                categoryId = categoryId,
                accountId = accountId,
                priority = priority,
                enabled = enabled,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertRule(rule)
        }
    }

    fun updateRule(rule: TransactionRule) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun toggleRule(rule: TransactionRule, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(enabled = isEnabled, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteRule(id: Int) {
        viewModelScope.launch {
            repository.deleteRule(id)
        }
    }
}
