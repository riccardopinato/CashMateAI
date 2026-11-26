package com.example.data.local

import androidx.room.*
import com.example.domain.model.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)

    @Query("UPDATE achievements SET isUnlocked = :isUnlocked, unlockDateMillis = :unlockDateMillis WHERE id = :id")
    suspend fun updateUnlockStatus(id: String, isUnlocked: Boolean, unlockDateMillis: Long?)

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    suspend fun getUnlockedCount(): Int
}
