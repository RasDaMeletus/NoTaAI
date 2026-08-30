package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.DetectionEventEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.entities.UserSessionEntity
import com.example.data.local.entities.WalletAccountEntity
import com.example.data.model.GoalItem
import com.example.data.model.TransactionItem

@Database(
    entities = [
        TransactionItem::class,
        GoalItem::class,
        UserSessionEntity::class,
        WalletAccountEntity::class,
        DetectionEventEntity::class,
        SyncQueueEntity::class,
        BudgetEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ViNoteDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun walletAccountDao(): WalletAccountDao
    abstract fun detectionEventDao(): DetectionEventDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: ViNoteDatabase? = null

        fun getDatabase(context: Context): ViNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ViNoteDatabase::class.java,
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

