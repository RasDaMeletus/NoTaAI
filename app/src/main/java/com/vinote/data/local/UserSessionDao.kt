package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.UserSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_sessions WHERE isAuthenticated = 1 ORDER BY lastActiveTimestamp DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<UserSessionEntity?>

    @Query("SELECT * FROM user_sessions WHERE isAuthenticated = 1 ORDER BY lastActiveTimestamp DESC LIMIT 1")
    suspend fun getActiveSession(): UserSessionEntity?

    @Query("SELECT * FROM user_sessions WHERE userId = :userId LIMIT 1")
    suspend fun getSessionByUserId(userId: String): UserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: UserSessionEntity)

    @Update
    suspend fun updateSession(session: UserSessionEntity)

    @Query("UPDATE user_sessions SET isAuthenticated = 0 WHERE userId = :userId")
    suspend fun deactivateSession(userId: String)

    @Query("UPDATE user_sessions SET isAuthenticated = 0")
    suspend fun deactivateAllSessions()

    @Query("DELETE FROM user_sessions WHERE userId = :userId")
    suspend fun deleteSession(userId: String)

    @Query("DELETE FROM user_sessions")
    suspend fun clearAll()
}
