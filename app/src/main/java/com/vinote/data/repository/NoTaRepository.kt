package com.vinote.data.repository

import com.vinote.data.repository.AuthRepository
import com.vinote.data.local.BudgetDao
import com.vinote.data.local.DetectionEventDao
import com.vinote.data.local.GoalDao
import com.vinote.data.local.SyncQueueDao
import com.vinote.data.local.TransactionDao
import com.vinote.data.local.UserSessionDao
import com.vinote.data.local.WalletAccountDao
import com.vinote.data.local.entities.BudgetEntity
import com.vinote.data.local.entities.DetectionEventEntity
import com.vinote.data.local.entities.DetectionStatus
import com.vinote.data.local.entities.SyncOperation
import com.vinote.data.local.entities.SyncQueueEntity
import com.vinote.data.local.entities.WalletAccountEntity
import com.vinote.data.local.entities.WalletType
import com.vinote.data.model.GoalItem
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.data.model.TransactionType
import com.vinote.data.sync.SyncSummary
import com.vinote.data.sync.NoTaCloudSynchronizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single Unified Repository for NoTa 2.
 * Serves as the immediate Room source-of-truth with background cloud synchronization.
 */
class NoTaRepository(
    val transactionDao: TransactionDao,
    val goalDao: GoalDao,
    val userSessionDao: UserSessionDao,
    val walletAccountDao: WalletAccountDao,
    val detectionEventDao: DetectionEventDao,
    val syncQueueDao: SyncQueueDao,
    val budgetDao: BudgetDao,
    val authRepository: AuthRepository,
    val cloudSynchronizer: NoTaCloudSynchronizer,
) {
    val allTransactions: Flow<List<TransactionItem>> = transactionDao.getAllTransactions()
    val allGoals: Flow<List<GoalItem>> = goalDao.getAllGoals()

    

    fun getWalletsFlow(userId: String): Flow<List<WalletAccountEntity>> {
        return walletAccountDao.getWalletsForUserFlow(userId)
    }

    fun getGoalsFlow(userId: String): Flow<List<GoalItem>> {
        return goalDao.getGoalsForUser(userId)
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

        // Update wallet balance if walletName is provided
        transaction.walletName?.let { walletName ->
            updateWalletBalanceForTransaction(walletName, transaction.userId, transaction.amount, transaction.type)
        }

        // Queue for synchronization. Column names/types must match the
        // public.transactions table exactly or PostgREST rejects the push.
        syncQueueDao.enqueue(
            SyncQueueEntity(
                userId = transaction.userId,
                entityType = "TRANSACTION",
                entityId = insertedId.toString(),
                operation = SyncOperation.INSERT,
                payloadJson = transaction.toSupabaseJson()
            )
        )
        return insertedId
    }

    private suspend fun updateWalletBalanceForTransaction(
        walletName: String,
        userId: String,
        amount: Long,
        type: com.vinote.data.model.TransactionType
    ) {
        val wallet = walletAccountDao.getWalletByName(walletName, userId)
        if (wallet != null) {
            val balanceChange = if (type == com.vinote.data.model.TransactionType.INCOME) amount else -amount
            val newBalance = wallet.calculatedBalance + balanceChange
            val timestamp = System.currentTimeMillis()
            val updatedWallet = wallet.copy(calculatedBalance = newBalance, lastSyncTimestamp = timestamp)
            walletAccountDao.insertWallet(updatedWallet)
        }
    }

    suspend fun confirmPendingTransaction(id: Long) {
        transactionDao.confirmTransaction(id)
    }

    suspend fun updateTransaction(transaction: TransactionItem) {
        // Get the old transaction to calculate balance difference
        val oldTransaction = transactionDao.getTransactionById(transaction.id)
        
        transactionDao.updateTransaction(transaction)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                userId = transaction.userId,
                entityType = "TRANSACTION",
                entityId = transaction.id.toString(),
                operation = SyncOperation.UPDATE,
                payloadJson = transaction.toSupabaseJson()
            )
        )

        // Update wallet balance if wallet changed or amount/type changed
        if (oldTransaction != null) {
            val oldWalletName = oldTransaction.walletName
            val newWalletName = transaction.walletName
            
            // If wallet changed, revert old wallet and apply to new wallet
            if (oldWalletName != newWalletName) {
                oldWalletName?.let { name ->
                    val balanceChange = if (oldTransaction.type == TransactionType.INCOME) -oldTransaction.amount else oldTransaction.amount
                    updateWalletBalanceForTransaction(name, transaction.userId, oldTransaction.amount, 
                        if (oldTransaction.type == TransactionType.INCOME) TransactionType.EXPENSE else TransactionType.INCOME)
                }
                newWalletName?.let { name ->
                    updateWalletBalanceForTransaction(name, transaction.userId, transaction.amount, transaction.type)
                }
            } else if (oldTransaction.amount != transaction.amount || oldTransaction.type != transaction.type) {
                // Same wallet, but amount or type changed - calculate difference
                newWalletName?.let { name ->
                    val oldEffect = if (oldTransaction.type == TransactionType.INCOME) oldTransaction.amount else -oldTransaction.amount
                    val newEffect = if (transaction.type == TransactionType.INCOME) transaction.amount else -transaction.amount
                    val diff = newEffect - oldEffect
                    if (diff != 0L) {
                        val wallet = walletAccountDao.getWalletByName(name, transaction.userId)
                        if (wallet != null) {
                            val newBalance = wallet.calculatedBalance + diff
                            val timestamp = System.currentTimeMillis()
                            val updatedWallet = wallet.copy(calculatedBalance = newBalance, lastSyncTimestamp = timestamp)
                            walletAccountDao.insertWallet(updatedWallet)
                        }
                    }
                }
            }
        } else {
            // No old transaction found, just apply new one
            transaction.walletName?.let { walletName ->
                updateWalletBalanceForTransaction(walletName, transaction.userId, transaction.amount, transaction.type)
            }
        }
    }

    suspend fun deleteTransaction(id: Long, userId: String = "") {
        // Get transaction before deleting to update wallet balance
        val transaction = transactionDao.getTransactionById(id)

        transactionDao.deleteById(id)
        // The sync push filters remote rows by user_id, so this must be the
        // transaction's real owner, not the caller's convenience default.
        val owner = transaction?.userId?.ifBlank { userId } ?: userId
        syncQueueDao.enqueue(
            SyncQueueEntity(
                userId = owner,
                entityType = "TRANSACTION",
                entityId = id.toString(),
                operation = SyncOperation.DELETE,
                payloadJson = """{"id":$id}"""
            )
        )

        // Revert wallet balance
        transaction?.let { tx ->
            tx.walletName?.let { walletName ->
                val balanceChange = if (tx.type == TransactionType.INCOME) -tx.amount else tx.amount
                val wallet = walletAccountDao.getWalletByName(walletName, userId)
                if (wallet != null) {
                    val newBalance = wallet.calculatedBalance + balanceChange
                    val timestamp = System.currentTimeMillis()
                    val updatedWallet = wallet.copy(calculatedBalance = newBalance, lastSyncTimestamp = timestamp)
                    walletAccountDao.insertWallet(updatedWallet)
                }
            }
        }
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
        // Enqueue delete sync
        syncQueueDao.enqueue(
            SyncQueueEntity(
                userId = userId,
                entityType = "WALLET",
                entityId = walletId,
                operation = SyncOperation.DELETE,
                payloadJson = """{"id":"$walletId"}"""
            )
        )
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
