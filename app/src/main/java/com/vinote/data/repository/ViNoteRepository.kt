package com.example.data.repository

import com.example.data.auth.AuthRepository
import com.example.data.local.BudgetDao
import com.example.data.local.DetectionEventDao
import com.example.data.local.GoalDao
import com.example.data.local.SyncQueueDao
import com.example.data.local.TransactionDao
import com.example.data.local.UserSessionDao
import com.example.data.local.WalletAccountDao
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.DetectionEventEntity
import com.example.data.local.entities.DetectionStatus
import com.example.data.local.entities.SyncOperation
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.entities.WalletAccountEntity
import com.example.data.local.entities.WalletType
import com.example.data.model.GoalItem
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.data.sync.SyncSummary
import com.example.data.sync.ViNoteCloudSynchronizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single Unified Repository for ViNote 2.
 * Serves as the immediate Room source-of-truth with background cloud synchronization.
 */
class ViNoteRepository(
    val transactionDao: TransactionDao,
    val goalDao: GoalDao,
    val userSessionDao: UserSessionDao,
    val walletAccountDao: WalletAccountDao,
    val detectionEventDao: DetectionEventDao,
    val syncQueueDao: SyncQueueDao,
    val budgetDao: BudgetDao,
    val authRepository: AuthRepository,
    val cloudSynchronizer: ViNoteCloudSynchronizer
) {
    val allTransactions: Flow<List<TransactionItem>> = transactionDao.getAllTransactions()
    val allGoals: Flow<List<GoalItem>> = goalDao.getAllGoals()

    init {
        // Pre-populate only standard empty wallet profiles if none exist
        CoroutineScope(Dispatchers.IO).launch {
            val userId = authRepository.getCanonicalUserId()

            // Wallets initialization (Starts with clean 0 balances unless user reconciles or adds transactions)
            val existingWallets = walletAccountDao.getWalletsForUser(userId)
            if (existingWallets.isEmpty()) {
                val starterWallets = listOf(
                    WalletAccountEntity(
                        id = "w_bca",
                        userId = userId,
                        name = "Bank Central Asia (BCA)",
                        type = WalletType.BANK,
                        calculatedBalance = 0L,
                        providerReportedBalance = 0L,
                        isAutoDetectEnabled = true,
                        iconColorHex = "#003893",
                        accountNumber = "•••• 8821",
                        isConnected = true
                    ),
                    WalletAccountEntity(
                        id = "w_mandiri",
                        userId = userId,
                        name = "Bank Mandiri (Livin')",
                        type = WalletType.BANK,
                        calculatedBalance = 0L,
                        providerReportedBalance = 0L,
                        isAutoDetectEnabled = true,
                        iconColorHex = "#002B66",
                        accountNumber = "•••• 4102",
                        isConnected = true
                    ),
                    WalletAccountEntity(
                        id = "w_gopay",
                        userId = userId,
                        name = "GoPay",
                        type = WalletType.EWALLET,
                        calculatedBalance = 0L,
                        providerReportedBalance = 0L,
                        isAutoDetectEnabled = true,
                        iconColorHex = "#00B14F",
                        accountNumber = "0812-****-8821",
                        isConnected = true
                    ),
                    WalletAccountEntity(
                        id = "w_ovo",
                        userId = userId,
                        name = "OVO",
                        type = WalletType.EWALLET,
                        calculatedBalance = 0L,
                        providerReportedBalance = 0L,
                        isAutoDetectEnabled = true,
                        iconColorHex = "#4C3494",
                        accountNumber = "0812-****-8821",
                        isConnected = true
                    ),
                    WalletAccountEntity(
                        id = "w_dana",
                        userId = userId,
                        name = "DANA",
                        type = WalletType.EWALLET,
                        calculatedBalance = 0L,
                        providerReportedBalance = 0L,
                        isAutoDetectEnabled = true,
                        iconColorHex = "#118EEA",
                        accountNumber = "0812-****-8821",
                        isConnected = true
                    )
                )
                walletAccountDao.insertAll(starterWallets)
            }

            // Budget
            val existingBudget = budgetDao.getBudget(userId)
            if (existingBudget == null) {
                budgetDao.saveBudget(
                    BudgetEntity(
                        id = "budget_$userId",
                        userId = userId,
                        monthlyLimit = 5000000L,
                        dailyLimit = 150000L
                    )
                )
            }
        }
    }

    fun getWalletsFlow(userId: String): Flow<List<WalletAccountEntity>> {
        return walletAccountDao.getWalletsForUserFlow(userId)
    }

    fun getDetectionEventsFlow(userId: String): Flow<List<DetectionEventEntity>> {
        return detectionEventDao.getAllDetectionEventsFlow(userId)
    }

    fun getPendingTransactionsFlow(userId: String): Flow<List<TransactionItem>> {
        return transactionDao.getPendingTransactionsForUser(userId)
    }

    suspend fun insertTransaction(transaction: TransactionItem): Long {
        val insertedId = transactionDao.insertTransaction(transaction)
        val itemToSync = if (transaction.id == 0L) transaction.copy(id = insertedId) else transaction

        // Queue for synchronization
        syncQueueDao.enqueue(
            SyncQueueEntity(
                userId = transaction.userId,
                entityType = "TRANSACTION",
                entityId = insertedId.toString(),
                operation = SyncOperation.INSERT,
                payloadJson = """{"id":$insertedId,"title":"${transaction.title}","amount":${transaction.amount}}"""
            )
        )
        return insertedId
    }

    suspend fun confirmPendingTransaction(id: Long) {
        transactionDao.confirmTransaction(id)
    }

    suspend fun updateTransaction(transaction: TransactionItem) {
        transactionDao.updateTransaction(transaction)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                userId = transaction.userId,
                entityType = "TRANSACTION",
                entityId = transaction.id.toString(),
                operation = SyncOperation.UPDATE,
                payloadJson = """{"id":${transaction.id},"title":"${transaction.title}","amount":${transaction.amount}}"""
            )
        )
    }

    suspend fun deleteTransaction(id: Long, userId: String = "user_default") {
        transactionDao.deleteById(id)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                userId = userId,
                entityType = "TRANSACTION",
                entityId = id.toString(),
                operation = SyncOperation.DELETE,
                payloadJson = """{"id":$id}"""
            )
        )
    }

    suspend fun clearAllTransactions(userId: String) {
        transactionDao.clearUserTransactions(userId)
    }

    suspend fun toggleWalletAutoDetect(walletId: String, userId: String, isEnabled: Boolean) {
        walletAccountDao.setAutoDetectEnabled(walletId, userId, isEnabled)
    }

    suspend fun reconcileWalletBalance(walletId: String, userId: String, balance: Long) {
        walletAccountDao.reconcileWalletBalance(walletId, userId, balance)
    }

    suspend fun syncWithCloud(): SyncSummary {
        return cloudSynchronizer.performFullSync()
    }

    suspend fun insertWallet(wallet: WalletAccountEntity) {
        walletAccountDao.insertWallet(wallet)
    }

    suspend fun updateWallet(wallet: WalletAccountEntity) {
        walletAccountDao.updateWallet(wallet)
    }

    suspend fun deleteWallet(walletId: String, userId: String) {
        walletAccountDao.deleteWallet(walletId, userId)
    }

    fun getBudgetFlow(userId: String): Flow<BudgetEntity?> {
        return budgetDao.getBudgetFlow(userId)
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        budgetDao.saveBudget(budget)
    }

    suspend fun clearAllData(userId: String) {
        transactionDao.clearUserTransactions(userId)
        detectionEventDao.clearUserEvents(userId)
        goalDao.clearAll()
        // Reset balances on existing wallets to 0
        val wallets = walletAccountDao.getWalletsForUser(userId)
        val resetWallets = wallets.map { it.copy(calculatedBalance = 0L, providerReportedBalance = 0L) }
        walletAccountDao.insertAll(resetWallets)
    }

    suspend fun insertGoal(goal: GoalItem): Long {
        return goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalItem) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(id: Long) {
        goalDao.deleteById(id)
    }
}
