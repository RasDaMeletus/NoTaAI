package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vinote.data.local.entities.TransactionCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionCategoryDao {
    @Query("SELECT * FROM transaction_categories WHERE userId = :userId AND type = :type ORDER BY name ASC")
    fun getCategoriesByType(userId: String, type: String): Flow<List<TransactionCategoryEntity>>

    @Query("SELECT * FROM transaction_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<TransactionCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TransactionCategoryEntity): Long

    @Query("DELETE FROM transaction_categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)
}