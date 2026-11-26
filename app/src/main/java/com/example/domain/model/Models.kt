package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME, EXPENSE
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val dateMillis: Long,
    val note: String,
    val paymentMethod: String,
    val accountId: Int? = null,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long = 0L
)

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val tipo: String, // Conto Corrente, Carta di Credito, Carta di Debito, Contanti, PayPal, Revolut, Conto Risparmio, Altro
    val saldo: Double,
    val colore: String, // Hex string
    val icona: String, // Emoji
    val ordine: Int = 0,
    val visibile: Boolean = true,
    val creatoIl: Long = System.currentTimeMillis(),
    val aggiornatoIl: Long = System.currentTimeMillis(),
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long = 0L
)

@Entity(tableName = "split_bills")
data class SplitBill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val totalAmount: Double,
    val divisionType: String, // EQUAL, PERCENT, EXACT
    val dateMillis: Long = System.currentTimeMillis(),
    val participantsJson: String, // Serialized list of participants
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long = 0L
)

data class SplitParticipant(
    val name: String,
    val value: Double, // can be percentage or exact share or equal weight
    val shareAmount: Double // final calculated amount they owe
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String,
    val limitAmount: Double
)

@Entity(tableName = "ai_usage_logs")
data class AiUsageLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val tokensEstimated: Int
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val emoji: String,
    val targetDateMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val description: String = "",
    val status: String = "ACTIVE", // ACTIVE, ARCHIVED, COMPLETED
    val color: String = "#00E676",
    val notes: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long = 0L
)

@Entity(tableName = "savings_deposits")
data class SavingsDeposit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val transactionId: Int? = null
)

@Entity(tableName = "transaction_rules")
data class TransactionRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchantName: String,
    val containsText: String,
    val categoryId: String, // String mapping to Transaction's category field
    val accountId: Int? = null,
    val priority: Int = 0,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val userId: String = "",
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long = 0L
)

data class UserProfile(
    val id: String = "",
    val displayName: String = "Utente CashMate",
    val email: String = "",
    val photoUrl: String? = null,
    val provider: String = "email",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val premiumPlan: String = "FREE",
    val currency: String = "€",
    val language: String = "it",
    val isOnboardingCompleted: Boolean = false,
    val isPremium: Boolean = false
)

data class AiCoachResponse(
    val summary: String,
    val topRiskCategory: String,
    val savingTips: List<String>,
    val budgetForecast: String,
    val motivationMessage: String
)
