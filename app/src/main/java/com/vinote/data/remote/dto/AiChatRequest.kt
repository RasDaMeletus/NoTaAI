package com.vinote.data.remote.dto

import com.squareup.moshi.Json

data class AiChatRequest(
    @Json(name = "message") val message: String,
    @Json(name = "context") val context: AiContext?,
    @Json(name = "conversationHistory") val conversationHistory: List<AiMessage> = emptyList()
)

data class AiContext(
    @Json(name = "currentBalance") val currentBalance: Long,
    @Json(name = "wallets") val wallets: List<WalletAccountDto>,
    @Json(name = "transactions") val transactions: List<TransactionDto>,
    @Json(name = "goals") val goals: List<GoalDto>,
    @Json(name = "budgets") val budgets: List<BudgetDto>
)

data class AiMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

data class AiChatResponse(
    @Json(name = "reply") val reply: String,
    @Json(name = "intent") val intent: String,
    @Json(name = "action") val action: AiAction?,
    @Json(name = "suggestedChips") val suggestedChips: List<String> = emptyList()
)

data class AiAction(
    @Json(name = "type") val type: String,
    @Json(name = "payload") val payload: Map<String, Any> = emptyMap()
)