package com.vinote.data.remote.dto

import com.squareup.moshi.Json

data class TransactionDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "amount") val amount: Long,
    @Json(name = "currency") val currency: String,
    @Json(name = "type") val type: String,        // income | expense | transfer | adjustment
    @Json(name = "category") val category: String,
    @Json(name = "title") val title: String,
    @Json(name = "merchant") val merchant: String,
    @Json(name = "walletId") val walletId: String?,
    @Json(name = "source") val source: String,
    @Json(name = "sourceEventId") val sourceEventId: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "createdAt") val createdAt: Long,
    @Json(name = "updatedAt") val updatedAt: Long,
    @Json(name = "syncState") val syncState: String,
    @Json(name = "confidence") val confidence: Float?,
    @Json(name = "confirmationState") val confirmationState: String
)