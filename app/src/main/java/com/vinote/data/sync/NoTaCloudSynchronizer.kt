package com.vinote.data.sync

import android.util.Log
import com.vinote.data.local.GoalDao
import com.vinote.data.local.SyncQueueDao
import com.vinote.data.local.TransactionDao
import com.vinote.data.local.entities.SyncOperation
import com.vinote.data.local.entities.SyncQueueEntity
import com.vinote.data.local.entities.SyncQueueStatus
import com.vinote.data.repository.AuthRepository
import com.vinote.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoTaCloudSynchronizer(
    private val transactionDao: TransactionDao,
    private val goalDao: GoalDao,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: AuthRepository,
    private val supabaseClientProvider: SupabaseClientProvider,
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

        // Nothing authenticated to push against — leave everything PENDING
        // rather than silently claiming success.
        val client = supabaseClientProvider?.clientOrNull
            ?: return 0

        for (item in pendingItems) {
            syncQueueDao.updateStatus(item.id, SyncQueueStatus.SYNCING)
            try {
                val pushed = when (item.entityType) {
                    "TRANSACTION" -> pushTransaction(client, item)
                    // Wallet/Budget/Goal remote sync is not implemented yet.
                    // Keep PENDING so the queue keeps retrying instead of
                    // looking backed up.
                    else -> false
                }
                if (pushed) {
                    syncQueueDao.updateStatus(item.id, SyncQueueStatus.SYNCED)
                    processed++
                } else {
                    syncQueueDao.updateStatus(item.id, SyncQueueStatus.PENDING)
                }
            } catch (e: Exception) {
                syncQueueDao.markFailed(item.id, SyncQueueStatus.FAILED, e.message)
                Log.w("CloudSync", "Failed to process sync queue item ${item.id}", e)
            }
        }
        return processed
    }

    /**
     * Pushes one queued transaction to Supabase via PostgREST, using the
     * signed-in user's session token (RLS-scoped). INSERT/UPDATE/DELETE map
     * onto the REST verbs; returns true only on a server ACK. The remote PK
     * is a generated uuid, so UPDATE/DELETE filter on the stored [remoteId].
     */
    private suspend fun pushTransaction(
        client: io.github.jan.supabase.SupabaseClient,
        item: SyncQueueEntity
    ): Boolean {
        val table = client.postgrest["transactions"]
        when (item.operation) {
            SyncOperation.INSERT, SyncOperation.UPDATE -> {
                val response = table.upsert(item.payloadJson) {
                    select()
                }
                // Persist the generated uuid so later UPDATE/DELETE can target it.
                // JsonObject, not a typed map: the row also holds numbers/bools
                // that a Map<String,String?> decode would reject.
                response.decodeSingle<JsonObject>()["id"]?.jsonPrimitive?.contentOrNull?.let { rid ->
                    transactionDao.setRemoteId(item.entityId.toLong(), rid)
                }
            }
            SyncOperation.DELETE -> {
                table.delete {
                    filter {
                        // Remote PK is a uuid, not the local autoincrement Long.
                        transactionDao.getRemoteId(item.entityId.toLong())
                            ?.let { eq("id", it) } ?: return@filter
                        eq("user_id", item.userId)
                    }
                }
            }
        }
        return true
    }
}
