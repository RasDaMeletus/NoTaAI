package com.vinote.services.wallet

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.vinote.domain.wallet.WalletNotification
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Native Android NotificationListenerService for automatic real-time E-Wallet and Banking detection.
 *
 * Uses Hilt EntryPoint to obtain an application-scoped WalletTransactionProcessor,
 * replacing the mutable global processor state with lifecycle-safe dependency injection.
 *
 * Captures notifications from GoPay, OVO, DANA, BCA, Mandiri, etc. and pipes them
 * to the injected WalletTransactionProcessor.
 */
class WalletNotificationListenerService : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WalletProcessorEntryPoint {
        fun walletTransactionProcessor(): WalletTransactionProcessor
    }

    private val processor: WalletTransactionProcessor by lazy {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WalletProcessorEntryPoint::class.java
        )
        entryPoint.walletTransactionProcessor()
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

        // Process via application-scoped injected processor, off the main thread
        CoroutineScope(Dispatchers.IO).launch {
            processor.processNotification(notificationObj)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("WalletNotifListener", "ViNote Notification Listener successfully connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i("WalletNotifListener", "ViNote Notification Listener disconnected")
    }
}
