package com.vinote.data.remote.dto

import com.squareup.moshi.Json

/**
 * Request to sync transactions from local to cloud.
 * Uses idempotency keys for financial sync operations (PRD section 7).
 */
data class TransactionSyncRequest(
    @Json(name = "transactions") val transactions: List<TransactionDto>,
    @Json(name = "idempotencyKey") val idempotencyKey: String
)

data class TransactionSyncResponse(
    @Json(name = "synced") val synced: List<String>,     // IDs of successfully synced transactions
    @Json(name = "conflicts") val conflicts: List<TransactionConflict>,
    @Json(name = "errors") val errors: List<SyncError>
)

data class TransactionConflict(
    @Json(name = "localId") val localId: String,
    @Json(name = "remoteId") val remoteId: String,
    @Json(name = "resolution") val resolution: String    // "LOCAL_WINS" | "REMOTE_WINS" | "MANUAL"
)

data class SyncError(
    @Json(name = "entityId") val entityId: String,
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String
)