package com.vinote.data.remote.dto

import com.squareup.moshi.Json

data class AiTransactionDraftRequest(
    @Json(name = "naturalLanguage") val naturalLanguage: String,
    @Json(name = "detectedWallet") val detectedWallet: String?
)

data class AiTransactionDraftResponse(
    @Json(name = "title") val title: String,
    @Json(name = "amount") val amount: Long,
    @Json(name = "type") val type: String,
    @Json(name = "category") val category: String,
    @Json(name = "merchant") val merchant: String?,
    @Json(name = "wallet") val wallet: String?,
    @Json(name = "confidence") val confidence: Float
)