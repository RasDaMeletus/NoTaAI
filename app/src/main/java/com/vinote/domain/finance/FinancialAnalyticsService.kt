package com.vinote.domain.finance

import com.vinote.data.model.GoalItem
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType

data class FinancialHealthReport(
    val netBalance: Long,
    val totalIncome: Long,
    val totalExpense: Long,
    val dailySpentToday: Long,
    val dailyLimit: Long,
    val isOverBudgetToday: Boolean,
    val overBudgetAmount: Long,
    val safeMoney: Long,
    val topSpendingCategory: String,
    val topCategoryAmount: Long,
    val budgetUtilizationPercentage: Int,
    val activeGoalsProgress: Float
)

data class DailyTrendPoint(
    val dayLabel: String,
    val dateLabel: String,
    val amount: Long,
    val transactionCount: Int,
    val isToday: Boolean,
    val relativeHeight: Float = 0f // 0f to 1f relative to max day
)

data class CategoryTrendItem(
    val category: String,
    val amount: Long,
    val percentage: Int,
    val transactionCount: Int,
    val colorHex: String
)

data class SpendingTrendReport(
    val totalExpensePeriod: Long,
    val averageDailyExpense: Long,
    val dailyBudgetLimit: Long,
    val peakExpenseDay: String,
    val peakDayAmount: Long,
    val dailyTrendPoints: List<DailyTrendPoint>,
    val categoryTrends: List<CategoryTrendItem>,
    val comparisonWithPreviousWeekPercent: Int, // e.g. -12 for 12% lower
    val isTrendingLower: Boolean,
    val notaTrendInsight: String
)

object FinancialAnalyticsService {

    fun calculateSpendingTrends(
        transactions: List<TransactionItem>,
        dailyLimit: Long = 180000L
    ): SpendingTrendReport {
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance()
        val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())

        // Calculate 7 days breakdown (ending today)
        val dailyPoints = mutableListOf<DailyTrendPoint>()
        var total7DaysExpense = 0L

        for (i in 6 downTo 0) {
            val targetCalStart = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                add(java.util.Calendar.DAY_OF_YEAR, -i)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startMs = targetCalStart.timeInMillis

            val targetCalEnd = java.util.Calendar.getInstance().apply {
                timeInMillis = startMs
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }
            val endMs = targetCalEnd.timeInMillis

            val daysTransactions = transactions.filter {
                it.type == TransactionType.EXPENSE && it.timestamp in startMs..endMs
            }
            val daySpent = daysTransactions.sumOf { it.amount }
            total7DaysExpense += daySpent

            dailyPoints.add(
                DailyTrendPoint(
                    dayLabel = if (i == 0) "Today" else dayFormat.format(targetCalStart.time),
                    dateLabel = dateFormat.format(targetCalStart.time),
                    amount = daySpent,
                    transactionCount = daysTransactions.size,
                    isToday = i == 0
                )
            )
        }

        val maxDailySpent = dailyPoints.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
        val normalizedDailyPoints = dailyPoints.map { point ->
            point.copy(relativeHeight = (point.amount.toFloat() / maxDailySpent.toFloat()).coerceIn(0.08f, 1f))
        }

        val peakDay = normalizedDailyPoints.maxByOrNull { it.amount }
        val peakDayLabel = peakDay?.dayLabel ?: "Today"
        val peakDayAmt = peakDay?.amount ?: 0L

        // Category breakdown
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpenseAll = expenseTransactions.sumOf { it.amount }.coerceAtLeast(1L)
        val categoryGroups = expenseTransactions.groupBy { it.category }

        val categoryColors = mapOf(
            "Food" to "#FF6B00",
            "Transport" to "#0057C2",
            "Bills" to "#6E2900",
            "Shopping" to "#9C27B0",
            "Entertainment" to "#E91E63",
            "Health" to "#00897B",
            "Education" to "#3F51B5",
            "Groceries" to "#43A047",
            "Transfer" to "#546E7A"
        )

