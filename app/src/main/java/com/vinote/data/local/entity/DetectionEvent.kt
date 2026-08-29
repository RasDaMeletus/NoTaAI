package com.vinote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Raw notification detection event.
 * Retained for reviewable detections and debugging.
 * Minimized per PRD: do not retain raw notification content longer than necessary.
 */
@Entity(
    tableName = "detection_events",
    indices = [
        Index(value = ["userId"], name = "idx_detection_events_userId"),
        Index(value = ["fingerprint"], name = "idx_detection_events_fingerprint", unique = true)
    ]
)
data class DetectionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    val packageName: String,       // Source app package name
    val provider: String,          // "gopay", "dana", etc.
    val title: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fingerprint: String,       // Stable dedup fingerprint
    val confidence: Float,         // 0.0 to 1.0
    val result: String,            // "AUTO_RECORDED", "PENDING_REVIEW", "IGNORED", "DUPLICATE"
    val transactionId: Long? = null, // FK to TransactionEntity if recorded
    val createdAt: Long = System.currentTimeMillis()
)
