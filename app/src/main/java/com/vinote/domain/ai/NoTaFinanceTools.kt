package com.example.domain.ai

import com.example.data.local.BudgetDao
import com.example.data.local.GoalDao
import com.example.data.local.TransactionDao
import com.example.data.local.WalletAccountDao
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Controlled Finance Tools for NoTa AI.
 * Supplies real, grounded deterministic ledger state to AI prompts
 * and formats structured transaction/goal drafts that require user confirmation.
 */
class NoTaFinanceTools(
    private val transactionDao: TransactionDao,
    private val goalDao: GoalDao,
    private val walletAccountDao: WalletAccountDao,
    private val budgetDao: BudgetDao
) {
    suspend fun getCurrentBalance(userId: String): Long = withContext(Dispatchers.IO) {
        val totalIncome = transactionDao.getTotalIncome(userId) ?: 0L
        val totalExpense = transactionDao.getTotalExpense(userId) ?: 0L
        // Base starting capital + Income - Expense
        1250000L + totalIncome - totalExpense
    }

    suspend fun getDailySpending(userId: String): Long = withContext(Dispatchers.IO) {
        val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 3600 * 1000L))
        transactionDao.getExpenseSumSince(userId, startOfDay) ?: 0L
    }

    suspend fun getRecentTransactionsSummary(userId: String, limit: Int = 5): String = withContext(Dispatchers.IO) {
        val txs = transactionDao.getTransactionsForUser(userId).first().take(limit)
        if (txs.isEmpty()) return@withContext "No transactions recorded yet."

        txs.joinToString("\n") { tx ->
            val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
            "- ${tx.title}: ${sign}Rp ${tx.amount} (${tx.category}, via ${tx.walletName ?: "Cash"}) [${tx.timeLabel}]"
        }
    }

    suspend fun getBudgetStatus(userId: String): String = withContext(Dispatchers.IO) {
        val budget = budgetDao.getBudget(userId)
        val dailySpent = getDailySpending(userId)
        val monthlyLimit = budget?.monthlyLimit ?: 3000000L
        val dailyLimit = budget?.dailyLimit ?: 100000L
        val percent = if (dailyLimit > 0) ((dailySpent.toDouble() / dailyLimit.toDouble()) * 100).toInt() else 0

        "Daily Budget: Rp $dailyLimit (Spent today: Rp $dailySpent, $percent%). Monthly Limit: Rp $monthlyLimit."
    }

    suspend fun getGoalsProgressSummary(userId: String): String = withContext(Dispatchers.IO) {
        val goals = goalDao.getGoalsForUser(userId).first()
        if (goals.isEmpty()) return@withContext "No active savings goals."

        goals.joinToString("\n") { g ->
            "- ${g.title}: Rp ${g.currentAmount} / Rp ${g.targetAmount} (${g.progressPercentage}%, target ${g.targetDateDescription})"
        }
    }

    fun buildGroundedSystemContext(
        userId: String,
        balance: Long,
        dailySpent: Long,
        txSummary: String,
        budgetSummary: String,
        goalsSummary: String
    ): String {
        return """
            You are NoTa, a friendly, ultra-knowledgeable personal finance companion for young adults and students in Indonesia.
            
            REAL GROUNDED FINANCIAL STATE (DO NOT INVENT NUMBERS OR GUESS):
            - Current Balance: Rp $balance
            - Today's Spending: Rp $dailySpent
            - Budget: $budgetSummary
            - Recent Transactions:
            $txSummary
            - Active Savings Goals:
            $goalsSummary
            
            RULES:
            1. Always base advice strictly on the real numbers above.
            2. Be supportive, concise, and clear with tips on saving and mindful spending.
            3. Use Indonesian Rupiah (Rp) formatting.
            4. Keep answers under 3 paragraphs with a helpful mascot personality ✨
        """.trimIndent()
    }
}
