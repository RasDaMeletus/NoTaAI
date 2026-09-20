package com.vinote.data.gateway

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Official Midtrans Gateway integration.
 *
 * Supports GoPay Tokenization (balance inquiry via BI-SNAP) and
 * OVO/DANA via BI-SNAP registration-account-inquiry.
 */
class MidtransGatewayService(
    private val supabaseEdgeFunctionUrl: String,
    private val supabaseAnonKey: String
) : PaymentGatewayService {

    companion object {
        private const val TAG = "MidtransGateway"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override val gatewayName: String = "Midtrans (Official)"

    override fun supportsProvider(provider: PaymentGatewayService.Provider): Boolean {
        return provider in listOf(
            PaymentGatewayService.Provider.GOPAY,
            PaymentGatewayService.Provider.OVO,
            PaymentGatewayService.Provider.DANA,
        )
    }

    override suspend fun fetchBalance(
        provider: PaymentGatewayService.Provider,
        accountId: String,
        accessToken: String?
    ): BalanceFetchResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("action", "midtrans-balance")
                put("provider", provider.name)
                put("accountId", accountId)
                accessToken?.let { put("accessToken", it) }
            }

            val request = Request.Builder()
                .url("$supabaseEdgeFunctionUrl/gateway-proxy")
                .post(body.toString().toRequestBody(jsonMediaType))
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext BalanceFetchResult.Error(
                    message = "HTTP ${response.code}: $responseBody",
                    code = response.code.toString(),
                    provider = provider.displayName
                )
            }

            val json = JSONObject(responseBody)
            val balance = json.optLong("balance", -1)
            val status = json.optString("status", "UNKNOWN")

            if (balance >= 0) {
                BalanceFetchResult.Success(
                    balance = balance,
                    provider = provider.displayName,
                    accountId = accountId,
                    rawResponse = responseBody
                )
            } else {
                BalanceFetchResult.Error(
                    message = json.optString("message", "Balance fetch failed: $status"),
                    code = status,
                    provider = provider.displayName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Balance fetch failed for ${provider.displayName}", e)
            BalanceFetchResult.Error(
                message = e.message ?: "Unknown error",
                provider = provider.displayName
            )
        }
    }

    override suspend fun initiateLinking(
        provider: PaymentGatewayService.Provider,
        phoneNumber: String
    ): BalanceFetchResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("action", "link-account")
                put("provider", provider.name)
                put("phoneNumber", phoneNumber)
            }

            val request = Request.Builder()
                .url("$supabaseEdgeFunctionUrl/gateway-proxy")
                .post(body.toString().toRequestBody(jsonMediaType))
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val json = JSONObject(responseBody)
            val linkingUrl = json.optString("linkingUrl", "")
            val accountId = json.optString("accountId", "")

            if (linkingUrl.isNotEmpty()) {
                BalanceFetchResult.LinkingRequired(
                    provider = provider.displayName,
                    linkingUrl = linkingUrl,
                    message = "Open the link to link your ${provider.displayName} account"
                )
            } else {
                BalanceFetchResult.Error(
                    message = json.optString("message", "Linking initiation failed"),
                    provider = provider.displayName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Linking failed for ${provider.displayName}", e)
            BalanceFetchResult.Error(
                message = e.message ?: "Unknown error",
                provider = provider.displayName
            )
        }
    }
}
