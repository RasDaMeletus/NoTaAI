package com.vinote.data.remote.api

import com.vinote.data.remote.dto.BudgetDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface BudgetApi {
    @GET("api/budgets")
    suspend fun getBudgets(): List<BudgetDto>

    @POST("api/budgets")
    suspend fun createBudget(@Body budget: BudgetDto): BudgetDto

    @PATCH("api/budgets/{id}")
    suspend fun updateBudget(@Path("id") id: String, @Body budget: BudgetDto): BudgetDto
}
