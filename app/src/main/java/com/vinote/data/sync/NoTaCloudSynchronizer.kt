package com.vinote.data.sync

import android.util.Log
import com.vinote.data.local.BudgetDao
import com.vinote.data.local.GoalDao
import com.vinote.data.local.SyncQueueDao
import com.vinote.data.local.TransactionDao
import com.vinote.data.local.WalletAccountDao
import com.vinote.data.repository.AuthRepository
import com.vinote.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Uploads the complete authenticated Room snapshot to Supabase.
 * Room stays the immediate source of truth; Supabase is the durable cloud copy.
 * All requests use the current user JWT and are protected by RLS.
 */
class NoTaCloudSynchronizer(
    private val transactionDao: TransactionDao,
    private val goalDao: GoalDao,
    private val walletAccountDao: WalletAccountDao,
    private val budgetDao: BudgetDao,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: AuthRepository,
    private val supabaseClientProvider: SupabaseClientProvider,
    private val scope: CoroutineScope
) {
    private val _syncState = MutableStateFlow(SyncSummary())
    val syncState: StateFlow<SyncSummary> = _syncState

    fun requestSync() {
        if (authRepository.supabaseSessionFlow.value == null) return
        scope.launch { performFullSync() }
    }

    suspend fun performFullSync(): SyncSummary = withContext(Dispatchers.IO) {
        val userId = authRepository.supabaseSessionFlow.value?.user?.id
        val client = supabaseClientProvider.clientOrNull
        if (userId.isNullOrBlank() || client == null) {
            _syncState.value = SyncSummary(
                status = CloudSyncStatus.OFFLINE,
                errorMessage = "Supabase session is not ready"
            )
            return@withContext _syncState.value
        }

        _syncState.value = SyncSummary(status = CloudSyncStatus.SYNCING)
        try {
            var count = 0
            count += pushTransactions(userId)
            count += pushWallets(userId)
            count += pushBudget(userId)
            count += pushGoals(userId)
            syncQueueDao.clearUserQueue(userId)
            _syncState.value = SyncSummary(
                status = CloudSyncStatus.SYNCED,
                syncedCount = count
            )
        } catch (error: Throwable) {
            Log.e("CloudSync", "Supabase synchronization failed", error)
            _syncState.value = SyncSummary(
                status = CloudSyncStatus.ERROR,
                errorMessage = error.message ?: "Supabase synchronization failed"
            )
        }
        _syncState.value
    }

    private suspend fun pushTransactions(userId: String): Int {
        val transactions = transactionDao.getTransactionsForUser(userId).first()
        for (transaction in transactions) {
            val fingerprint = transaction.fingerprint ?: "android:$userId:${transaction.id}"
            val payload = buildJsonObject {
                put("user_id", userId)
                put("amount", transaction.amount)
                put("category", transaction.category)
                put("description", transaction.title)
                put("date", isoTimestamp(transaction.timestamp))
                put("type", transaction.type.ordinal)
                put("is_deleted", false)
                put("merchant", transaction.merchant)
                put("source", transaction.source.ordinal)
                transaction.walletName?.let { put("wallet_name", it) }
                put("tx_timestamp", isoTimestamp(transaction.timestamp))
                put("time_label", transaction.timeLabel)
                put("fingerprint", fingerprint)
                put("confidence", transaction.confidence)
                put("is_confirmed", transaction.isConfirmed)
                put("sync_state", "SYNCED")
            }
            clientTable("transactions").insert(
                payload,
                upsert = true,
                onConflict = "fingerprint"
            )
            if (transaction.fingerprint == null || transaction.syncState != "SYNCED") {
                transactionDao.updateTransaction(
                    transaction.copy(fingerprint = fingerprint, syncState = "SYNCED")
                )
            }
        }
        return transactions.size
    }

    private suspend fun pushWallets(userId: String): Int {
        val wallets = walletAccountDao.getWalletsForUser(userId)
        for (wallet in wallets) {
            val payload = buildJsonObject {
                // Cloud primary key is global, so namespace device-local IDs.
                put("id", "$userId:${wallet.id}")
                put("user_id", userId)
                put("name", wallet.name)
                put("type", wallet.type.name)
                put("calculated_balance", wallet.calculatedBalance)
                wallet.providerReportedBalance?.let { put("provider_reported_balance", it) }
                wallet.lastReconciledAt?.let { put("last_reconciled_at", isoTimestamp(it)) }
                put("is_auto_detect_enabled", wallet.isAutoDetectEnabled)
                put("icon_color_hex", wallet.iconColorHex)
                put("account_number", wallet.accountNumber)
                put("is_connected", wallet.isConnected)
                put("last_sync_timestamp", isoTimestamp(wallet.lastSyncTimestamp))
                put("gateway_type", wallet.gatewayType)
                put("linked_account_id", wallet.linkedAccountId)
                // Never upload gatewayAccessToken.
            }
            clientTable("wallet_accounts").insert(payload, upsert = true, onConflict = "id")
        }
        return wallets.size
    }

    private suspend fun pushBudget(userId: String): Int {
        val budget = budgetDao.getBudget(userId) ?: return 0
        val payload = buildJsonObject {
            put("id", "budget_$userId")
            put("user_id", userId)
            put("monthly_limit", budget.monthlyLimit)
            put("daily_limit", budget.dailyLimit)
            put("warning_threshold_percent", budget.warningThresholdPercent)
            put("period_month_year", budget.periodMonthYear)
            put("updated_at", isoTimestamp(budget.updatedTimestamp))
        }
        clientTable("budgets").insert(payload, upsert = true, onConflict = "id")
        return 1
    }

    private suspend fun pushGoals(userId: String): Int {
        val goals = goalDao.getGoalsForUser(userId).first()
        for (goal in goals) {
            val payload = buildJsonObject {
                put("user_id", userId)
                put("title", goal.title)
                put("target_amount", goal.targetAmount)
                put("current_amount", goal.currentAmount)
                put("icon", goal.iconName)
                put("local_id", goal.id)
                put("is_deleted", false)
                put("updated_at", isoTimestamp(System.currentTimeMillis()))
            }
            clientTable("savings_cloud").insert(
                payload,
                upsert = true,
                onConflict = "user_id,local_id"
            )
        }
        return goals.size
    }

    private fun clientTable(name: String) =
        supabaseClientProvider.client.postgrest[name]

    private fun isoTimestamp(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(timestamp.coerceAtLeast(0L)))
}
