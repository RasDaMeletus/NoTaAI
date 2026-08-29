package com.vinote.services.wallet

import com.vinote.domain.wallet.ParsedWalletTransaction
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Idempotent fingerprint deduplication for automated wallet transactions.
 * Prevents identical incoming notifications within a 5-minute time window
 * from creating duplicate database expenses.
 */
class WalletDeduplicationService {

    // Store recent hashes with timestamp
    private val processedFingerprints = ConcurrentHashMap<String, Long>()
    private val timeWindowMs = 5 * 60 * 1000L // 5 minutes deduplication window

    fun generateFingerprint(tx: ParsedWalletTransaction): String {
        // Bucket timestamp to 2-minute granularity
        val timeBucket = tx.rawNotification.timestamp / (2 * 60 * 1000L)
        val rawKey = "${tx.walletName}_${tx.rawNotification.packageName}_${tx.amount}_${tx.merchant}_$timeBucket"
        return sha256(rawKey)
    }

    @Synchronized
    fun isDuplicate(tx: ParsedWalletTransaction): Boolean {
        cleanOldFingerprints()
        val fp = generateFingerprint(tx)
        val existing = processedFingerprints[fp]
        if (existing != null && (System.currentTimeMillis() - existing) < timeWindowMs) {
            return true
        }
        processedFingerprints[fp] = System.currentTimeMillis()
        return false
    }

    private fun cleanOldFingerprints() {
        val now = System.currentTimeMillis()
        val it = processedFingerprints.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (now - entry.value > timeWindowMs) {
                it.remove()
            }
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
