package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.WalletAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletAccountDao {
    @Query("SELECT * FROM wallet_accounts WHERE userId = :userId ORDER BY name ASC")
    fun getWalletsForUserFlow(userId: String): Flow<List<WalletAccountEntity>>

    @Query("SELECT * FROM wallet_accounts WHERE userId = :userId ORDER BY name ASC")
    suspend fun getWalletsForUser(userId: String): List<WalletAccountEntity>

    @Query("SELECT * FROM wallet_accounts WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getWalletById(id: String, userId: String): WalletAccountEntity?

    @Query("SELECT * FROM wallet_accounts WHERE LOWER(name) = LOWER(:name) AND userId = :userId LIMIT 1")
    suspend fun getWalletByName(name: String, userId: String): WalletAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallets: List<WalletAccountEntity>)

    @Update
    suspend fun updateWallet(wallet: WalletAccountEntity)

    @Query("UPDATE wallet_accounts SET calculatedBalance = :balance, lastSyncTimestamp = :timestamp WHERE id = :id AND userId = :userId")
    suspend fun updateCalculatedBalance(id: String, userId: String, balance: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE wallet_accounts SET providerReportedBalance = :reportedBalance, lastSyncTimestamp = :timestamp WHERE id = :id AND userId = :userId")
    suspend fun updateProviderReportedBalance(id: String, userId: String, reportedBalance: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE wallet_accounts SET calculatedBalance = :reconciledBalance, providerReportedBalance = :reconciledBalance, lastReconciledAt = :timestamp WHERE id = :id AND userId = :userId")
    suspend fun reconcileWalletBalance(id: String, userId: String, reconciledBalance: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE wallet_accounts SET isAutoDetectEnabled = :isEnabled WHERE id = :id AND userId = :userId")
    suspend fun setAutoDetectEnabled(id: String, userId: String, isEnabled: Boolean)

    @Query("DELETE FROM wallet_accounts WHERE id = :id AND userId = :userId")
    suspend fun deleteWallet(id: String, userId: String)

    @Query("DELETE FROM wallet_accounts WHERE userId = :userId")
    suspend fun clearUserWallets(userId: String)
}
