package com.vinote.data.remote.dto

import com.squareup.moshi.Json

data class BudgetDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "category") val category: String,
    @Json(name = "monthKey") val monthKey: String,
    @Json(name = "limitAmount") val limitAmount: Long,
    @Json(name = "spentAmount") val spentAmount: Long
)