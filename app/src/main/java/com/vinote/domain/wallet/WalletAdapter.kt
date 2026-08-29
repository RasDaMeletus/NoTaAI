package com.vinote.domain.wallet

import com.vinote.data.model.TransactionType

data class WalletNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val bigText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = ""
)

data class ParsedWalletTransaction(
    val title: String,
    val amount: Long,
    val category: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val merchant: String,
    val walletName: String,
    val rawNotification: WalletNotification,
    val confidence: Float = 0.95f
)

interface WalletAdapter {
    val walletId: String
    val displayName: String
    val supportedPackageNames: Set<String>
    val iconColorHex: String

    fun isFinancialNotification(notification: WalletNotification): Boolean
    fun parseNotification(notification: WalletNotification): ParsedWalletTransaction?
}
