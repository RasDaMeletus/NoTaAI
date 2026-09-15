package com.vinote.services.wallet

import android.util.Log
import com.vinote.data.model.TransactionSource
import com.vinote.domain.ai.NoTaAiService
import com.vinote.domain.transaction.TransactionService
import com.vinote.domain.wallet.WalletDetectionService
import com.vinote.domain.wallet.WalletNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Orchestrates the full automatic wallet parsing pipeline:
 * Android Notification -> Adapter Identification -> Deterministic Parser -> Deduplication -> TransactionService
 */
class WalletTransactionProcessor(
    private val transactionService: TransactionService,
    private val deduplicationService: WalletDeduplicationService,
    private val aiService: NoTaAiService? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    fun processNotificationAsync(
        notification: WalletNotification,
        onProcessed: ((Boolean, String) -> Unit)? = null
    ) {
        scope.launch {
            val (success, message) = processNotification(notification)
            onProcessed?.invoke(success, message)
        }
    }

    suspend fun processNotification(notification: WalletNotification): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val adapter = WalletDetectionService.findAdapterForPackage(notification.packageName)
            if (adapter == null || !adapter.isFinancialNotification(notification)) {
                return@withContext Pair(false, "Not a recognized financial notification")
            }

            val parsedCandidate = adapter.parseNotification(notification)
            if (parsedCandidate == null || parsedCandidate.amount <= 0) {
                // Fallback to AI parser if local adapter couldn't extract amount
                val fullText = "${notification.title} ${notification.text} ${notification.bigText ?: ""}"
                val aiCandidate = aiService?.parseNaturalLanguageTransaction(fullText, isOnlineAllowed = true)
                if (aiCandidate != null && aiCandidate.amount > 0) {
                    val addResult = transactionService.addTransaction(
                        title = aiCandidate.title,
                        amount = aiCandidate.amount,
                        category = aiCandidate.category,
                        type = aiCandidate.type,
                        source = TransactionSource.E_WALLET,
                        merchant = aiCandidate.merchant,
                        walletName = aiCandidate.wallet ?: adapter.displayName
                    )
                    return@withContext if (addResult.isSuccess) {
                        Pair(true, "Auto-recorded ${aiCandidate.title}: Rp ${aiCandidate.amount}")
                    } else {
                        Pair(false, "Failed to insert transaction: ${addResult.exceptionOrNull()?.message}")
                    }
                }
                return@withContext Pair(false, "Could not extract valid financial amount")
            }

            // Check deduplication
            if (deduplicationService.isDuplicate(parsedCandidate)) {
                Log.d("WalletProcessor", "Duplicate notification ignored: ${parsedCandidate.title}")
                return@withContext Pair(false, "Duplicate notification ignored")
            }

            // Insert into unified TransactionService
            val addResult = transactionService.addTransaction(
                title = parsedCandidate.title,
                amount = parsedCandidate.amount,
                category = parsedCandidate.category,
                type = parsedCandidate.type,
                source = TransactionSource.E_WALLET,
                merchant = parsedCandidate.merchant,
                walletName = parsedCandidate.walletName,
                timestamp = notification.timestamp
            )

            if (addResult.isSuccess) {
                Log.i("WalletProcessor", "Successfully auto-recorded transaction ${parsedCandidate.title} (${parsedCandidate.amount}) from ${parsedCandidate.walletName}")
                Pair(true, "Recorded ${parsedCandidate.title} (${parsedCandidate.amount})")
            } else {
                Pair(false, "Transaction insertion error: ${addResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e("WalletProcessor", "Error processing wallet notification", e)
            Pair(false, "Error: ${e.message}")
        }
    }
}
