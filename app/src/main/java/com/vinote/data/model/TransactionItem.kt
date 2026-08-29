package com.vinote.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class TransactionSource {
    MANUAL,
    VOICE,
    SCAN,
    E_WALLET,
    AUTO_DETECTED,
    BANK_SYNC
}

/**
 * Legacy transaction model used by existing UI components.
 * Will be migrated to TransactionEntity in a future Room migration.
 */
@Entity(tableName = "transactions")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val amount: Long, // in IDR
    val category: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val timestamp: Long = System.currentTimeMillis(),
    val timeLabel: String = "Today",
    val merchant: String = "",
    val source: TransactionSource = TransactionSource.MANUAL,
    val walletName: String? = null,
    val userId: String = "",
    val syncState: String = "LOCAL_ONLY" // LOCAL_ONLY, SYNCED, PENDING_SYNC
)
