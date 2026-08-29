package com.vinote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Individual message in a NoTa AI conversation.
 */
@Entity(
    tableName = "ai_messages",
    indices = [Index(value = ["conversationId"], name = "idx_ai_messages_conversationId")]
)
data class AiMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val conversationId: Long,
    val role: String,              // "user" or "assistant"
    val content: String,
    val toolCalls: String? = null, // JSON array of tool calls if any
    val timestamp: Long = System.currentTimeMillis()
)
