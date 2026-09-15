package com.vinote.domain.transaction

import com.vinote.data.local.TransactionDao
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.data.model.TransactionType
import com.vinote.data.repository.FirestoreExpenseSyncRepository
import com.vinote.domain.finance.FinancialAnalyticsService
import com.vinote.domain.notification.FinancialNotificationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single Unified Transaction Service for ViNote.
 * All transaction creation, modification, validation, balance calculations,
 * deduplication, and sync triggers must pass through this service.
 */
class TransactionService(
    private val transactionDao: TransactionDao,
    private val firestoreSyncRepository: FirestoreExpenseSyncRepository = FirestoreExpenseSyncRepository(),
    private val notificationEngine: FinancialNotificationEngine? = null,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var userId: String = ""

    fun setUserId(id: String) { userId = id }

    val allTransactions: Flow<List<TransactionItem>> = transactionDao.getAllTransactions()

    fun validateTransaction(transaction: TransactionItem): TransactionValidationResult {
        if (transaction.amount <= 0) {
            return TransactionValidationResult.Invalid("Amount must be greater than zero")
        }
        if (transaction.title.isBlank()) {
            return TransactionValidationResult.Invalid("Transaction title cannot be empty")
        }
        if (transaction.category.isBlank()) {
            return TransactionValidationResult.Invalid("Category must be specified")
        }
        return TransactionValidationResult.Valid
    }

    suspend fun addTransaction(
        title: String,
        amount: Long,
        category: String,
        type: TransactionType = TransactionType.EXPENSE,
        source: TransactionSource = TransactionSource.MANUAL,
        merchant: String = "",
        walletName: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        timeLabel: String = "Just now"
    ): Result<Long> = withContext(Dispatchers.IO) {
        val transaction = TransactionItem(
            id = 0L,
            title = title.trim(),
            amount = amount,
            category = category.trim().ifBlank { "General" },
            type = type,
            timestamp = timestamp,
            timeLabel = timeLabel,
            merchant = merchant.trim(),
            source = source,
            walletName = walletName,
            userId = userId,
            syncState = "PENDING_SYNC"
        )

        when (val validation = validateTransaction(transaction)) {
            is TransactionValidationResult.Invalid -> return@withContext Result.failure(IllegalArgumentException(validation.reason))
            TransactionValidationResult.Valid -> {
                val insertedId = transactionDao.insertTransaction(transaction)
                val itemWithId = transaction.copy(id = insertedId)

                // Trigger cloud sync asynchronously (local-first, non-blocking)
                externalScope.launch {
                    try {
                        firestoreSyncRepository.syncLocalToRemote(listOf(itemWithId))
                    } catch (_: Exception) {}
                }

                // If automated or e-wallet, emit notification event
                if (source == TransactionSource.E_WALLET || source == TransactionSource.AUTO_DETECTED || source == TransactionSource.BANK_SYNC) {
                    notificationEngine?.notifyTransactionDetected(itemWithId)
                }

                Result.success(insertedId)
            }
        }
    }

    suspend fun deleteTransaction(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteById(id)
            externalScope.launch {
                try {
                    firestoreSyncRepository.deleteRemoteExpense(id)
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateCurrentBalance(): Long = withContext(Dispatchers.IO) {
        val list = transactionDao.getAllTransactions().first()
        FinancialAnalyticsService.calculateNetBalance(list)
    }

    suspend fun calculateTotalIncome(): Long = withContext(Dispatchers.IO) {
        val list = transactionDao.getAllTransactions().first()
        FinancialAnalyticsService.calculateTotalIncome(list)
    }

    suspend fun calculateTotalExpense(): Long = withContext(Dispatchers.IO) {
        val list = transactionDao.getAllTransactions().first()
        FinancialAnalyticsService.calculateTotalExpense(list)
    }

    suspend fun clearAll(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.clearAll()
            externalScope.launch {
                try {
                    firestoreSyncRepository.clearAllRemoteExpenses()
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
