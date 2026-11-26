package com.example.data.repository

import com.example.data.local.TransactionRuleDao
import com.example.domain.model.TransactionRule
import kotlinx.coroutines.flow.Flow

class TransactionRuleRepository(private val ruleDao: TransactionRuleDao) {
    val allRules: Flow<List<TransactionRule>> = ruleDao.getAllRules()

    fun getRuleById(id: Int): Flow<TransactionRule?> {
        return ruleDao.getRuleById(id)
    }

    suspend fun insertRule(rule: TransactionRule): Long {
        return ruleDao.insertRule(rule)
    }

    suspend fun updateRule(rule: TransactionRule) {
        ruleDao.updateRule(rule)
    }

    suspend fun deleteRule(id: Int) {
        ruleDao.deleteRuleById(id)
    }
}
