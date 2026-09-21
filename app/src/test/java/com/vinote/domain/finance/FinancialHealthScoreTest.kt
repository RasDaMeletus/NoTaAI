package com.vinote.domain.finance

import com.vinote.data.model.GoalItem
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialHealthScoreTest {

    @Test
    fun `calculateFinancialHealthScore returns high score and Grade S or A for disciplined finances`() {
        val transactions = listOf(
            TransactionItem(
                title = "Gaji",
                amount = 10000000L,
                category = "Income",
                type = TransactionType.INCOME,
                timestamp = System.currentTimeMillis()
            ),
            TransactionItem(
                title = "Makan",
                amount = 50000L,
                category = "Food",
                type = TransactionType.EXPENSE,
                timestamp = System.currentTimeMillis()
            )
        )

        val goals = listOf(
            GoalItem(
                title = "Dana Darurat",
                targetAmount = 5000000L,
                currentAmount = 4500000L
            )
        )

        val score = FinancialAnalyticsService.calculateFinancialHealthScore(
            transactions = transactions,
            goals = goals,
            dailyLimit = 180000L,
            monthlyIncome = 10000000L,
            startOfDayMs = System.currentTimeMillis() - 3600000L
        )

        assertTrue("Expected score >= 80, was ${score.score}", score.score >= 80)
        assertTrue("Expected grade S or A, was ${score.grade}", score.grade in listOf("S", "A"))
    }

    @Test
    fun `calculateFinancialHealthScore returns low score and Grade D when severely over budget`() {
        val transactions = listOf(
            TransactionItem(
                title = "Gaji",
                amount = 2000000L,
                category = "Income",
                type = TransactionType.INCOME,
                timestamp = System.currentTimeMillis()
            ),
            TransactionItem(
                title = "Belanja Mewah",
                amount = 5000000L,
                category = "Shopping",
                type = TransactionType.EXPENSE,
                timestamp = System.currentTimeMillis()
            )
        )

        val score = FinancialAnalyticsService.calculateFinancialHealthScore(
            transactions = transactions,
            goals = emptyList(),
            dailyLimit = 100000L,
            monthlyIncome = 2000000L,
            startOfDayMs = System.currentTimeMillis() - 3600000L
        )

        assertTrue("Expected score < 50, was ${score.score}", score.score < 50)
        assertEquals("D", score.grade)
    }
}
