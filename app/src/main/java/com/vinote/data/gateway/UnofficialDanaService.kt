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
 * DANA balance checker using the Official DANA Widget API.
 *
 * Uses the official DANA SDK endpoints from:
 * - https://github.com/dana-id/dana-node
 * - https://dashboard.dana.id/api-docs-v2/
 *
 * Flow:
 * 1. User logs into DANA widget (WebView) → gets access token
 * 2. App sends access token to this service
 * 3. Service queries balance via /dana/member/query/queryUserProfile.htm
 *
 * ⚠️ Official DANA API — requires partner registration.
 * ⚠️ Sandbox available for testing (free).
 * ⚠️ Production requires DANA merchant/partner agreement.
 */
class UnofficialDanaService(
    private val supabaseEdgeFunctionUrl: String,
    private val supabaseAnonKey: String
) : PaymentGatewayService {

    companion object {
        private const val TAG = "DanaService"
        // From dana-id/dana-node: sandbox base URL
        private const val DANA_SANDBOX_BASE = "https://api.sandbox.dana.id"
        private const val DANA_PROD_BASE = "https://api.saas.dana.id"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override val gatewayName: String = "DANA"

    override fun supportsProvider(provider: PaymentGatewayService.Provider): Boolean {
        return provider == PaymentGatewayService.Provider.DANA
    }

    override suspend fun fetchBalance(
        provider: PaymentGatewayService.Provider,
        accountId: String,
        accessToken: String?
    ): BalanceFetchResult = withContext(Dispatchers.IO) {
        if (accessToken == null) {
            return@withContext BalanceFetchResult.LinkingRequired(
                provider = "DANA",
                message = "Please sign in to DANA first to fetch your balance."
            )
        }

        try {
            // From dana-id/dana-node: WidgetApi.balanceInquiry
            // POST /dana/member/query/queryUserProfile.htm
            val request = Request.Builder()
                .url("$DANA_SANDBOX_BASE/dana/member/query/queryUserProfile.htm")
                .post(JSONObject().apply {
                    put("partnerReferenceNo", "NOTA-${System.currentTimeMillis()}")
                    put("merchantId", "1970010100000000000000")
                    put("token", accessToken)
                    put("resourceTypes", org.json.JSONArray().apply {
                        put("BALANCE")
                    })
                }.toString().toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-PARTNER-ID", "1970010100000000000000")
                .addHeader("X-TIMESTAMP", java.time.Instant.now().toString())
                .addHeader("X-REQUEST-ID", "NOTA-${System.currentTimeMillis()}")
                .addHeader("X-EXTERNAL-ID", "NOTA-${System.currentTimeMillis()}")
                .addHeader("CHANNEL-ID", "95221")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val json = JSONObject(responseBody)
            val resultCode = json.optJSONObject("result")
                ?.optString("resultCode", "")

            if (resultCode == "00000000") {
                val resourceInfos = json.optJSONArray("resourceInfos")
                if (resourceInfos != null) {
                    for (i in 0 until resourceInfos.length()) {
                        val info = resourceInfos.getJSONObject(i)
                        if (info.optString("resourceType") == "BALANCE") {
                            val balanceValue = info.optString("resourceValue", "{}")
                            val balanceJson = JSONObject(balanceValue)
                            val amount = balanceJson.optString("amount", "0").toLongOrNull() ?: 0

                            return@withContext BalanceFetchResult.Success(
                                balance = amount,
                                provider = "DANA",
                                accountId = accountId,
                                rawResponse = responseBody
                            )
                        }
                    }
                }
            }

            BalanceFetchResult.Error(
                message = json.optJSONObject("result")?.optString("resultCode")
                    ?: "DANA balance inquiry failed",
                provider = "DANA"
            )
        } catch (e: Exception) {
            Log.e(TAG, "DANA balance fetch failed", e)
            BalanceFetchResult.Error(
                message = e.message ?: "Unknown error",
                provider = "DANA"
            )
        }
    }

    override suspend fun initiateLinking(
        provider: PaymentGatewayService.Provider,
        phoneNumber: String
    ): BalanceFetchResult = withContext(Dispatchers.IO) {
        // DANA uses WebView-based widget flow, not OTP
        BalanceFetchResult.LinkingRequired(
            provider = "DANA",
            linkingUrl = "https://sandbox.dana.id/widget/bind?phone=$phoneNumber",
            message = "Open DANA widget to link your account. You'll be redirected to DANA's login page."
        )
    }
}
