package com.vinote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a user's wallet or bank account (GoPay, DANA, BCA, etc.).
 * Supports both ledger-derived and provider-reported balance modes.
 */
@Entity(
    tableName = "wallet_accounts",
    indices = [Index(value = ["userId"], name = "idx_wallet_accounts_userId")]
)
data class WalletAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    val name: String,              // "GoPay", "DANA", "BCA Savings", etc.
    val type: String,              // "E_WALLET" or "BANK"
    val provider: String,          // "gopay", "dana", "bca", etc.
    val openingBalance: Long = 0,  // Ledger starting point
    val providerBalance: Long? = null, // Balance reported by provider
    val currency: String = "IDR",
    val isActive: Boolean = true,
    val isAutoDetectEnabled: Boolean = true,
    val lastDetectionAt: Long? = null,
    val lastSyncAt: Long? = null,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
