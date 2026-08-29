package com.vinote.data.remote.dto

import com.squareup.moshi.Json

data class SyncPushRequest(
    @Json(name = "entities") val entities: List<SyncEntity>,
    @Json(name = "idempotencyKey") val idempotencyKey: String
)

data class SyncEntity(
    @Json(name = "type") val type: String,          // transaction | wallet | goal | budget
    @Json(name = "operation") val operation: String, // INSERT | UPDATE | DELETE
    @Json(name = "entityId") val entityId: String,
    @Json(name = "data") val data: Map<String, Any>
)

data class SyncPushResponse(
    @Json(name = "synced") val synced: List<String>,
    @Json(name = "conflicts") val conflicts: List<SyncConflict>
)

data class SyncConflict(
    @Json(name = "entityId") val entityId: String,
    @Json(name = "resolution") val resolution: String
)