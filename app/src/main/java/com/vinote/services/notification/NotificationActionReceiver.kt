package com.vinote.services.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vinote.data.local.NoTaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val txId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (txId == -1L) return

        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notifId != -1) {
            notifManager?.cancel(notifId)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = NoTaDatabase.getDatabase(context)
                when (action) {
                    ACTION_CONFIRM_TRANSACTION -> {
                        db.transactionDao().confirmTransaction(txId)
                        Log.i("NotifActionReceiver", "Transaction $txId confirmed directly via notification action.")
                    }
                    ACTION_DISMISS_TRANSACTION -> {
                        db.transactionDao().deleteById(txId)
                        Log.i("NotifActionReceiver", "Transaction $txId rejected/deleted directly via notification action.")
                    }
                }
            } catch (e: Exception) {
                Log.e("NotifActionReceiver", "Failed to process notification action $action for tx $txId", e)
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM_TRANSACTION = "com.vinote.action.CONFIRM_TRANSACTION"
        const val ACTION_DISMISS_TRANSACTION = "com.vinote.action.DISMISS_TRANSACTION"
        const val EXTRA_TRANSACTION_ID = "extra_tx_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notif_id"
    }
}
