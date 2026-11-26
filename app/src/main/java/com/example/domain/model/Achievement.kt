package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String, // "first_goal", "first_month", "hundred_tx", "thousand_saved", "streak_7", "streak_30", "budget_kept"
    val title: String,
    val description: String,
    val icon: String, // Emoji
    val isUnlocked: Boolean = false,
    val unlockDateMillis: Long? = null
)
