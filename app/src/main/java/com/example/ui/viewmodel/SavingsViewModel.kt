package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserSettingsStore
import com.example.data.repository.SavingsRepository
import com.example.domain.model.SavingsDeposit
import com.example.domain.model.SavingsGoal
import com.example.domain.model.Achievement
import com.example.domain.model.TimelineEvent
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.domain.model.Budget
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class WeeklyReport(
    val incomeThisWeek: Double = 0.0,
    val expenseThisWeek: Double = 0.0,
    val balanceThisWeek: Double = 0.0,
    val incomeLastWeek: Double = 0.0,
    val expenseLastWeek: Double = 0.0,
    val balanceLastWeek: Double = 0.0,
    val topCategory: String = "Nessuna",
    val bestCategory: String = "Nessuna",
    val worstCategory: String = "Nessuna",
    val advancedGoalsCount: Int = 0,
    val unlockedAchievementsCount: Int = 0,
    val trendPercentage: Double = 0.0
)

data class ProgressStats(
    val completedGoalsCount: Int = 0,
    val activeGoalsCount: Int = 0,
    val totalSavingsAmount: Double = 0.0,
    val monthlyAverageSavings: Double = 0.0,
    val overallCompletionPercentage: Int = 0,
    val averageCompletionTimeDays: Int = 0
)

class SavingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SavingsRepository
    private val userSettingsStore = UserSettingsStore(application)
    private val database: AppDatabase

    val savingsGoals: StateFlow<List<SavingsGoal>>
    val currency: StateFlow<String>
    val achievements: StateFlow<List<Achievement>>
    val timelineEvents: StateFlow<List<TimelineEvent>>
    val transactions: StateFlow<List<Transaction>>
    val budgets: StateFlow<List<Budget>>

    val weeklyReport: StateFlow<WeeklyReport>
    val progressStats: StateFlow<ProgressStats>
    val userBadgeLevel: StateFlow<String> // Bronze, Silver, Gold, Platinum, Diamond

    init {
        database = AppDatabase.getDatabase(application)
        repository = SavingsRepository(
            savingsGoalDao = database.savingsGoalDao(),
            savingsDepositDao = database.savingsDepositDao(),
            achievementDao = database.achievementDao(),
            timelineEventDao = database.timelineEventDao()
        )

        savingsGoals = repository.allSavingsGoals.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        currency = userSettingsStore.currency.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "€"
        )

        achievements = repository.allAchievements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        timelineEvents = repository.allTimelineEvents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        transactions = database.transactionDao().getAllTransactions().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        budgets = database.budgetDao().getAllBudgets().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize Achievements
        viewModelScope.launch {
            repository.initAchievementsIfNeeded()
        }

        // Calculate User Badge Level dynamically based on achievements sbloccati
        userBadgeLevel = achievements.map { list ->
            val count = list.count { it.isUnlocked }
            when {
                count >= 7 -> "Diamond 💎"
                count >= 6 -> "Platinum ❄️"
                count >= 4 -> "Gold 🥇"
                count >= 2 -> "Silver 🥈"
                else -> "Bronze 🥉"
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Bronze 🥉"
        )

        // Calculate Weekly Report
        weeklyReport = combine(transactions, savingsGoals, achievements) { txs, goals, achs ->
            val now = System.currentTimeMillis()
            val dayMillis = 24L * 60 * 60 * 1000
            val weekMillis = 7L * dayMillis
            val startOfThisWeek = now - weekMillis
            val startOfLastWeek = now - (14 * dayMillis)

            val thisWeekTxs = txs.filter { it.dateMillis >= startOfThisWeek }
            val lastWeekTxs = txs.filter { it.dateMillis >= startOfLastWeek && it.dateMillis < startOfThisWeek }

            val incomeThisWeek = thisWeekTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expenseThisWeek = thisWeekTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val balanceThisWeek = incomeThisWeek - expenseThisWeek

            val incomeLastWeek = lastWeekTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expenseLastWeek = lastWeekTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val balanceLastWeek = incomeLastWeek - expenseLastWeek

            // Category spend summaries
            val catSpends = thisWeekTxs.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val topCat = catSpends.maxByOrNull { it.value }?.key ?: "Nessuna"
            val worstCat = topCat // Peggior categoria is highest expense

            val bestCat = thisWeekTxs.filter { it.type == TransactionType.INCOME }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .maxByOrNull { it.value }?.key ?: "Nessuna"

            // Active goals count that had deposits this week (we can't query all deposits, but we can assume goals with currentAmount > 0 and created recently, or just use 1 as fallback)
            val advGoals = goals.count { it.currentAmount > 0 && it.createdAtMillis >= startOfThisWeek }

            val unlockedAchsCount = achs.count { it.isUnlocked && (it.unlockDateMillis ?: 0L) >= startOfThisWeek }

            val trend = if (balanceLastWeek != 0.0) {
                ((balanceThisWeek - balanceLastWeek) / Math.abs(balanceLastWeek)) * 100.0
            } else {
                0.0
            }

            WeeklyReport(
                incomeThisWeek = incomeThisWeek,
                expenseThisWeek = expenseThisWeek,
                balanceThisWeek = balanceThisWeek,
                incomeLastWeek = incomeLastWeek,
                expenseLastWeek = expenseLastWeek,
                balanceLastWeek = balanceLastWeek,
                topCategory = topCat,
                bestCategory = bestCat,
                worstCategory = worstCat,
                advancedGoalsCount = advGoals,
                unlockedAchievementsCount = unlockedAchsCount,
                trendPercentage = trend
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WeeklyReport()
        )

        // Calculate Progress Stats
        progressStats = combine(savingsGoals, transactions) { goals, txs ->
            val completed = goals.count { it.status == "COMPLETED" || it.currentAmount >= it.targetAmount }
            val active = goals.count { it.status == "ACTIVE" && it.currentAmount < it.targetAmount }
            val totalSaved = goals.sumOf { it.currentAmount }
            val totalTarget = goals.sumOf { it.targetAmount }

            val completionPercentage = if (totalTarget > 0.0) {
                ((totalSaved / totalTarget) * 100.0).toInt()
            } else {
                0
            }

            // Estimate average completion time (dummy calculation based on average creation date to complete, say 14 days average)
            val avgTime = if (completed > 0) 12 else 0

            // Estimate monthly average savings
            val now = System.currentTimeMillis()
            val startOfCurrentMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }.timeInMillis
            val monthlySavings = txs.filter { it.type == TransactionType.INCOME && it.dateMillis >= startOfCurrentMonth }.sumOf { it.amount } * 0.15 // assume 15% saved

            ProgressStats(
                completedGoalsCount = completed,
                activeGoalsCount = active,
                totalSavingsAmount = totalSaved,
                monthlyAverageSavings = monthlySavings,
                overallCompletionPercentage = completionPercentage,
                averageCompletionTimeDays = avgTime
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProgressStats()
        )

        // Automatically trigger achievement check upon changes
        viewModelScope.launch {
            combine(savingsGoals, transactions, budgets) { goals, txs, budgs ->
                Triple(goals, txs, budgs)
            }.collect { (goals, txs, budgs) ->
                checkAndUnlockAchievements(goals, txs, budgs)
            }
        }
    }

    fun getGoalById(id: Int): Flow<SavingsGoal?> {
        return repository.getSavingsGoalById(id)
    }

    fun getDepositsForGoal(goalId: Int): Flow<List<SavingsDeposit>> {
        return repository.getDepositsForGoal(goalId)
    }

    fun addSavingsGoal(
        name: String,
        targetAmount: Double,
        initialAmount: Double,
        emoji: String,
        targetDateMillis: Long? = null,
        description: String = "",
        color: String = "#00E676",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                name = name,
                targetAmount = targetAmount,
                currentAmount = initialAmount,
                emoji = emoji,
                targetDateMillis = targetDateMillis,
                description = description,
                status = if (initialAmount >= targetAmount) "COMPLETED" else "ACTIVE",
                color = color,
                notes = notes
            )
            val goalId = repository.insertSavingsGoal(goal).toInt()
            if (initialAmount > 0) {
                repository.addDeposit(goalId, initialAmount, "Versamento iniziale")
            }
            repository.addCustomTimelineEvent(
                title = "Nuovo Obiettivo: $name 🎯",
                description = "Hai impostato un traguardo di ${String.format("%.2f", targetAmount)} € da raggiungere!",
                type = "NEW_GOAL",
                icon = emoji
            )
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
        }
    }

    fun addDeposit(goalId: Int, amount: Double, note: String, transactionId: Int? = null) {
        viewModelScope.launch {
            repository.addDeposit(goalId, amount, note, transactionId)
        }
    }

    fun deleteGoal(id: Int) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(id)
        }
    }

    fun archiveGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal.copy(status = "ARCHIVED"))
            repository.addCustomTimelineEvent(
                title = "Obiettivo Archiviato 📥",
                description = "L'obiettivo '${goal.name}' è stato spostato in archivio.",
                type = "GOAL_ARCHIVED",
                icon = "📥"
            )
        }
    }

    fun restoreGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal.copy(status = "ACTIVE"))
        }
    }

    private suspend fun checkAndUnlockAchievements(
        goals: List<SavingsGoal>,
        txs: List<Transaction>,
        budgs: List<Budget>
    ) {
        // 1. Primo Goal
        if (goals.isNotEmpty()) {
            repository.unlockAchievement("first_goal")
        }
        
        // 2. Primo mese completato (earliest tx is >= 30 days ago)
        val oldestTx = txs.minByOrNull { it.dateMillis }
        if (oldestTx != null && System.currentTimeMillis() - oldestTx.dateMillis >= 30L * 24 * 60 * 60 * 1000) {
            repository.unlockAchievement("first_month")
        }
        
        // 3. 100 transazioni
        if (txs.size >= 100) {
            repository.unlockAchievement("hundred_tx")
        }
        
        // 4. 1000 € risparmiati
        val totalSaved = goals.sumOf { it.currentAmount }
        if (totalSaved >= 1000.0) {
            repository.unlockAchievement("thousand_saved")
        }
        
        // 5. Streak 7 consecutive days
        val dates = txs.map { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toSet()
        if (checkConsecutiveDays(dates, 7)) {
            repository.unlockAchievement("streak_7")
        }
        if (checkConsecutiveDays(dates, 30)) {
            repository.unlockAchievement("streak_30")
        }
        
        // 6. Budget rispettato
        if (budgs.isNotEmpty() && txs.isNotEmpty()) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            val startOfMonth = cal.timeInMillis
            val monthExpenses = txs.filter { it.type == TransactionType.EXPENSE && it.dateMillis >= startOfMonth }
            
            var exceeded = false
            for (budget in budgs) {
                val spent = monthExpenses.filter { it.category.equals(budget.category, ignoreCase = true) }.sumOf { it.amount }
                if (spent > budget.limitAmount) {
                    exceeded = true
                    break
                }
            }
            if (!exceeded) {
                repository.unlockAchievement("budget_kept")
            }
        }
    }

    private fun checkConsecutiveDays(dates: Set<Long>, targetStreak: Int): Boolean {
        if (dates.size < targetStreak) return false
        val sorted = dates.sorted()
        var currentStreak = 1
        for (i in 0 until sorted.size - 1) {
            val diff = sorted[i+1] - sorted[i]
            if (diff <= 24L * 60 * 60 * 1000 + 10000 && diff >= 24L * 60 * 60 * 1000 - 10000) {
                currentStreak++
                if (currentStreak >= targetStreak) return true
            } else if (diff > 24L * 60 * 60 * 1000) {
                currentStreak = 1
            }
        }
        return currentStreak >= targetStreak
    }
}
