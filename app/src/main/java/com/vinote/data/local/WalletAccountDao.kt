package com.vinote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vinote.data.local.entity.WalletAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletAccountDao {
    @Query("SELECT * FROM wallet_accounts WHERE userId = :userId ORDER BY name ASC")
    fun getWalletsByUser(userId: String): Flow<List<WalletAccount>>

    @Query("SELECT * FROM wallet_accounts WHERE id = :id")
    suspend fun getById(id: Long): WalletAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: WalletAccount): Long

    @Update
    suspend fun update(wallet: WalletAccount)

    @Query("DELETE FROM wallet_accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
