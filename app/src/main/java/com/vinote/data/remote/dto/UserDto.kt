package com.vinote.data.remote.dto

import com.squareup.moshi.Json

/**
 * Canonical user profile from backend. The `id` is the server-resolved
 * authenticated identity — never trust a client-supplied userId (PRD).
 */
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String,
    @Json(name = "name") val name: String?,
    @Json(name = "image") val image: String?
)