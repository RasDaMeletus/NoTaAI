package com.vinote.data.repository

import android.util.Log
import com.vinote.data.local.BudgetDao
import com.vinote.data.local.SyncQueueDao
import com.vinote.data.local.WalletAccountDao
import com.vinote.data.local.entities.BudgetEntity
import com.vinote.data.local.entities.SyncOperation
import com.vinote.data.local.entities.SyncQueueEntity
import com.vinote.data.local.entities.SyncQueueStatus
import com.vinote.data.local.entities.WalletAccountEntity
import com.vinote.data.local.entities.WalletType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import org.json.JSONObject

/** Bidirectional Firestore sync for wallet accounts and the user's budget. */
class FirestoreWalletBudgetSyncRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val walletDao: WalletAccountDao,
    private val budgetDao: BudgetDao,
    private val syncQueueDao: SyncQueueDao? = null,
    private val userIdProvider: (() -> String)? = null
) {
    companion object {
        private const val TAG = "FirestoreWalletBudgetSync"
    }

    private fun userDocument(userId: String) = firestore.collection("users").document(userId)

    suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = userIdProvider?.invoke() ?: "user_default"

        try {
            val user = userDocument(userId)
            val wallets = walletDao.getWalletsForUser(userId)
            val batch = firestore.batch()

            // Push local wallets to Firestore
            wallets.forEach { wallet ->
                val data = mapOf(
                    "id" to wallet.id,
                    "userId" to userId,
                    "name" to wallet.name,
                    "type" to wallet.type.name,
                    "calculatedBalance" to wallet.calculatedBalance,
                    "providerReportedBalance" to wallet.providerReportedBalance,
                    "lastReconciledAt" to wallet.lastReconciledAt,
                    "isAutoDetectEnabled" to wallet.isAutoDetectEnabled,
                    "iconColorHex" to wallet.iconColorHex,
                    "accountNumber" to wallet.accountNumber,
                    "isConnected" to wallet.isConnected,
                    "lastSyncTimestamp" to wallet.lastSyncTimestamp
                )
                batch.set(user.collection("wallets").document(wallet.id), data, SetOptions.merge())
            }

            // Push local budget to Firestore
            val budget = budgetDao.getBudget(userId)
            if (budget != null) {
                batch.set(
                    user.collection("budgets").document("current"),
                    mapOf(
                        "id" to budget.id,
                        "userId" to userId,
                        "monthlyLimit" to budget.monthlyLimit,
                        "dailyLimit" to budget.dailyLimit,
                        "warningThresholdPercent" to budget.warningThresholdPercent,
                        "periodMonthYear" to budget.periodMonthYear,
                        "updatedTimestamp" to budget.updatedTimestamp
                    ),
                    SetOptions.merge()
                )
            }

            awaitTask(batch.commit())

            // Pull remote wallets with conflict resolution (newer updatedTimestamp wins)
            val walletSnapshot = awaitTask(user.collection("wallets").get())
            val remoteWallets = walletSnapshot.documents.mapNotNull { doc ->
                runCatching {
                    WalletAccountEntity(
                        id = doc.getString("id") ?: doc.id,
                        userId = userId,
                        name = doc.getString("name") ?: return@runCatching null,
                        type = runCatching { WalletType.valueOf(doc.getString("type") ?: "EWALLET") }
                            .getOrDefault(WalletType.EWALLET),
                        calculatedBalance = doc.getLong("calculatedBalance") ?: 0L,
                        providerReportedBalance = doc.getLong("providerReportedBalance"),
                        lastReconciledAt = doc.getLong("lastReconciledAt"),
                        isAutoDetectEnabled = doc.getBoolean("isAutoDetectEnabled") ?: true,
                        iconColorHex = doc.getString("iconColorHex") ?: "#0057C2",
                        accountNumber = doc.getString("accountNumber") ?: "",
                        isConnected = doc.getBoolean("isConnected") ?: true,
                        lastSyncTimestamp = doc.getLong("lastSyncTimestamp") ?: 0L
                    )
                }.getOrNull()
            }

            remoteWallets.forEach { remote ->
                val local = walletDao.getWalletById(remote.id, userId)
                // Conflict resolution: newer lastSyncTimestamp wins
                if (local == null || remote.lastSyncTimestamp > local.lastSyncTimestamp) {
                    walletDao.insertWallet(remote)
                } else if (local.lastSyncTimestamp > remote.lastSyncTimestamp) {
                    // Local is newer - push to remote
                    val data = mapOf(
                        "id" to local.id,
                        "userId" to userId,
                        "name" to local.name,
                        "type" to local.type.name,
                        "calculatedBalance" to local.calculatedBalance,
                        "providerReportedBalance" to local.providerReportedBalance,
                        "lastReconciledAt" to local.lastReconciledAt,
                        "isAutoDetectEnabled" to local.isAutoDetectEnabled,
                        "iconColorHex" to local.iconColorHex,
                        "accountNumber" to local.accountNumber,
                        "isConnected" to local.isConnected,
                        "lastSyncTimestamp" to local.lastSyncTimestamp
                    )
                    awaitTask(user.collection("wallets").document(local.id).set(data, SetOptions.merge()))
                }
            }

            // Pull remote budget with conflict resolution (newer updatedTimestamp wins)
            val budgetSnapshot = awaitTask(user.collection("budgets").document("current").get())
            if (budgetSnapshot.exists()) {
                val remoteBudget = BudgetEntity(
                    id = budgetSnapshot.getString("id") ?: "budget_$userId",
                    userId = userId,
                    monthlyLimit = budgetSnapshot.getLong("monthlyLimit") ?: 0L,
                    dailyLimit = budgetSnapshot.getLong("dailyLimit") ?: 0L,
                    warningThresholdPercent = budgetSnapshot.getDouble("warningThresholdPercent")?.toFloat() ?: 0.85f,
                    periodMonthYear = budgetSnapshot.getString("periodMonthYear") ?: "",
                    updatedTimestamp = budgetSnapshot.getLong("updatedTimestamp") ?: 0L
                )
                val localBudget = budgetDao.getBudget(userId)
                // Conflict resolution: newer updatedTimestamp wins
                if (localBudget == null || remoteBudget.updatedTimestamp > localBudget.updatedTimestamp) {
                    budgetDao.saveBudget(remoteBudget)
                } else if (localBudget.updatedTimestamp > remoteBudget.updatedTimestamp) {
                    // Local is newer - push to remote
                    awaitTask(user.collection("budgets").document("current").set(
                        mapOf(
                            "id" to localBudget.id,
                            "userId" to userId,
                            "monthlyLimit" to localBudget.monthlyLimit,
                            "dailyLimit" to localBudget.dailyLimit,
                            "warningThresholdPercent" to localBudget.warningThresholdPercent,
                            "periodMonthYear" to localBudget.periodMonthYear,
                            "updatedTimestamp" to localBudget.updatedTimestamp
                        ),
                        SetOptions.merge()
                    ))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Enqueue wallet change for offline-first sync.
     * Called when wallet is modified locally.
     */
    suspend fun enqueueWalletChange(wallet: WalletAccountEntity, operation: SyncOperation) {
        syncQueueDao?.let { dao ->
            val payload = JSONObject()
            payload.put("id", wallet.id)
            payload.put("userId", wallet.userId)
            payload.put("name", wallet.name)
            payload.put("type", wallet.type.name)
            payload.put("calculatedBalance", wallet.calculatedBalance)
            payload.put("providerReportedBalance", wallet.providerReportedBalance)
            payload.put("lastReconciledAt", wallet.lastReconciledAt)
            payload.put("isAutoDetectEnabled", wallet.isAutoDetectEnabled)
            payload.put("iconColorHex", wallet.iconColorHex)
            payload.put("accountNumber", wallet.accountNumber)
            payload.put("isConnected", wallet.isConnected)
            payload.put("lastSyncTimestamp", System.currentTimeMillis())

            dao.enqueue(
                SyncQueueEntity(
                    userId = wallet.userId,
                    entityType = "WALLET",
                    entityId = wallet.id,
                    operation = operation,
                    payloadJson = payload.toString(),
                    timestamp = System.currentTimeMillis(),
                    status = SyncQueueStatus.PENDING
                )
            )
        }
    }

    /**
     * Enqueue budget change for offline-first sync.
     * Called when budget is modified locally.
     */
    suspend fun enqueueBudgetChange(budget: BudgetEntity, operation: SyncOperation) {
        syncQueueDao?.let { dao ->
            val payload = JSONObject()
            payload.put("id", budget.id)
            payload.put("userId", budget.userId)
            payload.put("monthlyLimit", budget.monthlyLimit)
            payload.put("dailyLimit", budget.dailyLimit)
            payload.put("warningThresholdPercent", budget.warningThresholdPercent)
            payload.put("periodMonthYear", budget.periodMonthYear)
            payload.put("updatedTimestamp", System.currentTimeMillis())

            dao.enqueue(
                SyncQueueEntity(
                    userId = budget.userId,
                    entityType = "BUDGET",
                    entityId = budget.id,
                    operation = operation,
                    payloadJson = payload.toString(),
                    timestamp = System.currentTimeMillis(),
                    status = SyncQueueStatus.PENDING
                )
            )
        }
    }

    private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { continuation ->
            task.addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                .addOnFailureListener { if (continuation.isActive) continuation.resumeWith(Result.failure(it)) }
        }
}