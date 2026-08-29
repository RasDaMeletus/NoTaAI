package com.vinote.data.remote.dto

import com.squareup.moshi.Json

data class SyncStatusDto(
    @Json(name = "lastSyncAt") val lastSyncAt: Long?,
    @Json(name = "pendingCount") val pendingCount: Int,
    @Json(name = "errorCount") val errorCount: Int,
    @Json(name = "isSyncing") val isSyncing: Boolean
)