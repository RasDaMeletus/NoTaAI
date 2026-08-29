package com.vinote.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val quickChips: List<String> = emptyList(),
    val eyeState: NotaEyeState = NotaEyeState.NEUTRAL
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = true,
    val countLabel: String = ""
)
