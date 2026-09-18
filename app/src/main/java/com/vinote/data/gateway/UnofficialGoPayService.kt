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
 * Unofficial GoPay balance checker.
 *
 * Uses reverse-engineered Gojek API endpoints from:
 * - https://github.com/akmaldira/unofficial-gojek-api
 * - https://github.com/namtxs/gopay-api
 *
 * Flow:
 * 1. User enters phone → send OTP via goid/login/request
 * 2. User enters OTP → verify via goid/token
 * 3. If MFA required → verify PIN via customer API → get token
 * 4. Fetch balance via /v1/payment-options/balances
 *
 * ⚠️ UNOFFICIAL — may break when Gojek updates their app.
 * ⚠️ All calls go through Supabase Edge Function proxy.
 */
class UnofficialGoPayService(
    private val supabaseEdgeFunctionUrl: String,
    private val supabaseAnonKey: String
) : PaymentGatewayService {

    companion object {
        private const val TAG = "UnofficialGoPay"
        // Endpoint constants from namtxs/gopay-api
        // (https://github.com/namtxs/gopay-api), Gojek API update of 2022-03-08.
        //
        // ⚠️ The client_secret is NEVER stored in the APK. Every call is
        // routed through the gateway-proxy Edge Function, which holds it as a
        // Supabase secret and injects it server-side.
        private const val BASE_URL_GOID = "https://goid.gojekapi.com"
        private const val BASE_URL_CUSTOMER = "https://customer.gopayapi.com"

        // Header set mirrors namtxs/gopay-api buildHeaders() exactly.
        // Identifiers that must be stable per install are generated at runtime.
        private val GOJEK_HEADERS = mapOf(
            "Content-Type" to "application/json",
            "Accept" to "*/*",
            "User-Agent" to "Gojek/122076431 CFNetwork/1404.0.5 Darwin/22.3.0",
            "X-Platform" to "iOS",
            "X-Appversion" to "4.88.0",
            "X-Appid" to "com.go-jek.ios",
            "X-User-Type" to "customer",
            "X-User-Locale" to "id_ID",
            "X-Deviceos" to "iOS, 15.6.1",
            "X-Phonemake" to "Apple",
            "X-Phonemodel" to "Apple, iPhone XS Max",
            "X-Signature" to "1001",
            "Gojek-Country-Code" to "ID"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override val gatewayName: String = "GoPay (Unofficial)"

    override fun supportsProvider(provider: PaymentGatewayService.Provider): Boolean {
        return provider == PaymentGatewayService.Provider.GOPAY
    }

    override suspend fun fetchBalance(
        provider: PaymentGatewayService.Provider,
        accountId: String,
        accessToken: String?
    ): BalanceFetchResult = withContext(Dispatchers.IO) {
        if (accessToken == null) {
            return@withContext BalanceFetchResult.LinkingRequired(
                provider = "GoPay",
                message = "Please sign in to GoPay first to fetch your balance."
            )
        }

        try {
            // Route through the gateway-proxy Edge Function so no GoPay
            // credentials or headers need to be assembled in the APK.
            // From akmaldira/unofficial-gojek-api: EP_PAYMENT_OPTIONS_BALANCES
            val request = Request.Builder()
                .url("$supabaseEdgeFunctionUrl/gateway-proxy")
                .post(JSONObject().apply {
                    put("action", "gopay-balance")
                    put("accessToken", accessToken)
                }.toString().toRequestBody(jsonMediaType))
                .apply {
                    addHeader("Authorization", "Bearer $supabaseAnonKey")
                }
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext BalanceFetchResult.Error(
                    message = "HTTP ${response.code}: $responseBody",
                    code = response.code.toString(),
                    provider = "GoPay"
                )
            }

            val json = JSONObject(responseBody)
            if (json.optBoolean("success")) {
                return@withContext BalanceFetchResult.Success(
                    balance = json.optLong("balance", 0),
                    provider = "GoPay",
                    accountId = accountId,
                    accessToken = accessToken,
                    rawResponse = responseBody
                )
            }

            BalanceFetchResult.Error(
                message = json.optString("message", "No balance data in response"),
                provider = "GoPay"
            )
        } catch (e: Exception) {
            Log.e(TAG, "GoPay balance fetch failed", e)
            BalanceFetchResult.Error(
                message = e.message ?: "Unknown error",
                provider = "GoPay"
            )
        }
    }

    override suspend fun initiateLinking(
        provider: PaymentGatewayService.Provider,
        phoneNumber: String
    ): BalanceFetchResult = withContext(Dispatchers.IO) {
        // Fail clearly when Supabase isn't configured, rather than throwing a
        // cryptic "no scheme" network error.
        if (!supabaseEdgeFunctionUrl.startsWith("http")) {
            return@withContext BalanceFetchResult.Error(
                message = "Server belum dikonfigurasi. Hubungkan proyek Supabase untuk mengaktifkan linking GoPay.",
                provider = "GoPay"
            )
        }
        try {
            // Route through the gateway-proxy Edge Function so the GoPay
            // client_secret stays server-side. namtxs/gopay-api loginRequest():
            // country_code + magic_link_ref + phone_number
            val request = Request.Builder()
                .url("$supabaseEdgeFunctionUrl/gateway-proxy")
                .post(JSONObject().apply {
                    put("action", "gopay-login")
                    put("phone", phoneNumber)
                }.toString().toRequestBody(jsonMediaType))
                .apply {
                    addHeader("Authorization", "Bearer $supabaseAnonKey")
                }
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")

            if (json.optBoolean("success")) {
                val otpToken = json.optString("otpToken", "")
                BalanceFetchResult.LinkingRequired(
                    provider = "GoPay",
                    linkingUrl = "gopay://otp?token=$otpToken",
                    message = json.optString("message", "OTP sent to $phoneNumber.")
                )
            } else {
                BalanceFetchResult.Error(
                    message = json.optString("message", "Failed to send OTP"),
                    provider = "GoPay"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "GoPay OTP request failed", e)
            BalanceFetchResult.Error(
                message = e.message ?: "Unknown error",
                provider = "GoPay"
            )
        }
    }

    /**
     * Verify OTP and get access token.
     * If MFA is required, returns requiresMFA=true with challenge info.
     */
    suspend fun verifyOtp(
        phone: String,
        otp: String,
        otpToken: String
    ): Result<GoPayAuthResult> = withContext(Dispatchers.IO) {
        try {
            // Route through the gateway-proxy Edge Function so the GoPay
            // client_secret stays server-side. The APK never sees it.
            val request = Request.Builder()
                .url("$supabaseEdgeFunctionUrl/gateway-proxy")
                .post(JSONObject().apply {
                    put("action", "gopay-verify-otp")
                    put("phone", phone)
                    put("otp", otp)
                    put("otpToken", otpToken)
                }.toString().toRequestBody(jsonMediaType))
                .apply {
                    addHeader("Authorization", "Bearer $supabaseAnonKey")
                }
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")

            if (json.optBoolean("success") && json.has("accessToken")) {
                return@withContext Result.success(
                    GoPayAuthResult(
                        accessToken = json.getString("accessToken"),
                        refreshToken = json.optString("refreshToken", "")
                    )
                )
            }

            // MFA challenge from the Edge Function
            if (json.optBoolean("requiresMFA")) {
                return@withContext Result.success(
                    GoPayAuthResult(
                        requiresMFA = true,
                        challengeId = json.optString("challengeId", ""),
                        challengeToken = json.optString("challengeToken", "")
                    )
                )
            }

            Result.failure(Exception(json.optString("message", "OTP verification failed")))
        } catch (e: Exception) {
            Log.e(TAG, "GoPay OTP verify failed", e)
            Result.failure(e)
        }
    }

    /**
     * Verify MFA PIN and get access token.
     * From akmaldira/unofficial-gojek-api: EP_VERIFY_MFA + EP_VERIFY_OTP
     */
    suspend fun verifyMfa(
        challengeId: String,
        pin: String,
        challengeToken: String
    ): Result<GoPayAuthResult> = withContext(Dispatchers.IO) {
        try {
            // Route the PIN/MFA exchange through the Edge Function so the
            // client_secret and MFA client id stay server-side.
            val request = Request.Builder()
                .url("$supabaseEdgeFunctionUrl/gateway-proxy")
                .post(JSONObject().apply {
                    put("action", "gopay-verify-mfa")
                    put("challengeId", challengeId)
                    put("pin", pin)
                    put("challengeToken", challengeToken)
                }.toString().toRequestBody(jsonMediaType))
                .apply {
                    addHeader("Authorization", "Bearer $supabaseAnonKey")
                }
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")

            if (json.optBoolean("success") && json.has("accessToken")) {
                Result.success(
                    GoPayAuthResult(
                        accessToken = json.getString("accessToken"),
                        refreshToken = json.optString("refreshToken", "")
                    )
                )
            } else {
                Result.failure(Exception(json.optString("message", "PIN verification failed")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "GoPay MFA verify failed", e)
            Result.failure(e)
        }
    }

    data class GoPayAuthResult(
        val accessToken: String = "",
        val refreshToken: String = "",
        val requiresMFA: Boolean = false,
        val challengeId: String = "",
        val challengeToken: String = ""
    )
}
