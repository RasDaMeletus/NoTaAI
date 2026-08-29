package com.vinote.data.remote.api

import com.vinote.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

/**
 * Current authenticated user profile endpoints.
 * Authorization is server-side; the user is resolved from the Auth.js session.
 */
interface UserApi {

    @GET("api/me")
    suspend fun getCurrentUser(): UserDto

    @PATCH("api/me")
    suspend fun updateUser(@Body user: UserDto): UserDto
}
