package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vinote.data.local.entity.AiConversation
import com.vinote.data.local.entity.AiMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface AiConversationDao {
    @Query("SELECT * FROM ai_conversations WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getConversationsByUser(userId: String): Flow<List<AiConversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessage): Long

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: Long): Flow<List<AiMessage>>

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)
}
