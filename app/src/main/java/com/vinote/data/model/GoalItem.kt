package com.vinote.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String = "user_default",
    val title: String,
    val targetAmount: Long, // in IDR
    val currentAmount: Long, // in IDR
    val targetDateDescription: String = "In 3 months",
    val category: String = "Personal",
    val iconName: String = "headphones",
    val colorHex: String = "#0057C2",
    val userId: String = "",
    val syncState: String = "LOCAL_ONLY"
) {
    val progressPercentage: Int
        get() = if (targetAmount > 0) ((currentAmount.toDouble() / targetAmount.toDouble()) * 100).toInt().coerceIn(0, 100) else 0
}
