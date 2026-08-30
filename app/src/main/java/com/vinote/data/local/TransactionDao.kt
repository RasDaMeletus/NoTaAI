package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vinote.data.model.TransactionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isConfirmed = 1 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isConfirmed = 1 ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: String): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isConfirmed = 0 ORDER BY timestamp DESC")
    fun getPendingTransactionsForUser(userId: String): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' AND isConfirmed = 1 ORDER BY timestamp DESC")
    fun getExpenses(): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transactions WHERE type = 'INCOME' AND isConfirmed = 1 ORDER BY timestamp DESC")
    fun getIncomes(): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transactions WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): TransactionItem?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionItem?

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'EXPENSE' AND isConfirmed = 1 AND timestamp >= :startTimestamp")
    suspend fun getExpenseSumSince(userId: String, startTimestamp: Long): Long?

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'INCOME' AND isConfirmed = 1")
    suspend fun getTotalIncome(userId: String): Long?

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'EXPENSE' AND isConfirmed = 1")
    suspend fun getTotalExpense(userId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionItem>)

    @Update
    suspend fun updateTransaction(transaction: TransactionItem)

    @Query("UPDATE transactions SET isConfirmed = 1 WHERE id = :id")
    suspend fun confirmTransaction(id: Long)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionItem)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE fingerprint = :fingerprint")
    suspend fun deleteByFingerprint(fingerprint: String)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun clearUserTransactions(userId: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

