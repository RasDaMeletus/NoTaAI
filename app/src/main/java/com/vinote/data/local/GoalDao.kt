package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GoalItem
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id ASC")
    fun getAllGoals(): Flow<List<GoalItem>>

    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY id ASC")
    fun getGoalsForUser(userId: String): Flow<List<GoalItem>>

    @Query("SELECT * FROM goals WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getGoalById(id: Long, userId: String): GoalItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalItem>)

    @Update
    suspend fun updateGoal(goal: GoalItem)

    @Delete
    suspend fun deleteGoal(goal: GoalItem)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM goals WHERE userId = :userId")
    suspend fun clearUserGoals(userId: String)

    @Query("DELETE FROM goals")
    suspend fun clearAll()
}
