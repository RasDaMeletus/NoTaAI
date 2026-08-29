package com.vinote.data.remote.dto

import com.squareup.moshi.Json

/**
 * Auth.js/NextAuth session response.
 * Contains the canonical user identity used by the backend for authorization.
 */
data class SessionResponse(
    @Json(name = "user") val user: UserDto?,
    @Json(name = "expires") val expires: String?
)