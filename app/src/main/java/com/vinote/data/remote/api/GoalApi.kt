package com.vinote.data.remote.api

import com.vinote.data.remote.dto.GoalDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface GoalApi {
    @GET("api/goals")
    suspend fun getGoals(): List<GoalDto>

    @POST("api/goals")
    suspend fun createGoal(@Body goal: GoalDto): GoalDto

    @PATCH("api/goals/{id}")
    suspend fun updateGoal(@Path("id") id: String, @Body goal: GoalDto): GoalDto

    @DELETE("api/goals/{id}")
    suspend fun deleteGoal(@Path("id") id: String)
}
