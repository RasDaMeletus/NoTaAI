package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DetectionStatus {
    AUTO_RECORDED,
    PENDING_REVIEW,
    REJECTED,
    DISMISSED,
    PROCESSED
}

@Entity(tableName = "detection_events")
data class DetectionEventEntity(
    @PrimaryKey
    val id: String, // Unique event ID
    val userId: String = "user_default",
    val provider: String, // e.g. "DANA", "GoPay", "BCA"
    val packageName: String,
    val rawTitle: String,
    val rawText: String,
    val rawSnippet: String,
    val amount: Long,
    val type: String = "EXPENSE",
    val merchant: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val fingerprint: String,
    val status: DetectionStatus = DetectionStatus.AUTO_RECORDED,
    val associatedTransactionId: Long? = null
)
