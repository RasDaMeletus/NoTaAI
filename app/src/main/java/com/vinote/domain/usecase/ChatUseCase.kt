package com.vinote.domain.usecase

import com.vinote.domain.ai.NoTaAiService
import com.vinote.domain.ai.NoTaFinanceTools
import com.vinote.data.model.ChatMessage
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.GoalItem
import com.vinote.data.model.BankAccountItem
import com.vinote.data.model.UserProfile
import com.vinote.core.ai.OpenRouterMessage
import com.vinote.domain.ai.AiAction
import com.vinote.domain.ai.AiIntent
import com.vinote.data.model.NotaEyeState
import com.vinote.data.model.TransactionSource
import javax.inject.Inject
import javax.inject.Singleton

data class ChatResponse(
    val message: ChatMessage,
    val pendingTransaction: TransactionItem? = null
)

interface ChatUseCaseInterface {
    suspend fun sendChatMessage(
        userText: String,
        userId: String,
        userProfile: UserProfile,
        transactions: List<TransactionItem>,
        goals: List<GoalItem>,
        accounts: List<BankAccountItem>,
        dailyLimit: Long,
        safeMoney: Long,
        chatHistory: List<ChatMessage>,
        isOnlineAllowed: Boolean
    ): ChatResponse
}

@Singleton
class ChatUseCaseImpl @Inject constructor(
    private val aiService: NoTaAiService,
    private val financeTools: NoTaFinanceTools
) : ChatUseCaseInterface {

    override suspend fun sendChatMessage(
        userText: String,
        userId: String,
        userProfile: UserProfile,
        transactions: List<TransactionItem>,
        goals: List<GoalItem>,
        accounts: List<BankAccountItem>,
        dailyLimit: Long,
        safeMoney: Long,
        chatHistory: List<ChatMessage>,
        isOnlineAllowed: Boolean
    ): ChatResponse {
        val balance = financeTools.getCurrentBalance(userId)
        val dailySpent = financeTools.getDailySpending(userId)
        val recentTxs = financeTools.getRecentTransactionsSummary(userId)
        val budgetStatus = financeTools.getBudgetStatus(userId)
        val goalsSummary = financeTools.getGoalsProgressSummary(userId)

        val systemContext = financeTools.buildGroundedSystemContext(
            userId = userId,
            balance = balance,
            dailySpent = dailySpent,
            txSummary = recentTxs,
            budgetSummary = budgetStatus,
            goalsSummary = goalsSummary
        )

        val history = chatHistory.takeLast(6).map {
            OpenRouterMessage(if (it.isUser) "user" else "assistant", it.text)
        }

        val aiResponse = aiService.chatWithNota(
            userMessage = userText,
            userProfile = userProfile,
            transactions = transactions,
            goals = goals,
            accounts = accounts,
            dailyLimit = dailyLimit,
            safeMoney = safeMoney,
            conversationHistory = history,
            isOnlineAllowed = isOnlineAllowed
        )

        var pendingTx: TransactionItem? = null
        if (aiResponse.action is AiAction.ProposeTransaction) {
            val parsed = aiResponse.action.transaction
            pendingTx = TransactionItem(
                userId = userId,
                title = parsed.title,
                amount = parsed.amount,
                category = parsed.category,
                type = parsed.type,
                merchant = parsed.merchant,
                walletName = parsed.wallet,
                source = TransactionSource.AUTO_DETECTED,
                timeLabel = "Just now"
            )
        }

        val eye = when (aiResponse.intent) {
            AiIntent.CREATE_TRANSACTION -> NotaEyeState.EXCITED
            AiIntent.QUERY_BALANCE -> NotaEyeState.HAPPY
            AiIntent.FINANCIAL_ADVICE -> NotaEyeState.PROUD
            AiIntent.QUERY_SPENDING -> NotaEyeState.THINKING
            else -> NotaEyeState.HAPPY
        }

        val responseMessage = ChatMessage(
            text = aiResponse.message,
            isUser = false,
            quickChips = aiResponse.suggestedChips,
            eyeState = eye
        )

        return ChatResponse(responseMessage, pendingTx)
    }
}