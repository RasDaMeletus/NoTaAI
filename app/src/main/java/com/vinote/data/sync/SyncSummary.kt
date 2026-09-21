package com.vinote.data.sync

data class SyncSummary(
    val status: CloudSyncStatus = CloudSyncStatus.IDLE,
    val syncedCount: Int = 0,
    val errorMessage: String? = null,
    val lastSyncTimestamp: Long? = null
)
