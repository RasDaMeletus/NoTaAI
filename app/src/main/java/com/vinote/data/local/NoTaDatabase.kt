package com.vinote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vinote.data.local.entities.AchievementEntity
import com.vinote.data.local.entities.BudgetEntity
import com.vinote.data.local.entities.DetectionEventEntity
import com.vinote.data.local.entities.MerchantEmbeddingEntity
import com.vinote.data.local.entities.RecurringTransactionEntity
import com.vinote.data.local.entities.SpendingPredictionEntity
import com.vinote.data.local.entities.SyncQueueEntity
import com.vinote.data.local.entities.TransactionCategoryEntity
import com.vinote.data.local.entities.TransactionTemplateEntity
import com.vinote.data.local.entities.UserSessionEntity
import com.vinote.data.local.entities.WalletAccountEntity
import com.vinote.data.model.GoalItem
import com.vinote.data.model.TransactionItem

@Database(
    entities = [
        TransactionItem::class,
        GoalItem::class,
        UserSessionEntity::class,
        WalletAccountEntity::class,
        DetectionEventEntity::class,
        SyncQueueEntity::class,
        BudgetEntity::class,
        TransactionTemplateEntity::class,
        RecurringTransactionEntity::class,
        MerchantEmbeddingEntity::class,
        AchievementEntity::class,
        SpendingPredictionEntity::class,
        TransactionCategoryEntity::class
    ],
    version = 6, // bumped from 5: added transactions.remoteId (cloud uuid)
    exportSchema = false
)
abstract class NoTaDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun walletAccountDao(): WalletAccountDao
    abstract fun detectionEventDao(): DetectionEventDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun merchantEmbeddingDao(): MerchantEmbeddingDao
    abstract fun achievementDao(): AchievementDao
    abstract fun spendingPredictionDao(): SpendingPredictionDao
    abstract fun transactionCategoryDao(): TransactionCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: NoTaDatabase? = null

        fun getDatabase(context: Context): NoTaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoTaDatabase::class.java,
                    "vinote_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
