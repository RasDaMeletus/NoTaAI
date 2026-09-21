package com.vinote.services.recurring

import android.util.Log
import com.vinote.data.local.RecurringTransactionDao
import com.vinote.data.local.TransactionDao
import com.vinote.data.local.entities.RecurringFrequency
import com.vinote.data.local.entities.RecurringTransactionEntity
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.domain.notification.FinancialNotificationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class RecurringTransactionScheduler(
    private val recurringTransactionDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao,
    private val notificationEngine: FinancialNotificationEngine? = null
) {
    suspend fun processDueTransactions(userId: String = ""): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dueItems = recurringTransactionDao.getDueRecurringTransactions(now)
        var processedCount = 0

        for (item in dueItems) {
            try {
                // Record the actual transaction in ledger
                val tx = TransactionItem(
                    userId = item.userId,
                    title = item.title,
                    amount = item.amount,
                    category = item.category,
                    type = item.type,
                    timestamp = now,
                    timeLabel = "Today",
                    merchant = item.walletName ?: "Recurring",
                    source = TransactionSource.MANUAL,
                    walletName = item.walletName,
                    isConfirmed = true,
                    syncState = "PENDING_SYNC"
                )
                val insertedId = transactionDao.insertTransaction(tx)

                // Calculate next due date
                val nextDue = calculateNextDueDate(item.nextDueDate, item.frequency)
                val updatedItem = item.copy(
                    nextDueDate = nextDue,
                    lastExecutedAt = now
                )
                recurringTransactionDao.updateRecurring(updatedItem)
                processedCount++

                notificationEngine?.notifyRecurringExecuted(tx)
                Log.i("RecurringScheduler", "Executed recurring tx: ${item.title} (ID $insertedId)")
            } catch (e: Exception) {
                Log.e("RecurringScheduler", "Error executing recurring item ${item.id}", e)
            }
        }
        processedCount
    }

    private fun calculateNextDueDate(currentDueDate: Long, frequency: RecurringFrequency): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentDueDate
        }
        when (frequency) {
            RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }
}
