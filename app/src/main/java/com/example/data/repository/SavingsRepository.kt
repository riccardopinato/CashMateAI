package com.example.data.repository

import com.example.data.local.SavingsDepositDao
import com.example.data.local.SavingsGoalDao
import com.example.data.local.AchievementDao
import com.example.data.local.TimelineEventDao
import com.example.domain.model.SavingsDeposit
import com.example.domain.model.SavingsGoal
import com.example.domain.model.Achievement
import com.example.domain.model.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class SavingsRepository(
    private val savingsGoalDao: SavingsGoalDao,
    private val savingsDepositDao: SavingsDepositDao,
    private val achievementDao: AchievementDao,
    private val timelineEventDao: TimelineEventDao
) {
    val allSavingsGoals: Flow<List<SavingsGoal>> = savingsGoalDao.getAllSavingsGoals()
    val allAchievements: Flow<List<Achievement>> = achievementDao.getAllAchievements()
    val allTimelineEvents: Flow<List<TimelineEvent>> = timelineEventDao.getAllTimelineEvents()

    fun getSavingsGoalById(id: Int): Flow<SavingsGoal?> {
        return savingsGoalDao.getSavingsGoalById(id)
    }

    fun getDepositsForGoal(goalId: Int): Flow<List<SavingsDeposit>> {
        return savingsDepositDao.getDepositsForGoal(goalId)
    }

    suspend fun insertSavingsGoal(goal: SavingsGoal): Long {
        val id = savingsGoalDao.insertSavingsGoal(goal)
        // Check if first goal achievement can be unlocked
        unlockAchievement("first_goal")
        return id
    }

    suspend fun updateSavingsGoal(goal: SavingsGoal) {
        savingsGoalDao.insertSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(id: Int) {
        savingsGoalDao.deleteSavingsGoalById(id)
        savingsDepositDao.deleteDepositsForGoal(id)
    }

    suspend fun addDeposit(goalId: Int, amount: Double, note: String, transactionId: Int? = null): Long {
        val depositId = savingsDepositDao.insertDeposit(
            SavingsDeposit(goalId = goalId, amount = amount, note = note, transactionId = transactionId)
        )
        val goal = savingsGoalDao.getSavingsGoalById(goalId).firstOrNull()
        if (goal != null) {
            val wasCompleted = goal.currentAmount >= goal.targetAmount
            val newAmount = goal.currentAmount + amount
            val isNowCompleted = newAmount >= goal.targetAmount
            
            val updatedGoal = goal.copy(
                currentAmount = newAmount,
                status = if (isNowCompleted) "COMPLETED" else goal.status
            )
            savingsGoalDao.insertSavingsGoal(updatedGoal)

            // Add timeline event if completed
            if (isNowCompleted && !wasCompleted) {
                timelineEventDao.insertEvent(
                    TimelineEvent(
                        title = "Obiettivo Raggiunto! 🎉",
                        description = "Hai completato con successo l'obiettivo '${goal.name}' accumulando ${String.format("%.2f", goal.targetAmount)} €!",
                        type = "GOAL_COMPLETE",
                        icon = goal.emoji
                    )
                )
                unlockAchievement("first_goal")
            }

            // Check if total saved is >= 1000
            val allGoals = savingsGoalDao.getAllSavingsGoals().firstOrNull() ?: emptyList()
            val totalSaved = allGoals.sumOf { it.currentAmount }
            if (totalSaved >= 1000.0) {
                unlockAchievement("thousand_saved")
            }
        }
        return depositId
    }

    suspend fun initAchievementsIfNeeded() {
        val defaultAchievements = listOf(
            Achievement("first_goal", "Primo Obiettivo", "Crea il tuo primo obiettivo di risparmio per iniziare il percorso.", "🎯"),
            Achievement("first_month", "Stabilità", "Completa il tuo primo mese di tracciamento finanziario.", "📅"),
            Achievement("hundred_tx", "Centurione", "Registra almeno 100 transazioni per un'analisi approfondita.", "💯"),
            Achievement("thousand_saved", "Grande Risparmiatore", "Risparmia un totale di almeno 1000 € nei tuoi obiettivi.", "💰"),
            Achievement("streak_7", "Costanza Settimanale", "Registra transazioni o risparmi per 7 giorni consecutivi.", "🔥"),
            Achievement("streak_30", "Costanza Mensile", "Mantieni il tracciamento attivo per 30 giorni consecutivi.", "🏆"),
            Achievement("budget_kept", "Disciplina", "Mantieni le spese mensili sotto il budget prefissato.", "🛡️")
        )
        achievementDao.insertAchievements(defaultAchievements)
    }

    suspend fun unlockAchievement(id: String) {
        achievementDao.updateUnlockStatus(id, true, System.currentTimeMillis())
        
        val achievementName = when(id) {
            "first_goal" -> "Primo Obiettivo 🎯"
            "first_month" -> "Stabilità 📅"
            "hundred_tx" -> "Centurione 💯"
            "thousand_saved" -> "Grande Risparmiatore 💰"
            "streak_7" -> "Costanza Settimanale 🔥"
            "streak_30" -> "Costanza Mensile 🏆"
            "budget_kept" -> "Disciplina 🛡️"
            else -> "Traguardo Raggiunto"
        }
        
        if (timelineEventDao.countEventsOfType("ACHIEVEMENT_$id") == 0) {
            timelineEventDao.insertEvent(
                TimelineEvent(
                    title = "Traguardo Sbloccato! $achievementName",
                    description = "Complimenti! Hai sbloccato il trofeo per aver completato questo traguardo.",
                    type = "ACHIEVEMENT_$id",
                    icon = "🏆"
                )
            )
        }
    }

    suspend fun addCustomTimelineEvent(title: String, description: String, type: String, icon: String) {
        timelineEventDao.insertEvent(
            TimelineEvent(
                title = title,
                description = description,
                type = type,
                icon = icon
            )
        )
    }
}
