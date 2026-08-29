package com.vinote.data.remote.dto

import com.squareup.moshi.Json

data class WalletAccountDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String,       // "E_WALLET" | "BANK"
    @Json(name = "provider") val provider: String,
    @Json(name = "openingBalance") val openingBalance: Long,
    @Json(name = "providerBalance") val providerBalance: Long?,
    @Json(name = "currency") val currency: String,
    @Json(name = "isActive") val isActive: Boolean,
    @Json(name = "isAutoDetectEnabled") val isAutoDetectEnabled: Boolean
)