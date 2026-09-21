package com.vinote.domain.transaction

import com.vinote.data.local.TransactionDao
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.data.model.TransactionType
import com.vinote.domain.finance.FinancialAnalyticsService
import com.vinote.domain.notification.FinancialNotificationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single Unified Transaction Service for ViNote.
 * All transaction creation, modification, validation, balance calculations,
 * deduplication, and sync triggers must pass through this service.
 */
class TransactionService(
    private val transactionDao: TransactionDao,
    private val notificationEngine: FinancialNotificationEngine? = null,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var userId: String = ""

    fun setUserId(id: String) { userId = id }

    val allTransactions: Flow<List<TransactionItem>> = transactionDao.getAllTransactions()
        .map { transactions ->
            val now = System.currentTimeMillis()
            transactions.map { transaction ->
                transaction.copy(timeLabel = formatRelativeTime(transaction.timestamp, now))
            }
        }

    private fun formatRelativeTime(timestamp: Long, now: Long): String {
        if (timestamp <= 0L) return "Unknown time"
        val elapsed = (now - timestamp).coerceAtLeast(0L)
        val minute = 60_000L
        val hour = 60L * minute
        val day = 24L * hour
        return when {
            elapsed < minute -> "Just now"
            elapsed < hour -> "${elapsed / minute} min ago"
            elapsed < day -> "${elapsed / hour} hr ago"
            elapsed < 2L * day -> "Yesterday"
            else -> SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(Date(timestamp))
        }
    }

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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
