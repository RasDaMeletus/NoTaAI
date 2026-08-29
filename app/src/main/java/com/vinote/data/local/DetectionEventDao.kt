package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vinote.data.local.entity.DetectionEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionEventDao {
    @Query("SELECT * FROM detection_events WHERE userId = :userId ORDER BY timestamp DESC")
    fun getEventsByUser(userId: String): Flow<List<DetectionEvent>>

    @Query("SELECT * FROM detection_events WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getByFingerprint(fingerprint: String): DetectionEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: DetectionEvent): Long

    @Query("DELETE FROM detection_events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
