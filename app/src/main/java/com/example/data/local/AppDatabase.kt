package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.domain.model.AiUsageLog
import com.example.domain.model.Budget
import com.example.domain.model.SplitBill
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.domain.model.SavingsGoal
import com.example.domain.model.SavingsDeposit

import com.example.domain.model.Account
import com.example.domain.model.TransactionRule
import com.example.domain.model.RecurringTransaction
import com.example.domain.model.Achievement
import com.example.domain.model.TimelineEvent
import com.example.data.local.RecurringTransactionDao
import com.example.data.local.AchievementDao
import com.example.data.local.TimelineEventDao

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }
    }
}

@Database(
    entities = [
        Transaction::class,
        SplitBill::class,
        Budget::class,
        AiUsageLog::class,
        SavingsGoal::class,
        SavingsDeposit::class,
        Account::class,
        TransactionRule::class,
        RecurringTransaction::class,
        Achievement::class,
        TimelineEvent::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun splitBillDao(): SplitBillDao
    abstract fun budgetDao(): BudgetDao
    abstract fun aiUsageLogDao(): AiUsageLogDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun savingsDepositDao(): SavingsDepositDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionRuleDao(): TransactionRuleDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun timelineEventDao(): TimelineEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cashmate_database"
                )
                // No destructive fallback: older unknown schemas must fail safely until explicit migrations exist.
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
