package com.vinote.data.sync

import com.vinote.data.local.TransactionDao
import com.vinote.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SupabaseRealtimeManager(
    private val clientProvider: SupabaseClientProvider,
    private val transactionDao: TransactionDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    fun startListening(userId: String) {
        if (_isSubscribed.value) return

        scope.launch {
            try {
                val client = clientProvider.clientOrNull ?: return@launch
                val channel = client.realtime.channel("public:transactions:$userId")
                channel.subscribe()
                _isSubscribed.value = true
            } catch (_: Throwable) {
                _isSubscribed.value = false
            }
        }
    }

    fun stopListening(userId: String) {
        if (!_isSubscribed.value) return

        scope.launch {
            try {
                val client = clientProvider.clientOrNull ?: return@launch
                val channel = client.realtime.channel("public:transactions:$userId")
                channel.unsubscribe()
                _isSubscribed.value = false
            } catch (_: Throwable) {
                _isSubscribed.value = false
            }
        }
    }
}
