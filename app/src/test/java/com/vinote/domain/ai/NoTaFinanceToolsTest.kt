package com.vinote.domain.ai

import com.vinote.data.local.BudgetDao
import com.vinote.data.local.GoalDao
import com.vinote.data.local.TransactionDao
import com.vinote.data.local.WalletAccountDao
import com.vinote.data.local.entities.BudgetEntity
import com.vinote.data.local.entities.WalletAccountEntity
import com.vinote.data.model.GoalItem
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTransactionDao(val txs: MutableList<TransactionItem> = mutableListOf()) : TransactionDao {
    override fun getAllTransactions(): Flow<List<TransactionItem>> = flowOf(txs)
    override fun getTransactionsForUser(userId: String): Flow<List<TransactionItem>> = flowOf(txs.filter { it.userId == userId })
    override fun getPendingTransactionsForUser(userId: String): Flow<List<TransactionItem>> = flowOf(emptyList())
    override fun getExpenses(): Flow<List<TransactionItem>> = flowOf(txs.filter { it.type == TransactionType.EXPENSE })
    override fun getIncomes(): Flow<List<TransactionItem>> = flowOf(txs.filter { it.type == TransactionType.INCOME })
    override suspend fun findByFingerprint(fingerprint: String): TransactionItem? = null
    override suspend fun getTransactionById(id: Long): TransactionItem? = null
    override suspend fun getExpenseSumSince(userId: String, startTimestamp: Long): Long = txs.filter { it.userId == userId && it.type == TransactionType.EXPENSE && it.timestamp >= startTimestamp }.sumOf { it.amount }
    override suspend fun getTotalIncome(userId: String): Long = txs.filter { it.userId == userId && it.type == TransactionType.INCOME }.sumOf { it.amount }
    override suspend fun getTotalExpense(userId: String): Long = txs.filter { it.userId == userId && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    override suspend fun insertTransaction(transaction: TransactionItem): Long = 1L
    override suspend fun insertAll(transactions: List<TransactionItem>) {}
    override suspend fun updateTransaction(transaction: TransactionItem) {}
    override suspend fun confirmTransaction(id: Long) {}
    override suspend fun deleteTransaction(transaction: TransactionItem) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun deleteByFingerprint(fingerprint: String) {}
    override suspend fun clearUserTransactions(userId: String) {}
    override suspend fun clearAll() {}
}

class FakeGoalDao : GoalDao {
    override fun getAllGoals(): Flow<List<GoalItem>> = flowOf(emptyList())
    override fun getGoalsForUser(userId: String): Flow<List<GoalItem>> = flowOf(emptyList())
    override suspend fun getGoalById(id: Long, userId: String): GoalItem? = null
    override suspend fun insertGoal(goal: GoalItem): Long = 1L
    override suspend fun insertAll(goals: List<GoalItem>) {}
    override suspend fun updateGoal(goal: GoalItem) {}
    override suspend fun deleteGoal(goal: GoalItem) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun clearUserGoals(userId: String) {}
    override suspend fun clearAll() {}
}

class FakeWalletAccountDao : WalletAccountDao {
    override fun getWalletsForUserFlow(userId: String): Flow<List<WalletAccountEntity>> = flowOf(emptyList())
    override fun getAllWalletsFlow(): Flow<List<WalletAccountEntity>> = flowOf(emptyList())
    override suspend fun getWalletsForUser(userId: String): List<WalletAccountEntity> = emptyList()
    override suspend fun getWalletById(id: String, userId: String): WalletAccountEntity? = null
    override suspend fun getWalletByName(name: String, userId: String): WalletAccountEntity? = null
    override suspend fun insertWallet(wallet: WalletAccountEntity) {}
    override suspend fun insertAll(wallets: List<WalletAccountEntity>) {}
    override suspend fun updateWallet(wallet: WalletAccountEntity) {}
    override suspend fun updateCalculatedBalance(id: String, userId: String, balance: Long, timestamp: Long) {}
    override suspend fun updateProviderReportedBalance(id: String, userId: String, reportedBalance: Long, timestamp: Long) {}
    override suspend fun reconcileWalletBalance(id: String, userId: String, reconciledBalance: Long, timestamp: Long) {}
    override suspend fun setAutoDetectEnabled(id: String, userId: String, isEnabled: Boolean) {}
    override suspend fun deleteWallet(id: String, userId: String) {}
    override suspend fun clearUserWallets(userId: String) {}
    override suspend fun getAllWalletsWithGateway(): List<WalletAccountEntity> = emptyList()
    override fun getAllWalletsWithGatewayFlow(): Flow<List<WalletAccountEntity>> = flowOf(emptyList())
    override suspend fun updateBalance(id: String, balance: Double) {}
}

class FakeBudgetDao : BudgetDao {
    override fun getBudgetFlow(userId: String): Flow<BudgetEntity?> = flowOf(null)
    override suspend fun getBudget(userId: String): BudgetEntity? = null
    override suspend fun saveBudget(budget: BudgetEntity) {}
    override suspend fun updateBudget(budget: BudgetEntity) {}
    override suspend fun deleteUserBudget(userId: String) {}
}

class NoTaFinanceToolsTest {

    private lateinit var tools: NoTaFinanceTools
    private lateinit var fakeTxDao: FakeTransactionDao

    @Before
    fun setup() {
        fakeTxDao = FakeTransactionDao()
        tools = NoTaFinanceTools(
            transactionDao = fakeTxDao,
            goalDao = FakeGoalDao(),
            walletAccountDao = FakeWalletAccountDao(),
            budgetDao = FakeBudgetDao()
        )
    }

    @Test
    fun testSearchTransactionsFindsMatchingKeywords() = runBlocking {
        fakeTxDao.txs.addAll(
            listOf(
                TransactionItem(id = 1L, userId = "user1", title = "Kopi Kenangan", amount = 25000L, category = "Food", type = TransactionType.EXPENSE, merchant = "Kenangan"),
                TransactionItem(id = 2L, userId = "user1", title = "Bensin Pertamina", amount = 50000L, category = "Transport", type = TransactionType.EXPENSE, merchant = "SPBU"),
                TransactionItem(id = 3L, userId = "user1", title = "Gaji", amount = 5000000L, category = "Income", type = TransactionType.INCOME)
            )
        )

        val results = tools.searchTransactions("user1", "kopi")
        assertEquals(1, results.size)
        assertEquals("Kopi Kenangan", results.first().title)

        val foodResults = tools.searchTransactions("user1", "Food")
        assertEquals(1, foodResults.size)
    }

    @Test
    fun testGetSpendingByCategoryAggregatesCorrectly() = runBlocking {
        fakeTxDao.txs.addAll(
            listOf(
                TransactionItem(id = 1L, userId = "user1", title = "Makan", amount = 30000L, category = "Food", type = TransactionType.EXPENSE),
                TransactionItem(id = 2L, userId = "user1", title = "Kopi", amount = 20000L, category = "Food", type = TransactionType.EXPENSE),
                TransactionItem(id = 3L, userId = "user1", title = "Pulsa", amount = 100000L, category = "Bills", type = TransactionType.EXPENSE)
            )
        )

        val categoryMap = tools.getSpendingByCategory("user1")
        assertEquals(50000L, categoryMap["Food"])
        assertEquals(100000L, categoryMap["Bills"])
    }

    @Test
    fun testCalculateSavingsRate() = runBlocking {
        fakeTxDao.txs.addAll(
            listOf(
                TransactionItem(id = 1L, userId = "user1", title = "Gaji", amount = 10000000L, category = "Income", type = TransactionType.INCOME),
                TransactionItem(id = 2L, userId = "user1", title = "Pengeluaran", amount = 7000000L, category = "Bills", type = TransactionType.EXPENSE)
            )
        )

        val rate = tools.calculateSavingsRate("user1")
        assertEquals(30.0, rate, 0.01)
    }

    @Test
    fun testBudgetSuggestionRule503020() {
        val suggestion = tools.createBudgetSuggestion(10000000L)
        assertTrue(suggestion.contains("5.000.000") || suggestion.contains("5000000"))
        assertTrue(suggestion.contains("3.000.000") || suggestion.contains("3000000"))
        assertTrue(suggestion.contains("2.000.000") || suggestion.contains("2000000"))
    }
}
