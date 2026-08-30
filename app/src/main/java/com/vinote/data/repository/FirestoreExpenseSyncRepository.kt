package com.example.data.repository

import android.util.Log
import com.example.data.local.TransactionDao
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

data class SyncResult(
    val isSuccess: Boolean,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val errorMessage: String? = null
)

class FirestoreExpenseSyncRepository(
    customFirestore: FirebaseFirestore? = null,
    private val authUserIdProvider: (() -> String)? = null
) {
    companion object {
        private const val TAG = "FirestoreExpenseSync"
        private const val COLLECTION_EXPENSES = "expenses"
        private const val COLLECTION_WALLETS = "wallets"
        private const val COLLECTION_BUDGETS = "budgets"
    }

    private val firestoreInstance: FirebaseFirestore? = customFirestore ?: runCatching {
        FirebaseFirestore.getInstance()
    }.getOrNull()

    private fun getUserId(): String {
        return authUserIdProvider?.invoke()
            ?: runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            ?: "usr_google_farras213"
    }

    private fun getExpensesCollection() = firestoreInstance
        ?.collection("users")
        ?.document(getUserId())
        ?.collection(COLLECTION_EXPENSES)

    /**
     * Uploads local transactions to Firestore remote collection.
     */
    suspend fun syncLocalToRemote(localExpenses: List<TransactionItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (localExpenses.isEmpty()) {
                return@withContext Result.success(0)
            }

            val collection = getExpensesCollection() ?: run {
                Log.w(TAG, "Firestore is not initialized, skipping remote sync")
                return@withContext Result.success(0)
            }

            var successCount = 0
            for (item in localExpenses) {
                val docId = if (item.id != 0L) "tx_${item.id}" else "tx_${item.timestamp}"
                val data = hashMapOf(
                    "id" to item.id,
                    "title" to item.title,
                    "amount" to item.amount,
                    "category" to item.category,
                    "type" to item.type.name,
                    "timestamp" to item.timestamp,
                    "timeLabel" to item.timeLabel,
                    "merchant" to item.merchant,
                    "source" to item.source.name,
                    "walletName" to (item.walletName ?: ""),
                    "lastSyncedAt" to System.currentTimeMillis()
                )

                val writeSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
                    collection.document(docId)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            if (continuation.isActive) continuation.resume(true)
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error writing transaction $docId to Firestore", e)
                            if (continuation.isActive) continuation.resume(false)
                        }
                }

                if (writeSuccess) {
                    successCount++
                }
            }

            Log.d(TAG, "Successfully synced $successCount / ${localExpenses.size} expenses to Firestore")
            Result.success(successCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync local expenses to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all remote expense documents from Firestore.
     */
    suspend fun fetchRemoteExpenses(): Result<List<TransactionItem>> = withContext(Dispatchers.IO) {
        try {
            val collection = getExpensesCollection() ?: run {
                Log.w(TAG, "Firestore is not initialized, skipping remote fetch")
                return@withContext Result.success(emptyList())
            }

            val remoteItems = suspendCancellableCoroutine<List<TransactionItem>> { continuation ->
                collection.get()
                    .addOnSuccessListener { querySnapshot ->
                        val list = querySnapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getLong("id") ?: 0L
                                val title = doc.getString("title") ?: "Expense"
                                val amount = doc.getLong("amount") ?: 0L
                                val category = doc.getString("category") ?: "General"
                                val typeStr = doc.getString("type") ?: "EXPENSE"
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val timeLabel = doc.getString("timeLabel") ?: "Today"
                                val merchant = doc.getString("merchant") ?: ""
                                val sourceStr = doc.getString("source") ?: "MANUAL"
                                val walletName = doc.getString("walletName").takeIf { !it.isNullOrEmpty() }

                                TransactionItem(
                                    id = id,
                                    title = title,
                                    amount = amount,
                                    category = category,
                                    type = runCatching { TransactionType.valueOf(typeStr) }.getOrDefault(TransactionType.EXPENSE),
                                    timestamp = timestamp,
                                    timeLabel = timeLabel,
                                    merchant = merchant,
                                    source = runCatching { TransactionSource.valueOf(sourceStr) }.getOrDefault(TransactionSource.MANUAL),
                                    walletName = walletName
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing remote transaction document: ${doc.id}", e)
                                null
                            }
                        }
                        if (continuation.isActive) continuation.resume(list)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching expenses from Firestore", e)
                        if (continuation.isActive) continuation.resume(emptyList())
                    }
            }
            Result.success(remoteItems)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during fetchRemoteExpenses", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a single transaction from Firestore.
     */
    suspend fun deleteRemoteExpense(transactionId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val collection = getExpensesCollection() ?: return@withContext false
            val docId = "tx_$transactionId"
            suspendCancellableCoroutine { continuation ->
                collection.document(docId)
                    .delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "Deleted transaction $docId from Firestore")
                        if (continuation.isActive) continuation.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to delete transaction $docId from Firestore", e)
                        if (continuation.isActive) continuation.resume(false)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting remote expense: $transactionId", e)
            false
        }
    }

    /**
     * Clears all remote expense documents from Firestore.
     */
    suspend fun clearAllRemoteExpenses(): Boolean = withContext(Dispatchers.IO) {
        try {
            val collection = getExpensesCollection() ?: return@withContext false
            val fs = firestoreInstance ?: return@withContext false

            suspendCancellableCoroutine { continuation ->
                collection.get()
                    .addOnSuccessListener { querySnapshot ->
                        val batch = fs.batch()
                        for (doc in querySnapshot.documents) {
                            batch.delete(doc.reference)
                        }
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d(TAG, "Successfully cleared all remote expenses from Firestore")
                                if (continuation.isActive) continuation.resume(true)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to execute batch delete in Firestore", e)
                                if (continuation.isActive) continuation.resume(false)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to fetch docs for clearAllRemoteExpenses", e)
                        if (continuation.isActive) continuation.resume(false)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in clearAllRemoteExpenses", e)
            false
        }
    }

    /**
     * Full bidirectional synchronization:
     * 1. Pushes local records to Firestore
     * 2. Pulls any remote records not present locally and inserts them into Room
     */
    suspend fun performFullSync(transactionDao: TransactionDao): SyncResult = withContext(Dispatchers.IO) {
        try {
            val localList = transactionDao.getAllTransactions().first()

            // 1. Upload local to remote
            val uploadResult = syncLocalToRemote(localList)
            val uploadedCount = uploadResult.getOrDefault(0)

            // 2. Fetch remote
            val remoteResult = fetchRemoteExpenses()
            val remoteList = remoteResult.getOrDefault(emptyList())

            // 3. Upsert missing remote items into local DB
            val existingIds = localList.map { it.id }.toSet()
            val missingLocal = remoteList.filter { it.id !in existingIds }
            if (missingLocal.isNotEmpty()) {
                transactionDao.insertAll(missingLocal)
            }

            SyncResult(
                isSuccess = true,
                uploadedCount = uploadedCount,
                downloadedCount = missingLocal.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
            SyncResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Sync error occurred"
            )
        }
    }
}
