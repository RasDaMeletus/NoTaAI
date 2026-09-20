package com.vinote.core.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenRouter AI Gateway Client.
 * All requests are proxied through a Supabase Edge Function so the
 * OPENROUTER_API_KEY is never stored inside the APK.
 *
 * Only free OpenRouter models are used, per the project requirement. The
 * primary model is finance-tuned; if it is rate-limited the call retries down
 * the [FreeModels.CHAIN] before giving up.
 */
object FreeModels {
    const val LING_FLASH_FIN = "inclusionai/ling-3.0-flash-fin:free"
    const val NEX_N25_MINI = "nex-agi/nex-n2.5-mini:free"
    const val LIQUID_LFM = "liquid/lfm-2.5-2.6b:free"
    const val LING_FLASH_VL = "inclusionai/ling-3.0-flash-vl:free"

    /** Ordered list tried on 429 / 5xx upstream errors. */
    val CHAIN = listOf(LING_FLASH_FIN, NEX_N25_MINI, LING_FLASH_VL, LIQUID_LFM)
}

private const val FREE_MODEL_PRIMARY = FreeModels.LING_FLASH_FIN

class OpenRouterClient(
    // Free OpenRouter models only, per project requirement. The finance-tuned
    // Ling Flash Fin is tried first; if it is rate-limited we fall back to
    // Nex N2.5 Mini, then Liquid LFM. Paid slugs are never used.
    private var defaultModel: String = FREE_MODEL_PRIMARY,
    // Edge Function URL injected externally via SupabaseClientProvider.
    // Must be `var` so configure() can store the project URL at runtime.
    private var proxyUrl: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Resolve the current Supabase user access token for every request.
    // The session may be refreshed or cleared after this client is created.
    private var accessTokenProvider: suspend () -> String? = { null }

    fun configure(
        proxyBaseUrl: String,
        accessTokenProvider: suspend () -> String?
    ) {
        proxyUrl = proxyBaseUrl.trimEnd('/')
        this.accessTokenProvider = accessTokenProvider
    }

    fun setModel(model: String) { defaultModel = model.trim() }
    fun getModel(): String = defaultModel

    suspend fun chatCompletion(
        messages: List<OpenRouterMessage>,
        model: String = defaultModel,
        temperature: Double = 0.3,
        responseFormatJson: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val url = proxyUrl.trimEnd('/')
        if (url.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("OpenRouter proxy URL not configured"))
        }

        val accessToken = accessTokenProvider()?.takeIf { it.isNotBlank() }
            ?: return@withContext Result.failure(
                IllegalStateException("Sign in is required to use online AI")
            )

        // Free models are frequently rate-limited; try the requested model,
        // then walk the free chain until one answers.
        val candidates = LinkedHashSet(listOf(model) + FreeModels.CHAIN)
        var lastError: Throwable? = null

        for (currentModel in candidates) {
            try {
                val messagesArray = JSONArray()
                messages.forEach { msg ->
                    messagesArray.put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
                val bodyJson = JSONObject().apply {
                    put("model", currentModel)
                    put("messages", messagesArray)
                    put("temperature", temperature)
                    // Free reasoning models need headroom to finish thinking
                    // before emitting the answer content.
                    put("max_tokens", 800)
                    if (responseFormatJson) put("response_format", JSONObject().put("type", "json_object"))
                }
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", accessToken)
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(req).execute()
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.w("OpenRouterClient", "Proxy HTTP ${response.code} for $currentModel: $bodyStr")
                    lastError = IllegalStateException("Proxy HTTP ${response.code}")
                    // 402/429 → try the next free model; other codes → stop.
                    if (response.code != 402 && response.code != 429 && response.code != 503) {
                        return@withContext Result.failure(lastError!!)
                    }
                    continue
                }
                val content = JSONObject(bodyStr).optString("content", "")
                if (content.isBlank()) {
                    Log.w("OpenRouterClient", "$currentModel returned empty content (reasoning-only)")
                    lastError = IllegalStateException("Empty content from $currentModel")
                    continue
                }
                return@withContext Result.success(content)
            } catch (e: Exception) {
                Log.w("OpenRouterClient", "$currentModel failed: ${e.message}")
                lastError = e
            }
        }

        Result.failure(lastError ?: IllegalStateException("No free model available"))
    }
}

data class OpenRouterMessage(
    val role: String,
    val content: String
)