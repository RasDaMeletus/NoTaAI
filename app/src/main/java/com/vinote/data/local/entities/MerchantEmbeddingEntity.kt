package com.vinote.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_embedding_cache")
data class MerchantEmbeddingEntity(
    @PrimaryKey
    @ColumnInfo(name = "merchant_name")
    val merchantName: String,

    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,

    @ColumnInfo(name = "category_id")
    val categoryId: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float = 1.0f,

    @ColumnInfo(name = "correction_count")
    val correctionCount: Int = 1,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
