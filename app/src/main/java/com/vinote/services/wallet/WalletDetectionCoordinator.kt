package com.example.services.wallet

import android.util.Log
import com.example.data.local.DetectionEventDao
import com.example.data.local.SyncQueueDao
import com.example.data.local.TransactionDao
import com.example.data.local.WalletAccountDao
import com.example.data.local.entities.DetectionEventEntity
import com.example.data.local.entities.DetectionStatus
import com.example.data.local.entities.SyncOperation
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.domain.ai.ViNoteAiService
import com.example.domain.wallet.ParsedWalletTransaction
import com.example.domain.wallet.WalletDetectionService
import com.example.domain.wallet.WalletNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Production Injected Coordinator for E-Wallet / Banking Notification Detection.
 * Decoupled from Android UI Lifecycle and Service lifecycles.
 * Handles parsing, validation, fingerprinting, deduplication, confidence tiering,
 * atomic Room persistence, and ledger accounting.
 */
class WalletDetectionCoordinator(
    private val transactionDao: TransactionDao,
    private val detectionEventDao: DetectionEventDao,
    private val walletAccountDao: WalletAccountDao,
    private val syncQueueDao: SyncQueueDao,
    private val aiService: ViNoteAiService? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    // In-memory cache for fast hot-path deduplication in addition to Room persistence
    private val deduplicationService = WalletDeduplicationService()

    // Notification event stream for in-app UI banners and alerts
    private val _detectionAlertFlow = MutableSharedFlow<DetectionAlert>(extraBufferCapacity = 16)
    val detectionAlertFlow: SharedFlow<DetectionAlert> = _detectionAlertFlow.asSharedFlow()

    data class DetectionAlert(
        val message: String,
        val transaction: TransactionItem,
        val status: DetectionStatus
    )

    fun processNotificationAsync(
        notification: WalletNotification,
        userId: String = "user_default",
        onCompleted: ((Boolean, String) -> Unit)? = null
    ) {
        scope.launch {
            val (success, message) = processNotification(notification, userId)
            onCompleted?.invoke(success, message)
        }
    }

    suspend fun processNotification(
        notification: WalletNotification,
        userId: String = "user_default"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val adapter = WalletDetectionService.findAdapterForPackage(notification.packageName)
            if (adapter == null || !adapter.isFinancialNotification(notification)) {
                return@withContext Pair(false, "Not a recognized financial notification")
            }

            // Check if user disabled detection for this wallet
            val walletAccount = walletAccountDao.getWalletByName(adapter.displayName, userId)
            if (walletAccount != null && !walletAccount.isAutoDetectEnabled) {
                Log.d("WalletCoordinator", "Detection paused by user for wallet ${adapter.displayName}")
                return@withContext Pair(false, "Detection paused for ${adapter.displayName}")
            }

            // Parse via deterministic adapter
            var candidate = adapter.parseNotification(notification)

            // Fallback to AI NLP parsing if adapter couldn't extract exact amount
            if (candidate == null || candidate.amount <= 0) {
                val fullText = "${notification.title} ${notification.text} ${notification.bigText ?: ""}"
                val aiCandidate = aiService?.parseNaturalLanguageTransaction(fullText, isOnlineAllowed = false)
                if (aiCandidate != null && aiCandidate.amount > 0) {
                    candidate = ParsedWalletTransaction(
                        title = aiCandidate.title,
                        amount = aiCandidate.amount,
                        category = aiCandidate.category,
                        type = aiCandidate.type,
                        merchant = aiCandidate.merchant.ifBlank { adapter.displayName },
                        walletName = aiCandidate.wallet ?: adapter.displayName,
                        rawNotification = notification,
                        confidence = 0.82f // AI extraction gets medium confidence
                    )
                }
            }

            if (candidate == null || candidate.amount <= 0) {
                // Log unparsed low-confidence event for diagnostics without financial mutation
                val eventId = "evt_${System.currentTimeMillis()}_${(1000..9999).random()}"
                val fallbackFp = sha256("${notification.packageName}_${notification.title}_${notification.timestamp}")
                detectionEventDao.insertEvent(
                    DetectionEventEntity(
                        id = eventId,
                        userId = userId,
                        provider = adapter.displayName,
                        packageName = notification.packageName,
                        rawTitle = notification.title,
                        rawText = notification.text,
                        rawSnippet = "${notification.title} - ${notification.text}".take(150),
                        amount = 0L,
                        type = "UNKNOWN",
                        merchant = "",
                        timestamp = notification.timestamp,
                        confidence = 0.1f,
                        fingerprint = fallbackFp,
                        status = DetectionStatus.REJECTED
                    )
                )
                return@withContext Pair(false, "Could not extract valid financial amount")
            }

            // Generate deterministic fingerprint
            val fingerprint = deduplicationService.generateFingerprint(candidate)

            // Check deduplication against database
            val existingTx = transactionDao.findByFingerprint(fingerprint)
            val existingEvt = detectionEventDao.findByFingerprint(fingerprint)
            if (existingTx != null || existingEvt != null) {
                Log.d("WalletCoordinator", "Duplicate notification ignored: ${candidate.title} ($fingerprint)")
                return@withContext Pair(false, "Duplicate notification ignored")
            }

            val eventId = "evt_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val confidence = candidate.confidence

            // Confidence tiering
            if (confidence >= 0.95f) {
                // HIGH CONFIDENCE: Auto-record transaction and update ledger
                val txItem = TransactionItem(
                    userId = userId,
                    title = candidate.title,
                    amount = candidate.amount,
                    category = candidate.category,
                    type = candidate.type,
                    timestamp = notification.timestamp,
                    timeLabel = "Just now",
                    merchant = candidate.merchant,
                    source = TransactionSource.AUTO_DETECTED,
                    walletName = candidate.walletName,
                    fingerprint = fingerprint,
                    confidence = confidence,
                    isConfirmed = true,
                    syncState = "PENDING_SYNC"
                )
                val insertedId = transactionDao.insertTransaction(txItem)
                val finalTx = txItem.copy(id = insertedId)

                // Record Detection Event
                detectionEventDao.insertEvent(
                    DetectionEventEntity(
                        id = eventId,
                        userId = userId,
                        provider = candidate.walletName,
                        packageName = notification.packageName,
                        rawTitle = notification.title,
                        rawText = notification.text,
                        rawSnippet = "${notification.title} - ${notification.text}".take(150),
                        amount = candidate.amount,
                        type = candidate.type.name,
                        merchant = candidate.merchant,
                        timestamp = notification.timestamp,
                        confidence = confidence,
                        fingerprint = fingerprint,
                        status = DetectionStatus.AUTO_RECORDED,
                        associatedTransactionId = insertedId
                    )
                )

                // Enqueue sync task
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        userId = userId,
                        entityType = "TRANSACTION",
                        entityId = insertedId.toString(),
                        operation = SyncOperation.INSERT,
                        payloadJson = """{"id":$insertedId,"title":"${candidate.title}","amount":${candidate.amount},"category":"${candidate.category}"}"""
                    )
                )

                // Post in-app alert
                _detectionAlertFlow.tryEmit(
                    DetectionAlert(
                        message = "Auto-recorded ${candidate.title} (Rp ${candidate.amount}) via ${candidate.walletName} ✨",
                        transaction = finalTx,
                        status = DetectionStatus.AUTO_RECORDED
                    )
                )

                Log.i("WalletCoordinator", "Auto-recorded HIGH confidence tx: ${candidate.title} (${candidate.amount})")
                Pair(true, "Auto-recorded ${candidate.title} (Rp ${candidate.amount})")
            } else if (confidence >= 0.70f) {
                // MEDIUM CONFIDENCE: Record as pending transaction for user confirmation
                val pendingTx = TransactionItem(
                    userId = userId,
                    title = candidate.title,
                    amount = candidate.amount,
                    category = candidate.category,
                    type = candidate.type,
                    timestamp = notification.timestamp,
                    timeLabel = "Just now",
                    merchant = candidate.merchant,
                    source = TransactionSource.AUTO_DETECTED,
                    walletName = candidate.walletName,
                    fingerprint = fingerprint,
                    confidence = confidence,
                    isConfirmed = false, // Pending confirmation
                    syncState = "LOCAL_ONLY"
                )
                val insertedId = transactionDao.insertTransaction(pendingTx)
                val finalPending = pendingTx.copy(id = insertedId)

                detectionEventDao.insertEvent(
                    DetectionEventEntity(
                        id = eventId,
                        userId = userId,
                        provider = candidate.walletName,
                        packageName = notification.packageName,
                        rawTitle = notification.title,
                        rawText = notification.text,
                        rawSnippet = "${notification.title} - ${notification.text}".take(150),
                        amount = candidate.amount,
                        type = candidate.type.name,
                        merchant = candidate.merchant,
                        timestamp = notification.timestamp,
                        confidence = confidence,
                        fingerprint = fingerprint,
                        status = DetectionStatus.PENDING_REVIEW,
                        associatedTransactionId = insertedId
                    )
                )

                _detectionAlertFlow.tryEmit(
                    DetectionAlert(
                        message = "New detected payment: ${candidate.title} (Rp ${candidate.amount}) - Tap to review",
                        transaction = finalPending,
                        status = DetectionStatus.PENDING_REVIEW
                    )
                )

                Log.i("WalletCoordinator", "Saved MEDIUM confidence pending tx: ${candidate.title} (${candidate.amount})")
                Pair(true, "Pending review: ${candidate.title} (Rp ${candidate.amount})")
            } else {
                // LOW CONFIDENCE: Log event only, no transaction created
                detectionEventDao.insertEvent(
                    DetectionEventEntity(
                        id = eventId,
                        userId = userId,
                        provider = candidate.walletName,
                        packageName = notification.packageName,
                        rawTitle = notification.title,
                        rawText = notification.text,
                        rawSnippet = "${notification.title} - ${notification.text}".take(150),
                        amount = candidate.amount,
                        type = candidate.type.name,
                        merchant = candidate.merchant,
                        timestamp = notification.timestamp,
                        confidence = confidence,
                        fingerprint = fingerprint,
                        status = DetectionStatus.REJECTED
                    )
                )
                Pair(false, "Low confidence notification ignored")
            }
        } catch (e: Exception) {
            Log.e("WalletCoordinator", "Error processing wallet notification", e)
            Pair(false, "Processing error: ${e.message}")
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
