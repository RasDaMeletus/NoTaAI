package com.example.data.repository

import com.example.data.local.BudgetDao
import com.example.data.local.WalletAccountDao
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.WalletAccountEntity
import com.example.data.local.entities.WalletType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Bidirectional Firestore sync for wallet accounts and the user's budget. */
class FirestoreWalletBudgetSyncRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val walletDao: WalletAccountDao,
    private val budgetDao: BudgetDao,
    private val userIdProvider: () -> String = {
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
) {
    private fun userDocument() = firestore.collection("users").document(userIdProvider())

    suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = userIdProvider()
        if (userId.isBlank()) return@withContext Result.failure(IllegalStateException("Not signed in"))

        try {
            val user = userDocument()
            val wallets = walletDao.getWalletsForUser(userId)
            val batch = firestore.batch()

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
                if (local == null || remote.lastSyncTimestamp > local.lastSyncTimestamp) {
                    walletDao.insertWallet(remote)
                }
            }

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
                if (localBudget == null || remoteBudget.updatedTimestamp > localBudget.updatedTimestamp) {
                    budgetDao.saveBudget(remoteBudget)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { continuation ->
            task.addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                .addOnFailureListener { if (continuation.isActive) continuation.resumeWith(Result.failure(it)) }
        }
}
