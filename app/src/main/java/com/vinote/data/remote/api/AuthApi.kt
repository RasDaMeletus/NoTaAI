package com.vinote.data.remote.api

import com.squareup.moshi.Json
import com.vinote.data.remote.dto.SessionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Backend auth/session endpoints. The backend owns the authentication flow
 * (session cookie issued at /api/auth/login, validated at /api/auth/session).
 * Android interacts with these endpoints to establish, restore, and invalidate
 * sessions per PRD section 6 (Authentication Architecture).
 */
interface AuthApi {

    @GET("api/auth/session")
    suspend fun getSession(): SessionResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: CredentialsRequest): SessionResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): SessionResponse

    @POST("api/auth/signout")
    suspend fun signOut(): Map<String, String>
}

data class CredentialsRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
)

data class RegisterRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "name") val name: String? = null,
)
