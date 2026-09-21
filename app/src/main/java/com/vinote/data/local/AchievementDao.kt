package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vinote.data.local.entities.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY is_unlocked DESC, id ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getAchievementById(id: String): AchievementEntity?

    @Query("SELECT COUNT(*) FROM achievements WHERE is_unlocked = 1")
    fun getUnlockedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(item: AchievementEntity)

    @Query("UPDATE achievements SET progress = :progress, is_unlocked = :isUnlocked, unlocked_at = :unlockedAt WHERE id = :id")
    suspend fun updateProgressAndUnlock(id: String, progress: Int, isUnlocked: Boolean, unlockedAt: Long?)
}
