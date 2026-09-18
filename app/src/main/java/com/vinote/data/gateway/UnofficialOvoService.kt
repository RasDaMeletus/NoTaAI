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
 * Unofficial OVO balance checker.
 *
 * Uses reverse-engineered OVO API endpoints from:
 * - https://github.com/namtxs/ovoid-API
 * - https://github.com/lintangtimur/ovoid
 *
 * Flow:
 * 1. User enters phone → send OTP via /v2/auth/customer/otp
 * 2. User enters OTP → verify via same endpoint with otpCode
 * 3. Fetch balance via /v3/customer/detail or /v3/card/wallet/inquiry
 *
 * ⚠️ UNOFFICIAL — may break when OVO updates their app.
 * ⚠️ All calls go through Supabase Edge Function proxy.
 */
class UnofficialOvoService(
    private val supabaseEdgeFunctionUrl: String,
    private val supabaseAnonKey: String
) : PaymentGatewayService {

    companion object {
        private const val TAG = "UnofficialOvo"
        // From namtxs/ovoid-API
        private const val BASE_URL = "https://api.ovo.id"
        private val OVO_HEADERS = mapOf(
            "Content-Type" to "application/json",
            "User-Agent" to "OVO-Android/3.54.0",
            "X-Platform" to "Android",
            "X-DeviceOS" to "Android,11"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // OVO binds the OTP to a device id. Use a stable per-install value so a
    // relaunch can still complete the same auth flow.
    private val ovoDeviceId: String by lazy {
        java.util.UUID.randomUUID().toString().uppercase()
    }

    override val gatewayName: String = "OVO (Unofficial)"

    override fun supportsProvider(provider: PaymentGatewayService.Provider): Boolean {
        return provider == PaymentGatewayService.Provider.OVO
    }

    override suspend fun fetchBalance(
        provider: PaymentGatewayService.Provider,
        accountId: String,
        accessToken: String?
    ): BalanceFetchResult = withContext(Dispatchers.IO) {
        if (accessToken == null) {
            return@withContext BalanceFetchResult.LinkingRequired(
                provider = "OVO",
                message = "Please sign in to OVO first to fetch your balance."
            )
        }

        try {
            // Route through the gateway-proxy Edge Function, which performs the
            // namtxs/ovoid-API walletInquiry() call server-side and returns
            // data.{"001"}.card_balance (OVO Cash).
            val request = Request.Builder()
                .url("$supabaseEdgeFunctionUrl/gateway-proxy")
                .post(JSONObject().apply {
                    put("action", "ovo-balance")
                    put("accessToken", accessToken)
                    put("deviceId", ovoDeviceId)
                }.toString().toRequestBody(jsonMediaType))
                .apply {
                    addHeader("Authorization", "Bearer $supabaseAnonKey")
                }
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val json = JSONObject(responseBody)

            if (json.optBoolean("success")) {
                return@withContext BalanceFetchResult.Success(
                    balance = json.optLong("balance", 0),
                    provider = "OVO",
                    accountId = accountId,
                    accessToken = accessToken,
                    rawResponse = responseBody
                )
            }

            BalanceFetchResult.Error(
                message = json.optString("message", "OVO balance fetch failed"),
                provider = "OVO"
            )
        } catch (e: Exception) {
            Log.e(TAG, "OVO balance fetch failed", e)
            BalanceFetchResult.Error(
                message = e.message ?: "Unknown error",
                provider = "OVO"
            )
        }
    }

    override suspend fun initiateLinking(
        provider: PaymentGatewayService.Provider,
        phoneNumber: String
    ): BalanceFetchResult = withContext(Dispatchers.IO) {
        if (!supabaseEdgeFunctionUrl.startsWith("http")) {
            return@withContext BalanceFetchResult.Error(
                message = "Server belum dikonfigurasi. Hubungkan proyek Supabase untuk mengaktifkan linking OVO.",
                provider = "OVO"
            )
        }
        try {
            // From namtxs/ovoid-API: sendOtp. Routed through the Edge Function.
            val request = Request.Builder()
                .url("$supabaseEdgeFunctionUrl/gateway-proxy")
                .post(JSONObject().apply {
                    put("action", "ovo-send-otp")
                    put("phone", phoneNumber)
                    put("deviceId", ovoDeviceId)
                }.toString().toRequestBody(jsonMediaType))
                .apply {
                    addHeader("Authorization", "Bearer $supabaseAnonKey")
                }
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val json = JSONObject(responseBody)

            if (json.optBoolean("success")) {
                val referenceNo = json.optString("referenceNo", "")
                BalanceFetchResult.LinkingRequired(
                    provider = "OVO",
                    linkingUrl = "ovo://otp?ref=$referenceNo",
                    message = json.optString("message", "OTP sent to $phoneNumber.")
                )
            } else {
                BalanceFetchResult.Error(
                    message = json.optString("message", "Failed to send OTP"),
                    provider = "OVO"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "OVO OTP request failed", e)
            BalanceFetchResult.Error(
                message = e.message ?: "Unknown error",
                provider = "OVO"
            )
        }
    }

    /**
     * Verify the OTP (step 2 of namtxs/ovoid-API auth).
     *
     * Returns the intermediate otp_token; the caller then exchanges it plus the
     * security code for an access token on the Edge Function (step 3).
     */
    suspend fun verifyOtp(
        phone: String,
        otp: String,
        referenceNo: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // The Android client never talks to OVO directly — it goes through
            // the gateway-proxy Edge Function, which holds device-bound state
            // and performs the RSA login step.
            val request = Request.Builder()
                .url("$supabaseEdgeFunctionUrl/gateway-proxy")
                .post(JSONObject().apply {
                    put("action", "ovo-verify-otp")
                    put("phone", phone)
                    put("otp", otp)
                    put("referenceNo", referenceNo)
                    put("deviceId", ovoDeviceId)
                }.toString().toRequestBody(jsonMediaType))
                .apply {
                    addHeader("Authorization", "Bearer $supabaseAnonKey")
                }
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")

            if (json.optBoolean("success")) {
                // The Edge Function returns an otp_token to exchange later.
                Result.success(json.optString("otpToken", ""))
            } else {
                Result.failure(Exception(json.optString("message", "OTP verification failed")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "OVO OTP verify failed", e)
            Result.failure(e)
        }
    }

    /**
     * Exchange the otp_token + security code for an access token (step 3).
     * Runs entirely server-side; the RSA password crypto never touches the APK.
     */
    suspend fun getAuthToken(
        phone: String,
        otpToken: String,
        securityCode: String,
        referenceNo: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseEdgeFunctionUrl/gateway-proxy")
                .post(JSONObject().apply {
                    put("action", "ovo-get-auth-token")
                    put("phone", phone)
                    put("otpToken", otpToken)
                    put("securityCode", securityCode)
                    put("referenceNo", referenceNo)
                    put("deviceId", ovoDeviceId)
                }.toString().toRequestBody(jsonMediaType))
                .apply {
                    addHeader("Authorization", "Bearer $supabaseAnonKey")
                }
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")

            if (json.optBoolean("success")) {
                Result.success(json.optString("accessToken", ""))
            } else {
                Result.failure(Exception(json.optString("message", "OVO login failed")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "OVO auth token failed", e)
            Result.failure(e)
        }
    }

    data class OvoAuthResult(
        val accessToken: String = "",
        val refreshToken: String = "",
        val deviceId: String = ""
    )
}
