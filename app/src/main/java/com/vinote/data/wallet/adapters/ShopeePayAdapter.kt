package com.vinote.data.wallet.adapters

import com.vinote.data.model.TransactionType
import com.vinote.domain.wallet.ParsedWalletTransaction
import com.vinote.domain.wallet.WalletAdapter
import com.vinote.domain.wallet.WalletNotification

class ShopeePayAdapter : WalletAdapter {
    override val walletId: String = "shopeepay"
    override val displayName: String = "ShopeePay"
    override val supportedPackageNames: Set<String> = setOf(
        "com.shopee.id",
        "com.shopee.id.seller"
    )
    override val iconColorHex: String = "#EE4D2D"

    override fun isFinancialNotification(notification: WalletNotification): Boolean {
        val full = "${notification.title} ${notification.text} ${notification.bigText ?: ""}".lowercase()
        return full.contains("shopeepay") ||
                full.contains("pembayaran berhasil") ||
                full.contains("transaksi berhasil") ||
                full.contains("isi saldo berhasil") ||
                full.contains("transfer berhasil") ||
                full.contains("kamu telah membayar") ||
                full.contains("kamu menerima") ||
                full.contains("cashback") ||
                (full.contains("shopee") && full.contains("rp"))
    }

    override fun parseNotification(notification: WalletNotification): ParsedWalletTransaction? {
        val content = "${notification.title} ${notification.text} ${notification.bigText ?: ""}"
        val lower = content.lowercase()

        val amountRegex = """(?:rp\.?\s*)?([0-9.,]{4,})""".toRegex(RegexOption.IGNORE_CASE)
        val match = amountRegex.find(content) ?: return null
        val cleanAmount = match.groupValues[1].replace(".", "").replace(",", "").toLongOrNull() ?: return null

        val isIncome = lower.contains("menerima") ||
                lower.contains("masuk") ||
                lower.contains("cashback") ||
                lower.contains("isi saldo") ||
                lower.contains("top up") ||
                lower.contains("pengembalian") ||
                lower.contains("refund")
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        val category = when {
            lower.contains("shopeefood") || lower.contains("makanan") || lower.contains("resto") -> "Food"
            lower.contains("pulsa") || lower.contains("tagihan") || lower.contains("pln") || lower.contains("bpjs") -> "Bills"
            lower.contains("mart") || lower.contains("supermarket") || lower.contains("indomaret") || lower.contains("alfamart") -> "Groceries"
            lower.contains("ongkir") || lower.contains("kurir") -> "Transport"
            else -> if (isIncome) "Income" else "Shopping"
        }

        val merchant = when {
            lower.contains("shopeefood") -> "ShopeeFood"
            lower.contains("indomaret") -> "Indomaret"
            lower.contains("alfamart") -> "Alfamart"
            else -> notification.title.ifBlank { "ShopeePay Merchant" }
        }

        return ParsedWalletTransaction(
            title = if (type == TransactionType.INCOME) "ShopeePay Inflow" else merchant,
            amount = cleanAmount,
            category = category,
            type = type,
            merchant = merchant,
            walletName = displayName,
            rawNotification = notification,
            confidence = 0.97f
        )
    }
}
