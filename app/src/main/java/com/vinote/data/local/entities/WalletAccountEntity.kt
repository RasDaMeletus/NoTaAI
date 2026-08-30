package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WalletType {
    EWALLET,
    BANK,
    CASH,
    INVESTMENT
}

@Entity(tableName = "wallet_accounts")
data class WalletAccountEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "user_default",
    val name: String,
    val type: WalletType = WalletType.EWALLET,
    val calculatedBalance: Long = 0L, // Derived from ledger
    val providerReportedBalance: Long? = null, // From app sync/notification
    val lastReconciledAt: Long? = null,
    val isAutoDetectEnabled: Boolean = true,
    val iconColorHex: String = "#0057C2",
    val accountNumber: String = "",
    val isConnected: Boolean = true,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
