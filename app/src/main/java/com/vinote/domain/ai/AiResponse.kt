package com.vinote.domain.ai

import com.vinote.data.model.TransactionType

enum class AiIntent {
    CREATE_TRANSACTION,
    QUERY_BALANCE,
    QUERY_HISTORY,
    QUERY_SPENDING,
    QUERY_BUDGET,
    FINANCIAL_ADVICE,
    CREATE_GOAL,
    UPDATE_GOAL,
    GENERAL_CHAT,
    UNKNOWN;

    companion object {
        fun fromString(str: String?): AiIntent {
            if (str == null) return UNKNOWN
            return when (str.lowercase().trim()) {
                "create_transaction", "createtransaction", "add_transaction" -> CREATE_TRANSACTION
                "query_balance", "querybalance", "get_balance" -> QUERY_BALANCE
                "query_history", "queryhistory", "get_history" -> QUERY_HISTORY
                "query_spending", "queryspending", "get_spending" -> QUERY_SPENDING
                "query_budget", "querybudget", "get_budget" -> QUERY_BUDGET
                "financial_advice", "financialadvice", "advice" -> FINANCIAL_ADVICE
                "create_goal", "creategoal", "add_goal" -> CREATE_GOAL
                "update_goal", "updategoal" -> UPDATE_GOAL
                "general_chat", "chat" -> GENERAL_CHAT
                else -> UNKNOWN
            }
        }
    }
}

data class ParsedAiTransaction(
    val title: String,
    val amount: Long,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String = "General",
    val merchant: String = "",
    val wallet: String? = null,
    val confidence: Float = 0.95f,
    val description: String = ""
)

sealed interface AiAction {
    data class ProposeTransaction(val transaction: ParsedAiTransaction) : AiAction
    data class ShowFinancialInsight(val message: String, val quickChips: List<String> = emptyList()) : AiAction
    data class ProposeGoal(val title: String, val targetAmount: Long, val category: String) : AiAction
    data class AnswerQuestion(val answer: String, val quickChips: List<String> = emptyList()) : AiAction
    data class NeedsClarification(val question: String) : AiAction
}

data class AiResponse(
    val intent: AiIntent,
    val message: String,
    val action: AiAction? = null,
    val structuredTransaction: ParsedAiTransaction? = null,
    val suggestedChips: List<String> = emptyList(),
    val isFromOfflineEngine: Boolean = false
)
