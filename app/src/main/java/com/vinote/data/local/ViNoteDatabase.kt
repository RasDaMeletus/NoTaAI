package com.vinote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vinote.data.local.entity.AiConversation
import com.vinote.data.local.entity.AiMessage
import com.vinote.data.local.entity.AppIntegration
import com.vinote.data.local.entity.Budget
import com.vinote.data.local.entity.DetectionEvent
import com.vinote.data.local.entity.SyncMetadata
import com.vinote.data.local.entity.WalletAccount
import com.vinote.data.model.GoalItem
import com.vinote.data.model.TransactionItem

@Database(
    entities = [
        TransactionItem::class,
        GoalItem::class,
        WalletAccount::class,
        DetectionEvent::class,
        Budget::class,
        AppIntegration::class,
        AiConversation::class,
        AiMessage::class,
        SyncMetadata::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ViNoteDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun walletAccountDao(): WalletAccountDao
    abstract fun detectionEventDao(): DetectionEventDao
    abstract fun budgetDao(): BudgetDao
    abstract fun appIntegrationDao(): AppIntegrationDao
    abstract fun aiConversationDao(): AiConversationDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: ViNoteDatabase? = null

        /**
         * Migration from v1 (transactions + goals) to v2 (full production schema).
         * Adds wallet_accounts, detection_events, budgets, app_integrations,
         * ai_conversations, ai_messages, sync_metadata tables.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Wallet accounts
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS wallet_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        openingBalance INTEGER NOT NULL DEFAULT 0,
                        providerBalance INTEGER,
                        currency TEXT NOT NULL DEFAULT 'IDR',
                        isActive INTEGER NOT NULL DEFAULT 1,
                        isAutoDetectEnabled INTEGER NOT NULL DEFAULT 1,
                        lastDetectionAt INTEGER,
                        lastSyncAt INTEGER,
                        syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_wallet_accounts_userId ON wallet_accounts(userId)")

                // Detection events
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS detection_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        title TEXT NOT NULL DEFAULT '',
                        text TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL DEFAULT 0,
                        fingerprint TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        result TEXT NOT NULL,
                        transactionId INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_detection_events_userId ON detection_events(userId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_detection_events_fingerprint ON detection_events(fingerprint)")

                // Budgets
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        category TEXT NOT NULL,
                        monthKey TEXT NOT NULL,
                        limitAmount INTEGER NOT NULL,
                        spentAmount INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_budgets_userId ON budgets(userId)")

                // App integrations
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_integrations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        parserVersion INTEGER NOT NULL DEFAULT 1,
                        lastNotificationAt INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_app_integrations_userId ON app_integrations(userId)")

                // AI conversations
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_conversations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL DEFAULT 'New conversation',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_ai_conversations_userId ON ai_conversations(userId)")

                // AI messages
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        conversationId INTEGER NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        toolCalls TEXT,
                        timestamp INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_ai_messages_conversationId ON ai_messages(conversationId)")

                // Sync metadata
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_metadata (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId INTEGER NOT NULL,
                        operation TEXT NOT NULL,
                        state TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        lastError TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_sync_metadata_entity ON sync_metadata(entityType, entityId)")
            }
        }

        /**
         * Migration from v2 to v3: Add userId and syncState columns to transactions and goals tables.
         * Phase 2 - Production Ledger: user-scoped data with sync state tracking.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add userId and syncState to transactions table
                db.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")

                // Add userId and syncState to goals table
                db.execSQL("ALTER TABLE goals ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE goals ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
            }
        }

        fun getDatabase(context: Context): ViNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ViNoteDatabase::class.java,
                    "vinote_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
