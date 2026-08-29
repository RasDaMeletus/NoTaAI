package com.vinote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A NoTa AI conversation session.
 */
@Entity(
    tableName = "ai_conversations",
    indices = [Index(value = ["userId"], name = "idx_ai_conversations_userId")]
)
data class AiConversation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    val title: String = "New conversation",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
