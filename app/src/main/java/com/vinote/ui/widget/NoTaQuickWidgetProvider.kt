package com.vinote.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.vinote.MainActivity
import com.vinote.ui.components.FormatUtils

class NoTaQuickWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val EXTRA_NAVIGATE_TO = "EXTRA_NAVIGATE_TO"
        const val DESTINATION_ADD_TRANSACTION = "ADD_TRANSACTION"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            safeBalance: Long? = null,
            totalBalance: Long? = null
        ) {
            val prefs = context.getSharedPreferences("vinote_widget_prefs", Context.MODE_PRIVATE)
            val storedSafe = safeBalance ?: prefs.getLong("safe_balance", 150_000L)
            val storedTotal = totalBalance ?: prefs.getLong("total_balance", 2_500_000L)

            val views = RemoteViews(context.packageName, R.layout.widget_quick_capture)
            views.setTextViewText(R.id.widget_safe_balance, FormatUtils.formatRupiah(storedSafe))
            views.setTextViewText(R.id.widget_total_balance, "Total Saldo: ${FormatUtils.formatRupiah(storedTotal)}")

            // Launch App on tap root
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Launch Add Transaction directly on tap "+ Catat"
            val addTxIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NAVIGATE_TO, DESTINATION_ADD_TRANSACTION)
            }
            val addTxPendingIntent = PendingIntent.getActivity(
                context,
                1,
                addTxIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, addTxPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun notifyDataChanged(context: Context, safeBalance: Long, totalBalance: Long) {
            val prefs = context.getSharedPreferences("vinote_widget_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("safe_balance", safeBalance)
                .putLong("total_balance", totalBalance)
                .apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, NoTaQuickWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId, safeBalance, totalBalance)
            }
        }
    }
}
