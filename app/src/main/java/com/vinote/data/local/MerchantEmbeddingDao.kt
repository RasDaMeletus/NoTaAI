package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vinote.data.local.entities.MerchantEmbeddingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantEmbeddingDao {

    @Query("SELECT * FROM merchant_embedding_cache WHERE merchant_name = :name LIMIT 1")
    suspend fun getMerchantByName(name: String): MerchantEmbeddingEntity?

    @Query("SELECT * FROM merchant_embedding_cache WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun getMerchantByNormalizedName(normalizedName: String): MerchantEmbeddingEntity?

    @Query("SELECT * FROM merchant_embedding_cache ORDER BY correction_count DESC")
    fun getAllLearnedMerchants(): Flow<List<MerchantEmbeddingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MerchantEmbeddingEntity)

    @Query("""
        UPDATE merchant_embedding_cache 
        SET category_id = :categoryId, 
            correction_count = correction_count + 1, 
            confidence = CASE WHEN correction_count >= 4 THEN 1.0 ELSE confidence + 0.1 END,
            last_updated = :timestamp 
        WHERE merchant_name = :merchantName
    """)
    suspend fun updateCategoryCorrection(merchantName: String, categoryId: String, timestamp: Long): Int

    @Query("DELETE FROM merchant_embedding_cache WHERE merchant_name = :name")
    suspend fun deleteByName(name: String)
}
