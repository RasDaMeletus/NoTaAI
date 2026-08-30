package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncOperation {
    INSERT,
    UPDATE,
    DELETE
}

enum class SyncQueueStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    val entityType: String, // "TRANSACTION", "GOAL", "WALLET", "BUDGET"
    val entityId: String,
    val operation: SyncOperation,
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: SyncQueueStatus = SyncQueueStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val idempotencyKey: String = "${entityType}_${entityId}_${System.currentTimeMillis()}"
)
