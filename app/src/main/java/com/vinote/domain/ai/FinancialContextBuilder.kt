package com.vinote.domain.ai

import com.vinote.data.model.BankAccountItem
import com.vinote.data.model.GoalItem
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import com.vinote.data.model.UserProfile
import com.vinote.ui.components.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds compact, concise, privacy-safe financial snapshots
 * for AI prompts without exposing excessive raw records.
 */
object FinancialContextBuilder {

    fun buildSystemPrompt(
        userProfile: UserProfile,
        transactions: List<TransactionItem>,
        goals: List<GoalItem>,
        accounts: List<BankAccountItem>,
        dailyLimit: Long,
        safeMoney: Long
    ): String {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val currentBalance = totalIncome - totalExpense

        // Category breakdown
        val expenseByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val topCategoriesSummary = if (expenseByCategory.isEmpty()) {
            "No recorded expenses yet"
        } else {
            expenseByCategory.joinToString("\n") { (cat, amount) ->
                "- $cat: ${FormatUtils.formatRupiah(amount)}"
            }
        }

        // Recent 5 transactions
        val recentTxSummary = if (transactions.isEmpty()) {
            "No recent transactions."
        } else {
            transactions.take(5).joinToString("\n") { tx ->
                val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
                "- ${tx.title} (${tx.category}): $sign${FormatUtils.formatRupiah(tx.amount)} [${tx.timeLabel}]"
            }
        }

        // Active Goals
        val activeGoalsSummary = if (goals.isEmpty()) {
            "No active savings goals."
        } else {
            goals.take(3).joinToString("\n") { g ->
                val pct = if (g.targetAmount > 0) ((g.currentAmount.toDouble() / g.targetAmount) * 100).toInt() else 0
                "- ${g.title}: ${FormatUtils.formatRupiah(g.currentAmount)} / ${FormatUtils.formatRupiah(g.targetAmount)} ($pct% saved)"
            }
        }

        // Connected Accounts
        val connectedWallets = accounts.filter { it.isConnected }
        val walletSummary = if (connectedWallets.isEmpty()) {
            "Cash only"
        } else {
            connectedWallets.joinToString(", ") { "${it.bankName} (${FormatUtils.formatRupiah(it.balance)})" }
        }

        val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())

        return """
You are NoTa, a lively, helpful, and highly intelligent AI Financial Companion in the ViNote Android app.
Today's Date: $dateStr

USER PROFILE & FINANCIAL METRICS:
- User Name: ${userProfile.fullName}
- Financial Persona: ${userProfile.financialPersona}
- Net Balance (Calculated): ${FormatUtils.formatRupiah(currentBalance)}
- Total Income: ${FormatUtils.formatRupiah(totalIncome)}
- Total Expenses: ${FormatUtils.formatRupiah(totalExpense)}
- Daily Budget Limit: ${FormatUtils.formatRupiah(dailyLimit)}
- Uang Aman (Safe to Spend): ${FormatUtils.formatRupiah(safeMoney)}
- Connected Accounts: $walletSummary

TOP SPENDING CATEGORIES:
$topCategoriesSummary

RECENT TRANSACTIONS:
$recentTxSummary

ACTIVE SAVINGS GOALS:
$activeGoalsSummary

INSTRUCTIONS:
1. When user reports spending or income (e.g. "Makan 25rb pakai GoPay", "Beli kopi 30k"), output valid JSON intent "create_transaction" with fields:
   {
     "intent": "create_transaction",
     "type": "expense" or "income",
     "amount": 25000,
     "currency": "IDR",
     "category": "Food" / "Transport" / "Shopping" / "Bills" / "Coffee" / "Entertainment" / "General",
     "description": "Short title",
     "merchant": "Merchant name if available",
     "wallet": "GoPay" / "OVO" / "DANA" / "BCA" / "Cash",
     "confidence": 0.95,
     "reply_text": "Friendly short acknowledgement in Indonesian/English matching user query"
   }

2. When user asks financial questions (e.g., "Saldo aku berapa?", "Uangku paling banyak habis buat apa?", "Aku minggu ini boros gak?"), provide an empathetic, accurate, and concise answer using the REAL metrics above. Never hallucinate fake balances.

3. Always answer warmly in the personality of NoTa (friendly, encouraging, smart financial mentor). Keep text concise and readable. Return valid structured responses.
""".trimIndent()
    }
}
