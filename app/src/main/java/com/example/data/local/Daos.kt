package com.example.data.local

import androidx.room.*
import com.example.domain.model.AiUsageLog
import com.example.domain.model.Budget
import com.example.domain.model.SplitBill
import com.example.domain.model.Transaction
import com.example.domain.model.SavingsGoal
import com.example.domain.model.SavingsDeposit
import com.example.domain.model.Account
import com.example.domain.model.TransactionRule
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionByIdSync(id: Int): Transaction?

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun countTransactionsForAccount(accountId: Int): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}

@Dao
interface SplitBillDao {
    @Query("SELECT * FROM split_bills ORDER BY dateMillis DESC")
    fun getAllSplitBills(): Flow<List<SplitBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplitBill(splitBill: SplitBill)

    @Query("DELETE FROM split_bills WHERE id = :id")
    suspend fun deleteSplitBillById(id: Int)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudgetByCategory(category: String)
}

@Dao
interface AiUsageLogDao {
    @Query("SELECT * FROM ai_usage_logs ORDER BY timestamp DESC")
    fun getAllUsageLogs(): Flow<List<AiUsageLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageLog(log: AiUsageLog)

    @Query("SELECT * FROM ai_usage_logs WHERE timestamp >= :timestamp")
    suspend fun getLogsAfterTimestamp(timestamp: Long): List<AiUsageLog>
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY createdAtMillis DESC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun getSavingsGoalById(id: Int): Flow<SavingsGoal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoal): Long

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: Int)
}

@Dao
interface SavingsDepositDao {
    @Query("SELECT * FROM savings_deposits WHERE goalId = :goalId ORDER BY dateMillis DESC")
    fun getDepositsForGoal(goalId: Int): Flow<List<SavingsDeposit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: SavingsDeposit): Long

    @Query("DELETE FROM savings_deposits WHERE id = :id")
    suspend fun deleteDepositById(id: Int)

    @Query("DELETE FROM savings_deposits WHERE goalId = :goalId")
    suspend fun deleteDepositsForGoal(goalId: Int)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY ordine ASC, creatoIl DESC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountById(id: Int): Flow<Account?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountByIdSync(id: Int): Account?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Int)

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()
}

@Dao
interface TransactionRuleDao {
    @Query("SELECT * FROM transaction_rules ORDER BY priority DESC, merchantName ASC")
    fun getAllRules(): Flow<List<TransactionRule>>

    @Query("SELECT * FROM transaction_rules WHERE id = :id")
    fun getRuleById(id: Int): Flow<TransactionRule?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: TransactionRule): Long

    @Update
    suspend fun updateRule(rule: TransactionRule)

    @Query("DELETE FROM transaction_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Int)

    @Query("DELETE FROM transaction_rules")
    suspend fun clearAllRules()
}

