package com.vinote.domain.finance

import com.vinote.data.local.entities.SpendingPredictionEntity
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import com.vinote.ui.components.FormatUtils
import java.util.Calendar
import java.util.UUID

data class SpendingPredictionResult(
    val weeklyProjectedExpense: Long,
    val topRiskDay: String,
    val topCategory: String,
    val insightText: String
)

object SpendingPredictionEngine {

    private val DAY_NAMES = mapOf(
        Calendar.SUNDAY to "Minggu",
        Calendar.MONDAY to "Senin",
        Calendar.TUESDAY to "Selasa",
        Calendar.WEDNESDAY to "Rabu",
        Calendar.THURSDAY to "Kamis",
        Calendar.FRIDAY to "Jumat",
        Calendar.SATURDAY to "Sabtu"
    )

    fun predictSpending(transactions: List<TransactionItem>): SpendingPredictionResult {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE && it.isConfirmed }

        if (expenses.isEmpty()) {
            return SpendingPredictionResult(
                weeklyProjectedExpense = 0L,
                topRiskDay = "Akhir Pekan",
                topCategory = "Umum",
                insightText = "Catat beberapa transaksi untuk melihat pola dan prakiraan pengeluaran mingguanmu ✨"
            )
        }

        val cal = Calendar.getInstance()

        // Group expenses by Day of Week
        val dayExpenses = mutableMapOf<Int, MutableList<Long>>()
        val dayCategories = mutableMapOf<Int, MutableMap<String, Long>>()

        for (tx in expenses) {
            cal.timeInMillis = tx.timestamp
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            dayExpenses.getOrPut(dayOfWeek) { mutableListOf() }.add(tx.amount)

            val catMap = dayCategories.getOrPut(dayOfWeek) { mutableMapOf() }
            catMap[tx.category] = (catMap[tx.category] ?: 0L) + tx.amount
        }

        // Calculate average per day
        val dayAverages = dayExpenses.mapValues { (_, amounts) ->
            if (amounts.isNotEmpty()) amounts.sum() / amounts.size else 0L
        }

        // Daily average overall burn rate
        val avgDailyBurn = if (dayAverages.isNotEmpty()) dayAverages.values.average().toLong() else 25000L
        val weeklyProjected = avgDailyBurn * 7

        // Peak / Highest risk day
        val peakDayEntry = dayAverages.maxByOrNull { it.value }
        val peakDayCode = peakDayEntry?.key ?: Calendar.FRIDAY
        val peakDayName = DAY_NAMES[peakDayCode] ?: "Jumat"
        val peakDayAvg = peakDayEntry?.value ?: avgDailyBurn

        // Top category on peak day
        val peakCatMap = dayCategories[peakDayCode] ?: emptyMap()
        val topCat = peakCatMap.maxByOrNull { it.value }?.key ?: "Kebutuhan"

        val insight = if (expenses.size >= 3) {
            "Berdasarkan pola historis, hari $peakDayName adalah hari tersibukmu dengan rata-rata ${FormatUtils.formatRupiah(peakDayAvg)} (terbanyak di kategori $topCat)."
        } else {
            "Prakiraan pengeluaran 7 hari ke depan: ~${FormatUtils.formatRupiah(weeklyProjected)}."
        }

        return SpendingPredictionResult(
            weeklyProjectedExpense = weeklyProjected,
            topRiskDay = peakDayName,
            topCategory = topCat,
            insightText = insight
        )
    }

    fun toEntity(result: SpendingPredictionResult): SpendingPredictionEntity {
        return SpendingPredictionEntity(
            id = UUID.randomUUID().toString(),
            predictionDate = System.currentTimeMillis(),
            categoryId = result.topCategory,
            predictedAmountCents = result.weeklyProjectedExpense,
            riskDay = result.topRiskDay,
            insightText = result.insightText,
            generatedAt = System.currentTimeMillis()
        )
    }
}
