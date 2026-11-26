package com.example.data.repository

import com.example.data.local.RecurringTransactionDao
import com.example.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

class RecurringTransactionRepository(private val dao: RecurringTransactionDao) {
    val allRecurring: Flow<List<RecurringTransaction>> = dao.getAllRecurring()

    fun getRecurringById(id: Int): Flow<RecurringTransaction?> {
        return dao.getRecurringById(id)
    }

    suspend fun insertRecurring(recurring: RecurringTransaction): Long {
        return dao.insertRecurring(recurring)
    }

    suspend fun updateRecurring(recurring: RecurringTransaction) {
        dao.updateRecurring(recurring)
    }

    suspend fun deleteRecurring(id: Int) {
        dao.deleteRecurringById(id)
    }
}
