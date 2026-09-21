package com.vinote.data.wallet.adapters

import com.vinote.data.model.TransactionType
import com.vinote.domain.wallet.ParsedWalletTransaction
import com.vinote.domain.wallet.WalletAdapter
import com.vinote.domain.wallet.WalletNotification

class BRImoAdapter : WalletAdapter {
    override val walletId: String = "brimo"
    override val displayName: String = "BRI (BRImo)"
    override val supportedPackageNames: Set<String> = setOf("id.co.bri.brimo")
    override val iconColorHex: String = "#00529C"

    override fun isFinancialNotification(notification: WalletNotification): Boolean {
        val full = "${notification.title} ${notification.text} ${notification.bigText ?: ""}".lowercase()
        return full.contains("brimo") ||
                full.contains("transaksi berhasil") ||
                full.contains("pembayaran berhasil") ||
                full.contains("transfer berhasil") ||
                full.contains("qris") ||
                full.contains("debet") ||
                full.contains("kredit") ||
                (full.contains("bri") && full.contains("rp"))
    }

    override fun parseNotification(notification: WalletNotification): ParsedWalletTransaction? {
        val content = "${notification.title} ${notification.text} ${notification.bigText ?: ""}"
        val lower = content.lowercase()

        val amountRegex = """(?:rp\.?\s*)?([0-9.,]{4,})""".toRegex(RegexOption.IGNORE_CASE)
        val match = amountRegex.find(content) ?: return null
        val cleanAmount = match.groupValues[1].replace(".", "").replace(",", "").toLongOrNull() ?: return null

        val isIncome = lower.contains("kredit") ||
                lower.contains("masuk") ||
                lower.contains("terima") ||
                lower.contains("setor")
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        val category = when {
            lower.contains("qris") -> "General"
            lower.contains("pulsa") || lower.contains("tagihan") || lower.contains("pln") -> "Bills"
            lower.contains("makan") || lower.contains("resto") -> "Food"
            lower.contains("transfer") -> "Transfer"
            else -> if (isIncome) "Income" else "General"
        }

        val title = when {
            type == TransactionType.INCOME -> "BRImo Inflow"
            lower.contains("qris") -> "QRIS BRImo"
            lower.contains("transfer") -> "Transfer BRImo"
            else -> "Transaksi BRImo"
        }

        return ParsedWalletTransaction(
            title = title,
            amount = cleanAmount,
            category = category,
            type = type,
            merchant = "BRI Merchant",
            walletName = displayName,
            rawNotification = notification,
            confidence = 0.97f
        )
    }
}
