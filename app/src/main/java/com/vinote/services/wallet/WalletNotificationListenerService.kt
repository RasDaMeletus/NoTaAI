package com.example.services.wallet

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.local.ViNoteDatabase
import com.example.domain.wallet.WalletNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Native Android NotificationListenerService for automatic real-time E-Wallet and Banking detection.
 * Captures notifications from GoPay, OVO, DANA, BCA, Mandiri, etc. and delegates to WalletDetectionCoordinator.
 * Survives Activity lifecycle closure and reboots.
 */
class WalletNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var coordinatorInstance: WalletDetectionCoordinator? = null

    override fun onCreate() {
        super.onCreate()
        ensureCoordinator()
    }

    private fun ensureCoordinator(): WalletDetectionCoordinator {
        val existing = coordinator ?: coordinatorInstance
        if (existing != null) return existing

        val db = ViNoteDatabase.getDatabase(applicationContext)
        val newCoordinator = WalletDetectionCoordinator(
            transactionDao = db.transactionDao(),
            detectionEventDao = db.detectionEventDao(),
            walletAccountDao = db.walletAccountDao(),
            syncQueueDao = db.syncQueueDao()
        )
        coordinatorInstance = newCoordinator
        coordinator = newCoordinator
        return newCoordinator
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        if (title.isBlank() && text.isBlank()) return

        val notificationObj = WalletNotification(
            packageName = packageName,
            title = title,
            text = text,
            bigText = bigText,
            timestamp = sbn.postTime,
            id = "${sbn.id}_${sbn.postTime}"
        )

        Log.d("WalletNotifListener", "Captured notification from $packageName: $title - $text")

        if (isListenerActive) {
            serviceScope.launch {
                val coord = ensureCoordinator()
                coord.processNotification(notificationObj, activeUserId)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        Log.i("WalletNotifListener", "ViNote Notification Listener successfully connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected = false
        Log.i("WalletNotifListener", "ViNote Notification Listener disconnected")
    }

    companion object {
        var coordinator: WalletDetectionCoordinator? = null
        var isListenerActive: Boolean = true
        var isServiceConnected: Boolean = false
        var activeUserId: String = "user_default"
    }
}

