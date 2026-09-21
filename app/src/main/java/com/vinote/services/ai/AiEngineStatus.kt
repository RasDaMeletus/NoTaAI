package com.vinote.services.ai

data class AiEngineStatus(
    val isOnline: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val connectionType: String = "",
    val isWifiOnlyPreferred: Boolean = false,
    val isForceOffline: Boolean = false,
    val hasApiKey: Boolean = false
)
