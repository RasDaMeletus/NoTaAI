package com.vinote.data.wallet.adapters

import com.vinote.data.model.TransactionType
import com.vinote.domain.wallet.ParsedWalletTransaction
import com.vinote.domain.wallet.WalletAdapter
import com.vinote.domain.wallet.WalletNotification

class OVOAdapter : WalletAdapter {
    override val walletId: String = "ovo"
    override val displayName: String = "OVO"
    override val supportedPackageNames: Set<String> = setOf("ovo.id", "com.ovoapp")
    override val iconColorHex: String = "#4C3494"

    override fun isFinancialNotification(notification: WalletNotification): Boolean {
        val full = "${notification.title} ${notification.text} ${notification.bigText ?: ""}".lowercase()
        return full.contains("berhasil") || full.contains("ovo cash") || full.contains("transaksi") || full.contains("rp")
    }

    override fun parseNotification(notification: WalletNotification): ParsedWalletTransaction? {
        val content = "${notification.title} ${notification.text} ${notification.bigText ?: ""}"
        val lower = content.lowercase()

        val amountRegex = """(?:rp\.?\s*)?([0-9.,]{4,})""".toRegex(RegexOption.IGNORE_CASE)
        val match = amountRegex.find(content) ?: return null
        val cleanAmount = match.groupValues[1].replace(".", "").replace(",", "").toLongOrNull() ?: return null

        val isIncome = lower.contains("top up") || lower.contains("cashback") || lower.contains("transfer masuk")
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        val merchantMatch = """(?:ke|di)\s+([A-Za-z0-9\s&.'-]+?)(?:\s+sebesar|\s+berhasil|\s+menggunakan|\s*$)""".toRegex(RegexOption.IGNORE_CASE).find(content)
        val extractedMerchant = merchantMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() && it.length < 50 }
        val merchant = extractedMerchant ?: "OVO Merchant"

        val category = when {
            lower.contains("grab") || lower.contains("food") || lower.contains("kopi") || lower.contains("resto") -> "Food"
            lower.contains("ride") || lower.contains("car") || lower.contains("parkir") -> "Transport"
            lower.contains("pln") || lower.contains("pulsa") || lower.contains("tagihan") -> "Bills"
            else -> if (isIncome) "Income" else "General"
        }

        return ParsedWalletTransaction(
            title = if (type == TransactionType.INCOME) "OVO Inflow" else extractedMerchant ?: "OVO Payment",
            amount = cleanAmount,
            category = category,
            type = type,
            merchant = merchant,
            walletName = displayName,
            rawNotification = notification,
            confidence = 0.96f
        )
    }
}
