package com.vinote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Monthly or category budget limit.
 * Supports category limits, progress, remaining amount, warnings.
 */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["userId"], name = "idx_budgets_userId")]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    val category: String,          // "Food", "Transport", etc. or "ALL"
    val monthKey: String,          // "2026-08" format
    val limitAmount: Long,         // Budget cap
    val spentAmount: Long = 0,     // Running total
    val isActive: Boolean = true,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val remainingAmount: Long get() = (limitAmount - spentAmount).coerceAtLeast(0)
    val progressPercentage: Int get() =
        if (limitAmount > 0) ((spentAmount.toDouble() / limitAmount.toDouble()) * 100).toInt().coerceIn(0, 100) else 0
    val isExceeded: Boolean get() = spentAmount > limitAmount
}
