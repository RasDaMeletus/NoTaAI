package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.DetectionEventEntity
import com.example.data.local.entities.DetectionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionEventDao {
    @Query("SELECT * FROM detection_events WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllDetectionEventsFlow(userId: String): Flow<List<DetectionEventEntity>>

    @Query("SELECT * FROM detection_events WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentDetectionEventsFlow(userId: String, limit: Int = 10): Flow<List<DetectionEventEntity>>

    @Query("SELECT * FROM detection_events WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): DetectionEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DetectionEventEntity)

    @Update
    suspend fun updateEvent(event: DetectionEventEntity)

    @Query("UPDATE detection_events SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: DetectionStatus)

    @Query("DELETE FROM detection_events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM detection_events WHERE userId = :userId")
    suspend fun clearUserEvents(userId: String)
}
