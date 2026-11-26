package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val note: String,
    val amount: Double,
    val category: String,
    val type: TransactionType,
    val paymentMethod: String,
    val accountId: Int? = null,
    val frequency: String, // Giornaliera, Settimanale, Mensile, Annuale, Personalizzata
    val enabled: Boolean = true,
    val lastExecutedMillis: Long = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long = 0L
)
