package com.vinote.domain.ai

import com.vinote.data.local.BudgetDao
import com.vinote.data.local.GoalDao
import com.vinote.data.local.TransactionDao
import com.vinote.data.local.WalletAccountDao
import com.vinote.data.local.RecurringTransactionDao
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Controlled Finance Tools for NoTa AI.
 * Supplies real, grounded deterministic ledger state to AI prompts
 * and formats structured transaction/goal drafts that require user confirmation.
 */
class NoTaFinanceTools(
    private val transactionDao: TransactionDao,
    private val goalDao: GoalDao,
    private val walletAccountDao: WalletAccountDao,
    private val budgetDao: BudgetDao,
    private val recurringTransactionDao: RecurringTransactionDao? = null
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

    suspend fun searchTransactions(userId: String, keyword: String): List<TransactionItem> = withContext(Dispatchers.IO) {
        val all = transactionDao.getTransactionsForUser(userId).first()
        val query = keyword.trim().lowercase()
        all.filter {
            it.title.lowercase().contains(query) ||
            it.category.lowercase().contains(query) ||
            (it.merchant.lowercase().contains(query)) ||
            (it.walletName?.lowercase()?.contains(query) == true)
        }
    }

    suspend fun getSpendingByCategory(userId: String): Map<String, Long> = withContext(Dispatchers.IO) {
        val all = transactionDao.getTransactionsForUser(userId).first()
        all.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    suspend fun getMerchantSummary(userId: String, merchantName: String): String = withContext(Dispatchers.IO) {
        val all = transactionDao.getTransactionsForUser(userId).first()
        val query = merchantName.trim().lowercase()
        val txs = all.filter { it.merchant.lowercase().contains(query) || it.title.lowercase().contains(query) }
        if (txs.isEmpty()) return@withContext "Belum ada transaksi tercatat di merchant '$merchantName'."
        val totalSpent = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val count = txs.size
        "Kamu telah bertransaksi sebanyak $count kali di '$merchantName' dengan total pengeluaran Rp $totalSpent."
    }

    suspend fun comparePeriods(userId: String): String = withContext(Dispatchers.IO) {
        val all = transactionDao.getTransactionsForUser(userId).first()
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 3600 * 1000L
        val currentPeriodTxs = all.filter { it.timestamp >= (now - thirtyDaysMs) && it.type == TransactionType.EXPENSE }
        val previousPeriodTxs = all.filter { it.timestamp in (now - 2 * thirtyDaysMs) until (now - thirtyDaysMs) && it.type == TransactionType.EXPENSE }

        val currentSpent = currentPeriodTxs.sumOf { it.amount }
        val previousSpent = previousPeriodTxs.sumOf { it.amount }

        if (previousSpent == 0L) {
            return@withContext "Pengeluaran 30 hari terakhir: Rp $currentSpent. (Data periode sebelumnya belum mencukupi)."
        }
        val diff = currentSpent - previousSpent
        val percent = ((diff.toDouble() / previousSpent.toDouble()) * 100).toInt()
        val status = if (diff > 0) "naik $percent%" else "turun ${-percent}%"
        "Pengeluaran 30 hari terakhir (Rp $currentSpent) $status dibandingkan 30 hari sebelumnya (Rp $previousSpent)."
    }

    suspend fun getRecurringExpenses(userId: String): String = withContext(Dispatchers.IO) {
        val recurringList = recurringTransactionDao?.getRecurringForUser(userId)?.first() ?: emptyList()
        if (recurringList.isEmpty()) return@withContext "Belum ada pengeluaran rutin / langganan aktif."
        recurringList.filter { it.isActive }.joinToString("\n") { r ->
            "- Frekuensi: ${r.frequency}, Jatuh tempo: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(r.nextDueDate))}"
        }
    }

    suspend fun calculateSavingsRate(userId: String): Double = withContext(Dispatchers.IO) {
        val totalIncome = transactionDao.getTotalIncome(userId) ?: 0L
        val totalExpense = transactionDao.getTotalExpense(userId) ?: 0L
        if (totalIncome <= 0L) return@withContext 0.0
        val savings = (totalIncome - totalExpense).coerceAtLeast(0L)
        (savings.toDouble() / totalIncome.toDouble()) * 100.0
    }

    suspend fun predictEndOfMonthBalance(userId: String): Long = withContext(Dispatchers.IO) {
        val currentBal = getCurrentBalance(userId)
        val dailySpent = getDailySpending(userId)
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val remainingDays = (daysInMonth - currentDay).coerceAtLeast(1)
        val projectedBurn = dailySpent * remainingDays
        (currentBal - projectedBurn).coerceAtLeast(0L)
    }

    fun createBudgetSuggestion(monthlyIncome: Long): String {
        val needs = (monthlyIncome * 0.50).toLong()
        val wants = (monthlyIncome * 0.30).toLong()
        val savings = (monthlyIncome * 0.20).toLong()
        return "Alokasi Budget Ideal 50/30/20 untuk penghasilan Rp $monthlyIncome:\n" +
               "- Kebutuhan Pokok (50%): Rp $needs\n" +
               "- Keinginan & Hiburan (30%): Rp $wants\n" +
               "- Tabungan & Investasi (20%): Rp $savings"
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
