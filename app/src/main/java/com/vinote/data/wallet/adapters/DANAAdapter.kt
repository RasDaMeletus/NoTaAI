package com.vinote.data.wallet.adapters

import com.vinote.data.model.TransactionType
import com.vinote.domain.wallet.ParsedWalletTransaction
import com.vinote.domain.wallet.WalletAdapter
import com.vinote.domain.wallet.WalletNotification

class DANAAdapter : WalletAdapter {
    override val walletId: String = "dana"
    override val displayName: String = "DANA"
    override val supportedPackageNames: Set<String> = setOf("id.dana")
    override val iconColorHex: String = "#118EEA"

    override fun isFinancialNotification(notification: WalletNotification): Boolean {
        val full = "${notification.title} ${notification.text} ${notification.bigText ?: ""}".lowercase()
        return full.contains("berhasil") || full.contains("transaksi") || full.contains("bayar") || full.contains("rp")
    }

    override fun parseNotification(notification: WalletNotification): ParsedWalletTransaction? {
        val content = "${notification.title} ${notification.text} ${notification.bigText ?: ""}"
        val lower = content.lowercase()

        val amountRegex = """(?:rp\.?\s*)?([0-9.,]{4,})""".toRegex(RegexOption.IGNORE_CASE)
        val match = amountRegex.find(content) ?: return null
        val cleanAmount = match.groupValues[1].replace(".", "").replace(",", "").toLongOrNull() ?: return null

        val isIncome = lower.contains("terima") || lower.contains("masuk") || lower.contains("saldo bertambah")
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        val category = when {
            lower.contains("makan") || lower.contains("resto") || lower.contains("kopi") -> "Food"
            lower.contains("pulsa") || lower.contains("tagihan") || lower.contains("listrik") -> "Bills"
            lower.contains("games") || lower.contains("voucher") -> "Entertainment"
            else -> if (isIncome) "Income" else "General"
        }

        return ParsedWalletTransaction(
            title = if (type == TransactionType.INCOME) "DANA Inflow" else "DANA Payment",
            amount = cleanAmount,
            category = category,
            type = type,
            merchant = "DANA Merchant",
            walletName = displayName,
            rawNotification = notification,
            confidence = 0.96f
        )
    }
}

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

class BCAAdapter : WalletAdapter {
    override val walletId: String = "bca"
    override val displayName: String = "Bank Central Asia"
    override val supportedPackageNames: Set<String> = setOf("com.bca", "mybca.bca.co.id")
    override val iconColorHex: String = "#003893"

    override fun isFinancialNotification(notification: WalletNotification): Boolean {
        val full = "${notification.title} ${notification.text} ${notification.bigText ?: ""}".lowercase()
        return full.contains("transfer") || full.contains("qris") || full.contains("debit") || full.contains("kredit") || full.contains("rp")
    }

    override fun parseNotification(notification: WalletNotification): ParsedWalletTransaction? {
        val content = "${notification.title} ${notification.text} ${notification.bigText ?: ""}"
        val lower = content.lowercase()

        val amountRegex = """(?:rp\.?\s*)?([0-9.,]{4,})""".toRegex(RegexOption.IGNORE_CASE)
        val match = amountRegex.find(content) ?: return null
        val cleanAmount = match.groupValues[1].replace(".", "").replace(",", "").toLongOrNull() ?: return null

        val isIncome = lower.contains("cr") || lower.contains("masuk") || lower.contains("terima")
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        return ParsedWalletTransaction(
            title = if (type == TransactionType.INCOME) "BCA Inflow" else "QRIS / Debit BCA",
            amount = cleanAmount,
            category = if (type == TransactionType.INCOME) "Income" else "General",
            type = type,
            merchant = "BCA Transaction",
            walletName = "BCA",
            rawNotification = notification,
            confidence = 0.98f
        )
    }
}

class MandiriAdapter : WalletAdapter {
    override val walletId: String = "mandiri"
    override val displayName: String = "Bank Mandiri (Livin')"
    override val supportedPackageNames: Set<String> = setOf("id.bmri.livin")
    override val iconColorHex: String = "#002B66"

    override fun isFinancialNotification(notification: WalletNotification): Boolean {
        val full = "${notification.title} ${notification.text} ${notification.bigText ?: ""}".lowercase()
        return full.contains("transaksi") || full.contains("qris") || full.contains("debit") || full.contains("rp")
    }

    override fun parseNotification(notification: WalletNotification): ParsedWalletTransaction? {
        val content = "${notification.title} ${notification.text} ${notification.bigText ?: ""}"
        val lower = content.lowercase()

        val amountRegex = """(?:rp\.?\s*)?([0-9.,]{4,})""".toRegex(RegexOption.IGNORE_CASE)
        val match = amountRegex.find(content) ?: return null
        val cleanAmount = match.groupValues[1].replace(".", "").replace(",", "").toLongOrNull() ?: return null

        val isIncome = lower.contains("masuk") || lower.contains("kredit") || lower.contains("terima")
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        return ParsedWalletTransaction(
            title = if (type == TransactionType.INCOME) "Livin' Inflow" else "Livin' Mandiri",
            amount = cleanAmount,
            category = if (type == TransactionType.INCOME) "Income" else "General",
            type = type,
            merchant = "Mandiri Merchant",
            walletName = "Mandiri",
            rawNotification = notification,
            confidence = 0.97f
        )
    }
}

class GenericWalletAdapter : WalletAdapter {
    override val walletId: String = "generic"
    override val displayName: String = "E-Wallet / Banking"
    override val supportedPackageNames: Set<String> = emptySet()
    override val iconColorHex: String = "#0057C2"

    override fun isFinancialNotification(notification: WalletNotification): Boolean {
        val full = "${notification.title} ${notification.text}".lowercase()
        return full.contains("rp") || full.contains("transaksi") || full.contains("pembayaran")
    }

    override fun parseNotification(notification: WalletNotification): ParsedWalletTransaction? {
        val content = "${notification.title} ${notification.text}"
        val amountRegex = """(?:rp\.?\s*)?([0-9.,]{4,})""".toRegex(RegexOption.IGNORE_CASE)
        val match = amountRegex.find(content) ?: return null
        val cleanAmount = match.groupValues[1].replace(".", "").replace(",", "").toLongOrNull() ?: return null

        return ParsedWalletTransaction(
            title = notification.title.ifBlank { "Auto Detected Expense" },
            amount = cleanAmount,
            category = "General",
            type = TransactionType.EXPENSE,
            merchant = notification.title,
            walletName = "E-Wallet",
            rawNotification = notification,
            confidence = 0.90f
        )
    }
}
