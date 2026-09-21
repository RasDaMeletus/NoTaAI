package com.vinote.domain.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ClipboardSecurityHelper {

    /**
     * Copies text to clipboard and schedules automatic clearing after [clearDelayMs] (default 30 seconds).
     */
    fun copyWithAutoClear(
        context: Context,
        label: String,
        text: String,
        scope: CoroutineScope,
        clearDelayMs: Long = 30_000L
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)

        scope.launch(Dispatchers.Main) {
            delay(clearDelayMs)
            val currentClip = clipboard.primaryClip
            if (currentClip != null && currentClip.itemCount > 0) {
                val currentText = currentClip.getItemAt(0).text?.toString()
                if (currentText == text) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        }
    }
}
