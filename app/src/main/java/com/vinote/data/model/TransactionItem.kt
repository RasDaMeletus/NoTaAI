package com.example.data.model

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

@Entity(tableName = "transactions")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String = "user_default",
    val title: String,
    val amount: Long, // in IDR (exact integer representation, no float inaccuracies)
    val category: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val timestamp: Long = System.currentTimeMillis(),
    val timeLabel: String = "Today",
    val merchant: String = "",
    val source: TransactionSource = TransactionSource.MANUAL,
    val walletName: String? = null,
    val fingerprint: String? = null,
    val confidence: Float = 1.0f,
    val isConfirmed: Boolean = true,
    val syncState: String = "SYNCED"
)
