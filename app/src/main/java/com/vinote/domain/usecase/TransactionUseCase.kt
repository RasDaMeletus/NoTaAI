package com.vinote.domain.usecase

import com.vinote.data.local.TransactionDao
import com.vinote.data.local.TransactionTemplateDao
import com.vinote.data.local.RecurringTransactionDao
import com.vinote.data.local.entities.TransactionTemplateEntity
import com.vinote.data.local.entities.RecurringTransactionEntity
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import com.vinote.data.model.TransactionSource
import com.vinote.domain.transaction.TransactionService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionUseCaseInterface {
    fun getAllTransactions(): Flow<List<TransactionItem>>
    fun getTemplates(userId: String): Flow<List<TransactionTemplateEntity>>
    fun getRecurringTransactions(userId: String): Flow<List<RecurringTransactionEntity>>
    suspend fun addTransaction(
        title: String,
        amount: Long,
        category: String,
        type: TransactionType,
        source: TransactionSource = TransactionSource.MANUAL,
        merchant: String = "",
        walletName: String? = null
    )
    suspend fun deleteTransaction(id: Long)
    suspend fun clearAll()
    suspend fun insertTemplate(template: TransactionTemplateEntity)
    suspend fun deleteTemplate(id: String)
    suspend fun incrementTemplateUsage(id: String)
    suspend fun insertRecurring(recurring: RecurringTransactionEntity)
    suspend fun deleteRecurring(id: String)
}

@Singleton
class TransactionUseCaseImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val templateDao: TransactionTemplateDao,
    private val recurringDao: RecurringTransactionDao,
    private val transactionService: TransactionService
) : TransactionUseCaseInterface {

    override fun getAllTransactions(): Flow<List<TransactionItem>> =
        transactionService.allTransactions

    override fun getTemplates(userId: String): Flow<List<TransactionTemplateEntity>> =
        templateDao.getTemplatesForUser(userId)

    override fun getRecurringTransactions(userId: String): Flow<List<RecurringTransactionEntity>> =
        recurringDao.getRecurringForUser(userId)

    override suspend fun addTransaction(
        title: String,
        amount: Long,
        category: String,
        type: TransactionType,
        source: TransactionSource,
        merchant: String,
        walletName: String?
    ) {
        transactionService.addTransaction(
            title = title,
            amount = amount,
            category = category,
            type = type,
            source = source,
            merchant = merchant,
            walletName = walletName
        )
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    override suspend fun clearAll() {
        transactionService.clearAll()
    }

    override suspend fun insertTemplate(template: TransactionTemplateEntity) {
        templateDao.insertTemplate(template)
    }

    override suspend fun deleteTemplate(id: String) {
        templateDao.deleteById(id)
    }

    override suspend fun incrementTemplateUsage(id: String) {
        templateDao.incrementUsage(id)
    }

    override suspend fun insertRecurring(recurring: RecurringTransactionEntity) {
        recurringDao.insertRecurring(recurring)
    }

    override suspend fun deleteRecurring(id: String) {
        recurringDao.deleteById(id)
    }
}