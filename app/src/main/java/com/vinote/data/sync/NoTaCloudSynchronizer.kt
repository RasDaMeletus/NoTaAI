package com.vinote.data.sync

import android.util.Log
import com.vinote.data.local.GoalDao
import com.vinote.data.local.SyncQueueDao
import com.vinote.data.local.TransactionDao
import com.vinote.data.local.entities.SyncQueueStatus
import com.vinote.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoTaCloudSynchronizer(
    private val transactionDao: TransactionDao,
    private val goalDao: GoalDao,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _syncState = MutableStateFlow(SyncSummary())
    val syncState: StateFlow<SyncSummary> = _syncState

    suspend fun performFullSync(): SyncSummary = withContext(Dispatchers.IO) {
        val userId = authRepository.getUserId()
        if (userId == null) {
            _syncState.value = SyncSummary(
                status = CloudSyncStatus.ERROR,
                errorMessage = "Not authenticated"
            )
            return@withContext _syncState.value
        }

        _syncState.value = SyncSummary(status = CloudSyncStatus.SYNCING)

        var totalSynced = 0
        var hasError = false
        var errorMessage: String? = null

        try {
            // 1. Process pending sync queue. Items are only marked SYNCED once
            // the remote acknowledges the operation; processSyncQueue returns
            // the count it actually pushed and confirmed.
            val queueProcessed = processSyncQueue(userId)
            totalSynced += queueProcessed

            // 2. Sync transactions. The remote push happens inside
            // processSyncQueue (TRANSACTION entity type); there is no separate
            // no-op step here, so nothing is reported as synced without an ACK.
            //
            // 3. Sync wallets and budgets - same path via the sync queue. Until
            // a real Edge Function push exists for an entity type, items of
            // that type stay PENDING rather than being marked SYNCED.

            // 4. Clear completed sync queue items (only ACK'd ones).
            syncQueueDao.clearCompleted(userId)

        } catch (e: Exception) {
            hasError = true
            errorMessage = e.message ?: "Unknown sync error"
            Log.e("CloudSync", "Full sync exception", e)
        }

        // Report PENDING rather than SYNCED when nothing was actually
        // acknowledged by the server. A no-op run must never look like a
        // successful backup.
        val finalStatus = when {
            hasError -> CloudSyncStatus.ERROR
            totalSynced > 0 -> CloudSyncStatus.SYNCED
            else -> CloudSyncStatus.PENDING
        }
        _syncState.value = SyncSummary(
            status = finalStatus,
            syncedCount = totalSynced,
            errorMessage = errorMessage
        )
        _syncState.value
    }

    private suspend fun processSyncQueue(userId: String): Int {
        val pendingItems = syncQueueDao.getPendingQueue(userId)
        var processed = 0

        for (item in pendingItems) {
            syncQueueDao.updateStatus(item.id, SyncQueueStatus.SYNCING)
            try {
                // Entity types without a real remote push must stay PENDING.
                // Marking them SYNCED here would make a backup look successful
                // when nothing was ever sent. Only types whose push actually
                // acknowledges the operation are advanced to SYNCED below.
                when (item.entityType) {
                    "TRANSACTION" -> {
                        // No transaction push is implemented yet (the sync Edge
                        // Function is not deployed). Leave the item pending.
                        syncQueueDao.updateStatus(item.id, SyncQueueStatus.PENDING)
                    }
                    "WALLET", "BUDGET" -> {
                        // Wallet/Budget remote sync is not implemented yet.
                        syncQueueDao.updateStatus(item.id, SyncQueueStatus.PENDING)
                    }
                    "GOAL" -> {
                        // Goal remote sync is not implemented yet.
                        syncQueueDao.updateStatus(item.id, SyncQueueStatus.PENDING)
                    }
                    else -> {
                        // Unknown entity type: leave pending rather than
                        // silently claiming success.
                        syncQueueDao.updateStatus(item.id, SyncQueueStatus.PENDING)
                    }
                }
            } catch (e: Exception) {
                syncQueueDao.markFailed(item.id, SyncQueueStatus.FAILED, e.message)
                Log.w("CloudSync", "Failed to process sync queue item ${item.id}", e)
            }
        }
        return processed
    }
}
