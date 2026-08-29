package com.vinote.domain.ai

data class AiModelConfig(
    val selectedModel: String = "google/gemini-2.5-flash",
    val availableModels: List<String> = listOf(
        "google/gemini-2.5-flash",
        "anthropic/claude-3.5-sonnet",
        "openai/gpt-4o-mini",
        "meta-llama/llama-3.3-70b-instruct",
        "mistralai/mistral-small-3"
    ),
    val isOnlineAiEnabled: Boolean = true,
    val apiKey: String = "",
    val autoConfirmThreshold: Float = 0.90f
)
