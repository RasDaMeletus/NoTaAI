package com.vinote.data.remote.api

import com.vinote.data.remote.dto.AiChatRequest
import com.vinote.data.remote.dto.AiChatResponse
import com.vinote.data.remote.dto.AiTransactionDraftRequest
import com.vinote.data.remote.dto.AiTransactionDraftResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * NoTa AI backend endpoints. Android never talks directly to OpenRouter in
 * production — the backend proxies AI requests so the OpenRouter API key
 * never reaches the APK (PRD section 12).
 */
interface AiApi {

    @POST("api/ai/chat")
    suspend fun chat(@Body request: AiChatRequest): AiChatResponse

    @POST("api/ai/transaction-draft")
    suspend fun transactionDraft(@Body request: AiTransactionDraftRequest): AiTransactionDraftResponse
}
