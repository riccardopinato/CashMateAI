package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timeline_events")
data class TimelineEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val type: String, // STIPENDIO, GOAL_COMPLETE, BUDGET_REACHED, NEW_WALLET, PREMIUM_ACTIVE, SAVINGS_RECORD
    val icon: String, // Emoji
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long = 0L
)
