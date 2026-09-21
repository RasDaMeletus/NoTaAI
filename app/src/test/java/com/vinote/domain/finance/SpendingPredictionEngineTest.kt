package com.vinote.domain.finance

import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SpendingPredictionEngineTest {

    @Test
    fun testEmptyTransactionsReturnsZeroProjection() {
        val result = SpendingPredictionEngine.predictSpending(emptyList())
        assertEquals(0L, result.weeklyProjectedExpense)
        assertNotNull(result.insightText)
    }

    @Test
    fun testPredictSpendingWithTransactionsIdentifiesPeakDay() {
        val cal = Calendar.getInstance()
        // Set to a Friday
        cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        val fridayTs = cal.timeInMillis

        // Set to a Monday
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val mondayTs = cal.timeInMillis

        val transactions = listOf(
            TransactionItem(id = 1L, title = "Makan Siang", amount = 20000L, category = "Food", type = TransactionType.EXPENSE, isConfirmed = true, timestamp = mondayTs),
            TransactionItem(id = 2L, title = "Kopi Santai", amount = 85000L, category = "Food", type = TransactionType.EXPENSE, isConfirmed = true, timestamp = fridayTs),
            TransactionItem(id = 3L, title = "Dinner Mewah", amount = 95000L, category = "Food", type = TransactionType.EXPENSE, isConfirmed = true, timestamp = fridayTs)
        )

        val result = SpendingPredictionEngine.predictSpending(transactions)

        assertTrue("Weekly projected expense should be positive", result.weeklyProjectedExpense > 0L)
        assertEquals("Jumat", result.topRiskDay)
        assertEquals("Food", result.topCategory)
        assertTrue(result.insightText.contains("Jumat"))
    }
}
