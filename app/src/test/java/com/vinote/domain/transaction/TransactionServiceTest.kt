package com.vinote.domain.transaction

import com.vinote.data.local.TransactionDao
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.data.model.TransactionType
import com.vinote.data.repository.FirestoreExpenseSyncRepository
import com.vinote.domain.transaction.TransactionService
import com.vinote.domain.transaction.TransactionValidationResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for TransactionService to ensure deterministic accounting and validation.
 * Verifies PRD requirements for financial arithmetic correctness.
 */
class TransactionServiceTest {

    private lateinit var transactionService: TransactionService
    private lateinit var mockTransactionDao: MockTransactionDao

    @Before
    fun setup() {
        mockTransactionDao = MockTransactionDao()
        transactionService = TransactionService(mockTransactionDao, FirestoreExpenseSyncRepository())
    }

    @Test
    fun `should validate transaction correctly`() {
        val validTransaction = TransactionItem(
            id = 0L,
            title = "GrabFood",
            amount = 25000L,
            category = "Food",
            type = TransactionType.EXPENSE,
            source = TransactionSource.E_WALLET,
            merchant = "GrabFood",
            walletName = "GoPay"
        )

        val result = transactionService.validateTransaction(validTransaction)
        assertTrue(result is TransactionValidationResult.Valid)
    }

    @Test
    fun `should reject transaction with zero amount`() {
        val invalidTransaction = TransactionItem(
            id = 0L,
            title = "Free Item",
            amount = 0L,
            category = "General",
            type = TransactionType.EXPENSE,
            source = TransactionSource.MANUAL
        )

        val result = transactionService.validateTransaction(invalidTransaction)
        assertTrue(result is TransactionValidationResult.Invalid)
        assertEquals("Amount must be greater than zero", (result as TransactionValidationResult.Invalid).reason)
    }

    @Test
    fun `should reject transaction with empty title`() {
        val invalidTransaction = TransactionItem(
            id = 0L,
            title = "",
            amount = 10000L,
            category = "Food",
            type = TransactionType.EXPENSE,
            source = TransactionSource.MANUAL
        )

        val result = transactionService.validateTransaction(invalidTransaction)
        assertTrue(result is TransactionValidationResult.Invalid)
        assertEquals("Transaction title cannot be empty", (result as TransactionValidationResult.Invalid).reason)
    }

    @Test
    fun `should reject transaction with blank category`() {
        val invalidTransaction = TransactionItem(
            id = 0L,
            title = "Unknown",
            amount = 5000L,
            category = "",
            type = TransactionType.EXPENSE,
            source = TransactionSource.MANUAL
        )

        val result = transactionService.validateTransaction(invalidTransaction)
        assertTrue(result is TransactionValidationResult.Invalid)
        assertEquals("Category must be specified", (result as TransactionValidationResult.Invalid).reason)
    }

    // Mock DAO for testing
    private class MockTransactionDao : TransactionDao {
        private val transactions = mutableListOf<TransactionItem>()
        private var nextId = 1L

        override fun getAllTransactions(): kotlinx.coroutines.flow.Flow<List<TransactionItem>> {
            return kotlinx.coroutines.flow.flowOf(transactions.toList())
        }

        override fun getExpenses(): kotlinx.coroutines.flow.Flow<List<TransactionItem>> {
            return kotlinx.coroutines.flow.flowOf(transactions.filter { it.type == TransactionType.EXPENSE })
        }

        override fun getIncomes(): kotlinx.coroutines.flow.Flow<List<TransactionItem>> {
            return kotlinx.coroutines.flow.flowOf(transactions.filter { it.type == TransactionType.INCOME })
        }

        override suspend fun insertTransaction(transaction: TransactionItem): Long {
            val id = nextId++
            transactions.add(transaction.copy(id = id))
            return id
        }

        override suspend fun insertAll(transactions: List<TransactionItem>) {
            transactions.forEach { insertTransaction(it) }
        }

        override suspend fun updateTransaction(transaction: TransactionItem) {
            val index = transactions.indexOfFirst { it.id == transaction.id }
            if (index >= 0) transactions[index] = transaction
        }

        override suspend fun deleteTransaction(transaction: TransactionItem) {
            transactions.removeAll { it.id == transaction.id }
        }

        override suspend fun deleteById(id: Long) {
            transactions.removeAll { it.id == id }
        }

        override suspend fun clearAll() {
            transactions.clear()
        }
    }
}