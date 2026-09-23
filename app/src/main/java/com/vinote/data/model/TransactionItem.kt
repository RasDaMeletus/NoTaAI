package com.vinote.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class TransactionSource {
    MANUAL,
    VOICE,
    SCAN,
    E_WALLET,
    AUTO_DETECTED,
    BANK_SYNC
}

@Entity(tableName = "transactions")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String = "",
    val title: String,
    val amount: Long, // in IDR (exact integer representation, no float inaccuracies)
    val category: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val timestamp: Long = System.currentTimeMillis(),
    val timeLabel: String = "Today",
    val merchant: String = "",
    val source: TransactionSource = TransactionSource.MANUAL,
    val walletName: String? = null,
    val fingerprint: String? = null,
    val confidence: Float = 1.0f,
    val isConfirmed: Boolean = true,
    val syncState: String = "SYNCED",
    // Remote row's uuid. The cloud table's PK is a generated uuid while the
    // local PK is an autoincrement Long, so UPDATE/DELETE pushes must filter
    // on this, not the local id.
    val remoteId: String? = null
) {
    /**
     * Serializes to the public.transactions row format. Column names and
     * types mirror the PostgREST schema — the sync push 400s on any mismatch.
     * Local enums map to the integer codes the table uses (type/source).
     * The id is omitted so Postgres generates the uuid; the caller reads it
     * back from the upsert response and stores it in [remoteId].
     */
    fun toSupabaseJson(): String {
        val esc = { s: String ->
            s.replace("\\", "\\\\").replace("\"", "\\\"")
        }
        val typeCode = when (type) { TransactionType.EXPENSE -> 1; TransactionType.INCOME -> 2 }
        val sourceCode = when (source) {
            TransactionSource.MANUAL -> 0; TransactionSource.VOICE -> 1; TransactionSource.SCAN -> 2
            TransactionSource.E_WALLET -> 3; TransactionSource.AUTO_DETECTED -> 4; TransactionSource.BANK_SYNC -> 5
        }
        // ISO-8601 UTC from the epoch-millis timestamp.
        val isoDate = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneOffset.UTC)
            .toString()
        val merchantJson = merchant.ifBlank { null }?.let { "\"$esc(it)\"" }
        val walletJson = walletName?.let { "\"$esc(it)\"" }
        val fingerprintJson = fingerprint?.let { "\"$esc(it)\"" }
        return """{""" +
            """"amount":$amount,""" +
            """"category":"$esc(category)",""" +
            """"description":"$esc(title)",""" +
            """"date":"$isoDate",""" +
            """"type":$typeCode,""" +
            """"user_id":"$esc(userId)",""" +
            """"merchant":$merchantJson,""" +
            """"source":$sourceCode,""" +
            """"wallet_name":$walletJson,""" +
            """"tx_timestamp":"$isoDate",""" +
            """"time_label":"$esc(timeLabel)",""" +
            """"fingerprint":$fingerprintJson,""" +
            """"confidence":$confidence,""" +
            """"is_confirmed":$isConfirmed,""" +
            """"sync_state":"SYNCED"""" +
            """}"""
    }
}
