package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vinote.data.local.entity.AppIntegration
import kotlinx.coroutines.flow.Flow

@Dao
interface AppIntegrationDao {
    @Query("SELECT * FROM app_integrations WHERE userId = :userId")
    fun getIntegrationsByUser(userId: String): Flow<List<AppIntegration>>

    @Query("SELECT * FROM app_integrations WHERE userId = :userId AND provider = :provider LIMIT 1")
    suspend fun getByProvider(userId: String, provider: String): AppIntegration?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(integration: AppIntegration): Long

    @Update
    suspend fun update(integration: AppIntegration)
}
