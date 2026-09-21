package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vinote.data.local.entities.TransactionTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTemplateDao {
    @Query("SELECT * FROM transaction_templates WHERE userId = :userId ORDER BY usageCount DESC, createdAt DESC")
    fun getTemplatesForUser(userId: String): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: String): TransactionTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TransactionTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<TransactionTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: TransactionTemplateEntity)

    @Query("UPDATE transaction_templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: String)

    @Delete
    suspend fun deleteTemplate(template: TransactionTemplateEntity)

    @Query("DELETE FROM transaction_templates WHERE id = :id")
    suspend fun deleteById(id: String)
}
