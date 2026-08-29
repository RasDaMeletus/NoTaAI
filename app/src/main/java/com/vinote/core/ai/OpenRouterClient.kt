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
 * OpenRouter AI Gateway Client
 * Provides OpenAI-compatible Chat Completion API access.
 * Securely wraps network calls and falls back gracefully when offline.
 */
class OpenRouterClient(
    private var apiKey: String = "",
    private var defaultModel: String = "google/gemini-2.5-flash"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun setApiKey(key: String) {
        apiKey = key.trim()
    }

    fun getApiKey(): String = apiKey

    fun setModel(model: String) {
        defaultModel = model.trim()
    }

    fun getModel(): String = defaultModel

    suspend fun chatCompletion(
        messages: List<OpenRouterMessage>,
        model: String = defaultModel,
        temperature: Double = 0.3,
        responseFormatJson: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("OpenRouter API key is not configured"))
            }

            val rootJson = JSONObject()
            rootJson.put("model", model)
            rootJson.put("temperature", temperature)

            if (responseFormatJson) {
                val formatObj = JSONObject()
                formatObj.put("type", "json_object")
                rootJson.put("response_format", formatObj)
            }

            val messagesArray = JSONArray()
            for (msg in messages) {
                val msgObj = JSONObject()
                msgObj.put("role", msg.role)
                msgObj.put("content", msg.content)
                messagesArray.put(msgObj)
            }
            rootJson.put("messages", messagesArray)

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://vinote.app")
                .addHeader("X-Title", "ViNote Financial Companion")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w("OpenRouterClient", "Request failed with code ${response.code}: $responseBody")
                return@withContext Result.failure(Exception("OpenRouter API error: ${response.code} - $responseBody"))
            }

            val respJson = JSONObject(responseBody)
            val choices = respJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(Exception("No choices returned in OpenRouter response"))
            }

            val firstChoice = choices.getJSONObject(0)
            val messageObj = firstChoice.optJSONObject("message")
            val content = messageObj?.optString("content") ?: ""

            Result.success(content)
        } catch (e: Exception) {
            Log.e("OpenRouterClient", "Network or parsing error calling OpenRouter", e)
            Result.failure(e)
        }
    }
}

data class OpenRouterMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)
