package com.vinote.data.remote.dto

import com.squareup.moshi.Json

data class SyncPullRequest(
    @Json(name = "since") val since: Long,
    @Json(name = "entityTypes") val entityTypes: List<String> = listOf("transaction", "wallet", "goal", "budget")
)

data class SyncPullResponse(
    @Json(name = "entities") val entities: List<SyncEntityData>,
    @Json(name = "serverTime") val serverTime: Long
)

data class SyncEntityData(
    @Json(name = "type") val type: String,
    @Json(name = "operation") val operation: String,
    @Json(name = "data") val data: Map<String, Any>
)