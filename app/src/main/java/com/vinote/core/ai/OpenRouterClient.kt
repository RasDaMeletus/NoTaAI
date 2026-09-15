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
 */
class OpenRouterClient(
    private var defaultModel: String = "google/gemini-2.5-flash",
    // Edge Function URL injected externally via SupabaseClientProvider
    private val proxyUrl: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // anonKey injected externally (safe to expose in APK; not the secret key)
    private var anonKey: String = ""

    fun configure(proxyBaseUrl: String, anon: String) {
        // ponytail: if multiple concurrent requests needed, build a new OkHttpClient here
        anonKey = anon
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
        try {
            val messagesArray = JSONArray()
            messages.forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
            val bodyJson = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("temperature", temperature)
                if (responseFormatJson) put("response_format", JSONObject().put("type", "json_object"))
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .apply { if (anonKey.isNotBlank()) addHeader("apikey", anonKey) }
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(req).execute()
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("OpenRouterClient", "Proxy HTTP ${response.code}: $bodyStr")
                return@withContext Result.failure(IllegalStateException("Proxy HTTP ${response.code}"))
            }
            val content = JSONObject(bodyStr).optString("content", "")
            if (content.isBlank()) {
                Result.failure(IllegalStateException("OpenRouter proxy returned empty content"))
            } else {
                Result.success(content)
            }
        } catch (e: Exception) {
            Log.e("OpenRouterClient", "Error calling OpenRouter proxy", e)
            Result.failure(e)
        }
    }
}

data class OpenRouterMessage(
    val role: String,
    val content: String
)