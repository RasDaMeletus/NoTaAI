package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.entities.SyncQueueStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE userId = :userId AND status != 'SYNCED' ORDER BY timestamp ASC")
    fun getPendingQueueFlow(userId: String): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE userId = :userId AND status != 'SYNCED' ORDER BY timestamp ASC")
    suspend fun getPendingQueue(userId: String): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE userId = :userId AND status != 'SYNCED'")
    fun getPendingCountFlow(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SyncQueueStatus)

    @Query("UPDATE sync_queue SET status = :status, retryCount = retryCount + 1, errorMessage = :error WHERE id = :id")
    suspend fun markFailed(id: Long, status: SyncQueueStatus = SyncQueueStatus.FAILED, error: String?)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue WHERE userId = :userId AND status = 'SYNCED'")
    suspend fun clearCompleted(userId: String)

    @Query("DELETE FROM sync_queue WHERE userId = :userId")
    suspend fun clearUserQueue(userId: String)
}
