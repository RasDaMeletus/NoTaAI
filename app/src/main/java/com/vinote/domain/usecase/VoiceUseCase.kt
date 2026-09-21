package com.vinote.domain.usecase

import com.vinote.data.engine.ExtractedVoiceEntity
import com.vinote.data.engine.OfflineNlpEngine
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.services.ai.HybridAiProcessor
import javax.inject.Inject
import javax.inject.Singleton

data class VoiceParseResult(
    val parsed: ExtractedVoiceEntity,
    val pendingTransaction: TransactionItem? = null
)

interface VoiceUseCaseInterface {
    suspend fun parseVoiceText(text: String, userId: String): VoiceParseResult
}

@Singleton
class VoiceUseCaseImpl @Inject constructor(
    private val hybridAiProcessor: HybridAiProcessor
) : VoiceUseCaseInterface {

    override suspend fun parseVoiceText(text: String, userId: String): VoiceParseResult {
        val parsed = hybridAiProcessor.parseVoiceText(text)

        val pendingTx = TransactionItem(
            userId = userId,
            title = parsed.title,
            amount = parsed.amount,
            category = parsed.category,
            type = parsed.type,
            merchant = if (parsed.merchant.isNotBlank()) parsed.merchant else parsed.title,
            source = TransactionSource.VOICE,
            walletName = parsed.walletName,
            timeLabel = "Just now"
        )

        return VoiceParseResult(parsed, pendingTx)
    }
}