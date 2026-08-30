package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String = "default_budget",
    val userId: String = "user_default",
    val monthlyLimit: Long = 3000000L,
    val dailyLimit: Long = 100000L,
    val warningThresholdPercent: Float = 0.85f,
    val periodMonthYear: String = "2026-08",
    val updatedTimestamp: Long = System.currentTimeMillis()
)
