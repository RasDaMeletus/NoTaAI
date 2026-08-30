package com.vinote.core.ai

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * OpenRouter AI Gateway Client.
 * The APK never contains or sends an OpenRouter API key. Requests go through
 * the authenticated Firebase Callable Function `openRouterChat`.
 */
class OpenRouterClient(
    private var defaultModel: String = "google/gemini-2.5-flash"
) {
    private val functions = FirebaseFunctions.getInstance("asia-southeast2")

    fun setApiKey(@Suppress("UNUSED_PARAMETER") key: String) {
        // Kept for source compatibility. OpenRouter credentials are server-side only.
    }

    fun getApiKey(): String = "firebase-function"

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
            val payload = hashMapOf<String, Any>(
                "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
                "model" to model,
                "temperature" to temperature,
                "responseFormatJson" to responseFormatJson
            )

            val result = suspendCancellableCoroutine<Any?> { continuation ->
                val task = functions
                    .getHttpsCallable("openRouterChat")
                    .call(payload)
                    .addOnSuccessListener { response ->
                        if (continuation.isActive) continuation.resume(response.data)
                    }
                    .addOnFailureListener { error ->
                        Log.w("OpenRouterClient", "Firebase AI gateway failed", error)
                        if (continuation.isActive) continuation.resume(null)
                    }

                continuation.invokeOnCancellation { task.cancel() }
            }

            @Suppress("UNCHECKED_CAST")
            val responseMap = result as? Map<String, Any?>
            val content = responseMap?.get("content") as? String
            if (content.isNullOrBlank()) {
                Result.failure(IllegalStateException("AI gateway returned no content"))
            } else {
                Result.success(content)
            }
        } catch (e: Exception) {
            Log.e("OpenRouterClient", "Error calling Firebase AI gateway", e)
            Result.failure(e)
        }
    }
}

data class OpenRouterMessage(
    val role: String,
    val content: String
)
