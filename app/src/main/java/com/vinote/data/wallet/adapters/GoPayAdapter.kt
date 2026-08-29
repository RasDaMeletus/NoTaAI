package com.vinote.data.wallet.adapters

import com.vinote.data.model.TransactionType
import com.vinote.domain.wallet.ParsedWalletTransaction
import com.vinote.domain.wallet.WalletAdapter
import com.vinote.domain.wallet.WalletNotification

class GoPayAdapter : WalletAdapter {
    override val walletId: String = "gopay"
    override val displayName: String = "GoPay"
    override val supportedPackageNames: Set<String> = setOf(
        "com.gojek.app",
        "com.gopay.wallet",
        "com.gojek.gopay"
    )
    override val iconColorHex: String = "#00B14F"

    override fun isFinancialNotification(notification: WalletNotification): Boolean {
        val full = "${notification.title} ${notification.text} ${notification.bigText ?: ""}".lowercase()
        return full.contains("pembayaran berhasil") || full.contains("transaksi berhasil") ||
                full.contains("kamu telah membayar") || full.contains("kamu menerima") ||
                full.contains("top up berhasil") || full.contains("transfer berhasil") ||
                full.contains("rp")
    }

    override fun parseNotification(notification: WalletNotification): ParsedWalletTransaction? {
        val content = "${notification.title} ${notification.text} ${notification.bigText ?: ""}"
        val lower = content.lowercase()

        val amountRegex = """(?:rp\.?\s*)?([0-9.,]{4,})""".toRegex(RegexOption.IGNORE_CASE)
        val match = amountRegex.find(content) ?: return null
        val cleanAmount = match.groupValues[1].replace(".", "").replace(",", "").toLongOrNull() ?: return null

        val isIncome = lower.contains("menerima") || lower.contains("masuk") || lower.contains("cashback")
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        val category = when {
            lower.contains("gofood") || lower.contains("makan") || lower.contains("kopi") -> "Food"
            lower.contains("goride") || lower.contains("gocar") -> "Transport"
            lower.contains("gopulsa") || lower.contains("pln") || lower.contains("tagihan") -> "Bills"
            lower.contains("gomart") || lower.contains("indomaret") || lower.contains("alfamart") -> "Shopping"
            else -> if (isIncome) "Income" else "General"
        }

        val merchant = when {
            lower.contains("gofood") -> "GoFood"
            lower.contains("goride") -> "GoRide"
            lower.contains("gocar") -> "GoCar"
            lower.contains("indomaret") -> "Indomaret"
            lower.contains("alfamart") -> "Alfamart"
            else -> notification.title.ifBlank { "GoPay Merchant" }
        }

        return ParsedWalletTransaction(
            title = if (type == TransactionType.INCOME) "GoPay Inflow" else merchant,
            amount = cleanAmount,
            category = category,
            type = type,
            merchant = merchant,
            walletName = displayName,
            rawNotification = notification,
            confidence = 0.98f
        )
    }
}
