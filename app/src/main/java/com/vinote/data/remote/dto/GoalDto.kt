package com.vinote.data.remote.dto

import com.squareup.moshi.Json

data class GoalDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "title") val title: String,
    @Json(name = "targetAmount") val targetAmount: Long,
    @Json(name = "currentAmount") val currentAmount: Long,
    @Json(name = "targetDate") val targetDate: String,
    @Json(name = "category") val category: String,
    @Json(name = "iconName") val iconName: String,
    @Json(name = "colorHex") val colorHex: String
)