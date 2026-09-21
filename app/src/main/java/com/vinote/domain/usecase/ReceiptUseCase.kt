package com.vinote.domain.usecase

import android.graphics.Bitmap
import com.vinote.data.engine.ExtractedReceiptData
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import com.vinote.data.model.TransactionSource
import com.vinote.services.ai.HybridAiProcessor
import com.vinote.ui.components.FormatUtils
import javax.inject.Inject
import javax.inject.Singleton

data class ReceiptResult(
    val pendingTransaction: TransactionItem,
    val extractedData: ExtractedReceiptData
)

interface ReceiptUseCaseInterface {
    suspend fun processReceipt(bitmap: Bitmap, userId: String): ReceiptResult
}

@Singleton
class ReceiptUseCaseImpl @Inject constructor(
    private val hybridAiProcessor: HybridAiProcessor
) : ReceiptUseCaseInterface {

    override suspend fun processReceipt(bitmap: Bitmap, userId: String): ReceiptResult {
        val parsedData = hybridAiProcessor.processReceipt(bitmap)

        val pendingTx = TransactionItem(
            userId = userId,
            title = parsedData.merchant,
            amount = parsedData.totalAmount,
            category = parsedData.category,
            type = TransactionType.EXPENSE,
            merchant = parsedData.merchant,
            source = TransactionSource.SCAN,
            timeLabel = "Just now"
        )

        return ReceiptResult(pendingTx, parsedData)
    }
}