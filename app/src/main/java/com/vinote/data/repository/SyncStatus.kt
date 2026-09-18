package com.vinote.data.repository

/**
 * Coarse sync state surfaced to the UI. The detailed sync result lives in
 * com.vinote.data.sync.SyncSummary; this is the lightweight status the
 * Activity/Settings screens display.
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}
