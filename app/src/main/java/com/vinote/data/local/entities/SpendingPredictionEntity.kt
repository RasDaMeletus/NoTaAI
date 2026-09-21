package com.vinote.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spending_predictions")
data class SpendingPredictionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "prediction_date")
    val predictionDate: Long,

    @ColumnInfo(name = "category_id")
    val categoryId: String?,

    @ColumnInfo(name = "predicted_amount_cents")
    val predictedAmountCents: Long,

    @ColumnInfo(name = "actual_amount_cents")
    val actualAmountCents: Long? = null,

    @ColumnInfo(name = "risk_day")
    val riskDay: String? = null,

    @ColumnInfo(name = "insight_text")
    val insightText: String,

    @ColumnInfo(name = "generated_at")
    val generatedAt: Long = System.currentTimeMillis()
)
