package com.vinote.domain.ai

import com.vinote.core.ai.FreeModels

data class AiModelConfig(
    val selectedModel: String = FreeModels.LING_FLASH_FIN,
    val availableModels: List<String> = FreeModels.CHAIN,
    val isOnlineAiEnabled: Boolean = true,
    val apiKey: String = "",
    val autoConfirmThreshold: Float = 0.90f
)
