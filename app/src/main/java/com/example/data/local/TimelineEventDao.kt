package com.example.data.local

import androidx.room.*
import com.example.domain.model.TimelineEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {
    @Query("SELECT * FROM timeline_events ORDER BY dateMillis DESC")
    fun getAllTimelineEvents(): Flow<List<TimelineEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TimelineEvent): Long

    @Query("DELETE FROM timeline_events WHERE id = :id")
    suspend fun deleteEventById(id: Int)

    @Query("SELECT COUNT(*) FROM timeline_events WHERE type = :type")
    suspend fun countEventsOfType(type: String): Int
}
