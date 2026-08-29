package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vinote.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthKey = :monthKey")
    fun getBudgetsForMonth(userId: String, monthKey: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND category = :category AND monthKey = :monthKey LIMIT 1")
    suspend fun getBudgetForCategory(userId: String, category: String, monthKey: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
