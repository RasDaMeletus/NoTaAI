package com.vinote.ui.components

import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    private val idLocale = Locale("id", "ID")

    fun formatRupiah(amount: Long, isPrivacyMode: Boolean = false): String {
        if (isPrivacyMode) return "Rp ••••••"
        val formatter = NumberFormat.getNumberInstance(idLocale)
        return "Rp " + formatter.format(amount)
    }

    fun formatShortRupiah(amount: Long, isPrivacyMode: Boolean = false): String {
        if (isPrivacyMode) return "Rp •••"
        return when {
            amount >= 1_000_000_000L -> "Rp ${(amount / 1_000_000_000.0).toString().removeSuffix(".0")}b"
            amount >= 1_000_000L -> "Rp ${(amount / 1_000_000.0).toString().removeSuffix(".0")}m"
            amount >= 1_000L -> "Rp ${(amount / 1_000.0).toString().removeSuffix(".0")}k"
            else -> "Rp $amount"
        }
    }
}
