package com.example.services.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.engine.ExtractedReceiptData
import com.example.data.engine.ExtractedVoiceEntity
import com.example.data.engine.OfflineNlpEngine
import com.example.services.media.ReceiptImageProcessor
import com.example.services.network.ConnectionType
import com.example.services.network.NetworkConnectivityMonitor
import com.example.services.network.NetworkStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class AiEngineStatus(
    val isOnline: Boolean = false,
    val isWifi: Boolean = false,
    val engineName: String = "On-Device Engine",
    val connectionType: String = "Offline",
    val hasApiKey: Boolean = false,
    val isWifiOnlyPreferred: Boolean = true,
    val isForceOffline: Boolean = false
)

class HybridAiProcessor(
    private val context: Context,
    private val networkMonitor: NetworkConnectivityMonitor = NetworkConnectivityMonitor(context),
    private val huggingFaceService: HuggingFaceApiService = HuggingFaceApiService()
) {
    private val _hfApiKey = MutableStateFlow<String>("")
    val hfApiKey: StateFlow<String> = _hfApiKey.asStateFlow()

    private val _wifiOnlyForCloud = MutableStateFlow(false)
    val wifiOnlyForCloud: StateFlow<Boolean> = _wifiOnlyForCloud.asStateFlow()

    private val _forceOfflineMode = MutableStateFlow(false)
    val forceOfflineMode: StateFlow<Boolean> = _forceOfflineMode.asStateFlow()

    val engineStatus: Flow<AiEngineStatus> = combine(
        networkMonitor.observeNetworkStatus(),
        _hfApiKey,
        _wifiOnlyForCloud,
        _forceOfflineMode
    ) { netStatus, apiKey, wifiOnly, forceOffline ->
        val canUseCloud = !forceOffline && netStatus.isConnected && (!wifiOnly || netStatus.isWifi)
        val name = if (canUseCloud) "Hugging Face Cloud AI ⚡" else "On-Device Neural Engine 🔒"
        val connType = when (netStatus.connectionType) {
            ConnectionType.WIFI -> "Wi-Fi"
            ConnectionType.CELLULAR -> "Cellular Data"
            ConnectionType.ETHERNET -> "Ethernet"
            ConnectionType.OFFLINE -> "Offline"
        }
        AiEngineStatus(
            isOnline = canUseCloud,
            isWifi = netStatus.isWifi,
            engineName = name,
            connectionType = connType,
            hasApiKey = apiKey.isNotBlank(),
            isWifiOnlyPreferred = wifiOnly,
            isForceOffline = forceOffline
        )
    }

    fun setHuggingFaceApiKey(key: String) {
        _hfApiKey.value = key.trim()
    }

    fun setWifiOnlyPreference(enabled: Boolean) {
        _wifiOnlyForCloud.value = enabled
    }

    fun setForceOfflineMode(forced: Boolean) {
        _forceOfflineMode.value = forced
    }

    /**
     * Hybrid Receipt OCR:
     * - Uses Hugging Face Vision Inference if connected to Wi-Fi / Internet
     * - Seamlessly falls back to On-Device OCR & Pattern Analysis if offline
     */
    suspend fun processReceipt(bitmap: Bitmap): ExtractedReceiptData {
        val netStatus = networkMonitor.getCurrentNetworkStatus()
        val canUseOnline = !_forceOfflineMode.value && netStatus.isConnected && (!_wifiOnlyForCloud.value || netStatus.isWifi)

        if (canUseOnline) {
            Log.d("HybridAI", "Attempting Hugging Face online OCR processing...")
            val onlineResult = huggingFaceService.performOnlineReceiptOcr(
                bitmap = bitmap,
                apiKey = _hfApiKey.value
            )
            if (onlineResult.isSuccess) {
                val data = onlineResult.getOrNull()
                if (data != null && data.totalAmount > 0L) {
                    Log.d("HybridAI", "Successfully extracted receipt via Hugging Face Cloud AI!")
                    return data
                }
            } else {
                Log.w("HybridAI", "Hugging Face OCR was not successful, using on-device neural engine fallback", onlineResult.exceptionOrNull())
            }
        }

        // On-Device Fallback / Pure Offline Processing
        Log.d("HybridAI", "Processing receipt via On-Device Neural Engine...")
        return ReceiptImageProcessor.processReceiptBitmap(bitmap)
    }

    /**
     * Hybrid Voice / NLP Entity Extraction:
     * - Uses Hugging Face LLM / NLP reasoning if online
     * - Uses On-Device Indonesian rule engine if offline
     */
    suspend fun parseVoiceTranscript(transcript: String): ExtractedVoiceEntity {
        val netStatus = networkMonitor.getCurrentNetworkStatus()
        val canUseOnline = !_forceOfflineMode.value && netStatus.isConnected && (!_wifiOnlyForCloud.value || netStatus.isWifi)

        if (canUseOnline) {
            Log.d("HybridAI", "Attempting Hugging Face online NLP extraction for: \"$transcript\"")
            val onlineResult = huggingFaceService.performOnlineNlpExtraction(
                transcript = transcript,
                apiKey = _hfApiKey.value
            )
            if (onlineResult.isSuccess) {
                val parsed = onlineResult.getOrNull()
                if (parsed != null && parsed.amount > 0L) {
                    Log.d("HybridAI", "Successfully parsed voice expense via Hugging Face AI!")
                    return parsed
                }
            }
        }

        // Fast on-device parsing
        Log.d("HybridAI", "Parsing voice expense via On-Device Indonesian NLP engine...")
        return OfflineNlpEngine.parseSpokenTransaction(transcript)
    }
}
