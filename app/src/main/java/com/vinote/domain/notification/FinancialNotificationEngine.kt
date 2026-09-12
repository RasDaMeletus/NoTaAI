package com.vinote.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.vinote.data.model.TransactionItem
import com.vinote.services.notification.NotificationActionReceiver
import com.vinote.ui.components.FormatUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class FinancialEventType {
    TRANSACTION_DETECTED,
    PENDING_REVIEW,
    BALANCE_CHANGED,
    BUDGET_WARNING,
    UNUSUAL_SPENDING,
    GOAL_PROGRESS,
    RECURRING_EXECUTED,
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
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (context == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val channelTx = NotificationChannel(
                CHANNEL_ID_TX,
                "Transaksi Otomatis",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi transaksi yang terdeteksi otomatis dari e-wallet"
            }

            val channelPending = NotificationChannel(
                CHANNEL_ID_PENDING,
                "Konfirmasi Transaksi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Konfirmasi transaksi pending dengan tombol aksi langsung"
            }

            val channelBudget = NotificationChannel(
                CHANNEL_ID_BUDGET,
                "Peringatan Anggaran",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Peringatan saat pengeluaran melebihi batas target harian/bulanan"
            }

            val channelGoals = NotificationChannel(
                CHANNEL_ID_GOALS,
                "Target & Tabungan",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pencapaian progres target tabungan NoTa"
            }

            val channelRecurring = NotificationChannel(
                CHANNEL_ID_RECURRING,
                "Transaksi Rutin",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi eksekusi otomatis transaksi rutin terjadwal"
            }

            manager.createNotificationChannels(listOf(channelTx, channelPending, channelBudget, channelGoals, channelRecurring))
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
        postSystemNotification(event.title, event.message, NOTIF_ID_TX, CHANNEL_ID_TX)
    }

    fun notifyPendingReview(transaction: TransactionItem) {
        val event = FinancialNotificationEvent(
            type = FinancialEventType.PENDING_REVIEW,
            title = "Konfirmasi Transaksi Baru",
            message = "${FormatUtils.formatRupiah(transaction.amount)} dari ${transaction.walletName ?: "E-Wallet"} - Tap untuk konfirmasi",
            transaction = transaction
        )
        _events.tryEmit(event)

        if (context == null) return
        try {
            val notifId = (NOTIF_ID_PENDING_BASE + (transaction.id % 10000)).toInt()

            // Action 1: Confirm / Catat
            val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_CONFIRM_TRANSACTION
                putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transaction.id)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
            }
            val confirmPendingIntent = PendingIntent.getBroadcast(
                context,
                (transaction.id * 2).toInt(),
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 2: Dismiss / Abaikan
            val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_DISMISS_TRANSACTION
                putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transaction.id)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                (transaction.id * 2 + 1).toInt(),
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID_PENDING)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(event.title)
                .setContentText(event.message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(android.R.drawable.checkbox_on_background, "Catat", confirmPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Abaikan", dismissPendingIntent)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(notifId, builder.build())
        } catch (_: Exception) {
            // Ignored in test environment
        }
    }

    fun notifyRecurringExecuted(transaction: TransactionItem) {
        val event = FinancialNotificationEvent(
            type = FinancialEventType.RECURRING_EXECUTED,
            title = "Transaksi Rutin Tercatat",
            message = "${transaction.title} (${FormatUtils.formatRupiah(transaction.amount)}) telah dicatat otomatis.",
            transaction = transaction
        )
        _events.tryEmit(event)
        postSystemNotification(event.title, event.message, NOTIF_ID_RECURRING, CHANNEL_ID_RECURRING)
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
        postSystemNotification(event.title, event.message, NOTIF_ID_BUDGET, CHANNEL_ID_BUDGET)
    }

    fun notifyGoalProgress(goalTitle: String, current: Long, target: Long) {
        val event = FinancialNotificationEvent(
            type = FinancialEventType.GOAL_PROGRESS,
            title = "🎯 Savings Milestone",
            message = "You've saved ${FormatUtils.formatRupiah(current)} of ${FormatUtils.formatRupiah(target)} for $goalTitle!"
        )
        _events.tryEmit(event)
        postSystemNotification(event.title, event.message, NOTIF_ID_GOAL, CHANNEL_ID_GOALS)
    }

    private fun postSystemNotification(title: String, message: String, id: Int, channelId: String = CHANNEL_ID_TX) {
        if (context == null) return
        try {
            val builder = NotificationCompat.Builder(context, channelId)
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
        const val CHANNEL_ID_TX = "vinote_tx_detected"
        const val CHANNEL_ID_PENDING = "vinote_pending_review"
        const val CHANNEL_ID_BUDGET = "vinote_budget_alerts"
        const val CHANNEL_ID_GOALS = "vinote_goals"
        const val CHANNEL_ID_RECURRING = "vinote_recurring"

        const val NOTIF_ID_TX = 1001
        const val NOTIF_ID_BUDGET = 1002
        const val NOTIF_ID_GOAL = 1003
        const val NOTIF_ID_RECURRING = 1004
        const val NOTIF_ID_PENDING_BASE = 2000
    }
}
