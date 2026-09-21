package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vinote.data.local.entities.SpendingPredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpendingPredictionDao {

    @Query("SELECT * FROM spending_predictions ORDER BY prediction_date DESC LIMIT 1")
    fun getLatestPrediction(): Flow<SpendingPredictionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(prediction: SpendingPredictionEntity)

    @Query("DELETE FROM spending_predictions WHERE prediction_date < :beforeDate")
    suspend fun cleanOldPredictions(beforeDate: Long)
}