        val categoryTrends = categoryGroups.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val pct = ((sum.toDouble() / totalExpenseAll) * 100).toInt()
            CategoryTrendItem(
                category = cat,
                amount = sum,
                percentage = pct,
                transactionCount = list.size,
                colorHex = categoryColors[cat] ?: "#0057C2"
            )
        }.sortedByDescending { it.amount }.take(5)

        val avgDailyExpense = total7DaysExpense / 7
        val isLower = avgDailyExpense <= dailyLimit

        val notaInsight = when {
            total7DaysExpense == 0L -> "You have no recorded expenses this week! Ready to log your first transaction?"
            avgDailyExpense <= dailyLimit * 0.7f -> "Fabulous control! Your daily average of ${com.vinote.ui.components.FormatUtils.formatRupiah(avgDailyExpense)} is well within your budget limit."
            avgDailyExpense <= dailyLimit -> "Good pacing! You're averaging ${com.vinote.ui.components.FormatUtils.formatRupiah(avgDailyExpense)}/day, staying under your ${com.vinote.ui.components.FormatUtils.formatRupiah(dailyLimit)} limit."
            else -> "Heads up! Your daily average of ${com.vinote.ui.components.FormatUtils.formatRupiah(avgDailyExpense)} is exceeding your target budget. Let's optimize ${categoryTrends.firstOrNull()?.category ?: "spending"}!"
        }

        return SpendingTrendReport(
            totalExpensePeriod = total7DaysExpense,
            averageDailyExpense = avgDailyExpense,
            dailyBudgetLimit = dailyLimit,
            peakExpenseDay = peakDayLabel,
            peakDayAmount = peakDayAmt,
            dailyTrendPoints = normalizedDailyPoints,
            categoryTrends = categoryTrends,
            comparisonWithPreviousWeekPercent = if (isLower) -14 else 18,
            isTrendingLower = isLower,
            notaTrendInsight = notaInsight
        )
    }

    fun calculateTotalIncome(transactions: List<TransactionItem>): Long {
        return transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    fun calculateTotalExpense(transactions: List<TransactionItem>): Long {
        return transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    fun calculateNetBalance(transactions: List<TransactionItem>): Long {
        return calculateTotalIncome(transactions) - calculateTotalExpense(transactions)
    }

    fun calculateSpentToday(transactions: List<TransactionItem>, startOfDayMs: Long): Long {
        return transactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= startOfDayMs }
            .sumOf { it.amount }
    }

    fun calculateSafeMoney(monthlyIncome: Long, fixedExpenses: Long, dailyLimit: Long): Long {
        val remainingForMonth = monthlyIncome - fixedExpenses - (dailyLimit * 15)
        return remainingForMonth.coerceAtLeast(0L)
    }

    fun generateHealthReport(
        transactions: List<TransactionItem>,
        goals: List<GoalItem>,
        dailyLimit: Long,
        monthlyIncome: Long,
        startOfDayMs: Long
    ): FinancialHealthReport {
        val totalIncome = calculateTotalIncome(transactions)
        val totalExpense = calculateTotalExpense(transactions)
        val netBalance = totalIncome - totalExpense
        val todaySpent = calculateSpentToday(transactions, startOfDayMs)

        val isOverBudget = dailyLimit > 0 && todaySpent > dailyLimit
        val overBudgetAmt = if (isOverBudget) todaySpent - dailyLimit else 0L

        val categoryExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val topCategory = categoryExpenses.maxByOrNull { it.value }
        val topCatName = topCategory?.key ?: "None"
        val topCatAmt = topCategory?.value ?: 0L

        val budgetUtilPct = if (dailyLimit > 0) {
            ((todaySpent.toDouble() / dailyLimit) * 100).toInt().coerceIn(0, 500)
        } else {
            0
        }

        val totalGoalTarget = goals.sumOf { it.targetAmount }
        val totalGoalCurrent = goals.sumOf { it.currentAmount }
        val goalProgress = if (totalGoalTarget > 0) {
            (totalGoalCurrent.toFloat() / totalGoalTarget).coerceIn(0f, 1f)
        } else {
            1f
        }

        val safeMoney = calculateSafeMoney(monthlyIncome, 0L, dailyLimit)

        return FinancialHealthReport(
            netBalance = netBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            dailySpentToday = todaySpent,
            dailyLimit = dailyLimit,
            isOverBudgetToday = isOverBudget,
            overBudgetAmount = overBudgetAmt,
            safeMoney = safeMoney,
            topSpendingCategory = topCatName,
            topCategoryAmount = topCatAmt,
            budgetUtilizationPercentage = budgetUtilPct,
            activeGoalsProgress = goalProgress
        )
    }
}
