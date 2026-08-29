package com.vinote.data.remote.api

import com.vinote.data.remote.dto.TransactionDto
import com.vinote.data.remote.dto.TransactionSyncRequest
import com.vinote.data.remote.dto.TransactionSyncResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TransactionApi {

    @GET("api/transactions")
    suspend fun getTransactions(): List<TransactionDto>

    @POST("api/transactions")
    suspend fun createTransaction(@Body tx: TransactionDto): TransactionDto

    @PATCH("api/transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: String, @Body tx: TransactionDto): TransactionDto

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String)

    @POST("api/transactions/sync")
    suspend fun syncTransactions(@Body request: TransactionSyncRequest): TransactionSyncResponse
}
