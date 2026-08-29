package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vinote.data.local.entity.SyncMetadata
import com.vinote.data.local.entity.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE state = :state ORDER BY createdAt ASC")
    fun getByState(state: SyncState): Flow<List<SyncMetadata>>

    @Query("SELECT * FROM sync_metadata WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun getByEntity(entityType: String, entityId: Long): SyncMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SyncMetadata): Long

    @Update
    suspend fun update(metadata: SyncMetadata)

    @Query("UPDATE sync_metadata SET state = :newState, updatedAt = :now WHERE id = :id")
    suspend fun updateState(id: Long, newState: SyncState, now: Long = System.currentTimeMillis())
}
