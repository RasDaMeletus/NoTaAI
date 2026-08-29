package com.vinote.data.local.entity

/**
 * Synchronization state for entities that can be synced to the backend.
 */
enum class SyncState {
    LOCAL_ONLY,
    SYNCED,
    PENDING_SYNC,
    SYNC_FAILED
}