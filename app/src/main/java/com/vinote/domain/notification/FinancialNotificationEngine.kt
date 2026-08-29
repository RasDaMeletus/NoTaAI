package com.vinote.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.vinote.data.model.TransactionItem
import com.vinote.ui.components.FormatUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class FinancialEventType {
    TRANSACTION_DETECTED,
    BALANCE_CHANGED,
    BUDGET_WARNING,
    UNUSUAL_SPENDING,
    GOAL_PROGRESS,
    WEEKLY_SUMMARY
}

data class FinancialNotificationEvent(
    val type: FinancialEventType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val transaction: TransactionItem? = null
)

class FinancialNotificationEngine(private val context: Context? = null) {

    private val _events = MutableSharedFlow<FinancialNotificationEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<FinancialNotificationEvent> = _events.asSharedFlow()

    private var lastBudgetAlertTimestamp = 0L
    private val budgetAlertCooldownMs = 30 * 60 * 1000L // 30 mins cooldown

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (context == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ViNote Financial Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Real-time updates on automated wallet expenses and budget limits"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun notifyTransactionDetected(transaction: TransactionItem) {
        val event = FinancialNotificationEvent(
            type = FinancialEventType.TRANSACTION_DETECTED,
            title = "Detected ${transaction.walletName ?: "Wallet"} Expense",
            message = "${FormatUtils.formatRupiah(transaction.amount)} at ${transaction.merchant.ifBlank { transaction.title }} recorded automatically.",
            transaction = transaction
        )
        _events.tryEmit(event)
        postSystemNotification(event.title, event.message, NOTIF_ID_TX)
    }

    fun notifyBudgetExceeded(overAmount: Long, dailyLimit: Long) {
        val now = System.currentTimeMillis()
        if (now - lastBudgetAlertTimestamp < budgetAlertCooldownMs) {
            return // Respect cooldown
        }
        lastBudgetAlertTimestamp = now

        val event = FinancialNotificationEvent(
            type = FinancialEventType.BUDGET_WARNING,
            title = "⚠️ Daily Budget Exceeded!",
            message = "You have exceeded today's limit of ${FormatUtils.formatRupiah(dailyLimit)} by ${FormatUtils.formatRupiah(overAmount)}. NoTa is furious!"
        )
        _events.tryEmit(event)
        postSystemNotification(event.title, event.message, NOTIF_ID_BUDGET)
    }

    fun notifyGoalProgress(goalTitle: String, current: Long, target: Long) {
        val event = FinancialNotificationEvent(
            type = FinancialEventType.GOAL_PROGRESS,
            title = "🎯 Savings Milestone",
            message = "You've saved ${FormatUtils.formatRupiah(current)} of ${FormatUtils.formatRupiah(target)} for $goalTitle!"
        )
        _events.tryEmit(event)
    }

    private fun postSystemNotification(title: String, message: String, id: Int) {
        if (context == null) return
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(id, builder.build())
        } catch (_: Exception) {
            // Ignored if permissions or testing environment
        }
    }

    companion object {
        const val CHANNEL_ID = "vinote_financial_alerts"
        const val NOTIF_ID_TX = 1001
        const val NOTIF_ID_BUDGET = 1002
    }
}
