package com.vinote.data.remote.api

import com.vinote.data.remote.dto.WalletAccountDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface WalletApi {

    @GET("api/wallets")
    suspend fun getWallets(): List<WalletAccountDto>

    @POST("api/wallets")
    suspend fun createWallet(@Body wallet: WalletAccountDto): WalletAccountDto

    @PATCH("api/wallets/{id}")
    suspend fun updateWallet(@Path("id") id: String, @Body wallet: WalletAccountDto): WalletAccountDto

    @DELETE("api/wallets/{id}")
    suspend fun deleteWallet(@Path("id") id: String)
}
