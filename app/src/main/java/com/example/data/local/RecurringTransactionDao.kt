package com.example.data.local

import androidx.room.*
import com.example.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY createdAtMillis DESC")
    fun getAllRecurring(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    fun getRecurringById(id: Int): Flow<RecurringTransaction?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransaction): Long

    @Update
    suspend fun updateRecurring(recurring: RecurringTransaction)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringById(id: Int)

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearAllRecurring()
}
