package com.vinote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks the sync state for each entity that needs cloud synchronization.
 * Used for the sync queue: LOCAL_ONLY → SYNCING → SYNCED (or SYNC_ERROR).
 */
@Entity(
    tableName = "sync_metadata",
    indices = [Index(value = ["entityType", "entityId"], name = "idx_sync_metadata_entity", unique = true)]
)
data class SyncMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val entityType: String,        // "transaction", "wallet", "goal", "budget"
    val entityId: Long,
    val operation: String,         // "INSERT", "UPDATE", "DELETE"
    val state: SyncState = SyncState.LOCAL_ONLY,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
